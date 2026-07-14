import { Component, OnInit, signal, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { HttpClient } from '@angular/common/http';
import { Navigation } from '../../../core/navigation/navigation';
import { Footer } from '../../../core/footer/footer';

@Component({
  selector: 'app-ticket-detail',
  standalone: true,
  imports: [CommonModule, RouterLink, Navigation, Footer],
  templateUrl: './ticket-detail.html',
  styleUrl: './ticket-detail.scss'
})
export class TicketDetailComponent implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly http = inject(HttpClient);
  private readonly router = inject(Router);

  readonly ticket = signal<any>(null);
  readonly isLoading = signal(true);
  readonly errorMessage = signal<string | null>(null);

  ngOnInit(): void {
    window.scrollTo(0, 0);
    this.route.paramMap.subscribe(params => {
      const code = params.get('ticketCode');
      if (code) {
        this.fetchTicketDetail(code);
      } else {
        this.errorMessage.set('Ticket code is missing.');
        this.isLoading.set(false);
      }
    });
  }

  fetchTicketDetail(ticketCode: string): void {
    this.isLoading.set(true);
    this.errorMessage.set(null);

    this.http.get<any>(`http://localhost:8080/api/tickets/code/${ticketCode}`).subscribe({
      next: (data) => {
        this.ticket.set(data);
        this.isLoading.set(false);
      },
      error: (err) => {
        console.error('Failed to load ticket details', err);
        this.errorMessage.set('Failed to retrieve ticket details. Please ensure you are logged in and the ticket code is correct.');
        this.isLoading.set(false);
      }
    });
  }

  normalizeImageUrl(url: string | null | undefined): string {
    if (!url) return '';
    const match = url.match(/drive\.google\.com\/file\/d\/([^\/]+)/);
    if (match?.[1]) return `https://drive.google.com/thumbnail?id=${match[1]}&sz=w1000`;
    return url;
  }

  downloadTicketImage() {
    const element = document.querySelector('.ticket-card-wrapper') as HTMLElement;
    if (!element) return;

    element.classList.add('is-exporting');

    const t = this.ticket();
    const cleanTitle = t.eventTitle ? t.eventTitle.trim().replace(/\s+/g, '_').replace(/[^a-zA-Z0-9_-]/g, '') : 'ticket';

    const saveAsImage = (canvas: HTMLCanvasElement) => {
      try {
        const dataUrl = canvas.toDataURL('image/png');
        const link = document.createElement('a');
        link.download = `${cleanTitle}_${t.ticketCode}.png`;
        link.href = dataUrl;
        link.click();
      } catch (err) {
        console.error('Failed to save image', err);
      } finally {
        element.classList.remove('is-exporting');
      }
    };

    const runHtml2Canvas = () => {
      (window as any).html2canvas(element, { useCORS: true, scale: 2 }).then((canvas: HTMLCanvasElement) => {
        saveAsImage(canvas);
      }).catch((err: any) => {
        console.error(err);
        element.classList.remove('is-exporting');
      });
    };

    if ((window as any).html2canvas) {
      runHtml2Canvas();
    } else {
      const script = document.createElement('script');
      script.src = 'https://cdnjs.cloudflare.com/ajax/libs/html2canvas/1.4.1/html2canvas.min.js';
      script.onload = () => {
        runHtml2Canvas();
      };
      document.body.appendChild(script);
    }
  }
}
