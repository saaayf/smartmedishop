import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { RoleGuard } from '../../core/guards/role.guard';
import { ProductListComponent } from './product-list/product-list.component';
import { ProductDetailComponent } from './product-detail/product-detail.component';
import { ProductFormComponent } from './product-form/product-form.component';
import { MovementFormComponent } from './movement-form/movement-form.component';
import { PredictComponent } from './predict/predict.component';
import { RecommendationsComponent } from './recommendations/recommendations.component';

const routes: Routes = [
  {
    path: '',
    redirectTo: 'products',
    pathMatch: 'full'
  },
  {
    path: 'products',
    component: ProductListComponent
  },
  {
    path: 'recommendations',
    component: RecommendationsComponent
  },
  {
    path: 'products/new',
    component: ProductFormComponent,
    canActivate: [RoleGuard],
    data: { allowedRoles: ['ADMIN'] }
  },
  {
    path: 'products/:id',
    component: ProductDetailComponent
  },
  {
    path: 'products/:id/predict',
    component: PredictComponent
  },
  {
    path: 'products/:id/edit',
    component: ProductFormComponent,
    canActivate: [RoleGuard],
    data: { allowedRoles: ['ADMIN'] }
  },
  {
    path: 'movements/new',
    component: MovementFormComponent,
    canActivate: [RoleGuard],
    data: { allowedRoles: ['ADMIN'] }
  }
];

@NgModule({
  imports: [RouterModule.forChild(routes)],
  exports: [RouterModule]
})
export class StockRoutingModule { }
