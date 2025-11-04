# 🎯 Commandes de Démarrage Rapide

## 🚀 Démarrage du Projet

### 1️⃣ Démarrer le Backend (Terminal 1)
```bash
cd backend
./mvnw spring-boot:run
```
**Ou sur Windows:**
```powershell
cd backend
mvnw.cmd spring-boot:run
```

✅ Backend disponible sur: **http://localhost:8080**
✅ API Stock: **http://localhost:8080/api/stock**

---

### 2️⃣ Démarrer le Frontend (Terminal 2)
```bash
cd angular
npm install    # Première fois seulement
npm start
```

✅ Frontend disponible sur: **http://localhost:4200**

---

## 🧪 Tester l'Intégration

### Étape 1: Créer un compte Admin
1. Ouvrir http://localhost:4200
2. Aller sur "Register"
3. Créer un compte
4. **Important:** Modifier le rôle en base de données:

```sql
-- Connectez-vous à votre base de données et exécutez:
UPDATE users SET user_type = 'ADMIN' WHERE username = 'votre_username';
```

### Étape 2: Tester les Produits
1. Se connecter avec le compte Admin
2. Cliquer sur "Stock" → "Ajouter un produit"
3. Créer un produit de test:
   - **SKU:** `TEST001`
   - **Nom:** `Produit Test`
   - **Description:** `Test d'intégration`
   - **Quantité:** `50`
   - **Seuil:** `10`
   - **Prix:** `15.99`
   - **Expiration:** `2025-12-31`
4. Cliquer sur "Créer"

### Étape 3: Tester les Mouvements
1. Aller sur "Stock" → "Enregistrer un mouvement"
2. Sélectionner le produit créé
3. Choisir "Sortie (OUT)"
4. Quantité: `5`
5. Raison: "Vente"
6. Enregistrer

### Étape 4: Vérifier
1. Retourner sur "Stock" → "Produits"
2. Cliquer sur le produit
3. Aller dans l'onglet "Historique des mouvements"
4. ✅ Le mouvement doit apparaître
5. ✅ La quantité doit être 45 (50 - 5)

---

## 🔍 Endpoints API à Tester (avec Postman/Insomnia)

### Login et obtenir le token
```http
POST http://localhost:8080/api/auth/login
Content-Type: application/json

{
  "username": "votre_username",
  "password": "votre_password"
}
```

**Réponse:**
```json
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "username": "votre_username",
  "email": "email@example.com",
  "userType": "ADMIN",
  "userId": 1
}
```

### Créer un produit
```http
POST http://localhost:8080/api/stock/products
Authorization: Bearer {votre_token}
Content-Type: application/json

{
  "sku": "PARACETAMOL001",
  "name": "Paracétamol 500mg",
  "description": "Boîte de 16 comprimés",
  "quantity": 100,
  "lowStockThreshold": 20,
  "price": 5.99,
  "expirationDate": "2025-12-31"
}
```

### Récupérer tous les produits
```http
GET http://localhost:8080/api/stock/products
Authorization: Bearer {votre_token}
```

### Récupérer un produit spécifique
```http
GET http://localhost:8080/api/stock/products/1
Authorization: Bearer {votre_token}
```

### Modifier un produit
```http
PUT http://localhost:8080/api/stock/products/1
Authorization: Bearer {votre_token}
Content-Type: application/json

{
  "name": "Paracétamol 500mg (Modifié)",
  "quantity": 150,
  "lowStockThreshold": 25,
  "price": 6.50
}
```

### Enregistrer un mouvement
```http
POST http://localhost:8080/api/stock/movements
Authorization: Bearer {votre_token}
Content-Type: application/json

{
  "productId": 1,
  "movementType": "IN",
  "quantity": 50,
  "reason": "PURCHASE"
}
```

### Récupérer l'historique des mouvements
```http
GET http://localhost:8080/api/stock/movements/product/1
Authorization: Bearer {votre_token}
```

### Récupérer les alertes d'un produit
```http
GET http://localhost:8080/api/stock/alerts/product/1
Authorization: Bearer {votre_token}
```

