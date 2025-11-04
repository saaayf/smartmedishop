import { Component, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { StockService } from '../../../core/services/stock.service';
import { PredictionDto } from '../../../models/prediction.model';

@Component({
  selector: 'app-predict',
  templateUrl: './predict.component.html',
  styleUrls: ['./predict.component.scss']
})
export class PredictComponent implements OnInit {
  productId!: number;
  loading = false;
  error: string | null = null;
  prediction: PredictionDto | null = null;

  constructor(
    private route: ActivatedRoute,
    private stockService: StockService
  ) { }

  ngOnInit(): void {
    this.route.paramMap.subscribe(pm => {
      const id = pm.get('id');
      if (id) {
        this.productId = +id;
        this.loadPrediction();
      }
    });
  }

  loadPrediction() {
    this.loading = true;
    this.error = null;
    this.prediction = null;
    this.stockService.getPredictionByProduct(this.productId).subscribe({
      next: (res) => {
        this.prediction = res as PredictionDto;
        this.loading = false;
      },
      error: (err) => {
        this.error = err?.error?.message || 'Erreur lors de la récupération de la prédiction';
        this.loading = false;
      }
    });
  }

  confidenceBadgeColor(): 'primary' | 'accent' | 'warn' {
    const c = this.prediction?.confidence ?? null;
    if (c == null) return 'warn';
    if (c >= 0.75) return 'primary';
    if (c >= 0.4) return 'accent';
    return 'warn';
  }
}
