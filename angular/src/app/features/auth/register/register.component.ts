import { Component, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { AuthService } from '../../../core/services/auth.service';
import { NotificationService } from '../../../core/services/notification.service';

@Component({
  selector: 'app-register',
  templateUrl: './register.component.html',
  styleUrls: ['./register.component.scss']
})
export class RegisterComponent implements OnInit {
  registerForm: FormGroup;
  loading = false;
  hidePassword = true;
  hideConfirmPassword = true;
  maxDate!: Date;
  minDate!: Date;

  constructor(
    private fb: FormBuilder,
    private authService: AuthService,
    private notificationService: NotificationService,
    private router: Router
  ) {
    this.registerForm = this.fb.group({
      firstName: ['', [Validators.required, Validators.minLength(2)]],
      lastName: ['', [Validators.required, Validators.minLength(2)]],
      email: ['', [Validators.required, Validators.email]],
      username: ['', [Validators.required, Validators.minLength(3)]],
      password: ['', [Validators.required, Validators.minLength(6)]],
      confirmPassword: ['', [Validators.required]],
      phone: [''],
      birthDate: ['', [Validators.required]],
      role: ['user', [Validators.required]],
      acceptTerms: [false, [Validators.requiredTrue]]
    }, { validators: this.passwordMatchValidator });
    
    // Initialize date picker restrictions (no age limit, just reasonable date range)
    const today = new Date();
    this.maxDate = today; // Cannot select future dates
    this.minDate = new Date(today.getFullYear() - 120, today.getMonth(), today.getDate()); // No more than 120 years ago
  }

  ngOnInit() {
    // Check if user is already logged in
    if (this.authService.isAuthenticated()) {
      this.router.navigate(['/dashboard']);
    }
  }

  passwordMatchValidator(form: FormGroup) {
    const password = form.get('password');
    const confirmPassword = form.get('confirmPassword');
    
    if (password && confirmPassword && password.value !== confirmPassword.value) {
      confirmPassword.setErrors({ passwordMismatch: true });
      return { passwordMismatch: true };
    }
    return null;
  }

  onSubmit() {
    if (this.registerForm.valid) {
      this.loading = true;
      const formData = this.registerForm.value;
      
      const registerData = {
        username: formData.username,
        email: formData.email,
        password: formData.password,
        firstName: formData.firstName,
        lastName: formData.lastName,
        phone: formData.phone,
        birthDate: formData.birthDate ? new Date(formData.birthDate).toISOString().split('T')[0] : null, // Format as YYYY-MM-DD
        role: formData.role
      };

      this.authService.register(registerData).subscribe({
        next: (success) => {
          if (success) {
            this.notificationService.showSuccess('Registration successful! You are now logged in.');
            this.router.navigate(['/dashboard']);
          } else {
            this.notificationService.showError('Registration failed. Please try again.');
          }
          this.loading = false;
        },
        error: (error) => {
          this.notificationService.showError('Registration failed. Please try again.');
          this.loading = false;
          console.error('Registration error:', error);
        }
      });
    } else {
      this.notificationService.showWarning('Please fill in all required fields correctly');
    }
  }

  goToLogin() {
    this.router.navigate(['/auth/login']);
  }
}
