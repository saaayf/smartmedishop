# 🔍 Guide de Débogage - Alertes de Stock

## 📋 Étapes pour identifier le problème

### 1. Ouvrir la Console du Navigateur
1. Appuyer sur **F12** pour ouvrir les outils de développement
2. Aller dans l'onglet **Console**
3. Garder la console ouverte

### 2. Naviguer vers le Produit "hiba"
```
http://localhost:4200/stock/products/{id-de-hiba}
```

### 3. Cliquer sur l'onglet "Alertes"

### 4. Analyser les Logs dans la Console

Vous devriez voir des messages comme ceci:

```
🔍 Loading alerts for product ID: 1
✅ Alerts received from backend: [...]
🔧 Generating missing alerts for product: hiba
Product quantity: 4
Low stock threshold: 5
Is low stock? true
Current alerts count: X
Has LOW_STOCK alert? true/false
📊 Final alerts list: [...]
```

---

## 🎯 Scénarios Possibles

### Scénario 1: Le backend retourne les alertes ✅

**Logs attendus:**
```
🔍 Loading alerts for product ID: 1
✅ Alerts received from backend: [
  {
    id: 1,
    productId: 1,
    alertType: "LOW_STOCK",
    message: "Product hiba low stock: 4",
    status: "ACTIVE",
    createdAt: "2025-11-04T01:46:00"
  }
]
🔧 Generating missing alerts for product: hiba
Has LOW_STOCK alert? true
📊 Final alerts list: [1 alert]
```

**Résultat:** Les alertes s'affichent normalement ✅

**Actions:** Aucune, tout fonctionne!

---

### Scénario 2: Le backend retourne un tableau vide ⚠️

**Logs attendus:**
```
🔍 Loading alerts for product ID: 1
✅ Alerts received from backend: []
🔧 Generating missing alerts for product: hiba
Product quantity: 4
Low stock threshold: 5
Is low stock? true
Has LOW_STOCK alert? false
⚠️ Creating local LOW_STOCK alert
📊 Final alerts list: [
  {
    productId: 1,
    alertType: "LOW_STOCK",
    message: "Stock bas: seulement 4 unités restantes (seuil: 5)",
    status: "ACTIVE",
    createdAt: "2025-11-04T10:30:00"
    // Pas d'ID → alerte locale
  }
]
```

**Résultat:** Une alerte "locale" s'affiche avec le badge "Local" 👁️

**Raison:** Le backend n'a pas créé l'alerte dans la base de données

**Actions à faire:**
1. Vérifier que le backend crée bien les alertes à la création du produit
2. Exécuter un script de migration pour créer les alertes manquantes
3. Vérifier le code backend dans `StockController` et `StockService`

---

### Scénario 3: Erreur HTTP 403 (Forbidden) 🔒

**Logs attendus:**
```
🔍 Loading alerts for product ID: 1
❌ Error loading alerts: HttpErrorResponse
Error details: {
  status: 403,
  message: "Forbidden",
  url: "http://localhost:8080/api/stock/alerts/product/1"
}
🔧 Generating missing alerts for product: hiba
⚠️ Creating local LOW_STOCK alert
📊 Final alerts list: [1 local alert]
```

**Résultat:** Alerte locale affichée

**Raison:** L'utilisateur n'a pas les permissions pour accéder aux alertes

**Actions à faire:**
1. Vérifier que vous êtes connecté en tant qu'ADMIN
2. Vérifier le JWT token contient le rôle ADMIN
3. Vérifier le backend accepte le rôle pour cet endpoint:
   ```java
   @GetMapping("/alerts/product/{productId}")
   @PreAuthorize("hasRole('ADMIN')") // ← Vérifier cette ligne
   ```

---

### Scénario 4: Erreur HTTP 404 (Not Found) 🔎

**Logs attendus:**
```
🔍 Loading alerts for product ID: 1
❌ Error loading alerts: HttpErrorResponse
Error details: {
  status: 404,
  message: "Not Found",
  url: "http://localhost:8080/api/stock/alerts/product/1"
}
```

**Raison:** L'endpoint n'existe pas ou l'URL est incorrecte

**Actions à faire:**
1. Vérifier que le backend expose bien cet endpoint:
   ```
   GET /api/stock/alerts/product/{productId}
   ```
2. Tester avec Postman:
   ```bash
   GET http://localhost:8080/api/stock/alerts/product/1
   Headers:
     Authorization: Bearer {votre-token}
   ```
3. Vérifier les logs du backend Spring Boot

---

### Scénario 5: Erreur HTTP 500 (Server Error) 💥

**Logs attendus:**
```
🔍 Loading alerts for product ID: 1
❌ Error loading alerts: HttpErrorResponse
Error details: {
  status: 500,
  message: "Internal Server Error",
  url: "http://localhost:8080/api/stock/alerts/product/1"
}
```

**Raison:** Erreur côté backend (base de données, query SQL, etc.)

**Actions à faire:**
1. Vérifier les logs du backend Spring Boot
2. Vérifier la connexion à la base de données
3. Vérifier que la table `stock_alerts` existe
4. Vérifier la query SQL dans le repository

---

### Scénario 6: Les alertes existent mais ne s'affichent pas 🤔

**Logs attendus:**
```
🔍 Loading alerts for product ID: 1
✅ Alerts received from backend: [
  {
    id: 1,
    productId: 1,
    alertType: "LOW_STOCK",
    message: "...",
    status: "ACTIVE",
    createdAt: "2025-11-04T01:46:00"
  }
]
📊 Final alerts list: [1 alert]
```

**Mais:** Rien ne s'affiche dans l'onglet

**Raison possible:** Problème de template Angular

