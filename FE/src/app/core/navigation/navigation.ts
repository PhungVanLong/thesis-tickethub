import { Component, inject, signal, Input, HostListener } from '@angular/core';
import { RouterLink, Router } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { AuthService } from '../../features/auth/auth.service';
import { LanguageService } from '../services/language.service';
import { TranslatePipe } from '../../shared/pipes/translate.pipe';

@Component({
  selector: 'app-navigation',
  imports: [RouterLink, TranslatePipe, FormsModule],
  templateUrl: './navigation.html',
  styleUrl: './navigation.scss',
})
export class Navigation {
  private readonly authService = inject(AuthService);
  private readonly langService = inject(LanguageService);
  private readonly router = inject(Router);

  /** 'categories' = default nav links, 'search' = search bar */
  @Input() navMode: 'categories' | 'search' = 'categories';

  readonly currentUserToken = this.authService.currentUserToken;
  readonly currentUserProfile = this.authService.currentUserProfile;
  readonly showDropdown = signal(false);
  readonly showLangDropdown = signal(false);
  
  readonly currentLang = this.langService.currentLang;

  searchQuery = '';

  get isOrganizer(): boolean {
    const role = this.authService.currentUserProfile()?.role;
    if (!role) return false;
    return role.includes('ORGANIZER');
  }

  getInitials(name: string | null | undefined): string {
    if (!name) return 'U';
    return name.trim().charAt(0).toUpperCase();
  }

  toggleDropdown(event: Event): void {
    event.stopPropagation();
    this.showLangDropdown.set(false);
    this.showDropdown.update(v => !v);
  }

  toggleLangDropdown(event: Event): void {
    event.stopPropagation();
    this.showDropdown.set(false);
    this.showLangDropdown.update(v => !v);
  }

  setLanguage(lang: 'Vie' | 'Eng'): void {
    this.langService.setLanguage(lang);
    this.showLangDropdown.set(false);
  }

  @HostListener('document:click')
  onDocumentClick(): void {
    this.showDropdown.set(false);
    this.showLangDropdown.set(false);
  }

  logout(): void {
    this.authService.logout();
    this.showDropdown.set(false);
    this.showLangDropdown.set(false);
  }

  onSearch(): void {
    if (this.searchQuery.trim()) {
      // Navigate to home with search query param
      this.router.navigate(['/'], { queryParams: { q: this.searchQuery.trim() } });
    }
  }
}
