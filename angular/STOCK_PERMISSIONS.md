# 🔐 Gestion des Permissions - Module Stock

## Vue d'ensemble
Le module de gestion de stock implémente un système de permissions à plusieurs niveaux pour contrôler l'accès aux fonctionnalités selon le rôle de l'utilisateur.

---

## 👥 Rôles et Permissions

### 🔵 Utilisateur Simple (USER/CLIENT)

**✅ Autorisé:**
- Voir la liste des produits
- Voir les détails d'un produit (nom, description, prix, quantité disponible, date d'expiration)
- Ajouter des produits au panier
- Acheter des produits (ce qui crée automatiquement un mouvement de stock côté backend)

**❌ Non autorisé:**
- Créer un nouveau produit
- Modifier un produit existant
- Voir l'historique des mouvements de stock
- Voir les alertes de stock
- Enregistrer manuellement un mouvement de stock
- Accéder à `/stock/products/new`
- Accéder à `/stock/products/:id/edit`
- Accéder à `/stock/movements/new`

**📱 Interface:**
- Bouton "Ajouter au panier" visible sur chaque produit
- Pas de bouton "Modifier" ou "Ajouter un produit"
- Pas d'onglets "Historique des mouvements" ou "Alertes" dans les détails du produit
- Message informatif: "L'historique des mouvements et les alertes de stock sont réservés aux administrateurs"

---

### 🔴 Administrateur (ADMIN)

**✅ Autorisé:**
- **Toutes les permissions des utilisateurs simples, plus:**
- Créer un nouveau produit
- Modifier un produit existant
- Supprimer un produit (si implémenté)
- Voir l'historique complet des mouvements de stock
- Voir toutes les alertes de stock (stock bas, produits expirés)
- Enregistrer manuellement des mouvements de stock (entrées/sorties)
- Gérer les raisons de mouvements (Achat, Vente, Retour, Ajustement manuel, Expiré, Endommagé)

**📱 Interface:**
- Menu déroulant "Stock" dans la navbar avec:
  - Produits
  - Ajouter un produit
  - Enregistrer un mouvement
- Boutons "Modifier" sur chaque produit
- Bouton "Ajouter un produit" en haut de la liste
- Onglets "Historique des mouvements" et "Alertes" dans les détails du produit
- Pas de bouton "Ajouter au panier" (un admin n'achète pas via l'interface client)

---

## 🛡️ Implémentation des Protections

### 1. Protections Frontend

#### Routes protégées (RoleGuard)
```typescript
// stock-routing.module.ts
{
  path: 'products/new',
  component: ProductFormComponent,
  canActivate: [RoleGuard],
  data: { allowedRoles: ['ADMIN'] }
}
```

#### Affichage conditionnel (Directives *ngIf)
```html
<!-- Bouton visible seulement pour ADMIN -->
<button *ngIf="isAdmin()" (click)="editProduct()">Modifier</button>

<!-- Bouton visible seulement pour NON-ADMIN -->
<button *ngIf="!isAdmin()" (click)="addToCart()">Ajouter au panier</button>

<!-- Onglets visibles seulement pour ADMIN -->
<mat-tab label="Historique des mouvements" *ngIf="isAdmin()">
```

#### Vérification dans les composants
```typescript
isAdmin(): boolean {
  return this.authService.hasRole('ADMIN');
}

loadProduct(id: number): void {
  // ...
  if (this.isAdmin()) {
    this.loadMovements(id);
    this.loadAlerts(id);
  }
}
```

### 2. Protections Backend (à vérifier)

**Important:** Le frontend seul ne suffit pas pour la sécurité. Le backend Spring Boot doit également implémenter ces protections avec `@PreAuthorize` ou `@Secured`.

Exemple attendu côté backend:
```java
@PostMapping("/products")
@PreAuthorize("hasRole('ADMIN')")
public ResponseEntity<Product> createProduct(@RequestBody Product product) {
    // ...
}

@GetMapping("/movements/product/{productId}")
@PreAuthorize("hasRole('ADMIN')")
public ResponseEntity<List<StockMovement>> getMovements(@PathVariable Long productId) {
    // ...
}
```

---

## 🔄 Flux d'Achat (Utilisateur Simple)

