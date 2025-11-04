import { Component, OnInit, OnDestroy, ChangeDetectorRef } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { ActivatedRoute, Router, NavigationEnd } from '@angular/router';
import { Subscription } from 'rxjs';
import { filter } from 'rxjs/operators';
import { MatDialog } from '@angular/material/dialog';
import { ColumnDefinition } from '../../shared/components/data-table/data-table.component';
import { ApiService, Transaction, TransactionResponse, User, FraudAlert } from '../../core/services/api.service';
import { NotificationService } from '../../core/services/notification.service';
import { AuthService } from '../../core/services/auth.service';
import { TransactionDetailsDialogComponent } from './transaction-details-dialog.component';
import { ResolveFraudAlertDialogComponent } from './resolve-fraud-alert-dialog.component';

@Component({
  selector: 'app-payments',
  templateUrl: './payments.component.html',
  styleUrls: ['./payments.component.scss']
})
export class PaymentsComponent implements OnInit, OnDestroy {
  transactions: Transaction[] = [];
  allTransactions: Transaction[] = [];
  userBehaviors: any[] = [];
  activeFraudAlertsCount: number = 0;
  users: User[] = [];
  fraudAlerts: FraudAlert[] = [];
  filteredFraudAlerts: FraudAlert[] = [];
  fraudAlertFilter: 'all' | 'active' | 'resolved' = 'all';
  
  columns: ColumnDefinition[] = [
    { key: 'id', title: 'ID', type: 'number', sortable: true, width: '80px' },
    { key: 'amount', title: 'Amount', type: 'currency', sortable: true },
    { key: 'paymentMethod', title: 'Method', type: 'text', sortable: true },
    { key: 'status', title: 'Status', type: 'text', sortable: true },
    { key: 'fraudScore', title: 'Fraud Score', type: 'number', sortable: true },
    { key: 'riskLevel', title: 'Risk Level', type: 'text', sortable: true },
    { key: 'isFraud', title: 'Is Fraud', type: 'boolean', sortable: true },
    { key: 'transactionDate', title: 'Date', type: 'date', sortable: true },
    { key: 'merchantName', title: 'Merchant', type: 'text', sortable: true }
  ];
  
  // For FRAUD_ANALYST view
  allTransactionsColumns: ColumnDefinition[] = [
    { key: 'id', title: 'ID', type: 'number', sortable: true, width: '80px' },
    { key: 'userId', title: 'User ID', type: 'number', sortable: true },
    { key: 'username', title: 'Username', type: 'text', sortable: true },
    { key: 'amount', title: 'Amount', type: 'currency', sortable: true },
    { key: 'paymentMethod', title: 'Method', type: 'text', sortable: true },
    { key: 'status', title: 'Status', type: 'text', sortable: true },
    { key: 'fraudScore', title: 'Fraud Score', type: 'number', sortable: true },
    { key: 'riskLevel', title: 'Risk Level', type: 'text', sortable: true },
    { key: 'isFraud', title: 'Is Fraud', type: 'boolean', sortable: true },
    { key: 'transactionDate', title: 'Date', type: 'date', sortable: true },
    { key: 'merchantName', title: 'Merchant', type: 'text', sortable: true }
  ];
  
  loading = false;
  showCreateForm = false;
  transactionForm: FormGroup;
  statistics: any = {};
  allStatistics: any = {};
  
  // FRAUD_ANALYST features
  isFraudAnalystOrAdmin = false;
  isFraudAnalyst = false;
  activeTab = 0;
  
  fraudAlertColumns: ColumnDefinition[] = [
    { key: 'id', title: 'Alert ID', type: 'number', sortable: true, width: '80px' },
    { key: 'transactionId', title: 'Transaction ID', type: 'number', sortable: true, width: '100px' },
    { key: 'username', title: 'Username', type: 'text', sortable: true },
    { key: 'amount', title: 'Amount', type: 'currency', sortable: true },
    { key: 'severity', title: 'Severity', type: 'text', sortable: true },
    { key: 'status', title: 'Status', type: 'text', sortable: true },
    { key: 'fraudScore', title: 'Fraud Score', type: 'number', sortable: true },
    { key: 'alertType', title: 'Alert Type', type: 'text', sortable: true },
    { key: 'createdAt', title: 'Created At', type: 'date', sortable: true },
    { key: 'resolvedAt', title: 'Resolved At', type: 'date', sortable: true },
    { key: 'resolvedBy', title: 'Resolved By', type: 'text', sortable: true },
    { key: 'investigationNotes', title: 'Investigation Notes', type: 'text', sortable: false }
  ];

