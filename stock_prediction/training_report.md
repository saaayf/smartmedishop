# Training report
Generated on 2025-11-04T16:09:38.433666

- Dataset initial: 550 lignes
- Dataset après nettoyage: 523 lignes

## Meilleurs modèles
- Regression: RandomForest (CV RMSE estimé: 27.8523)
- Classification: GradientBoosting (CV F1 estimé: 0.9988)

## Métriques sur le jeu test
```json
{
  "regression": {
    "mse": 1004.4253733333335,
    "rmse": 31.692670656373114
  },
  "classification": {
    "accuracy": 0.9809523809523809,
    "precision": 1.0,
    "recall": 0.5,
    "f1": 0.6666666666666666
  }
}
```

## Exemple de prédiction
```json
{
  "product_name_used": "Aspirine 802mg",
  "predicted_demand": 69.7,
  "is_low_stock": false,
  "recommended_stock": 0,
  "confidence_score": 0.9999780982686992
}
```