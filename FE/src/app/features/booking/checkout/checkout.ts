import { Component, inject, OnInit, OnDestroy, signal, computed, HostListener } from '@angular/core';
import { Observable, Subject } from 'rxjs';
import { take } from 'rxjs/operators';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router, RouterLink, RouterStateSnapshot } from '@angular/router';
import { HttpClient } from '@angular/common/http';
import { FormsModule } from '@angular/forms';
import { Navigation } from '../../../core/navigation/navigation';
import { Footer } from '../../../core/footer/footer';
import { AuthService } from '../../auth/auth.service';

@Component({
  selector: 'app-checkout',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink, Navigation, Footer],
  templateUrl: './checkout.html',
  styleUrl: './checkout.scss'
})
export class CheckoutComponent implements OnInit, OnDestroy {
  private readonly route  = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly http   = inject(HttpClient);
  private readonly auth   = inject(AuthService);

  readonly orderId      = signal<string | null>(null);
  readonly orderDetails = signal<any>(null);
  readonly loading      = signal(true);
  readonly paying       = signal(false);
  readonly paymentError = signal('');
  readonly timeLeftStr  = signal<string>('10:00');
  readonly isTimerDanger = signal<boolean>(false);

  paymentMethod = 'VNPAY';
  paymentCompleted = false;

  readonly showLeaveModal = signal(false);
  readonly showSuccessModal = signal(false);
  readonly showProcessingModal = signal(false);
  private leaveSubject = new Subject<boolean>();
  private pollInterval: any;
  private countdownInterval: any;

  readonly userEmail = computed(() => {
    const profile = this.auth.currentUserProfile();
    return profile?.email || 'user@tickethub.vn';
  });

  ngOnInit() {
    window.scrollTo(0, 0);
    const id = this.route.snapshot.paramMap.get('orderId');
    if (id) {
      this.orderId.set(id);
      this.fetchOrderDetails(id);
      
      // Check query params for payment error
      this.route.queryParams.subscribe(params => {
        if (params['error'] === 'payment_failed') {
          this.paymentError.set('Payment was unsuccessful or cancelled by VNPay.');
        }
      });
    } else {
      this.router.navigate(['/']);
    }
  }

  private allowLeave = false;

  // Intercept route changes within Angular
  canDeactivate(currentRoute: any, currentState: any, nextState?: RouterStateSnapshot): Observable<boolean> | boolean {
    if (this.paymentCompleted || this.allowLeave) {
      return true;
    }
    this.showLeaveModal.set(true);
    return this.leaveSubject.asObservable().pipe(take(1));
  }

  confirmLeave(leave: boolean): void {
    this.showLeaveModal.set(false);
    if (leave) {
      this.allowLeave = true; // Prevent modal from showing again during redirect
      this.cancelPendingOrder();
      this.leaveSubject.next(true);
    } else {
      this.leaveSubject.next(false);
    }
  }

  // Intercept window close / tab refresh
  @HostListener('window:beforeunload', ['$event'])
  unloadNotification($event: any): void {
    if (!this.paymentCompleted) {
      this.cancelPendingOrder();
      $event.returnValue = true;
    }
  }

  ngOnDestroy() {
    if (this.pollInterval) {
      clearInterval(this.pollInterval);
    }
    if (this.countdownInterval) {
      clearInterval(this.countdownInterval);
    }
    if (!this.paymentCompleted) {
      this.cancelPendingOrder();
    }
  }

  private cancelPendingOrder() {
    const id = this.orderId();
    if (id) {
      this.http.post(`http://localhost:8080/api/bookings/${id}/mock-cancel`, {}).subscribe({
        next: () => console.log('Order cancelled, seats released back to AVAILABLE'),
        error: (err) => console.warn('Failed to cancel order', err)
      });
    }
  }

  private fetchOrderDetails(orderId: string) {
    this.http.get(`http://localhost:8080/api/orders/${orderId}`).subscribe({
      next: (res: any) => {
        this.orderDetails.set(res);
        this.loading.set(false);
        if (res.status === 'PAID') {
          this.paymentCompleted = true;
          this.showSuccessModal.set(true);
        } else if (res.status === 'REFUNDED') {
          this.paymentCompleted = true;
          this.paymentError.set('Seat conflict detected! This seat was booked by another user first. You have been fully refunded.');
        } else if (res.status === 'CANCELLED') {
          this.handleTimeout(false);
        } else {
          this.startCountdown(res.createdAt);
        }
      },
      error: () => {
        // Fallback dev data
        this.orderDetails.set({
          orderCode: orderId,
          eventTitle: 'Event Title',
          eventDate: new Date().toISOString(),
          eventVenue: 'Venue',
          bannerUrl: null,
          subtotal: 250000,
          totalAmount: 250000,
          status: 'PENDING',
          items: [
            { ticketTierName: 'BALCONY', price: 250000, quantity: 1, seatLabel: 'LD-24' }
          ]
        });
        this.loading.set(false);
        this.startCountdown(null);
      }
    });
  }

  startCountdown(createdAtStr: string | null) {
    if (this.countdownInterval) clearInterval(this.countdownInterval);

    const createdAt = createdAtStr ? new Date(createdAtStr).getTime() : Date.now();
    const expireTime = createdAt + 10 * 60 * 1000; // 10 minutes

    const updateTimer = () => {
      const now = Date.now();
      const diff = expireTime - now;

      if (diff <= 0) {
        clearInterval(this.countdownInterval);
        this.timeLeftStr.set('00:00');
        this.isTimerDanger.set(true);
        this.handleTimeout(true);
      } else {
        if (diff <= 120000) { // 2 minutes or less
          this.isTimerDanger.set(true);
        } else {
          this.isTimerDanger.set(false);
        }
        const mins = Math.floor(diff / 60000);
        const secs = Math.floor((diff % 60000) / 1000);
        this.timeLeftStr.set(`${mins.toString().padStart(2, '0')}:${secs.toString().padStart(2, '0')}`);
      }
    };

    updateTimer();
    this.countdownInterval = setInterval(updateTimer, 1000);
  }

