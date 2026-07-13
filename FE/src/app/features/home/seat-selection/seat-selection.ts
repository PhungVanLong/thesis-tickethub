import { Component, OnInit, signal, computed, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink, ActivatedRoute, Router } from '@angular/router';
import { Navigation } from '../../../core/navigation/navigation';
import { Footer } from '../../../core/footer/footer';
import { EventApiService } from '../../../core/services/event.service';
import { BookingApiService } from '../../../core/services/booking.service';
import { AuthService } from '../../auth/auth.service';

interface SelectedSeat {
  seatId: string;
  label: string;
  tierName: string;
  tierColor: string;
  tierId: number;
  price: number;
}

@Component({
  selector: 'app-seat-selection',
  standalone: true,
  imports: [CommonModule, RouterLink, Navigation, Footer],
  templateUrl: './seat-selection.html',
  styleUrl: './seat-selection.scss',
})
export class SeatSelectionComponent implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly eventApi = inject(EventApiService);
  private readonly bookingApi = inject(BookingApiService);
  private readonly authService = inject(AuthService);

  readonly eventDetail = signal<any>(null);
  readonly loading = signal(false);
  readonly bookingStatus = signal<'IDLE' | 'BOOKING' | 'SUCCESS' | 'FAILED'>('IDLE');
  readonly errorMessage = signal('');

  readonly selectedSeats = signal<SelectedSeat[]>([]);

  // Map zoom/pan state
  readonly mapScales = signal<Record<string, number>>({});
  isDraggingMap = false;
  dragStartX = 0;
  dragStartY = 0;
  dragScrollLeft = 0;
  dragScrollTop = 0;

  private sse: EventSource | null = null;

  readonly subtotal = computed(() =>
    this.selectedSeats().reduce((sum, s) => sum + s.price, 0)
  );

  ngOnInit() {
    const id = this.route.snapshot.paramMap.get('id');
    if (id) {
      this.loadEvent(Number(id));
    }
  }

  private loadEvent(id: number) {
    this.loading.set(true);
    this.eventApi.getEventDetail(id).subscribe({
      next: (res) => {
        this.eventDetail.set(res);
        this.loading.set(false);
      },
      error: (err) => {
        console.error(err);
        this.loading.set(false);
      }
    });
  }

  normalizeImageUrl(url: string | null | undefined): string {
    if (!url) return '';
    const match = url.match(/drive\.google\.com\/file\/d\/([^\/]+)/);
    if (match?.[1]) return `https://drive.google.com/thumbnail?id=${match[1]}&sz=w1000`;
    return url;
  }

  parseLayoutJson(layoutJsonStr: string | null | undefined): any[] {
    if (!layoutJsonStr) return [];
    try {
      return JSON.parse(layoutJsonStr).elements || [];
    } catch { return []; }
  }

  getArray(n: any): number[] {
    return Array(Number(n) || 1).fill(0).map((_, i) => i);
  }

  getRowLabel(prefix: string | null | undefined, rowIndex: number): string {
    const p = prefix || 'A';
    return p.length === 1
      ? String.fromCharCode(p.charCodeAt(0) + rowIndex)
      : `${p}${String.fromCharCode(65 + rowIndex)}`;
  }

  getTierForName(tierName: string | null | undefined): any {
    const tiers = this.eventDetail()?.ticketTiers || [];
    return tiers.find((t: any) => t.name === tierName) || null;
  }

  getTierColor(tierName: string | null | undefined): string {
    return this.getTierForName(tierName)?.colorCode || '#2563EB';
  }

  buildSeatId(mapId: number, tierName: string, rowIdx: number, colIdx: number, startCol: number, prefix: string | null): string {
    return `${mapId}_${tierName}_${this.getRowLabel(prefix, rowIdx)}${colIdx + startCol}`;
  }

  isSeatSelected(seatId: string): boolean {
    return this.selectedSeats().some(s => s.seatId === seatId);
  }

  toggleSeat(mapId: number, item: any): void {
    const rows = item.rows || 1;
    const cols = item.cols || 1;
    // this is called per-seat, so we use the seatId approach
  }

  selectSeat(seatId: string, label: string, tierName: string, mapId: number): void {
    const tier = this.getTierForName(tierName);
    if (!tier) return;

    const existing = this.selectedSeats().find(s => s.seatId === seatId);
    if (existing) {
      this.selectedSeats.update(seats => seats.filter(s => s.seatId !== seatId));
    } else {
      const newSeat: SelectedSeat = {
        seatId,
        label,
        tierName,
        tierColor: tier.colorCode || '#2563EB',
        tierId: tier.id,
        price: tier.price,
      };
      this.selectedSeats.update(seats => [...seats, newSeat]);
    }
  }

  removeSeat(seatId: string): void {
    this.selectedSeats.update(seats => seats.filter(s => s.seatId !== seatId));
  }

  // --- Map interaction ---
  onWheel(event: WheelEvent, mapId: string): void {
    event.preventDefault();
    const curr = this.mapScales()[mapId] || 0.8;
    let next = curr + (event.deltaY > 0 ? -0.1 : 0.1);
    next = Math.max(0.2, Math.min(5, next));
    this.mapScales.update(s => ({ ...s, [mapId]: next }));
  }

  onMouseDown(e: MouseEvent): void {
    this.isDraggingMap = true;
    const t = e.currentTarget as HTMLElement;
    this.dragStartX = e.pageX - t.offsetLeft;
    this.dragStartY = e.pageY - t.offsetTop;
    this.dragScrollLeft = t.scrollLeft;
    this.dragScrollTop = t.scrollTop;
    t.style.cursor = 'grabbing';
  }

  onMouseLeave(e: MouseEvent): void {
    this.isDraggingMap = false;
    (e.currentTarget as HTMLElement).style.cursor = 'grab';
  }

  onMouseUp(e: MouseEvent): void {
    this.isDraggingMap = false;
    (e.currentTarget as HTMLElement).style.cursor = 'grab';
  }

  onMouseMove(e: MouseEvent): void {
    if (!this.isDraggingMap) return;
    e.preventDefault();
    const t = e.currentTarget as HTMLElement;
    t.scrollLeft = this.dragScrollLeft - (e.pageX - t.offsetLeft - this.dragStartX);
    t.scrollTop = this.dragScrollTop - (e.pageY - t.offsetTop - this.dragStartY);
  }

  // --- Checkout ---
  proceedToCheckout(): void {
    if (this.selectedSeats().length === 0) return;

    const user = this.authService.currentUserProfile();
    if (!user) {
      this.router.navigate(['/login'], { queryParams: { returnUrl: this.router.url } });
      return;
    }

    const items = this.selectedSeats().map(seat => ({
      ticketTierId: seat.tierId,
      seatLabel: seat.label,
    }));

    const payload = {
      eventId: this.eventDetail().id,
      customerId: user.id,
      idempotencyKey: crypto.randomUUID(),
      items,
    };

    this.bookingStatus.set('BOOKING');
    this.errorMessage.set('');

    this.bookingApi.submitBooking(payload).subscribe({
      next: (res: any) => this.listenToBookingResult(res.requestId),
      error: (err) => {
        this.bookingStatus.set('FAILED');
        this.errorMessage.set(err?.error?.message || 'Failed to submit booking.');
      }
    });
  }

  private listenToBookingResult(requestId: string) {
    this.sse = new EventSource(this.bookingApi.getBookingStreamUrl(requestId));

    this.sse.addEventListener('SUCCESS', (e: any) => {
      this.bookingStatus.set('SUCCESS');
      this.closeSse();
      this.router.navigate(['/checkout', e.data]);
    });

    this.sse.addEventListener('FAILED', (e: any) => {
      this.bookingStatus.set('FAILED');
      this.errorMessage.set(e.data || 'Booking failed.');
      this.closeSse();
    });

    this.sse.onerror = () => {
      this.bookingStatus.set('FAILED');
      this.errorMessage.set('Connection lost while waiting for result.');
      this.closeSse();
    };
  }

  private closeSse() {
    this.sse?.close();
    this.sse = null;
  }
}
