import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { Product } from '../../models/product.model';
import { StockMovement } from '../../models/stock-movement.model';
import { StockAlert } from '../../models/stock-alert.model';
import { AuthService } from './auth.service';

@Injectable({
  providedIn: 'root'
})
export class StockService {
  private baseUrl = `${environment.apiUrl}/stock`;

  constructor(
    private http: HttpClient,
    private authService: AuthService
  ) {}

  private getHeaders(): HttpHeaders {
    let headers = new HttpHeaders({
      'Content-Type': 'application/json'
    });
    
    const token = this.authService.getToken();
    if (token) {
      headers = headers.set('Authorization', `Bearer ${token}`);
    }
    
    return headers;
  }

  // Product methods
  getAllProducts(): Observable<Product[]> {
    return this.http.get<Product[]>(`${this.baseUrl}/products`, {
      headers: this.getHeaders()
    });
  }

  getProductById(id: number): Observable<Product> {
    return this.http.get<Product>(`${this.baseUrl}/products/${id}`, {
      headers: this.getHeaders()
    });
  }

  createProduct(product: Product): Observable<Product> {
    return this.http.post<Product>(`${this.baseUrl}/products`, product, {
      headers: this.getHeaders()
    });
  }

  updateProduct(id: number, product: Partial<Product>): Observable<Product> {
    return this.http.put<Product>(`${this.baseUrl}/products/${id}`, product, {
      headers: this.getHeaders()
    });
  }

  // Movement methods
  recordMovement(movement: StockMovement): Observable<StockMovement> {
    return this.http.post<StockMovement>(`${this.baseUrl}/movements`, movement, {
      headers: this.getHeaders()
    });
  }

  getMovementsByProduct(productId: number): Observable<StockMovement[]> {
    return this.http.get<StockMovement[]>(`${this.baseUrl}/movements/product/${productId}`, {
      headers: this.getHeaders()
    });
  }

  // Alert methods
  getAlertsByProduct(productId: number): Observable<StockAlert[]> {
    return this.http.get<StockAlert[]>(`${this.baseUrl}/alerts/product/${productId}`, {
      headers: this.getHeaders()
    });
  }

  // Prediction method
  getPredictionByProduct(productId: number): Observable<any> {
    return this.http.get<any>(`${this.baseUrl}/products/${productId}/predict`, {
      headers: this.getHeaders()
    });
  }
}