---

## 🔧 Commandes de Build

### Build de développement
```bash
cd angular
npm run build
```

### Build de production
```bash
cd angular
npm run build -- --configuration production
```

### Tests unitaires
```bash
cd angular
npm test
```

### Linting
```bash
cd angular
npm run lint
```

---

## 📦 Structure des Fichiers Créés

```
angular/
├── src/app/
│   ├── models/
│   │   ├── product.model.ts ✅
│   │   ├── stock-movement.model.ts ✅
│   │   └── stock-alert.model.ts ✅
│   ├── core/services/
│   │   ├── stock.service.ts ✅
│   │   └── auth.service.ts (modifié) ✅
│   └── features/stock/
│       ├── stock.module.ts ✅
│       ├── stock-routing.module.ts ✅
│       ├── product-list/
│       │   ├── product-list.component.ts ✅
│       │   ├── product-list.component.html ✅
│       │   └── product-list.component.scss ✅
│       ├── product-detail/
│       │   ├── product-detail.component.ts ✅
│       │   ├── product-detail.component.html ✅
│       │   └── product-detail.component.scss ✅
│       ├── product-form/
│       │   ├── product-form.component.ts ✅
│       │   ├── product-form.component.html ✅
│       │   └── product-form.component.scss ✅
│       └── movement-form/
│           ├── movement-form.component.ts ✅
│           ├── movement-form.component.html ✅
│           └── movement-form.component.scss ✅
├── STOCK_MODULE_README.md ✅
├── INTEGRATION_COMPLETE.md ✅
└── CART_INTEGRATION_EXAMPLE.txt ✅
```

---

## 🎯 Vérifications Importantes

### ✅ Avant de lancer:
- [ ] Node.js installé (v14+)
- [ ] npm installé
- [ ] Java JDK installé (v11+)
- [ ] Maven installé
- [ ] Base de données configurée (MySQL/PostgreSQL)

### ✅ Fichiers de configuration:
- [ ] `angular/src/environments/environment.ts` → `apiUrl: 'http://localhost:8080/api'`
- [ ] `backend/application.properties` → Configuration DB

### ✅ Après démarrage:
- [ ] Backend démarre sans erreur
- [ ] Frontend compile sans erreur
- [ ] Connexion à la base de données OK
- [ ] JWT token fonctionne
- [ ] Routes Angular fonctionnent

---

## 🐛 Résolution de Problèmes Courants

### Problème: Port 8080 déjà utilisé
**Solution:**
```bash
# Windows
netstat -ano | findstr :8080
taskkill /PID <PID> /F

# Linux/Mac
lsof -i :8080
kill -9 <PID>
```

### Problème: Port 4200 déjà utilisé
**Solution:**
```bash
# Changer le port dans angular
ng serve --port 4201
```

### Problème: npm install échoue
**Solution:**
```bash
# Nettoyer le cache npm
npm cache clean --force
rm -rf node_modules package-lock.json
npm install
```

### Problème: Base de données non connectée
**Solution:**
1. Vérifier que MySQL/PostgreSQL est démarré
2. Vérifier les credentials dans `application.properties`
3. Créer la base de données si elle n'existe pas:
```sql
CREATE DATABASE smartmedishop;
```

### Problème: CORS errors
**Solution:** Vérifier la configuration CORS dans le backend Spring Boot

---

## 📚 Documentation Complète

- 📖 **Guide complet:** `INTEGRATION_COMPLETE.md`
- 📖 **Documentation module:** `STOCK_MODULE_README.md`
- 📖 **Intégration panier:** `CART_INTEGRATION_EXAMPLE.txt`

---

## 🎊 Checklist de Démarrage

- [ ] Backend démarré et accessible
- [ ] Frontend démarré et accessible
- [ ] Compte Admin créé
- [ ] Produit de test créé
- [ ] Mouvement de test enregistré
- [ ] Navigation fonctionnelle
- [ ] Permissions testées

---

**🚀 Prêt à démarrer ? Bon développement !**
