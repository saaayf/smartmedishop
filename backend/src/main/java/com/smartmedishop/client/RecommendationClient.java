package com.smartmedishop.client;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.client.SimpleClientHttpRequestFactory;

/**
 * RecommendationClient: calls the Flask Recommendation API (port 8001).
 * Configurable via `recommendation.api.url` property.
 */
@Component
public class RecommendationClient {

    private final String flaskUrl;
    private final RestTemplate rest;

    public RecommendationClient(@Value("${recommendation.api.url:http://127.0.0.1:8001}") String flaskUrl) {
        this.flaskUrl = flaskUrl != null ? flaskUrl.replaceAll("/$", "") : "http://127.0.0.1:8001";
        SimpleClientHttpRequestFactory rf = new SimpleClientHttpRequestFactory();
        rf.setConnectTimeout((int) Duration.ofSeconds(5).toMillis());
        rf.setReadTimeout((int) Duration.ofSeconds(10).toMillis());
        this.rest = new RestTemplate(rf);
    }

    /**
     * Get general recommendations based on state, type, and price
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> getRecommendations(String state, String type, Double price, Integer topN) {
        String url = flaskUrl + "/recommend";
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            Map<String, Object> requestBody = Map.of(
                "state", state != null ? state : "",
                "type", type != null ? type : "",
                "price", price != null ? price : 0.0,
                "top_n", topN != null ? topN : 10
            );
            
            HttpEntity<Map<String, Object>> req = new HttpEntity<>(requestBody, headers);
            Map<String, Object> resp = rest.postForObject(url, req, Map.class);
            return resp != null ? resp : Map.of("recommendations", List.of(), "count", 0);
        } catch (RestClientException ex) {
            // Retry once on failure
            try {
                Map<String, Object> requestBody = Map.of(
                    "state", state != null ? state : "",
                    "type", type != null ? type : "",
                    "price", price != null ? price : 0.0,
                    "top_n", topN != null ? topN : 10
                );
                Map<String, Object> resp = rest.postForObject(url, requestBody, Map.class);
                return resp != null ? resp : Map.of("recommendations", List.of(), "count", 0);
            } catch (Exception e) {
                return Map.of("recommendations", List.of(), "count", 0, "error", e.getMessage());
            }
        } catch (Exception e) {
            return Map.of("recommendations", List.of(), "count", 0, "error", e.getMessage());
        }
    }

    /**
     * Get user-specific recommendations based on username
     * @param username The username
     * @param topN Number of recommendations to return
     * @param purchaseHistory Optional purchase history from database (State, Type, Price, StockId)
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> getUserRecommendations(String username, Integer topN, List<Map<String, Object>> purchaseHistory) {
        String url = flaskUrl + "/recommend_user";
        try {
            System.out.println("DEBUG RecommendationClient: Calling Flask API at: " + url);
            System.out.println("DEBUG RecommendationClient: Username: " + username);
            System.out.println("DEBUG RecommendationClient: Purchase history size: " + (purchaseHistory != null ? purchaseHistory.size() : 0));
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            Map<String, Object> requestBody = new java.util.HashMap<>();
            requestBody.put("username", username != null ? username : "");
            requestBody.put("top_n", topN != null ? topN : 10);
            if (purchaseHistory != null && !purchaseHistory.isEmpty()) {
                requestBody.put("purchase_history", purchaseHistory);
                System.out.println("DEBUG RecommendationClient: Added purchase_history to request body");
                System.out.println("DEBUG RecommendationClient: First purchase: " + purchaseHistory.get(0));
            } else {
                System.out.println("DEBUG RecommendationClient: purchase_history is null or empty, not adding to request");
            }
            
            System.out.println("DEBUG RecommendationClient: Request body keys: " + requestBody.keySet());
            System.out.println("DEBUG RecommendationClient: Request body: " + requestBody);
            
            HttpEntity<Map<String, Object>> req = new HttpEntity<>(requestBody, headers);
            Map<String, Object> resp = rest.postForObject(url, req, Map.class);
            System.out.println("DEBUG RecommendationClient: Flask API response: " + resp);
            return resp != null ? resp : Map.of("recommendations", List.of(), "count", 0);
        } catch (RestClientException ex) {
            // Retry once on failure
            try {
                Map<String, Object> requestBody = new java.util.HashMap<>();
                requestBody.put("username", username != null ? username : "");
                requestBody.put("top_n", topN != null ? topN : 10);
                if (purchaseHistory != null && !purchaseHistory.isEmpty()) {
                    requestBody.put("purchase_history", purchaseHistory);
                }
                Map<String, Object> resp = rest.postForObject(url, requestBody, Map.class);
                return resp != null ? resp : Map.of("recommendations", List.of(), "count", 0);
            } catch (Exception e) {
                return Map.of("recommendations", List.of(), "count", 0, "error", e.getMessage());
            }
        } catch (Exception e) {
            return Map.of("recommendations", List.of(), "count", 0, "error", e.getMessage());
        }
    }

    /**
     * Health check for the recommendation API
     */
    public Map<String, Object> healthCheck() {
        String url = flaskUrl + "/health";
        try {
            Map<String, Object> resp = rest.getForObject(url, Map.class);
            return resp != null ? resp : Map.of("status", "unknown", "models_loaded", false);
        } catch (Exception e) {
            return Map.of("status", "error", "models_loaded", false, "error", e.getMessage());
        }
    }
}

