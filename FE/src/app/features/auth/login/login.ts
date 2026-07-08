import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '../auth.service';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [ReactiveFormsModule, RouterLink],
  templateUrl: './login.html',
  styleUrl: './login.scss',
})
export class LoginComponent {
  private readonly fb = inject(FormBuilder);
  private readonly authService = inject(AuthService);
  private readonly router = inject(Router);

  // Expose signals from authService for template binding
  readonly isLoggingIn = this.authService.isLoggingIn;
  readonly loginError = this.authService.loginError;

  readonly showPassword = signal(false);

  // Strongly typed form
  readonly loginForm = this.fb.nonNullable.group({
    email: ['', [Validators.required, Validators.email]],
    password: ['', [Validators.required, Validators.minLength(6)]],
    rememberMe: [false],
  });

  togglePasswordVisibility(): void {
    this.showPassword.update((val) => !val);
  }

  onSubmit(): void {
    if (this.loginForm.invalid) {
      this.loginForm.markAllAsTouched();
      return;
    }

    const { email, password } = this.loginForm.getRawValue();

    this.authService.login({ email, password }).subscribe({
      next: () => {
        // Wait for profile to load then redirect based on role
        this.authService.getProfile().subscribe({
          next: (profile) => {
            if (profile?.role === 'ADMIN') {
              this.router.navigate(['/admin']);
            } else {
              this.router.navigate(['/']);
            }
          },
          error: () => this.router.navigate(['/'])
        });
      },
      error: (err) => {
        console.error('Đăng nhập thất bại:', err);
      },
    });
  }

  onGoogleSignIn(): void {
    console.log('Google Sign In clicked');
    // Implement Mock / Google login logic here
  }
}
