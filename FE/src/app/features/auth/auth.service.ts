import { HttpClient } from '@angular/common/http';
import { inject, Injectable, signal } from '@angular/core';
import { Observable, tap, catchError, throwError } from 'rxjs';
import { AuthResponse, LoginRequest, RegisterRequest } from './auth';

@Injectable({
  providedIn: 'root',
})
export class AuthService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = 'http://localhost:8080/api/auth/login';

  readonly currentUserToken = signal<string | null>(localStorage.getItem('accessToken'));
  readonly currentUserProfile = signal<any>(null);
  readonly isLoggingIn = signal<boolean>(false);
  readonly loginError = signal<string | null>(null);

  constructor() {
    this.initializeProfile();
  }

  private initializeProfile(): void {
    const token = this.currentUserToken();
    if (token) {
      this.getProfile().subscribe({
        next: (profile) => this.currentUserProfile.set(profile),
        error: () => this.logout(),
      });
    }
  }

  login(credentials: LoginRequest): Observable<AuthResponse> {
    this.isLoggingIn.set(true);
    this.loginError.set(null);

    return this.http.post<AuthResponse>(this.apiUrl, credentials).pipe(
      tap((response) => {
        if (response.accessToken) {
          localStorage.setItem('accessToken', response.accessToken);
          this.currentUserToken.set(response.accessToken);
          this.initializeProfile();
        }
        this.isLoggingIn.set(false);
      }),
      catchError((error) => {
        let errorMsg = 'An error occurred. Please verify your credentials.';
        if (error.error && typeof error.error === 'object') {
          if (error.error.message) {
            errorMsg = error.error.message;
          } else if (error.error.error) {
            errorMsg = error.error.error;
          }
        } else if (error.error && typeof error.error === 'string') {
          errorMsg = error.error;
        }
        this.loginError.set(errorMsg);
        this.isLoggingIn.set(false);
        return throwError(() => new Error(errorMsg));
      })
    );
  }

  register(data: RegisterRequest): Observable<any> {
    const registerUrl = 'http://localhost:8080/api/auth/register';
    return this.http.post(registerUrl, data);
  }

  logout(): void {
    localStorage.removeItem('accessToken');
    this.currentUserToken.set(null);
    this.currentUserProfile.set(null);
  }

  forgotPassword(email: string): Observable<string> {
    const forgotUrl = 'http://localhost:8080/api/auth/forgot-password';
    return this.http.post(forgotUrl, { email }, { responseType: 'text' });
  }

  resetPassword(data: { token: string; newPassword: string }): Observable<string> {
    const resetUrl = 'http://localhost:8080/api/auth/reset-password';
    return this.http.post(resetUrl, data, { responseType: 'text' });
  }

  getProfile(): Observable<any> {
    const token = this.currentUserToken();
    if (!token) {
      return throwError(() => new Error('No access token found'));
    }
    const userId = this.getUserIdFromToken(token);
    if (!userId) {
      return throwError(() => new Error('Invalid token payload'));
    }
    const headers = { Authorization: `Bearer ${token}` };
    return this.http.get(`http://localhost:8080/api/users/${userId}`, { headers }).pipe(
      tap((profile) => this.currentUserProfile.set(profile))
    );
  }

  updateProfile(data: any): Observable<any> {
    const token = this.currentUserToken();
    if (!token) {
      return throwError(() => new Error('No access token found'));
    }
    const userId = this.getUserIdFromToken(token);
    if (!userId) {
      return throwError(() => new Error('Invalid token payload'));
    }
    const headers = { Authorization: `Bearer ${token}` };
    return this.http.put(`http://localhost:8080/api/users/${userId}`, data, { headers }).pipe(
      tap((profile) => this.currentUserProfile.set(profile))
    );
  }

  getUserIdFromToken(token: string): string | null {
    try {
      const payload = token.split('.')[1];
      const decodedPayload = atob(payload.replace(/-/g, '+').replace(/_/g, '/'));
      const decoded = JSON.parse(decodedPayload);
      return decoded.sub || null;
    } catch (e) {
      console.error('Failed to decode JWT token:', e);
      return null;
    }
  }

  registerOrganizer(data: any): Observable<any> {
    const token = this.currentUserToken();
    if (!token) {
      return throwError(() => new Error('No access token found'));
    }
    const headers = { Authorization: `Bearer ${token}` };
    return this.http.post('http://localhost:8080/api/organizations', data, { headers });
  }

  isAuthenticated(): boolean {
    return !!this.currentUserToken();
  }
}
