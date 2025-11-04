"""
Quick test script that loads the preprocessor and models and runs a prediction
from a partial feature dict to validate the API logic without running Flask.
"""
import joblib
import pandas as pd
from pathlib import Path

BASE_DIR = Path(__file__).parent
MODEL_DIR = BASE_DIR / 'models_v2'

preprocessor = joblib.load(MODEL_DIR / 'preprocessor_v2.pkl')
reg = joblib.load(MODEL_DIR / 'demand_model_v2.pkl')
clf = joblib.load(MODEL_DIR / 'stock_classifier_v2.pkl')

def build_sample(features_dict):
    num_cols = list(preprocessor.transformers_[0][2])
    cat_cols = list(preprocessor.transformers_[1][2])
    sample = {}
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

    for c in num_cols:
        if c in features_dict and features_dict[c] is not None:
            sample[c] = features_dict[c]
        else:
            if c == 'movement_type_num' and 'movement_type' in features_dict:
                mt = str(features_dict.get('movement_type', '')).strip().upper()
                mapping = {'IN': 1, 'OUT': -1, 'ADJUSTMENT': 0, 'SALE': -1, 'RESTOCK': 1}
                sample[c] = mapping.get(mt, 0)
            else:
                sample[c] = 0

    for c in cat_cols:
        if c in features_dict and features_dict[c] is not None:
            sample[c] = features_dict[c]
        else:
            sample[c] = '__MISSING__'

    if 'name' not in sample:
        sample['name'] = features_dict.get('name', '__UNKNOWN__')

    return pd.DataFrame([sample])


if __name__ == '__main__':
    features = {
        'name': 'Aspirine 500mg',
        'quantity_stock': 10,
        'low_stock_threshold': 5,
        'price': 12.5,
        'movement_type': 'IN',
        'created_at': '2024-01-01',
        'category': 'Pharmacie',
        'supplier_name': 'FournisseurX'
    }

    sample = build_sample(features)
    print('Sample used for prediction:')
    print(sample.to_dict(orient='records')[0])
    X = preprocessor.transform(sample)
    pred_d = float(reg.predict(X)[0])
    pred_l = bool(clf.predict(X)[0])
    conf = float(clf.predict_proba(X)[0].max()) if hasattr(clf, 'predict_proba') else None
    print({'predicted_demand': pred_d, 'is_low_stock': pred_l, 'confidence': conf})
