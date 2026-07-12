import { Component, OnInit, Output, EventEmitter, inject, signal, computed } from '@angular/core';
import { CommonModule, DatePipe, DecimalPipe } from '@angular/common';
import { DomSanitizer } from '@angular/platform-browser';
import { EventApiService, EventStatus } from '../../../../../core/services/event.service';
import { forkJoin, of } from 'rxjs';
import { catchError } from 'rxjs/operators';

export interface EventWithDetail {
  id: number;
  organizationId: number;
  organizationName: string;
  title: string;
  description: string;
  venue: string;
  city: string;
  locationCoords: string;
  startTime: string;
  endTime: string;
  bannerUrl: string;
  status: EventStatus;
  isPublished: boolean;
  createdAt: string;
  updatedAt: string;
  ticketTiers: any[];
  seatMaps: any[];
  totalTickets: number;
  soldTickets: number;
  revenue: number;
  percentSold: number;
}

@Component({
  selector: 'app-events-tab',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './events-tab.component.html',
  styleUrl: './events-tab.component.scss'
})
export class EventsTabComponent implements OnInit {
  private readonly eventApi = inject(EventApiService);
  private readonly sanitizer = inject(DomSanitizer);

  @Output() navigateTo = new EventEmitter<string>();

  readonly events = signal<EventWithDetail[]>([]);
  readonly isLoading = signal(true);
  readonly errorMessage = signal<string | null>(null);

  readonly activeFilter = signal<'ALL' | 'PUBLISHED' | 'PENDING' | 'DRAFTS'>('ALL');
  readonly selectedEvent = signal<EventWithDetail | null>(null);

  readonly filteredEvents = computed(() => {
    const list = this.events();
    const filter = this.activeFilter();
    if (filter === 'ALL') {
      return list;
    }
    if (filter === 'DRAFTS') {
      return list.filter(e => e.status === 'APPROVED' || e.status === 'CANCELLED');
    }
    return list.filter(e => e.status === filter);
  });

  readonly safeMapUrl = computed(() => {
    const event = this.selectedEvent();
    if (!event || !event.venue) return null;
    const query = encodeURIComponent(event.venue);
    const url = `https://maps.google.com/maps?q=${query}&t=&z=15&ie=UTF8&iwloc=&output=embed`;
    return this.sanitizer.bypassSecurityTrustResourceUrl(url);
  });

  // Count helper for tabs
  readonly counts = computed(() => {
    const list = this.events();
    return {
      all: list.length,
      published: list.filter(e => e.status === 'PUBLISHED').length,
      pending: list.filter(e => e.status === 'PENDING').length,
      drafts: list.filter(e => e.status === 'APPROVED' || e.status === 'CANCELLED').length,
    };
  });

  // Metrics
  readonly metrics = computed(() => {
    const list = this.events();
    const totalTickets = list.reduce((sum, e) => sum + e.totalTickets, 0);
    const netRevenue = list.reduce((sum, e) => sum + e.revenue, 0);
    const pendingApprovals = list.filter(e => e.status === 'PENDING').length;
    return {
      totalTickets,
      netRevenue,
      pendingApprovals
    };
  });

  ngOnInit(): void {
    this.fetchEvents();
  }

  fetchEvents(): void {
    this.isLoading.set(true);
    this.errorMessage.set(null);

    this.eventApi.getOrganizerEvents().pipe(
      catchError(err => {
        this.isLoading.set(false);
        this.errorMessage.set(err?.error?.message || 'Failed to fetch events.');
        return of([]);
      })
    ).subscribe(eventsList => {
      if (!eventsList || eventsList.length === 0) {
        this.events.set([]);
        this.isLoading.set(false);
        return;
      }

      // Sort by newest first (using createdAt if available, fallback to id)
      eventsList.sort((a, b) => {
        const timeA = a.createdAt ? new Date(a.createdAt).getTime() : a.id || 0;
        const timeB = b.createdAt ? new Date(b.createdAt).getTime() : b.id || 0;
        return timeB - timeA;
      });

      const detailRequests = eventsList.map(ev => 
        this.eventApi.getEventDetail(ev.id).pipe(
          catchError(() => of(ev)) 
        )
      );

      forkJoin(detailRequests).subscribe({
        next: (details: any[]) => {
          const mapped: EventWithDetail[] = details.map(d => {
            const tiers = d.ticketTiers || [];
            const totalTickets = tiers.reduce((sum: number, t: any) => sum + (t.quantityTotal || 0), 0);
            const soldTickets = tiers.reduce((sum: number, t: any) => sum + (t.quantitySold || 0), 0);
            const revenue = tiers.reduce((sum: number, t: any) => sum + ((t.quantitySold || 0) * (t.price || 0)), 0);
            const percentSold = totalTickets > 0 ? Math.round((soldTickets / totalTickets) * 100) : 0;

            return {
              ...d,
              ticketTiers: tiers,
              seatMaps: d.seatMaps || [],
              totalTickets,
              soldTickets,
              revenue,
              percentSold
            };
          });

          // Sort by newest first (using createdAt if available, fallback to id)
          mapped.sort((a, b) => {
            const timeA = a.createdAt ? new Date(a.createdAt).getTime() : a.id || 0;
            const timeB = b.createdAt ? new Date(b.createdAt).getTime() : b.id || 0;
            return timeB - timeA;
          });

          this.events.set(mapped);
          this.isLoading.set(false);

          // Update selected event if it is currently open (to refresh values)
          const currentSelected = this.selectedEvent();
          if (currentSelected) {
            const updated = mapped.find(e => e.id === currentSelected.id);
            if (updated) {
              this.selectedEvent.set(updated);
            }
          }
        },
        error: () => {
          const mapped: EventWithDetail[] = eventsList.map(ev => ({
            ...ev,
            ticketTiers: [],
            seatMaps: [],
            totalTickets: 0,
            soldTickets: 0,
            revenue: 0,
            percentSold: 0
          }));
          this.events.set(mapped);
          this.isLoading.set(false);
        }
      });
    });
  }

