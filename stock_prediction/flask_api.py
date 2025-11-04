"""
Simple Flask API to serve predictions from the trained artifacts saved by
`stock_pipeline_v2.py`.

Endpoints:
 - GET  /health
 - POST /predict         { "product_name": "Aspirine 500mg" }
 - POST /predict_features  { "features": { ... } }  (optional: full row)

The file loads `models_v2/preprocessor_v2.pkl`, `demand_model_v2.pkl`,
`stock_classifier_v2.pkl` and the dataset `stock_dataset.xlsx` at startup.
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
import os

# Configure logging
logging.basicConfig(level=logging.DEBUG)

BASE_DIR = Path(__file__).parent
MODEL_DIR = BASE_DIR / 'models_v2'
DATA_FILE = BASE_DIR / 'stock_dataset.xlsx'

app = Flask(__name__)
CORS(app)

# Try to import helper functions from the pipeline (they are defined in the script)
try:
    import stock_pipeline_v2 as sp
    print('Successfully imported stock_pipeline_v2')
except Exception as e:
    print(f'Warning: Could not import stock_pipeline_v2: {e}')
    sp = None


def load_artifacts():
    preproc_path = MODEL_DIR / 'preprocessor_v2.pkl'
    reg_path = MODEL_DIR / 'demand_model_v2.pkl'
    clf_path = MODEL_DIR / 'stock_classifier_v2.pkl'

    missing = [p for p in (preproc_path, reg_path, clf_path) if not p.exists()]
    if missing:
        raise FileNotFoundError(f"Artefacts manquants: {missing}")

    preprocessor = joblib.load(preproc_path)
    reg_model = joblib.load(reg_path)
    clf_model = joblib.load(clf_path)
    return preprocessor, reg_model, clf_model


try:
    PREPROCESSOR, REG_MODEL, CLF_MODEL = load_artifacts()
    print('Loaded models and preprocessor from', MODEL_DIR)
except Exception as e:
    PREPROCESSOR, REG_MODEL, CLF_MODEL = None, None, None
    print('Warning: could not load artifacts at startup:', e)


def load_and_prepare_dataset():
    if not DATA_FILE.exists():
        raise FileNotFoundError(f"Dataset not found: {DATA_FILE}")
    df = pd.read_excel(DATA_FILE)
    if sp is not None:
        df = sp.detect_and_rename_columns(df)
        df = sp.clean_data(df)
        df = sp.extract_time_features(df)
        df = sp.feature_engineering(df)
    return df


@app.route('/health', methods=['GET'])
def health():
    ok = PREPROCESSOR is not None and REG_MODEL is not None and CLF_MODEL is not None
    return jsonify({'status': 'ok' if ok else 'missing_artifacts'})


@app.route('/predict', methods=['POST'])
def predict():
    if PREPROCESSOR is None or REG_MODEL is None or CLF_MODEL is None:
        return jsonify({'error': 'Model artifacts not loaded on server'}), 500

    if sp is None:
        return jsonify({'error': 'stock_pipeline_v2 module not available'}), 500

    payload = request.get_json(force=True)
    product_name = payload.get('product_name') if payload else None
    if not product_name:
        return jsonify({'error': 'Please provide product_name in JSON body'}), 400

    try:
        df = load_and_prepare_dataset()
    except Exception as e:
        import traceback
        error_msg = f'Could not load dataset: {str(e)}\n{traceback.format_exc()}'
        print(f"ERROR in /predict (load_dataset): {error_msg}")
        return jsonify({'error': f'Could not load dataset: {e}'}), 500

    try:
        result = sp.predict_for_product(product_name, df, PREPROCESSOR, REG_MODEL, CLF_MODEL)
        return jsonify(result)
    except Exception as e:
        import traceback
        error_msg = f'Prediction error: {str(e)}\n{traceback.format_exc()}'
        print(f"ERROR in /predict: {error_msg}")
        return jsonify({'error': f'Prediction error: {e}'}), 500


@app.route('/predict_features', methods=['POST'])
def predict_features():
    if PREPROCESSOR is None or REG_MODEL is None or CLF_MODEL is None:
        return jsonify({'error': 'Model artifacts not loaded on server'}), 500

    payload = request.get_json(force=True)
    print(f"DEBUG: Received payload: {payload}")
    features = payload.get('features') if payload else None
    print(f"DEBUG: Extracted features: {features}")
    
    if not features or not isinstance(features, dict):
        return jsonify({'error': 'Please provide a features object in JSON body'}), 400
    
    try:
        import traceback
        # Build a full-row DataFrame filling missing columns with sensible defaults
        def build_sample(features_dict):
            # Extract expected numeric and categorical columns from preprocessor
            num_cols = []
            cat_cols = []
            try:
                # Try to get column names from the preprocessor
                if hasattr(PREPROCESSOR, 'transformers_'):
                    # ColumnTransformer structure: transformers_ is a list of tuples
                    # Each tuple is (name, transformer, columns)
                    for transformer_tuple in PREPROCESSOR.transformers_:
                        if len(transformer_tuple) >= 3:
                            cols = transformer_tuple[2]
                            if isinstance(cols, (list, tuple)):
                                # Check if it's numeric or categorical based on transformer type
                                transformer_name = transformer_tuple[0].lower()
                                if 'numeric' in transformer_name or 'num' in transformer_name:
                                    num_cols.extend(list(cols))
                                elif 'categorical' in transformer_name or 'cat' in transformer_name:
                                    cat_cols.extend(list(cols))
                                else:
                                    # Default: try to infer from data type
                                    num_cols.extend(list(cols))
            except Exception as e:
                print(f"Warning: Could not extract columns from preprocessor: {e}")
                # Fallback: use keys from provided features
                num_cols = [k for k, v in features_dict.items() if isinstance(v, (int, float))]
                cat_cols = [k for k, v in features_dict.items() if isinstance(v, str)]

            # If still empty, try to infer from dataset
            if not num_cols and not cat_cols:
                try:
                    df = load_and_prepare_dataset()
                    num_cols = df.select_dtypes(include=[np.number]).columns.tolist()
                    cat_cols = df.select_dtypes(include=['object', 'category']).columns.tolist()
                except:
                    pass

            sample = {}

            # Handle dates: if created_at provided, expand into year/month/day/weekday/season
            if 'created_at' in features_dict:
                try:
                    dt = pd.to_datetime(features_dict['created_at'])
                    features_dict['created_year'] = int(dt.year)
                    features_dict['created_month'] = int(dt.month)
                    features_dict['created_day'] = int(dt.day)
                    features_dict['created_weekday'] = int(dt.dayofweek)
                    features_dict['created_season'] = int(dt.month % 12 // 3 + 1)
                except Exception:
                    pass

            # Fill numeric columns
            for c in num_cols:
                if c in features_dict and features_dict[c] is not None:
                    sample[c] = features_dict[c]
                else:
                    # special mapping for movement_type -> movement_type_num
                    if c == 'movement_type_num' and 'movement_type' in features_dict:
                        mt = str(features_dict.get('movement_type', '')).strip().upper()
                        mapping = {'IN': 1, 'OUT': -1, 'ADJUSTMENT': 0, 'SALE': -1, 'RESTOCK': 1}
                        sample[c] = mapping.get(mt, 0)
                    else:
                        sample[c] = 0

            # Fill categorical columns
            for c in cat_cols:
                if c in features_dict and features_dict[c] is not None:
                    sample[c] = features_dict[c]
                else:
                    sample[c] = '__MISSING__'

            # Ensure 'name' exists to keep consistent shape if used by downstream logic
            if 'name' not in sample:
                sample['name'] = features_dict.get('name', '__UNKNOWN__')

            return pd.DataFrame([sample])

        sample = build_sample(features)
        print(f"DEBUG: Built sample DataFrame:\n{sample}")
        print(f"DEBUG: Sample columns: {sample.columns.tolist()}")
        
        # Ensure columns exist in correct order for preprocessor
        # Get the expected column order from the dataset
        try:
            df = load_and_prepare_dataset()
            expected_cols = df.columns.tolist()
            print(f"DEBUG: Expected columns from dataset: {expected_cols}")
            
            # Map backend column names to dataset column names
            column_mapping = {
                'quantity_stock': 'quantity',
                'low_stock_threshold': 'low_stock_threshold',
                'expiration_date': 'expiration_date',
                'created_at': 'created_at',
                'name': 'name',
                'sku': 'sku',
                'price': 'price'
            }
            
            # Rename columns if needed
            sample_renamed = sample.rename(columns=column_mapping)
            
            # Reorder sample columns to match expected order and fill missing columns
            sample = sample_renamed.reindex(columns=expected_cols, fill_value=0)
            print(f"DEBUG: Reordered sample:\n{sample}")
        except Exception as e:
            print(f"DEBUG: Warning - could not load dataset for column matching: {e}")
            # If we can't load dataset, try to transform as-is
            pass
        
        print(f"DEBUG: Attempting to transform sample with preprocessor...")
        X = PREPROCESSOR.transform(sample)
        print(f"DEBUG: Transformation successful, shape: {X.shape}")
        pred_d = float(REG_MODEL.predict(X)[0])
        pred_l = bool(CLF_MODEL.predict(X)[0])
        confidence = None
        if hasattr(CLF_MODEL, 'predict_proba'):
            confidence = float(CLF_MODEL.predict_proba(X)[0].max())
        return jsonify({'predicted_demand': pred_d, 'is_low_stock': pred_l, 'confidence': confidence, 'used_sample': sample.to_dict(orient='records')[0]})
    except Exception as e:
        import traceback
        error_msg = f'Prediction features error: {str(e)}\n{traceback.format_exc()}'
        print(f"ERROR in /predict_features: {error_msg}")
        app.logger.error(f"ERROR in /predict_features: {error_msg}")
        return jsonify({'error': f'Prediction features error: {e}', 'traceback': traceback.format_exc()}), 500


if __name__ == '__main__':
    # Enable debug mode and logging
    app.config['DEBUG'] = True
    app.logger.setLevel(logging.DEBUG)
    
    # If artifacts weren't loaded at import, try loading once before running
    if PREPROCESSOR is None or REG_MODEL is None or CLF_MODEL is None:
        try:
            PREPROCESSOR, REG_MODEL, CLF_MODEL = load_artifacts()
            print('Successfully loaded artifacts before starting server')
        except Exception as e:
            print('Failed to load artifacts before starting server:', e)
            import traceback
            traceback.print_exc()
    
    print(f"Starting Flask server on 0.0.0.0:8000")
    print(f"Models loaded: PREPROCESSOR={PREPROCESSOR is not None}, REG_MODEL={REG_MODEL is not None}, CLF_MODEL={CLF_MODEL is not None}")
    app.run(host='0.0.0.0', port=8000, debug=True)
