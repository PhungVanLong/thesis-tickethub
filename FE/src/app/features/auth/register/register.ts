import { Component, inject, signal } from '@angular/core';
import { AbstractControl, FormBuilder, ReactiveFormsModule, ValidationErrors, ValidatorFn, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { AuthService } from '../auth.service';

export const passwordMatchValidator: ValidatorFn = (control: AbstractControl): ValidationErrors | null => {
  const password = control.get('password');
  const confirmPassword = control.get('confirmPassword');

  if (!password || !confirmPassword) {
    return null;
  }

  return password.value === confirmPassword.value ? null : { passwordsMismatch: true };
};

@Component({
  selector: 'app-register',
  standalone: true,
  imports: [ReactiveFormsModule],
  templateUrl: './register.html',
  styleUrl: './register.scss',
})
export class RegisterComponent {
  private readonly fb = inject(FormBuilder);
  private readonly authService = inject(AuthService);
  private readonly router = inject(Router);

  readonly isRegistering = signal<boolean>(false);
  readonly registerError = signal<string | null>(null);

  readonly showPassword = signal(false);
  readonly showConfirmPassword = signal(false);

  // Strongly typed form
  readonly registerForm = this.fb.nonNullable.group(
    {
      fullName: ['', [Validators.required]],
      phone: ['', [Validators.required, Validators.pattern(/^[0-9]{10}$/)]],
      email: ['', [Validators.required, Validators.email]],
      password: ['', [Validators.required, Validators.minLength(6)]],
      confirmPassword: ['', [Validators.required]],
    },
    {
      validators: [passwordMatchValidator],
    }
  );

  togglePasswordVisibility(): void {
    this.showPassword.update((val) => !val);
  }

  toggleConfirmPasswordVisibility(): void {
    this.showConfirmPassword.update((val) => !val);
  }

  onSubmit(): void {
    if (this.registerForm.invalid) {
      this.registerForm.markAllAsTouched();
      return;
    }

    const { fullName, phone, email, password } = this.registerForm.getRawValue();

    this.isRegistering.set(true);
    this.registerError.set(null);

    this.authService.register({ fullName, phone, email, password }).subscribe({
      next: () => {
        this.isRegistering.set(false);
        // On successful registration, redirect to login
        this.router.navigate(['/login']);
      },
      error: (err) => {
        this.isRegistering.set(false);
        console.error('Registration failed:', err);
        let errorMsg = 'Registration failed. Please try again later.';
        if (err.error && typeof err.error === 'object') {
          if (err.error.message) {
            errorMsg = err.error.message;
          } else if (err.error.error) {
            errorMsg = err.error.error;
          }
        } else if (err.error && typeof err.error === 'string') {
          errorMsg = err.error;
        }
        this.registerError.set(errorMsg);
      },
    });
  }
}