  normalizeImageUrl(url: string | null | undefined): string {
    if (!url) return '';
    const gdriveRegex = /drive\.google\.com\/file\/d\/([^\/]+)/;
    const match = url.match(gdriveRegex);
    if (match && match[1]) {
      return `https://drive.google.com/thumbnail?id=${match[1]}&sz=w1000`;
    }
    return url;
  }

  setFilter(filter: 'ALL' | 'PUBLISHED' | 'PENDING' | 'DRAFTS'): void {
    this.activeFilter.set(filter);
  }

  selectEvent(event: EventWithDetail): void {
    this.selectedEvent.set(event);
  }

  closeModal(): void {
    this.selectedEvent.set(null);
  }

  parseLayoutJson(layoutJsonStr: string | null | undefined): any[] {
    if (!layoutJsonStr) return [];
    try {
      const parsed = JSON.parse(layoutJsonStr);
      return parsed.elements || [];
    } catch (e) {
      return [];
    }
  }

  getArray(n: any): number[] {
    const size = Number(n) || 1;
    return Array(size).fill(0).map((_, i) => i);
  }

  getRowLabel(prefix: string | null | undefined, rowIndex: number): string {
    const p = prefix || 'A';
    if (p.length === 1) {
      const baseCode = p.charCodeAt(0);
      return String.fromCharCode(baseCode + rowIndex);
    }
    return `${p}${String.fromCharCode(65 + rowIndex)}`;
  }

  getTierColor(tierName: string | null | undefined, tiers: any[]): string {
    if (!tierName) return '#2563EB';
    const tier = tiers.find((t: any) => t.name === tierName);
    return tier?.colorCode || '#2563EB';
  }

  readonly confirmModal = signal<{ eventId: number; action: 'PUBLISH' | 'CANCEL' } | null>(null);
  readonly submittingAction = signal(false);

  readonly fullScreenMap = signal<any | null>(null);

  openFullScreenMap(map: any): void {
    this.fullScreenMap.set(map);
  }

  closeFullScreenMap(): void {
    this.fullScreenMap.set(null);
  }

  readonly mapScales = signal<Record<string, number>>({});

  onWheel(event: WheelEvent, mapId: string): void {
    event.preventDefault();
    const currentScale = this.mapScales()[mapId] || 1; // Default scale to 1 (đúng tỉ lệ như lúc vẽ)
    const delta = event.deltaY > 0 ? -0.1 : 0.1;
    let newScale = currentScale + delta;
    if (newScale < 0.2) newScale = 0.2;
    if (newScale > 5) newScale = 5;
    
    this.mapScales.update(scales => ({ ...scales, [mapId]: newScale }));
  }

  isDraggingMap = false;
  dragStartX = 0;
  dragStartY = 0;
  dragScrollLeft = 0;
  dragScrollTop = 0;

  onMouseDown(e: MouseEvent): void {
    this.isDraggingMap = true;
    const target = e.currentTarget as HTMLElement;
    this.dragStartX = e.pageX - target.offsetLeft;
    this.dragStartY = e.pageY - target.offsetTop;
    this.dragScrollLeft = target.scrollLeft;
    this.dragScrollTop = target.scrollTop;
    target.style.cursor = 'grabbing';
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
    const target = e.currentTarget as HTMLElement;
    const x = e.pageX - target.offsetLeft;
    const y = e.pageY - target.offsetTop;
    const walkX = (x - this.dragStartX);
    const walkY = (y - this.dragStartY);
    target.scrollLeft = this.dragScrollLeft - walkX;
    target.scrollTop = this.dragScrollTop - walkY;
  }

  openConfirm(eventId: number, action: 'PUBLISH' | 'CANCEL'): void {
    this.confirmModal.set({ eventId, action });
  }

  closeConfirm(): void {
    this.confirmModal.set(null);
  }

  submitAction(): void {
    const modal = this.confirmModal();
    if (!modal) return;
    
    this.submittingAction.set(true);
    
    if (modal.action === 'PUBLISH') {
      this.eventApi.publishEvent(modal.eventId).subscribe({
        next: () => {
          this.submittingAction.set(false);
          this.closeConfirm();
          this.fetchEvents();
        },
        error: (err) => {
          this.submittingAction.set(false);
          alert(err?.error?.message || 'Failed to publish event');
        }
      });
    } else {
      this.eventApi.cancelEvent(modal.eventId).subscribe({
        next: () => {
          this.submittingAction.set(false);
          this.closeConfirm();
          this.fetchEvents();
        },
        error: (err) => {
          this.submittingAction.set(false);
          alert(err?.error?.message || 'Failed to cancel event');
        }
      });
    }
  }
}
