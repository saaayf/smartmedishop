"""
Flask API for Product Recommendation System

Endpoints:
- GET  /health
- POST /recommend          { "state": "London", "type": "Pansement médical", "price": 999 }
- POST /recommend_user     { "username": "Man", "top_n": 10 }
"""
import json
import traceback
import logging
from pathlib import Path
from flask import Flask, request, jsonify
from flask_cors import CORS
import joblib
import pandas as pd
import numpy as np

# Configure logging
logging.basicConfig(level=logging.DEBUG)

BASE_DIR = Path(__file__).parent
MODELS_DIR = BASE_DIR / 'models'

app = Flask(__name__)
CORS(app)

# Global variables for models
MODEL = None
OHE = None
SCALER = None
DATA = None


def load_models():
    """Load trained models and preprocessors"""
    global MODEL, OHE, SCALER, DATA
    
    try:
        model_path = MODELS_DIR / 'recommendation_model.pkl'
        ohe_path = MODELS_DIR / 'onehot_encoder.pkl'
        scaler_path = MODELS_DIR / 'price_scaler.pkl'
        data_path = MODELS_DIR / 'dataset.pkl'
        
        if not all(p.exists() for p in [model_path, ohe_path, scaler_path, data_path]):
            raise FileNotFoundError(f"Model files not found in {MODELS_DIR}")
        
        MODEL = joblib.load(model_path)
        OHE = joblib.load(ohe_path)
        SCALER = joblib.load(scaler_path)
        DATA = joblib.load(data_path)
        
        print(f'Loaded models from {MODELS_DIR}')
        return True
    except Exception as e:
        print(f'Error loading models: {e}')
        return False


# Try to load models at startup
try:
    load_models()
except Exception as e:
    print(f'Warning: Could not load models at startup: {e}')


@app.route('/health', methods=['GET'])
def health():
    """Health check endpoint"""
    ok = MODEL is not None and OHE is not None and SCALER is not None and DATA is not None
    return jsonify({
        'status': 'ok' if ok else 'models_not_loaded',
        'models_loaded': ok
    })


@app.route('/recommend', methods=['POST'])
def recommend():
    """Recommend products based on state, type, and price"""
    if MODEL is None or OHE is None or SCALER is None or DATA is None:
        return jsonify({'error': 'Models not loaded. Please train models first.'}), 500
    
    try:
        payload = request.get_json(force=True)
        state = payload.get('state')
        type_ = payload.get('type')
        price = payload.get('price')
        top_n = payload.get('top_n', 10)
        max_per_type = payload.get('max_per_type', 3)
        
        if not all([state, type_, price]):
            return jsonify({'error': 'Please provide state, type, and price in JSON body'}), 400
        
        # Validate state and type against training data
        valid_states = sorted(DATA['State'].unique().tolist())
        valid_types = sorted(DATA['Type'].unique().tolist())
        
        if state not in valid_states:
            return jsonify({
                'error': f'Invalid state: "{state}". Valid states are: {valid_states}'
            }), 400
        
        if type_ not in valid_types:
            return jsonify({
                'error': f'Invalid type: "{type_}". Valid types are: {valid_types[:10]}... (showing first 10)'
            }), 400
        
        price = float(price)
        top_n = int(top_n)
        max_per_type = int(max_per_type)
        
        # Call recommendation function
        recommended_df = recommend_products_diverse_strict_ordered(
            state, type_, price, top_n, max_per_type
        )
        
        # Convert to list of dictionaries
        recommendations = recommended_df.to_dict(orient='records')
        
        return jsonify({
            'recommendations': recommendations,
            'count': len(recommendations)
        })
        
    except Exception as e:
        import traceback
        error_msg = f'Recommendation error: {str(e)}\n{traceback.format_exc()}'
        print(f"ERROR in /recommend: {error_msg}")
        app.logger.error(error_msg)
        return jsonify({'error': f'Recommendation error: {e}'}), 500


