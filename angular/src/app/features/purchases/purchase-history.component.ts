import { Component, OnInit } from '@angular/core';
import { ApiService } from '../../core/services/api.service';
import { NotificationService } from '../../core/services/notification.service';
import { UserPurchase } from '../../models/user-purchase.model';

@Component({
  selector: 'app-purchase-history',
  templateUrl: './purchase-history.component.html',
  styleUrls: ['./purchase-history.component.scss']
})
export class PurchaseHistoryComponent implements OnInit {
  purchases: UserPurchase[] = [];
  loading = false;

  displayedColumns: string[] = ['stockId', 'name', 'marque', 'type', 'state', 'price', 'quantity', 'purchaseDate'];

  constructor(
    private apiService: ApiService,
    private notificationService: NotificationService
  ) {}

  ngOnInit(): void {
    this.loadPurchases();
  }

  loadPurchases(): void {
    this.loading = true;
    this.apiService.getMyPurchases().subscribe({
      next: (purchases: UserPurchase[]) => {
        this.purchases = purchases;
        this.loading = false;
      },
      error: (error) => {
        console.error('Error loading purchases:', error);
        this.notificationService.showError('Erreur lors du chargement de l\'historique des achats');
        this.loading = false;
      }
    });
  }

  getStateClass(state: string | undefined): string {
    if (!state) return '';
    const stateUpper = state.toUpperCase();
    if (stateUpper === 'COMPLETED' || stateUpper === 'APPROVED') return 'state-completed';
    if (stateUpper === 'PENDING') return 'state-pending';
    if (stateUpper === 'FAILED' || stateUpper === 'CANCELLED') return 'state-failed';
    return '';
  }
}

