"""
stock_pipeline_v2.py

Pipeline amélioré pour :
- nettoyage robuste et feature-engineering
- entraînement multi-modèles (régression + classification)
- optimisation d'hyperparamètres (RandomizedSearchCV)
- gestion du déséquilibre de classes (SMOTE ou class_weight)
- sauvegarde des modèles, métriques et rapport
- fonction de prédiction avancée avec suggestion de produit similaire

Usage : placer 'stock_dataset.xlsx' dans le même dossier que ce script, installer
les dépendances listées dans requirements.txt, puis lancer :
    python stock_pipeline_v2.py

Dépendances (minimales) : pandas, numpy, scikit-learn, matplotlib, seaborn, joblib
Optionnelles (améliorations) : xgboost, imbalanced-learn

Le script est conçu pour détecter l'absence d'une dépendance optionnelle et la
contourner proprement.
"""

import os
import sys
import json
from pathlib import Path
from datetime import datetime
import warnings
warnings.filterwarnings('ignore')

import numpy as np
import pandas as pd
import matplotlib.pyplot as plt
import seaborn as sns

from sklearn.model_selection import train_test_split, RandomizedSearchCV, cross_val_score
from sklearn.ensemble import RandomForestRegressor, RandomForestClassifier, GradientBoostingRegressor, GradientBoostingClassifier
from sklearn.linear_model import LinearRegression, Ridge, Lasso, LogisticRegression
from sklearn.preprocessing import StandardScaler, OneHotEncoder
from sklearn.impute import SimpleImputer
from sklearn.compose import ColumnTransformer
from sklearn.pipeline import Pipeline
from sklearn.metrics import mean_squared_error, accuracy_score, precision_score, recall_score, f1_score
from sklearn.metrics import confusion_matrix
from sklearn.model_selection import KFold

import joblib

try:
    import xgboost as xgb
    HAS_XGBOOST = True
except Exception:
    HAS_XGBOOST = False

try:
    from imblearn.over_sampling import SMOTE
    HAS_SMOTE = True
except Exception:
    HAS_SMOTE = False

from difflib import get_close_matches


BASE_DIR = Path(__file__).parent
DATA_FILE = BASE_DIR / 'stock_dataset.xlsx'
MODEL_DIR = BASE_DIR / 'models_v2'
REPORT_PATH = BASE_DIR / 'training_report.md'
METRICS_PATH = BASE_DIR / 'model_metrics.json'
FIG_DIR = BASE_DIR / 'figures_v2'

os.makedirs(MODEL_DIR, exist_ok=True)
os.makedirs(FIG_DIR, exist_ok=True)


def load_data(path: Path) -> pd.DataFrame:
    print(f"[1/9] Chargement du dataset depuis: {path}")
    if not path.exists():
        raise FileNotFoundError(f"Fichier introuvable: {path}. Placez 'stock_dataset.xlsx' dans le dossier du script.")
    df = pd.read_excel(path)
    print(f"  -> Chargé : {len(df)} lignes, {len(df.columns)} colonnes")
    return df


def detect_and_rename_columns(df: pd.DataFrame) -> pd.DataFrame:
    # Normaliser noms colonnes attendues (plusieurs variantes possibles)
    df = df.rename(columns={
        'product_name': 'name',
        'product_category': 'category',
        'movementType': 'movement_type',
        'movement_type': 'movement_type',
        'quantity': 'movement_quantity',
        'stock_quantity': 'quantity_stock',
        'reorder_level': 'low_stock_threshold',
        'supplier': 'supplier_name',
        'created_at': 'created_at',
        'expiration_date': 'expiration_date'
    })
    return df


