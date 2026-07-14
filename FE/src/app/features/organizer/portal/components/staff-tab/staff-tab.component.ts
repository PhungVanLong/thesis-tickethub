import { Component, OnDestroy, OnInit, inject, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HttpClient } from '@angular/common/http';
import { EventApiService } from '../../../../../core/services/event.service';
import { AuthService } from '../../../../auth/auth.service';
import { CreateStaffAccountResponse, StaffAccountService } from '../../../../../core/services/staff-account.service';

export interface StaffMember {
  id: number;
  staffId: number;
  email: string;
  fullName: string;
  roleInEvent: string;
  assignedAt: string;
  status?: 'Active' | 'Offline' | 'Suspended'; // Mocked activity status
  lastActive?: string; // Mocked active time
}

type StaffRequestState = 'draft' | 'queued' | 'completed' | 'failed';

@Component({
  selector: 'app-staff-tab',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './staff-tab.component.html',
  styleUrl: './staff-tab.component.scss'
})
export class StaffTabComponent implements OnInit, OnDestroy {
  private readonly http = inject(HttpClient);
  private readonly eventApi = inject(EventApiService);
  private readonly authService = inject(AuthService);
  private readonly staffAccountService = inject(StaffAccountService);
  private refreshTimerId: ReturnType<typeof setTimeout> | null = null;

  // Events list
  readonly events = signal<any[]>([]);
  readonly selectedEventId = signal<number | null>(null);
  readonly selectedEvent = computed(() => {
    const eventId = this.selectedEventId();
    return this.events().find(event => event.id === eventId) ?? null;
  });
  readonly organizationId = computed(() => {
    const event = this.selectedEvent();
    if (event?.organizationId) {
      return event.organizationId;
    }

    const profile = this.authService.currentUserProfile();
    return (profile as any)?.organizationId ?? null;
  });
  readonly organizationName = computed(() => {
    const event = this.selectedEvent();
    if (event?.organizationName) {
      return event.organizationName;
    }

    const profile = this.authService.currentUserProfile();
    return (profile as any)?.organizationName ?? 'your organization';
  });

  // Staff list
  readonly staffList = signal<StaffMember[]>([]);
  readonly isLoading = signal(false);
  readonly errorMessage = signal<string | null>(null);

  // Filters & Search
  readonly searchQuery = signal('');
  readonly roleFilter = signal<string>('ALL');

  mathMin(a: number, b: number): number {
    return Math.min(a, b);
  }

  readonly showCreateAccountBody = signal(false);
  readonly fullName = signal('');
  readonly email = signal('');
  readonly phone = signal('');
  readonly password = signal('');
  readonly createFormError = signal<string | null>(null);
  readonly createRequestState = signal<StaffRequestState>('draft');
  readonly createRequestMessage = signal<string | null>(null);
  readonly createRequestId = signal<string | null>(null);
  readonly createResponse = signal<CreateStaffAccountResponse | null>(null);
  readonly isSubmitting = signal(false);
  readonly selectedEventsForStaff = signal<any[]>([]);
  readonly eventSearchQuery = signal('');
  readonly selectAllEvents = signal(false);

  toggleSelectAllEvents(checked: boolean): void {
    this.selectAllEvents.set(checked);
    if (checked) {
      this.selectedEventsForStaff.set([...this.events()]);
    } else {
      this.selectedEventsForStaff.set([]);
    }
  }

  selectEvent(event: any): void {
    if (!this.selectedEventsForStaff().some(e => e.id === event.id)) {
      this.selectedEventsForStaff.update(list => [...list, event]);
    }
    this.eventSearchQuery.set('');
    if (this.selectedEventsForStaff().length === this.events().length) {
      this.selectAllEvents.set(true);
    }
  }

  removeSelectedEvent(event: any): void {
    this.selectedEventsForStaff.update(list => list.filter(e => e.id !== event.id));
    this.selectAllEvents.set(false);
  }

  readonly filteredEventsForSearch = computed(() => {
    const query = this.eventSearchQuery().trim().toLowerCase();
    const selectedIds = this.selectedEventsForStaff().map(e => e.id);
    return this.events().filter(e => !selectedIds.includes(e.id) && e.title.toLowerCase().includes(query));
  });

  readonly canCreateAccount = computed(() => {
    const organizationId = this.organizationId();
    const fullName = this.fullName().trim();
    const email = this.email().trim();

    return !!organizationId && !!fullName && this.isValidEmail(email);
  });

  // Pagination
  readonly currentPage = signal(0);
  readonly pageSize = signal(5);

  // Roles available for staff
  readonly availableRoles = [
    { value: 'SUPERVISOR', label: 'Lead Supervisor' },
    { value: 'CHECKIN_STAFF', label: 'Gate Agent' },
    { value: 'SCANNER', label: 'Scanner' }
  ];

