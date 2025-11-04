import { Component, Input, Output, EventEmitter, ViewChild, OnInit, AfterViewInit, OnChanges, SimpleChanges, OnDestroy } from '@angular/core';
import { Subscription } from 'rxjs';
import { MatTableDataSource } from '@angular/material/table';
import { MatPaginator } from '@angular/material/paginator';
import { MatSort } from '@angular/material/sort';

export interface ColumnDefinition {
  key: string;
  title: string;
  type?: 'text' | 'number' | 'date' | 'boolean' | 'action' | 'currency';
  sortable?: boolean;
  width?: string;
}

@Component({
  selector: 'app-data-table',
  templateUrl: './data-table.component.html',
  styleUrls: ['./data-table.component.scss']
})
export class DataTableComponent implements OnInit, AfterViewInit, OnChanges, OnDestroy {
  @Input() columns: ColumnDefinition[] = [];
  @Input() data: any[] = [];
  @Input() loading: boolean = false;
  @Input() pageSize: number = 10;
  @Input() pageSizeOptions: number[] = [5, 10, 25, 100];
  @Input() showActions: boolean = true;
  @Input() actionsLabel: string = 'Actions';
  
  // Server-side pagination properties
  @Input() totalItems: number = 0;
  @Input() totalPages: number = 0;
  @Input() currentPage: number = 0;
  @Input() serverSidePagination: boolean = false;

  @Output() edit = new EventEmitter<any>();
  @Output() delete = new EventEmitter<any>();
  @Output() view = new EventEmitter<any>();
  @Output() resolve = new EventEmitter<any>();
  @Output() pageChange = new EventEmitter<any>();
  @Input() showResolveButton: boolean = false;
  @Input() showEditButton: boolean = false;
  @Input() showDeleteButton: boolean = false;

  @ViewChild(MatPaginator) paginator!: MatPaginator;
  @ViewChild(MatSort) sort!: MatSort;

  dataSource = new MatTableDataSource<any>();
  private paginatorSubscription?: Subscription;

  ngOnInit() {
    this.dataSource.data = this.data;
  }

  ngAfterViewInit() {
    this.dataSource.paginator = this.paginator;
    this.dataSource.sort = this.sort;
    
    // Listen to paginator events for server-side pagination
    if (this.serverSidePagination && this.paginator) {
      this.paginatorSubscription = this.paginator.page.subscribe(event => {
        this.pageChange.emit({
          pageIndex: event.pageIndex,
          pageSize: event.pageSize,
          length: this.totalItems
        });
      });
    }
  }
  
  ngOnDestroy() {
    if (this.paginatorSubscription) {
      this.paginatorSubscription.unsubscribe();
    }
  }

  ngOnChanges(changes: SimpleChanges) {
    if (changes['data']) {
      this.dataSource.data = this.data;
    }
    
    // Update paginator length for server-side pagination
    if (this.serverSidePagination && this.paginator) {
      this.paginator.length = this.totalItems;
      if (changes['currentPage'] && this.currentPage !== undefined) {
        this.paginator.pageIndex = this.currentPage;
      }
    }
  }

  get displayedColumns(): string[] {
    const columns = this.columns.map(col => col.key);
    if (this.showActions) {
      columns.push('actions');
    }
    return columns;
  }

  onEdit(item: any) {
    this.edit.emit(item);
  }

  onDelete(item: any) {
    this.delete.emit(item);
  }

  onView(item: any) {
    this.view.emit(item);
  }

  onResolve(item: any) {
    this.resolve.emit(item);
  }

  applyFilter(event: Event) {
    const filterValue = (event.target as HTMLInputElement).value;
    this.dataSource.filter = filterValue.trim().toLowerCase();
  }

  getFraudScoreClass(score: number | undefined): string {
    if (!score) return 'none';
    if (score < 0.3) return 'low';
    if (score < 0.6) return 'medium';
    return 'high';
  }
}
