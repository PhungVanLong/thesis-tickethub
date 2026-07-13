import { Component } from '@angular/core';
import { Navigation } from '../../core/navigation/navigation';
// import { CategorySection } from './category-section/category-section';
import { DealBand } from './deal-band/deal-band';
import { EventSection } from './event-section/event-section';
import { HeroSection } from './hero-section/hero-section';
import { WhyBook } from './why-book/why-book';
import { Footer } from '../../core/footer/footer';

@Component({
  selector: 'app-home',
  imports: [Navigation, HeroSection, EventSection, DealBand, WhyBook, Footer],
  templateUrl: './home.html',
  styleUrl: './home.scss',
})
export class HomeComponent { }

