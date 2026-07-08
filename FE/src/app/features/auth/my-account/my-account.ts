import { Component, inject, OnInit, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { AuthService } from '../auth.service';
import { TranslatePipe } from '../../../shared/pipes/translate.pipe';
import { Navigation } from '../../../core/navigation/navigation';
import { Footer } from '../../../core/footer/footer';

@Component({
  selector: 'app-my-account',
  standalone: true,
  imports: [ReactiveFormsModule, TranslatePipe, Navigation, Footer],
  templateUrl: './my-account.html',
  styleUrl: './my-account.scss',
})
export class MyAccountComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly authService = inject(AuthService);

  readonly isLoading = signal(true);
  readonly isSaving = signal(false);
  readonly errorMessage = signal<string | null>(null);
  readonly successMessage = signal<string | null>(null);
  
  readonly userEmail = signal<string>('');
  readonly userRole = signal<string>('');
  readonly avatarPreviewUrl = signal<string | null>(null);

  // Strongly typed form (removed avatarUrl field since it's hidden)
  readonly accountForm = this.fb.nonNullable.group({
    fullName: ['', [Validators.required]],
    phone: ['', [Validators.required, Validators.pattern(/^[0-9]{10}$/)]],
  });

  ngOnInit(): void {
    this.fetchProfile();
  }

  fetchProfile(): void {
    this.isLoading.set(true);
    this.errorMessage.set(null);

    this.authService.getProfile().subscribe({
      next: (profile) => {
        this.userEmail.set(profile.email);
        this.userRole.set(profile.role);
        
        // Fill form fields
        this.accountForm.patchValue({
          fullName: profile.fullName || '',
          phone: profile.phone || '',
        });

        if (profile.avatarUrl) {
          this.avatarPreviewUrl.set(profile.avatarUrl);
        } else {
          this.avatarPreviewUrl.set(null);
        }
        this.isLoading.set(false);
      },
      error: (err) => {
        console.error('Failed to load profile:', err);
        let errorMsg = 'Failed to load user profile. Please try again later.';
        if (err.error && typeof err.error === 'object') {
          errorMsg = err.error.message || err.error.error || errorMsg;
        } else if (err.error && typeof err.error === 'string') {
          errorMsg = err.error;
        }
        this.errorMessage.set(errorMsg);
        this.isLoading.set(false);
      },
    });
  }

  getInitials(name: string | null | undefined): string {
    if (!name) {
      return 'U';
    }
    return name.trim().charAt(0).toUpperCase();
  }

  onSubmit(): void {
    if (this.accountForm.invalid) {
      this.accountForm.markAllAsTouched();
      return;
    }

    this.isSaving.set(true);
    this.errorMessage.set(null);
    this.successMessage.set(null);

    const formValues = this.accountForm.getRawValue();

    this.authService.updateProfile(formValues).subscribe({
      next: (updatedProfile) => {
        this.isSaving.set(false);
        this.successMessage.set('account.updateSuccess');
        if (updatedProfile.avatarUrl) {
          this.avatarPreviewUrl.set(updatedProfile.avatarUrl);
        } else {
          this.avatarPreviewUrl.set(null);
        }
      },
      error: (err) => {
        console.error('Failed to update profile:', err);
        let errorMsg = 'account.updateError';
        if (err.error && typeof err.error === 'object') {
          errorMsg = err.error.message || err.error.error || errorMsg;
        } else if (err.error && typeof err.error === 'string') {
          errorMsg = err.error;
        }
        this.errorMessage.set(errorMsg);
        this.isSaving.set(false);
      },
    });
  }
}