  handleTimeout(showAlert = true) {
    if (this.pollInterval) clearInterval(this.pollInterval);
    if (this.countdownInterval) clearInterval(this.countdownInterval);

    this.paymentCompleted = true; // prevent exit guard
    this.allowLeave = true;

    if (showAlert) {
      alert('Your payment session has expired (over 10 minutes)! Your reserved seats have been released.');
    }

    const eventId = this.orderDetails()?.eventId;
    if (eventId) {
      this.router.navigate([`/event/${eventId}/booking`]);
    } else {
      this.router.navigate(['/']);
    }
  }

  payOrder() {
    const order = this.orderDetails();
    if (!order || this.paying()) return;

    this.paying.set(true);
    this.paymentError.set('');

    if (this.paymentMethod === 'VNPAY') {
      this.http.get<{ paymentUrl: string }>(`http://localhost:8080/api/bookings/${this.orderId()}/vnpay-url`).subscribe({
        next: (res) => {
          this.paying.set(false);
          this.showProcessingModal.set(true);
          window.open(res.paymentUrl, '_blank');
          this.startPaymentPolling();
        },
        error: (err) => {
          this.paying.set(false);
          this.paymentError.set(err?.error?.message || 'Failed to initiate VNPay payment.');
        }
      });
    } else if (this.paymentMethod === 'PAYPAL') {
      this.http.get<{ paymentUrl: string }>(`http://localhost:8080/api/bookings/${this.orderId()}/paypal-url`).subscribe({
        next: (res) => {
          this.paying.set(false);
          if (res.paymentUrl) {
            this.showProcessingModal.set(true);
            window.open(res.paymentUrl, '_blank');
            this.startPaymentPolling();
          } else {
            this.paymentError.set('Failed to initiate PayPal payment: Empty approval URL.');
          }
        },
        error: (err) => {
          this.paying.set(false);
          this.paymentError.set(err?.error?.message || 'Failed to initiate PayPal payment.');
        }
      });
    } else {
      // Mock Payment for other methods
      this.http.post(`http://localhost:8080/api/bookings/${this.orderId()}/mock-pay`, {}).subscribe({
        next: () => {
          this.paymentCompleted = true;
          this.paying.set(false);
          this.showSuccessModal.set(true);
        },
        error: (err) => {
          this.paying.set(false);
          this.paymentError.set(err?.error?.message || 'Payment failed.');
        }
      });
    }
  }

  closeSuccessModal(): void {
    this.showSuccessModal.set(false);
    this.router.navigate(['/my-account'], { queryParams: { tab: 'tickets' } });
  }

  normalizeImageUrl(url: string | null | undefined): string {
    if (!url) return '';
    const match = url.match(/drive\.google\.com\/file\/d\/([^\/]+)/);
    if (match?.[1]) return `https://drive.google.com/thumbnail?id=${match[1]}&sz=w1200`;
    return url;
  }

  formatEventDateTime(startStr: string, endStr: string | null | undefined): string {
    if (!startStr) return '';
    const start = new Date(startStr);
    
    const pad = (n: number) => n.toString().padStart(2, '0');
    const formatTime = (d: Date) => `${pad(d.getHours())}:${pad(d.getMinutes())}`;
    const formatDate = (d: Date) => `${d.getDate()}/${d.getMonth() + 1}/${d.getFullYear()}`;
    
    if (!endStr) {
      return `${formatTime(start)}, ${formatDate(start)}`;
    }
    
    const end = new Date(endStr);
    const isSameDay = start.getFullYear() === end.getFullYear() &&
                      start.getMonth() === end.getMonth() &&
                      start.getDate() === end.getDate();
                      
    if (isSameDay) {
      return `${formatTime(start)} – ${formatTime(end)}, ${formatDate(start)}`;
    } else {
      return `${formatTime(start)}, ${formatDate(start)} – ${formatTime(end)}, ${formatDate(end)}`;
    }
  }

  startPaymentPolling() {
    if (this.pollInterval) clearInterval(this.pollInterval);
    
    this.pollInterval = setInterval(() => {
      const orderId = this.orderId();
      if (!orderId) return;
      
      this.http.get(`http://localhost:8080/api/orders/${orderId}`).subscribe({
        next: (res: any) => {
          if (res.status === 'PAID') {
            clearInterval(this.pollInterval);
            this.showProcessingModal.set(false);
            this.paymentCompleted = true;
            this.showSuccessModal.set(true);
          } else if (res.status === 'CANCELLED') {
            clearInterval(this.pollInterval);
            this.showProcessingModal.set(false);
            this.paymentError.set('Payment was cancelled or failed.');
          } else if (res.status === 'REFUNDED') {
            clearInterval(this.pollInterval);
            this.showProcessingModal.set(false);
            this.paymentCompleted = true;
            this.paymentError.set('Seat conflict detected! This seat was booked by another user first. You have been fully refunded.');
          }
        }
      });
    }, 3000);
  }

  cancelProcessing() {
    if (this.pollInterval) clearInterval(this.pollInterval);
    this.showProcessingModal.set(false);
    this.cancelPendingOrder();
    this.paying.set(false);
  }
}
