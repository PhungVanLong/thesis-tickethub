import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface CreateStaffAccountPayload {
    email: string;
    password: string;
    fullName: string;
    phone?: string | null;
}

export interface CreateStaffAccountResponse {
    requestId: string;
    requestStatus: 'QUEUED' | 'COMPLETED' | 'FAILED' | string;
    organizationId: number;
    organizationName: string;
    userId: number | null;
    email: string;
    fullName: string;
    role: string;
    organizationRole: string;
    assignedAt: string | null;
    createdAt: string | null;
    updatedAt: string | null;
}

@Injectable({
    providedIn: 'root',
})
export class StaffAccountService {
    private readonly http = inject(HttpClient);
    private readonly apiBase = 'http://localhost:8080/api/organizations';

    createStaffAccount(organizationId: number, payload: CreateStaffAccountPayload): Observable<CreateStaffAccountResponse> {
        return this.http.post<CreateStaffAccountResponse>(`${this.apiBase}/${organizationId}/staff-accounts`, payload);
    }
}