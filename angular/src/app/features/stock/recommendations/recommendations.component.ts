import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { Product } from '../../../models/product.model';
import { ApiService } from '../../../core/services/api.service';
import { AuthService } from '../../../core/services/auth.service';
import { NotificationService } from '../../../core/services/notification.service';
import { CartService } from '../../../core/services/cart.service';

@Component({
  selector: 'app-recommendations',
  templateUrl: './recommendations.component.html',
  styleUrls: ['./recommendations.component.scss']
})
export class RecommendationsComponent implements OnInit {
  recommendedProducts: Product[] = [];
  loading: boolean = false;

  constructor(
    private apiService: ApiService,
    private authService: AuthService,
    private notificationService: NotificationService,
    private cartService: CartService,
    private router: Router
  ) {}

  ngOnInit(): void {
    if (!this.authService.isAuthenticated()) {
      this.router.navigate(['/auth/login']);
      return;
    }
    this.loadRecommendations();
  }

  loadRecommendations(): void {
    this.loading = true;
    console.log('DEBUG RecommendationsComponent: Loading recommendations...');
    this.apiService.getUserRecommendations(10).subscribe({
      next: (response) => {
        console.log('DEBUG RecommendationsComponent: Received response:', response);
        console.log('DEBUG RecommendationsComponent: Recommendations count:', response.count);
        console.log('DEBUG RecommendationsComponent: Recommendations:', response.recommendations);
        this.recommendedProducts = response.recommendations || [];
        this.loading = false;
        if (this.recommendedProducts.length === 0) {
          console.log('DEBUG RecommendationsComponent: No recommendations in response');
          this.notificationService.showInfo('No recommendations available. Make some purchases to get personalized recommendations!');
        } else {
          console.log('DEBUG RecommendationsComponent: Loaded', this.recommendedProducts.length, 'recommendations');
        }
      },
      error: (error) => {
        console.error('DEBUG RecommendationsComponent: Error loading recommendations:', error);
        console.error('DEBUG RecommendationsComponent: Error details:', JSON.stringify(error, null, 2));
        this.notificationService.showError('Failed to load recommendations');
        this.loading = false;
      }
    });
  }

  viewDetails(productId: number | undefined): void {
    if (!productId) {
      this.notificationService.showError('Product ID not available');
      return;
    }
    this.router.navigate(['/stock/products', productId]);
  }

  addToCart(product: Product): void {
    try {
      // Check if product is available
      if (product.quantity <= 0) {
        this.notificationService.showError('Product out of stock');
        return;
      }

      // Check if product is expired
      if (this.isExpired(product)) {
        this.notificationService.showError('This product is expired');
        return;
      }

      // Add to cart with quantity 1
      this.cartService.addToCart(product, 1);
      this.notificationService.showSuccess(`${product.name} added to cart`);
    } catch (error: any) {
      this.notificationService.showError(error.message || 'Error adding to cart');
    }
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

  refreshRecommendations(): void {
    this.loadRecommendations();
  }
}

