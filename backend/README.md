# SmartMediShop Backend API

## 🏥💳🤖 SmartMediShop AI Fraud Detection Backend

A comprehensive Spring Boot backend API for the SmartMediShop payment management system with integrated AI fraud detection capabilities.

## 🚀 Features

### 🔐 Authentication & Authorization
- **JWT-based authentication** with secure token management
- **Role-based access control** (Customer, Admin, Fraud Analyst)
- **User registration and login** with validation
- **Password encryption** using BCrypt
- **Session management** with token refresh

### 💳 Transaction Management
- **Real-time transaction processing** with fraud detection
- **Transaction history** and statistics
- **Payment method support** (Credit Card, Bank Transfer, Digital Wallet)
- **Transaction status tracking** (Pending, Completed, Failed, Cancelled)
- **User-specific transaction filtering**

### 🤖 AI Fraud Detection
- **Real-time fraud analysis** using machine learning models
- **Risk scoring** (LOW, MEDIUM, HIGH, CRITICAL)
- **Fraud reason explanations** for transparency
- **User behavior analysis** for personalized fraud detection
- **Automatic fraud alert generation**
- **Integration with Python AI models** via REST API

### 📊 User Management
- **User profiles** with comprehensive data
- **User behavior tracking** for AI features
- **Risk profile management** with dynamic updates
- **User statistics** and transaction analytics
- **Admin user management** capabilities

### 🚨 Fraud Alert System
- **Real-time fraud alerts** with severity levels
- **Alert management** for fraud analysts
- **Investigation workflow** with notes and resolution
- **Fraud statistics** and reporting
- **Suspicious transaction monitoring**

## 🛠️ Technology Stack

- **Spring Boot 3.2.0** - Main framework
- **Spring Security** - Authentication and authorization
- **Spring Data JPA** - Database operations
- **MySQL** - Database (via XAMPP)
- **JWT** - Token-based authentication
- **Maven** - Dependency management
- **Java 17** - Programming language

## 📋 Prerequisites

- **Java 17** or higher
- **Maven 3.6+**
- **XAMPP** with MySQL running
- **Python Flask API** running (for AI models)

## 🚀 Quick Start

### 1. Database Setup
```bash
# Start XAMPP services
# Access phpMyAdmin: http://localhost/phpmyadmin
# Create database: smart_medishop
```

### 2. Configure Application
```properties
# Update application.properties
spring.datasource.url=jdbc:mysql://localhost:3306/smart_medishop
spring.datasource.username=root
spring.datasource.password=your_password
```

### 3. Start the Application
```bash
cd backend
mvn clean install
mvn spring-boot:run
```

### 4. Verify Installation
```bash
# Health check
curl http://localhost:8080/api/health

# Expected response:
{
  "status": "healthy",
  "service": "SmartMediShop Backend API",
  "timestamp": "2024-01-01T00:00:00",
  "version": "1.0.0"
}
```

## 📚 API Endpoints

### 🔐 Authentication
- `POST /api/auth/register` - User registration
- `POST /api/auth/login` - User login
- `POST /api/auth/logout` - User logout
- `GET /api/auth/profile` - Get user profile

### 💳 Transactions
- `POST /api/transactions/create` - Create transaction
- `GET /api/transactions/my-transactions` - Get user transactions
- `GET /api/transactions/{id}` - Get transaction details
- `POST /api/transactions/{id}/process` - Process transaction
- `GET /api/transactions/statistics` - Get transaction statistics

### 🚨 Fraud Detection
- `GET /api/fraud/alerts` - Get fraud alerts (Admin/Fraud Analyst)
- `GET /api/fraud/alerts/{id}` - Get fraud alert details
- `PUT /api/fraud/alerts/{id}/resolve` - Resolve fraud alert
- `GET /api/fraud/suspicious-transactions` - Get suspicious transactions
- `GET /api/fraud/statistics` - Get fraud statistics

### 🏥 Health Check
- `GET /api/health` - Service health status

## 🔧 Configuration

