import { Component } from '@angular/core';
import { RouterLink } from '@angular/router';

export interface CityItem {
  name: string;
  icon: string;
  image: string;
  count?: string;
}

@Component({
  selector: 'app-category-section',
  imports: [RouterLink],
  templateUrl: './category-section.html',
  styleUrl: './category-section.scss',
})
export class CategorySection {
  readonly cities: CityItem[] = [
    {
      name: 'Hà Nội',
      icon: '🏛️',
      image: 'https://images.unsplash.com/photo-1509030450996-dd1a26dda07a?auto=format&fit=crop&w=700&q=80',
      count: 'Thủ đô'
    },
    {
      name: 'Hồ Chí Minh',
      icon: '🌆',
      image: 'https://images.unsplash.com/photo-1583417267826-aebc4d1542e1?auto=format&fit=crop&w=700&q=80',
      count: 'TP. HCM'
    },
    {
      name: 'Đà Nẵng',
      icon: '🏖️',
      image: 'https://images.unsplash.com/photo-1559592413-7cec4d0cae2b?auto=format&fit=crop&w=700&q=80',
      count: 'Miền Trung'
    },
    {
      name: 'Sân vận động Mỹ Đình',
      icon: '🏟️',
      image: 'https://images.unsplash.com/photo-1540747913346-19e32dc3e97e?auto=format&fit=crop&w=700&q=80',
      count: 'Sân vận động'
    },
  ];
}
