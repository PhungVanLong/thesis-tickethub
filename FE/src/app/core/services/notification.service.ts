import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface Notification {
  id: number;
  userId: number | null;
  recipientRole: string | null;
  title: string;
  message: string;
  eventId: number | null;
  read: boolean;
  createdAt: string;
}

@Injectable({ providedIn: 'root' })
export class NotificationService {
  private readonly http = inject(HttpClient);
  private readonly API = 'http://localhost:8080/api/notifications';

  getNotifications(): Observable<Notification[]> {
    return this.http.get<Notification[]>(this.API);
  }

  markAsRead(id: number): Observable<any> {
    return this.http.post<any>(`${this.API}/${id}/read`, {});
  }

  markAllAsRead(): Observable<any> {
    return this.http.post<any>(`${this.API}/read-all`, {});
  }
}
