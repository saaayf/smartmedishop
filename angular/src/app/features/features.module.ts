import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';

// Feature modules
import { DashboardModule } from './dashboard/dashboard.module';
import { CartModule } from './cart/cart.module';
import { UsersModule } from './users/users.module';
import { ServicesModule } from './services/services.module';
import { PaymentsModule } from './payments/payments.module';
import { ClientsModule } from './clients/clients.module';
import { StockModule } from './stock/stock.module';
import { AuthModule } from './auth/auth.module';
import { NotFoundModule } from './not-found/not-found.module';

@NgModule({
  declarations: [],
  imports: [
    CommonModule,
    DashboardModule,
    CartModule,
    UsersModule,
    ServicesModule,
    PaymentsModule,
    ClientsModule,
    StockModule,
    AuthModule,
    NotFoundModule
  ],
  exports: [
    DashboardModule,
    CartModule,
    UsersModule,
    ServicesModule,
    PaymentsModule,
    ClientsModule,
    StockModule,
    AuthModule,
    NotFoundModule
  ]
})
export class FeaturesModule { }