1. **Utilisateur consulte les produits**
   - Voit la liste avec quantités disponibles
   - Peut filtrer par nom ou SKU

2. **Utilisateur ajoute au panier**
   - Sélectionne la quantité désirée
   - Produit ajouté au panier avec validation de stock

3. **Utilisateur finalise l'achat**
   - Panier envoyé au backend via CartService
   - Backend crée la transaction
   - **Backend décrémente automatiquement le stock**
   - **Backend enregistre automatiquement un mouvement de type "OUT" avec raison "SALE"**

4. **Résultat**
   - Stock mis à jour automatiquement
   - Mouvement visible uniquement par les admins
   - Si stock < seuil → Alerte créée automatiquement

---

## 📊 Exemple de Scénarios

### Scénario 1: Client achète du Paracétamol
- **Avant:** Stock = 100 unités
- **Client commande:** 5 unités
- **Backend enregistre:** Mouvement OUT, quantité=5, raison=SALE
- **Après:** Stock = 95 unités
- **Visible par le client:** ❌ Non (mouvement masqué)
- **Visible par l'admin:** ✅ Oui (dans l'historique)

### Scénario 2: Admin ajuste le stock manuellement
- **Admin constate:** Produit endommagé
- **Admin enregistre:** Mouvement OUT, quantité=3, raison=DAMAGED
- **Après:** Stock diminue de 3
- **Visible par le client:** ❌ Non
- **Visible par l'admin:** ✅ Oui

### Scénario 3: Admin réapprovisionne
- **Fournisseur livre:** 200 unités de Doliprane
- **Admin enregistre:** Mouvement IN, quantité=200, raison=PURCHASE
- **Après:** Stock augmente de 200
- **Client voit:** ✅ Quantité disponible mise à jour
- **Client voit l'historique:** ❌ Non

---

## 🚨 Alertes de Stock

### Types d'alertes (visibles uniquement par ADMIN)

1. **LOW_STOCK**
   - Déclenchée quand: `quantity < lowStockThreshold`
   - Badge rouge avec icône warning
   - Message: "Stock bas: seulement X unités restantes"

2. **EXPIRED**
   - Déclenchée quand: `expirationDate < today`
   - Badge orange avec icône schedule
   - Message: "Produit expiré depuis le [date]"

### Résolution des alertes
- Admin peut marquer une alerte comme RESOLVED
- Alerte reste dans l'historique mais devient grise
- Nouvelle alerte créée si condition se reproduit

---

## ✅ Checklist de Sécurité

- [x] Routes protégées avec RoleGuard
- [x] Affichage conditionnel dans les templates
- [x] Vérification du rôle avant chargement des données sensibles
- [x] Messages informatifs pour les utilisateurs non autorisés
- [x] Menu de navigation adapté selon le rôle
- [x] Mouvements automatiques lors des achats
- [ ] **À vérifier:** Protection backend avec @PreAuthorize
- [ ] **À vérifier:** JWT contient bien le rôle de l'utilisateur
- [ ] **À vérifier:** Backend valide les permissions sur tous les endpoints

---

## 🔧 Configuration Recommandée Backend

Pour assurer une sécurité complète, vérifiez que votre backend Spring Boot implémente:

```java
@Configuration
@EnableWebSecurity
@EnableGlobalMethodSecurity(prePostEnabled = true)
public class SecurityConfig {
    // Configuration de sécurité
}

@RestController
@RequestMapping("/api/stock")
public class StockController {
    
    @GetMapping("/products")
    public List<Product> getAllProducts() {
        // Public - tous les utilisateurs authentifiés
    }
    
    @PostMapping("/products")
    @PreAuthorize("hasRole('ADMIN')")
    public Product createProduct(@RequestBody Product product) {
        // Protégé - admin uniquement
    }
    
    @GetMapping("/movements/product/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public List<StockMovement> getMovements(@PathVariable Long id) {
        // Protégé - admin uniquement
    }
}
```

---

## 📞 Support

En cas de problème avec les permissions:
1. Vérifier que le JWT contient le bon rôle
2. Vérifier les logs du RoleGuard
3. Tester avec un utilisateur ADMIN connu
4. Vérifier les réponses HTTP (403 = Forbidden)
5. Consulter les logs backend pour les erreurs d'autorisation
