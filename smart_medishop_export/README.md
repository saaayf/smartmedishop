
# SmartMediShop AI Fraud Detection System

## Overview
This package contains the trained AI models for SmartMediShop fraud detection.

## Files
- `isolation_forest.pkl`: Unsupervised anomaly detection model
- `random_forest.pkl`: Supervised classification model
- `scaler.pkl`: Feature scaler
- `feature_engineer.pkl`: Feature engineering pipeline
- `model_metadata.json`: Model information and performance metrics
- `flask_integration_example.py`: Example Flask API implementation
- `requirements.txt`: Python dependencies

## Usage
1. Install dependencies: `pip install -r requirements.txt`
2. Run Flask example: `python flask_integration_example.py`
3. Test with: `curl -X POST http://localhost:5000/api/analyze-transaction -H "Content-Type: application/json" -d '{"amount": 100, "hour": 14, ...}'`

## Integration with Angular
The Flask API can be integrated with Angular frontend for real-time fraud detection.
    