import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { AuthGuard } from './core/guards/auth.guard';
import { GuestGuard } from './core/guards/guest.guard';
import { RoleGuard } from './core/guards/role.guard';

const routes: Routes = [
  { path: '', redirectTo: '/dashboard', pathMatch: 'full' },
  
  // Authentication routes (only for non-authenticated users)
  { 
    path: 'auth', 
    canActivate: [GuestGuard],
    loadChildren: () => import('./features/auth/auth.module').then(m => m.AuthModule)
  },
  
  // Main application routes (authentication required)
  { 
    path: 'dashboard', 
    canActivate: [AuthGuard],
    loadChildren: () => import('./features/dashboard/dashboard.module').then(m => m.DashboardModule)
  },
  { 
    path: 'cart', 
    canActivate: [AuthGuard],
    loadChildren: () => import('./features/cart/cart.module').then(m => m.CartModule)
  },
  { 
    path: 'users', 
    canActivate: [RoleGuard],
    data: { allowedRoles: ['ADMIN', 'FRAUD_ANALYST'] },
    loadChildren: () => import('./features/users/users.module').then(m => m.UsersModule)
  },
  { 
    path: 'services', 
    canActivate: [AuthGuard],
    loadChildren: () => import('./features/services/services.module').then(m => m.ServicesModule)
  },
  { 
    path: 'payments', 
    canActivate: [AuthGuard],
    loadChildren: () => import('./features/payments/payments.module').then(m => m.PaymentsModule)
  },
  { 
    path: 'clients', 
    canActivate: [AuthGuard],
    loadChildren: () => import('./features/clients/clients.module').then(m => m.ClientsModule)
  },
  { 
    path: 'stock', 
    canActivate: [AuthGuard],
    loadChildren: () => import('./features/stock/stock.module').then(m => m.StockModule)
  },
  
  // 404 Not Found page
  { 
    path: 'not-found', 
    loadChildren: () => import('./features/not-found/not-found.module').then(m => m.NotFoundModule)
  },
  
  // Catch all other routes and redirect to login
  { path: '**', redirectTo: '/auth/login' }
];

@NgModule({
  imports: [RouterModule.forRoot(routes)],
  exports: [RouterModule]
})
export class AppRoutingModule { }
