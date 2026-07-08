import { Pipe, PipeTransform, inject } from '@angular/core';
import { LanguageService } from '../../core/services/language.service';
import { TRANSLATIONS } from '../data/translations';

@Pipe({
  name: 'translate',
  standalone: true,
  pure: false, // Set pure: false so it updates dynamically when signal changes
})
export class TranslatePipe implements PipeTransform {
  private readonly langService = inject(LanguageService);

  transform(key: string): string {
    const lang = this.langService.currentLang();
    return TRANSLATIONS[lang]?.[key] || key;
  }
}
