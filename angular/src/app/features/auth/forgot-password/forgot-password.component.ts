import { Component, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { NotificationService } from '../../../core/services/notification.service';

@Component({
  selector: 'app-forgot-password',
  templateUrl: './forgot-password.component.html',
  styleUrls: ['./forgot-password.component.scss']
})
export class ForgotPasswordComponent implements OnInit {
  forgotPasswordForm: FormGroup;
  loading = false;
  emailSent = false;

  constructor(
    private fb: FormBuilder,
    private notificationService: NotificationService,
    private router: Router
  ) {
    this.forgotPasswordForm = this.fb.group({
      email: ['', [Validators.required, Validators.email]]
    });
  }

  ngOnInit() {
    // Check if user is already logged in
    // if (this.authService.isAuthenticated()) {
    //   this.router.navigate(['/dashboard']);
    // }
  }

  onSubmit() {
    if (this.forgotPasswordForm.valid) {
      this.loading = true;
      const email = this.forgotPasswordForm.get('email')?.value;
      
      // TODO: Implement actual forgot password API call
      setTimeout(() => {
        this.emailSent = true;
        this.notificationService.showSuccess('Password reset instructions sent to your email!');
        this.loading = false;
      }, 2000);
    } else {
      this.notificationService.showWarning('Please enter a valid email address');
    }
  }

  goToLogin() {
    this.router.navigate(['/auth/login']);
  }

  resendEmail() {
    this.emailSent = false;
    this.forgotPasswordForm.reset();
  }
}
