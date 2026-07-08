import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '../auth.service';

@Component({
  selector: 'app-forgot-password',
  standalone: true,
  imports: [ReactiveFormsModule, RouterLink],
  templateUrl: './forgot-password.html',
  styleUrl: './forgot-password.scss',
})
export class ForgotPasswordComponent {
  private readonly fb = inject(FormBuilder);
  private readonly authService = inject(AuthService);
  private readonly router = inject(Router);

  readonly currentStep = signal<1 | 2>(1);
  readonly isSubmitting = signal(false);
  readonly errorMessage = signal<string | null>(null);
  readonly successMessage = signal<string | null>(null);
  readonly requestEmail = signal<string>('');

  readonly forgotForm = this.fb.nonNullable.group({
    email: ['', [Validators.required, Validators.email]],
  });

  readonly resetForm = this.fb.nonNullable.group({
    token: ['', [Validators.required]],
    newPassword: ['', [Validators.required, Validators.minLength(6)]],
  });

  requestOtp(): void {
    if (this.forgotForm.invalid) {
      this.forgotForm.markAllAsTouched();
      return;
    }

    const { email } = this.forgotForm.getRawValue();
    this.isSubmitting.set(true);
    this.errorMessage.set(null);
    this.successMessage.set(null);

    this.authService.forgotPassword(email).subscribe({
      next: () => {
        this.isSubmitting.set(false);
        this.requestEmail.set(email);
        this.currentStep.set(2);
        this.successMessage.set('Mã xác thực OTP đã được gửi về email của bạn.');
      },
      error: (err) => {
        this.isSubmitting.set(false);
        let errorMsg = 'Yêu cầu thất bại. Vui lòng kiểm tra lại email.';
        if (err.error && typeof err.error === 'object') {
          errorMsg = err.error.message || err.error.error || errorMsg;
        } else if (err.error && typeof err.error === 'string') {
          errorMsg = err.error;
        }
        this.errorMessage.set(errorMsg);
      },
    });
  }

  resetPassword(): void {
    if (this.resetForm.invalid) {
      this.resetForm.markAllAsTouched();
      return;
    }

    const { token, newPassword } = this.resetForm.getRawValue();
    this.isSubmitting.set(true);
    this.errorMessage.set(null);
    this.successMessage.set(null);

    this.authService.resetPassword({ token, newPassword }).subscribe({
      next: () => {
        this.isSubmitting.set(false);
        this.successMessage.set('Đổi mật khẩu thành công! Bạn đang được chuyển hướng về trang đăng nhập...');
        setTimeout(() => {
          this.router.navigate(['/login']);
        }, 2500);
      },
      error: (err) => {
        this.isSubmitting.set(false);
        let errorMsg = 'Mã xác thực không hợp lệ hoặc đã hết hạn.';
        if (err.error && typeof err.error === 'object') {
          errorMsg = err.error.message || err.error.error || errorMsg;
        } else if (err.error && typeof err.error === 'string') {
          errorMsg = err.error;
        }
        this.errorMessage.set(errorMsg);
      },
    });
  }
}
