import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { SharedModule } from '../../shared/shared.module';
import { PurchaseHistoryComponent } from './purchase-history.component';
import { PurchasesRoutingModule } from './purchases-routing.module';

@NgModule({
  declarations: [
    PurchaseHistoryComponent
  ],
  imports: [
    CommonModule,
    SharedModule,
    PurchasesRoutingModule
  ]
})
export class PurchasesModule { }

