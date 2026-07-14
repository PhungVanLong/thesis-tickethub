import { Component, OnInit, OnDestroy, signal, computed, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { DomSanitizer } from '@angular/platform-browser';
import { RouterLink, ActivatedRoute, Router } from '@angular/router';
import { Navigation } from '../../../core/navigation/navigation';
import { Footer } from '../../../core/footer/footer';
import { EventApiService } from '../../../core/services/event.service';
import { BookingApiService } from '../../../core/services/booking.service';
import { AuthService } from '../../auth/auth.service';
import { TranslatePipe } from '../../../shared/pipes/translate.pipe';

interface RecommendedEvent {
  id: number;
  title: string;
  date: string;
  price: string;
  imageGradient: string;
}

interface SelectedSeat {
  id: string;       // blockId_label  e.g. "42_A3"
  label: string;    // e.g. "A3"
  tierName: string;
  tierId: number;
  price: number;
  color: string;
}

@Component({
  selector: 'app-event-details',
  standalone: true,
  imports: [CommonModule, RouterLink, Navigation, Footer, TranslatePipe],
  templateUrl: './event-details.html',
  styleUrl: './event-details.scss',
})
export class EventDetailsComponent implements OnInit, OnDestroy {
  private readonly route    = inject(ActivatedRoute);
  private readonly router   = inject(Router);
  private readonly eventApi = inject(EventApiService);
  private readonly bookingApi = inject(BookingApiService);
  private readonly authService = inject(AuthService);
  private readonly sanitizer   = inject(DomSanitizer);

  // ── Event data ──
  readonly eventDetail = signal<any>(null);
  readonly loading     = signal(false);

  // ── Seat Picker Overlay ──
  readonly seatPickerOpen = signal(false);
  readonly overlayScale   = signal(1);
  readonly selectedSeats  = signal<SelectedSeat[]>([]);
  readonly bookingStatus  = signal<'IDLE' | 'BOOKING' | 'FAILED'>('IDLE');
  readonly errorMessage   = signal('');

  readonly subtotal = computed(() =>
    this.selectedSeats().reduce((sum, s) => sum + s.price, 0)
  );

  readonly lowestPrice = computed(() => {
    const event = this.eventDetail();
    if (!event || !event.ticketTiers || event.ticketTiers.length === 0) return 0;
    return Math.min(...event.ticketTiers.map((t: any) => t.price));
  });

  // drag/pan state for overlay
  isOverlayDragging = false;
  ovDragStartX = 0; ovDragStartY = 0;
  ovDragScrollLeft = 0; ovDragScrollTop = 0;

  private sse: EventSource | null = null;

  // ══ Overlay Open / Close ══
  openSeatPicker(): void {
    this.seatPickerOpen.set(true);
    this.selectedSeats.set([]);
    this.bookingStatus.set('IDLE');
    this.errorMessage.set('');
    this.overlayScale.set(1);
    document.body.style.overflow = 'hidden';
  }

  closeSeatPicker(): void {
    this.seatPickerOpen.set(false);
    document.body.style.overflow = '';
    this.closeSse();
  }

  // ══ Seat Selection ══
  buildSeatId(blockId: any, prefix: string | null | undefined, rowIdx: number, colIdx: number, startCol: number): string {
    const label = this.getSeatLabel(prefix, rowIdx, colIdx, startCol);
    return `${blockId}_${label}`;
  }

  getSeatLabel(prefix: string | null | undefined, rowIdx: number, colIdx: number, startCol: number): string {
    return this.getRowLabel(prefix, rowIdx) + (colIdx + (startCol || 1));
  }

  isSeatSelected(seatId: string): boolean {
    return this.selectedSeats().some(s => s.id === seatId);
  }

  toggleSeat(e: MouseEvent, item: any, rowIdx: number, colIdx: number): void {
    e.stopPropagation();
    if (this.isOverlayDragging) return; // don't select while panning

    const label  = this.getSeatLabel(item.labelPrefix, rowIdx, colIdx, item.startCol || 1);
    const id     = `${item.id}_${label}`;
    const tier   = this.getTierByName(item.tierName);
    if (!tier) return;

    if (this.isSeatSelected(id)) {
      this.selectedSeats.update(seats => seats.filter(s => s.id !== id));
    } else {
      this.selectedSeats.update(seats => [...seats, {
        id,
        label,
        tierName: item.tierName,
        tierId:   tier.id,
        price:    tier.price,
        color:    tier.colorCode || '#2563eb',
      }]);
    }
  }

  removeSeat(seatId: string): void {
    this.selectedSeats.update(seats => seats.filter(s => s.id !== seatId));
  }

  clearSeats(): void {
    this.selectedSeats.set([]);
  }

  // ══ Zoom / Pan ══
  zoomIn():    void { this.overlayScale.update(s => Math.min(s + 0.15, 4)); }
  zoomOut():   void { this.overlayScale.update(s => Math.max(s - 0.15, 0.3)); }
  resetZoom(): void { this.overlayScale.set(1); }

  onOverlayWheel(e: WheelEvent): void {
    e.preventDefault();
    this.overlayScale.update(s => Math.max(0.3, Math.min(4, s + (e.deltaY > 0 ? -0.1 : 0.1))));
  }

  onOverlayMouseDown(e: MouseEvent): void {
    this.isOverlayDragging = false; // reset; set true only on move
    const t = e.currentTarget as HTMLElement;
    this.ovDragStartX    = e.pageX - t.offsetLeft;
    this.ovDragStartY    = e.pageY - t.offsetTop;
    this.ovDragScrollLeft = t.scrollLeft;
    this.ovDragScrollTop  = t.scrollTop;
    t.style.cursor = 'grabbing';
  }

  onOverlayMouseLeave(e: MouseEvent): void {
    this.isOverlayDragging = false;
    (e.currentTarget as HTMLElement).style.cursor = 'grab';
  }

  onOverlayMouseUp(e: MouseEvent): void {
    this.isOverlayDragging = false;
    (e.currentTarget as HTMLElement).style.cursor = 'grab';
  }

  onOverlayMouseMove(e: MouseEvent): void {
    if (!(e.buttons & 1)) return;      // left button must be held
    const t  = e.currentTarget as HTMLElement;
    const dx = e.pageX - t.offsetLeft - this.ovDragStartX;
    const dy = e.pageY - t.offsetTop  - this.ovDragStartY;
    if (Math.abs(dx) > 3 || Math.abs(dy) > 3) {
      this.isOverlayDragging = true;   // mark as drag so seats won't fire
    }
    if (!this.isOverlayDragging) return;
    e.preventDefault();
    t.scrollLeft = this.ovDragScrollLeft - dx;
    t.scrollTop  = this.ovDragScrollTop  - dy;
  }

  // ══ Booking ══
  proceedToCheckout(): void {
    const seats = this.selectedSeats();
    if (seats.length === 0) return;

    const user = this.authService.currentUserProfile();
    if (!user) {
      this.closeSeatPicker();
      this.router.navigate(['/login'], { queryParams: { returnUrl: this.router.url } });
      return;
    }

    const payload = {
      eventId:        this.eventDetail().id,
      customerId:     user.id,
      customerEmail:  user.email,
      idempotencyKey: crypto.randomUUID(),
      items: seats.map(s => ({ ticketTierId: s.tierId, seatLabel: s.label })),
    };

    this.bookingStatus.set('BOOKING');
    this.errorMessage.set('');

    this.bookingApi.submitBooking(payload).subscribe({
      next: (res: any) => this.listenBookingResult(res.requestId),
      error: (err)     => {
        this.bookingStatus.set('FAILED');
        this.errorMessage.set(err?.error?.message || 'Booking failed.');
      },
    });
  }

  private listenBookingResult(requestId: string) {
    this.sse = new EventSource(this.bookingApi.getBookingStreamUrl(requestId));

    this.sse.addEventListener('SUCCESS', (e: any) => {
      this.closeSse();
      this.closeSeatPicker();
      this.router.navigate(['/checkout', e.data]);
    });

    this.sse.addEventListener('FAILED', (e: any) => {
      this.bookingStatus.set('FAILED');
      this.errorMessage.set(e.data || 'Booking failed.');
      this.closeSse();
    });

    this.sse.onerror = () => {
      this.bookingStatus.set('FAILED');
      this.errorMessage.set('Connection lost, please try again.');
      this.closeSse();
    };
  }

  private closeSse() { this.sse?.close(); this.sse = null; }

  // ── Recommended events ──
  readonly recommendedEvents = signal<any[]>([]);

  readonly venueFeatures = [
    { icon: 'parking',    text: 'P1 & P2 Parking available onsite' },
    { icon: 'accessible', text: 'Full accessible seating and elevators' },
    { icon: 'restaurant', text: '15+ Dining options and concert bistros' },
  ];

  // ── Google map ──
  readonly safeMapUrl = computed(() => {
    const event = this.eventDetail();
    if (!event?.venue) return null;
    const query = encodeURIComponent(event.venue);
    return this.sanitizer.bypassSecurityTrustResourceUrl(
      `https://maps.google.com/maps?q=${query}&t=&z=15&ie=UTF8&iwloc=&output=embed`
    );
  });

  // ── Preview map state ──
  readonly mapScales = signal<Record<string, number>>({});
  isDraggingMap = false;
  dragStartX = 0; dragStartY = 0; dragScrollLeft = 0; dragScrollTop = 0;

  ngOnInit() {
    this.route.paramMap.subscribe(params => {
      window.scrollTo(0, 0);
      const id = params.get('id');
      if (id) {
        const numericId = Number(id);
        this.loadEvent(numericId);
        this.loadRecommendedEvents(numericId);
      }
    });
  }
  ngOnDestroy() { this.closeSse(); document.body.style.overflow = ''; }

  private loadEvent(id: number) {
    this.loading.set(true);
    this.eventApi.getEventDetail(id).subscribe({
      next:  (res) => { this.eventDetail.set(res); this.loading.set(false); },
      error: (err) => { console.error(err); this.loading.set(false); }
    });
  }

  loadRecommendedEvents(currentId: number) {
    this.eventApi.getDiscoveryEvents({}).subscribe({
      next: (events) => {
        // Filter out current event
        let list = events.filter((e: any) => e.id !== currentId);
        
        // Randomize list
        list = this.shuffleArray(list);

        // Take top 4 and map
        const mapped = list.slice(0, 4).map((e: any) => {
          let priceStr = 'Từ 200.000đ';
          if (e.ticketTiers && e.ticketTiers.length > 0) {
            const minPrice = Math.min(...e.ticketTiers.map((t: any) => t.price));
            priceStr = `Từ ${minPrice.toLocaleString('vi-VN')} ₫`;
          }
          return {
            id: e.id,
            title: e.title,
            date: e.startTime ? new Date(e.startTime).toLocaleDateString('vi-VN', { weekday: 'short', day: 'numeric', month: 'numeric', year: 'numeric' }) : 'TBA',
            price: priceStr,
            bannerUrl: e.bannerUrl,
            imageGradient: this.getRandomGradient(e.id)
          };
        });
        this.recommendedEvents.set(mapped);
      },
      error: (err) => {
        console.error('Error loading recommendations', err);
      }
    });
  }

  private shuffleArray(array: any[]): any[] {
    const arr = [...array];
    for (let i = arr.length - 1; i > 0; i--) {
      const j = Math.floor(Math.random() * (i + 1));
      [arr[i], arr[j]] = [arr[j], arr[i]];
    }
    return arr;
  }

  private getRandomGradient(id: number): string {
    const gradients = [
      'linear-gradient(135deg, #667eea 0%, #764ba2 100%)',
      'linear-gradient(135deg, #f093fb 0%, #f5576c 100%)',
      'linear-gradient(135deg, #4facfe 0%, #00f2fe 100%)',
      'linear-gradient(135deg, #43e97b 0%, #38f9d7 100%)',
      'linear-gradient(135deg, #fa709a 0%, #fee140 100%)',
      'linear-gradient(135deg, #30cfd0 0%, #330867 100%)'
    ];
    return gradients[id % gradients.length];
  }

  normalizeImageUrl(url: string | null | undefined): string {
    if (!url) return '';
    const match = url.match(/drive\.google\.com\/file\/d\/([^\/]+)/);
    if (match?.[1]) return `https://drive.google.com/thumbnail?id=${match[1]}&sz=w1000`;
    return url;
  }

  parseLayoutJson(layoutJsonStr: string | null | undefined): any[] {
    if (!layoutJsonStr) return [];
    try { return JSON.parse(layoutJsonStr).elements || []; } catch { return []; }
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

  getTierColor(tierName: string | null | undefined, tiers: any[]): string {
    if (!tierName || !tiers) return '#2563EB';
    return tiers.find((t: any) => t.name === tierName)?.colorCode || '#2563EB';
  }

  getTierByName(tierName: string | null | undefined): any {
    return (this.eventDetail()?.ticketTiers || []).find((t: any) => t.name === tierName) || null;
  }

  // Preview map interactions
  onWheel(event: WheelEvent, mapId: string): void {
    event.preventDefault();
    const curr = this.mapScales()[mapId] || 1;
    const next = Math.max(0.2, Math.min(5, curr + (event.deltaY > 0 ? -0.1 : 0.1)));
    this.mapScales.update(s => ({ ...s, [mapId]: next }));
  }

  onMouseDown(e: MouseEvent): void {
    this.isDraggingMap = true;
    const t = e.currentTarget as HTMLElement;
    this.dragStartX = e.pageX - t.offsetLeft; this.dragStartY = e.pageY - t.offsetTop;
    this.dragScrollLeft = t.scrollLeft;       this.dragScrollTop = t.scrollTop;
    t.style.cursor = 'grabbing';
  }

  onMouseLeave(e: MouseEvent): void { this.isDraggingMap = false; (e.currentTarget as HTMLElement).style.cursor = 'grab'; }
  onMouseUp(e: MouseEvent):    void { this.isDraggingMap = false; (e.currentTarget as HTMLElement).style.cursor = 'grab'; }

  onMouseMove(e: MouseEvent): void {
    if (!this.isDraggingMap) return;
    e.preventDefault();
    const t = e.currentTarget as HTMLElement;
    t.scrollLeft = this.dragScrollLeft - (e.pageX - t.offsetLeft - this.dragStartX);
    t.scrollTop  = this.dragScrollTop  - (e.pageY - t.offsetTop  - this.dragStartY);
  }

  scrollToTickets(): void {
    document.getElementById('select-tickets')?.scrollIntoView({ behavior: 'smooth' });
  }
}
