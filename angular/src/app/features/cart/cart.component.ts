import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { CartService, CartItem } from '../../core/services/cart.service';
import { NotificationService } from '../../core/services/notification.service';

@Component({
  selector: 'app-cart',
  templateUrl: './cart.component.html',
  styleUrls: ['./cart.component.scss']
})
export class CartComponent implements OnInit {
  cartItems: CartItem[] = [];
  totalAmount = 0;
  processing = false;

  constructor(
    private cartService: CartService,
    private notificationService: NotificationService,
    private router: Router
  ) { }

  ngOnInit() {
    this.loadCartItems();
    
    // Subscribe to cart changes
    this.cartService.cartItems$.subscribe(items => {
      this.cartItems = items;
      this.calculateTotal();
    });
  }

  loadCartItems() {
    this.cartItems = this.cartService.getCartItems();
    this.calculateTotal();
  }

  updateQuantity(item: CartItem, newQuantity: number) {
    try {
      if (newQuantity <= 0) {
        this.removeItem(item);
      } else {
        this.cartService.updateQuantity(item.product.id!, newQuantity);
        this.notificationService.showSuccess('Quantité mise à jour');
      }
    } catch (error: any) {
      this.notificationService.showError(error.message || 'Erreur lors de la mise à jour');
    }
  }

  removeItem(item: CartItem) {
    this.cartService.removeFromCart(item.product.id!);
    this.notificationService.showSuccess('Produit retiré du panier');
  }

  calculateTotal() {
    this.totalAmount = this.cartService.getTotal();
  }

  proceedToCheckout() {
    if (this.cartItems.length === 0) {
      this.notificationService.showWarning('Votre panier est vide');
      return;
    }

    this.processing = true;

    // First validate the cart
    this.cartService.validateCart().subscribe({
      next: (validation) => {
        if (!validation.valid) {
          validation.errors.forEach(error => {
            this.notificationService.showError(error);
          });
          this.processing = false;
          return;
        }

        // Process the purchase
        this.cartService.processPurchase('CARD').subscribe({
          next: (result) => {
            this.notificationService.showSuccess(
              `Achat effectué avec succès! Transaction ID: ${result.transaction.transactionId}`
            );
            this.cartService.clearCart();
            this.processing = false;
            
            // Redirect to payments page to see the transaction
            this.router.navigate(['/payments']);
          },
          error: (error) => {
            console.error('Error processing purchase:', error);
            this.notificationService.showError(
              'Erreur lors du traitement de l\'achat. Veuillez réessayer.'
            );
            this.processing = false;
          }
        });
      },
      error: (error) => {
        console.error('Error validating cart:', error);
        this.notificationService.showError('Erreur lors de la validation du panier');
        this.processing = false;
      }
    });
  }

  clearCart() {
    this.cartService.clearCart();
    this.notificationService.showSuccess('Panier vidé');
  }
}
