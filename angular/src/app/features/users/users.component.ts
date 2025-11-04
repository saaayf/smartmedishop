import { Component, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { MatDialog } from '@angular/material/dialog';
import { ColumnDefinition } from '../../shared/components/data-table/data-table.component';
import { ApiService } from '../../core/services/api.service';
import { NotificationService } from '../../core/services/notification.service';
import { AuthService } from '../../core/services/auth.service';
import { UserDetailsDialogComponent } from './user-details-dialog.component';
import { UserEditDialogComponent } from './user-edit-dialog.component';

export interface User {
  id: number;
  username: string;
  email: string;
  firstName: string;
  lastName: string;
  userType: string;
  isActive: boolean;
  isVerified: boolean;
  riskProfile: string;
  fraudCount: number;
  totalTransactions: number;
  averageAmount: number;
  registrationDate: string;
  lastLogin: string;
}

@Component({
  selector: 'app-users',
  templateUrl: './users.component.html',
  styleUrls: ['./users.component.scss']
})
export class UsersComponent implements OnInit {
  users: User[] = [];
  columns: ColumnDefinition[] = [
    { key: 'id', title: 'ID', type: 'number', sortable: true, width: '80px' },
    { key: 'username', title: 'Username', type: 'text', sortable: true },
    { key: 'email', title: 'Email', type: 'text', sortable: true },
    { key: 'firstName', title: 'First Name', type: 'text', sortable: true },
    { key: 'lastName', title: 'Last Name', type: 'text', sortable: true },
    { key: 'userType', title: 'Role', type: 'text', sortable: true },
    { key: 'riskProfile', title: 'Risk Profile', type: 'text', sortable: true },
    { key: 'isActive', title: 'Active', type: 'boolean', sortable: true, width: '100px' }
  ];
  loading = false;
  showCreateForm = false;
  userForm: FormGroup;
  statistics: any = {};
  searchTerm = '';
  currentPage = 0;
  pageSize = 10;
  totalItems = 0;
  totalPages = 0;
  maxDate!: Date;
  minDate!: Date;

  isAdmin: boolean = false;

  constructor(
    private apiService: ApiService,
    private notificationService: NotificationService,
    private fb: FormBuilder,
    private dialog: MatDialog,
    private authService: AuthService
  ) {
    // Initialize date picker restrictions (no age limit, just reasonable date range)
    const today = new Date();
    this.maxDate = today; // Cannot select future dates
    this.minDate = new Date(today.getFullYear() - 120, today.getMonth(), today.getDate()); // No more than 120 years ago
    
    this.userForm = this.fb.group({
      username: ['', [Validators.required, Validators.minLength(3)]],
      email: ['', [Validators.required, Validators.email]],
      password: ['', [Validators.required, Validators.minLength(6)]],
      firstName: ['', [Validators.required]],
      lastName: ['', [Validators.required]],
      birthDate: ['', [Validators.required]],
      userType: ['CUSTOMER', [Validators.required]],
      isActive: [true],
      isVerified: [false]
    });
  }

  ngOnInit() {
    this.checkUserRole();
    this.loadUsers();
    this.loadStatistics();
  }

  checkUserRole() {
    this.isAdmin = this.authService.hasRole('ADMIN');
  }

  loadUsers() {
    this.loading = true;
    const params = {
      page: this.currentPage,
      size: this.pageSize,
      sortBy: 'id',
      sortDir: 'asc',
      search: this.searchTerm
    };

    this.apiService.getUsers(params).subscribe({
      next: (response: any) => {
        this.users = response.users;
        this.totalItems = response.totalItems;
        this.totalPages = response.totalPages;
        this.loading = false;
      },
      error: (error) => {
        this.notificationService.showError('Failed to load users');
        this.loading = false;
        console.error('Error loading users:', error);
      }
    });
  }

  loadStatistics() {
    this.apiService.getUserStatistics().subscribe({
      next: (stats) => {
        this.statistics = stats;
      },
      error: (error) => {
        console.error('Error loading user statistics:', error);
      }
    });
  }

  onEdit(user: User) {
    // Fetch full user details first
    this.apiService.getUser(user.id).subscribe({
      next: (fullUser) => {
        const dialogRef = this.dialog.open(UserEditDialogComponent, {
          width: '600px',
          maxWidth: '90vw',
          data: fullUser
        });

        dialogRef.afterClosed().subscribe(result => {
          if (result) {
            // User was updated, reload the list
            this.loadUsers();
            this.loadStatistics();
          }
        });
      },
      error: (error) => {
        // Check if it's a 404 (user not found) or other error
        if (error.status === 404) {
          this.notificationService.showError(`User with ID ${user.id} not found`);
        } else if (error.status === 0) {
          this.notificationService.showError('Cannot connect to server. Please check if the backend is running.');
        } else {
          this.notificationService.showError('Failed to load user details: ' + (error.message || 'Unknown error'));
        }
        console.error('Error loading user:', error);
      }
    });
  }

  onDelete(user: User) {
    if (confirm(`Are you sure you want to delete user ${user.username}? This action cannot be undone.`)) {
      this.loading = true;
      this.apiService.deleteUser(user.id).subscribe({
        next: () => {
          this.notificationService.showSuccess('User deleted successfully');
          this.loadUsers();
          this.loadStatistics();
          this.loading = false;
        },
        error: (error) => {
          this.notificationService.showError('Failed to delete user');
          this.loading = false;
          console.error('Error deleting user:', error);
        }
      });
    }
  }

  onView(user: User) {
    // Fetch full user details first
    this.apiService.getUser(user.id).subscribe({
      next: (fullUser) => {
        const dialogRef = this.dialog.open(UserDetailsDialogComponent, {
          width: '700px',
          maxWidth: '90vw',
          data: fullUser
        });

        dialogRef.afterClosed().subscribe(() => {
          console.log('User details dialog closed');
        });
      },
      error: (error) => {
        // Check if it's a 404 (user not found) or other error
        if (error.status === 404) {
          this.notificationService.showError(`User with ID ${user.id} not found`);
        } else if (error.status === 0) {
          this.notificationService.showError('Cannot connect to server. Please check if the backend is running.');
        } else {
          this.notificationService.showError('Failed to load user details: ' + (error.message || 'Unknown error'));
        }
        console.error('Error loading user:', error);
      }
    });
  }

  onCreateUser() {
    if (this.userForm.valid) {
      this.loading = true;
      const formData = this.userForm.value;
      const userData = {
        ...formData,
        birthDate: formData.birthDate ? new Date(formData.birthDate).toISOString().split('T')[0] : null // Format as YYYY-MM-DD
      };
      
      this.apiService.createUser(userData).subscribe({
        next: () => {
          this.notificationService.showSuccess('User created successfully');
          this.loadUsers();
          this.loadStatistics();
          this.showCreateForm = false;
          this.userForm.reset();
          this.loading = false;
        },
        error: (error) => {
          this.notificationService.showError('Failed to create user');
          this.loading = false;
          console.error('Error creating user:', error);
        }
      });
    } else {
      this.notificationService.showWarning('Please fill in all required fields');
    }
  }

  toggleCreateForm() {
    this.showCreateForm = !this.showCreateForm;
    if (!this.showCreateForm) {
      this.userForm.reset();
    }
  }

  onSearch() {
    this.currentPage = 0;
    this.loadUsers();
  }

  onPageChange(page: number) {
    this.currentPage = page;
    this.loadUsers();
  }

  onPageSizeChange(size: number) {
    this.pageSize = size;
    this.currentPage = 0;
    this.loadUsers();
  }

  activateUser(user: User) {
    this.apiService.activateUser(user.id).subscribe({
      next: () => {
        this.notificationService.showSuccess('User activated successfully');
        this.loadUsers();
      },
      error: (error) => {
        this.notificationService.showError('Failed to activate user');
        console.error('Error activating user:', error);
      }
    });
  }

  deactivateUser(user: User) {
    this.apiService.deactivateUser(user.id).subscribe({
      next: () => {
        this.notificationService.showSuccess('User deactivated successfully');
        this.loadUsers();
      },
      error: (error) => {
        this.notificationService.showError('Failed to deactivate user');
        console.error('Error deactivating user:', error);
      }
    });
  }

  getRiskProfileColor(riskProfile: string): string {
    switch (riskProfile) {
      case 'LOW': return '#4caf50';
      case 'MEDIUM': return '#ff9800';
      case 'HIGH': return '#f44336';
      case 'CRITICAL': return '#9c27b0';
      default: return '#666';
    }
  }
}
