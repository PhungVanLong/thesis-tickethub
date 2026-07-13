import { Component, inject, OnInit, OnDestroy, signal, computed } from '@angular/core';
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
  promoCode     = '';

  // Countdown timer
  readonly countdownMinutes = signal(15);
  readonly countdownSeconds = signal(0);
  private timer: any;

  readonly userEmail = computed(() => {
    const profile = this.auth.currentUserProfile();
    return profile?.email || 'user@tickethub.vn';
  });

  readonly timerDisplay = computed(() => {
    const m = String(this.countdownMinutes()).padStart(2, '0');
    const s = String(this.countdownSeconds()).padStart(2, '0');
    return `${m}:${s}`;
  });

  readonly timerDanger = computed(() => this.countdownMinutes() < 3);

  ngOnInit() {
    window.scrollTo(0, 0);
    const id = this.route.snapshot.paramMap.get('orderId');
    if (id) {
      this.orderId.set(id);
      this.fetchOrderDetails(id);
      this.startTimer();
    } else {
      this.router.navigate(['/']);
    }
  }

  ngOnDestroy() {
    if (this.timer) clearInterval(this.timer);
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

  private startTimer() {
    this.timer = setInterval(() => {
      const s = this.countdownSeconds();
      const m = this.countdownMinutes();
      if (s === 0) {
        if (m === 0) {
          clearInterval(this.timer);
          this.handleTimeout();
          return;
        }
        this.countdownMinutes.set(m - 1);
        this.countdownSeconds.set(59);
      } else {
        this.countdownSeconds.set(s - 1);
      }
    }, 1000);
  }

  private handleTimeout() {
    alert('Thời gian thanh toán đã hết. Đơn hàng sẽ bị hủy.');
    this.router.navigate(['/']);
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

    // In production, call payment API
    // this.http.post('http://localhost:8080/api/payments/create', payload).subscribe(...)
    setTimeout(() => {
      alert(`Đang chuyển hướng đến ${this.paymentMethod}...`);
      this.paying.set(false);
    }, 1500);
  }

  normalizeImageUrl(url: string | null | undefined): string {
    if (!url) return '';
    const match = url.match(/drive\.google\.com\/file\/d\/([^\/]+)/);
    if (match?.[1]) return `https://drive.google.com/thumbnail?id=${match[1]}&sz=w1200`;
    return url;
  }
}
