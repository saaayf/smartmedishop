#!/usr/bin/env python3
"""
Simple SmartMediShop Flask API
AI-Powered Fraud Detection System for Payment Management
"""

from flask import Flask, request, jsonify
from flask_cors import CORS
import pandas as pd
import numpy as np
import sqlite3
import json
from datetime import datetime
import os
import sys
import joblib
import pickle
from pathlib import Path
from sklearn.preprocessing import StandardScaler, LabelEncoder


# Initialize Flask app
app = Flask(__name__)
CORS(app)

# Feature Engineer class (needed to unpickle the saved model)
class SmartMediShopFeatureEngineer:
    """
    Advanced feature engineering for SmartMediShop fraud detection
    """
    
    def __init__(self):
        self.scaler = StandardScaler()
        self.label_encoders = {}
        
    def create_time_features(self, df):
        """Create time-based features"""
        # Basic time features
        df['is_weekend'] = df['day_of_week'].isin([5, 6]).astype(int)
        df['is_night'] = df['hour'].isin([22, 23, 0, 1, 2, 3, 4, 5]).astype(int)
        df['is_business_hours'] = df['hour'].between(9, 17).astype(int)
        df['is_morning'] = df['hour'].between(6, 12).astype(int)
        df['is_evening'] = df['hour'].between(18, 22).astype(int)
        return df
    
    def create_amount_features(self, df):
        """Create amount-based features"""
        # Amount transformations
        df['amount_log'] = np.log1p(df['amount'])
        df['amount_sqrt'] = np.sqrt(df['amount'])
        df['amount_percentile'] = df['amount'].rank(pct=True)
        
        # Amount categories
        df['amount_category'] = pd.cut(df['amount'], 
                                     bins=[0, 50, 200, 1000, float('inf')], 
                                     labels=['low', 'medium', 'high', 'very_high'])
        
        # Amount vs average
        if 'avg_transaction_amount' in df.columns:
            df['amount_vs_avg'] = df['amount'] / df['avg_transaction_amount'].replace(0, 1)
            df['amount_deviation'] = abs(df['amount'] - df['avg_transaction_amount']) / df['avg_transaction_amount'].replace(0, 1)
        else:
            df['amount_vs_avg'] = 0
            df['amount_deviation'] = 0
        
        return df
    
    def create_frequency_features(self, df):
        """Create frequency-based features"""
        if 'transaction_count_24h' in df.columns and 'transaction_count_7d' in df.columns:
            df['transactions_per_hour'] = df['transaction_count_24h'] / 24
            df['transactions_per_day'] = df['transaction_count_7d'] / 7
            df['high_frequency'] = (df['transaction_count_24h'] > 10).astype(int)
            df['very_high_frequency'] = (df['transaction_count_24h'] > 20).astype(int)
            df['frequency_ratio_24h_7d'] = df['transaction_count_24h'] / (df['transaction_count_7d'] + 1)
        else:
            df['transactions_per_hour'] = 0
            df['transactions_per_day'] = 0
            df['high_frequency'] = 0
            df['very_high_frequency'] = 0
            df['frequency_ratio_24h_7d'] = 0
        return df
    
    def create_user_features(self, df):
        """Create user behavior features"""
        if 'user_registration_days' in df.columns:
            df['new_user'] = (df['user_registration_days'] < 30).astype(int)
            df['recent_user'] = (df['user_registration_days'].between(30, 90)).astype(int)
            df['established_user'] = (df['user_registration_days'] > 90).astype(int)
        else:
            df['new_user'] = 0
            df['recent_user'] = 0
            df['established_user'] = 0
        return df
    
    def create_risk_features(self, df):
        """Create risk scoring features"""
        # Age groups
        if 'user_age' in df.columns:
            df['age_group'] = pd.cut(df['user_age'], 
                                   bins=[0, 18, 25, 35, 50, 100], 
                                   labels=['teen', 'young', 'adult', 'middle', 'senior'])
        else:
            df['age_group'] = 'adult'
        
        # Risk score calculation
        risk_score = 0
        if 'is_night' in df.columns:
            risk_score += df['is_night'] * 0.3
        if 'high_frequency' in df.columns:
            risk_score += df['high_frequency'] * 0.4
        if 'new_user' in df.columns:
            risk_score += df['new_user'] * 0.2
        if 'amount_vs_avg' in df.columns:
            risk_score += (df['amount_vs_avg'] > 3).astype(int) * 0.3
        if 'amount_deviation' in df.columns:
            risk_score += (df['amount_deviation'] > 2).astype(int) * 0.2
        
        df['risk_score'] = risk_score
        
        # Risk categories
        df['risk_category'] = pd.cut(df['risk_score'], 
                                   bins=[0, 0.2, 0.5, 0.8, 1.0], 
                                   labels=['low', 'medium', 'high', 'critical'])
        return df
    
    def encode_categorical_features(self, df):
        """Encode categorical features with handling for unseen labels"""
        categorical_features = ['payment_method', 'device_type', 'location_country', 
                               'amount_category', 'age_group', 'risk_category']
        
        for feature in categorical_features:
            if feature in df.columns:
                if feature not in self.label_encoders:
                    # First time encoding - fit the encoder
                    self.label_encoders[feature] = LabelEncoder()
                    df[f'{feature}_encoded'] = self.label_encoders[feature].fit_transform(df[feature].astype(str))
                else:
                    # Already fitted - handle unseen labels
                    def safe_transform(x, encoder, feature_name):
                        """Safely transform label, handling unseen values"""
                        x_str = str(x)
                        if x_str in encoder.classes_:
                            # Label exists - encode normally
                            return encoder.transform([x_str])[0]
                        else:
                            # Unseen label - assign to max encoded value + 1
                            # This creates a new category for unknown values
                            max_encoded = len(encoder.classes_) - 1
                            print(f"⚠️  Unseen label '{x_str}' in feature '{feature_name}'. Assigning encoded value: {max_encoded + 1}")
                            return max_encoded + 1
                    
                    df[f'{feature}_encoded'] = df[feature].astype(str).apply(
                        lambda x: safe_transform(x, self.label_encoders[feature], feature)
                    )
        
        return df
    
    def prepare_features(self, df):
        """Prepare all features for model training"""
        # Apply all feature engineering steps
        df = self.create_time_features(df)
        df = self.create_amount_features(df)
        df = self.create_frequency_features(df)
        df = self.create_user_features(df)
        df = self.create_risk_features(df)
        df = self.encode_categorical_features(df)
        
        # Select features for training
        feature_columns = [
            # Original features
            'amount', 'hour', 'day_of_week', 'user_age',
            'transaction_count_24h', 'transaction_count_7d',
            'avg_transaction_amount', 'user_registration_days',
            
            # Time features
            'is_weekend', 'is_night', 'is_business_hours', 'is_morning', 'is_evening',
            
            # Amount features
            'amount_log', 'amount_sqrt', 'amount_percentile',
            'amount_vs_avg', 'amount_deviation',
            
            # Frequency features
            'transactions_per_hour', 'transactions_per_day',
            'high_frequency', 'very_high_frequency', 'frequency_ratio_24h_7d',
            
            # User features
            'new_user', 'recent_user', 'established_user',
            
            # Risk features
            'risk_score',
            
            # Encoded categorical features
            'payment_method_encoded', 'device_type_encoded', 'location_country_encoded',
            'amount_category_encoded', 'age_group_encoded', 'risk_category_encoded'
        ]
        
        # Filter existing columns
        existing_features = [col for col in feature_columns if col in df.columns]
        
        return df[existing_features] if existing_features else df

