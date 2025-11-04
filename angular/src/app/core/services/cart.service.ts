import { Injectable } from '@angular/core';
import { BehaviorSubject, Observable, forkJoin } from 'rxjs';
import { map, switchMap } from 'rxjs/operators';
import { Product } from '../../models/product.model';
import { StockService } from './stock.service';
import { ApiService, Transaction, TransactionResponse } from './api.service';
import { AuthService } from './auth.service';

export interface CartItem {
  product: Product;
  quantity: number;
}

export interface PurchaseResult {
  transaction: TransactionResponse;
  stockUpdated: boolean;
}

@Injectable({
  providedIn: 'root'
})
export class CartService {
  private cartItems = new BehaviorSubject<CartItem[]>([]);
  public cartItems$ = this.cartItems.asObservable();

  constructor(
    private stockService: StockService,
    private apiService: ApiService,
    private authService: AuthService
  ) {
    // Load cart from localStorage on initialization
    this.loadCartFromStorage();
  }

  private loadCartFromStorage(): void {
    const savedCart = localStorage.getItem('cart');
    if (savedCart) {
      try {
        const items = JSON.parse(savedCart);
        this.cartItems.next(items);
      } catch (error) {
        console.error('Error loading cart from storage:', error);
      }
    }
  }

  private saveCartToStorage(): void {
    localStorage.setItem('cart', JSON.stringify(this.cartItems.value));
  }

  getCartItems(): CartItem[] {
    return this.cartItems.value;
  }

  addToCart(product: Product, quantity: number = 1): void {
    const currentItems = this.cartItems.value;
    const existingItem = currentItems.find(item => item.product.id === product.id);

    if (existingItem) {
      // Check if new quantity doesn't exceed stock
      const newQuantity = existingItem.quantity + quantity;
      if (newQuantity > product.quantity) {
        throw new Error(`Stock insuffisant. Disponible: ${product.quantity}`);
      }
      existingItem.quantity = newQuantity;
    } else {
      // Check if quantity doesn't exceed stock
      if (quantity > product.quantity) {
        throw new Error(`Stock insuffisant. Disponible: ${product.quantity}`);
      }
      currentItems.push({ product, quantity });
    }

    this.cartItems.next([...currentItems]);
    this.saveCartToStorage();
  }

  removeFromCart(productId: number): void {
    const currentItems = this.cartItems.value.filter(
      item => item.product.id !== productId
    );
    this.cartItems.next(currentItems);
    this.saveCartToStorage();
  }

  updateQuantity(productId: number, quantity: number): void {
    const currentItems = this.cartItems.value;
    const item = currentItems.find(item => item.product.id === productId);
    
    if (item) {
      if (quantity <= 0) {
        this.removeFromCart(productId);
      } else if (quantity > item.product.quantity) {
        throw new Error(`Stock insuffisant. Disponible: ${item.product.quantity}`);
      } else {
        item.quantity = quantity;
        this.cartItems.next([...currentItems]);
        this.saveCartToStorage();
      }
    }
  }

  clearCart(): void {
    this.cartItems.next([]);
    localStorage.removeItem('cart');
  }

  getTotal(): number {
    return this.cartItems.value.reduce(
      (total, item) => total + (item.product.price * item.quantity),
      0
    );
  }

  getItemCount(): number {
    return this.cartItems.value.reduce(
      (count, item) => count + item.quantity,
      0
    );
  }

  /**
   * Process the purchase: create transaction and update stock
   */
  processPurchase(paymentMethod: string = 'CARD'): Observable<PurchaseResult> {
    const items = this.cartItems.value;
    
    if (items.length === 0) {
      throw new Error('Le panier est vide');
    }

    // Create transaction
    const transaction: Transaction = {
      amount: this.getTotal(),
      paymentMethod: paymentMethod,
      transactionType: 'PURCHASE',
      merchantName: 'SmartMediShop'
    };

    return this.apiService.createTransaction(transaction).pipe(
      switchMap((transactionResponse: TransactionResponse) => {
        // Record stock movements for each item
        const movementRequests = items.map(item => 
          this.stockService.recordMovement({
            productId: item.product.id!,
            movementType: 'OUT',
            quantity: item.quantity,
            reason: 'SALE'
          })
        );

        // Execute all movement requests
        return forkJoin(movementRequests).pipe(
          map(() => ({
            transaction: transactionResponse,
            stockUpdated: true
          }))
        );
      })
    );
  }

  /**
   * Update stock for purchase without creating a transaction
   * Used when transaction is already created (e.g., via dialog)
   */
  updateStockForPurchase(): Observable<boolean> {
    const items = this.cartItems.value;
    
    if (items.length === 0) {
      return new Observable(observer => {
        observer.next(false);
        observer.complete();
      });
    }

    // Record stock movements for each item
    const movementRequests = items.map(item => 
      this.stockService.recordMovement({
        productId: item.product.id!,
        movementType: 'OUT',
        quantity: item.quantity,
        reason: 'SALE'
      })
    );

    // Execute all movement requests
    return forkJoin(movementRequests).pipe(
      map(() => true)
    );
  }

  /**
   * Record purchases in user purchase history
   */
  recordPurchases(transactionId: number, location: string): Observable<any> {
    const items = this.cartItems.value;
    
    if (items.length === 0) {
      return new Observable(observer => {
        observer.next([]);
        observer.complete();
      });
    }

    const purchaseItems = items.map(item => ({
      productId: item.product.id!,
      quantity: item.quantity
    }));

    return this.apiService.recordPurchases({
      transactionId: transactionId,
      items: purchaseItems,
      location: location
    });
  }

  /**
   * Validate cart items against current stock
   */
  validateCart(): Observable<{ valid: boolean; errors: string[] }> {
    const items = this.cartItems.value;
    const errors: string[] = [];

    if (items.length === 0) {
      return new Observable(observer => {
        observer.next({ valid: false, errors: ['Le panier est vide'] });
        observer.complete();
      });
    }

    // Get all product IDs
    const productIds = items.map(item => item.product.id!);

    // Fetch current stock for all products
    const productRequests = productIds.map(id => 
      this.stockService.getProductById(id)
    );

    return forkJoin(productRequests).pipe(
      map((products: Product[]) => {
        items.forEach((item, index) => {
          const currentProduct = products[index];
          
          // Check if product still exists
          if (!currentProduct) {
            errors.push(`Le produit ${item.product.name} n'existe plus`);
            return;
          }

          // Check if enough stock
          if (currentProduct.quantity < item.quantity) {
            errors.push(
              `Stock insuffisant pour ${item.product.name}. ` +
              `Disponible: ${currentProduct.quantity}, Demandé: ${item.quantity}`
            );
          }

          // Check if product is expired
          if (currentProduct.expirationDate) {
            const expDate = new Date(currentProduct.expirationDate);
            const today = new Date();
            if (expDate < today) {
              errors.push(`Le produit ${item.product.name} est expiré`);
            }
          }

          // Update product info in cart
          item.product = currentProduct;
        });

        // Update cart with latest product info
        if (errors.length === 0) {
          this.cartItems.next([...items]);
          this.saveCartToStorage();
        }

        return {
          valid: errors.length === 0,
          errors
        };
      })
    );
  }
}
