# Module de Gestion de Stock - SmartMediShop

Ce module permet la gestion complète du stock de produits pharmaceutiques.

## ✅ Fonctionnalités Implémentées

### 1. **Gestion des Produits**
- ✅ Liste de tous les produits avec recherche
- ✅ Détails d'un produit
- ✅ Création de produit (Admin uniquement)
- ✅ Modification de produit (Admin uniquement)
- ✅ Badges d'alerte (stock bas, produit expiré)

### 2. **Mouvements de Stock**
- ✅ Enregistrement de mouvements (Admin uniquement)
- ✅ Types de mouvements: Entrée (IN) / Sortie (OUT)
- ✅ Raisons: Achat, Vente, Retour, Ajustement manuel, Expiré, Endommagé
- ✅ Historique des mouvements par produit

### 3. **Alertes**
- ✅ Affichage des alertes par produit
- ✅ Types d'alertes: Stock bas, Produit expiré
- ✅ Statuts: Active, Résolue

### 4. **Contrôle d'Accès**
- ✅ Tous les utilisateurs authentifiés peuvent consulter les produits
- ✅ Seuls les ADMIN peuvent:
  - Créer des produits
  - Modifier des produits
  - Enregistrer des mouvements de stock

## 📁 Structure des Fichiers

```
src/app/
├── models/
│   ├── product.model.ts
│   ├── stock-movement.model.ts
│   └── stock-alert.model.ts
├── core/services/
│   └── stock.service.ts
└── features/stock/
    ├── stock.module.ts
    ├── stock-routing.module.ts
    ├── product-list/
    │   ├── product-list.component.ts
    │   ├── product-list.component.html
    │   └── product-list.component.scss
    ├── product-detail/
    │   ├── product-detail.component.ts
    │   ├── product-detail.component.html
    │   └── product-detail.component.scss
    ├── product-form/
    │   ├── product-form.component.ts
    │   ├── product-form.component.html
    │   └── product-form.component.scss
    └── movement-form/
        ├── movement-form.component.ts
        ├── movement-form.component.html
        └── movement-form.component.scss
```

## 🔗 Routes Configurées

| Route | Composant | Accès | Description |
|-------|-----------|-------|-------------|
| `/stock/products` | ProductListComponent | Tous | Liste des produits |
| `/stock/products/new` | ProductFormComponent | Admin | Créer un produit |
| `/stock/products/:id` | ProductDetailComponent | Tous | Détails du produit |
| `/stock/products/:id/edit` | ProductFormComponent | Admin | Modifier un produit |
| `/stock/movements/new` | MovementFormComponent | Admin | Enregistrer un mouvement |

## 🎨 Navigation

La navigation a été mise à jour dans `app.component.html`:

**Pour les Administrateurs:**
- Menu déroulant "Stock" avec options:
  - Produits
  - Ajouter un produit
  - Enregistrer un mouvement

**Pour les autres utilisateurs:**
- Lien simple "Stock" vers la liste des produits

## 🔧 Configuration API

L'URL de l'API est configurée dans `src/environments/environment.ts`:

```typescript
export const environment = {
  production: false,
  apiUrl: 'http://localhost:8080/api'
};
```

Le service `StockService` utilise automatiquement:
- Base URL: `http://localhost:8080/api/stock`
- Headers JWT automatiques via `AuthService`

## 📋 TODO: Intégration avec le Panier

Si vous avez un système de panier d'achat, vous devrez:

1. **Créer un CartService** (si pas déjà fait) avec:
   ```typescript
   interface CartItem {
     product: Product;
     quantity: number;
   }
   ```

2. **Modifier la méthode addToCart** dans `product-list.component.ts`:
   ```typescript
   addToCart(product: Product): void {
     this.cartService.addItem({
       product: product,
       quantity: 1
     });
     this.notificationService.showSuccess(`${product.name} ajouté au panier`);
   }
   ```

3. **Lors de la création d'une transaction**, inclure le SKU:
   ```typescript
   createTransaction(cartItems: CartItem[]): Observable<Transaction> {
     const transaction = {
       userId: this.authService.getCurrentUser()?.id,
       amount: this.calculateTotal(cartItems),
       items: cartItems.map(item => ({
         sku: item.product.sku,  // ⚠️ IMPORTANT
         quantity: item.quantity,
         price: item.product.price
       }))
     };
     return this.http.post<Transaction>(`${this.apiUrl}/transactions`, transaction);
   }
   ```

Le backend décrémentera automatiquement le stock lors de la création d'une transaction.

## 🧪 Test de l'Intégration

1. **Démarrer le backend Spring Boot:**
   ```bash
   cd backend
   ./mvnw spring-boot:run
   ```

2. **Démarrer le frontend Angular:**
   ```bash
   cd angular
   npm start
   ```

3. **Se connecter avec un compte ADMIN**

4. **Tester les fonctionnalités:**
   - Créer un produit
   - Voir la liste des produits
   - Voir les détails d'un produit
   - Modifier un produit
   - Enregistrer un mouvement de stock

## 🎯 Prochaines Étapes

- [ ] Implémenter la fonctionnalité d'ajout au panier complète
- [ ] Ajouter la pagination sur la liste des produits
- [ ] Ajouter des graphiques pour visualiser les mouvements de stock
- [ ] Ajouter la possibilité de résoudre les alertes
- [ ] Ajouter l'export des données en CSV/PDF
- [ ] Ajouter des filtres avancés (par catégorie, par date d'expiration, etc.)

## 📞 Support

Pour toute question ou problème, consulter la documentation du backend Spring Boot.