# Register the feature engineer class for pickle compatibility
# This allows pickle to find the class even if it was saved in a different module
import __main__
__main__.SmartMediShopFeatureEngineer = SmartMediShopFeatureEngineer

# Hybrid fraud detection: ML Models + Rule-Based
class HybridFraudDetector:
    """Hybrid fraud detection combining ML models with rule-based logic"""
    
    def __init__(self):
        # Initialize ML models (optional - falls back to rules if not available)
        self.ml_models_available = False
        self.isolation_forest = None
        self.random_forest = None
        self.scaler = None
        self.feature_engineer = None
        
        # Try to load ML models
        try:
            model_dir = Path('smart_medishop_export')
            if not model_dir.exists():
                model_dir = Path('models')
            
            if model_dir.exists():
                print(f"🤖 Attempting to load ML models from: {model_dir}")
                
                # Load models
                isolation_path = model_dir / 'isolation_forest.pkl'
                random_forest_path = model_dir / 'random_forest.pkl'
                scaler_path = model_dir / 'scaler.pkl'
                feature_engineer_path = model_dir / 'feature_engineer.pkl'
                
                # Alternative names (if models/ directory)
                if not isolation_path.exists():
                    isolation_path = model_dir / 'smart_medishop_isolation_forest.pkl'
                if not random_forest_path.exists():
                    random_forest_path = model_dir / 'smart_medishop_random_forest.pkl'
                if not scaler_path.exists():
                    scaler_path = model_dir / 'smart_medishop_scaler.pkl'
                if not feature_engineer_path.exists():
                    feature_engineer_path = model_dir / 'smart_medishop_feature_engineer.pkl'
                
                if all([p.exists() for p in [isolation_path, random_forest_path, scaler_path, feature_engineer_path]]):
                    print(f"✅ Loading ML models...")
                    self.isolation_forest = joblib.load(isolation_path)
                    self.random_forest = joblib.load(random_forest_path)
                    self.scaler = joblib.load(scaler_path)
                    self.feature_engineer = joblib.load(feature_engineer_path)
                    self.ml_models_available = True
                    print(f"✅ ML models loaded successfully!")
                    print(f"   - Isolation Forest: Ready")
                    print(f"   - Random Forest: Ready")
                    print(f"   - Feature Engineer: Ready")
                    print(f"   - Scaler: Ready")
                else:
                    print(f"⚠️ ML model files not found, using rule-based detection only")
                    missing = [str(p) for p in [isolation_path, random_forest_path, scaler_path, feature_engineer_path] if not p.exists()]
                    print(f"   Missing files: {missing}")
            else:
                print(f"⚠️ Model directory not found, using rule-based detection only")
        except Exception as e:
            print(f"⚠️ Error loading ML models: {e}")
            print(f"   Falling back to rule-based detection only")
        
        if not self.ml_models_available:
            print(f"📋 Using rule-based fraud detection only")
    
    def _get_ml_predictions(self, transaction_data):
        """Get predictions from ML models"""
        try:
            if not self.ml_models_available:
                return None
            
            # Prepare DataFrame for feature engineering
            df = pd.DataFrame([transaction_data])
            
            # Apply feature engineering (creates 33 features)
            df_features = self.feature_engineer.prepare_features(df)
            
            # Scale features
            df_scaled = self.scaler.transform(df_features)
            
            # Get predictions from both models
            if_prediction = self.isolation_forest.predict(df_scaled)[0]
            if_score = self.isolation_forest.decision_function(df_scaled)[0]
            rf_prediction = self.random_forest.predict(df_scaled)[0]
            rf_probability = self.random_forest.predict_proba(df_scaled)[0][1]
            
            # Normalize Isolation Forest score (-1 to 1) → (0 to 1)
            # Lower IF score = more anomalous = higher fraud risk
            # Convert: -1 (most anomalous) → 1.0, +1 (normal) → 0.0
            if_normalized = (1 - if_score) / 2 if if_score <= 0 else max(0, (1 - if_score) / 2)
            if_normalized = max(0, min(1, if_normalized))  # Clamp to [0, 1]
            
            # Combine ML scores (weighted average)
            # Isolation Forest: 40%, Random Forest: 60%
            ml_score = (if_normalized * 0.4) + (rf_probability * 0.6)
            
            print(f"\n🤖 ML Models Analysis:")
            print(f"   - Isolation Forest: {if_prediction} (score: {if_score:.3f} → normalized: {if_normalized:.3f})")
            print(f"   - Random Forest: {rf_prediction} (probability: {rf_probability:.3f})")
            print(f"   - Combined ML Score: {ml_score:.3f}")
            
            return {
                'ml_score': ml_score,
                'isolation_forest': {
                    'prediction': int(if_prediction),
                    'score': float(if_score),
                    'normalized': float(if_normalized)
                },
                'random_forest': {
                    'prediction': int(rf_prediction),
                    'probability': float(rf_probability)
                }
            }
        except Exception as e:
            print(f"⚠️ Error in ML prediction: {e}")
            return None
    
    def _get_if_interpretation(self, prediction, normalized_score):
        """Get interpretation for Isolation Forest results"""
        if prediction == -1:
            if normalized_score > 0.7:
                return f"Strong anomaly detected (score: {normalized_score:.2f}). The transaction exhibits highly unusual patterns compared to normal behavior."
            elif normalized_score > 0.5:
                return f"Moderate anomaly detected (score: {normalized_score:.2f}). The transaction shows some unusual characteristics."
            else:
                return f"Minor anomaly detected (score: {normalized_score:.2f}). The transaction has slight deviations from normal patterns."
        else:
            return f"No anomaly detected (score: {normalized_score:.2f}). The transaction appears normal based on historical patterns."
    
    def _get_rf_interpretation(self, probability):
        """Get interpretation for Random Forest results"""
        if probability > 0.7:
            return f"High fraud probability ({probability:.1%}). The model is confident this transaction is fraudulent based on learned patterns."
        elif probability > 0.5:
            return f"Moderate fraud probability ({probability:.1%}). The model suggests this transaction may be fraudulent."
        elif probability > 0.3:
            return f"Low fraud probability ({probability:.1%}). The model indicates some risk but not strongly confident."
        else:
            return f"Very low fraud probability ({probability:.1%}). The model indicates this transaction is likely legitimate."
    
    def _get_ml_interpretation(self, ml_score):
        """Get interpretation for combined ML score"""
        if ml_score > 0.7:
            return f"ML models indicate high fraud risk (score: {ml_score:.2f}). Both anomaly detection and classification models are raising concerns."
        elif ml_score > 0.5:
            return f"ML models indicate moderate fraud risk (score: {ml_score:.2f}). At least one model has detected suspicious patterns."
        elif ml_score > 0.3:
            return f"ML models indicate low fraud risk (score: {ml_score:.2f}). Some minor concerns detected but not strongly indicative of fraud."
        else:
            return f"ML models indicate very low fraud risk (score: {ml_score:.2f}). Models suggest the transaction appears legitimate."
    
    def _get_rule_interpretation(self, rule_score, reason_count):
        """Get interpretation for rule-based score"""
        if rule_score > 0.7:
            return f"Rule-based system detected high fraud risk (score: {rule_score:.2f}) with {reason_count} risk factors flagged. Multiple business rules have been triggered indicating suspicious activity."
        elif rule_score > 0.5:
            return f"Rule-based system detected moderate fraud risk (score: {rule_score:.2f}) with {reason_count} risk factors. Several business rules indicate potential issues."
        elif rule_score > 0.3:
            return f"Rule-based system detected low fraud risk (score: {rule_score:.2f}) with {reason_count} risk factors. Some rules have been triggered but risk is manageable."
        else:
            return f"Rule-based system indicates low fraud risk (score: {rule_score:.2f}) with {reason_count} risk factors. Transaction appears to meet normal business criteria."
    
    def _get_conclusion_explanation(self, final_score, risk_level, is_fraud, ml_score, rule_score):
        """Get explanation for final conclusion"""
        if is_fraud:
            if ml_score > 0 and rule_score > 0:
                return f"This transaction has been flagged as fraudulent (score: {final_score:.2f}). The hybrid approach combining ML models (contributing {ml_score * 0.7:.2f}) and rule-based detection (contributing {rule_score * 0.3:.2f}) indicates a {risk_level} risk level. Both systems agree this transaction requires investigation."
            else:
                return f"This transaction has been flagged as fraudulent (score: {final_score:.2f}) with a {risk_level} risk level. The rule-based system has detected multiple risk factors."
        else:
            if ml_score > 0 and rule_score > 0:
                return f"This transaction appears legitimate (score: {final_score:.2f}). The hybrid analysis shows {risk_level} risk with ML models contributing {ml_score * 0.7:.2f} and rules contributing {rule_score * 0.3:.2f}. No significant fraud indicators detected."
            else:
                return f"This transaction appears legitimate (score: {final_score:.2f}) with {risk_level} risk. Rule-based analysis found minimal risk factors."
    
    def _get_rule_based_score(self, transaction_data):
        """Get fraud score from rule-based detection"""
        # Extract transaction features
        amount = transaction_data.get('amount', 0)
        hour = transaction_data.get('hour', 12)
        user_age = transaction_data.get('user_age', 30)
        payment_method = transaction_data.get('payment_method', 'credit_card')
        transaction_count_24h = transaction_data.get('transaction_count_24h', 1)
        user_registration_days = transaction_data.get('user_account_age_days', 
                                                      transaction_data.get('user_registration_days', 30))
        device_type = transaction_data.get('device_type', 'desktop')
        location_country = transaction_data.get('location_country', 'US')
        
        # Extract user-specific features
        user_average_amount = transaction_data.get('user_average_amount', 0)
        user_max_amount = transaction_data.get('user_max_transaction_amount', 0)
        user_total_transactions = transaction_data.get('user_total_transactions', 0)
        user_fraud_count = transaction_data.get('user_fraud_count', 0)
        user_risk_profile = transaction_data.get('user_risk_profile', 'LOW')
        
        # Initialize fraud detection
        rule_score = 0.0
        reasons = []
        
        # USER-SPECIFIC AMOUNT ANALYSIS
        if user_average_amount > 0:  # User has transaction history
            amount_deviation = abs(amount - user_average_amount) / user_average_amount
            if amount_deviation > 3.0:
                rule_score += 0.4
                reasons.append(f"Amount {amount_deviation:.1f}x higher than user's typical ${user_average_amount:.2f}")
            elif amount_deviation > 2.0:
                rule_score += 0.3
                reasons.append(f"Amount {amount_deviation:.1f}x higher than user's typical ${user_average_amount:.2f}")
            elif amount_deviation > 1.5:
                rule_score += 0.2
                reasons.append(f"Amount {amount_deviation:.1f}x higher than user's typical ${user_average_amount:.2f}")
            
            if user_max_amount > 0 and amount > user_max_amount * 1.5:
                rule_score += 0.3
                reasons.append(f"Amount exceeds user's historical maximum by 50%")
        else:  # New user
            if amount > 1000:
                rule_score += 0.2
                reasons.append('High amount for new user')
            if amount > 5000:
                rule_score += 0.3
                reasons.append('Very high amount for new user')
        
        # USER-SPECIFIC RISK PROFILE ANALYSIS
        if user_risk_profile == 'HIGH':
            rule_score += 0.2
            reasons.append('User has high risk profile')
        elif user_risk_profile == 'CRITICAL':
            rule_score += 0.3
            reasons.append('User has critical risk profile')
        
        # USER FRAUD HISTORY ANALYSIS
        if user_fraud_count > 0:
            rule_score += 0.1 * user_fraud_count
            reasons.append(f'User has {user_fraud_count} previous fraud incidents')
        
        # USER EXPERIENCE ANALYSIS
        if user_total_transactions < 5:
            rule_score += 0.2
            reasons.append('User has limited transaction history')
        elif user_total_transactions < 10:
            rule_score += 0.1
            reasons.append('User is relatively new')
        
        # Time-based rules
        if hour in [0, 1, 2, 3, 4, 5, 22, 23]:
            rule_score += 0.2
            reasons.append('Transaction made during unusual hours')
        
        # Frequency-based rules
        if transaction_count_24h > 10:
            rule_score += 0.3
            reasons.append('High transaction frequency detected')
        if transaction_count_24h > 20:
            rule_score += 0.2
            reasons.append('Very high transaction frequency')
        
        # User registration analysis
        if user_registration_days < 1:
            rule_score += 0.3
            reasons.append('Brand new user')
        elif user_registration_days < 7:
            rule_score += 0.2
            reasons.append('New user with limited history')
        
        # Device-based rules
        if device_type == 'mobile' and amount > 500:
            rule_score += 0.1
            reasons.append('Large mobile transaction')
        
        # Location-based rules
        if location_country in ['FR', 'DE'] and amount > 200:
            rule_score += 0.1
            reasons.append('International transaction')
        
        # Age-based rules
        if user_age is not None and user_age < 18:
            rule_score += 0.2
            reasons.append(f'Underage user (age: {user_age} years)')
        
        # Normalize rule score to [0, 1] range
        rule_score = min(rule_score, 1.0)
        
        return {
            'rule_score': rule_score,
            'reasons': reasons
        }
    
    def analyze_transaction(self, transaction_data):
        """Analyze transaction using hybrid approach: ML + Rule-Based"""
        try:
            # Extract basic info for logging
            amount = transaction_data.get('amount', 0)
            user_total_transactions = transaction_data.get('user_total_transactions', 0)
            user_average_amount = transaction_data.get('user_average_amount', 0)
            
            # DEBUG: Log received values
            print(f"\n🔍 === TRANSACTION ANALYSIS ===")
            print(f"   - amount: {amount}")
            print(f"   - user_total_transactions: {user_total_transactions}")
            print(f"   - user_average_amount: {user_average_amount}")
            
            # STEP 1: Get ML predictions (if available)
            ml_result = self._get_ml_predictions(transaction_data)
            
            # STEP 2: Get rule-based score
            rule_result = self._get_rule_based_score(transaction_data)
            rule_score = rule_result['rule_score']
            reasons = rule_result['reasons']
            
            print(f"\n📋 Rule-Based Analysis:")
            print(f"   - Rule Score: {rule_score:.3f}")
            print(f"   - Reasons: {len(reasons)} flags")
            
            # STEP 3: Combine scores (Hybrid Approach)
            if ml_result and self.ml_models_available:
                # ML models available - use hybrid approach
                ml_score = ml_result['ml_score']
                
                # Weighted combination: 70% ML, 30% Rules
                ML_WEIGHT = 0.7
                RULE_WEIGHT = 0.3
                final_score = (ml_score * ML_WEIGHT) + (rule_score * RULE_WEIGHT)
                
                # Add ML-specific reasons
                # Check Isolation Forest anomaly detection (-1 means anomaly detected)
                if ml_result['isolation_forest']['prediction'] == -1:
                    reasons.append('Anomaly detection flagged unusual behavior')
                
                # Check Random Forest high probability
                if ml_result['random_forest']['probability'] > 0.5:
                    reasons.append(f"Classification model indicates fraud probability ({ml_result['random_forest']['probability']:.1%})")
                
                # Check combined ML score (lower threshold for suspicious patterns)
                if ml_score > 0.4:
                    reasons.append(f'ML models detected suspicious pattern (score: {ml_score:.2f})')
                
                print(f"\n🔗 Hybrid Analysis (ML + Rules):")
                print(f"   - ML Score (70%): {ml_score:.3f}")
                print(f"   - Rule Score (30%): {rule_score:.3f}")
                print(f"   - Final Score: {final_score:.3f}")
            else:
                # ML models not available - use rules only
                final_score = rule_score
                print(f"\n📋 Rule-Based Only:")
                print(f"   - Final Score: {final_score:.3f}")
            
            # Determine risk level
            if final_score >= 0.7:
                risk_level = 'CRITICAL'
                is_fraud = True
            elif final_score >= 0.5:
                risk_level = 'HIGH'
                is_fraud = True
            elif final_score >= 0.3:
                risk_level = 'MEDIUM'
                is_fraud = False
            else:
                risk_level = 'LOW'
                is_fraud = False
            
            result = {
                'success': True,
                'is_fraud': is_fraud,
                'fraud_score': final_score,
                'risk_level': risk_level,
                'reasons': reasons,
                'confidence': min(final_score, 1.0),
                'method': 'hybrid' if (ml_result and self.ml_models_available) else 'rule-based'
            }
            
            # Add detailed explanations for analysts
            if ml_result and self.ml_models_available:
                # ML Models Explanation
                ml_explanation = {
                    'isolation_forest': {
                        'prediction': 'Anomaly Detected' if ml_result['isolation_forest']['prediction'] == -1 else 'Normal',
                        'score': ml_result['isolation_forest']['score'],
                        'normalized_score': ml_result['isolation_forest']['normalized'],
                        'interpretation': self._get_if_interpretation(ml_result['isolation_forest']['prediction'], 
                                                                      ml_result['isolation_forest']['normalized'])
                    },
                    'random_forest': {
                        'prediction': 'Fraud' if ml_result['random_forest']['prediction'] == 1 else 'Normal',
                        'probability': ml_result['random_forest']['probability'],
                        'interpretation': self._get_rf_interpretation(ml_result['random_forest']['probability'])
                    },
                    'combined_score': ml_result['ml_score'],
                    'weight': 0.7,
                    'contribution': ml_result['ml_score'] * 0.7,
                    'interpretation': self._get_ml_interpretation(ml_result['ml_score'])
                }
                
                # Rule-Based Explanation
                rule_explanation = {
                    'score': rule_score,
                    'weight': 0.3,
                    'contribution': rule_score * 0.3,
                    'reason_count': len(reasons),
                    'interpretation': self._get_rule_interpretation(rule_score, len(reasons))
                }
                
                # Conclusion
                conclusion = {
                    'final_score': final_score,
                    'final_risk_level': risk_level,
                    'is_fraud': is_fraud,
                    'method': 'Hybrid (70% ML + 30% Rules)',
                    'explanation': self._get_conclusion_explanation(final_score, risk_level, is_fraud, 
                                                                   ml_result['ml_score'], rule_score)
                }
                
                result['ml_details'] = {
                    'isolation_forest_score': ml_result['isolation_forest']['score'],
                    'random_forest_probability': ml_result['random_forest']['probability'],
                    'combined_ml_score': ml_result['ml_score']
                }
                result['rule_score'] = rule_score
                result['ml_explanation'] = ml_explanation
                result['rule_explanation'] = rule_explanation
                result['conclusion'] = conclusion
            else:
                # Rule-based only
                rule_explanation = {
                    'score': rule_score,
                    'weight': 1.0,
                    'contribution': rule_score,
                    'reason_count': len(reasons),
                    'interpretation': self._get_rule_interpretation(rule_score, len(reasons))
                }
                
                conclusion = {
                    'final_score': final_score,
                    'final_risk_level': risk_level,
                    'is_fraud': is_fraud,
                    'method': 'Rule-Based Only',
                    'explanation': self._get_conclusion_explanation(final_score, risk_level, is_fraud, 
                                                                   0, rule_score)
                }
                
                result['rule_explanation'] = rule_explanation
                result['conclusion'] = conclusion
            
            return result
            
        except Exception as e:
            return {
                'success': False,
                'error': str(e),
                'is_fraud': False,
                'fraud_score': 0.0,
                'risk_level': 'UNKNOWN'
            }

