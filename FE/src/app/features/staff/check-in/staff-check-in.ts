import { CommonModule } from '@angular/common';
import { Component, computed, inject, OnInit, OnDestroy, signal, HostListener } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { AuthService } from '../../auth/auth.service';
import { LanguageService } from '../../../core/services/language.service';
import { HttpClient } from '@angular/common/http';
import { EventApiService } from '../../../core/services/event.service';

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
  selector: 'app-staff-check-in',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './staff-check-in.html',
  styleUrl: './staff-check-in.scss',
})
export class StaffCheckInComponent implements OnInit, OnDestroy {
  private readonly authService = inject(AuthService);
  private readonly router = inject(Router);
  private readonly langService = inject(LanguageService);
  private readonly http = inject(HttpClient);
  private readonly eventService = inject(EventApiService);

  readonly userProfile = this.authService.currentUserProfile;
  readonly currentLang = this.langService.currentLang;
  readonly showLangDropdown = signal(false);
  readonly showUserMenu = signal(false);

  readonly events = signal<StaffCheckInEvent[]>([]);
  readonly selectedEventId = signal<number>(0);
  readonly activeTab = signal<'scan' | 'history'>('scan');
  readonly historyStatusFilter = signal<'ALL' | 'SUCCESS' | 'FAILED' | 'FLAGGED'>('ALL');
  readonly historyDateFilter = signal<string>('TODAY');
  readonly historySearchQuery = signal<string>('');
  readonly scannerOpen = signal(false);
  readonly scannerMessage = signal('Place the attendee\'s QR code within the camera frame or connect an external scanner.');

  readonly attendeeName = signal('');
  readonly orderId = signal('');
  readonly selectedQuickFilter = signal<'ALL' | 'VIP' | 'UNPAID' | 'STAFF'>('ALL');
  readonly searchResultMessage = signal<string | null>(null);
  readonly filterOptions: ('VIP' | 'UNPAID' | 'STAFF')[] = ['VIP', 'UNPAID', 'STAFF'];

  readonly recentCheckIns = signal<CheckInRecord[]>([]);
  readonly ticketCodeInput = signal('');
  readonly errorMessage = signal<string | null>(null);
  readonly successMessage = signal<string | null>(null);

  // Camera settings
  readonly cameras = signal<MediaDeviceInfo[]>([]);
  readonly selectedCameraId = signal<string>('');
  private mediaStream: MediaStream | null = null;
  private jsQRLoaded = false;
  private checkinSse: EventSource | null = null;

  readonly selectedEvent = computed(() => {
    return this.events().find(event => event.id === this.selectedEventId()) ?? this.events()[0] ?? null;
  });

  readonly checkedInCount = computed(() => this.selectedEvent()?.checkedIn ?? 0);
  readonly totalCapacity = computed(() => this.selectedEvent()?.total ?? 0);
  readonly capacityPercent = computed(() => this.selectedEvent()?.capacity ?? 0);
  readonly remainingSeats = computed(() => Math.max(this.totalCapacity() - this.checkedInCount(), 0));

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

  readonly totalEntriesToday = computed(() => this.recentCheckIns().length);
  
  readonly successfulScansPercent = computed(() => {
    const total = this.recentCheckIns().length;
    if (total === 0) return 100;
    const success = this.recentCheckIns().filter(r => r.status === 'Verified').length;
    return Math.round((success / total) * 1000) / 10;
  });

  readonly manualEntryCount = computed(() => this.recentCheckIns().filter(r => r.method === 'MANUAL').length);

  readonly failedScansCount = computed(() => this.recentCheckIns().filter(r => r.status === 'Flagged').length);

