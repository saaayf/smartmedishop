import { Component, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { Product } from '../../../models/product.model';
import { StockService } from '../../../core/services/stock.service';
import { NotificationService } from '../../../core/services/notification.service';

@Component({
  selector: 'app-product-form',
  templateUrl: './product-form.component.html',
  styleUrls: ['./product-form.component.scss']
})
export class ProductFormComponent implements OnInit {
  productForm!: FormGroup;
  isEditMode: boolean = false;
  productId: number | null = null;
  loading: boolean = false;
  saving: boolean = false;

  constructor(
    private fb: FormBuilder,
    private route: ActivatedRoute,
    private router: Router,
    private stockService: StockService,
    private notificationService: NotificationService
  ) {}

  ngOnInit(): void {
    this.initForm();
    
    const id = this.route.snapshot.paramMap.get('id');
    if (id && id !== 'new') {
      this.isEditMode = true;
      this.productId = +id;
      this.loadProduct(this.productId);
    }
  }

  initForm(): void {
    this.productForm = this.fb.group({
      sku: ['', [Validators.required, Validators.minLength(3)]],
      name: ['', [Validators.required, Validators.minLength(3)]],
      description: ['', Validators.required],
      quantity: [0, [Validators.required, Validators.min(0)]],
      lowStockThreshold: [0, [Validators.required, Validators.min(0)]],
      price: [0, [Validators.required, Validators.min(0.01)]],
      expirationDate: ['', Validators.required]
    });
  }

  loadProduct(id: number): void {
    this.loading = true;
    this.stockService.getProductById(id).subscribe({
      next: (product) => {
        this.productForm.patchValue({
          sku: product.sku,
          name: product.name,
          description: product.description,
          quantity: product.quantity,
          lowStockThreshold: product.lowStockThreshold,
          price: product.price,
          expirationDate: product.expirationDate
        });
        // Disable SKU field in edit mode
        this.productForm.get('sku')?.disable();
        this.loading = false;
      },
      error: (error) => {
        console.error('Error loading product:', error);
        this.notificationService.showError('Erreur lors du chargement du produit');
        this.loading = false;
        this.router.navigate(['/stock/products']);
      }
    });
  }

  onSubmit(): void {
    if (this.productForm.invalid) {
      Object.keys(this.productForm.controls).forEach(key => {
        this.productForm.get(key)?.markAsTouched();
      });
      return;
    }

    this.saving = true;
    const formValue = this.productForm.getRawValue(); // Use getRawValue to include disabled fields
    
    if (this.isEditMode && this.productId) {
      // Update product
      const updateData: Partial<Product> = {
        name: formValue.name,
        description: formValue.description,
        quantity: formValue.quantity,
        lowStockThreshold: formValue.lowStockThreshold,
        price: formValue.price,
        expirationDate: formValue.expirationDate
      };
      
      this.stockService.updateProduct(this.productId, updateData).subscribe({
        next: () => {
          this.notificationService.showSuccess('Produit modifié avec succès');
          this.router.navigate(['/stock/products', this.productId]);
          this.saving = false;
        },
        error: (error) => {
          console.error('Error updating product:', error);
          this.notificationService.showError('Erreur lors de la modification du produit');
          this.saving = false;
        }
      });
    } else {
      // Create product
      const product: Product = formValue;
      
      this.stockService.createProduct(product).subscribe({
        next: (createdProduct) => {
          this.notificationService.showSuccess('Produit créé avec succès');
          this.router.navigate(['/stock/products', createdProduct.id]);
          this.saving = false;
        },
        error: (error) => {
          console.error('Error creating product:', error);
          this.notificationService.showError('Erreur lors de la création du produit');
          this.saving = false;
        }
      });
    }
  }

  cancel(): void {
    if (this.isEditMode && this.productId) {
      this.router.navigate(['/stock/products', this.productId]);
    } else {
      this.router.navigate(['/stock/products']);
    }
  }

  getErrorMessage(fieldName: string): string {
    const control = this.productForm.get(fieldName);
    if (!control) return '';

    if (control.hasError('required')) {
      return 'Ce champ est requis';
    }
    if (control.hasError('min')) {
      const min = control.errors?.['min'].min;
      return `La valeur minimale est ${min}`;
    }
    if (control.hasError('minlength')) {
      const minLength = control.errors?.['minlength'].requiredLength;
      return `Minimum ${minLength} caractères requis`;
    }
    return '';
  }
}
