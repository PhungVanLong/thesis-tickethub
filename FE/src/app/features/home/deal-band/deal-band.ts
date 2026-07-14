import { Component, OnInit, signal, inject } from '@angular/core';
import { RouterLink } from '@angular/router';
import { CommonModule } from '@angular/common';
import { EventApiService } from '../../../core/services/event.service';

@Component({
  selector: 'app-deal-band',
  standalone: true,
  imports: [RouterLink, CommonModule],
  templateUrl: './deal-band.html',
  styleUrl: './deal-band.scss',
})
export class DealBand implements OnInit {
  private readonly eventApi = inject(EventApiService);
  readonly deals = signal<any[]>([]);

  ngOnInit(): void {
    this.eventApi.getEvents('PUBLISHED').subscribe(data => {
      // Lấy 3 sự kiện đầu tiên lúc đầu để hiển thị
      const mapped = (data || []).slice(0, 3).map(e => ({
        id: e.id,
        title: e.title,
        image: this.normalizeImageUrl(e.bannerUrl),
        eyebrow: e.category || 'EVENT',
        buttonText: 'See Details'
      }));
      this.deals.set(mapped);
    });
  }

  normalizeImageUrl(url: string | null | undefined): string {
    if (!url) return '';
    const match = url.match(/drive\.google\.com\/file\/d\/([^\/]+)/);
    if (match?.[1]) return `https://drive.google.com/thumbnail?id=${match[1]}&sz=w1000`;
    return url;
  }
}