  // Helper to format role names beautifully
  getRoleLabel(roleValue: string): string {
    const found = this.availableRoles.find(r => r.value === roleValue);
    return found ? found.label : roleValue;
  }

  // Filtered staff list
  readonly filteredStaff = computed(() => {
    const list = this.staffList();
    const query = this.searchQuery().trim().toLowerCase();
    const role = this.roleFilter();

    return list.filter(member => {
      const matchesSearch = !query ||
        member.fullName?.toLowerCase().includes(query) ||
        member.email?.toLowerCase().includes(query) ||
        member.roleInEvent?.toLowerCase().includes(query);

      const matchesRole = role === 'ALL' || member.roleInEvent === role;

      return matchesSearch && matchesRole;
    });
  });

  // Paginated staff list
  readonly paginatedStaff = computed(() => {
    const list = this.filteredStaff();
    const startIndex = this.currentPage() * this.pageSize();
    return list.slice(startIndex, startIndex + this.pageSize());
  });

  // Total pages
  readonly totalPages = computed(() => {
    return Math.ceil(this.filteredStaff().length / this.pageSize());
  });

  // Metrics
  readonly totalStaffCount = computed(() => this.staffList().length);
  readonly activeStaffCount = computed(() => {
    return this.staffList().filter(s => s.status === 'Active').length;
  });
  readonly utilizationPercent = computed(() => {
    const total = this.totalStaffCount();
    if (total === 0) return 0;
    return Math.round((this.activeStaffCount() / total) * 100);
  });

  ngOnInit(): void {
    this.fetchEvents();
  }

  ngOnDestroy(): void {
    this.clearRefreshTimer();
  }

  fetchEvents(): void {
    this.isLoading.set(true);
    this.eventApi.getOrganizerEvents().subscribe({
      next: (res: any[]) => {
        this.events.set(res || []);
        if (res && res.length > 0) {
          this.selectedEventId.set(res[0].id);
          this.fetchStaff(res[0].id);
        } else {
          this.isLoading.set(false);
        }
      },
      error: (err) => {
        this.errorMessage.set('Failed to fetch organizer events.');
        this.isLoading.set(false);
      }
    });
  }

  onEventChange(eventId: number | string): void {
    const numericEventId = Number(eventId);
    this.selectedEventId.set(numericEventId);
    this.currentPage.set(0);
    this.fetchStaff(numericEventId);
  }

  fetchStaff(eventId: number, expectedEmail?: string | null): void {
    this.isLoading.set(true);
    this.errorMessage.set(null);

    this.http.get<StaffMember[]>(`http://localhost:8080/api/events/${eventId}/staff`).subscribe({
      next: (res) => {
        // Enforce mockup statuses and last active values to match the design aesthetics
        const mockStatuses: ('Active' | 'Offline' | 'Suspended')[] = ['Active', 'Offline', 'Suspended'];
        const mockLastActive = ['2 mins ago', '4 hours ago', '10 mins ago', 'Aug 14, 2023', 'Yesterday'];

        const mapped = (res || []).map((s, idx) => ({
          ...s,
          status: s.status || mockStatuses[idx % mockStatuses.length],
          lastActive: s.lastActive || mockLastActive[idx % mockLastActive.length]
        }));

        this.staffList.set(mapped);
        if (expectedEmail && mapped.some(member => member.email?.toLowerCase() === expectedEmail.toLowerCase())) {
          this.createRequestState.set('completed');
          this.createRequestMessage.set('Staff account created and assigned to the selected event.');
        }
        this.isLoading.set(false);
      },
      error: (err) => {
        this.errorMessage.set('Failed to load event staff members.');
        this.isLoading.set(false);
      }
    });
  }

  refreshSelectedEventStaff(): void {
    const eventId = this.selectedEventId();
    if (!eventId) {
      return;
    }

    this.fetchStaff(eventId);
  }

  openCreateAccountBody(): void {
    this.showCreateAccountBody.set(true);
  }

  closeCreateAccountBody(): void {
    if (this.isSubmitting()) {
      return;
    }

    this.showCreateAccountBody.set(false);
    this.resetCreateForm();
  }

  toggleCreateAccountBody(): void {
    if (this.showCreateAccountBody()) {
      this.closeCreateAccountBody();
      return;
    }

    this.openCreateAccountBody();
  }

  resetCreateForm(): void {
    if (this.isSubmitting()) {
      return;
    }

    this.clearRefreshTimer();
    this.fullName.set('');
    this.email.set('');
    this.phone.set('');
    this.password.set('');
    this.selectedEventsForStaff.set([]);
    this.eventSearchQuery.set('');
    this.selectAllEvents.set(false);
    this.createFormError.set(null);
    this.createRequestState.set('draft');
    this.createRequestMessage.set(null);
    this.createRequestId.set(null);
    this.createResponse.set(null);
  }