  userBehaviorColumns: ColumnDefinition[] = [
    { key: 'userId', title: 'User ID', type: 'number', sortable: true, width: '100px' },
    { key: 'transactionVelocity', title: 'Transaction Velocity', type: 'number', sortable: true },
    { key: 'averageTransactionAmount', title: 'Avg Amount', type: 'currency', sortable: true },
    { key: 'maxTransactionAmount', title: 'Max Amount', type: 'currency', sortable: true },
    { key: 'minTransactionAmount', title: 'Min Amount', type: 'currency', sortable: true },
    { key: 'preferredPaymentMethod', title: 'Payment Method', type: 'text', sortable: true },
    { key: 'preferredDeviceType', title: 'Device Type', type: 'text', sortable: true },
    { key: 'locationCountry', title: 'Country', type: 'text', sortable: true },
    { key: 'unusualPatternsCount', title: 'Unusual Patterns', type: 'number', sortable: true }
  ];
  
  // All transactions pagination
  allTransactionsPage = 0;
  allTransactionsPageSize = 20;
  allTransactionsTotalItems = 0;
  allTransactionsTotalPages = 0;
  
  // Filters
  filterRiskLevel = '';
  filterIsFraud: boolean | null = null;
  selectedUserId: number | null = null;

  private queryParamsSubscription?: Subscription;
  private routerSubscription?: Subscription;

  constructor(
    private apiService: ApiService,
    private notificationService: NotificationService,
    private authService: AuthService,
    private fb: FormBuilder,
    private route: ActivatedRoute,
    private router: Router,
    private cdr: ChangeDetectorRef,
    private dialog: MatDialog
  ) {
    this.transactionForm = this.fb.group({
      amount: ['', [Validators.required, Validators.min(0.01)]],
      paymentMethod: ['credit_card', [Validators.required]],
      deviceType: ['desktop'],
      locationCountry: ['US'],
      merchantName: ['', [Validators.required]],
      transactionType: ['purchase']
    });
    
    // Check if user is FRAUD_ANALYST or ADMIN
    this.isFraudAnalystOrAdmin = this.authService.hasRole('FRAUD_ANALYST') || this.authService.hasRole('ADMIN');
    this.isFraudAnalyst = this.authService.hasRole('FRAUD_ANALYST');
    console.log('🔐 isFraudAnalystOrAdmin in constructor:', this.isFraudAnalystOrAdmin);
    console.log('🔐 isFraudAnalyst in constructor:', this.isFraudAnalyst);
    console.log('👤 Current user:', this.authService.getCurrentUser());
    console.log('🔍 User type:', this.authService.getCurrentUser()?.userType);
  }

  ngOnInit() {
    // Re-check isFraudAnalystOrAdmin in case auth wasn't ready in constructor
    this.updateIsFraudAnalystOrAdmin();
    
    // Load users if user is FRAUD_ANALYST or ADMIN
    if (this.isFraudAnalystOrAdmin) {
      this.loadUsers();
    }
    
    // Check initial query params
    this.checkQueryParamsAndLoadTab();
    
    // Listen to query param changes (including navigation events)
    this.queryParamsSubscription = this.route.queryParams.subscribe(params => {
      this.handleQueryParams(params);
      // For regular customers, reload transactions if not a tab param
      if (!this.isFraudAnalystOrAdmin && !params['tab']) {
        this.loadTransactions();
        this.calculateStatisticsFromTransactions();
      }
    });
    
    // Listen to router navigation events
    this.routerSubscription = this.router.events.pipe(
      filter(event => event instanceof NavigationEnd)
    ).subscribe(() => {
      this.updateIsFraudAnalystOrAdmin();
      this.checkQueryParamsAndLoadTab();
      
      // For regular customers, ensure transactions are loaded
      if (!this.isFraudAnalystOrAdmin) {
        this.loadTransactions();
        this.calculateStatisticsFromTransactions();
      }
    });
    
    // Subscribe to user changes to update isFraudAnalystOrAdmin
    this.authService.currentUser$.subscribe(user => {
      this.updateIsFraudAnalystOrAdmin();
      // Load transactions for regular customers when user is detected
      if (user && !this.isFraudAnalystOrAdmin) {
        this.loadTransactions();
        this.calculateStatisticsFromTransactions();
      }
    });
  }
  