### Database Configuration
```properties
# MySQL Configuration
spring.datasource.url=jdbc:mysql://localhost:3306/smart_medishop
spring.datasource.username=root
spring.datasource.password=
spring.jpa.hibernate.ddl-auto=update
```

### JWT Configuration
```properties
# JWT Settings
jwt.secret=your_secret_key_here
jwt.expiration=86400000
```

### AI Model Configuration
```properties
# AI Model Integration
ai.model.base-url=http://localhost:5000
ai.model.timeout=30000
```

## 🗄️ Database Schema

### Core Tables
- **users** - User accounts and profiles
- **transactions** - Transaction records with fraud detection
- **fraud_alerts** - Fraud alert management
- **user_behavior** - User behavior tracking for AI
- **user_risk_scores** - Risk assessment data
- **user_sessions** - Authentication sessions

### Key Relationships
- User → Transactions (One-to-Many)
- Transaction → Fraud Alerts (One-to-Many)
- User → User Behavior (One-to-One)
- User → Risk Scores (One-to-Many)

## 🔒 Security Features

### Authentication
- **JWT tokens** with configurable expiration
- **Password hashing** using BCrypt
- **Session management** with token refresh
- **CORS configuration** for Angular integration

### Authorization
- **Role-based access control** (RBAC)
- **Endpoint-level security** with method security
- **User context** in all operations
- **Admin and fraud analyst** specific endpoints

### Data Protection
- **Input validation** with Bean Validation
- **SQL injection protection** via JPA
- **XSS protection** with proper encoding
- **CSRF protection** (disabled for API)

## 🤖 AI Integration

### Fraud Detection Flow
1. **Transaction Creation** → User submits transaction
2. **Data Preparation** → Extract features for AI model
3. **AI Analysis** → Call Python Flask API for fraud detection
4. **Risk Assessment** → Determine risk level and fraud reasons
5. **Alert Generation** → Create fraud alerts if needed
6. **User Update** → Update user behavior and risk profile

### AI Features
- **Real-time analysis** during transaction processing
- **User-specific context** for personalized detection
- **Behavioral analysis** using user history
- **Risk scoring** with confidence levels
- **Fraud reason explanations** for transparency

## 📊 Monitoring & Logging

### Health Monitoring
- **Service health** endpoint for monitoring
- **Database connectivity** checks
- **AI model availability** verification
- **Performance metrics** tracking

### Logging
- **Request/Response logging** for debugging
- **Security event logging** for audit
- **Fraud detection logging** for analysis
- **Error logging** with stack traces

## 🚀 Deployment

### Development
```bash
mvn spring-boot:run
```

### Production
```bash
mvn clean package
java -jar target/smart-medishop-backend-1.0.0.jar
```

### Docker (Optional)
```dockerfile
FROM openjdk:17-jdk-slim
COPY target/smart-medishop-backend-1.0.0.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app.jar"]
```

## 🔧 Troubleshooting

### Common Issues
1. **Database Connection** - Check XAMPP MySQL service
2. **AI Model Integration** - Verify Flask API is running
3. **JWT Token Issues** - Check secret key configuration
4. **CORS Errors** - Verify Angular frontend configuration

### Debug Mode
```properties
# Enable debug logging
logging.level.com.smartmedishop=DEBUG
logging.level.org.springframework.security=DEBUG
```

## 📈 Performance

### Optimization
- **Connection pooling** for database operations
- **Caching** for frequently accessed data
- **Async processing** for AI model calls
- **Batch operations** for bulk data processing

### Monitoring
- **Response time** tracking
- **Memory usage** monitoring
- **Database performance** metrics
- **AI model latency** measurement

## 🤝 Contributing

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Add tests
5. Submit a pull request

## 📄 License

This project is licensed under the MIT License - see the LICENSE file for details.

## 🆘 Support

For support and questions:
- Create an issue in the repository
- Contact the development team
- Check the documentation

---

**SmartMediShop Backend API** - Powered by Spring Boot & AI 🤖💳🏥
