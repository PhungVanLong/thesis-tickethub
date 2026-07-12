import { Component, inject, OnInit, signal, computed } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { NgClass, DecimalPipe } from '@angular/common';
import { DomSanitizer, SafeResourceUrl } from '@angular/platform-browser';
import { AdminService, EventResponse } from '../admin.service';

type EventDecision = 'APPROVED' | 'REJECTED';

interface ApproveModal {
  event: EventResponse;
  decision: EventDecision;
  reason: string;
}

@Component({
  selector: 'app-admin-event-verification',
  standalone: true,
  imports: [FormsModule, NgClass, DecimalPipe],
  templateUrl: './admin-event-verification.html',
  styleUrl: './admin-event-verification.scss',
})
export class AdminEventVerificationComponent implements OnInit {
  private readonly adminService = inject(AdminService);
  private readonly sanitizer = inject(DomSanitizer);

  readonly events = signal<EventResponse[]>([]);
  readonly loading = signal(true);
  readonly error = signal<string | null>(null);
  readonly filterStatus = signal<string>('PENDING');
  readonly approveModal = signal<ApproveModal | null>(null);
  readonly submitting = signal(false);
  readonly submitSuccess = signal<string | null>(null);
  readonly detailEvent = signal<any | null>(null);
  readonly mapScales = signal<Record<string, number>>({});

  readonly safeMapUrl = computed(() => {
    const event = this.detailEvent();
    if (!event || !event.locationCoords) return null;
    const cleanCoords = event.locationCoords.trim();
    if (!cleanCoords) return null;
    const url = `https://maps.google.com/maps?q=${encodeURIComponent(cleanCoords)}&z=15&output=embed`;
    return this.sanitizer.bypassSecurityTrustResourceUrl(url);
  });

  readonly statusOptions = ['ALL', 'PENDING', 'APPROVED', 'PUBLISHED', 'CANCELLED', 'REJECTED'];

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    this.loading.set(true);
    this.error.set(null);
    this.adminService.getEvents(this.filterStatus()).subscribe({
      next: (data) => {
        this.events.set(data);
        this.loading.set(false);
      },
      error: () => {
        this.error.set('Failed to load events');
        this.loading.set(false);
      }
    });
  }

  onFilterChange(status: string): void {
    this.filterStatus.set(status);
    this.load();
  }

  openApprove(event: EventResponse, decision: EventDecision): void {
    this.approveModal.set({ event, decision, reason: '' });
    this.submitSuccess.set(null);
  }

  closeApprove(): void {
    this.approveModal.set(null);
  }

  openDetail(event: EventResponse): void {
    this.adminService.getEventById(event.id).subscribe({
      next: (fullEvent: any) => {
        this.detailEvent.set(fullEvent);
        // Initialize scales for all seat maps
        const initialScales: Record<number, number> = {};
        if (fullEvent.seatMaps) {
          fullEvent.seatMaps.forEach((sm: any) => {
            initialScales[sm.id] = 1.0;
          });
        }
        this.mapScales.set(initialScales);
      },
      error: () => {
        alert('Failed to load event details');
      }
    });
  }

  closeDetail(): void {
    this.detailEvent.set(null);
  }

  submitApproval(): void {
    const modal = this.approveModal();
    if (!modal) return;
    this.submitting.set(true);
    this.adminService.approveEvent(modal.event.id, {
      decision: modal.decision,
      reason: modal.reason || undefined
    }).subscribe({
      next: () => {
        this.submitting.set(false);
        this.approveModal.set(null);
        this.submitSuccess.set(`Event "${modal.event.title}" has been ${modal.decision.toLowerCase()}.`);
        this.load();
        setTimeout(() => this.submitSuccess.set(null), 4000);
      },
      error: (err) => {
        this.submitting.set(false);
        this.error.set(err?.error?.error || 'Approval failed. Please try again.');
      }
    });
  }

  getStatusBadgeClass(status: string): string {
    const map: Record<string, string> = {
      PENDING: 'badge-pending',
      APPROVED: 'badge-approved',
      PUBLISHED: 'badge-published',
      CANCELLED: 'badge-cancelled',
    };
    return map[status] || 'badge-pending';
  }

  formatDate(dateStr: string | undefined): string {
    if (!dateStr) return '—';
    return new Date(dateStr).toLocaleDateString('en-US', {
      year: 'numeric', month: 'short', day: 'numeric', hour: '2-digit', minute: '2-digit'
    });
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

  normalizeImageUrl(url: string | null | undefined): string {
    if (!url) return '';
    const gdriveRegex = /drive\.google\.com\/file\/d\/([^\/]+)/;
    const match = url.match(gdriveRegex);
    if (match && match[1]) {
      return `https://drive.google.com/thumbnail?id=${match[1]}&sz=w1000`;
    }
    return url;
  }

  readonly fullScreenMap = signal<any | null>(null);

  openFullScreenMap(map: any): void {
    this.fullScreenMap.set(map);
  }

  closeFullScreenMap(): void {
    this.fullScreenMap.set(null);
  }

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
}
