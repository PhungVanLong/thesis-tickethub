import { Component, inject, OnInit, OnDestroy, signal, computed, HostListener } from '@angular/core';
import { Observable, Subject } from 'rxjs';
import { take } from 'rxjs/operators';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
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

  paymentMethod = 'VNPAY';
  paymentCompleted = false;

  readonly showLeaveModal = signal(false);
  readonly showSuccessModal = signal(false);
  private leaveSubject = new Subject<boolean>();

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
    } else {
      this.router.navigate(['/']);
    }
  }

  // Intercept route changes within Angular
  canDeactivate(): Observable<boolean> | boolean {
    if (this.paymentCompleted) {
      return true;
    }
    this.showLeaveModal.set(true);
    return this.leaveSubject.asObservable().pipe(take(1));
  }

  confirmLeave(leave: boolean): void {
    this.showLeaveModal.set(false);
    if (leave) {
      this.cancelPendingOrder();
    }
    this.leaveSubject.next(leave);
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
      }
    });
  }

  payOrder() {
    const order = this.orderDetails();
    if (!order || this.paying()) return;

    this.paying.set(true);
    this.paymentError.set('');

    const payload = {
      orderId: this.orderId(),
      gateway: this.paymentMethod,
      returnUrl: `${window.location.origin}/payment-result`
    };

    this.http.post(`http://localhost:8080/api/bookings/${this.orderId()}/mock-pay`, {}).subscribe({
      next: () => {
        this.paymentCompleted = true;
        this.paying.set(false);
        this.showSuccessModal.set(true);
      },
      error: (err) => {
        this.paying.set(false);
        this.paymentError.set(err?.error?.message || 'Thanh toán thất bại.');
      }
    });
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
}
