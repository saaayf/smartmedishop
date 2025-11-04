export interface StockAlert {
  id?: number;
  productId: number;
  alertType: 'LOW_STOCK' | 'EXPIRED';
  message: string;
  status: 'ACTIVE' | 'RESOLVED';
  createdAt?: string;
}
