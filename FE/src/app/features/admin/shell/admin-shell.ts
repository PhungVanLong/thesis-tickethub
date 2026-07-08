import { Component, inject, OnInit, signal } from '@angular/core';
import { Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { AuthService } from '../../auth/auth.service';

@Component({
  selector: 'app-admin-shell',
  standalone: true,
  imports: [RouterOutlet, RouterLink, RouterLinkActive],
  templateUrl: './admin-shell.html',
  styleUrl: './admin-shell.scss',
})
export class AdminShellComponent implements OnInit {
  private readonly authService = inject(AuthService);
  private readonly router = inject(Router);

  readonly userProfile = this.authService.currentUserProfile;
  readonly sidebarOpen = signal(true);

  ngOnInit(): void {
    const profile = this.userProfile();
    if (profile && profile.role !== 'ADMIN') {
      this.router.navigate(['/']);
    }
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

  logout(): void {
    this.authService.logout();
    this.router.navigate(['/login']);
  }

  get userInitial(): string {
    const name = this.userProfile()?.fullName || this.userProfile()?.email || 'A';
    return name.charAt(0).toUpperCase();
  }
}
