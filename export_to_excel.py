#!/usr/bin/env python3
"""
Export SmartMediShop Fraud Detection Data to Excel
Extracts all data generated in the Jupyter notebook to Excel file with multiple sheets
"""

import pandas as pd
import numpy as np
from datetime import datetime
import warnings
warnings.filterwarnings('ignore')

# Import the classes from notebook context
# Note: You may need to run this after executing the notebook cells
# Or import from a separate file if classes are saved

try:
    # Try to load variables from notebook if running in Jupyter
    from IPython import get_ipython
    ipython = get_ipython()
    if ipython is not None:
        # We're in Jupyter - variables should be available
        print("✅ Running in Jupyter notebook environment")
        print("📝 Make sure you've executed all notebook cells before running this!")
except:
    print("⚠️  Running as standalone script")
    print("📝 You may need to regenerate data if variables aren't available")

def export_data_to_excel(output_file='smart_medishop_data_export.xlsx'):
    """
    Export all generated data to Excel file with multiple sheets
    """
    print(f"\n🚀 Starting SmartMediShop Data Export to Excel")
    print("=" * 60)
    
    # Dictionary to store all sheets
    excel_data = {}
    
    try:
        # Sheet 1: Raw Transaction Data
        if 'transaction_data' in globals():
            print("\n📊 Exporting Raw Transaction Data...")
            raw_data = transaction_data.copy()
            # Format timestamp if exists
            if 'timestamp' in raw_data.columns:
                raw_data['timestamp'] = pd.to_datetime(raw_data['timestamp']).dt.strftime('%Y-%m-%d %H:%M:%S')
            excel_data['Raw_Transactions'] = raw_data
            print(f"   ✅ {len(raw_data)} transactions exported")
            
            # Add summary statistics
            summary_stats = pd.DataFrame({
                'Metric': [
                    'Total Transactions',
                    'Normal Transactions',
                    'Fraudulent Transactions',
                    'Fraud Rate (%)',
                    'Average Amount ($)',
                    'Min Amount ($)',
                    'Max Amount ($)',
                    'Average User Age',
                    'Average 24h Transactions',
                    'Average 7d Transactions'
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
            excel_data['Summary_Statistics'] = summary_stats
            print("   ✅ Summary statistics added")
        else:
            print("   ⚠️  'transaction_data' not found. Skipping raw transactions.")
        
        # Sheet 2: Feature-Engineered Data
        if 'df_features' in globals():
            print("\n🔧 Exporting Feature-Engineered Data...")
            features_data = df_features.copy()
            # Add target if not present
            if 'is_fraud' not in features_data.columns and 'transaction_data' in globals():
                features_data['is_fraud'] = transaction_data['is_fraud'].values
            excel_data['Feature_Engineered'] = features_data
            print(f"   ✅ {len(features_data)} rows with {len(features_data.columns)} features exported")
            
            # Feature descriptions
            feature_descriptions = pd.DataFrame({
                'Feature': features_data.columns.tolist(),
                'Type': features_data.dtypes.astype(str).tolist(),
                'Non_Null_Count': features_data.count().tolist(),
                'Mean': features_data.select_dtypes(include=[np.number]).mean().tolist() + [None] * (len(features_data.columns) - len(features_data.select_dtypes(include=[np.number]).columns)),
                'Std': features_data.select_dtypes(include=[np.number]).std().tolist() + [None] * (len(features_data.columns) - len(features_data.select_dtypes(include=[np.number]).columns))
            })
            excel_data['Feature_Descriptions'] = feature_descriptions
            print("   ✅ Feature descriptions added")
        else:
            print("   ⚠️  'df_features' not found. Skipping feature-engineered data.")
        
        # Sheet 3: Model Predictions and Evaluations
        if 'fraud_detector' in globals() and 'transaction_data' in globals():
            print("\n🤖 Generating Model Predictions...")
            
            # Prepare features
            X = df_features.drop('is_fraud', axis=1, errors='ignore') if 'df_features' in globals() else None
            y = transaction_data['is_fraud'] if 'transaction_data' in globals() else None
            
            if X is not None and y is not None:
                # Get predictions
                from sklearn.preprocessing import StandardScaler
                
                # Scale features
                scaler = fraud_detector.scaler if hasattr(fraud_detector, 'scaler') else StandardScaler().fit(X)
                X_scaled = scaler.transform(X)
                
                # Isolation Forest predictions
                if_estimator = fraud_detector.isolation_forest if hasattr(fraud_detector, 'isolation_forest') else None
                if if_estimator is not None:
                    if_predictions = if_estimator.predict(X_scaled)
                    if_scores = if_estimator.decision_function(X_scaled)
                    
                    # Random Forest predictions
                    rf_estimator = fraud_detector.random_forest if hasattr(fraud_detector, 'random_forest') else None
                    if rf_estimator is not None:
                        rf_predictions = rf_estimator.predict(X_scaled)
                        rf_probabilities = rf_estimator.predict_proba(X_scaled)[:, 1]
                        
                        # Create predictions dataframe
                        predictions_df = pd.DataFrame({
                            'transaction_id': transaction_data.get('transaction_id', range(len(transaction_data))),
                            'actual_fraud': y.values,
                            'isolation_forest_prediction': if_predictions,
                            'isolation_forest_score': if_scores,
                            'random_forest_prediction': rf_predictions,
                            'random_forest_probability': rf_probabilities,
                            'amount': transaction_data['amount'].values if 'amount' in transaction_data.columns else None
                        })
                        
                        # Convert IF prediction (-1 = anomaly, 1 = normal) to 0/1
                        predictions_df['if_anomaly'] = (predictions_df['isolation_forest_prediction'] == -1).astype(int)
                        
                        excel_data['Model_Predictions'] = predictions_df
                        print(f"   ✅ {len(predictions_df)} predictions exported")
                        
                        # Model Performance Summary
                        from sklearn.metrics import classification_report, confusion_matrix
                        
                        # RF Performance
                        rf_report = classification_report(y, rf_predictions, output_dict=True, zero_division=0)
                        rf_cm = confusion_matrix(y, rf_predictions)
                        
                        performance_summary = pd.DataFrame({
                            'Model': ['Random Forest'],
                            'Accuracy': [rf_report['accuracy']],
                            'Precision': [rf_report['1']['precision']],
                            'Recall': [rf_report['1']['recall']],
                            'F1_Score': [rf_report['1']['f1-score']],
                            'True_Negatives': [rf_cm[0, 0]],
                            'False_Positives': [rf_cm[0, 1]],
                            'False_Negatives': [rf_cm[1, 0]],
                            'True_Positives': [rf_cm[1, 1]]
                        })
                        
                        excel_data['Model_Performance'] = performance_summary
                        print("   ✅ Model performance metrics added")
        else:
            print("   ⚠️  'fraud_detector' not found. Skipping model predictions.")
        
        # Sheet 4: Payment Method Distribution
        if 'transaction_data' in globals():
            print("\n💳 Exporting Payment Method Analysis...")
            payment_analysis = pd.DataFrame({
                'Payment_Method': transaction_data['payment_method'].value_counts().index,
                'Count': transaction_data['payment_method'].value_counts().values,
                'Percentage': (transaction_data['payment_method'].value_counts().values / len(transaction_data) * 100).round(2),
                'Fraud_Count': transaction_data.groupby('payment_method')['is_fraud'].sum().values,
                'Fraud_Rate': (transaction_data.groupby('payment_method')['is_fraud'].mean() * 100).round(2).values
            })
            excel_data['Payment_Method_Analysis'] = payment_analysis
            print("   ✅ Payment method analysis exported")
        
        # Sheet 5: Country Distribution
        if 'transaction_data' in globals():
            print("\n🌍 Exporting Country Analysis...")
            country_analysis = pd.DataFrame({
                'Country': transaction_data['location_country'].value_counts().index,
                'Count': transaction_data['location_country'].value_counts().values,
                'Percentage': (transaction_data['location_country'].value_counts().values / len(transaction_data) * 100).round(2),
                'Fraud_Count': transaction_data.groupby('location_country')['is_fraud'].sum().values,
                'Fraud_Rate': (transaction_data.groupby('location_country')['is_fraud'].mean() * 100).round(2).values
            })
            excel_data['Country_Analysis'] = country_analysis
            print("   ✅ Country analysis exported")
        
        # Sheet 6: Device Type Analysis
        if 'transaction_data' in globals():
            print("\n📱 Exporting Device Type Analysis...")
            device_analysis = pd.DataFrame({
                'Device_Type': transaction_data['device_type'].value_counts().index,
                'Count': transaction_data['device_type'].value_counts().values,
                'Percentage': (transaction_data['device_type'].value_counts().values / len(transaction_data) * 100).round(2),
                'Fraud_Count': transaction_data.groupby('device_type')['is_fraud'].sum().values,
                'Fraud_Rate': (transaction_data.groupby('device_type')['is_fraud'].mean() * 100).round(2).values
            })
            excel_data['Device_Type_Analysis'] = device_analysis
            print("   ✅ Device type analysis exported")
        
        # Sheet 7: Feature Importance (if available)
        if 'fraud_detector' in globals() and hasattr(fraud_detector, 'random_forest'):
            print("\n📈 Exporting Feature Importance...")
            rf_model = fraud_detector.random_forest
            if hasattr(rf_model, 'feature_importances_'):
                feature_names = df_features.columns.tolist() if 'df_features' in globals() else []
                if 'is_fraud' in feature_names:
                    feature_names.remove('is_fraud')
                
                importance_df = pd.DataFrame({
                    'Feature': feature_names[:len(rf_model.feature_importances_)],
                    'Importance': rf_model.feature_importances_,
                    'Rank': range(1, len(rf_model.feature_importances_) + 1)
                }).sort_values('Importance', ascending=False).reset_index(drop=True)
                
                importance_df['Rank'] = range(1, len(importance_df) + 1)
                excel_data['Feature_Importance'] = importance_df
                print("   ✅ Feature importance exported")
        
        # Write to Excel
        if excel_data:
            print(f"\n💾 Writing to Excel file: {output_file}")
            with pd.ExcelWriter(output_file, engine='openpyxl') as writer:
                for sheet_name, df in excel_data.items():
                    df.to_excel(writer, sheet_name=sheet_name, index=False)
                    print(f"   ✅ Sheet '{sheet_name}' written ({len(df)} rows)")
            
            print(f"\n✅ Export completed successfully!")
            print(f"📁 File saved: {output_file}")
            print(f"📊 Total sheets: {len(excel_data)}")
            print("=" * 60)
            return True
        else:
            print("\n❌ No data found to export!")
            print("⚠️  Make sure you've executed all notebook cells before running this script.")
            return False
            
    except Exception as e:
        print(f"\n❌ Error during export: {str(e)}")
        import traceback
        traceback.print_exc()
        return False

if __name__ == "__main__":
    # Run export
    timestamp = datetime.now().strftime('%Y%m%d_%H%M%S')
    output_file = f'smart_medishop_data_export_{timestamp}.xlsx'
    
    print("\n" + "=" * 60)
    print("📊 SmartMediShop Data Export to Excel")
    print("=" * 60)
    print("\n📝 Instructions:")
    print("   1. Open your Jupyter notebook")
    print("   2. Execute all cells to generate data")
    print("   3. Run this script in a new cell:")
    print("      !python export_to_excel.py")
    print("   OR import and run in notebook:")
    print("      from export_to_excel import export_data_to_excel")
    print("      export_data_to_excel('my_data.xlsx')")
    print("\n" + "=" * 60)
    
    # Try to export
    success = export_data_to_excel(output_file)
    
    if not success:
        print("\n💡 Alternative: Add this code to your notebook:")
        print("""
# In a new notebook cell:
from export_to_excel import export_data_to_excel
export_data_to_excel('smart_medishop_data_export.xlsx')
        """)