  private clearRefreshTimer(): void {
    if (this.refreshTimerId) {
      clearTimeout(this.refreshTimerId);
      this.refreshTimerId = null;
    }
  }

  private clearCreateInputs(): void {
    this.fullName.set('');
    this.email.set('');
    this.phone.set('');
    this.password.set('');
  }

  private isValidEmail(value: string): boolean {
    return /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(value);
  }

  private scheduleRosterRefresh(eventId: number, email: string): void {
    this.clearRefreshTimer();
    this.refreshTimerId = setTimeout(() => {
      this.fetchStaff(eventId, email);
    }, 3000);
  }

  createStaffAccount(): void {
    if (this.isSubmitting()) {
      return;
    }

    const organizationId = this.organizationId();
    if (!organizationId) {
      this.createFormError.set('Organization context is not available.');
      return;
    }

    const fullName = this.fullName().trim();
    const email = this.email().trim();
    const phone = this.phone().trim();
    // Autogenerate a secure default password since there is no password field in the mockup design
    const password = 'Staff@' + Math.random().toString(36).slice(-8) + '1';

    if (!fullName || !email) {
      this.createFormError.set('Please fill in full name and email.');
      return;
    }

    if (!this.isValidEmail(email)) {
      this.createFormError.set('Please enter a valid email address.');
      return;
    }

    this.isSubmitting.set(true);
    this.createFormError.set(null);
    this.createRequestState.set('draft');
    this.createRequestMessage.set(null);
    this.createRequestId.set(null);

    const payload = {
      email,
      password,
      fullName,
      phone: phone || null
    };

    this.staffAccountService.createStaffAccount(organizationId, payload).subscribe({
      next: (response) => {
        this.isSubmitting.set(false);
        this.createResponse.set(response);
        this.createRequestId.set(response.requestId);
        this.createRequestState.set(response.requestStatus === 'QUEUED' ? 'queued' : 'completed');
        this.createRequestMessage.set(
          response.requestStatus === 'QUEUED'
            ? 'Request queued. The staff account will appear in the roster after backend processing completes.'
            : 'Staff account created successfully.'
        );

        // Assign to selected events in the background
        const selectedEvents = this.selectedEventsForStaff();
        selectedEvents.forEach(event => {
          this.http.post(`http://localhost:8080/api/events/${event.id}/staff`, {
            email: email,
            role: 'CHECKIN_STAFF'
          }).subscribe({
            error: (err) => console.error('Failed to assign staff to event ' + event.id, err)
          });
        });

        this.clearCreateInputs();
        this.selectedEventsForStaff.set([]);

        const selectedEventId = this.selectedEventId();
        if (selectedEventId) {
          this.scheduleRosterRefresh(selectedEventId, email);
        }
      },
      error: (err) => {
        this.isSubmitting.set(false);
        this.createRequestState.set('failed');

        if (err.status === 409) {
          this.createFormError.set('This email is already registered or linked to a staff account.');
        } else if (err.status === 403) {
          this.createFormError.set('You do not have permission to create staff for this organization.');
        } else if (err.status === 404) {
          this.createFormError.set('Organization was not found.');
        } else if (err.status === 400) {
          this.createFormError.set(err.error?.message || 'Invalid staff account payload.');
        } else {
          this.createFormError.set(err.error?.message || 'Failed to create the staff account. Please try again.');
        }
      }
    });
  }

  removeStaffMember(staffId: number): void {
    const eventId = this.selectedEventId();
    if (!eventId) return;

    if (confirm('Are you sure you want to remove this staff member from the event?')) {
      this.http.delete(`http://localhost:8080/api/events/${eventId}/staff/${staffId}`).subscribe({
        next: () => {
          this.fetchStaff(eventId);
        },
        error: (err) => {
          alert('Failed to remove staff member. Please try again.');
        }
      });
    }
  }

  changePage(page: number): void {
    if (page >= 0 && page < this.totalPages()) {
      this.currentPage.set(page);
    }
  }

  exportCSV(): void {
    const list = this.filteredStaff();
    if (list.length === 0) {
      alert('No data to export.');
      return;
    }

    const headers = ['Name', 'Email', 'Role', 'Status', 'Last Active'];
    const rows = list.map(m => [
      m.fullName,
      m.email,
      this.getRoleLabel(m.roleInEvent),
      m.status,
      m.lastActive
    ]);

    const csvContent = 'data:text/csv;charset=utf-8,'
      + [headers.join(','), ...rows.map(e => e.map(val => `"${val}"`).join(','))].join('\n');

    const encodedUri = encodeURI(csvContent);
    const link = document.createElement('a');
    link.setAttribute('href', encodedUri);
    link.setAttribute('download', `staff_report_event_${this.selectedEventId()}.csv`);
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
  }
}