# Initialize hybrid fraud detector (ML + Rules)
fraud_detector = HybridFraudDetector()

# Database setup
def init_db():
    """Initialize SQLite database for transactions"""
    conn = sqlite3.connect('smart_medishop.db')
    cursor = conn.cursor()
    
    # Transactions table
    cursor.execute('''
        CREATE TABLE IF NOT EXISTS transactions (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            transaction_id TEXT UNIQUE NOT NULL,
            amount REAL NOT NULL,
            hour INTEGER,
            payment_method TEXT,
            device_type TEXT,
            location_country TEXT,
            is_fraud BOOLEAN,
            fraud_score REAL,
            risk_level TEXT,
            reasons TEXT,
            timestamp DATETIME DEFAULT CURRENT_TIMESTAMP,
            status TEXT DEFAULT 'PENDING'
        )
    ''')
    
    # Fraud alerts table
    cursor.execute('''
        CREATE TABLE IF NOT EXISTS fraud_alerts (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            transaction_id TEXT,
            fraud_score REAL,
            risk_level TEXT,
            reasons TEXT,
            status TEXT DEFAULT 'PENDING',
            created_at DATETIME DEFAULT CURRENT_TIMESTAMP
        )
    ''')
    
    conn.commit()
    conn.close()
    print("Database initialized successfully!")

