import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { Product } from '../../models/product.model';

export interface Transaction {
  id?: number;
  amount: number;
  paymentMethod: string;
  deviceType?: string;
  ipAddress?: string;
  locationCountry?: string;
  merchantName?: string;
  transactionType?: string;
  status?: string;
  fraudScore?: number;
  riskLevel?: string;
  isFraud?: boolean;
  fraudReasons?: string;
  transactionDate?: string;
  createdAt?: string;
}

export interface TransactionResponse {
  transactionId: number;
  amount: number;
  status: string;
  fraudScore: number;
  riskLevel: string;
  isFraud: boolean;
  fraudReasons?: string;
  transactionDate: string;
}

export interface FraudAlert {
  id: number;
  transactionId: number;
  userId?: number;
  username?: string;
  amount?: number;
  alertType: string;
  severity: string;
  description: string;
  status: string;
  fraudScore?: number;
  riskFactors?: string;
  investigationNotes?: string;
  resolvedBy?: string;
  createdAt: string;
  resolvedAt?: string;
}

export interface User {
  id: number;
  username: string;
  email: string;
  firstName: string;
  lastName: string;
  userType: string;
  isActive: boolean;
  isVerified: boolean;
  riskProfile: string;
  fraudCount: number;
  totalTransactions: number;
  averageAmount: number;
  registrationDate: string;
  lastLogin: string;
}

@Injectable({
  providedIn: 'root'
})
export class ApiService {
  private baseUrl = environment.apiUrl;

  constructor(private http: HttpClient) { }

  private getHeaders(): HttpHeaders {
    const token = localStorage.getItem('token');
    let headers = new HttpHeaders({
      'Content-Type': 'application/json'
    });
    
    if (token) {
      headers = headers.set('Authorization', `Bearer ${token}`);
    }
    
    return headers;
  }

  // Generic CRUD operations
  get<T>(endpoint: string): Observable<T> {
    return this.http.get<T>(`${this.baseUrl}${endpoint}`, { headers: this.getHeaders() });
  }

  post<T>(endpoint: string, data: any): Observable<T> {
    return this.http.post<T>(`${this.baseUrl}${endpoint}`, data, { headers: this.getHeaders() });
  }

  put<T>(endpoint: string, data: any): Observable<T> {
    return this.http.put<T>(`${this.baseUrl}${endpoint}`, data, { headers: this.getHeaders() });
  }

  delete<T>(endpoint: string): Observable<T> {
    return this.http.delete<T>(`${this.baseUrl}${endpoint}`, { headers: this.getHeaders() });
  }

  // Authentication endpoints
  login(username: string, password: string): Observable<any> {
    return this.post('/auth/login', { username, password });
  }

  register(userData: any): Observable<any> {
    return this.post('/auth/register', userData);
  }

  getProfile(): Observable<any> {
    return this.get('/auth/profile');
  }

  // Transaction endpoints
  createTransaction(transaction: Transaction): Observable<TransactionResponse> {
    return this.post('/transactions', transaction);
  }

  getMyTransactions(): Observable<Transaction[]> {
    return this.get('/transactions/my-transactions');
  }

  getTransaction(id: number): Observable<Transaction> {
    return this.get(`/transactions/${id}`);
  }

  updateTransaction(id: number, transaction: Partial<Transaction>): Observable<any> {
    return this.put(`/transactions/${id}`, transaction);
  }

  deleteTransaction(id: number): Observable<any> {
    return this.delete(`/transactions/${id}`);
  }

  processTransaction(id: number): Observable<any> {
    return this.post(`/transactions/${id}/process`, {});
  }

  getTransactionStatistics(): Observable<any> {
    return this.get('/transactions/statistics');
  }

  // FRAUD_ANALYST endpoints - All transactions
  getAllTransactions(params?: any): Observable<any> {
    let url = '/transactions/all';
    if (params) {
      const queryParams = new URLSearchParams();
      Object.keys(params).forEach(key => {
        if (params[key] !== null && params[key] !== undefined) {
          queryParams.append(key, params[key]);
        }
      });
      url += '?' + queryParams.toString();
    }
    return this.get(url);
  }

  getTransactionsByUserId(userId: number): Observable<Transaction[]> {
    return this.get(`/transactions/user/${userId}`);
  }

