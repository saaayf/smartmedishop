export interface StockMovement {
  id?: number;
  productId: number;
  movementType: 'IN' | 'OUT';
  quantity: number;
  reason: string;
  createdAt?: string;
}