# API Routes
@app.route('/api/health', methods=['GET'])
def health_check():
    """Health check endpoint"""
    return jsonify({
        'status': 'healthy',
        'service': 'SmartMediShop AI Fraud Detection',
        'timestamp': datetime.now().isoformat()
    })

@app.route('/api/analyze-transaction', methods=['POST'])
def analyze_transaction():
    """Analyze transaction for fraud"""
    try:
        data = request.json
        
        # Validate required fields
        if 'amount' not in data:
            return jsonify({
                'success': False,
                'error': 'Missing required field: amount'
            }), 400
        
        # IMPORTANT: Pass ALL fields from request to fraud detector
        # This ensures user-specific fields like user_total_transactions, user_average_amount, etc.
        # are correctly passed through to the fraud detection logic
        transaction_data = {
            'amount': data.get('amount', 0),
            'hour': data.get('hour', datetime.now().hour),
            'day_of_week': data.get('day_of_week', datetime.now().weekday()),
            'month': data.get('month', datetime.now().month),
            'user_age': data.get('user_age', 30),
            'payment_method': data.get('payment_method', 'credit_card'),
            'device_type': data.get('device_type', 'desktop'),
            'location_country': data.get('location_country', 'US'),
            'merchant_name': data.get('merchant_name', ''),
            'transaction_type': data.get('transaction_type', 'purchase'),
            
            # User-specific fields - CRITICAL for accurate fraud detection
            'user_total_transactions': data.get('user_total_transactions', 0),
            'user_average_amount': data.get('user_average_amount', 0),
            'user_max_transaction_amount': data.get('user_max_transaction_amount', 0),
            'user_fraud_count': data.get('user_fraud_count', 0),
            'user_risk_profile': data.get('user_risk_profile', 'LOW'),
            'user_account_age_days': data.get('user_account_age_days', data.get('user_registration_days', 30)),
            'user_id': data.get('user_id', None),
            
            # User behavior fields
            'user_transaction_velocity': data.get('user_transaction_velocity', 0),
            'user_amount_velocity': data.get('user_amount_velocity', 0),
            'user_average_transaction_amount': data.get('user_average_transaction_amount', 0),
            'user_unusual_patterns_count': data.get('user_unusual_patterns_count', 0),
            'user_transaction_frequency_per_day': data.get('user_transaction_frequency_per_day', 0),
            'user_weekend_transaction_ratio': data.get('user_weekend_transaction_ratio', 0),
            'user_night_transaction_ratio': data.get('user_night_transaction_ratio', 0),
            
            # Legacy fields (for backward compatibility)
            'transaction_count_24h': data.get('transaction_count_24h', 1),
            'transaction_count_7d': data.get('transaction_count_7d', 3),
            'avg_transaction_amount': data.get('avg_transaction_amount', data.get('amount', 0)),
            'user_registration_days': data.get('user_registration_days', data.get('user_account_age_days', 30))
        }
        
        # Debug: Log what we received from backend
        print(f"\n📥 Flask API received from backend:")
        print(f"   - user_total_transactions: {transaction_data.get('user_total_transactions')}")
        print(f"   - user_average_amount: {transaction_data.get('user_average_amount')}")
        print(f"   - user_max_transaction_amount: {transaction_data.get('user_max_transaction_amount')}")
        print(f"   - raw data keys: {list(data.keys())}")
        
        # Analyze transaction
        result = fraud_detector.analyze_transaction(transaction_data)
        
        # Generate transaction ID
        transaction_id = f"TXN_{datetime.now().strftime('%Y%m%d_%H%M%S')}_{np.random.randint(1000, 9999)}"
        
        # Save to database
        conn = sqlite3.connect('smart_medishop.db')
        cursor = conn.cursor()
        
        cursor.execute('''
            INSERT INTO transactions (
                transaction_id, amount, hour, payment_method,
                device_type, location_country, is_fraud, fraud_score,
                risk_level, reasons
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        ''', (
            transaction_id, transaction_data['amount'], transaction_data['hour'],
            transaction_data['payment_method'], transaction_data['device_type'],
            transaction_data['location_country'], result['is_fraud'],
            result['fraud_score'], result['risk_level'], json.dumps(result['reasons'])
        ))
        
        # Create fraud alert if needed
        if result['is_fraud'] or result['risk_level'] in ['HIGH', 'CRITICAL']:
            cursor.execute('''
                INSERT INTO fraud_alerts (
                    transaction_id, fraud_score, risk_level, reasons
                ) VALUES (?, ?, ?, ?)
            ''', (
                transaction_id, result['fraud_score'], result['risk_level'],
                json.dumps(result['reasons'])
            ))
        
        conn.commit()
        conn.close()
        
        return jsonify({
            'success': True,
            'transaction_id': transaction_id,
            'analysis': result
        })
        
    except Exception as e:
        return jsonify({
            'success': False,
            'error': str(e)
        }), 500

