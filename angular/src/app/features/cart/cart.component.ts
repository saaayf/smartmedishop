import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { MatDialog } from '@angular/material/dialog';
import { CartService, CartItem } from '../../core/services/cart.service';
import { NotificationService } from '../../core/services/notification.service';
import { CreateTransactionDialogComponent } from '../payments/create-transaction-dialog.component';

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
    private router: Router,
    private dialog: MatDialog
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

    // Calculate total with VAT (19%)
    const totalWithVAT = this.totalAmount * 1.19;

    // Open transaction dialog with cart total pre-filled
    const dialogRef = this.dialog.open(CreateTransactionDialogComponent, {
      width: '700px',
      maxWidth: '90vw',
      disableClose: true,
      data: { amount: totalWithVAT }
    });

    dialogRef.afterClosed().subscribe((result) => {
      if (result && result.success) {
        // Transaction created successfully, now update stock and process cart
        this.processCartAfterTransaction(result.transaction, result.locationCountry);
      }
    });
  }

  private processCartAfterTransaction(transaction: any, locationCountry?: string) {
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

        // Update stock without creating another transaction
        this.cartService.updateStockForPurchase().subscribe({
          next: (stockUpdated) => {
            if (stockUpdated) {
              // Record purchases in user purchase history
              const transactionId = transaction.transactionId || transaction.id;
              const location = locationCountry || transaction.locationCountry || 'Unknown';
              
              this.cartService.recordPurchases(transactionId, location).subscribe({
                next: () => {
                  this.notificationService.showSuccess(
                    `Achat effectué avec succès! Transaction ID: ${transactionId}`
                  );
                  this.cartService.clearCart();
                  this.processing = false;
                  
                  // Redirect to payments page to see the transaction
                  this.router.navigate(['/payments']);
                },
                error: (error) => {
                  console.error('Error recording purchases:', error);
                  // Still show success even if purchase history recording fails
                  this.notificationService.showSuccess(
                    `Achat effectué avec succès! Transaction ID: ${transactionId}`
                  );
                  this.cartService.clearCart();
                  this.processing = false;
                  this.router.navigate(['/payments']);
                }
              });
            } else {
              this.notificationService.showError('Erreur lors de la mise à jour du stock');
              this.processing = false;
            }
          },
          error: (error) => {
            console.error('Error updating stock:', error);
            this.notificationService.showError(
              'Erreur lors de la mise à jour du stock. Veuillez réessayer.'
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
