import { Component, inject, signal, HostListener } from '@angular/core';
import { RouterLink } from '@angular/router';
import { AuthService } from '../../features/auth/auth.service';
import { LanguageService } from '../services/language.service';
import { TranslatePipe } from '../../shared/pipes/translate.pipe';

@Component({
  selector: 'app-navigation',
  imports: [RouterLink, TranslatePipe],
  templateUrl: './navigation.html',
  styleUrl: './navigation.scss',
})
export class Navigation {
  private readonly authService = inject(AuthService);
  private readonly langService = inject(LanguageService);
  
  readonly currentUserToken = this.authService.currentUserToken;
  readonly showDropdown = signal(false);
  readonly showLangDropdown = signal(false);
  
  readonly currentLang = this.langService.currentLang;

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
}
