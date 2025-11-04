import { Component, Inject, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { ApiService, TransactionResponse } from '../../core/services/api.service';
import { NotificationService } from '../../core/services/notification.service';

@Component({
  selector: 'app-create-transaction-dialog',
  templateUrl: './create-transaction-dialog.component.html',
  styleUrls: ['./create-transaction-dialog.component.scss']
})
export class CreateTransactionDialogComponent implements OnInit {
  transactionForm: FormGroup;
  loading = false;

  constructor(
    private fb: FormBuilder,
    private dialogRef: MatDialogRef<CreateTransactionDialogComponent>,
    @Inject(MAT_DIALOG_DATA) public data: { amount: number },
    private apiService: ApiService,
    private notificationService: NotificationService
  ) {
    this.transactionForm = this.fb.group({
      amount: ['', [Validators.required, Validators.min(0.01)]],
      paymentMethod: ['credit_card', [Validators.required]],
      deviceType: ['desktop'],
      locationCountry: ['US'],
      merchantName: ['', [Validators.required]],
      transactionType: ['purchase']
    });
  }

  ngOnInit() {
    // Pre-fill amount if provided
    if (this.data && this.data.amount) {
      this.transactionForm.patchValue({
        amount: this.data.amount.toFixed(2),
        merchantName: 'SmartMediShop' // Default merchant name
      });
    } else {
      // Set default merchant name even if no amount provided
      this.transactionForm.patchValue({
        merchantName: 'SmartMediShop'
      });
    }
  }

  onSubmit() {
    if (this.transactionForm.valid) {
      this.loading = true;
      const transactionData = this.transactionForm.value;
      
      this.apiService.createTransaction(transactionData).subscribe({
        next: (response: TransactionResponse) => {
          this.loading = false;
          this.notificationService.showSuccess('Transaction created successfully!');
          
          // Close dialog and return the transaction response with location
          this.dialogRef.close({
            success: true,
            transaction: response,
            transactionData: transactionData,
            locationCountry: transactionData.locationCountry
          });
          
          if (response.isFraud) {
            this.notificationService.showWarning(`⚠️ FRAUD DETECTED! Risk Level: ${response.riskLevel}, Score: ${response.fraudScore}`);
          } else {
            this.notificationService.showInfo(`✅ Transaction approved. Risk Level: ${response.riskLevel}, Score: ${response.fraudScore}`);
          }
        },
        error: (error) => {
          this.loading = false;
          this.notificationService.showError('Failed to create transaction');
          console.error('Error creating transaction:', error);
        }
      });
    } else {
      this.notificationService.showWarning('Please fill in all required fields');
    }
  }

  onCancel() {
    this.dialogRef.close({ success: false });
  }
}

