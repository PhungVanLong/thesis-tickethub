import { CommonModule } from '@angular/common';
import { Component, computed, inject, OnInit, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { AuthService } from '../../auth/auth.service';

interface StaffCheckInEvent {
  id: number;
  title: string;
  checkedIn: number;
  total: number;
  capacity: number;
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
}

@Component({
  selector: 'app-staff-check-in',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './staff-check-in.html',
  styleUrl: './staff-check-in.scss',
})
export class StaffCheckInComponent implements OnInit {
  private readonly authService = inject(AuthService);
  private readonly router = inject(Router);

  readonly events = signal<StaffCheckInEvent[]>([
    { id: 1, title: 'Electric Horizon Festival 2024', checkedIn: 12450, total: 15000, capacity: 83 },
    { id: 2, title: 'TicketHub Launch Night', checkedIn: 780, total: 1200, capacity: 65 },
  ]);
  readonly selectedEventId = signal<number>(1);
  readonly scannerOpen = signal(false);
  readonly scannerMessage = signal('Place the attendee\'s QR code within the camera frame or connect an external scanner.');

  readonly attendeeName = signal('');
  readonly orderId = signal('');
  readonly selectedQuickFilter = signal<'ALL' | 'VIP' | 'UNPAID' | 'STAFF'>('ALL');
  readonly searchResultMessage = signal<string | null>(null);
  readonly filterOptions: ('VIP' | 'UNPAID' | 'STAFF')[] = ['VIP', 'UNPAID', 'STAFF'];

  readonly recentCheckIns = signal<CheckInRecord[]>([
    {
      id: 1,
      status: 'Verified',
      attendeeName: 'Marcus Holloway',
      email: 'm.holloway@nexus.com',
      seat: 'K19',
      timestamp: '14:22:05',
      orderId: 'TH-8821-X',
      tags: ['VIP'],
    },
    {
      id: 2,
      status: 'Verified',
      attendeeName: 'Sarah Chen',
      email: 'schen.dev@gmail.com',
      seat: 'V04',
      timestamp: '14:21:48',
      orderId: 'TH-7712-Q',
      tags: ['STAFF'],
    },
    {
      id: 3,
      status: 'Verified',
      attendeeName: 'Julian Aris',
      email: 'julian.aris@outlook.com',
      seat: 'A12',
      timestamp: '14:21:12',
      orderId: 'TH-6534-K',
      tags: ['UNPAID'],
    },
    {
      id: 4,
      status: 'Pending',
      attendeeName: 'Elena Rodriguez',
      email: 'elena_rod@univ.edu',
      seat: 'G22',
      timestamp: '14:20:55',
      orderId: 'TH-4412-L',
      tags: ['VIP'],
    },
  ]);

  readonly selectedEvent = computed(() => {
    return this.events().find(event => event.id === this.selectedEventId()) ?? this.events()[0] ?? null;
  });

  readonly checkedInCount = computed(() => this.selectedEvent()?.checkedIn ?? 0);
  readonly totalCapacity = computed(() => this.selectedEvent()?.total ?? 0);
  readonly capacityPercent = computed(() => this.selectedEvent()?.capacity ?? 0);
  readonly remainingSeats = computed(() => Math.max(this.totalCapacity() - this.checkedInCount(), 0));

  readonly filteredRecentCheckIns = computed(() => {
    const query = `${this.attendeeName().trim()} ${this.orderId().trim()}`.toLowerCase();
    const filter = this.selectedQuickFilter();

    return this.recentCheckIns().filter(record => {
      const matchesQuery = !query || record.attendeeName.toLowerCase().includes(query) || record.email.toLowerCase().includes(query) || record.orderId.toLowerCase().includes(query);
      const matchesFilter = filter === 'ALL' || record.tags.includes(filter);
      return matchesQuery && matchesFilter;
    });
  });

  ngOnInit(): void {
    const profile = this.authService.currentUserProfile();
    const role = profile?.role || '';

    if (profile && !role.includes('STAFF')) {
      this.router.navigate(['/']);
      return;
    }

    if (!this.events().length) {
      this.router.navigate(['/']);
    }
  }

  onEventChange(eventId: number | string): void {
    this.selectedEventId.set(Number(eventId));
  }

  openScanner(): void {
    this.scannerOpen.set(true);
    this.scannerMessage.set('Scanner ready. Waiting for a ticket QR code to be detected.');
  }

  findAttendee(): void {
    const matches = this.filteredRecentCheckIns();
    if (!this.attendeeName().trim() && !this.orderId().trim()) {
      this.searchResultMessage.set('Enter an attendee name, email, or order ID to search.');
      return;
    }

    if (matches.length === 0) {
      this.searchResultMessage.set('No matching attendee was found. Check the spelling or scan the QR again.');
      return;
    }

    this.searchResultMessage.set(`Found ${matches.length} matching attendee${matches.length > 1 ? 's' : ''}.`);
  }

  setQuickFilter(filter: 'ALL' | 'VIP' | 'UNPAID' | 'STAFF'): void {
    this.selectedQuickFilter.set(filter);
  }

  getQuickFilterLabel(filter: 'ALL' | 'VIP' | 'UNPAID' | 'STAFF'): string {
    switch (filter) {
      case 'VIP':
        return 'VIP Only';
      case 'UNPAID':
        return 'Unpaid';
      case 'STAFF':
        return 'Staff';
      default:
        return 'All';
    }
  }

  resetSearch(): void {
    this.attendeeName.set('');
    this.orderId.set('');
    this.selectedQuickFilter.set('ALL');
    this.searchResultMessage.set(null);
  }

  trackById(_: number, item: CheckInRecord): number {
    return item.id;
  }
}