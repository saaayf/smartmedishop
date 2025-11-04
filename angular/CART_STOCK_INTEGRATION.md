# 🛒 Système de Panier et Gestion Automatique du Stock

## 📋 Vue d'ensemble

Ce système intègre complètement le panier d'achats avec la gestion du stock. Lorsqu'un utilisateur achète des produits, le système :

1. ✅ Valide la disponibilité du stock
2. ✅ Crée une transaction
3. ✅ Diminue automatiquement les quantités en stock
4. ✅ Enregistre les mouvements dans l'historique

---

## 🔄 Flux Complet d'un Achat

### 1️⃣ Ajout au Panier

**Composant :** `ProductListComponent`

```typescript
// L'utilisateur clique sur "Ajouter au panier"
addToCart(product: Product): void {
  // Vérifications :
  // - Stock disponible (quantity > 0)
  // - Produit non expiré
  // - Quantité demandée <= stock disponible
  
  this.cartService.addToCart(product, 1);
}
```

**Fonctionnalités :**
- ✅ Validation du stock en temps réel
- ✅ Vérification de la date d'expiration
- ✅ Messages d'erreur clairs si problème
- ✅ Stockage local (localStorage) du panier

---

### 2️⃣ Gestion du Panier

**Service :** `CartService`

```typescript
// Méthodes disponibles :
- addToCart(product, quantity)      // Ajouter un produit
- removeFromCart(productId)         // Retirer un produit
- updateQuantity(productId, qty)    // Modifier la quantité
- clearCart()                       // Vider le panier
- getTotal()                        // Calculer le total
- validateCart()                    // Valider avant achat
- processPurchase(paymentMethod)    // Finaliser l'achat
```

**Validation du Panier :**
```typescript
validateCart(): Observable<{ valid: boolean; errors: string[] }>
```

Cette méthode vérifie :
- ✅ Le panier n'est pas vide
- ✅ Les produits existent toujours
- ✅ Le stock est toujours suffisant
- ✅ Les produits ne sont pas expirés
- ✅ Met à jour les prix en temps réel

---

### 3️⃣ Finalisation de l'Achat

**Composant :** `CartComponent`

```typescript
proceedToCheckout() {
  // 1. Validation du panier
  this.cartService.validateCart().subscribe(validation => {
    if (!validation.valid) {
      // Afficher les erreurs
      return;
    }

    // 2. Traitement de l'achat
    this.cartService.processPurchase('CARD').subscribe(result => {
      // Succès : panier vidé, redirection vers transactions
    });
  });
}
```

---

### 4️⃣ Traitement Backend (processPurchase)

**Service :** `CartService.processPurchase()`

```typescript
processPurchase(paymentMethod: string): Observable<PurchaseResult> {
  // Étape 1 : Créer la transaction
  const transaction = {
    amount: this.getTotal(),
    paymentMethod: 'CARD',
    transactionType: 'PURCHASE'
  };
  
  return this.apiService.createTransaction(transaction).pipe(
    switchMap((transactionResponse) => {
      // Étape 2 : Enregistrer les mouvements de stock
      const movements = items.map(item => 
        this.stockService.recordMovement({
          productId: item.product.id,
          movementType: 'OUT',    // Sortie de stock
          quantity: item.quantity,
          reason: 'SALE'          // Raison : Vente
        })
      );
      
      // Étape 3 : Exécuter tous les mouvements
      return forkJoin(movements);
    })
  );
}
```

**Ce qui se passe côté backend :**

1. **Transaction créée** → `POST /api/transactions`
   - Montant enregistré
   - Type = PURCHASE
   - Utilisateur lié

2. **Pour chaque produit** → `POST /api/stock/movements`
   - `productId` : ID du produit
   - `movementType` : "OUT" (sortie)
   - `quantity` : quantité achetée
   - `reason` : "SALE" (vente)

3. **Stock mis à jour automatiquement**
   - `product.quantity -= movement.quantity`
   - Si stock < seuil → Alerte créée automatiquement

---

## 📊 Historique des Mouvements

### Visualisation

Après un achat, on peut voir l'historique dans :

**Page Produit → Onglet "Historique des mouvements"**

```
Date                Type    Quantité    Raison
04/11/2025 14:30   OUT     2           Vente
03/11/2025 10:15   IN      50          Achat
```

### Types de Mouvements

| Type | Description | Icône | Couleur |
|------|-------------|-------|---------|
| IN   | Entrée de stock | ➕ | Vert |
| OUT  | Sortie de stock | ➖ | Rouge |

### Raisons des Mouvements

