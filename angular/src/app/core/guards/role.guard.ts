import { Injectable } from '@angular/core';
import { CanActivate, Router, ActivatedRouteSnapshot } from '@angular/router';
import { AuthService } from '../services/auth.service';

@Injectable({
  providedIn: 'root'
})
export class RoleGuard implements CanActivate {

  constructor(
    private authService: AuthService,
    private router: Router
  ) { }

  canActivate(route: ActivatedRouteSnapshot): boolean {
    if (!this.authService.isAuthenticated()) {
      this.router.navigate(['/auth/login']);
      return false;
    }

    // Get allowed roles from route data
    const allowedRoles = route.data['allowedRoles'] as string[];
    
    if (!allowedRoles || allowedRoles.length === 0) {
      // No role restriction, allow access
      return true;
    }

    // Check if user has one of the allowed roles
    const user = this.authService.getCurrentUser();
    if (!user) {
      this.router.navigate(['/auth/login']);
      return false;
    }

    const hasAccess = allowedRoles.some(role => this.authService.hasRole(role));
    
    if (!hasAccess) {
      // User doesn't have required role, redirect to dashboard
      this.router.navigate(['/dashboard']);
      return false;
    }

    return true;
  }
}