def clean_data(df: pd.DataFrame) -> pd.DataFrame:
    print('[2/9] Nettoyage des données...')
    df = df.copy()

    before = len(df)
    # Drop exact duplicates
    df.drop_duplicates(inplace=True)
    print(f"  -> Doublons exacts supprimés: {before - len(df)}")

    # Strip column names
    df.columns = [c.strip() for c in df.columns]

    # Drop obviously useless columns if present
    for c in ['description', 'extra_notes', 'barcode', 'warehouse_location']:
        if c in df.columns:
            df.drop(columns=c, inplace=True)

    # Standardiser les colonnes numériques importantes
    numeric_cols = ['quantity_stock', 'movement_quantity', 'low_stock_threshold', 'price', 'defective_units']
    for c in numeric_cols:
        if c in df.columns:
            df[c] = pd.to_numeric(df[c], errors='coerce')

    # Dates
    for dcol in ['created_at', 'expiration_date']:
        if dcol in df.columns:
            df[dcol] = pd.to_datetime(df[dcol], errors='coerce')

    # Remove rows with invalid key data
    if 'quantity_stock' in df.columns:
        df = df[df['quantity_stock'].notna()]
    if 'movement_quantity' in df.columns:
        df = df[df['movement_quantity'].notna()]

    # Fill numeric NA: price by median, others by 0
    if 'price' in df.columns:
        median_price = df['price'].median()
        df['price'].fillna(median_price, inplace=True)
    for c in ['movement_quantity', 'quantity_stock', 'low_stock_threshold', 'defective_units']:
        if c in df.columns:
            df[c].fillna(0, inplace=True)

    # Map movement_type
    if 'movement_type' in df.columns:
        df['movement_type'] = df['movement_type'].astype(str).str.strip().str.upper()
        mapping = {'IN': 1, 'OUT': -1, 'ADJUSTMENT': 0, 'SALE': -1, 'RESTOCK': 1}
        df['movement_type_num'] = df['movement_type'].map(mapping).fillna(0)
    else:
        df['movement_type_num'] = 0

    # Remove negative quantities/price
    if 'quantity_stock' in df.columns:
        neg_q = (df['quantity_stock'] < 0).sum()
        df = df[df['quantity_stock'] >= 0]
        print(f"  -> Lignes avec quantity_stock négatif supprimées: {neg_q}")
    if 'price' in df.columns:
        neg_p = (df['price'] < 0).sum()
        df = df[df['price'] >= 0]
        print(f"  -> Lignes avec price négatif supprimées: {neg_p}")

    # Remove outliers simple rule (cap movement_quantity at 99th percentile)
    if 'movement_quantity' in df.columns:
        upper = df['movement_quantity'].quantile(0.99)
        capped = (df['movement_quantity'] > upper).sum()
        if capped > 0:
            df.loc[df['movement_quantity'] > upper, 'movement_quantity'] = upper
            print(f"  -> movement_quantity plafonné à 99e percentile ({upper}) pour {capped} lignes")

    df.reset_index(drop=True, inplace=True)
    print(f"  -> Après nettoyage: {len(df)} lignes")
    return df


def extract_time_features(df: pd.DataFrame) -> pd.DataFrame:
    print('[3/9] Extraction des features temporelles...')
    df = df.copy()
    if 'created_at' in df.columns:
        dt = df['created_at']
        df['created_year'] = dt.dt.year
        df['created_month'] = dt.dt.month
        df['created_day'] = dt.dt.day
        df['created_weekday'] = dt.dt.dayofweek
        # Saison: simple mapping
        df['created_season'] = df['created_month'] % 12 // 3 + 1
    return df


def feature_engineering(df: pd.DataFrame) -> pd.DataFrame:
    print('[4/9] Feature engineering...')
    df = df.copy()

    # Signed movement
    if 'movement_quantity' in df.columns and 'movement_type_num' in df.columns:
        df['movement_signed'] = df['movement_quantity'] * df['movement_type_num']
    else:
        df['movement_signed'] = df.get('movement_quantity', 0)

    # Aggregats par produit
    if 'name' in df.columns:
        agg = df.groupby('name').agg(
            total_movement=('movement_signed', 'sum'),
            mean_stock=('quantity_stock', 'mean'),
            nb_records=('name', 'count')
        ).reset_index()
        df = df.merge(agg, on='name', how='left')

    # is_low_stock
    if 'quantity_stock' in df.columns and 'low_stock_threshold' in df.columns:
        df['is_low_stock'] = (df['quantity_stock'] <= df['low_stock_threshold']).astype(int)
    else:
        df['is_low_stock'] = 0

    return df


