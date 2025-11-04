"""
Code to add to Jupyter notebook for exporting data to Excel
Add this as a new cell at the end of your notebook
"""

# Export all data to Excel
import pandas as pd
import numpy as np
from datetime import datetime

def export_to_excel(output_file='smart_medishop_data_export.xlsx'):
    """Export all generated data to Excel"""
    print(f"\n🚀 Exporting SmartMediShop Data to Excel: {output_file}")
    print("=" * 60)
    
    excel_data = {}
    
    # 1. Raw Transaction Data
    if 'transaction_data' in globals():
        print("📊 Exporting Raw Transaction Data...")
        raw_data = transaction_data.copy()
        if 'timestamp' in raw_data.columns:
            raw_data['timestamp'] = pd.to_datetime(raw_data['timestamp']).dt.strftime('%Y-%m-%d %H:%M:%S')
        excel_data['Raw_Transactions'] = raw_data
        
        # Summary Statistics
        summary = pd.DataFrame({
            'Metric': [
                'Total Transactions', 'Normal', 'Fraud', 'Fraud Rate (%)',
                'Avg Amount ($)', 'Min Amount ($)', 'Max Amount ($)',
                'Avg User Age', 'Avg 24h Txns', 'Avg 7d Txns'
            ],
            'Value': [
                len(raw_data),
                len(raw_data[raw_data['is_fraud'] == 0]),
                len(raw_data[raw_data['is_fraud'] == 1]),
                f"{raw_data['is_fraud'].mean() * 100:.2f}%",
                f"${raw_data['amount'].mean():.2f}",
                f"${raw_data['amount'].min():.2f}",
                f"${raw_data['amount'].max():.2f}",
                f"{raw_data['user_age'].mean():.1f}",
                f"{raw_data['transaction_count_24h'].mean():.2f}",
                f"{raw_data['transaction_count_7d'].mean():.2f}"
            ]
        })
        excel_data['Summary_Statistics'] = summary
    
    # 2. Feature-Engineered Data
    if 'df_features' in globals():
        print("🔧 Exporting Feature-Engineered Data...")
        features_data = df_features.copy()
        if 'is_fraud' not in features_data.columns and 'transaction_data' in globals():
            features_data['is_fraud'] = transaction_data['is_fraud'].values
        excel_data['Feature_Engineered'] = features_data
        
        # Feature Info
        feature_info = pd.DataFrame({
            'Feature': features_data.columns.tolist(),
            'Type': features_data.dtypes.astype(str).tolist(),
            'Non_Null_Count': features_data.count().tolist()
        })
        numeric_features = features_data.select_dtypes(include=[np.number])
        feature_info['Mean'] = numeric_features.mean().tolist() + [None] * (len(features_data.columns) - len(numeric_features.columns))
        feature_info['Std'] = numeric_features.std().tolist() + [None] * (len(features_data.columns) - len(numeric_features.columns))
        excel_data['Feature_Descriptions'] = feature_info
    
    # 3. Model Predictions
    if 'fraud_detector' in globals() and 'df_features' in globals() and 'transaction_data' in globals():
        print("🤖 Generating Model Predictions...")
        X = df_features.drop('is_fraud', axis=1, errors='ignore')
        y = transaction_data['is_fraud']
        
        X_scaled = fraud_detector.scaler.transform(X)
        
        if_predictions = fraud_detector.isolation_forest.predict(X_scaled)
        if_scores = fraud_detector.isolation_forest.decision_function(X_scaled)
        rf_predictions = fraud_detector.random_forest.predict(X_scaled)
        rf_probabilities = fraud_detector.random_forest.predict_proba(X_scaled)[:, 1]
        
        predictions_df = pd.DataFrame({
            'transaction_id': transaction_data.get('transaction_id', range(len(transaction_data))),
            'amount': transaction_data['amount'].values,
            'actual_fraud': y.values,
            'if_anomaly': (if_predictions == -1).astype(int),
            'if_score': if_scores,
            'rf_prediction': rf_predictions,
            'rf_probability': rf_probabilities
        })
        excel_data['Model_Predictions'] = predictions_df
        
        # Model Performance
        from sklearn.metrics import classification_report, confusion_matrix
        rf_report = classification_report(y, rf_predictions, output_dict=True, zero_division=0)
        rf_cm = confusion_matrix(y, rf_predictions)
        
        performance = pd.DataFrame({
            'Metric': ['Accuracy', 'Precision', 'Recall', 'F1_Score'],
            'Value': [
                rf_report['accuracy'],
                rf_report['1']['precision'],
                rf_report['1']['recall'],
                rf_report['1']['f1-score']
            ]
        })
        excel_data['Model_Performance'] = performance
    
    # 4. Payment Method Analysis
    if 'transaction_data' in globals():
        print("💳 Exporting Payment Method Analysis...")
        payment_df = pd.DataFrame({
            'Payment_Method': transaction_data['payment_method'].value_counts().index,
            'Count': transaction_data['payment_method'].value_counts().values,
            'Percentage': (transaction_data['payment_method'].value_counts().values / len(transaction_data) * 100).round(2),
            'Fraud_Count': transaction_data.groupby('payment_method')['is_fraud'].sum().values,
            'Fraud_Rate_%': (transaction_data.groupby('payment_method')['is_fraud'].mean() * 100).round(2).values
        })
        excel_data['Payment_Method_Analysis'] = payment_df
    
    # 5. Country Analysis
    if 'transaction_data' in globals():
        print("🌍 Exporting Country Analysis...")
        country_df = pd.DataFrame({
            'Country': transaction_data['location_country'].value_counts().index,
            'Count': transaction_data['location_country'].value_counts().values,
            'Percentage': (transaction_data['location_country'].value_counts().values / len(transaction_data) * 100).round(2),
            'Fraud_Count': transaction_data.groupby('location_country')['is_fraud'].sum().values,
            'Fraud_Rate_%': (transaction_data.groupby('location_country')['is_fraud'].mean() * 100).round(2).values
        })
        excel_data['Country_Analysis'] = country_df
    
    # 6. Device Type Analysis
    if 'transaction_data' in globals():
        print("📱 Exporting Device Type Analysis...")
        device_df = pd.DataFrame({
            'Device_Type': transaction_data['device_type'].value_counts().index,
            'Count': transaction_data['device_type'].value_counts().values,
            'Percentage': (transaction_data['device_type'].value_counts().values / len(transaction_data) * 100).round(2),
            'Fraud_Count': transaction_data.groupby('device_type')['is_fraud'].sum().values,
            'Fraud_Rate_%': (transaction_data.groupby('device_type')['is_fraud'].mean() * 100).round(2).values
        })
        excel_data['Device_Type_Analysis'] = device_df
    
    # 7. Feature Importance
    if 'fraud_detector' in globals() and hasattr(fraud_detector, 'random_forest'):
        print("📈 Exporting Feature Importance...")
        rf_model = fraud_detector.random_forest
        if hasattr(rf_model, 'feature_importances_'):
            feature_names = [col for col in df_features.columns if col != 'is_fraud']
            importance_df = pd.DataFrame({
                'Feature': feature_names[:len(rf_model.feature_importances_)],
                'Importance': rf_model.feature_importances_,
                'Rank': range(1, len(rf_model.feature_importances_) + 1)
            }).sort_values('Importance', ascending=False).reset_index(drop=True)
            importance_df['Rank'] = range(1, len(importance_df) + 1)
            excel_data['Feature_Importance'] = importance_df
    
    # Write to Excel
    if excel_data:
        print(f"\n💾 Writing to Excel: {output_file}")
        with pd.ExcelWriter(output_file, engine='openpyxl') as writer:
            for sheet_name, df in excel_data.items():
                df.to_excel(writer, sheet_name=sheet_name, index=False)
                print(f"   ✅ {sheet_name}: {len(df)} rows")
        
        print(f"\n✅ Export completed! File: {output_file}")
        print(f"📊 Sheets created: {len(excel_data)}")
        return True
    else:
        print("❌ No data found to export!")
        return False

# Run export
timestamp = datetime.now().strftime('%Y%m%d_%H%M%S')
output_file = f'smart_medishop_data_export_{timestamp}.xlsx'
export_to_excel(output_file)

