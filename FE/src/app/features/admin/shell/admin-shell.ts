import { Component, inject, OnInit, signal, HostListener, computed } from '@angular/core';
import { Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { AuthService } from '../../auth/auth.service';
import { LanguageService } from '../../../core/services/language.service';
import { NotificationService, Notification } from '../../../core/services/notification.service';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-admin-shell',
  standalone: true,
  imports: [RouterOutlet, RouterLink, RouterLinkActive, CommonModule],
  templateUrl: './admin-shell.html',
  styleUrl: './admin-shell.scss',
})
export class AdminShellComponent implements OnInit {
  private readonly authService = inject(AuthService);
  private readonly langService = inject(LanguageService);
  private readonly notificationService = inject(NotificationService);
  private readonly router = inject(Router);

  readonly userProfile = this.authService.currentUserProfile;
  readonly sidebarOpen = signal(true);
  readonly showLangDropdown = signal(false);
  readonly showUserMenu = signal(false);
  readonly showNotifications = signal(false);
  readonly currentLang = this.langService.currentLang;

  readonly notifications = signal<Notification[]>([]);
  readonly unreadCount = computed(() => this.notifications().filter(n => !n.read).length);

  ngOnInit(): void {
    const profile = this.userProfile();
    if (profile && profile.role !== 'ADMIN') {
      this.router.navigate(['/']);
      return;
    }
    this.loadNotifications();
  }

  loadNotifications(): void {
    this.notificationService.getNotifications().subscribe({
      next: (res) => this.notifications.set(res),
      error: (err) => console.error('Failed to load notifications', err)
    });
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

  onNotificationClick(notification: Notification, event: Event): void {
    if (!notification.read) {
      this.notificationService.markAsRead(notification.id).subscribe({
        next: () => {
          this.notifications.update(list => 
            list.map(n => n.id === notification.id ? { ...n, read: true } : n)
          );
        }
      });
    }
    this.showNotifications.set(false);
    this.router.navigate(['/admin/events']);
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

  get searchPlaceholder(): string {
    const url = this.router.url;
    if (url.includes('/admin/organizations')) {
      return 'Search organizations...';
    } else if (url.includes('/admin/events')) {
      return 'Search events...';
    }
    return 'Global search...';
  }

  toggleSidebar(): void {
    this.sidebarOpen.update(v => !v);
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
    const name = this.userProfile()?.fullName || this.userProfile()?.email || 'A';
    return name.charAt(0).toUpperCase();
  }

  get userName(): string {
    return this.userProfile()?.fullName || this.userProfile()?.email || 'Admin';
  }
}
