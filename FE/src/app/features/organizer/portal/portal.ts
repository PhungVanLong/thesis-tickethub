import { Component, inject, OnInit, signal, HostListener, computed } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '../../auth/auth.service';
import { LanguageService } from '../../../core/services/language.service';
import { UpperCasePipe, CommonModule } from '@angular/common';
import { CreateEventTabComponent } from './components/create-event-tab/create-event-tab.component';
import { EventsTabComponent } from './components/events-tab/events-tab.component';
import { StaffTabComponent } from './components/staff-tab/staff-tab.component';
import { OrganizerCheckinTabComponent } from './components/checkin-tab/checkin-tab.component';
import { NotificationService, Notification } from '../../../core/services/notification.service';
import { EventApiService } from '../../../core/services/event.service';
import { BookingApiService } from '../../../core/services/booking.service';

@Component({
  selector: 'app-organizer-portal',
  standalone: true,
  imports: [UpperCasePipe, CommonModule, CreateEventTabComponent, EventsTabComponent, StaffTabComponent, OrganizerCheckinTabComponent, RouterLink],
  templateUrl: './portal.html',
  styleUrl: './portal.scss',
})
export class OrganizerPortalComponent implements OnInit {
  private readonly authService = inject(AuthService);
  private readonly langService = inject(LanguageService);
  private readonly notificationService = inject(NotificationService);
  private readonly eventApi = inject(EventApiService);
  private readonly bookingApi = inject(BookingApiService);
  private readonly router = inject(Router);

  readonly userProfile = this.authService.currentUserProfile;
  readonly activeTab = signal('dashboard');
  readonly currentLang = this.langService.currentLang;
  readonly showLangDropdown = signal(false);
  readonly showUserMenu = signal(false);
  readonly showNotifications = signal(false);

  readonly notifications = signal<Notification[]>([]);
  readonly unreadCount = computed(() => this.notifications().filter(n => !n.read).length);

  readonly events = signal<any[]>([]);
  readonly stats = signal<any>({ totalRevenue: 0, totalTicketsSold: 0, totalCheckins: 0 });
  readonly recentOrders = signal<any[]>([]);
  readonly loading = signal(true);

  readonly totalCapacity = computed(() => {
    return this.events().reduce((sum, e) => {
      const tiers = e.ticketTiers || [];
      return sum + tiers.reduce((s: number, t: any) => s + (t.quantityTotal || 0), 0);
    }, 0);
  });

  readonly occupancyRate = computed(() => {
    const capacity = this.totalCapacity();
    if (capacity === 0) return 0;
    const sold = this.stats().totalTicketsSold || 0;
    return Math.round((sold / capacity) * 100);
  });

  readonly checkinRate = computed(() => {
    const sold = this.stats().totalTicketsSold || 0;
    if (sold === 0) return 0;
    const checkins = this.stats().totalCheckins || 0;
    return Math.round((checkins / sold) * 100);
  });

  ngOnInit(): void {
    const profile = this.userProfile();
    if (profile) {
      const role = profile.role || '';
      if (!role.includes('ORGANIZER') && !role.includes('ADMIN')) {
        this.router.navigate(['/']);
        return;
      }
    }
    this.loadNotifications();
    this.loadDashboardData();
  }

  loadDashboardData(): void {
    this.loading.set(true);
    this.eventApi.getOrganizerEvents().subscribe({
      next: (events) => {
        const eventIds = events.map(e => e.id);
        if (eventIds.length > 0) {
          // Fetch stats & orders
          this.bookingApi.getOrganizerStats(eventIds).subscribe({
            next: (stats) => this.stats.set(stats)
          });
          this.bookingApi.getOrganizerRecentOrders(eventIds).subscribe({
            next: (orders) => this.recentOrders.set(orders)
          });

          // Fetch full detailed events to populate ticketTiers
          import('rxjs').then(({ forkJoin }) => {
            const detailRequests = events.map(e => this.eventApi.getEventDetail(e.id));
            forkJoin(detailRequests).subscribe({
              next: (detailedEvents) => {
                this.events.set(detailedEvents);
                this.loading.set(false);
              },
              error: (err) => {
                console.error('Failed to load event details', err);
                this.events.set(events);
                this.loading.set(false);
              }
            });
          });
        } else {
          this.events.set([]);
          this.loading.set(false);
        }
      },
      error: (err) => {
        console.error('Failed to load organizer events', err);
        this.loading.set(false);
      }
    });
  }

  loadNotifications(): void {
    this.notificationService.getNotifications().subscribe({
      next: (res) => this.notifications.set(res),
      error: (err) => console.error('Failed to load notifications', err)
    });
  }

  getEventRevenue(event: any): number {
    const tiers = event.ticketTiers || [];
    return tiers.reduce((sum: number, t: any) => sum + ((t.quantitySold || 0) * (t.price || 0)), 0);
  }

  getEventTicketsSold(event: any): number {
    const tiers = event.ticketTiers || [];
    return tiers.reduce((sum: number, t: any) => sum + (t.quantitySold || 0), 0);
  }

  getEventCapacity(event: any): number {
    const tiers = event.ticketTiers || [];
    return tiers.reduce((sum: number, t: any) => sum + (t.quantityTotal || 0), 0);
  }

  toggleNotifications(event: Event): void {
    event.stopPropagation();
    this.showNotifications.update(v => !v);
    this.showLangDropdown.set(false);
    this.showUserMenu.set(false);
  }

  markAsRead(id: number, event: Event): void {
    event.stopPropagation();
    this.notificationService.markAsRead(id).subscribe({
      next: () => {
        this.notifications.update(list => 
          list.map(n => n.id === id ? { ...n, read: true } : n)
        );
      }
    });
  }

  markAllAsRead(event: Event): void {
    event.stopPropagation();
    this.notificationService.markAllAsRead().subscribe({
      next: () => {
        this.notifications.update(list => 
          list.map(n => ({ ...n, read: true }))
        );
      }
    });
  }

  toggleLangDropdown(event: Event): void {
    event.stopPropagation();
    this.showLangDropdown.update(v => !v);
    this.showUserMenu.set(false);
  }

  toggleUserMenu(event: Event): void {
    event.stopPropagation();
    this.showUserMenu.update(v => !v);
    this.showLangDropdown.set(false);
  }

  setLanguage(lang: 'Vie' | 'Eng'): void {
    this.langService.setLanguage(lang);
    this.showLangDropdown.set(false);
  }

  @HostListener('document:click')
  onDocumentClick(): void {
    this.showLangDropdown.set(false);
    this.showUserMenu.set(false);
    this.showNotifications.set(false);
  }

  logout(): void {
    this.authService.logout();
    this.router.navigate(['/login']);
  }

  get userInitial(): string {
    const name = this.userProfile()?.fullName || this.userProfile()?.email || 'O';
    return name.charAt(0).toUpperCase();
  }

  get userName(): string {
    return this.userProfile()?.fullName || this.userProfile()?.email || 'Organizer';
  }

  navigateToTab(tab: string): void {
    this.activeTab.set(tab);
  }
}
