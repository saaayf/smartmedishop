
# SmartMediShop Flask API Integration Example
from flask import Flask, request, jsonify
import joblib
import pandas as pd
import numpy as np

app = Flask(__name__)

# Load models
isolation_forest = joblib.load('smart_medishop_export/isolation_forest.pkl')
random_forest = joblib.load('smart_medishop_export/random_forest.pkl')
scaler = joblib.load('smart_medishop_export/scaler.pkl')
feature_engineer = joblib.load('smart_medishop_export/feature_engineer.pkl')

@app.route('/api/analyze-transaction', methods=['POST'])
def analyze_transaction():
    try:
        data = request.json

        # Prepare features
        df = pd.DataFrame([data])
        df_features = feature_engineer.prepare_features(df)
        df_scaled = scaler.transform(df_features)

        # Get predictions
        if_prediction = isolation_forest.predict(df_scaled)[0]
        if_score = isolation_forest.decision_function(df_scaled)[0]
        rf_prediction = random_forest.predict(df_scaled)[0]
        rf_probability = random_forest.predict_proba(df_scaled)[0][1]

        # Combine results
        is_fraud = (if_prediction == -1) or (rf_prediction == 1)
        fraud_score = (if_score + rf_probability) / 2

        # Determine risk level
        if fraud_score < -0.5:
            risk_level = 'CRITICAL'
        elif fraud_score < -0.2:
            risk_level = 'HIGH'
        elif fraud_score < 0.1:
            risk_level = 'MEDIUM'
        else:
            risk_level = 'LOW'

        return jsonify({
            'success': True,
            'is_fraud': bool(is_fraud),
            'fraud_score': float(fraud_score),
            'risk_level': risk_level,
            'isolation_forest_score': float(if_score),
            'random_forest_probability': float(rf_probability)
        })

    except Exception as e:
        return jsonify({'success': False, 'error': str(e)}), 500

if __name__ == '__main__':
    app.run(debug=True, host='0.0.0.0', port=5000)
    