import { Component, Inject } from '@angular/core';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { AuthService } from '../../core/services/auth.service';

@Component({
  selector: 'app-transaction-details-dialog',
  templateUrl: './transaction-details-dialog.component.html',
  styleUrls: ['./transaction-details-dialog.component.scss']
})
export class TransactionDetailsDialogComponent {
  transaction: any;
  isFraudAnalystOrAdmin: boolean = false;

  constructor(
    public dialogRef: MatDialogRef<TransactionDetailsDialogComponent>,
    @Inject(MAT_DIALOG_DATA) public data: any,
    private authService: AuthService
  ) {
    this.transaction = data;
    // Check if user is FRAUD_ANALYST or ADMIN to show detailed explanations
    this.isFraudAnalystOrAdmin = this.authService.hasRole('FRAUD_ANALYST') || this.authService.hasRole('ADMIN');
  }

  close(): void {
    this.dialogRef.close();
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
    if (!score) return 'gray';
    if (score < 0.3) return 'green';
    if (score < 0.6) return 'orange';
    return 'red';
  }

  formatDate(date: string | Date): string {
    if (!date) return 'N/A';
    return new Date(date).toLocaleString();
  }

  getFraudReasonsList(): string[] {
    if (!this.transaction?.fraudReasons) return [];
    // If it's a string, split by comma or newline
    if (typeof this.transaction.fraudReasons === 'string') {
      return this.transaction.fraudReasons.split(/[,\n]/).map((r: string) => r.trim()).filter((r: string) => r.length > 0);
    }
    // If it's already an array
    if (Array.isArray(this.transaction.fraudReasons)) {
      return this.transaction.fraudReasons;
    }
    return [];
  }

  getPredictionClass(prediction: string): string {
    if (!prediction) return '';
    const pred = prediction.toLowerCase();
    if (pred.includes('fraud') || pred.includes('anomaly')) {
      return 'prediction-fraud';
    }
    return 'prediction-normal';
  }
}

