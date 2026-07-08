import { HttpClient, HttpHeaders } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { AuthService } from '../auth/auth.service';

export interface OrganizationResponse {
  id: number;
  name: string;
  abbreviationName?: string;
  taxCode?: string;
  representativeName?: string;
  representativePosition?: string;
  hotline?: string;
  officialEmail?: string;
  provinceCity?: string;
  district?: string;
  wardCommune?: string;
  headquarterAddress?: string;
  websiteUrl?: string;
  fanpageUrl?: string;
  description?: string;
  status: 'PENDING' | 'ACTIVE' | 'REJECTED' | 'BANNED';
  verifiedByAdminId?: number;
  verifiedAt?: string;
  verificationReason?: string;
  syncedAt?: string;
}

export interface EventResponse {
  id: number;
  organizationId: number;
  title: string;
  description?: string;
  venue: string;
  city: string;
  startTime: string;
  endTime: string;
  bannerUrl?: string;
  status: 'PENDING' | 'APPROVED' | 'PUBLISHED' | 'CANCELLED';
  isPublished: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface OrganizationVerificationRequest {
  decision: 'ACTIVE' | 'REJECTED' | 'BANNED';
  reason?: string;
}

export interface EventApprovalRequest {
  decision: 'APPROVED' | 'REJECTED';
  reason?: string;
}

@Injectable({ providedIn: 'root' })
export class AdminService {
  private readonly http = inject(HttpClient);
  private readonly authService = inject(AuthService);
  private readonly baseUrl = 'http://localhost:8080';

  private getHeaders(): HttpHeaders {
    const token = this.authService.currentUserToken();
    return new HttpHeaders({ Authorization: `Bearer ${token}` });
  }

  getOrganizations(status?: string): Observable<OrganizationResponse[]> {
    const params = status ? `?status=${status}` : '';
    return this.http.get<OrganizationResponse[]>(
      `${this.baseUrl}/api/organizations${params}`,
      { headers: this.getHeaders() }
    );
  }

  getOrganizationById(id: number): Observable<OrganizationResponse> {
    return this.http.get<OrganizationResponse>(
      `${this.baseUrl}/api/organizations/${id}`,
      { headers: this.getHeaders() }
    );
  }

  verifyOrganization(id: number, body: OrganizationVerificationRequest): Observable<OrganizationResponse> {
    return this.http.post<OrganizationResponse>(
      `${this.baseUrl}/api/organizations/${id}/verify`,
      body,
      { headers: this.getHeaders() }
    );
  }

  getEvents(status?: string): Observable<EventResponse[]> {
    const params = status ? `?status=${status}` : '';
    return this.http.get<EventResponse[]>(
      `${this.baseUrl}/api/events${params}`,
      { headers: this.getHeaders() }
    );
  }

  getEventById(id: number): Observable<EventResponse> {
    return this.http.get<EventResponse>(
      `${this.baseUrl}/api/events/${id}`,
      { headers: this.getHeaders() }
    );
  }

  approveEvent(eventId: number, body: EventApprovalRequest): Observable<any> {
    return this.http.post<any>(
      `${this.baseUrl}/api/events/${eventId}/approve`,
      body,
      { headers: this.getHeaders() }
    );
  }
}