@app.route('/api/transactions', methods=['GET'])
def get_transactions():
    """Get all transactions"""
    try:
        conn = sqlite3.connect('smart_medishop.db')
        cursor = conn.cursor()
        
        cursor.execute('''
            SELECT transaction_id, amount, hour, payment_method, 
                   is_fraud, fraud_score, risk_level, timestamp
            FROM transactions 
            ORDER BY timestamp DESC 
            LIMIT 100
        ''')
        
        transactions = []
        for row in cursor.fetchall():
            transactions.append({
                'transaction_id': row[0],
                'amount': row[1],
                'hour': row[2],
                'payment_method': row[3],
                'is_fraud': bool(row[4]),
                'fraud_score': row[5],
                'risk_level': row[6],
                'timestamp': row[7]
            })
        
        conn.close()
        
        return jsonify({
            'success': True,
            'transactions': transactions,
            'count': len(transactions)
        })
        
    except Exception as e:
        return jsonify({
            'success': False,
            'error': str(e)
        }), 500

@app.route('/api/fraud-alerts', methods=['GET'])
def get_fraud_alerts():
    """Get all fraud alerts"""
    try:
        conn = sqlite3.connect('smart_medishop.db')
        cursor = conn.cursor()
        
        cursor.execute('''
            SELECT transaction_id, fraud_score, risk_level, reasons, status, created_at
            FROM fraud_alerts 
            ORDER BY created_at DESC
        ''')
        
        alerts = []
        for row in cursor.fetchall():
            alerts.append({
                'transaction_id': row[0],
                'fraud_score': row[1],
                'risk_level': row[2],
                'reasons': json.loads(row[3]) if row[3] else [],
                'status': row[4],
                'created_at': row[5]
            })
        
        conn.close()
        
        return jsonify({
            'success': True,
            'alerts': alerts,
            'count': len(alerts)
        })
        
    except Exception as e:
        return jsonify({
            'success': False,
            'error': str(e)
        }), 500