  updateIsFraudAnalystOrAdmin() {
    const oldValue = this.isFraudAnalystOrAdmin;
    this.isFraudAnalystOrAdmin = this.authService.hasRole('FRAUD_ANALYST') || this.authService.hasRole('ADMIN');
    this.isFraudAnalyst = this.authService.hasRole('FRAUD_ANALYST');
    console.log('🔐 updateIsFraudAnalystOrAdmin - old:', oldValue, 'new:', this.isFraudAnalystOrAdmin);
    console.log('🔐 isFraudAnalyst:', this.isFraudAnalyst);
    console.log('👤 Current user:', this.authService.getCurrentUser());
    console.log('🔍 User type:', this.authService.getCurrentUser()?.userType);
    console.log('✅ Has FRAUD_ANALYST:', this.authService.hasRole('FRAUD_ANALYST'));
    console.log('✅ Has ADMIN:', this.authService.hasRole('ADMIN'));
    
    if (this.isFraudAnalystOrAdmin && !oldValue) {
      // User just became FRAUD_ANALYST/ADMIN, load users
      this.loadUsers();
    }
    
    // Force change detection
    this.cdr.detectChanges();
  }

  ngOnDestroy() {
    if (this.queryParamsSubscription) {
      this.queryParamsSubscription.unsubscribe();
    }
    if (this.routerSubscription) {
      this.routerSubscription.unsubscribe();
    }
  }

  checkQueryParamsAndLoadTab() {
    const params = this.route.snapshot.queryParams;
    this.handleQueryParams(params);
    
    // For regular customers (non-analysts), load their transactions if not already loading tabs
    if (!this.isFraudAnalystOrAdmin && !params['tab']) {
      console.log('👤 Regular customer - loading transactions');
      this.loadTransactions();
      this.calculateStatisticsFromTransactions();
    }
  }

  handleQueryParams(params: any) {
    // Only handle tabs for FRAUD_ANALYST/ADMIN
    if (!this.isFraudAnalystOrAdmin) {
      // Regular customers should just load their transactions
      return;
    }
    
    if (params['tab']) {
      const tabIndex = parseInt(params['tab'], 10);
      console.log('📌 handleQueryParams - tab param:', tabIndex, 'current activeTab:', this.activeTab);
      // For FRAUD_ANALYST/ADMIN, tabs are 0-4 (no "My Transactions" tab)
      if (!isNaN(tabIndex) && tabIndex >= 0 && tabIndex <= 4) {
        if (this.activeTab !== tabIndex) {
          console.log('🔄 Switching tab from', this.activeTab, 'to', tabIndex);
          this.activeTab = tabIndex;
          // Use setTimeout to ensure Angular Material tab group updates
          setTimeout(() => {
            this.cdr.detectChanges();
            // Load content for the selected tab
            this.loadTabContent(tabIndex);
          }, 0);
        } else {
          // Same tab, just ensure content is loaded
          console.log('✅ Same tab, loading content');
          this.loadTabContent(tabIndex);
        }
      }
    } else {
      // No tab param, load default (All Transactions for FRAUD_ANALYST/ADMIN)
      if (this.activeTab !== 0) {
        console.log('🏠 No tab param, setting to default tab 0 (All Transactions)');
        this.activeTab = 0;
        setTimeout(() => {
          this.cdr.detectChanges();
          this.loadTabContent(0);
        }, 0);
      }
    }
  }
  