def build_preprocessor(df: pd.DataFrame):
    # Determine categorical and numerical columns automatically
    cat_cols = [c for c in df.columns if df[c].dtype == 'object' and c not in ['name', 'reason', 'movement_type']]
    # Include some known cats if present
    for c in ['category', 'reason', 'movement_type']:
        if c in df.columns and c not in cat_cols:
            cat_cols.append(c)

    num_cols = [c for c in df.select_dtypes(include=[np.number]).columns if c not in ['is_low_stock']]

    # Exclude identifier columns that can leak information
    id_cols = ['product_id', 'movement_id', 'sku']
    for idc in id_cols:
        if idc in num_cols:
            num_cols.remove(idc)
        if idc in cat_cols:
            cat_cols.remove(idc)

    # Remove target or directly derived columns to avoid leakage
    target_like = ['movement_quantity', 'movement_signed']
    for t in target_like:
        if t in num_cols:
            num_cols.remove(t)

    # Keep feature list stable
    print(f"  -> Colonnes numériques utilisées: {num_cols}")
    print(f"  -> Colonnes catégorielles utilisées: {cat_cols}")

    # Impute missing values before scaling/encoding
    numeric_transformer = Pipeline(steps=[
        ('imputer', SimpleImputer(strategy='median')),
        ('scaler', StandardScaler())
    ])
    # OneHotEncoder parameter name differs between sklearn versions ('sparse' vs 'sparse_output').
    try:
        ohe = OneHotEncoder(handle_unknown='ignore', sparse_output=False)
    except TypeError:
        ohe = OneHotEncoder(handle_unknown='ignore', sparse=False)
    categorical_transformer = Pipeline(steps=[
        ('imputer', SimpleImputer(strategy='constant', fill_value='__MISSING__')),
        ('onehot', ohe)
    ])

    preprocessor = ColumnTransformer(transformers=[
        ('num', numeric_transformer, num_cols),
        ('cat', categorical_transformer, cat_cols)
    ], remainder='drop')

    feature_cols = num_cols + cat_cols
    return preprocessor, num_cols, cat_cols


def select_and_train_regression(X, y):
    print('[5/9] Entraînement régression (sélection multi-modèles + RandomizedSearchCV)...')
    results = {}

    models = {}
    models['RandomForest'] = RandomForestRegressor(random_state=42)
    models['GradientBoosting'] = GradientBoostingRegressor(random_state=42)
    models['Linear'] = LinearRegression()
    models['Ridge'] = Ridge()
    models['Lasso'] = Lasso()
    if HAS_XGBOOST:
        models['XGBoost'] = xgb.XGBRegressor(objective='reg:squarederror', random_state=42)

    param_dists = {
        'RandomForest': {'n_estimators': [50, 100], 'max_depth': [None, 10, 20]},
        'GradientBoosting': {'n_estimators': [50, 100], 'learning_rate': [0.01, 0.1], 'max_depth': [3, 5]},
        'XGBoost': {'n_estimators': [50, 100], 'learning_rate': [0.01, 0.1], 'max_depth': [3, 5]} if HAS_XGBOOST else {}
    }

    best_model = None
    best_score = float('inf')

    for name, model in models.items():
        print(f"  -> Testing {name}...")
        if name in param_dists and param_dists[name]:
            rs = RandomizedSearchCV(model, param_distributions=param_dists[name], n_iter=4, cv=5, scoring='neg_root_mean_squared_error', random_state=42, n_jobs=-1)
            rs.fit(X, y)
            chosen = rs.best_estimator_
            score = -rs.best_score_
            print(f"     Best CV RMSE (est): {score:.4f} | params: {rs.best_params_}")
        else:
            # quick CV estimate
            scores = cross_val_score(model, X, y, scoring='neg_root_mean_squared_error', cv=5, n_jobs=-1)
            score = -np.mean(scores)
            model.fit(X, y)
            chosen = model
            print(f"     CV RMSE (est): {score:.4f}")

        if score < best_score:
            best_score = score
            best_model = chosen
            best_name = name

    print(f"  -> Meilleur modèle régression: {best_name} (RMSE CV estimé: {best_score:.4f})")
    results['model'] = best_model
    results['name'] = best_name
    results['cv_rmse'] = float(best_score)
    return results


def select_and_train_classification(X, y):
    print('[6/9] Entraînement classification (gestion imbalance + comparaison modèles)...')
    results = {}

    # Option SMOTE
    if HAS_SMOTE:
        print('  -> SMOTE disponible: utilisation pour sur-échantillonnage de la classe minoritaire')
        smote = SMOTE(random_state=42)
        X_res, y_res = smote.fit_resample(X, y)
    else:
        print('  -> SMOTE non disponible: utilisation des class_weight="balanced" sur certains modèles')
        X_res, y_res = X, y

    models = {}
    models['Logistic'] = LogisticRegression(max_iter=1000, class_weight='balanced')
    models['RandomForest'] = RandomForestClassifier(class_weight='balanced', random_state=42)
    models['GradientBoosting'] = GradientBoostingClassifier(random_state=42)
    if HAS_XGBOOST:
        models['XGBoost'] = xgb.XGBClassifier(use_label_encoder=False, eval_metric='logloss', random_state=42)

    best_model = None
    best_score = 0.0

    for name, model in models.items():
        print(f"  -> Testing {name}...")
        scores = cross_val_score(model, X_res, y_res, scoring='f1', cv=5, n_jobs=-1)
        mean_f1 = np.mean(scores)
        print(f"     CV F1: {mean_f1:.4f}")
        model.fit(X_res, y_res)

        if mean_f1 > best_score:
            best_score = mean_f1
            best_model = model
            best_name = name

    print(f"  -> Meilleur modèle classification: {best_name} (F1 CV: {best_score:.4f})")
    results['model'] = best_model
    results['name'] = best_name
    results['cv_f1'] = float(best_score)
    return results


