export interface PredictionDto {
  productNameUsed?: string;
  predictedDemand?: number;
  recommendedStock?: number;
  confidence?: number | null;
  lowStock?: boolean;
}
