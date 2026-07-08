import { Injectable, signal } from '@angular/core';

export type Language = 'Vie' | 'Eng';

@Injectable({
  providedIn: 'root',
})
export class LanguageService {
  private readonly STORAGE_KEY = 'tickethub_lang';

  // State signal initialized from localStorage or defaults to 'Vie'
  readonly currentLang = signal<Language>(
    (localStorage.getItem(this.STORAGE_KEY) as Language) || 'Vie'
  );

  setLanguage(lang: Language): void {
    localStorage.setItem(this.STORAGE_KEY, lang);
    this.currentLang.set(lang);
  }
}