  getAllTransactionStatistics(): Observable<any> {
    return this.get('/transactions/statistics/all');
  }

  // User Behavior endpoints
  getUserBehavior(userId: number): Observable<any> {
    return this.get(`/user-behavior/${userId}`);
  }

  getAllUserBehaviors(): Observable<any> {
    return this.get('/user-behavior/all');
  }

  // Fraud detection endpoints
  getFraudAlerts(): Observable<FraudAlert[]> {
    return this.get('/fraud/alerts');
  }

  getFraudAlert(id: number): Observable<FraudAlert> {
    return this.get(`/fraud/alerts/${id}`);
  }

  updateFraudAlert(id: number, status: string): Observable<any> {
    return this.put(`/fraud/alerts/${id}`, { status });
  }

  resolveFraudAlert(id: number, investigationNotes?: string): Observable<any> {
    return this.put(`/fraud/alerts/${id}/resolve`, { investigationNotes: investigationNotes || '' });
  }

  getFraudStatistics(): Observable<any> {
    return this.get('/fraud/statistics');
  }

  // Health check
  getHealth(): Observable<any> {
    return this.get('/health');
  }

  // Legacy endpoints (for existing template)
  getProducts(): Observable<any[]> {
    return this.get<any[]>('/products');
  }

  // User Management
  getUsers(params?: any): Observable<any> {
    let url = '/users';
    if (params) {
      const queryParams = new URLSearchParams();
      Object.keys(params).forEach(key => {
        if (params[key] !== null && params[key] !== undefined) {
          queryParams.append(key, params[key]);
        }
      });
      url += '?' + queryParams.toString();
    }
    return this.get(url);
  }

  getUser(id: number): Observable<User> {
    return this.get<User>(`/users/${id}`);
  }

  createUser(user: any): Observable<User> {
    return this.post<User>('/users', user);
  }

  updateUser(id: number, user: any): Observable<User> {
    return this.put<User>(`/users/${id}`, user);
  }

  deleteUser(id: number): Observable<any> {
    return this.delete(`/users/${id}`);
  }

  activateUser(id: number): Observable<any> {
    return this.put(`/users/${id}/activate`, {});
  }

  deactivateUser(id: number): Observable<any> {
    return this.put(`/users/${id}/deactivate`, {});
  }

  getUserStatistics(): Observable<any> {
    return this.get('/users/statistics');
  }

  getServices(): Observable<any[]> {
    return this.get<any[]>('/services');
  }

  getPayments(): Observable<any[]> {
    return this.get<any[]>('/payments');
  }

  getClients(): Observable<any[]> {
    return this.get<any[]>('/clients');
  }

  getStock(): Observable<any[]> {
    return this.get<any[]>('/stock');
  }

  // User Purchase endpoints
  recordPurchases(data: { transactionId: number; items: any[]; location?: string }): Observable<any> {
    return this.post('/purchases/record', data);
  }

  getMyPurchases(): Observable<any[]> {
    return this.get('/purchases/my-purchases');
  }

  getUserPurchases(userId: number): Observable<any[]> {
    return this.get(`/purchases/user/${userId}`);
  }

  getTransactionPurchases(transactionId: number): Observable<any[]> {
    return this.get(`/purchases/transaction/${transactionId}`);
  }

  // Recommendation endpoints
  getRecommendations(params?: { state?: string; type?: string; price?: number; top_n?: number }): Observable<{ recommendations: Product[]; count: number }> {
    let url = '/recommendations';
    if (params) {
      const queryParams = new URLSearchParams();
      if (params.state) queryParams.append('state', params.state);
      if (params.type) queryParams.append('type', params.type);
      if (params.price !== undefined) queryParams.append('price', params.price.toString());
      if (params.top_n !== undefined) queryParams.append('top_n', params.top_n.toString());
      const queryString = queryParams.toString();
      if (queryString) url += '?' + queryString;
    }
    return this.get<{ recommendations: Product[]; count: number }>(url);
  }

  getUserRecommendations(top_n?: number): Observable<{ username: string; recommendations: Product[]; count: number }> {
    let url = '/recommendations/user';
    if (top_n !== undefined) {
      url += `?top_n=${top_n}`;
    }
    return this.get<{ username: string; recommendations: Product[]; count: number }>(url);
  }
}
