import { Component, inject, OnInit, signal, effect } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { AuthService } from '../../auth/auth.service';
import { TranslatePipe } from '../../../shared/pipes/translate.pipe';
import { Navigation } from '../../../core/navigation/navigation';
import { Footer } from '../../../core/footer/footer';

interface MockEvent {
  id: number;
  title: string;
  venue: string;
  startTime: string;
  status: 'PENDING' | 'APPROVED' | 'PUBLISHED' | 'CANCELLED';
  ticketsSold: number;
  revenue: string;
}

@Component({
  selector: 'app-organization-dashboard',
  standalone: true,
  imports: [ReactiveFormsModule, TranslatePipe, Navigation, Footer],
  templateUrl: './dashboard.html',
  styleUrl: './dashboard.scss',
})
export class OrganizationDashboardComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly authService = inject(AuthService);

  readonly isSubmitting = signal(false);
  readonly errorMessage = signal<string | null>(null);
  readonly successMessage = signal<string | null>(null);
  readonly isPendingApproval = signal(false);

  // Expose user profile signal from AuthService
  readonly userProfile = this.authService.currentUserProfile;

  readonly mockEvents = signal<MockEvent[]>([
    {
      id: 1,
      title: 'Liveshow Lệ Quyên & Những Người Bạn',
      venue: 'Trung tâm Hội nghị Quốc gia',
      startTime: '2026-09-01T12:00:00Z',
      status: 'PUBLISHED',
      ticketsSold: 450,
      revenue: '900,000,000đ',
    },
    {
      id: 2,
      title: 'Đại nhạc hội EDM Summer Vibes 2026',
      venue: 'Sân vận động Mỹ Đình',
      startTime: '2026-10-15T10:00:00Z',
      status: 'APPROVED',
      ticketsSold: 0,
      revenue: '0đ',
    },
    {
      id: 3,
      title: 'Hội thảo Công nghệ Tech Summit 2026',
      venue: 'Tech Center Hall A',
      startTime: '2026-11-20T09:00:00Z',
      status: 'PENDING',
      ticketsSold: 0,
      revenue: '0đ',
    }
  ]);

  readonly orgForm = this.fb.nonNullable.group({
    name: ['', [Validators.required]],
    abbreviationName: [''],
    taxCode: ['', [Validators.pattern(/^[0-9]{10}$|^[0-9]{13}$/)]],
    representativeName: [''],
    representativePosition: [''],
    hotline: ['', [Validators.pattern(/^[0-9]{9,15}$/)]],
    officialEmail: ['', [Validators.email]],
    provinceCity: [''],
    district: [''],
    wardCommune: [''],
    headquarterAddress: [''],
    websiteUrl: [''],
    fanpageUrl: [''],
    description: [''],
  });

  constructor() {
    // Check role and pending status whenever user profile updates
    effect(() => {
      const profile = this.userProfile();
      if (profile) {
        if (profile.role === 'ORGANIZER') {
          this.isPendingApproval.set(false);
          localStorage.removeItem('pendingOrgReg');
        }
      }
    });
  }

  ngOnInit(): void {
    // Initial check for pending status in local storage
    const isPending = localStorage.getItem('pendingOrgReg') === 'true';
    const role = this.userProfile()?.role;
    
    if (isPending && role !== 'ORGANIZER') {
      this.isPendingApproval.set(true);
    }
  }

  onSubmitRegistration(): void {
    if (this.orgForm.invalid) {
      this.orgForm.markAllAsTouched();
      return;
    }

    this.isSubmitting.set(true);
    this.errorMessage.set(null);
    this.successMessage.set(null);

    const formValues = this.orgForm.value;

    this.authService.registerOrganizer(formValues).subscribe({
      next: () => {
        this.isSubmitting.set(false);
        this.successMessage.set('organizer.success');
        localStorage.setItem('pendingOrgReg', 'true');
        this.isPendingApproval.set(true);
        this.orgForm.reset();
      },
      error: (err) => {
        console.error('Failed to submit organization registration:', err);
        let errorMsg = 'organizer.error';
        if (err.error && typeof err.error === 'object') {
          errorMsg = err.error.message || err.error.error || errorMsg;
        } else if (err.error && typeof err.error === 'string') {
          errorMsg = err.error;
        }
        this.errorMessage.set(errorMsg);
        this.isSubmitting.set(false);
      },
    });
  }
}