@app.route('/recommend_user', methods=['POST'])
def recommend_user():
    """Recommend products for a specific user based on their purchase history"""
    if MODEL is None or OHE is None or SCALER is None or DATA is None:
        return jsonify({'error': 'Models not loaded. Please train models first.'}), 500
    
    try:
        payload = request.get_json(force=True)
        username = payload.get('username')
        top_n = payload.get('top_n', 10)
        max_per_type = payload.get('max_per_type', 3)
        purchase_history = payload.get('purchase_history', [])  # New: purchase history from database
        
        print(f"DEBUG /recommend_user: username={username}, purchase_history length={len(purchase_history) if purchase_history else 0}")
        if purchase_history:
            print(f"DEBUG /recommend_user: First purchase = {purchase_history[0] if purchase_history else 'None'}")
        
        if not username:
            return jsonify({'error': 'Please provide username in JSON body'}), 400
        
        top_n = int(top_n)
        max_per_type = int(max_per_type)
        
        # Call user recommendation function with purchase history
        recommended_df = recommend_for_user_v3(username, top_n, max_per_type, purchase_history)
        
        if recommended_df.empty:
            print(f"DEBUG /recommend_user: No recommendations generated for user: {username}")
            return jsonify({
                'error': f'No purchase history found for user: {username}',
                'recommendations': [],
                'count': 0
            }), 404
        
        # Convert to list of dictionaries
        recommendations = recommended_df.to_dict(orient='records')
        
        print(f"DEBUG /recommend_user: Generated {len(recommendations)} recommendations")
        if recommendations:
            print(f"DEBUG /recommend_user: First recommendation: {recommendations[0]}")
            print(f"DEBUG /recommend_user: All recommendations: {recommendations}")
        
        response_data = {
            'username': username,
            'recommendations': recommendations,
            'count': len(recommendations)
        }
        
        print(f"DEBUG /recommend_user: Response data: {response_data}")
        
        return jsonify(response_data)
        
    except Exception as e:
        import traceback
        error_msg = f'User recommendation error: {str(e)}\n{traceback.format_exc()}'
        print(f"ERROR in /recommend_user: {error_msg}")
        app.logger.error(error_msg)
        return jsonify({'error': f'User recommendation error: {e}'}), 500


def recommend_products_diverse_strict_ordered(state, type_, price, top_n=10, max_per_type=3):
    """
    Recommande top_n produits différents (Type, Price) avec pondération,
    limite max par Type et pas de Type identique pour deux produits consécutifs.
    """
    # 1️⃣ Features avec pondération
    x_cat = OHE.transform([[state, type_]]) * 5
    x_price = SCALER.transform([[price]]) * 3
    x_input = np.hstack([x_cat, x_price])
    
    # 2️⃣ Récupérer les 300 produits les plus proches
    distances, indices = MODEL.kneighbors(x_input, n_neighbors=300)
    recommended = DATA.iloc[indices[0]][['Type', 'Price']].copy()
    
    # 3️⃣ Sélection avec contraintes
    seen_keys = set()
    type_counts = {}
    selected = []
    last_type = None
    
    for _, row in recommended.iterrows():
        key = (row['Type'], row['Price'])
        t = row['Type']
        if key not in seen_keys:
            count = type_counts.get(t, 0)
            # Conditions : max_per_type et pas le même que le précédent
            if count < max_per_type and t != last_type:
                selected.append(row)
                seen_keys.add(key)
                type_counts[t] = count + 1
                last_type = t
        if len(selected) >= top_n:
            break
    
    # 4️⃣ Compléter si moins de top_n
    if len(selected) < top_n:
        remaining = top_n - len(selected)
        remaining_pool = DATA[['Type', 'Price']].copy()
        remaining_pool = remaining_pool[~remaining_pool.apply(
            lambda r: (r['Type'], r['Price']) in seen_keys, axis=1
        )]
        
        # Ajouter en évitant que deux types identiques se suivent
        for _, row in remaining_pool.iterrows():
            t = row['Type']
            if t != last_type:
                selected.append(row)
                last_type = t
            if len(selected) >= top_n:
                break
    
    return pd.DataFrame(selected)


