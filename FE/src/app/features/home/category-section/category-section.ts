import { Component } from '@angular/core';
import { EVENT_CATEGORIES } from '../../../shared/data/home-content';
import { TranslatePipe } from '../../../shared/pipes/translate.pipe';

@Component({
  selector: 'app-category-section',
  imports: [TranslatePipe],
  templateUrl: './category-section.html',
  styleUrl: './category-section.scss',
})
export class CategorySection {
  protected readonly categories = EVENT_CATEGORIES;
}
