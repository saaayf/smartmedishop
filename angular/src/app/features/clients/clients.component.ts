import { Component, OnInit } from '@angular/core';
import { ColumnDefinition } from '../../shared/components/data-table/data-table.component';

@Component({
  selector: 'app-clients',
  templateUrl: './clients.component.html',
  styleUrls: ['./clients.component.scss']
})
export class ClientsComponent implements OnInit {
  clients: any[] = [];
  columns: ColumnDefinition[] = [
    { key: 'id', title: 'ID', type: 'number', sortable: true, width: '80px' },
    { key: 'firstName', title: 'First Name', type: 'text', sortable: true },
    { key: 'lastName', title: 'Last Name', type: 'text', sortable: true },
    { key: 'email', title: 'Email', type: 'text', sortable: true },
    { key: 'phone', title: 'Phone', type: 'text', sortable: true },
    { key: 'dateOfBirth', title: 'Date of Birth', type: 'date', sortable: true },
    { key: 'medicalHistory', title: 'Medical History', type: 'text', sortable: false },
    { key: 'isActive', title: 'Active', type: 'boolean', sortable: true, width: '100px' }
  ];
  loading = false;

  constructor() { }

  ngOnInit() {
    this.loadClients();
  }

  loadClients() {
    this.loading = true;
    // TODO: Load from API
    setTimeout(() => {
      this.clients = [
        { id: 1, firstName: 'John', lastName: 'Doe', email: 'john.doe@email.com', phone: '+216 12 345 678', dateOfBirth: '1985-03-15', medicalHistory: 'Diabetes, Hypertension', isActive: true },
        { id: 2, firstName: 'Jane', lastName: 'Smith', email: 'jane.smith@email.com', phone: '+216 23 456 789', dateOfBirth: '1990-07-22', medicalHistory: 'Allergies', isActive: true },
        { id: 3, firstName: 'Mike', lastName: 'Johnson', email: 'mike.johnson@email.com', phone: '+216 34 567 890', dateOfBirth: '1978-11-08', medicalHistory: 'None', isActive: true },
        { id: 4, firstName: 'Sarah', lastName: 'Wilson', email: 'sarah.wilson@email.com', phone: '+216 45 678 901', dateOfBirth: '1992-05-30', medicalHistory: 'Asthma', isActive: false }
      ];
      this.loading = false;
    }, 1000);
  }

  onEdit(client: any) {
    console.log('Edit client:', client);
  }

  onDelete(client: any) {
    console.log('Delete client:', client);
  }

  onView(client: any) {
    console.log('View client:', client);
  }
}
