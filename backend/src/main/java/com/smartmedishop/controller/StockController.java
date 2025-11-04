package com.smartmedishop.controller;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.smartmedishop.dto.ProductDto;
import com.smartmedishop.dto.StockMovementDto;
import com.smartmedishop.entity.Product;
import com.smartmedishop.entity.StockAlert;
import com.smartmedishop.entity.StockMovement;
import com.smartmedishop.service.AlertMigrationService;
import com.smartmedishop.service.StockService;

@RestController
@RequestMapping("/api/stock")
@CrossOrigin(origins = "*")
public class StockController {

    @Autowired
    private StockService stockService;

    @Autowired
    private AlertMigrationService alertMigrationService;

    @Autowired
    private com.smartmedishop.service.PredictionService predictionService;

    // Create product (ADMIN)
    @PostMapping("/products")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> createProduct(@RequestBody Product p) {
        Product created = stockService.createProduct(p);
        return ResponseEntity.ok(new ProductDto(created));
    }

    @GetMapping("/products")
    public ResponseEntity<?> listProducts(@RequestParam(required = false) String search) {
        List<Product> all = stockService.findAll();
        List<ProductDto> dtos = all.stream().map(ProductDto::new).collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    @GetMapping("/products/{id}")
    public ResponseEntity<?> getProduct(@PathVariable Long id) {
        var p = stockService.findById(id);
        if (p.isEmpty()) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(new ProductDto(p.get()));
    }

    @PutMapping("/products/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> updateProduct(@PathVariable Long id, @RequestBody Product body) {
        var pOpt = stockService.findById(id);
        if (pOpt.isEmpty()) return ResponseEntity.notFound().build();
        Product p = pOpt.get();
        // update fields (allow partial)
        if (body.getName() != null) p.setName(body.getName());
        if (body.getDescription() != null) p.setDescription(body.getDescription());
        if (body.getQuantity() != null) p.setQuantity(body.getQuantity());
        if (body.getLowStockThreshold() != null) p.setLowStockThreshold(body.getLowStockThreshold());
        if (body.getPrice() != null) p.setPrice(body.getPrice());
        if (body.getExpirationDate() != null) p.setExpirationDate(body.getExpirationDate());
        Product updated = stockService.updateProduct(p);
        return ResponseEntity.ok(new ProductDto(updated));
    }

    @PostMapping("/movements")
    @PreAuthorize("hasRole('ADMIN') or isAuthenticated()")
    public ResponseEntity<?> createMovement(@RequestBody Map<String, Object> req) {
        try {
            Long productId = req.get("productId") != null ? Long.parseLong(req.get("productId").toString()) : null;
            String type = req.get("movementType") != null ? req.get("movementType").toString() : "IN";
            Integer qty = req.get("quantity") != null ? Integer.parseInt(req.get("quantity").toString()) : 0;
            String reason = req.get("reason") != null ? req.get("reason").toString() : "MANUAL";

            var pOpt = stockService.findById(productId);
            if (pOpt.isEmpty()) return ResponseEntity.badRequest().body(Map.of("error", "product not found"));
            StockMovement.MovementType mt = StockMovement.MovementType.valueOf(type.toUpperCase());
            StockMovement m = stockService.recordMovement(pOpt.get(), mt, qty, reason);
            return ResponseEntity.ok(new StockMovementDto(m));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/movements/product/{productId}")
    public ResponseEntity<?> getMovements(@PathVariable Long productId) {
        List<StockMovement> list = stockService.getMovementsForProduct(productId);
        List<StockMovementDto> dto = list.stream().map(StockMovementDto::new).collect(Collectors.toList());
        return ResponseEntity.ok(dto);
    }

    @GetMapping("/alerts/product/{productId}")
    public ResponseEntity<?> getAlerts(@PathVariable Long productId) {
        List<StockAlert> alerts = stockService.getAlertsForProduct(productId);
        return ResponseEntity.ok(alerts);
    }

    @GetMapping("/products/{id}/predict")
    public ResponseEntity<?> predictForProduct(@PathVariable Long id) {
        var pOpt = stockService.findById(id);
        if (pOpt.isEmpty()) return ResponseEntity.notFound().build();
        var p = pOpt.get();
        var pred = predictionService.predictForProduct(p);
        return ResponseEntity.ok(pred);
    }

    /**
     * Endpoint pour générer les alertes manquantes sur tous les produits existants
     * Parcourt tous les produits et crée les alertes LOW_STOCK et EXPIRED si nécessaire
     * ADMIN uniquement
     */
    @GetMapping("/alerts/generate-missing")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> generateMissingAlerts() {
        try {
            Map<String, Object> result = alertMigrationService.generateMissingAlerts();
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
