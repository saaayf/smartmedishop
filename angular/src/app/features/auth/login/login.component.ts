import { Component, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { AuthService } from '../../../core/services/auth.service';
import { NotificationService } from '../../../core/services/notification.service';

@Component({
  selector: 'app-login',
  templateUrl: './login.component.html',
  styleUrls: ['./login.component.scss']
})
export class LoginComponent implements OnInit {
  loginForm: FormGroup;
  loading = false;
  hidePassword = true;

  constructor(
    private fb: FormBuilder,
    private authService: AuthService,
    private notificationService: NotificationService,
    private router: Router
  ) {
    this.loginForm = this.fb.group({
      username: ['', [Validators.required, Validators.minLength(3)]],
      password: ['', [Validators.required, Validators.minLength(6)]],
      rememberMe: [false]
    });
  }

  ngOnInit() {
    // Check if user is already logged in
    if (this.authService.isAuthenticated()) {
      this.router.navigate(['/dashboard']);
    }
  }

  onSubmit() {
    if (this.loginForm.valid) {
      this.loading = true;
      const { username, password, rememberMe } = this.loginForm.value;

      this.authService.login(username, password).subscribe({
        next: (success) => {
          if (success) {
            console.log('Login successful, redirecting to dashboard...');
            this.notificationService.showSuccess('Login successful!');
            this.router.navigate(['/dashboard']).then(() => {
              console.log('Navigation to dashboard completed');
            });
          } else {
            this.notificationService.showError('Invalid credentials');
          }
          this.loading = false;
        },
        error: (error) => {
          this.notificationService.showError('Login failed. Please try again.');
          this.loading = false;
          console.error('Login error:', error);
        }
      });
    } else {
      this.notificationService.showWarning('Please fill in all required fields');
    }
  }

  goToRegister() {
    this.router.navigate(['/auth/register']);
  }

  goToForgotPassword() {
    this.router.navigate(['/auth/forgot-password']);
  }
}
