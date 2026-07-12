import { Component, inject, OnInit, signal } from '@angular/core';
import { TranslatePipe } from '../../../shared/pipes/translate.pipe';
import { EventApiService } from '../../../core/services/event.service';

@Component({
  selector: 'app-event-section',
  imports: [TranslatePipe],
  templateUrl: './event-section.html',
  styleUrl: './event-section.scss',
})
export class EventSection implements OnInit {
  private readonly eventApi = inject(EventApiService);
  protected readonly events = signal<any[]>([]);

  ngOnInit() {
    this.eventApi.getEvents('PUBLISHED').subscribe(data => {
      const mapped = data.map(e => ({
        id: e.id,
        image: e.bannerUrl,
        alt: e.title,
        date: e.startTime ? new Date(e.startTime).toLocaleDateString('en-US', { weekday: 'short', month: 'short', day: 'numeric', year: 'numeric' }) : 'TBA',
        title: e.title,
        location: e.venue && e.city ? `${e.venue}, ${e.city}` : e.venue || e.city || 'TBA',
        buttonText: 'See Tickets'
      }));
      this.events.set(mapped);
    });
  }
}
