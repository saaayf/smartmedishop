import { Component, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { Product } from '../../../models/product.model';
import { StockMovement } from '../../../models/stock-movement.model';
import { StockService } from '../../../core/services/stock.service';
import { NotificationService } from '../../../core/services/notification.service';
import { Observable } from 'rxjs';
import { map, startWith } from 'rxjs/operators';

@Component({
  selector: 'app-movement-form',
  templateUrl: './movement-form.component.html',
  styleUrls: ['./movement-form.component.scss']
})
export class MovementFormComponent implements OnInit {
  movementForm!: FormGroup;
  products: Product[] = [];
  filteredProducts!: Observable<Product[]>;
  loading: boolean = false;
  saving: boolean = false;

  movementTypes = [
    { value: 'IN', label: 'Entrée', icon: 'add_circle' },
    { value: 'OUT', label: 'Sortie', icon: 'remove_circle' }
  ];

  reasons = [
    { value: 'PURCHASE', label: 'Achat' },
    { value: 'SALE', label: 'Vente' },
    { value: 'RETURN', label: 'Retour' },
    { value: 'MANUAL', label: 'Ajustement manuel' },
    { value: 'EXPIRED', label: 'Produit expiré' },
    { value: 'DAMAGED', label: 'Produit endommagé' }
  ];

  constructor(
    private fb: FormBuilder,
    private router: Router,
    private stockService: StockService,
    private notificationService: NotificationService
  ) {}

  ngOnInit(): void {
    this.initForm();
    this.loadProducts();
  }

  initForm(): void {
    this.movementForm = this.fb.group({
      productId: ['', Validators.required],
      movementType: ['IN', Validators.required],
      quantity: [1, [Validators.required, Validators.min(1)]],
      reason: ['', Validators.required]
    });

    // Setup autocomplete filtering
    this.filteredProducts = this.movementForm.get('productId')!.valueChanges.pipe(
      startWith(''),
      map(value => this._filterProducts(value))
    );
  }

  loadProducts(): void {
    this.loading = true;
    this.stockService.getAllProducts().subscribe({
      next: (products) => {
        this.products = products;
        this.loading = false;
      },
      error: (error) => {
        console.error('Error loading products:', error);
        this.notificationService.showError('Erreur lors du chargement des produits');
        this.loading = false;
      }
    });
  }

  private _filterProducts(value: any): Product[] {
    if (typeof value !== 'string') {
      return this.products;
    }
    
    const filterValue = value.toLowerCase();
    return this.products.filter(product =>
      product.name.toLowerCase().includes(filterValue) ||
      product.sku.toLowerCase().includes(filterValue)
    );
  }

  displayProduct(productId: number): string {
    if (!productId) return '';
    const product = this.products.find(p => p.id === productId);
    return product ? `${product.sku} - ${product.name}` : '';
  }

  onSubmit(): void {
    if (this.movementForm.invalid) {
      Object.keys(this.movementForm.controls).forEach(key => {
        this.movementForm.get(key)?.markAsTouched();
      });
      return;
    }

    this.saving = true;
    const movement: StockMovement = this.movementForm.value;

    this.stockService.recordMovement(movement).subscribe({
      next: () => {
        this.notificationService.showSuccess('Mouvement enregistré avec succès');
        this.movementForm.reset({
          productId: '',
          movementType: 'IN',
          quantity: 1,
          reason: ''
        });
        this.saving = false;
      },
      error: (error) => {
        console.error('Error recording movement:', error);
        this.notificationService.showError('Erreur lors de l\'enregistrement du mouvement');
        this.saving = false;
      }
    });
  }

  cancel(): void {
    this.router.navigate(['/stock/products']);
  }

  getErrorMessage(fieldName: string): string {
    const control = this.movementForm.get(fieldName);
    if (!control) return '';

    if (control.hasError('required')) {
      return 'Ce champ est requis';
    }
    if (control.hasError('min')) {
      const min = control.errors?.['min'].min;
      return `La valeur minimale est ${min}`;
    }
    return '';
  }
}
