import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export type TierType = 'SEATED' | 'STANDING';
export type EventStatus = 'PENDING' | 'APPROVED' | 'PUBLISHED' | 'CANCELLED';
export type ApprovalDecision = 'APPROVED' | 'REJECTED';

export interface TicketTierPayload {
  name: string;
  tierType: TierType;
  price: number;
  quantityTotal: number;
  colorCode?: string;
  saleStart?: string | null;
  saleEnd?: string | null;
}

export interface SeatPayload {
  seatCode: string;
  rowLabel: string;
  colNumber: number;
  ticketTierName: string;
}

export interface SeatMapPayload {
  name: string;
  totalRows: number;
  totalCols: number;
  layoutJson?: string;
  seats?: SeatPayload[];
}

export interface CreateEventPayload {
  organizationId: number;
  title: string;
  description?: string;
  venue: string;
  city: string;
  locationCoords?: string | null;
  startTime: string | null;
  endTime: string | null;
  bannerUrl?: string | null;
  ticketTiers?: TicketTierPayload[];
  seatMaps?: SeatMapPayload[];
}

export interface CreateEventResult {
  id: number;
  status: EventStatus;
  createdAt: string;
  updatedAt: string;
}

@Injectable({ providedIn: 'root' })
export class EventApiService {
  private readonly http = inject(HttpClient);
  private readonly API = 'http://localhost:8080/api/events';

  /** Tạo sự kiện mới (PENDING) */
  createEvent(payload: CreateEventPayload): Observable<CreateEventResult> {
    return this.http.post<CreateEventResult>(`${this.API}/create`, payload);
  }

  /** Lấy danh sách sự kiện, lọc theo status nếu cần */
  getEvents(status?: EventStatus): Observable<any[]> {
    const params: any = {};
    if (status) params['status'] = status;
    return this.http.get<any[]>(this.API, { params });
  }

  /** Organizer lấy danh sách sự kiện của mình */
  getOrganizerEvents(): Observable<any[]> {
    return this.http.get<any[]>(`${this.API}/organizer/my-events`);
  }

  /** Lấy chi tiết thông tin của một sự kiện */
  getEventDetail(eventId: number): Observable<any> {
    return this.http.get<any>(`${this.API}/${eventId}`);
  }

  /** Trả về URL của luồng SSE để lắng nghe cập nhật sơ đồ ghế */
  getSeatMapStreamUrl(eventId: number): string {
    return `${this.API}/${eventId}/seat-maps/stream`;
  }

  /** Organizer publish sự kiện (phải ở trạng thái APPROVED) */
  publishEvent(eventId: number): Observable<{ message: string }> {
    return this.http.post<{ message: string }>(`${this.API}/${eventId}/publish`, {});
  }

  /** Organizer huỷ sự kiện */
  cancelEvent(eventId: number): Observable<{ message: string }> {
    return this.http.post<{ message: string }>(`${this.API}/${eventId}/cancel`, {});
  }

  /** Admin duyệt / từ chối sự kiện */
  approveEvent(eventId: number, decision: ApprovalDecision, reason: string): Observable<any> {
    return this.http.post(`${this.API}/${eventId}/approve`, { decision, reason });
  }
}