def recommend_for_user_v3(username, top_n=10, max_per_type=3, purchase_history=None):
    """
    Version optimisée de recommandation personnalisée.
    Utilise les distances moyennes entre les produits de l'utilisateur
    et les produits restants.
    
    Args:
        username: Username to search in CSV
        top_n: Number of recommendations
        max_per_type: Max recommendations per type
        purchase_history: List of dicts from database with keys: State, Type, Price, StockId
    """
    # Historique utilisateur depuis CSV
    user_history = DATA[DATA['Name'] == username].copy()
    print(f"DEBUG recommend_for_user_v3: Found {len(user_history)} purchases in CSV for user: {username}")
    
    # Si on a un historique depuis la base de données, l'ajouter
    if purchase_history and len(purchase_history) > 0:
        print(f"DEBUG recommend_for_user_v3: Found {len(purchase_history)} purchases from database for user {username}")
        # Convertir l'historique de la DB en DataFrame
        db_history_list = []
        for purchase in purchase_history:
            state = purchase.get('State', 'USA')
            type_val = purchase.get('Type', '')
            price = purchase.get('Price', 0.0)
            stock_id = purchase.get('StockId', '')
            marque = purchase.get('Marque', '')
            
            print(f"DEBUG recommend_for_user_v3: Processing purchase - State: {state}, Type: {type_val}, Price: {price}, StockId: {stock_id}")
            
            db_history_list.append({
                'Name': username,
                'State': state,
                'Type': type_val,
                'Price': float(price),
                'StockId': str(stock_id),
                'Marque': marque if marque else ''
            })
        
        if db_history_list:
            db_history_df = pd.DataFrame(db_history_list)
            print(f"DEBUG recommend_for_user_v3: Created DataFrame with {len(db_history_df)} rows")
            print(f"DEBUG recommend_for_user_v3: DataFrame columns: {db_history_df.columns.tolist()}")
            print(f"DEBUG recommend_for_user_v3: DataFrame sample:\n{db_history_df.head()}")
            
            # Concaténer avec l'historique CSV
            if not user_history.empty:
                user_history = pd.concat([user_history, db_history_df], ignore_index=True)
            else:
                user_history = db_history_df.copy()
            print(f"DEBUG recommend_for_user_v3: Combined history: {len(user_history)} purchases (CSV + DB)")
        else:
            print(f"DEBUG recommend_for_user_v3: db_history_list is empty!")
    else:
        print(f"DEBUG recommend_for_user_v3: No purchase_history provided or empty")
    
    # Si toujours vide, retourner vide
    if user_history.empty:
        print(f"DEBUG recommend_for_user_v3: No purchase history found for user: {username}")
        return pd.DataFrame()
    
    print(f"DEBUG recommend_for_user_v3: Final user_history has {len(user_history)} rows")
    print(f"DEBUG recommend_for_user_v3: Columns: {user_history.columns.tolist()}")
    
    # Obtenir les StockIds déjà achetés
    purchased_stock_ids = set(user_history['StockId'].astype(str).unique())
    
    # Produits restants (pas encore achetés)
    remaining_products = DATA[~DATA['StockId'].astype(str).isin(purchased_stock_ids)]
    
    # Filtrer les lignes avec des valeurs manquantes ou invalides
    valid_history = user_history[
        (user_history['State'].notna()) & 
        (user_history['Type'].notna()) & 
        (user_history['Type'] != '') &
        (user_history['Price'].notna()) &
        (user_history['Price'] > 0)
    ].copy()
    
    if valid_history.empty:
        print(f"DEBUG recommend_for_user_v3: No valid purchase history after filtering (empty Type/State or Price)")
        print(f"DEBUG recommend_for_user_v3: user_history info:\n{user_history.info()}")
        print(f"DEBUG recommend_for_user_v3: user_history sample:\n{user_history.head()}")
        return pd.DataFrame()
    
    print(f"DEBUG recommend_for_user_v3: Valid history after filtering: {len(valid_history)} rows")
    
    # Vérifier que les types et états sont dans les données d'entraînement
    valid_states = set(DATA['State'].unique())
    valid_types = set(DATA['Type'].unique())
    
    # Mapper les types non-standard vers les types du CSV si possible
    type_mapping = {
        'Vitamins': 'Pansement médical',  # Fallback pour Vitamins
        'Vitamin': 'Pansement médical',
        'Medicaments': 'Pansement médical',
        'Medicine': 'Pansement médical'
    }
    
    # Appliquer le mapping
    valid_history['Type'] = valid_history['Type'].apply(
        lambda x: type_mapping.get(x, x) if x in type_mapping else x
    )
    
    # Filtrer pour ne garder que les types et états valides
    valid_history = valid_history[
        (valid_history['State'].isin(valid_states)) &
        (valid_history['Type'].isin(valid_types))
    ].copy()
    
    if valid_history.empty:
        print(f"DEBUG recommend_for_user_v3: No valid purchase history after state/type validation")
        print(f"DEBUG recommend_for_user_v3: Valid states: {sorted(valid_states)}")
        print(f"DEBUG recommend_for_user_v3: Valid types (first 10): {sorted(list(valid_types))[:10]}")
        print(f"DEBUG recommend_for_user_v3: User history states: {sorted(user_history['State'].unique())}")
        print(f"DEBUG recommend_for_user_v3: User history types: {sorted(user_history['Type'].unique())}")
        print(f"DEBUG recommend_for_user_v3: User history before filtering:\n{user_history[['State', 'Type', 'Price']].head()}")
        return pd.DataFrame()
    
    print(f"DEBUG recommend_for_user_v3: Final valid history: {len(valid_history)} rows")
    
    # 🔹 Encoder toutes les lignes à la fois (plus rapide)
    try:
        X_user_cat = OHE.transform(valid_history[['State', 'Type']]) * 5
        X_user_price = SCALER.transform(valid_history[['Price']].values.reshape(-1, 1)) * 3
        X_user = np.hstack([X_user_cat, X_user_price])
    except Exception as e:
        print(f"ERROR recommend_for_user_v3: Encoding failed: {e}")
        print(f"DEBUG recommend_for_user_v3: valid_history sample:\n{valid_history[['State', 'Type', 'Price']].head()}")
        return pd.DataFrame()
    
    if remaining_products.empty:
        print(f"DEBUG recommend_for_user_v3: No remaining products to recommend")
        return pd.DataFrame()
    
    X_rem_cat = OHE.transform(remaining_products[['State', 'Type']]) * 5
    X_rem_price = SCALER.transform(remaining_products[['Price']].values.reshape(-1, 1)) * 3
    X_rem = np.hstack([X_rem_cat, X_rem_price])
    
    # 🔸 Calculer toutes les distances à la fois (broadcast)
    # shape = (len(remaining_products), len(user_history))
    dists = np.linalg.norm(X_rem[:, None, :] - X_user[None, :, :], axis=2)
    avg_dist = dists.mean(axis=1)  # moyenne des distances pour chaque produit restant
    
    # 🔹 Ajouter les scores dans un DataFrame
    scores_df = remaining_products[['Type', 'Price']].copy()
    scores_df['Score'] = avg_dist
    scores_df = scores_df.sort_values(by='Score')
    
    # 🔸 Appliquer la diversité (max 3 par type + pas 2 similaires d'affilée)
    seen_types = {}
    final_list = []
    last_type = None
    
    print(f"DEBUG recommend_for_user_v3: Processing {len(scores_df)} scored products for recommendations")
    print(f"DEBUG recommend_for_user_v3: Top 10 scored products:\n{scores_df.head(10)}")
    
    for _, row in scores_df.iterrows():
        t = row['Type']
        seen_types[t] = seen_types.get(t, 0)
        if seen_types[t] < max_per_type and t != last_type:
            final_list.append({'Type': t, 'Price': row['Price']})
            seen_types[t] += 1
            last_type = t
        if len(final_list) >= top_n:
            break
    
    print(f"DEBUG recommend_for_user_v3: Generated {len(final_list)} recommendations after diversity filtering")
    if final_list:
        print(f"DEBUG recommend_for_user_v3: Recommendations: {final_list}")
    
    return pd.DataFrame(final_list)


if __name__ == '__main__':
    # If models weren't loaded at import, try loading once before running
    if MODEL is None or OHE is None or SCALER is None or DATA is None:
        try:
            load_models()
        except Exception as e:
            print('Failed to load models before starting server:', e)
            import traceback
            traceback.print_exc()
    
    print(f"Starting Flask server on 0.0.0.0:8001")
    print(f"Models loaded: MODEL={MODEL is not None}, OHE={OHE is not None}, SCALER={SCALER is not None}, DATA={DATA is not None}")
    app.run(host='0.0.0.0', port=8001, debug=True)

