import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { Product } from '../../../models/product.model';
import { StockMovement } from '../../../models/stock-movement.model';
import { StockAlert } from '../../../models/stock-alert.model';
import { StockService } from '../../../core/services/stock.service';
import { AuthService } from '../../../core/services/auth.service';
import { NotificationService } from '../../../core/services/notification.service';

@Component({
  selector: 'app-product-detail',
  templateUrl: './product-detail.component.html',
  styleUrls: ['./product-detail.component.scss']
})
export class ProductDetailComponent implements OnInit {
  product: Product | null = null;
  movements: StockMovement[] = [];
  alerts: StockAlert[] = [];
  loading: boolean = false;
  loadingMovements: boolean = false;
  loadingAlerts: boolean = false;

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private stockService: StockService,
    private authService: AuthService,
    private notificationService: NotificationService
  ) {}

  ngOnInit(): void {
    const id = this.route.snapshot.paramMap.get('id');
    if (id) {
      this.loadProduct(+id);
    }
  }

  loadProduct(id: number): void {
    this.loading = true;
    this.stockService.getProductById(id).subscribe({
      next: (product) => {
        this.product = product;
        this.loading = false;
        // Only load movements and alerts if user is admin
        if (this.isAdmin()) {
          this.loadMovements(id);
          this.loadAlerts(id);
        }
      },
      error: (error) => {
        console.error('Error loading product:', error);
        this.notificationService.showError('Erreur lors du chargement du produit');
        this.loading = false;
        this.router.navigate(['/stock/products']);
      }
    });
  }

  loadMovements(productId: number): void {
    this.loadingMovements = true;
    this.stockService.getMovementsByProduct(productId).subscribe({
      next: (movements) => {
        this.movements = movements;
        this.loadingMovements = false;
      },
      error: (error) => {
        console.error('Error loading movements:', error);
        this.loadingMovements = false;
      }
    });
  }

  loadAlerts(productId: number): void {
    this.loadingAlerts = true;
    console.log('🔍 Loading alerts for product ID:', productId);
    
    this.stockService.getAlertsByProduct(productId).subscribe({
      next: (alerts) => {
        console.log('✅ Alerts received from backend:', alerts);
        console.log('Number of alerts:', alerts.length);
        this.alerts = alerts;
        this.loadingAlerts = false;
      },
      error: (error) => {
        console.error('❌ Error loading alerts:', error);
        console.error('Error details:', {
          status: error.status,
          message: error.message,
          url: error.url
        });
        this.alerts = [];
        this.loadingAlerts = false;
      }
    });
  }

  isAdmin(): boolean {
    return this.authService.hasRole('ADMIN');
  }

  isLowStock(): boolean {
    if (!this.product) return false;
    return this.product.quantity < this.product.lowStockThreshold;
  }

  isExpired(): boolean {
    if (!this.product || !this.product.expirationDate) return false;
    const expirationDate = new Date(this.product.expirationDate);
    const today = new Date();
    today.setHours(0, 0, 0, 0);
    return expirationDate < today;
  }

  editProduct(): void {
    if (this.product && this.product.id) {
      this.router.navigate(['/stock/products', this.product.id, 'edit']);
    }
  }

  openPrediction(): void {
    if (this.product && this.product.id) {
      this.router.navigate(['/stock/products', this.product.id, 'predict']);
    }
  }

  goBack(): void {
    this.router.navigate(['/stock/products']);
  }

  getMovementTypeClass(type: string): string {
    return type === 'IN' ? 'movement-in' : 'movement-out';
  }

  getMovementTypeLabel(type: string): string {
    return type === 'IN' ? 'Entrée' : 'Sortie';
  }

  getAlertTypeClass(status: string): string {
    return status === 'ACTIVE' ? 'alert-active' : 'alert-resolved';
  }

  getAlertTypeLabel(status: string): string {
    return status === 'ACTIVE' ? 'Active' : 'Résolue';
  }

  getReasonLabel(reason: string): string {
    const labels: { [key: string]: string } = {
      'PURCHASE': 'Achat',
      'SALE': 'Vente',
      'RETURN': 'Retour',
      'MANUAL': 'Ajustement manuel',
      'EXPIRED': 'Produit expiré',
      'DAMAGED': 'Produit endommagé'
    };
    return labels[reason] || reason;
  }
}