@app.route('/api/simulate-transaction', methods=['POST'])
def simulate_transaction():
    """Simulate a random transaction for testing"""
    import random
    
    # Generate random transaction data
    transaction_data = {
        'amount': random.uniform(10, 1000),
        'hour': random.randint(0, 23),
        'day_of_week': random.randint(0, 6),
        'user_age': random.randint(18, 65),
        'payment_method': random.choice(['credit_card', 'debit_card', 'paypal', 'bank_transfer']),
        'transaction_count_24h': random.randint(1, 20),
        'transaction_count_7d': random.randint(1, 50),
        'avg_transaction_amount': random.uniform(50, 500),
        'user_registration_days': random.randint(1, 365),
        'device_type': random.choice(['desktop', 'mobile', 'tablet']),
        'location_country': random.choice(['US', 'CA', 'UK', 'DE', 'FR'])
    }
    
    # Analyze the simulated transaction
    result = fraud_detector.analyze_transaction(transaction_data)
    
    return jsonify({
        'success': True,
        'simulated_transaction': transaction_data,
        'analysis': result
    })

if __name__ == '__main__':
    # Initialize database
    init_db()
    
    print("\nStarting SmartMediShop AI Fraud Detection API...")
    print("API Endpoints:")
    print("   GET  /api/health - Health check")
    print("   POST /api/analyze-transaction - Analyze transaction for fraud")
    print("   GET  /api/transactions - Get transaction history")
    print("   GET  /api/fraud-alerts - Get fraud alerts")
    print("   POST /api/simulate-transaction - Simulate random transaction")
    print("\nAPI will be available at: http://localhost:5000")
    print("API Documentation: http://localhost:5000/api/health")
    
    # Run Flask app
    app.run(debug=True, host='0.0.0.0', port=5000)