  readonly peakEntryTime = computed(() => {
    const checkins = this.recentCheckIns();
    if (checkins.length === 0) return 'N/A';
    const hours: Record<number, number> = {};
    checkins.forEach(c => {
      const parts = c.timestamp.split(':');
      if (parts.length > 0) {
        const hr = parseInt(parts[0], 10);
        if (!isNaN(hr)) {
          hours[hr] = (hours[hr] || 0) + 1;
        }
      }
    });
    let peakHr = -1;
    let maxCount = -1;
    for (const hr in hours) {
      const h = parseInt(hr, 10);
      if (hours[h] > maxCount) {
        maxCount = hours[h];
        peakHr = h;
      }
    }
    if (peakHr === -1) return 'N/A';
    const ampm = peakHr >= 12 ? 'PM' : 'AM';
    const displayHr = peakHr % 12 || 12;
    return `${String(displayHr).padStart(2, '0')}:00 ${ampm}`;
  });

  readonly currentDate = new Date();

  readonly filteredRecentCheckIns = computed(() => {
    const nameQ = this.attendeeName().trim().toLowerCase();
    const orderQ = this.orderId().trim().toLowerCase();
    const filter = this.selectedQuickFilter();

    return this.recentCheckIns().filter(record => {
      const matchesName = !nameQ || 
        record.attendeeName.toLowerCase().includes(nameQ) || 
        record.email.toLowerCase().includes(nameQ);
      const matchesOrder = !orderQ || 
        record.orderId.toLowerCase().includes(orderQ);
      const matchesFilter = filter === 'ALL' || record.tags.includes(filter);
      return matchesName && matchesOrder && matchesFilter;
    });
  });

  ngOnInit(): void {
    const profile = this.authService.currentUserProfile();
    const role = profile?.role || '';

    if (profile && !role.includes('STAFF')) {
      this.router.navigate(['/']);
      return;
    }

    this.loadEvents();
  }

  ngOnDestroy(): void {
    this.stopCamera();
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
    this.http.get<any>(`http://localhost:8080/api/tickets/staff/checkins?eventId=${eventId}`).subscribe({
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
            staffName: c.staffName || 'System Auto'
          };
        });
        this.recentCheckIns.set(records);

        // Update the checkedIn count for the active event
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
        
