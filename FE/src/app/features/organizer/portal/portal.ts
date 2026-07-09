import { Component, inject, OnInit, signal, HostListener } from '@angular/core';
import { Router } from '@angular/router';
import { AuthService } from '../../auth/auth.service';
import { LanguageService } from '../../../core/services/language.service';
import { UpperCasePipe } from '@angular/common';
import { CreateEventTabComponent } from './components/create-event-tab/create-event-tab.component';

@Component({
  selector: 'app-organizer-portal',
  standalone: true,
  imports: [UpperCasePipe, CreateEventTabComponent],
  templateUrl: './portal.html',
  styleUrl: './portal.scss',
})
export class OrganizerPortalComponent implements OnInit {
  private readonly authService = inject(AuthService);
  private readonly langService = inject(LanguageService);
  private readonly router = inject(Router);

  readonly userProfile = this.authService.currentUserProfile;
  readonly activeTab = signal('dashboard');
  readonly currentLang = this.langService.currentLang;
  readonly showLangDropdown = signal(false);

  ngOnInit(): void {
    const profile = this.userProfile();
    if (profile) {
      const role = profile.role || '';
      if (!role.includes('ORGANIZER') && !role.includes('ADMIN')) {
        this.router.navigate(['/']);
      }
    }
  }

  toggleLangDropdown(event: Event): void {
    event.stopPropagation();
    this.showLangDropdown.update(v => !v);
  }

  setLanguage(lang: 'Vie' | 'Eng'): void {
    this.langService.setLanguage(lang);
    this.showLangDropdown.set(false);
  }

  @HostListener('document:click')
  onDocumentClick(): void {
    this.showLangDropdown.set(false);
  }

  logout(): void {
    this.authService.logout();
    this.router.navigate(['/login']);
  }

  get userInitial(): string {
    const name = this.userProfile()?.fullName || this.userProfile()?.email || 'O';
    return name.charAt(0).toUpperCase();
  }

  navigateToTab(tab: string): void {
    this.activeTab.set(tab);
  }
}
