import { CommonModule } from '@angular/common';
import { Component, computed, inject, OnInit, OnDestroy, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { HttpClient } from '@angular/common/http';
import { EventApiService } from '../../../../../core/services/event.service';
import { AuthService } from '../../../../auth/auth.service';

interface StaffCheckInEvent {
  id: number;
  title: string;
  checkedIn: number;
  total: number;
  capacity: number;
  startTime?: string;
  endTime?: string;
}

interface CheckInRecord {
  id: number;
  status: 'Verified' | 'Pending' | 'Flagged';
  attendeeName: string;
  email: string;
  seat: string;
  timestamp: string;
  orderId: string;
  tags: string[];
  method?: string;
  staffName?: string;
}

@Component({
  selector: 'app-organizer-checkin-tab',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './checkin-tab.component.html',
  styleUrl: './checkin-tab.component.scss',
})
export class OrganizerCheckinTabComponent implements OnInit, OnDestroy {
  private readonly http = inject(HttpClient);
  private readonly eventService = inject(EventApiService);
  private readonly authService = inject(AuthService);

  readonly events = signal<StaffCheckInEvent[]>([]);
  readonly selectedEventId = signal<number>(0);
  readonly activeTab = signal<'scan' | 'history'>('scan');
  readonly historyStatusFilter = signal<'ALL' | 'SUCCESS' | 'FAILED' | 'FLAGGED'>('ALL');
  readonly historySearchQuery = signal<string>('');
  readonly recentCheckIns = signal<CheckInRecord[]>([]);
  readonly ticketCodeInput = signal('');
  readonly errorMessage = signal<string | null>(null);
  readonly successMessage = signal<string | null>(null);

  private checkinSse: EventSource | null = null;

  readonly selectedEvent = computed(() => {
    return this.events().find(event => event.id === this.selectedEventId()) ?? this.events()[0] ?? null;
  });

  readonly checkedInCount = computed(() => this.selectedEvent()?.checkedIn ?? 0);
  readonly totalCapacity = computed(() => this.selectedEvent()?.total ?? 0);
  readonly capacityPercent = computed(() => this.selectedEvent()?.capacity ?? 0);

  readonly filteredHistoryCheckIns = computed(() => {
    const query = this.historySearchQuery().trim().toLowerCase();
    const status = this.historyStatusFilter();

    return this.recentCheckIns().filter(record => {
      const matchesQuery = !query || 
        record.attendeeName.toLowerCase().includes(query) || 
        record.email.toLowerCase().includes(query) || 
        record.orderId.toLowerCase().includes(query);
      
      const matchesStatus = status === 'ALL' || 
        (status === 'SUCCESS' && record.status === 'Verified') ||
        (status === 'FAILED' && record.status === 'Flagged') ||
        (status === 'FLAGGED' && record.status === 'Pending');

      return matchesQuery && matchesStatus;
    });
  });

  ngOnInit(): void {
    this.loadEvents();
  }

  ngOnDestroy(): void {
    if (this.checkinSse) {
      this.checkinSse.close();
    }
  }

  loadEvents(): void {
    this.eventService.getOrganizerEvents().subscribe({
      next: (res: any[]) => {
        const staffEvents = res.map(e => ({
          id: e.id,
          title: e.title,
          checkedIn: 0,
          total: 0,
          capacity: 0,
          startTime: e.startTime,
          endTime: e.endTime
        }));
        this.events.set(staffEvents);
        if (staffEvents.length > 0) {
          const firstEvent = staffEvents[0];
          this.selectedEventId.set(firstEvent.id);
          this.loadEventDetails(firstEvent.id);
          this.loadCheckins(firstEvent.id);
          this.connectToCheckinSse(firstEvent.id);
        }
      },
      error: (err) => {
        console.error('Failed to load events', err);
      }
    });
  }

  loadEventDetails(eventId: number): void {
    this.eventService.getEventDetail(eventId).subscribe({
      next: (detail: any) => {
        let totalQty = 0;
        if (detail.ticketTiers) {
          totalQty = detail.ticketTiers.reduce((acc: number, tier: any) => acc + (tier.quantityTotal || 0), 0);
        }
        
        this.events.update(currentEvents => {
          return currentEvents.map(e => {
            if (e.id === eventId) {
              const capacityPercent = totalQty > 0 ? Math.round((e.checkedIn / totalQty) * 100) : 0;
              return {
                ...e,
                total: totalQty,
                capacity: capacityPercent
              };
            }
            return e;
          });
        });
      }
    });
  }

  loadCheckins(eventId: number): void {
    this.http.get<any>(`http://localhost:8080/api/tickets/event/${eventId}/checkins?size=100`).subscribe({
      next: (res) => {
        const content = res.content || [];
        const records = content.map((c: any) => {
          return {
            id: c.checkinId,
            status: 'Verified',
            attendeeName: c.customerEmail ? c.customerEmail.split('@')[0] : 'N/A',
            email: c.customerEmail || 'N/A',
            seat: c.seatCode || 'N/A',
            timestamp: new Date(c.checkedInAt).toLocaleTimeString(),
            orderId: c.ticketCode || 'N/A',
            tags: c.tierName ? [c.tierName] : [],
            method: c.method,
            staffName: c.staffId ? `Staff #${c.staffId}` : 'System Auto'
          };
        });
        this.recentCheckIns.set(records);

        const totalCheckedIn = res.totalElements || content.length;
        this.events.update(currentEvents => {
          return currentEvents.map(e => {
            if (e.id === eventId) {
              const capacityPercent = e.total > 0 ? Math.round((totalCheckedIn / e.total) * 100) : 0;
              return {
                ...e,
                checkedIn: totalCheckedIn,
                capacity: capacityPercent
              };
            }
            return e;
          });
        });
      },
      error: (err) => {
        console.error('Failed to load checkins', err);
      }
    });
  }

  onEventChange(eventId: number | string): void {
    const id = Number(eventId);
    this.selectedEventId.set(id);
    this.loadEventDetails(id);
    this.loadCheckins(id);
    this.connectToCheckinSse(id);
  }

  connectToCheckinSse(eventId: number): void {
    if (this.checkinSse) {
      this.checkinSse.close();
      this.checkinSse = null;
    }

    this.checkinSse = new EventSource(`http://localhost:8080/api/tickets/staff/checkins/stream?eventId=${eventId}`);
    
    this.checkinSse.addEventListener('CHECKIN', (event: any) => {
      try {
        const c = JSON.parse(event.data);
        const exists = this.recentCheckIns().some(r => r.orderId === c.ticketCode);
        if (exists) return;

        const record: CheckInRecord = {
          id: c.checkinId || Math.random(),
          status: 'Verified',
          attendeeName: c.customerEmail ? c.customerEmail.split('@')[0] : 'N/A',
          email: c.customerEmail || 'N/A',
          seat: c.seatCode || 'N/A',
          timestamp: new Date(c.checkedInAt || Date.now()).toLocaleTimeString(),
          orderId: c.ticketCode || 'N/A',
          tags: c.tierName ? [c.tierName] : [],
          method: c.method,
          staffName: c.staffId ? `Staff #${c.staffId}` : 'System Auto'
        };
        
        this.recentCheckIns.update(list => [record, ...list]);

        this.events.update(currentEvents => {
          return currentEvents.map(e => {
            if (e.id === eventId) {
              const newCheckedIn = e.checkedIn + 1;
              const capacityPercent = e.total > 0 ? Math.round((newCheckedIn / e.total) * 100) : 0;
              return {
                ...e,
                checkedIn: newCheckedIn,
                capacity: capacityPercent
              };
            }
            return e;
          });
        });
      } catch (err) {
        console.error('Failed to parse SSE checkin event data', err);
      }
    });
  }

  verifyTicket(): void {
    const code = this.ticketCodeInput().trim();
    if (!code) {
      this.errorMessage.set('Please enter a ticket code');
      return;
    }

    this.errorMessage.set(null);
    this.successMessage.set(null);

    const currentUser = this.authService.currentUserProfile();
    const payload = {
      ticketCode: code,
      eventId: this.selectedEventId(),
      method: 'MANUAL',
      staffId: currentUser ? currentUser.id : null,
      deviceId: 'Organizer Web Portal'
    };

    this.http.post('http://localhost:8080/api/tickets/checkin', payload).subscribe({
      next: (res: any) => {
        this.successMessage.set(`Successfully checked in seat ${res.seatCode || 'N/A'}!`);
        this.ticketCodeInput.set('');
        this.loadCheckins(this.selectedEventId());
      },
      error: (err) => {
        console.error(err);
        this.errorMessage.set(err.error?.message || 'Verification failed. Invalid or already used ticket.');
      }
    });
  }

  trackById(index: number, item: CheckInRecord): number {
    return item.id;
  }
}
