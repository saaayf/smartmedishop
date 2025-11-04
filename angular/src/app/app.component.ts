import { Component, OnInit, OnDestroy } from '@angular/core';
import { Router } from '@angular/router';
import { Subscription, interval } from 'rxjs';
import { AuthService } from './core/services/auth.service';
import { ApiService, FraudAlert } from './core/services/api.service';
import { CartService } from './core/services/cart.service';

@Component({
  selector: 'app-root',
  templateUrl: './app.component.html',
  styleUrls: ['./app.component.scss']
})
export class AppComponent implements OnInit, OnDestroy {
  title = 'SmartMediShop';
  activeFraudAlertsCount: number = 0;
  cartItemsCount: number = 0;
  private alertsSubscription?: Subscription;
  private refreshInterval?: Subscription;
  private cartSubscription?: Subscription;

  constructor(
    public authService: AuthService,
    private router: Router,
    private apiService: ApiService,
    private cartService: CartService
  ) {}

  ngOnInit() {
    // Load fraud alerts count when authenticated and user is FRAUD_ANALYST or ADMIN
    if (this.authService.isAuthenticated() && this.isFraudAnalystOrAdmin()) {
      this.loadFraudAlertsCount();
      
      // Refresh alerts count every 30 seconds
      this.refreshInterval = interval(30000).subscribe(() => {
        if (this.authService.isAuthenticated() && this.isFraudAnalystOrAdmin()) {
          this.loadFraudAlertsCount();
        }
      });
    }

    // Subscribe to authentication changes
    this.authService.currentUser$.subscribe(user => {
      if (user && this.isFraudAnalystOrAdmin()) {
        this.loadFraudAlertsCount();
      } else {
        this.activeFraudAlertsCount = 0;
      }
    });

    // Subscribe to cart changes
    this.cartSubscription = this.cartService.cartItems$.subscribe(items => {
      this.cartItemsCount = this.cartService.getItemCount();
    });
  }

  ngOnDestroy() {
    if (this.alertsSubscription) {
      this.alertsSubscription.unsubscribe();
    }
    if (this.refreshInterval) {
      this.refreshInterval.unsubscribe();
    }
    if (this.cartSubscription) {
      this.cartSubscription.unsubscribe();
    }
  }

  loadFraudAlertsCount() {
    if (!this.isFraudAnalystOrAdmin()) {
      this.activeFraudAlertsCount = 0;
      return;
    }

    this.alertsSubscription = this.apiService.getFraudAlerts().subscribe({
      next: (alerts: FraudAlert[]) => {
        // Count only active (unresolved) alerts
        this.activeFraudAlertsCount = alerts?.filter(
          alert => alert.status !== 'RESOLVED' && alert.status !== 'resolved'
        ).length || 0;
      },
      error: (error) => {
        console.error('Error loading fraud alerts count:', error);
        this.activeFraudAlertsCount = 0;
      }
    });
  }

  logout() {
    this.authService.logout();
    this.activeFraudAlertsCount = 0;
    this.cartService.clearCart();
    this.router.navigate(['/auth/login']);
  }

  canAccessUsers(): boolean {
    return this.authService.hasRole('ADMIN') || this.authService.hasRole('FRAUD_ANALYST');
  }

  isFraudAnalystOrAdmin(): boolean {
    return this.authService.hasRole('FRAUD_ANALYST') || this.authService.hasRole('ADMIN');
  }

  isAdmin(): boolean {
    return this.authService.hasRole('ADMIN');
  }

  navigateToTransactionTab(tabIndex: number) {
    console.log('🧭 Navigating to transaction tab:', tabIndex);
    this.router.navigate(['/payments'], { 
      queryParams: { tab: tabIndex },
      queryParamsHandling: 'merge'
    }).then(() => {
      console.log('✅ Navigation completed to tab:', tabIndex);
      // Refresh alerts count after navigation
      if (tabIndex === 4) { // Fraud Alerts tab
        setTimeout(() => this.loadFraudAlertsCount(), 1000);
      }
    }).catch(error => {
      console.error('❌ Navigation error:', error);
    });
  }
}
