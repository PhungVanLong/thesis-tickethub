import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { EventApiService } from '../../../core/services/event.service';
import { Navigation } from '../../../core/navigation/navigation';
import { Footer } from '../../../core/footer/footer';

@Component({
  selector: 'app-explore-events',
  standalone: true,
  imports: [CommonModule, RouterLink, FormsModule, Navigation, Footer],
  templateUrl: './explore-events.html',
  styleUrl: './explore-events.scss'
})
export class ExploreEventsComponent implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly eventApi = inject(EventApiService);

  // Filters state
  readonly categoryFilter = signal<string>('');
  readonly cityFilter = signal<string>('');
  readonly timeRangeFilter = signal<string>('');
  readonly sortByFilter = signal<string>('CHRONOLOGICAL');
  readonly searchQuery = signal<string>('');

  // Events list
  readonly events = signal<any[]>([]);
  readonly loading = signal<boolean>(false);

  readonly categories = ['Concerts', 'Sports', 'Arts & Theater', 'Family', 'Conference', 'Festival', 'Exhibition', 'Special'];
  readonly cities = signal<string[]>([]);
  readonly timeRanges = [
    { value: 'TODAY', label: 'Today' },
    { value: 'WEEKEND', label: 'This Weekend' },
    { value: 'MONTH_END', label: 'End of Month' }
  ];

  ngOnInit() {
    // Dynamically fetch unique cities from all discovery events
    this.eventApi.getDiscoveryEvents({ limit: 100 }).subscribe(data => {
      if (data) {
        const uniqueCities = Array.from(new Set(data.map(e => e.city).filter(c => !!c)));
        this.cities.set(uniqueCities);
      }
    });

    // Listen to query param changes
    this.route.queryParams.subscribe(params => {
      this.categoryFilter.set(params['category'] || '');
      this.cityFilter.set(params['city'] || '');
      this.timeRangeFilter.set(params['timeRange'] || '');
      this.sortByFilter.set(params['sortBy'] || 'CHRONOLOGICAL');
      this.searchQuery.set(params['search'] || '');

      this.loadEvents();
    });
  }

  loadEvents() {
    this.loading.set(true);
    const filters = {
      category: this.categoryFilter() || undefined,
      city: this.cityFilter() || undefined,
      timeRange: this.timeRangeFilter() || undefined,
      sortBy: this.sortByFilter() || undefined,
      limit: 100 // Load more for View All page
    };

    this.eventApi.getDiscoveryEvents(filters).subscribe({
      next: (data) => {
        let list = data;
        // Client side search filter if query is present
        if (this.searchQuery()) {
          const q = this.searchQuery().toLowerCase();
          list = list.filter(e => 
            e.title.toLowerCase().includes(q) || 
            (e.description && e.description.toLowerCase().includes(q)) ||
            (e.venue && e.venue.toLowerCase().includes(q))
          );
        }
        this.events.set(this.mapEvents(list));
        this.loading.set(false);
      },
      error: () => {
        this.events.set([]);
        this.loading.set(false);
      }
    });
  }

  applyFilters() {
    this.router.navigate([], {
      relativeTo: this.route,
      queryParams: {
        category: this.categoryFilter() || null,
        city: this.cityFilter() || null,
        timeRange: this.timeRangeFilter() || null,
        sortBy: this.sortByFilter() || null,
        search: this.searchQuery() || null
      },
      queryParamsHandling: 'merge'
    });
  }

  clearFilters() {
    this.router.navigate([], {
      relativeTo: this.route,
      queryParams: {}
    });
  }

  private mapEvents(data: any[]): any[] {
    return data.map(e => ({
      id: e.id,
      image: this.normalizeImageUrl(e.bannerUrl),
      alt: e.title,
      date: e.startTime ? new Date(e.startTime).toLocaleDateString('vi-VN', { weekday: 'short', day: 'numeric', month: 'numeric', year: 'numeric' }) : 'TBA',
      title: e.title,
      location: e.venue && e.city ? `${e.venue}, ${e.city}` : e.venue || e.city || 'TBA',
      price: 'Từ 200.000đ'
    }));
  }

  normalizeImageUrl(url: string | null | undefined): string {
    if (!url) return '';
    const match = url.match(/drive\.google\.com\/file\/d\/([^\/]+)/);
    if (match?.[1]) return `https://drive.google.com/thumbnail?id=${match[1]}&sz=w1000`;
    return url;
  }
}
