# SmartMediShop - Angular Template

A comprehensive Angular template for a medical management system built for the Alpha Coding team.

## 🏥 Project Overview

SmartMediShop is a medical management system designed to handle various aspects of healthcare operations including:

- **Cart Management** - Shopping cart for medical products
- **User Management** - Staff and user administration
- **Nursing Services** - Medical service scheduling and management
- **Payment Management** - Payment processing and tracking
- **Client Management** - Patient/client information management
- **Stock Management** - Medical inventory and supplies tracking

## 🚀 Quick Start

### Prerequisites

- Node.js (v18 or higher)
- npm or yarn
- Angular CLI (v17 or higher)

### Installation

1. **Clone the repository**
   ```bash
   git clone <repository-url>
   cd smartmedishop
   ```

2. **Install dependencies**
   ```bash
   npm install
   ```

3. **Start the development server**
   ```bash
   npm start
   ```

4. **Open your browser**
   Navigate to `http://localhost:4200`

## 📁 Project Structure

```
src/
├── app/
│   ├── core/                    # Core functionality
│   │   └── services/            # Core services (Auth, API, Notifications)
│   ├── shared/                  # Shared components and utilities
│   │   ├── components/         # Reusable components
│   │   ├── pipes/              # Custom pipes
│   │   └── directives/         # Custom directives
│   ├── features/               # Feature modules
│   │   ├── dashboard/          # Dashboard module
│   │   ├── cart/               # Shopping cart module
│   │   ├── users/              # User management module
│   │   ├── services/           # Nursing services module
│   │   ├── payments/           # Payment management module
│   │   ├── clients/            # Client management module
│   │   └── stock/              # Stock management module
│   ├── app.component.*         # Root component
│   ├── app.module.ts           # Root module
│   └── app-routing.module.ts   # Main routing
├── assets/                     # Static assets
├── environments/               # Environment configurations
└── styles.scss                # Global styles
```

## 🛠️ Available Scripts

- `npm start` - Start development server
- `npm run build` - Build for production
- `npm run test` - Run unit tests
- `npm run lint` - Run linting
- `npm run e2e` - Run end-to-end tests

## 🎨 UI Components

The template includes:

- **Angular Material** - Modern UI components
- **Responsive Design** - Mobile-first approach
- **Custom Theme** - Medical-themed color scheme
- **Data Tables** - Sortable, filterable data tables
- **Loading Spinners** - User feedback components
- **Confirmation Dialogs** - User interaction dialogs

## 🔧 Configuration

### Environment Variables

Update `src/environments/environment.ts` for development:
```typescript
export const environment = {
  production: false,
  apiUrl: 'http://localhost:8080/api',
  appName: 'SmartMediShop',
  version: '1.0.0'
};
```

### API Integration

The template includes:
- `ApiService` - Generic HTTP service
- `AuthService` - Authentication management
- `NotificationService` - User notifications

## 👥 Team Collaboration

### Git Workflow

1. **Create feature branches**
   ```bash
   git checkout -b feature/cart-management
   ```

2. **Commit changes**
   ```bash
   git add .
   git commit -m "Add cart management functionality"
   ```

3. **Push and create PR**
   ```bash
   git push origin feature/cart-management
   ```

### Code Standards

- Use TypeScript strict mode
- Follow Angular style guide
- Write unit tests for components
- Use meaningful commit messages

## 📋 Module Assignments

Based on your team structure:

- **Frikha Slim** - Cart Management
- **Dhia Bellakoud** - User Management  
- **Saidi Ilyess** - Nursing Services Management
- **Seif eddine rguez** - Payment Management
- **Sourour Noumri** - Client Management
- **Charf Eddine Hasni** - Stock Management

## 🔗 Backend Integration

The template is ready for Spring Boot backend integration:

- RESTful API endpoints
- JWT authentication
- CORS configuration
- Error handling

## 📱 Features

### Dashboard
- Statistics overview
- Quick access to modules
- Real-time data display

### Cart Management
- Add/remove products
- Quantity management
- Price calculation
- Checkout process

### User Management
- CRUD operations
- Role-based access
- User authentication
- Profile management

### Services Management
- Service scheduling
- Availability tracking
- Pricing management
- Category organization

### Payment Management
- Payment processing
- Transaction history
- Payment methods
- Status tracking

### Client Management
- Patient records
- Medical history
- Contact information
- Appointment scheduling

### Stock Management
- Inventory tracking
- Low stock alerts
- Supplier management
- Expiry date monitoring

## 🚀 Deployment

### Production Build

```bash
npm run build
```

### Docker Support

```dockerfile
FROM node:18-alpine
WORKDIR /app
COPY package*.json ./
RUN npm install
COPY . .
RUN npm run build
EXPOSE 4200
CMD ["npm", "start"]
```

## 🤝 Contributing

1. Fork the repository
2. Create your feature branch
3. Commit your changes
4. Push to the branch
5. Create a Pull Request

## 📄 License

This project is licensed under the MIT License.

## 📞 Support

For questions and support, contact the Alpha Coding team.

---

**Happy Coding! 🎉**