| Raison | Utilisation |
|--------|-------------|
| SALE | Vente client (automatique lors d'un achat) |
| PURCHASE | Achat fournisseur (manuel) |
| RETURN | Retour client |
| MANUAL | Ajustement manuel |
| EXPIRED | Produit expiré retiré |
| DAMAGED | Produit endommagé retiré |

---

## 🎯 Indicateur de Panier

### Badge dans la Navbar

```html
<button mat-button routerLink="/cart">
  <mat-icon [matBadge]="cartItemsCount" 
            [matBadgeHidden]="cartItemsCount === 0"
            matBadgeColor="warn">
    shopping_cart
  </mat-icon>
  Panier
</button>
```

**Affichage :**
- 🔴 Badge rouge avec le nombre d'articles
- ⚪ Badge caché si panier vide
- ♻️ Mise à jour en temps réel

---

## 🔐 Sécurité et Validation

### Validation Frontend

```typescript
// 1. Vérification du stock avant ajout
if (quantity > product.quantity) {
  throw new Error('Stock insuffisant');
}

// 2. Vérification de l'expiration
if (product.expirationDate < today) {
  throw new Error('Produit expiré');
}

// 3. Validation complète avant achat
validateCart() // Re-vérifie tout en temps réel
```

### Validation Backend

Le backend doit également vérifier :
- ✅ Stock disponible
- ✅ Produit actif
- ✅ Quantité valide
- ✅ Prix cohérent

---

## 📱 Interface Utilisateur

### Page Panier

**Affichage pour chaque produit :**
```
┌─────────────────────────────────────────┐
│ Paracétamol 500mg                       │
│ SKU: PARACETAMOL001                     │
│ Boîte de 16 comprimés                   │
│ 5.99€                                   │
│ 📦 Stock disponible: 98                 │
│                                         │
│ [-] 2 [+]              11.98€     [🗑️] │
└─────────────────────────────────────────┘
```

**Boutons :**
- ➖ Diminuer quantité
- ➕ Augmenter quantité (désactivé si max atteint)
- 🗑️ Supprimer du panier

**Résumé :**
```
Sous-total:        50.00€
TVA (19%):          9.50€
─────────────────────────
Total:             59.50€

ℹ️ Le stock sera automatiquement mis à jour
   après l'achat

[Vider le panier] [🛒 Passer la commande]
```

---

## 🔄 Persistence

### LocalStorage

Le panier est sauvegardé dans le localStorage :

```typescript
// Structure stockée
{
  cartItems: [
    {
      product: { id: 1, sku: "...", name: "...", ... },
      quantity: 2
    }
  ]
}
```

**Avantages :**
- ✅ Panier conservé après rechargement
- ✅ Panier conservé entre sessions
- ✅ Synchronisation multi-onglets

**Nettoyage :**
- Après achat réussi
- À la déconnexion
- Manuel (bouton "Vider")

---

## 🧪 Tests et Scénarios

### Scénario 1 : Achat Normal

1. Utilisateur ajoute Produit A (qté: 2)
2. Utilisateur ajoute Produit B (qté: 1)
3. Utilisateur valide le panier
4. Transaction créée
5. 2 mouvements OUT enregistrés
6. Stock mis à jour :
   - Produit A : 100 → 98
   - Produit B : 50 → 49
7. Panier vidé
8. Redirection vers /payments

### Scénario 2 : Stock Insuffisant

1. Produit A en stock : 5 unités
2. Utilisateur ajoute 3 au panier ✅
3. Utilisateur veut passer à 10 ❌
4. Message : "Stock insuffisant. Disponible: 5"

### Scénario 3 : Produit Expiré

1. Produit A expire le 01/11/2025
2. Aujourd'hui : 04/11/2025
3. Utilisateur essaie d'ajouter ❌
4. Message : "Ce produit est expiré"

### Scénario 4 : Validation Avant Achat

1. Produit A ajouté hier (stock: 10)
2. Aujourd'hui quelqu'un a acheté 8
3. Stock actuel : 2
4. Utilisateur a 5 dans son panier
5. Validation détecte le problème ❌
6. Message : "Stock insuffisant pour Produit A"

---

## 📈 Avantages du Système

✅ **Automatique** : Pas de gestion manuelle du stock après vente  
✅ **Tracé** : Historique complet de tous les mouvements  
✅ **Sécurisé** : Validation à chaque étape  
✅ **Temps Réel** : Stock toujours à jour  
✅ **UX Fluide** : Indicateurs visuels clairs  
✅ **Persistant** : Panier sauvegardé  

---

## 🚀 Utilisation

### Pour un Client

1. Parcourir les produits : `/stock/products`
2. Cliquer sur "Ajouter au panier" 🛒
3. Voir le badge s'incrémenter dans la navbar
4. Aller au panier : `/cart`
5. Ajuster les quantités si besoin
6. Cliquer sur "Passer la commande"
7. Voir la confirmation
8. Consulter la transaction : `/payments`

### Pour un Admin

En plus de tout ce qui précède :

1. Voir l'historique des ventes : `/stock/products/{id}` → Onglet Mouvements
2. Vérifier les mouvements OUT avec raison "SALE"
3. Ajouter du stock manuellement : `/stock/movements/new`
4. Voir les alertes de stock bas

---

## 🎓 Code Clés à Retenir

### Ajouter au Panier
```typescript
this.cartService.addToCart(product, quantity);
```

### Valider avant Achat
```typescript
this.cartService.validateCart().subscribe(result => {
  if (result.valid) {
    // OK pour acheter
  }
});
```

### Finaliser l'Achat
```typescript
this.cartService.processPurchase('CARD').subscribe(result => {
  // Transaction ID: result.transaction.transactionId
  // Stock updated: result.stockUpdated
});
```

---

## 📚 Documentation Technique

### Services Impliqués

1. **CartService** (`cart.service.ts`)
   - Gestion du panier
   - Validation
   - Traitement des achats

2. **StockService** (`stock.service.ts`)
   - Récupération des produits
   - Enregistrement des mouvements
   - Gestion des alertes

3. **ApiService** (`api.service.ts`)
   - Création des transactions
   - Communication backend

### Composants Impliqués

1. **ProductListComponent**
   - Affichage des produits
   - Ajout au panier

2. **CartComponent**
   - Affichage du panier
   - Modification des quantités
   - Finalisation de l'achat

3. **AppComponent**
   - Indicateur de panier (badge)

---

## ✨ Résumé

Le système est maintenant **complètement intégré** :
- ✅ Ajout au panier depuis la liste des produits
- ✅ Gestion du panier avec validation temps réel
- ✅ Achat qui crée automatiquement :
  - Une transaction
  - Des mouvements de stock (OUT/SALE)
  - Mise à jour des quantités
- ✅ Historique consultable pour chaque produit
- ✅ Indicateur visuel dans la navbar

**Le stock diminue automatiquement à chaque vente ! 🎉**
