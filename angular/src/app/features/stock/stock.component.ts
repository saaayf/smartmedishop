import { Component, OnInit } from '@angular/core';
import { ColumnDefinition } from '../../shared/components/data-table/data-table.component';

@Component({
  selector: 'app-stock',
  templateUrl: './stock.component.html',
  styleUrls: ['./stock.component.scss']
})
export class StockComponent implements OnInit {
  stockItems: any[] = [];
  columns: ColumnDefinition[] = [
    { key: 'id', title: 'ID', type: 'number', sortable: true, width: '80px' },
    { key: 'name', title: 'Product Name', type: 'text', sortable: true },
    { key: 'category', title: 'Category', type: 'text', sortable: true },
    { key: 'quantity', title: 'Quantity', type: 'number', sortable: true },
    { key: 'unitPrice', title: 'Unit Price', type: 'number', sortable: true },
    { key: 'expiryDate', title: 'Expiry Date', type: 'date', sortable: true },
    { key: 'supplier', title: 'Supplier', type: 'text', sortable: true },
    { key: 'isLowStock', title: 'Low Stock', type: 'boolean', sortable: true, width: '100px' }
  ];
  loading = false;

  constructor() { }

  ngOnInit() {
    this.loadStockItems();
  }

  loadStockItems() {
    this.loading = true;
    // TODO: Load from API
    setTimeout(() => {
      this.stockItems = [
        { id: 1, name: 'Aspirin 500mg', category: 'Medication', quantity: 150, unitPrice: 15.50, expiryDate: '2025-12-31', supplier: 'PharmaCorp', isLowStock: false },
        { id: 2, name: 'Bandages', category: 'Medical Supplies', quantity: 5, unitPrice: 8.00, expiryDate: '2026-06-30', supplier: 'MedSupply', isLowStock: true },
        { id: 3, name: 'Thermometer', category: 'Equipment', quantity: 12, unitPrice: 25.00, expiryDate: '2027-01-15', supplier: 'TechMed', isLowStock: false },
        { id: 4, name: 'Insulin', category: 'Medication', quantity: 2, unitPrice: 45.00, expiryDate: '2024-08-20', supplier: 'PharmaCorp', isLowStock: true }
      ];
      this.loading = false;
    }, 1000);
  }

  onEdit(item: any) {
    console.log('Edit stock item:', item);
  }

  onDelete(item: any) {
    console.log('Delete stock item:', item);
  }

  onView(item: any) {
    console.log('View stock item:', item);
  }
}
