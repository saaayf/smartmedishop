"""
Train recommendation model from the notebook
"""
import pandas as pd
import numpy as np
from sklearn.preprocessing import OneHotEncoder, MinMaxScaler
from sklearn.neighbors import KNeighborsClassifier
import joblib
from pathlib import Path

# Load dataset
print("Loading dataset...")
data = pd.read_csv('Raw_Transactions.csv')
print(f"Dataset loaded: {len(data)} rows")
print(f"Columns: {data.columns.tolist()}")

# Variables explicatives
X = data[['State', 'Type', 'Price']].copy()

# Variable cible
y = data['Marque'].copy()

# Encoder les variables catégorielles (State et Type)
print("Encoding categorical variables...")
ohe = OneHotEncoder(sparse_output=False)
X_cat = ohe.fit_transform(X[['State', 'Type']])

# Normaliser Price
print("Scaling price...")
scaler = MinMaxScaler()
X_price = scaler.fit_transform(X[['Price']])

# Combiner features
X_final = np.hstack([X_cat, X_price])
print(f"Shape des features finales : {X_final.shape}")

# Train KNN model
print("Training KNN model...")
model = KNeighborsClassifier(n_neighbors=5, weights='distance')
model.fit(X_final, y)
print("Model trained successfully!")

# Save models and preprocessors
models_dir = Path('models')
models_dir.mkdir(exist_ok=True)

print("Saving models...")
joblib.dump(model, models_dir / 'recommendation_model.pkl')
joblib.dump(ohe, models_dir / 'onehot_encoder.pkl')
joblib.dump(scaler, models_dir / 'price_scaler.pkl')
joblib.dump(data, models_dir / 'dataset.pkl')  # Save dataset for reference

print("All models saved successfully!")
print(f"Models saved in: {models_dir.absolute()}")

