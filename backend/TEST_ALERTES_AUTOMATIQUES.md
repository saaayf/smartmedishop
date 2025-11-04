# Guide de Test - Alertes Automatiques de Stock

## ✅ Modifications Implémentées

### 1. **StockAlertRepository** - Nouvelle méthode
```java
boolean existsByProductAndAlertTypeAndStatus(Product product, String alertType, AlertStatus status);
```
Cette méthode permet de vérifier si une alerte existe déjà pour éviter les doublons.

### 2. **StockService** - Méthode `checkAndCreateAlerts()` améliorée
- ✅ Vérifie si `quantity <= lowStockThreshold`
- ✅ Vérifie qu'une alerte LOW_STOCK ACTIVE n'existe pas déjà
- ✅ Crée l'alerte uniquement si elle n'existe pas
- ✅ Fait pareil pour les produits expirés (expirationDate < today)
- ✅ Appelée automatiquement dans:
  - `createProduct()` - après création
  - `updateProduct()` - après mise à jour
  - `recordMovement()` - après mouvement de stock

### 3. **AlertMigrationService** - Nouveau service
Service pour générer les alertes manquantes sur les produits existants.
- Parcourt tous les produits
- Crée les alertes LOW_STOCK pour quantity <= threshold
- Crée les alertes EXPIRED pour produits expirés
- Retourne un résumé des alertes créées

### 4. **StockController** - Nouvel endpoint
```
GET /api/stock/alerts/generate-missing (ADMIN uniquement)
```
Appelle la migration pour créer toutes les alertes manquantes.

---

## 🧪 Tests à Effectuer

### Test 1: Création d'un produit avec stock bas
**Endpoint:** `POST /api/stock/products`

**Headers:**
```
Authorization: Bearer <ADMIN_TOKEN>
Content-Type: application/json
```

**Body:**
```json
{
  "sku": "TEST-LOW-001",
  "name": "Produit Stock Bas",
  "description": "Test alerte stock bas",
  "quantity": 3,
  "lowStockThreshold": 5,
  "price": 50.00,
  "expirationDate": "2025-12-31"
}
```

**Résultat attendu:**
- ✅ Produit créé avec ID (ex: 10)
- ✅ Une alerte LOW_STOCK automatiquement créée dans la table `stock_alerts`

**Vérification:**
```
GET /api/stock/alerts/product/{productId}
```

---

### Test 2: Création d'un produit expiré
**Endpoint:** `POST /api/stock/products`

**Body:**
```json
{
  "sku": "TEST-EXP-001",
  "name": "Produit Expiré",
  "description": "Test alerte expiration",
  "quantity": 50,
  "lowStockThreshold": 10,
  "price": 30.00,
  "expirationDate": "2024-01-01"
}
```

**Résultat attendu:**
- ✅ Produit créé
- ✅ Une alerte EXPIRED automatiquement créée

---

### Test 3: Mise à jour d'un produit qui passe en stock bas
**Étape 1:** Créer un produit avec stock OK
```json
POST /api/stock/products
{
  "sku": "TEST-UPDATE-001",
  "name": "Produit Normal",
  "quantity": 100,
  "lowStockThreshold": 10,
  "price": 25.00
}
```

**Étape 2:** Mettre à jour pour passer en stock bas
```json
PUT /api/stock/products/{productId}
{
  "quantity": 5
}
```

**Résultat attendu:**
- ✅ Produit mis à jour
- ✅ Alerte LOW_STOCK créée automatiquement après la mise à jour

---

### Test 4: Mouvement de stock qui provoque une alerte
**Étape 1:** Créer un produit avec stock OK (20 unités, seuil 5)

**Étape 2:** Enregistrer une sortie de stock
```json
POST /api/stock/movements
{
  "productId": {productId},
  "movementType": "OUT",
  "quantity": 16,
  "reason": "SALE"
}
```

**Résultat attendu:**
- ✅ Stock passe à 4 (20 - 16)
- ✅ Alerte LOW_STOCK créée car 4 < 5

---

### Test 5: Pas de doublon d'alertes
**Étape 1:** Créer un produit avec stock bas (alerte créée)

**Étape 2:** Faire un autre mouvement OUT
```json
POST /api/stock/movements
{
  "productId": {productId},
  "movementType": "OUT",
  "quantity": 1,
  "reason": "SALE"
}
```

**Résultat attendu:**
- ✅ Stock décrémenté
- ❌ PAS de nouvelle alerte créée (déjà une alerte ACTIVE existante)

**Vérification:**
```sql
SELECT COUNT(*) FROM stock_alerts 
WHERE product_id = {productId} 
AND alert_type = 'LOW_STOCK' 
AND status = 'ACTIVE';
-- Doit retourner 1, pas 2 ou plus
```

---

### Test 6: Migration des alertes manquantes (Produits existants)
**Contexte:** Vous avez déjà des produits dans la base de données SANS alertes