**Actions à faire:**
1. Vérifier que `this.alerts.length > 0` dans le template
2. Inspecter l'élément HTML avec F12 → Éléments
3. Vérifier qu'il n'y a pas d'erreur CSS masquant les éléments

---

## 🧪 Tests à Effectuer

### Test 1: Vérifier l'API Backend avec Postman

```bash
# Récupérer le token JWT
POST http://localhost:8080/api/auth/login
Body: {
  "username": "admin",
  "password": "votre-password"
}

# Utiliser le token pour récupérer les alertes
GET http://localhost:8080/api/stock/alerts/product/1
Headers:
  Authorization: Bearer {le-token-reçu}
```

**Résultat attendu:**
```json
[
  {
    "id": 1,
    "productId": 1,
    "alertType": "LOW_STOCK",
    "message": "Product hiba low stock: 4",
    "status": "ACTIVE",
    "createdAt": "2025-11-04T01:46:00"
  }
]
```

**Si vide `[]`:** Le backend n'a pas créé les alertes → Problème backend

**Si erreur:** Vérifier permissions, endpoint, base de données

---

### Test 2: Vérifier la Base de Données

```sql
-- Vérifier les alertes pour le produit "hiba" (supposons id=1)
SELECT * FROM stock_alerts WHERE product_id = 1;

-- Résultat attendu:
-- id | product_id | alert_type | message                      | status | created_at
-- 1  | 1          | LOW_STOCK  | Product hiba low stock: 4   | ACTIVE | 2025-11-04 01:46:00
```

**Si vide:** Le backend ne crée pas les alertes → Code backend à corriger

**Si plein:** Les alertes existent → Problème dans l'API ou le frontend

---

### Test 3: Comparer avec un Produit Fonctionnel

1. Ouvrir le produit "SKU123" (qui fonctionne)
2. Noter les logs dans la console
3. Comparer avec les logs du produit "hiba"
4. Identifier les différences

**Différences possibles:**
- `productId` différent
- Format des alertes différent
- Endpoint appelé différent

---

## 🔧 Solutions selon le Diagnostic

### Si: Backend ne retourne rien (tableau vide)
**→ Corriger le code backend pour créer les alertes**

```java
@Service
public class StockService {
    
    @Transactional
    public Product createProduct(ProductDTO dto) {
        Product product = productRepository.save(dto.toEntity());
        
        // Créer l'alerte si stock bas
        checkAndCreateLowStockAlert(product);
        
        return product;
    }
    
    private void checkAndCreateLowStockAlert(Product product) {
        if (product.getQuantity() < product.getLowStockThreshold()) {
            StockAlert alert = new StockAlert();
            alert.setProductId(product.getId());
            alert.setAlertType(AlertType.LOW_STOCK);
            alert.setMessage("Product " + product.getSku() + " low stock: " + product.getQuantity());
            alert.setStatus(AlertStatus.ACTIVE);
            alertRepository.save(alert);
        }
    }
}
```

---

### Si: Erreur 403 (permissions)
**→ Vérifier le rôle de l'utilisateur**

```typescript
// Dans la console du navigateur:
console.log(this.authService.getCurrentUser());
// Doit afficher: { userType: "ADMIN", ... }

console.log(this.authService.hasRole('ADMIN'));
// Doit afficher: true
```

**→ Vérifier le backend**
```java
@GetMapping("/alerts/product/{productId}")
@PreAuthorize("hasAnyRole('ADMIN', 'FRAUD_ANALYST')") // ← Autoriser plusieurs rôles
public ResponseEntity<List<StockAlert>> getAlerts(@PathVariable Long productId) {
    // ...
}
```

---

### Si: Erreur 404 (endpoint introuvable)
**→ Vérifier l'URL de l'API**

1. Vérifier `environment.ts`:
   ```typescript
   apiUrl: 'http://localhost:8080/api' // ← Doit pointer vers votre backend
   ```

2. Vérifier que le backend tourne sur le bon port:
   ```bash
   curl http://localhost:8080/api/stock/products
   ```

3. Vérifier le mapping dans le controller:
   ```java
   @RestController
   @RequestMapping("/api/stock") // ← Base path
   public class StockController {
       
       @GetMapping("/alerts/product/{productId}") // ← Path complet: /api/stock/alerts/product/{id}
       // ...
   }
   ```

---

## 📊 Tableau Récapitulatif

| Symptôme | Logs Console | Cause Probable | Solution |
|----------|--------------|----------------|----------|
| Aucune alerte | `Alerts received: []` | Backend ne crée pas les alertes | Corriger code backend |
| Badge "Local" | `Creating local alert` | Alertes générées frontend | Normal (temporaire) |
| Erreur 403 | `status: 403` | Permissions manquantes | Vérifier rôle ADMIN |
| Erreur 404 | `status: 404` | Endpoint inexistant | Vérifier backend + URL |
| Erreur 500 | `status: 500` | Erreur serveur | Vérifier logs backend |
| Rien ne s'affiche | Logs OK mais UI vide | Problème template | Inspecter HTML |

---

## 🎬 Action Immédiate

1. **Ouvrir la console (F12)**
2. **Aller sur le produit "hiba"**
3. **Cliquer sur l'onglet "Alertes"**
4. **Copier TOUS les logs de la console**
5. **Analyser avec ce guide**

Les logs vous diront exactement quel est le problème! 🔍

---

## 💡 Rappel

Le code frontend est maintenant **résilient**:
- ✅ Si le backend retourne les alertes → Affichage normal
- ✅ Si le backend ne retourne rien → Génération automatique locale
- ✅ Dans tous les cas → L'admin voit toujours les alertes

**C'est une solution de repli**, mais l'idéal reste que le backend crée les alertes en base de données pour la persistance.
