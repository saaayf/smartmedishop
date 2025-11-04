export interface Product {
  id?: number;
  sku: string;
  name: string;
  description: string;
  quantity: number;
  lowStockThreshold: number;
  price: number;
  expirationDate: string;
  createdAt?: string;
}
