import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { SharedModule } from '../../shared/shared.module';
import { MatDialogModule } from '@angular/material/dialog';

import { UsersComponent } from './users.component';
import { UsersRoutingModule } from './users-routing.module';
import { UserDetailsDialogComponent } from './user-details-dialog.component';
import { UserEditDialogComponent } from './user-edit-dialog.component';

@NgModule({
  declarations: [
    UsersComponent,
    UserDetailsDialogComponent,
    UserEditDialogComponent
  ],
  imports: [
    CommonModule,
    SharedModule,
    UsersRoutingModule,
    MatDialogModule
  ]
})
export class UsersModule { }
