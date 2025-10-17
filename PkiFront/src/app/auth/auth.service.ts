import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, tap } from 'rxjs';
import { jwtDecode } from 'jwt-decode';

export interface RegisterRequest {
  firstName: string;
  lastName: string;
  email: string;
  password: string
  confirmPassword: string
  organization?: string;
}
export interface LoginRequest {
  email: string;
  password: string;
  recaptchaToken: string;
}
export interface LoginResponse {
  token: string;
}
export interface DecodedToken {
  sub: string;      // Subject (email)
  role: string;     // Naš custom claim
  firstName: string;
  lastName: string;
  userId: number;
  exp: number;      // Expiration time
  iat: number;      // Issued at
  jti: string;      // JWT ID
}
@Injectable({
  providedIn: 'root'
})
export class AuthService {
  private apiUrl = 'https://localhost:8443/api/auth';
  private readonly AUTH_TOKEN_KEY = 'authToken';
  constructor(private http: HttpClient) { }

   register(userData: RegisterRequest): Observable<any> {
    return this.http.post(`${this.apiUrl}/register`, userData, { responseType: 'text' });
  }
   login(credentials: LoginRequest): Observable<LoginResponse> {
    return this.http.post<LoginResponse>(`${this.apiUrl}/login`, credentials).pipe(
      tap(response => {
        this.setToken(response.token);
      })
    );
  }

  // NOVA METODA: Čuva token u localStorage
  private setToken(token: string): void {
    localStorage.setItem(this.AUTH_TOKEN_KEY, token);
  }


  getToken(): string | null {
    return localStorage.getItem(this.AUTH_TOKEN_KEY);
  }

  isLoggedIn(): boolean {
    return this.getToken() !== null;
  }

  logout(): void {
    localStorage.removeItem(this.AUTH_TOKEN_KEY);
    window.location.href = '/login';
  }
  getUserDetails(): DecodedToken | null {
    const token = this.getToken();
    if (token) {
      try {
        const decodedToken: DecodedToken = jwtDecode(token);
        if (decodedToken.exp * 1000 < Date.now()) {
          this.logout();
          return null;
        }
        return decodedToken;
      } catch (error) {
        console.error("Failed to decode token:", error);
        return null;
      }
    }
    return null;
  }
}