  loadTabContent(tabIndex: number) {
    console.log('📥 Loading content for tab:', tabIndex);
    // For FRAUD_ANALYST/ADMIN, tabs are now 0-3 (no "My Transactions")
    switch(tabIndex) {
      case 0: // All Transactions
        console.log('📥 Loading All Transactions');
        this.loadAllTransactions();
        break;
      case 1: // Transactions by User
        console.log('📥 Loading Transactions by User');
        // Load users list for the dropdown
        if (this.users.length === 0) {
          this.loadUsers();
        }
        // Don't load transactions until user is selected
        // Clear previous results
        this.allTransactions = [];
        break;
      case 2: // All Statistics
        console.log('📥 Loading All Statistics');
        this.loadAllStatistics();
        break;
      case 3: // User Behaviors
        console.log('📥 Loading User Behaviors');
        this.loadAllUserBehaviors();
        break;
      case 4: // Fraud Alerts
        console.log('📥 Loading Fraud Alerts');
        this.loadFraudAlerts();
        break;
      default:
        // Fallback to All Transactions
        console.log('📥 Unknown tab, loading All Transactions');
        this.loadAllTransactions();
        break;
    }
  }

  onTabChange(index: number) {
    this.activeTab = index;
    
    // Update query params without triggering navigation
    this.router.navigate([], {
      relativeTo: this.route,
      queryParams: { tab: index },
      queryParamsHandling: 'merge',
      replaceUrl: true
    });
    
    // Load content for the selected tab
    this.loadTabContent(index);
  }

  loadUsers() {
    // Load all users with a large page size to get the complete list
    this.apiService.getUsers({ page: 0, size: 1000, sortBy: 'username', sortDir: 'asc' }).subscribe({
      next: (response: any) => {
        this.users = response.users || response;
        console.log('✅ Loaded users:', this.users.length);
      },
      error: (error) => {
        console.error('Error loading users:', error);
        this.notificationService.showError('Failed to load users. Please try again.');
      }
    });
  }

  loadTransactions() {
    this.loading = true;
    this.apiService.getMyTransactions().subscribe({
      next: (transactions) => {
        this.transactions = transactions;
        this.loading = false;
        this.calculateStatisticsFromTransactions();
      },
      error: (error) => {
        this.notificationService.showError('Failed to load transactions');
        this.loading = false;
        console.error('Error loading transactions:', error);
      }
    });
  }

  loadAllTransactions() {
    console.log('🔍 loadAllTransactions called');
    this.loading = true;
    const params: any = {
      page: this.allTransactionsPage,
      size: this.allTransactionsPageSize
    };
    
    if (this.filterRiskLevel) {
      params.riskLevel = this.filterRiskLevel;
    }
    
    if (this.filterIsFraud !== null) {
      params.isFraud = this.filterIsFraud;
    }
    
    if (this.selectedUserId) {
      params.userId = this.selectedUserId;
    }
    
    console.log('📡 Calling API getAllTransactions with params:', params);
    this.apiService.getAllTransactions(params).subscribe({
      next: (response: any) => {
        console.log('✅ getAllTransactions API Response:', response);
        this.allTransactions = response.transactions || [];
        this.allTransactionsTotalItems = response.totalItems || 0;
        this.allTransactionsTotalPages = response.totalPages || 0;
        console.log('📊 Loaded transactions:', this.allTransactions.length, 'Total items:', this.allTransactionsTotalItems);
        this.loading = false;
        this.cdr.detectChanges();
      },
      error: (error) => {
        console.error('❌ Error loading all transactions:', error);
        this.notificationService.showError('Failed to load all transactions');
        this.loading = false;
        this.cdr.detectChanges();
      }
    });
  }

  loadTransactionsByUser() {
    if (!this.selectedUserId) {
      this.notificationService.showWarning('Please select a user');
      return;
    }
    
    this.loading = true;
    this.apiService.getTransactionsByUserId(this.selectedUserId).subscribe({
      next: (transactions: Transaction[]) => {
        this.allTransactions = transactions;
        this.allTransactionsTotalItems = transactions.length;
        this.allTransactionsTotalPages = Math.ceil(transactions.length / this.allTransactionsPageSize);
        this.loading = false;
      },
      error: (error) => {
        this.notificationService.showError('Failed to load user transactions');
        this.loading = false;
        console.error('Error loading user transactions:', error);
      }
    });
  }

  loadStatistics() {
    this.apiService.getTransactionStatistics().subscribe({
      next: (stats) => {
        console.log('Statistics loaded:', stats);
        this.statistics = stats;
      },
      error: (error) => {
        console.error('Error loading statistics:', error);
        this.notificationService.showError('Failed to load statistics');
        this.calculateStatisticsFromTransactions();
      }
    });
  }

