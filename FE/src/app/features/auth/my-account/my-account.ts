import { Component, inject, OnInit, OnDestroy, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { Subscription } from 'rxjs';
import { AuthService } from '../auth.service';
import { TranslatePipe } from '../../../shared/pipes/translate.pipe';
import { Navigation } from '../../../core/navigation/navigation';
import { Footer } from '../../../core/footer/footer';
import { HttpClient } from '@angular/common/http';

type AccountTab = 'orders' | 'tickets' | 'settings';

@Component({
  selector: 'app-my-account',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, TranslatePipe, Navigation, Footer, RouterLink],
  templateUrl: './my-account.html',
  styleUrl: './my-account.scss',
})
export class MyAccountComponent implements OnInit, OnDestroy {
  private readonly fb          = inject(FormBuilder);
  private readonly authService = inject(AuthService);
  private readonly route       = inject(ActivatedRoute);
  private readonly router      = inject(Router);
  private readonly http        = inject(HttpClient);

  readonly activeTab = signal<AccountTab>('tickets');
  readonly isLoading = signal(true);
  readonly isSaving  = signal(false);
  readonly errorMessage   = signal<string | null>(null);
  readonly successMessage = signal<string | null>(null);

  readonly userEmail = signal<string>('');
  readonly userRole  = signal<string>('');
  readonly avatarPreviewUrl = signal<string | null>(null);

  // Tickets & Orders Data
  readonly tickets = signal<any[]>([]);
  readonly orders  = signal<any[]>([]);
  readonly orderFilter = signal<'ALL' | 'PAID' | 'PENDING' | 'CANCELLED'>('ALL');
  readonly ticketTimeFilter = signal<'UPCOMING' | 'ENDED'>('UPCOMING');

  private routeSub?: Subscription;

  readonly accountForm = this.fb.nonNullable.group({
    fullName: ['', [Validators.required]],
    phone:    ['', [Validators.required, Validators.pattern(/^[0-9]{10}$/)]],
  });

  get isOrganizer(): boolean {
    const role = this.userRole();
    return !!role && role.includes('ORGANIZER');
  }

  readonly filteredOrders = computed(() => {
    const all = this.orders();
    const filter = this.orderFilter();
    if (filter === 'ALL') return all;
    return all.filter(o => o.status === filter);
  });

  readonly filteredTickets = computed(() => {
    // Basic filter logic matching UI states
    const all = this.tickets();
    return all.filter(t => {
      const isEnded = new Date(t.eventDate) < new Date();
      if (this.ticketTimeFilter() === 'UPCOMING' && isEnded) return false;
      if (this.ticketTimeFilter() === 'ENDED' && !isEnded) return false;
      
      return true;
    });
  });

  ngOnInit(): void {
    window.scrollTo(0, 0);
    this.fetchProfile();

    // Listen to query params for tab switching
    this.routeSub = this.route.queryParams.subscribe(params => {
      const tab = params['tab'] as AccountTab;
      if (tab && ['orders', 'tickets', 'settings'].includes(tab)) {
        this.activeTab.set(tab);
      } else {
        this.activeTab.set('tickets');
      }
    });
  }

  ngOnDestroy(): void {
    this.routeSub?.unsubscribe();
  }

  setTab(tab: AccountTab): void {
    this.router.navigate([], {
      relativeTo: this.route,
      queryParams: { tab },
      queryParamsHandling: 'merge'
    });
  }

  fetchProfile(): void {
    this.isLoading.set(true);
    this.errorMessage.set(null);

    this.authService.getProfile().subscribe({
      next: (profile) => {
        this.userEmail.set(profile.email);
        this.userRole.set(profile.role);
        this.accountForm.patchValue({
          fullName: profile.fullName || '',
          phone:    profile.phone || '',
        });
        this.avatarPreviewUrl.set(profile.avatarUrl || null);
        this.isLoading.set(false);
        this.fetchTicketsAndOrders();
      },
      error: (err) => {
        console.error(err);
        this.errorMessage.set('Failed to load profile.');
        this.isLoading.set(false);
      }
    });
  }

  fetchTicketsAndOrders(): void {
    const user = this.authService.currentUserProfile();
    if (!user) return;

    // Fetch orders
    this.http.get(`http://localhost:8080/api/orders/customer/${user.id}`).subscribe({
      next: (res: any) => this.orders.set(res || []),
      error: () => this.orders.set([]) // fallback
    });

    // Fetch tickets (usually derived from successful bookings/orders)
    this.http.get(`http://localhost:8080/api/tickets/customer/${user.id}`).subscribe({
      next: (res: any) => this.tickets.set(res || []),
      error: () => this.tickets.set([]) // fallback
    });
  }

  getInitials(name: string | null | undefined): string {
    if (!name) return 'U';
    return name.trim().charAt(0).toUpperCase();
  }

  normalizeImageUrl(url: string | null | undefined): string {
    if (!url) return '';
    const match = url.match(/drive\.google\.com\/file\/d\/([^\/]+)/);
    if (match?.[1]) return `https://drive.google.com/thumbnail?id=${match[1]}&sz=w1000`;
    return url;
  }

  onSubmit(): void {
    if (this.accountForm.invalid) {
      this.accountForm.markAllAsTouched();
      return;
    }

    this.isSaving.set(true);
    this.errorMessage.set(null);
    this.successMessage.set(null);

    this.authService.updateProfile(this.accountForm.getRawValue()).subscribe({
      next: (updatedProfile) => {
        this.isSaving.set(false);
        this.successMessage.set('account.updateSuccess');
        this.avatarPreviewUrl.set(updatedProfile.avatarUrl || null);
      },
      error: (err) => {
        console.error(err);
        this.errorMessage.set('account.updateError');
        this.isSaving.set(false);
      }
    });
  }

  logout(): void {
    this.authService.logout();
    this.router.navigate(['/']);
  }
}
