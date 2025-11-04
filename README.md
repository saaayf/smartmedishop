# SmartMediShop - AI-Powered Fraud Detection System

A comprehensive e-commerce fraud detection system combining Machine Learning models with rule-based logic for enhanced security and explainability.

## 🚀 Project Overview

SmartMediShop is a full-stack application featuring:
- **Frontend**: Angular-based user interface
- **Backend**: Spring Boot REST API
- **AI Engine**: Python Flask API with ML models (Isolation Forest & Random Forest)
- **Database**: MySQL

## 📁 Project Structure

```
SmartMediShop/
├── angular/              # Angular frontend application
├── backend/              # Spring Boot backend API
├── models/               # Trained ML models (pickle files)
├── smart_medishop_export/ # Model export with metadata
├── simple_api.py         # Python Flask API for fraud detection
├── smart_medishop_fraud_detection.ipynb  # Jupyter notebook for ML model training
└── requirements.txt      # Python dependencies
```

## 🛠️ Technology Stack

### Frontend
- **Angular** 15+
- **Angular Material** for UI components
- **RxJS** for reactive programming
- **TypeScript**

### Backend
- **Spring Boot** 3.2.0
- **Spring Security** for authentication
- **Spring Data JPA** for database operations
- **MySQL** database
- **JWT** for token-based authentication

### AI/ML Engine
- **Python** 3.11+
- **Flask** REST API
- **Scikit-learn** for ML models:
  - Isolation Forest (anomaly detection)
  - Random Forest Classifier (fraud prediction)
- **Pandas** & **NumPy** for data processing
- **Joblib** for model persistence

## 🔧 Setup Instructions

### Prerequisites
- Node.js 18+ and npm
- Java 17+
- Maven 3.6+
- Python 3.11+
- MySQL 8.0+
- XAMPP (for MySQL server)

### 1. Clone the Repository
```bash
git clone https://github.com/saaayf/smartmedishop.git
cd smartmedishop
```

### 2. Backend Setup

```bash
cd backend

# Install dependencies (Maven will handle this automatically)
mvn clean install

# Update application.properties with your database credentials
# Edit: src/main/resources/application.properties

# Run the backend
mvn spring-boot:run
# Or use the provided batch file (Windows)
start.bat
```

The backend will run on `http://localhost:8080`

### 3. Python AI API Setup

```bash
# Install Python dependencies
pip install -r requirements.txt

# Run the Flask API
python simple_api.py
```

The AI API will run on `http://localhost:5000`

### 4. Frontend Setup

```bash
cd angular

# Install dependencies
npm install

# Run development server
ng serve
# Or
npm start
```

The frontend will run on `http://localhost:4200`

## 🧠 Machine Learning Models

### Models Included
- **Isolation Forest**: Detects anomalous transaction patterns
- **Random Forest Classifier**: Predicts fraud probability
- **Feature Engineer**: Custom preprocessing pipeline
- **Standard Scaler**: Normalizes features for model input

### Model Training
The ML models are trained using the Jupyter notebook `smart_medishop_fraud_detection.ipynb`. The notebook includes:
- Data generation and preprocessing
- Feature engineering
- Model training and evaluation
- Model export and persistence

## 🔐 Authentication & Authorization

### User Roles
- **CUSTOMER**: Regular users making transactions
- **ADMIN**: Full system access
- **FRAUD_ANALYST**: Access to detailed fraud analysis and explanations
- **SUPPLIER**: Product suppliers
- **NURSE**: Healthcare providers
- **DELIVERY_MAN**: Delivery personnel
- **TECHNICAL_SUPPORT**: Support staff

### API Endpoints
- `POST /api/auth/register` - User registration
- `POST /api/auth/login` - User login
- `GET /api/auth/me` - Get current user
- `POST /api/transactions` - Create transaction (with fraud detection)
- `GET /api/transactions/{id}` - Get transaction details (with ML/rule explanations for fraud analysts)

## 📊 Fraud Detection Features

### Hybrid Detection System
- **70% ML-based scoring**: Uses Isolation Forest and Random Forest predictions
- **30% Rule-based scoring**: Applies business rules (amount thresholds, time patterns, etc.)

### Explanations for Fraud Analysts
- **ML Explanation**: Detailed breakdown of ML model predictions
  - Isolation Forest anomaly score and interpretation
  - Random Forest probability and feature importance
- **Rule Explanation**: Rule-based triggers and reasons
- **Final Conclusion**: Combined analysis summary

### Risk Profiles
- **LOW**: Minimal risk transactions
- **MEDIUM**: Moderate risk requiring review
- **HIGH**: High risk transactions
- **CRITICAL**: Critical risk, immediate action required

## 📈 Dashboard Features

- Real-time statistics:
  - Total Users
  - Total Orders/Transactions
  - Total Revenue (TND currency)
  - Active Users
  - High Risk Users (HIGH + CRITICAL)
  - Fraud History Count

## 🧪 Testing

### Backend Tests
```bash
cd backend
mvn test
```

### Frontend Tests
```bash
cd angular
npm test
```

## 📝 API Documentation

### Fraud Detection Endpoint
**POST** `/analyze` (Python Flask API)

**Request Body:**
```json
{
  "amount": 150.50,
  "payment_method": "CREDIT_CARD",
  "device_type": "MOBILE",
  "location_country": "TN",
  "user_id": 1,
  "transaction_time": "2025-11-02T14:30:00"
}
```

**Response:**
```json
{
  "fraud_score": 0.75,
  "is_fraud": true,
  "risk_level": "HIGH",
  "ml_explanation": {...},
  "rule_explanation": {...},
  "conclusion": {...},
  "reasons": [...]
}
```

## 🚀 Deployment

### Backend Deployment
```bash
cd backend
mvn clean package
java -jar target/smart-medishop-backend-1.0.0.jar
```

### Frontend Deployment
```bash
cd angular
ng build --prod
# Deploy dist/ folder to your web server
```

### Python API Deployment
```bash
# Using Gunicorn (recommended for production)
pip install gunicorn
gunicorn -w 4 -b 0.0.0.0:5000 simple_api:app
```

## 🤝 Contributing

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/AmazingFeature`)
3. Commit your changes (`git commit -m 'Add some AmazingFeature'`)
4. Push to the branch (`git push origin feature/AmazingFeature`)
5. Open a Pull Request

## 📄 License

This project is licensed under the MIT License.

## 👥 Authors

- **Sayf Eddine** - [GitHub](https://github.com/saaayf)

## 🙏 Acknowledgments

- Spring Boot community
- Angular team
- Scikit-learn developers
- All open-source contributors

