export interface UserPurchase {
  id?: number;
  userId: number;
  transactionId: number;
  stockId: number;
  name: string;
  marque?: string;
  type?: string;
  state?: string; // State stores the location
  price: number;
  quantity?: number;
  purchaseDate?: string;
}

