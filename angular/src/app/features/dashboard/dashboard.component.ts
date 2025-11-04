import { Component, OnInit } from '@angular/core';
import { ApiService } from '../../core/services/api.service';
import { forkJoin, catchError, of } from 'rxjs';

@Component({
  selector: 'app-dashboard',
  templateUrl: './dashboard.component.html',
  styleUrls: ['./dashboard.component.scss']
})
export class DashboardComponent implements OnInit {
  stats = {
    totalUsers: 0,
    totalProducts: 0,
    totalOrders: 0,
    totalRevenue: 0
  };

  loading = true;

  constructor(private apiService: ApiService) { }

  ngOnInit() {
    this.loadDashboardData();
  }

  loadDashboardData() {
    this.loading = true;
    
    // Fetch real data from multiple endpoints
    forkJoin({
      userStats: this.apiService.getUserStatistics().pipe(
        catchError(error => {
          console.error('Error loading user statistics:', error);
          return of({ totalUsers: 0 });
        })
      ),
      transactionStats: this.apiService.getAllTransactionStatistics().pipe(
        catchError(error => {
          console.error('Error loading transaction statistics:', error);
          return of({ totalTransactions: 0, totalAmount: 0 });
        })
      )
    }).subscribe({
      next: (data) => {
        // Update with real data
        this.stats.totalUsers = data.userStats?.totalUsers || 0;
        this.stats.totalOrders = data.transactionStats?.totalTransactions || 0;
        this.stats.totalRevenue = data.transactionStats?.totalAmount || 0;
        
        // Keep products static for now (no endpoint available)
        this.stats.totalProducts = 45;
        
        this.loading = false;
      },
      error: (error) => {
        console.error('Error loading dashboard data:', error);
        this.loading = false;
      }
    });
  }
}
