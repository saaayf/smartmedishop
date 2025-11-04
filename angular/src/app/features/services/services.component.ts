import { Component, OnInit } from '@angular/core';
import { ColumnDefinition } from '../../shared/components/data-table/data-table.component';

@Component({
  selector: 'app-services',
  templateUrl: './services.component.html',
  styleUrls: ['./services.component.scss']
})
export class ServicesComponent implements OnInit {
  services: any[] = [];
  columns: ColumnDefinition[] = [
    { key: 'id', title: 'ID', type: 'number', sortable: true, width: '80px' },
    { key: 'name', title: 'Service Name', type: 'text', sortable: true },
    { key: 'description', title: 'Description', type: 'text', sortable: false },
    { key: 'price', title: 'Price', type: 'number', sortable: true },
    { key: 'duration', title: 'Duration (min)', type: 'number', sortable: true },
    { key: 'category', title: 'Category', type: 'text', sortable: true },
    { key: 'isAvailable', title: 'Available', type: 'boolean', sortable: true, width: '100px' }
  ];
  loading = false;

  constructor() { }

  ngOnInit() {
    this.loadServices();
  }

  loadServices() {
    this.loading = true;
    // TODO: Load from API
    setTimeout(() => {
      this.services = [
        { id: 1, name: 'Blood Pressure Check', description: 'Regular blood pressure monitoring', price: 25, duration: 15, category: 'Vital Signs', isAvailable: true },
        { id: 2, name: 'Vaccination', description: 'COVID-19 vaccination service', price: 50, duration: 30, category: 'Immunization', isAvailable: true },
        { id: 3, name: 'Wound Dressing', description: 'Professional wound care and dressing', price: 35, duration: 20, category: 'Wound Care', isAvailable: true },
        { id: 4, name: 'Health Consultation', description: 'General health consultation', price: 60, duration: 45, category: 'Consultation', isAvailable: false }
      ];
      this.loading = false;
    }, 1000);
  }

  onEdit(service: any) {
    console.log('Edit service:', service);
  }

  onDelete(service: any) {
    console.log('Delete service:', service);
  }

  onView(service: any) {
    console.log('View service:', service);
  }
}
