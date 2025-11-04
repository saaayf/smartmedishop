import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { MatDialogModule } from '@angular/material/dialog';
import { SharedModule } from '../../shared/shared.module';

import { PaymentsComponent } from './payments.component';
import { TransactionDetailsDialogComponent } from './transaction-details-dialog.component';
import { ResolveFraudAlertDialogComponent } from './resolve-fraud-alert-dialog.component';
import { CreateTransactionDialogComponent } from './create-transaction-dialog.component';
import { PaymentsRoutingModule } from './payments-routing.module';

@NgModule({
  declarations: [
    PaymentsComponent,
    TransactionDetailsDialogComponent,
    ResolveFraudAlertDialogComponent,
    CreateTransactionDialogComponent
  ],
  imports: [
    CommonModule,
    SharedModule,
    MatDialogModule,
    PaymentsRoutingModule
  ],
  exports: [
    CreateTransactionDialogComponent
  ]
})
export class PaymentsModule { }

