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
  readonly isLoggingIn = signal<boolean>(false);
  readonly loginError = signal<string | null>(null);

  login(credentials: LoginRequest): Observable<AuthResponse> {
    this.isLoggingIn.set(true);
    this.loginError.set(null);

    return this.http.post<AuthResponse>(this.apiUrl, credentials).pipe(
      tap((response) => {
        if (response.accessToken) {
          localStorage.setItem('accessToken', response.accessToken);
          this.currentUserToken.set(response.accessToken);
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
  }

  isAuthenticated(): boolean {
    return !!this.currentUserToken();
  }
}
