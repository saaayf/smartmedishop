import { Component, Inject, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { ApiService } from '../../core/services/api.service';
import { NotificationService } from '../../core/services/notification.service';

@Component({
  selector: 'app-user-edit-dialog',
  templateUrl: './user-edit-dialog.component.html',
  styleUrls: ['./user-edit-dialog.component.scss']
})
export class UserEditDialogComponent implements OnInit {
  userForm: FormGroup;
  user: any;
  maxDate!: Date;
  minDate!: Date;
  loading = false;

  constructor(
    public dialogRef: MatDialogRef<UserEditDialogComponent>,
    @Inject(MAT_DIALOG_DATA) public data: any,
    private fb: FormBuilder,
    private apiService: ApiService,
    private notificationService: NotificationService
  ) {
    this.user = data;
    
    // Initialize date picker restrictions
    const today = new Date();
    this.maxDate = today;
    this.minDate = new Date(today.getFullYear() - 120, today.getMonth(), today.getDate());
    
    this.userForm = this.fb.group({
      firstName: ['', [Validators.required]],
      lastName: ['', [Validators.required]],
      email: ['', [Validators.required, Validators.email]],
      phone: [''],
      birthDate: ['', [Validators.required]],
      userType: ['', [Validators.required]],
      isActive: [true],
      isVerified: [false],
      riskProfile: ['', [Validators.required]]
    });
  }

  ngOnInit() {
    // Populate form with user data
    if (this.user) {
      const birthDate = this.user.birthDate ? new Date(this.user.birthDate) : null;
      this.userForm.patchValue({
        firstName: this.user.firstName || '',
        lastName: this.user.lastName || '',
        email: this.user.email || '',
        phone: this.user.phone || '',
        birthDate: birthDate,
        userType: this.user.userType || 'CUSTOMER',
        isActive: this.user.isActive !== undefined ? this.user.isActive : true,
        isVerified: this.user.isVerified !== undefined ? this.user.isVerified : false,
        riskProfile: this.user.riskProfile || 'LOW'
      });
    }
  }

  onCancel(): void {
    this.dialogRef.close();
  }

  onSave(): void {
    if (this.userForm.valid) {
      this.loading = true;
      const formData = this.userForm.value;
      const userData = {
        ...formData,
        birthDate: formData.birthDate ? new Date(formData.birthDate).toISOString().split('T')[0] : null
      };

      this.apiService.updateUser(this.user.id, userData).subscribe({
        next: (updatedUser) => {
          this.notificationService.showSuccess('User updated successfully');
          this.dialogRef.close(updatedUser);
          this.loading = false;
        },
        error: (error) => {
          this.notificationService.showError('Failed to update user');
          this.loading = false;
          console.error('Error updating user:', error);
        }
      });
    } else {
      this.notificationService.showWarning('Please fill in all required fields');
    }
  }
}

