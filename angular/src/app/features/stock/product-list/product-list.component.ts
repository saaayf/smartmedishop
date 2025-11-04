import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { Product } from '../../../models/product.model';
import { StockService } from '../../../core/services/stock.service';
import { AuthService } from '../../../core/services/auth.service';
import { NotificationService } from '../../../core/services/notification.service';
import { CartService } from '../../../core/services/cart.service';
import { MatDialog } from '@angular/material/dialog';

@Component({
  selector: 'app-product-list',
  templateUrl: './product-list.component.html',
  styleUrls: ['./product-list.component.scss']
})
export class ProductListComponent implements OnInit {
  products: Product[] = [];
  filteredProducts: Product[] = [];
  searchTerm: string = '';
  loading: boolean = false;
  displayedColumns: string[] = ['sku', 'name', 'marque', 'type', 'description', 'quantity', 'threshold', 'price', 'expirationDate', 'status', 'actions'];

  constructor(
    private stockService: StockService,
    private authService: AuthService,
    private notificationService: NotificationService,
    private cartService: CartService,
    private router: Router,
    private dialog: MatDialog
  ) {}

  ngOnInit(): void {
    this.loadProducts();
  }

  loadProducts(): void {
    this.loading = true;
    this.stockService.getAllProducts().subscribe({
      next: (products) => {
        this.products = products;
        this.filteredProducts = products;
        this.loading = false;
      },
      error: (error) => {
        console.error('Error loading products:', error);
        this.notificationService.showError('Erreur lors du chargement des produits');
        this.loading = false;
      }
    });
  }

  filterProducts(): void {
    const term = this.searchTerm.toLowerCase().trim();
    if (!term) {
      this.filteredProducts = this.products;
      return;
    }
    
    this.filteredProducts = this.products.filter(product =>
      product.name.toLowerCase().includes(term) ||
      product.sku.toLowerCase().includes(term)
    );
  }

  isAdmin(): boolean {
    return this.authService.hasRole('ADMIN');
  }

  isAdminOrFraudAnalyst(): boolean {
    return this.authService.hasRole('ADMIN') || this.authService.hasRole('FRAUD_ANALYST');
  }

  isLowStock(product: Product): boolean {
    return product.quantity < product.lowStockThreshold;
  }

  isExpired(product: Product): boolean {
    if (!product.expirationDate) return false;
    const expirationDate = new Date(product.expirationDate);
    const today = new Date();
    today.setHours(0, 0, 0, 0);
    return expirationDate < today;
  }

  viewDetails(productId: number | undefined): void {
    if (!productId) {
      this.notificationService.showError('ID du produit non disponible');
      return;
    }
    this.router.navigate(['/stock/products', productId]);
  }

  editProduct(productId: number | undefined): void {
    if (!productId) {
      this.notificationService.showError('ID du produit non disponible');
      return;
    }
    this.router.navigate(['/stock/products', productId, 'edit']);
  }

  addToCart(product: Product): void {
    try {
      // Check if product is available
      if (product.quantity <= 0) {
        this.notificationService.showError('Produit en rupture de stock');
        return;
      }

      // Check if product is expired
      if (this.isExpired(product)) {
        this.notificationService.showError('Ce produit est expiré');
        return;
      }

      // Add to cart with quantity 1
      this.cartService.addToCart(product, 1);
      this.notificationService.showSuccess(`${product.name} ajouté au panier`);
    } catch (error: any) {
      this.notificationService.showError(error.message || 'Erreur lors de l\'ajout au panier');
    }
  }

  createProduct(): void {
    this.router.navigate(['/stock/products/new']);
  }

  truncateDescription(description: string): string {
    if (description.length <= 50) return description;
    return description.substring(0, 50) + '...';
  }
}