  loadAllStatistics() {
    this.loading = true;
    this.apiService.getAllTransactionStatistics().subscribe({
      next: (stats: any) => {
        this.allStatistics = stats;
        this.loading = false;
      },
      error: (error) => {
        this.notificationService.showError('Failed to load all statistics');
        this.loading = false;
        console.error('Error loading all statistics:', error);
      }
    });
  }

  loadAllUserBehaviors() {
    this.loading = true;
    this.apiService.getAllUserBehaviors().subscribe({
      next: (response: any) => {
        this.userBehaviors = response.userBehaviors || response || [];
        this.loading = false;
      },
      error: (error) => {
        this.notificationService.showError('Failed to load user behaviors');
        this.loading = false;
        console.error('Error loading user behaviors:', error);
      }
    });
  }

  loadFraudAlerts() {
    this.loading = true;
    this.apiService.getFraudAlerts().subscribe({
      next: (alerts: FraudAlert[]) => {
        this.fraudAlerts = alerts || [];
        // Calculate active (unresolved) alerts count
        this.activeFraudAlertsCount = alerts?.filter(
          alert => alert.status !== 'RESOLVED' && alert.status !== 'resolved'
        ).length || 0;
        // Apply filter
        this.applyFraudAlertFilter();
        this.loading = false;
      },
      error: (error) => {
        this.notificationService.showError('Failed to load fraud alerts');
        this.loading = false;
        console.error('Error loading fraud alerts:', error);
      }
    });
  }

  applyFraudAlertFilter() {
    if (!this.fraudAlerts || this.fraudAlerts.length === 0) {
      this.filteredFraudAlerts = [];
      return;
    }

    switch (this.fraudAlertFilter) {
      case 'active':
        this.filteredFraudAlerts = this.fraudAlerts.filter(
          alert => alert.status !== 'RESOLVED' && alert.status !== 'resolved'
        );
        break;
      case 'resolved':
        this.filteredFraudAlerts = this.fraudAlerts.filter(
          alert => alert.status === 'RESOLVED' || alert.status === 'resolved'
        );
        break;
      case 'all':
      default:
        this.filteredFraudAlerts = this.fraudAlerts;
        break;
    }
  }

  onFraudAlertFilterChange() {
    this.applyFraudAlertFilter();
  }

  onViewFraudAlert(alert: FraudAlert) {
    // Navigate to transaction details for the alert's transaction
    if (alert.transactionId) {
      this.loading = true;
      this.apiService.getTransaction(alert.transactionId).subscribe({
        next: (transaction: Transaction) => {
          this.loading = false;
          this.openTransactionDetailsDialog(transaction);
        },
        error: (error) => {
          this.loading = false;
          console.error('Error loading transaction for alert:', error);
          this.notificationService.showError('Failed to load transaction details');
        }
      });
    }
  }

  onResolveFraudAlert(alert: FraudAlert) {
    // Open dialog for investigation notes
    const dialogRef = this.dialog.open(ResolveFraudAlertDialogComponent, {
      width: '600px',
      maxWidth: '90vw',
      data: alert,
      disableClose: false
    });

    dialogRef.afterClosed().subscribe((investigationNotes?: string) => {
      if (investigationNotes !== undefined && investigationNotes !== null) {
        // User provided notes, resolve the alert
        this.loading = true;
        this.apiService.resolveFraudAlert(alert.id, investigationNotes).subscribe({
          next: (response) => {
            this.notificationService.showSuccess('Fraud alert resolved successfully');
            this.loadFraudAlerts(); // Reload alerts to show updated status (this will also apply the filter)
            this.loading = false;
            // Notify app component to refresh alert count
            // This will be handled by the auto-refresh mechanism
          },
          error: (error) => {
            this.notificationService.showError('Failed to resolve fraud alert');
            this.loading = false;
            console.error('Error resolving fraud alert:', error);
          }
        });
      }
    });
  }

  calculateStatisticsFromTransactions() {
    if (this.transactions && this.transactions.length > 0) {
      const totalTransactions = this.transactions.length;
      const totalAmount = this.transactions.reduce((sum, transaction) => sum + (transaction.amount || 0), 0);
      const averageAmount = totalAmount / totalTransactions;

      this.statistics = {
        totalTransactions: totalTransactions,
        totalAmount: totalAmount,
        averageAmount: averageAmount
      };
    }
  }

