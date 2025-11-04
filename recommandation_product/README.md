# Product Recommendation System

This module provides a product recommendation system based on user purchase history and product characteristics.

## Setup

1. Install dependencies:
```bash
pip install -r requirements.txt
```

2. Train the model:
```bash
python train_model.py
```

This will:
- Load the `Raw_Transactions.csv` dataset
- Train a KNN model for product recommendations
- Save the trained models to the `models/` directory

3. Start the Flask server:
```bash
python flask_api.py
```

The server will run on `http://localhost:8001`

## API Endpoints

### Health Check
```
GET /health
```
Returns the status of the models.

### General Recommendation
```
POST /recommend
Content-Type: application/json

{
  "state": "London",
  "type": "Pansement médical",
  "price": 999,
  "top_n": 10,
  "max_per_type": 3
}
```

### User-Specific Recommendation
```
POST /recommend_user
Content-Type: application/json

{
  "username": "Man",
  "top_n": 10,
  "max_per_type": 3
}
```

## Example Usage

### Python
```python
import requests

# General recommendation
response = requests.post('http://localhost:8001/recommend', json={
    "state": "Angleterre",
    "type": "Pansement médical",
    "price": 999,
    "top_n": 10
})
print(response.json())

# User-specific recommendation
response = requests.post('http://localhost:8001/recommend_user', json={
    "username": "Man",
    "top_n": 10
})
print(response.json())
```

### cURL (Linux/Mac/Git Bash)
```bash
# Health check
curl http://localhost:8001/health

# General recommendation
curl -X POST http://localhost:8001/recommend \
  -H "Content-Type: application/json" \
  -d '{"state": "Angleterre", "type": "Pansement médical", "price": 999}'

# User recommendation
curl -X POST http://localhost:8001/recommend_user \
  -H "Content-Type: application/json" \
  -d '{"username": "Man", "top_n": 10}'
```

### PowerShell (Windows)
```powershell
# Health check
Invoke-RestMethod -Uri "http://localhost:8001/health" -Method Get

# General recommendation
$body = @{
    state = "Angleterre"
    type = "Pansement médical"
    price = 999
    top_n = 10
} | ConvertTo-Json
Invoke-RestMethod -Uri "http://localhost:8001/recommend" -Method Post -Body $body -ContentType "application/json"

# User recommendation
$body = @{
    username = "Man"
    top_n = 10
} | ConvertTo-Json
Invoke-RestMethod -Uri "http://localhost:8001/recommend_user" -Method Post -Body $body -ContentType "application/json"

# Or use the test script:
.\test_api.ps1
```

## Models

The trained models are saved in the `models/` directory:
- `recommendation_model.pkl` - KNN classifier
- `onehot_encoder.pkl` - OneHot encoder for categorical features
- `price_scaler.pkl` - MinMax scaler for price
- `dataset.pkl` - Full dataset for reference

