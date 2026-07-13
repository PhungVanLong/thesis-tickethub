import { Component, inject, OnInit, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { CommonModule } from '@angular/common';
import { EventApiService } from '../../../core/services/event.service';

@Component({
  selector: 'app-event-section',
  standalone: true,
  imports: [RouterLink, CommonModule],
  templateUrl: './event-section.html',
  styleUrl: './event-section.scss',
})
export class EventSection implements OnInit {
  private readonly eventApi = inject(EventApiService);

  readonly trendingEvents = signal<any[]>([]);
  readonly musicEvents = signal<any[]>([]);
  readonly sportsEvents = signal<any[]>([]);
  readonly weekendEvents = signal<any[]>([]);

  ngOnInit() {
    // 1. Fetch Trending Events (vé bán chạy nhất)
    this.eventApi.getDiscoveryEvents({ sortBy: 'TRENDING', limit: 4 }).subscribe({
      next: (data) => this.trendingEvents.set(this.mapEvents(data)),
      error: () => this.trendingEvents.set([])
    });

    // 2. Fetch Concerts Events (Music & Concerts)
    this.eventApi.getDiscoveryEvents({ category: 'Concerts', limit: 4 }).subscribe({
      next: (data) => this.musicEvents.set(this.mapEvents(data)),
      error: () => this.musicEvents.set([])
    });

    // 3. Fetch Sports Events
    this.eventApi.getDiscoveryEvents({ category: 'Sports', limit: 3 }).subscribe({
      next: (data) => this.sportsEvents.set(this.mapEvents(data)),
      error: () => this.sportsEvents.set([])
    });

    // 4. Fetch Weekend Events
    this.eventApi.getDiscoveryEvents({ timeRange: 'WEEKEND', limit: 4 }).subscribe({
      next: (data) => this.weekendEvents.set(this.mapEvents(data)),
      error: () => this.weekendEvents.set([])
    });
  }

  private mapEvents(data: any[]): any[] {
    return data.map(e => ({
      id: e.id,
      image: this.normalizeImageUrl(e.bannerUrl),
      alt: e.title,
      date: e.startTime ? new Date(e.startTime).toLocaleDateString('en-US', { weekday: 'short', month: 'short', day: 'numeric' }) : 'TBA',
      title: e.title,
      location: e.venue && e.city ? `${e.venue}, ${e.city}` : e.venue || e.city || 'TBA',
      buttonText: 'Book Now'
    }));
  }

  normalizeImageUrl(url: string | null | undefined): string {
    if (!url) return '';
    const match = url.match(/drive\.google\.com\/file\/d\/([^\/]+)/);
    if (match?.[1]) return `https://drive.google.com/thumbnail?id=${match[1]}&sz=w1000`;
    return url;
  }
}