def evaluate_models(reg_model, clf_model, X_test_reg, y_test_reg, X_test_clf, y_test_clf):
    print('[7/9] Évaluation sur jeu test...')
    metrics = {}

    # Regression
    y_pred_reg = reg_model.predict(X_test_reg)
    mse = mean_squared_error(y_test_reg, y_pred_reg)
    rmse = np.sqrt(mse)
    metrics['regression'] = {'mse': float(mse), 'rmse': float(rmse)}
    print(f"  -> Regression: MSE={mse:.4f}, RMSE={rmse:.4f}")

    # Classification
    y_pred_clf = clf_model.predict(X_test_clf)
    acc = accuracy_score(y_test_clf, y_pred_clf)
    prec = precision_score(y_test_clf, y_pred_clf, zero_division=0)
    rec = recall_score(y_test_clf, y_pred_clf, zero_division=0)
    f1 = f1_score(y_test_clf, y_pred_clf, zero_division=0)
    metrics['classification'] = {'accuracy': float(acc), 'precision': float(prec), 'recall': float(rec), 'f1': float(f1)}
    print(f"  -> Classification: acc={acc:.4f}, precision={prec:.4f}, recall={rec:.4f}, f1={f1:.4f}")

    return metrics, y_pred_reg, y_pred_clf


def plot_feature_importances(model, preprocessor, out_path):
    try:
        # get feature names
        num_features = preprocessor.transformers_[0][2]
        cat_features = []
        if hasattr(preprocessor.transformers_[1][1].named_steps['onehot'], 'get_feature_names_out'):
            cat_features = list(preprocessor.transformers_[1][1].named_steps['onehot'].get_feature_names_out(preprocessor.transformers_[1][2]))
        feature_names = list(num_features) + list(cat_features)

        importances = None
        if hasattr(model, 'feature_importances_'):
            importances = model.feature_importances_
        elif HAS_XGBOOST and isinstance(model, xgb.XGBRegressor):
            importances = model.feature_importances_

        if importances is not None:
            fi = pd.Series(importances, index=feature_names).sort_values(ascending=False).head(30)
            plt.figure(figsize=(10, 8))
            sns.barplot(x=fi.values, y=fi.index)
            plt.title('Feature importances')
            plt.tight_layout()
            plt.savefig(out_path)
            plt.close()
            print(f"  -> Feature importances saved: {out_path}")
    except Exception as e:
        print(f"  -> Impossible d'afficher feature importances: {e}")


def save_artifacts(reg_model, clf_model, metrics):
    reg_path = MODEL_DIR / 'demand_model_v2.pkl'
    clf_path = MODEL_DIR / 'stock_classifier_v2.pkl'
    joblib.dump(reg_model, reg_path)
    joblib.dump(clf_model, clf_path)
    print(f"[8/9] Modèles sauvegardés: {reg_path}, {clf_path}")

    # Sauvegarder métriques
    with open(METRICS_PATH, 'w', encoding='utf-8') as f:
        json.dump(metrics, f, indent=2)
    print(f"  -> Métriques sauvegardées: {METRICS_PATH}")


def predict_for_product(product_name: str, df: pd.DataFrame, preprocessor, reg_model, clf_model):
    print(f"[9/9] Prédiction pour produit: {product_name}")
    names = df['name'].astype(str).unique().tolist()
    matches = [n for n in names if product_name.lower() in n.lower()]
    suggestion = None
    if len(matches) == 0:
        close = get_close_matches(product_name, names, n=1, cutoff=0.6)
        if close:
            suggestion = close[0]
            print(f"  -> Produit non trouvé exactement. Suggestion proche: {suggestion}")
            selected_name = suggestion
        else:
            print("  -> Produit non trouvé et pas de suggestion fiable. Utilisation d'un premier produit du dataset.")
            selected_name = names[0]
    else:
        selected_name = matches[0]

    row = df[df['name'] == selected_name].iloc[0]

    # Build features row for prediction: use the same pipeline preprocessor
    # Create a single-row DataFrame with required columns
    sample = row.to_frame().T
    X_sample = preprocessor.transform(sample)

    pred_demand = reg_model.predict(X_sample)[0]
    pred_low = clf_model.predict(X_sample)[0]

    # Confidence score: use probability if available
    confidence = None
    if hasattr(clf_model, 'predict_proba'):
        prob = clf_model.predict_proba(X_sample)[0]
        confidence = float(np.max(prob))
    else:
        confidence = 0.0

    q_stock = float(sample['quantity_stock'].iloc[0]) if 'quantity_stock' in sample.columns else 0.0
    low_thr = float(sample['low_stock_threshold'].iloc[0]) if 'low_stock_threshold' in sample.columns else 0.0
    recommended = max(0, int(round(pred_demand + low_thr - q_stock)))

    result = {
        'product_name_used': selected_name,
        'predicted_demand': float(pred_demand),
        'is_low_stock': bool(pred_low),
        'recommended_stock': int(recommended),
        'confidence_score': float(confidence)
    }

    print('  -> Résultat:', result)
    return result


