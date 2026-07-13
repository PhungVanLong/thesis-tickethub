import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface BookingItemRequest {
  seatId?: number;
  ticketTierId?: number;
  promotionId?: number;
}

export interface CreateBookingRequest {
  eventId: number;
  customerId: number;
  idempotencyKey: string;
  voucherCode?: string;
  items: BookingItemRequest[];
}

@Injectable({ providedIn: 'root' })
export class BookingApiService {
  private readonly http = inject(HttpClient);
  private readonly API = 'http://localhost:8080/api/bookings';

  /** Gửi yêu cầu đặt vé và nhận lại requestId */
  submitBooking(payload: CreateBookingRequest): Observable<{ requestId: string }> {
    return this.http.post<{ requestId: string }>(this.API, payload);
  }

  /** Trả về URL của luồng SSE để lắng nghe kết quả */
  getBookingStreamUrl(requestId: string): string {
    return `${this.API}/stream/${requestId}`;
  }
}
