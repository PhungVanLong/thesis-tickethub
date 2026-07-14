import { Component, OnInit, OnDestroy, signal, inject } from '@angular/core';
import { Router } from '@angular/router';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { TranslatePipe } from '../../../shared/pipes/translate.pipe';
import { EventApiService } from '../../../core/services/event.service';

@Component({
  selector: 'app-hero-section',
  standalone: true,
  imports: [TranslatePipe, CommonModule, FormsModule],
  templateUrl: './hero-section.html',
  styleUrl: './hero-section.scss',
})
export class HeroSection implements OnInit, OnDestroy {
  private readonly eventApi = inject(EventApiService);
  private readonly router = inject(Router);
  
  readonly featuredEvents = signal<any[]>([]);
  readonly currentIndex = signal<number>(0);
  readonly searchCity = signal<string>('');
  readonly searchQuery = signal<string>('');

  private slideInterval: any;

  ngOnInit(): void {
    // Tải tối đa 5 sự kiện đặc biệt để chạy slide carousel
    this.eventApi.getDiscoveryEvents({ category: 'Special', limit: 5 }).subscribe(events => {
      if (events && events.length > 0) {
        this.featuredEvents.set(events);
        this.startAutoSlide();
      } else {
        // Fallback: nếu không có sự kiện Special nào, lấy các sự kiện mặc định
        this.eventApi.getDiscoveryEvents({ limit: 5 }).subscribe(fallbackEvents => {
          if (fallbackEvents && fallbackEvents.length > 0) {
            this.featuredEvents.set(fallbackEvents);
            this.startAutoSlide();
          }
        });
      }
    });
  }

  ngOnDestroy(): void {
    this.stopAutoSlide();
  }

  startAutoSlide(): void {
    this.stopAutoSlide();
    this.slideInterval = setInterval(() => {
      this.nextSlide();
    }, 5000); // Tự động trượt mỗi 5 giây
  }

  stopAutoSlide(): void {
    if (this.slideInterval) {
      clearInterval(this.slideInterval);
    }
  }

  nextSlide(): void {
    const total = this.featuredEvents().length;
    if (total > 0) {
      this.currentIndex.set((this.currentIndex() + 1) % total);
    }
  }

  onSearch(event: Event): void {
    event.preventDefault();
    const params: any = {};
    const city = this.searchCity().trim();
    const query = this.searchQuery().trim();
    if (city) params['city'] = city;
    if (query) params['q'] = query;
    this.router.navigate(['/explore'], { queryParams: params });
  }

  setCurrentIndex(index: number): void {
    this.currentIndex.set(index);
    this.startAutoSlide(); // Reset timer on manual action
  }

  normalizeImageUrl(url: string | null | undefined): string {
    if (!url) return '';
    const match = url.match(/drive\.google\.com\/file\/d\/([^\/]+)/);
    if (match?.[1]) return `https://drive.google.com/thumbnail?id=${match[1]}&sz=w1000`;
    return url;
  }
}