def generate_report(initial_count, final_count, reg_info, clf_info, metrics, example_prediction):
    print('Génération du rapport Markdown...')
    lines = []
    lines.append(f"# Training report\nGenerated on {datetime.now().isoformat()}\n")
    lines.append(f"- Dataset initial: {initial_count} lignes")
    lines.append(f"- Dataset après nettoyage: {final_count} lignes\n")
    lines.append('## Meilleurs modèles')
    lines.append(f"- Regression: {reg_info['name']} (CV RMSE estimé: {reg_info['cv_rmse']:.4f})")
    lines.append(f"- Classification: {clf_info['name']} (CV F1 estimé: {clf_info['cv_f1']:.4f})\n")
    lines.append('## Métriques sur le jeu test')
    lines.append('```json')
    lines.append(json.dumps(metrics, indent=2))
    lines.append('```\n')
    lines.append('## Exemple de prédiction')
    lines.append('```json')
    lines.append(json.dumps(example_prediction, indent=2))
    lines.append('```')

    with open(REPORT_PATH, 'w', encoding='utf-8') as f:
        f.write('\n'.join(lines))
    print(f"Report saved: {REPORT_PATH}")


def main():
    try:
        df_raw = load_data(DATA_FILE)
    except FileNotFoundError as e:
        print(e)
        sys.exit(1)

    initial_count = len(df_raw)
    df = detect_and_rename_columns(df_raw)
    df = clean_data(df)
    df = extract_time_features(df)
    df = feature_engineering(df)
    final_count = len(df)

    # Prepare preprocessor and features
    preprocessor, num_cols, cat_cols = build_preprocessor(df)

    # Targets
    y_reg = df['movement_quantity'] if 'movement_quantity' in df.columns else df['movement_signed']
    y_clf = df['is_low_stock']

    # Build full feature matrix via preprocessor fit
    X_all = preprocessor.fit_transform(df)
    # Save preprocessor for later inference in API
    preprocessor_path = MODEL_DIR / 'preprocessor_v2.pkl'
    joblib.dump(preprocessor, preprocessor_path)
    print(f"  -> Preprocessor saved: {preprocessor_path}")

    # Train/test split
    X_train, X_test, y_train_reg, y_test_reg, y_train_clf, y_test_clf = train_test_split(
        X_all, y_reg, y_clf, test_size=0.2, random_state=42
    )

    # Train models
    reg_info = select_and_train_regression(X_train, y_train_reg)
    clf_info = select_and_train_classification(X_train, y_train_clf)

    reg_model = reg_info['model']
    clf_model = clf_info['model']

    # Evaluate on test set
    metrics, ypred_reg, ypred_clf = evaluate_models(reg_model, clf_model, X_test, y_test_reg, X_test, y_test_clf)

    # Save artifacts (serialize only JSON-serializable parts of model info)
    selected_info = {
        'regression': {'name': reg_info.get('name'), 'cv_rmse': reg_info.get('cv_rmse')},
        'classification': {'name': clf_info.get('name'), 'cv_f1': clf_info.get('cv_f1')}
    }
    save_artifacts(reg_model, clf_model, {'selected': selected_info, 'metrics': metrics})

    # Plots
    plot_feature_importances(reg_model, preprocessor, FIG_DIR / 'feature_importances.png')

    # Example prediction
    example_pred = predict_for_product('Aspirine 500mg', df, preprocessor, reg_model, clf_model)

    # Report
    generate_report(initial_count, final_count, reg_info, clf_info, metrics, example_pred)

    print('\nPipeline terminé. Artefacts dans dossier:', MODEL_DIR)


if __name__ == '__main__':
    main()