        // Check if the record already exists in UI to prevent duplicates
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
          staffName: c.staffName || 'System Auto'
        };
        
        // Prepend to recent check-ins list
        this.recentCheckIns.update(list => [record, ...list]);

        // Increment checkedIn count for the active event
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

    this.checkinSse.onerror = (err) => {
      console.warn('SSE connection lost or error occurred', err);
    };
  }

  loadJsQR(): Promise<void> {
    if ((window as any).jsQR) {
      return Promise.resolve();
    }
    return new Promise((resolve, reject) => {
      const script = document.createElement('script');
      script.src = 'https://cdn.jsdelivr.net/npm/jsqr@1.4.0/dist/jsQR.min.js';
      script.onload = () => {
        this.jsQRLoaded = true;
        resolve();
      };
      script.onerror = (err) => {
        reject(err);
      };
      document.body.appendChild(script);
    });
  }

  openScanner(): void {
    this.scannerOpen.set(true);
    this.scannerMessage.set('Requesting camera permission...');

    this.loadJsQR()
      .then(() => {
        return navigator.mediaDevices.getUserMedia({ video: true });
      })
      .then((tempStream) => {
        // Release permission stream immediately
        tempStream.getTracks().forEach(track => track.stop());

        return navigator.mediaDevices.enumerateDevices();
      })
      .then((devices) => {
        const videoDevices = devices.filter(d => d.kind === 'videoinput');
        this.cameras.set(videoDevices);
        if (videoDevices.length > 0) {
          const defaultCam = videoDevices[0].deviceId;
          this.selectedCameraId.set(defaultCam);
          this.startCamera(defaultCam);
        } else {
          this.scannerMessage.set('No cameras found.');
        }
      })
      .catch((err) => {
        console.error('Camera init failed', err);
        this.scannerMessage.set('Could not access cameras. Please ensure permissions are granted.');
      });
  }

  startCamera(deviceId: string): void {
    this.stopCamera();
    this.selectedCameraId.set(deviceId);

    const constraints = {
      video: deviceId ? { deviceId: { exact: deviceId } } : { facingMode: 'environment' }
    };

    navigator.mediaDevices.getUserMedia(constraints)
      .then((stream) => {
        this.mediaStream = stream;
        // Wait a tick for video element to render
        setTimeout(() => {
          const video = document.getElementById('scanner-video') as HTMLVideoElement;
          if (video) {
            video.srcObject = stream;
            video.setAttribute('playsinline', 'true');
            video.play().catch(e => console.error('Video play failed', e));
            this.scannerMessage.set('Point camera at a ticket QR code.');
            requestAnimationFrame(() => this.scanFrame());
          }
        }, 100);
      })
      .catch((err) => {
        console.error('Failed to start camera', err);
        this.scannerMessage.set('Failed to open camera: ' + err.message);
      });
  }

  stopCamera(): void {
    if (this.mediaStream) {
      this.mediaStream.getTracks().forEach(track => track.stop());
      this.mediaStream = null;
    }
  }

  closeScanner(): void {
    this.stopCamera();
    this.scannerOpen.set(false);
  }

  scanFrame(): void {
    if (!this.mediaStream || !this.scannerOpen()) {
      return;
    }

    const video = document.getElementById('scanner-video') as HTMLVideoElement;
    if (video && video.readyState === video.HAVE_ENOUGH_DATA) {
      let canvas = document.getElementById('scanner-canvas') as HTMLCanvasElement;
      if (!canvas) {
        canvas = document.createElement('canvas');
        canvas.id = 'scanner-canvas';
      }

      const width = video.videoWidth;
      const height = video.videoHeight;
      canvas.width = width;
      canvas.height = height;

      const ctx = canvas.getContext('2d');
      if (ctx) {
        ctx.drawImage(video, 0, 0, width, height);
        const imageData = ctx.getImageData(0, 0, width, height);
        
        const code = (window as any).jsQR(imageData.data, imageData.width, imageData.height, {
          inversionAttempts: 'dontInvert',
        });

        if (code && code.data) {
          // Found QR code!
          this.ticketCodeInput.set(code.data);
          this.verifyTicket('QR_SCAN');
          this.closeScanner();
          return;
        }
      }
    }

    // Next frame
    requestAnimationFrame(() => this.scanFrame());
  }

  verifyTicket(method: 'QR_SCAN' | 'MANUAL' = 'MANUAL'): void {
    const code = this.ticketCodeInput().trim();
    if (!code) {
      this.errorMessage.set('Please enter a ticket code');
      this.successMessage.set(null);
      return;
    }

    this.errorMessage.set(null);
    this.successMessage.set(null);

    const payload = {
      ticketCode: code,
      deviceId: 'Gate-1-Web',
      method: method
    };

    this.http.post<any>('http://localhost:8080/api/tickets/staff/checkin', payload).subscribe({
      next: (res) => {
        this.successMessage.set(`Successfully checked in! Seat: ${res.seatCode || 'N/A'} - Tier: ${res.tierName || 'N/A'}`);
        this.ticketCodeInput.set('');
        this.loadCheckins(this.selectedEventId());
      },
      error: (err) => {
        console.error('Checkin failed', err);
        const errMsg = err.error?.message || 'Verification failed. Please check ticket code or assignment.';
        this.errorMessage.set(errMsg);
      }
    });
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

  toggleLangDropdown(event: Event): void {
    event.stopPropagation();
    this.showLangDropdown.update(v => !v);
    this.showUserMenu.set(false);
  }

  toggleUserMenu(event: Event): void {
    event.stopPropagation();
    this.showUserMenu.update(v => !v);
    this.showLangDropdown.set(false);
  }

  setLanguage(lang: 'Vie' | 'Eng'): void {
    this.langService.setLanguage(lang);
    this.showLangDropdown.set(false);
  }

  @HostListener('document:click')
  onDocumentClick(): void {
    this.showLangDropdown.set(false);
    this.showUserMenu.set(false);
  }

  logout(): void {
    this.authService.logout();
    this.router.navigate(['/login']);
  }

  get userInitial(): string {
    const name = this.userProfile()?.fullName || this.userProfile()?.email || 'S';
    return name.charAt(0).toUpperCase();
  }

  get userName(): string {
    return this.userProfile()?.fullName || this.userProfile()?.email || 'Staff';
  }
}