import { Injectable } from '@angular/core';
import { BehaviorSubject, Observable, throwError } from 'rxjs';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { map, catchError, switchMap } from 'rxjs/operators';
import { environment } from '../../../environments/environment';

export interface User {
  id: number;
  username: string;
  email: string;
  userType: string;
  firstName: string;
  lastName: string;
  phone?: string;
  riskProfile?: string;
  isActive: boolean;
  isVerified: boolean;
}

export interface LoginRequest {
  username: string;
  password: string;
}

export interface RegisterRequest {
  username: string;
  email: string;
  password: string;
  firstName?: string;
  lastName?: string;
  phone?: string;
}

export interface AuthResponse {
  token: string;
  username: string;
  email: string;
  userType: string;
  userId: number;
}

@Injectable({
  providedIn: 'root'
})
export class AuthService {
  private currentUserSubject = new BehaviorSubject<User | null>(null);
  public currentUser$ = this.currentUserSubject.asObservable();
  private token: string | null = null;

  constructor(private http: HttpClient) {
    // Load token from localStorage on service initialization
    this.token = localStorage.getItem('token');
    if (this.token) {
      this.loadUserProfile();
    }
  }

  private getHeaders(): HttpHeaders {
    let headers = new HttpHeaders({
      'Content-Type': 'application/json'
    });
    
    if (this.token) {
      headers = headers.set('Authorization', `Bearer ${this.token}`);
    }
    
    return headers;
  }

  login(username: string, password: string): Observable<boolean> {
    const loginData: LoginRequest = { username, password };
    
    return this.http.post<AuthResponse>(`${environment.apiUrl}/auth/login`, loginData, {
      headers: this.getHeaders()
    }).pipe(
      switchMap(response => {
        this.token = response.token;
        localStorage.setItem('token', this.token);
        
        // Load user profile and wait for it to complete
        return this.http.get<User>(`${environment.apiUrl}/auth/profile`, {
          headers: this.getHeaders()
        }).pipe(
          map(user => {
            this.currentUserSubject.next(user);
            return true;
          }),
          catchError(error => {
            console.error('Failed to load user profile:', error);
            this.logout();
            return throwError(() => error);
          })
        );
      }),
      catchError(error => {
        console.error('Login failed:', error);
        return throwError(() => error);
      })
    );
  }

  register(registerData: RegisterRequest): Observable<boolean> {
    return this.http.post<AuthResponse>(`${environment.apiUrl}/auth/register`, registerData, {
      headers: this.getHeaders()
    }).pipe(
      switchMap(response => {
        this.token = response.token;
        localStorage.setItem('token', this.token);
        
        // Load user profile and wait for it to complete
        return this.http.get<User>(`${environment.apiUrl}/auth/profile`, {
          headers: this.getHeaders()
        }).pipe(
          map(user => {
            this.currentUserSubject.next(user);
            return true;
          }),
          catchError(error => {
            console.error('Failed to load user profile:', error);
            this.logout();
            return throwError(() => error);
          })
        );
      }),
      catchError(error => {
        console.error('Registration failed:', error);
        return throwError(() => error);
      })
    );
  }

  private loadUserProfile(): void {
    if (!this.token) return;
    
    this.http.get<User>(`${environment.apiUrl}/auth/profile`, {
      headers: this.getHeaders()
    }).subscribe({
      next: (user) => {
        this.currentUserSubject.next(user);
      },
      error: (error) => {
        console.error('Failed to load user profile:', error);
        this.logout();
      }
    });
  }

  logout(): void {
    this.token = null;
    localStorage.removeItem('token');
    this.currentUserSubject.next(null);
  }

  getCurrentUser(): User | null {
    return this.currentUserSubject.value;
  }

  isAuthenticated(): boolean {
    return this.token !== null;
  }

  hasRole(role: string): boolean {
    const user = this.getCurrentUser();
    return user ? user.userType === role : false;
  }

  isAdmin(): boolean {
    return this.hasRole('ADMIN');
  }

  getToken(): string | null {
    return this.token;
  }
}