**Endpoint:** `GET /api/stock/alerts/generate-missing`

**Headers:**
```
Authorization: Bearer <ADMIN_TOKEN>
```

**Résultat attendu:**
```json
{
  "productsChecked": 15,
  "lowStockAlertsCreated": 3,
  "expiredAlertsCreated": 1,
  "totalAlertsCreated": 4,
  "message": "Migration completed successfully"
}
```

**Vérification dans la base:**
```sql
-- Voir toutes les nouvelles alertes
SELECT sa.id, p.sku, p.name, sa.alert_type, sa.message, sa.status
FROM stock_alerts sa
JOIN products p ON sa.product_id = p.id
WHERE sa.status = 'ACTIVE'
ORDER BY sa.created_at DESC;
```

---

## 📊 Requêtes SQL de Vérification

### Voir tous les produits avec leur statut d'alerte
```sql
SELECT 
    p.id,
    p.sku,
    p.name,
    p.quantity,
    p.low_stock_threshold,
    p.expiration_date,
    COUNT(sa.id) as alert_count,
    GROUP_CONCAT(sa.alert_type) as alert_types
FROM products p
LEFT JOIN stock_alerts sa ON p.id = sa.product_id AND sa.status = 'ACTIVE'
GROUP BY p.id;
```

### Produits avec stock bas SANS alerte (ne devrait rien retourner après migration)
```sql
SELECT p.*
FROM products p
LEFT JOIN stock_alerts sa ON p.id = sa.product_id 
    AND sa.alert_type = 'LOW_STOCK' 
    AND sa.status = 'ACTIVE'
WHERE p.quantity <= p.low_stock_threshold
AND sa.id IS NULL;
```

### Produits expirés SANS alerte (ne devrait rien retourner après migration)
```sql
SELECT p.*
FROM products p
LEFT JOIN stock_alerts sa ON p.id = sa.product_id 
    AND sa.alert_type = 'EXPIRED' 
    AND sa.status = 'ACTIVE'
WHERE p.expiration_date < CURDATE()
AND sa.id IS NULL;
```

---

## 🔧 Commandes pour Tester

### 1. Démarrer l'application
```bash
cd 'c:\Users\HP\Downloads\smartmedishop-main111\smartmedishop-main\backend'
mvn spring-boot:run
```

### 2. S'authentifier en tant qu'ADMIN
```bash
# Enregistrer un admin
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "username": "admin",
    "email": "admin@smartmedishop.com",
    "password": "admin123",
    "firstName": "Admin",
    "lastName": "User"
  }'

# Se connecter
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "username": "admin",
    "password": "admin123"
  }'
```

### 3. Créer un produit avec stock bas (Test 1)
```bash
curl -X POST http://localhost:8080/api/stock/products \
  -H "Authorization: Bearer <TOKEN>" \
  -H "Content-Type: application/json" \
  -d '{
    "sku": "TEST-LOW-001",
    "name": "Produit Stock Bas",
    "quantity": 3,
    "lowStockThreshold": 5,
    "price": 50.00
  }'
```

### 4. Vérifier les alertes
```bash
# Remplacer {productId} par l'ID du produit créé
curl -X GET http://localhost:8080/api/stock/alerts/product/{productId} \
  -H "Authorization: Bearer <TOKEN>"
```

### 5. Lancer la migration
```bash
curl -X GET http://localhost:8080/api/stock/alerts/generate-missing \
  -H "Authorization: Bearer <TOKEN>"
```

---

## ✅ Critères de Succès

1. **Création automatique:** Alerte créée dès qu'un produit a stock bas ou expiré
2. **Pas de doublon:** Une seule alerte ACTIVE par type et par produit
3. **Après mouvement:** Alerte créée après un mouvement OUT si stock bas
4. **Après update:** Alerte créée après mise à jour si quantité passe sous le seuil
5. **Migration:** Endpoint `/generate-missing` crée toutes les alertes manquantes
6. **Performance:** Méthode `existsByProductAndAlertTypeAndStatus()` rapide (index sur product_id, alert_type, status)

---

## 🎯 Points Importants

- ✅ Les alertes sont créées avec `status = ACTIVE` par défaut
- ✅ La méthode `checkAndCreateAlerts()` est **privée** et appelée automatiquement
- ✅ Pas besoin d'appeler manuellement la création d'alerte
- ✅ L'endpoint `/generate-missing` est pour une migration ponctuelle (produits existants)
- ✅ Toutes les nouvelles créations/mises à jour/mouvements créent automatiquement les alertes

---

## 📝 Notes pour le Frontend

Le frontend peut maintenant :
1. Récupérer les alertes par produit : `GET /api/stock/alerts/product/{id}`
2. Lancer une migration si besoin (ADMIN) : `GET /api/stock/alerts/generate-missing`
3. Afficher un badge "Stock bas" ou "Expiré" en fonction des alertes actives
4. Filtrer les produits par alerte (stock bas, expirés, etc.)
