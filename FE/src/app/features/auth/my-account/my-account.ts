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
  readonly avatarPreviewUrl = signal<string>('https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?auto=format&fit=crop&w=80&q=80');

  // Strongly typed form
  readonly accountForm = this.fb.nonNullable.group({
    fullName: ['', [Validators.required]],
    phone: ['', [Validators.required, Validators.pattern(/^[0-9]{10}$/)]],
    avatarUrl: [''],
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
          avatarUrl: profile.avatarUrl || '',
        });

        if (profile.avatarUrl) {
          this.avatarPreviewUrl.set(profile.avatarUrl);
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

  onAvatarUrlChange(event: Event): void {
    const input = event.target as HTMLInputElement;
    if (input.value) {
      this.avatarPreviewUrl.set(input.value);
    }
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
