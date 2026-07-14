import { Component, OnInit, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { EventApiService } from '../../../core/services/event.service';

export interface CityItem {
  name: string;
  icon: string;
  image: string;
  count?: string;
}

@Component({
  selector: 'app-category-section',
  standalone: true,
  imports: [RouterLink],
  templateUrl: './category-section.html',
  styleUrl: './category-section.scss',
})
export class CategorySection implements OnInit {
  private readonly eventApi = inject(EventApiService);

  readonly cities = signal<CityItem[]>([]);

  ngOnInit(): void {
    const cityTemplates: Record<string, { icon: string, image: string }> = {
      'Hà Nội': {
        icon: '🏛️',
        image: 'https://images.unsplash.com/photo-1509030450996-dd1a26dda07a?auto=format&fit=crop&w=700&q=80'
      },
      'Hồ Chí Minh': {
        icon: '🌆',
        image: 'https://images.unsplash.com/photo-1583417267826-aebc4d1542e1?auto=format&fit=crop&w=700&q=80'
      },
      'Đà Nẵng': {
        icon: '🏖️',
        image: 'https://images.unsplash.com/photo-1559592413-7cec4d0cae2b?auto=format&fit=crop&w=700&q=80'
      },
      'Sân vận động Mỹ Đình': {
        icon: '🏟️',
        image: 'https://images.unsplash.com/photo-1540747913346-19e32dc3e97e?auto=format&fit=crop&w=700&q=80'
      }
    };

    this.eventApi.getDiscoveryEvents({ limit: 100 }).subscribe(events => {
      if (events) {
        const cityCounts: Record<string, number> = {};
        events.forEach(e => {
          if (e.city) {
            cityCounts[e.city] = (cityCounts[e.city] || 0) + 1;
          }
        });

        const mappedCities = Object.keys(cityCounts).map(cityName => {
          const template = cityTemplates[cityName] || {
            icon: '📍',
            image: 'https://images.unsplash.com/photo-1477959858617-67f85cf4f1df?auto=format&fit=crop&w=700&q=80'
          };
          const countVal = cityCounts[cityName];
          return {
            name: cityName,
            icon: template.icon,
            image: template.image,
            count: `${countVal} Event${countVal > 1 ? 's' : ''}`
          };
        });

        this.cities.set(mappedCities);
      }
    });
  }
}
