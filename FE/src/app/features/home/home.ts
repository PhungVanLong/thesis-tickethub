import { Component } from '@angular/core';
import { Navigation } from '../../core/navigation/navigation';
import { CategorySection } from './category-section/category-section';
import { DealBand } from './deal-band/deal-band';
import { EventSection } from './event-section/event-section';
import { HeroSection } from './hero-section/hero-section';

@Component({
  selector: 'app-home',
  imports: [Navigation, HeroSection, CategorySection, EventSection, DealBand],
  templateUrl: './home.html',
  styleUrl: './home.scss',
})
export class HomeComponent {}
