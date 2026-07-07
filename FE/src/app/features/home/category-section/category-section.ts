import { Component } from '@angular/core';
import { EVENT_CATEGORIES } from '../../../shared/data/home-content';

@Component({
  selector: 'app-category-section',
  templateUrl: './category-section.html',
  styleUrl: './category-section.scss',
})
export class CategorySection {
  protected readonly categories = EVENT_CATEGORIES;
}
