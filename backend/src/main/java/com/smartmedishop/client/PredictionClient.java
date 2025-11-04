package com.smartmedishop.client;

import java.time.Duration;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.client.SimpleClientHttpRequestFactory;

import com.smartmedishop.dto.PredictionDto;

/**
 * Real PredictionClient: calls the local Flask API provided in the `stock prediction` folder.
 * Configurable via `flask.api.url` property.
 */
@Component
public class PredictionClient {

    private final String flaskUrl;
    private final RestTemplate rest;

    public PredictionClient(@Value("${flask.api.url:http://127.0.0.1:8000}") String flaskUrl) {
        this.flaskUrl = flaskUrl != null ? flaskUrl.replaceAll("/$", "") : "http://127.0.0.1:8000";
        SimpleClientHttpRequestFactory rf = new SimpleClientHttpRequestFactory();
        rf.setConnectTimeout((int) Duration.ofSeconds(3).toMillis());
        rf.setReadTimeout((int) Duration.ofSeconds(5).toMillis());
        this.rest = new RestTemplate(rf);
    }

    @SuppressWarnings("unchecked")
    public PredictionDto predictByFeatures(java.util.Map<String, Object> features) {
        String url = flaskUrl + "/predict_features";
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> req = new HttpEntity<>(Map.of("features", features), headers);
            Map<String, Object> resp = rest.postForObject(url, req, Map.class);
            return mapResponseToDto(resp);
        } catch (RestClientException ex) {
            // simple retry once
            try {
                Map<String, Object> resp = rest.postForObject(url, Map.of("features", features), Map.class);
                return mapResponseToDto(resp);
            } catch (Exception e) {
                return new PredictionDto();
            }
        } catch (Exception e) {
            return new PredictionDto();
        }
    }

    @SuppressWarnings("unchecked")
    public PredictionDto predictByProductName(String productName) {
        String url = flaskUrl + "/predict";
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> req = new HttpEntity<>(Map.of("product_name", productName), headers);
            Map<String, Object> resp = rest.postForObject(url, req, Map.class);
            return mapResponseToDto(resp);
        } catch (RestClientException ex) {
            try {
                Map<String, Object> resp = rest.postForObject(url, Map.of("product_name", productName), Map.class);
                return mapResponseToDto(resp);
            } catch (Exception e) {
                return new PredictionDto();
            }
        } catch (Exception e) {
            return new PredictionDto();
        }
    }

    private PredictionDto mapResponseToDto(Map<String, Object> resp) {
        PredictionDto dto = new PredictionDto();
        if (resp == null) return dto;
        try {
            Object pd = resp.get("predicted_demand");
            if (pd instanceof Number) dto.setPredictedDemand(((Number) pd).doubleValue());
            else if (pd != null) dto.setPredictedDemand(Double.parseDouble(pd.toString()));

            Object low = resp.get("is_low_stock");
            if (low instanceof Boolean) dto.setLowStock((Boolean) low);
            else if (low instanceof Number) dto.setLowStock(((Number) low).intValue() != 0);
            else if (low != null) dto.setLowStock(Boolean.parseBoolean(low.toString()));

            Object conf = resp.get("confidence");
            if (conf instanceof Number) dto.setConfidence(((Number) conf).doubleValue());
            else if (conf != null) dto.setConfidence(Double.parseDouble(conf.toString()));

            // used_sample may contain a 'name' field
            Object used = resp.get("used_sample");
            if (used instanceof Map) {
                Map<String, Object> usedMap = (Map<String, Object>) used;
                Object name = usedMap.get("name");
                if (name != null) dto.setProductNameUsed(name.toString());
            }
        } catch (Exception e) {
            // ignore partial mapping errors
        }
        return dto;
    }
}
