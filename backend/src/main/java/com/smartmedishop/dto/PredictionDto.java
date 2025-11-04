package com.smartmedishop.dto;

public class PredictionDto {
    private String productNameUsed;
    private double predictedDemand;
    private boolean isLowStock;
    private int recommendedStock;
    private Double confidence;

    public PredictionDto() {}

    public String getProductNameUsed() {
        return productNameUsed;
    }

    public void setProductNameUsed(String productNameUsed) {
        this.productNameUsed = productNameUsed;
    }

    public double getPredictedDemand() {
        return predictedDemand;
    }

    public void setPredictedDemand(double predictedDemand) {
        this.predictedDemand = predictedDemand;
    }

    public boolean isLowStock() {
        return isLowStock;
    }

    public void setLowStock(boolean lowStock) {
        isLowStock = lowStock;
    }

    public int getRecommendedStock() {
        return recommendedStock;
    }

    public void setRecommendedStock(int recommendedStock) {
        this.recommendedStock = recommendedStock;
    }

    public Double getConfidence() {
        return confidence;
    }

    public void setConfidence(Double confidence) {
        this.confidence = confidence;
    }
}