  onCreateTransaction() {
    if (this.transactionForm.valid) {
      this.loading = true;
      const transactionData = this.transactionForm.value;
      
      this.apiService.createTransaction(transactionData).subscribe({
        next: (response: TransactionResponse) => {
          this.notificationService.showSuccess('Transaction created successfully!');
          this.loadTransactions();
          this.loadStatistics();
          this.showCreateForm = false;
          this.transactionForm.reset();
          this.loading = false;
          
          if (response.isFraud) {
            this.notificationService.showWarning(`⚠️ FRAUD DETECTED! Risk Level: ${response.riskLevel}, Score: ${response.fraudScore}`);
          } else {
            this.notificationService.showInfo(`✅ Transaction approved. Risk Level: ${response.riskLevel}, Score: ${response.fraudScore}`);
          }
        },
        error: (error) => {
          this.notificationService.showError('Failed to create transaction');
          this.loading = false;
          console.error('Error creating transaction:', error);
        }
      });
    } else {
      this.notificationService.showWarning('Please fill in all required fields');
    }
  }


  onView(transaction: any) {
    console.log('View transaction details:', transaction);
    if (!transaction || !transaction.id) {
      this.notificationService.showError('Invalid transaction');
      return;
    }
    
    // Load full transaction details
    this.loading = true;
    this.apiService.getTransaction(transaction.id).subscribe({
      next: (fullTransaction: Transaction) => {
        this.loading = false;
        // Open dialog with transaction details
        this.openTransactionDetailsDialog(fullTransaction);
      },
      error: (error) => {
        this.loading = false;
        console.error('Error loading transaction details:', error);
        this.notificationService.showError('Failed to load transaction details');
        // If API fails, show available data
        this.openTransactionDetailsDialog(transaction);
      }
    });
  }

  openTransactionDetailsDialog(transaction: any) {
    const dialogRef = this.dialog.open(TransactionDetailsDialogComponent, {
      width: '600px',
      maxWidth: '90vw',
      data: transaction
    });

    dialogRef.afterClosed().subscribe(result => {
      console.log('Transaction details dialog closed');
    });
  }

  onProcessTransaction(transaction: any) {
    this.apiService.processTransaction(transaction.id).subscribe({
      next: (response) => {
        this.notificationService.showSuccess('Transaction processed successfully!');
        this.loadTransactions();
        if (this.activeTab === 1) {
          this.loadAllTransactions();
        }
      },
      error: (error) => {
        this.notificationService.showError('Failed to process transaction');
        console.error('Error processing transaction:', error);
      }
    });
  }

  toggleCreateForm() {
    this.showCreateForm = !this.showCreateForm;
    if (!this.showCreateForm) {
      this.transactionForm.reset();
    }
  }

  onAllTransactionsPageChange(event: any) {
    this.allTransactionsPage = event.pageIndex;
    this.allTransactionsPageSize = event.pageSize;
    this.loadAllTransactions();
  }

  applyFilters() {
    this.allTransactionsPage = 0;
    if (this.activeTab === 1 && this.selectedUserId) {
      this.loadTransactionsByUser();
    } else {
      this.loadAllTransactions();
    }
  }
  
  clearFilters() {
    this.filterRiskLevel = '';
    this.filterIsFraud = null;
    this.selectedUserId = null;
    this.allTransactionsPage = 0;
    if (this.activeTab === 1) {
      this.allTransactions = [];
    } else {
      this.loadAllTransactions();
    }
  }

  getRiskLevelColor(riskLevel: string): string {
    switch (riskLevel?.toUpperCase()) {
      case 'LOW': return 'green';
      case 'MEDIUM': return 'orange';
      case 'HIGH': return 'red';
      case 'CRITICAL': return 'darkred';
      default: return 'gray';
    }
  }

  getFraudScoreColor(score: number): string {
    if (score < 0.3) return 'green';
    if (score < 0.6) return 'orange';
    return 'red';
  }

  getRiskDistributionPercentage(value: number, distribution: any): number {
    if (!distribution || !value) return 0;
    const total = (distribution.LOW || 0) + (distribution.MEDIUM || 0) + (distribution.HIGH || 0) + (distribution.CRITICAL || 0);
    if (total === 0) return 0;
    return (value / total) * 100;
  }
}
