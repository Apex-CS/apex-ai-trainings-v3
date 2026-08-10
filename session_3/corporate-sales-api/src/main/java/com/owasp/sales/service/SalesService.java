package com.owasp.sales.service;

import com.owasp.sales.domain.Product;
import com.owasp.sales.domain.Sale;
import com.owasp.sales.dto.ProductResponse;
import com.owasp.sales.dto.SaleResponse;
import com.owasp.sales.repository.ProductRepository;
import com.owasp.sales.repository.SaleRepository;
import com.owasp.sales.security.SecurityUtils;
import com.owasp.sales.util.PiiRedactor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class SalesService {

    private final ProductRepository productRepository;
    private final SaleRepository saleRepository;

    public SalesService(ProductRepository productRepository, SaleRepository saleRepository) {
        this.productRepository = productRepository;
        this.saleRepository = saleRepository;
    }

    @Transactional(readOnly = true)
    public List<ProductResponse> getProducts() {
        return productRepository.findAllByOrderByNameAsc().stream()
                .map(this::toProductResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<SaleResponse> getSales(String productCode) {
        String normalizedProductCode = productCode == null || productCode.isBlank()
                ? null
                : productCode.trim();

        if (normalizedProductCode != null) {
            productRepository.findByProductCodeIgnoreCase(normalizedProductCode)
                    .orElseThrow(() -> new ProductNotFoundException(
                            "Unknown product code: " + normalizedProductCode));
        }

        boolean includeCustomerPii = SecurityUtils.canViewSalesCustomerPii();
        return saleRepository.findSales(normalizedProductCode).stream()
                .map(sale -> toSaleResponse(sale, includeCustomerPii))
                .toList();
    }

    private ProductResponse toProductResponse(Product product) {
        return new ProductResponse(
                product.getId(),
                product.getProductCode(),
                product.getName(),
                product.getDescription(),
                product.getListPrice());
    }

    private SaleResponse toSaleResponse(Sale sale, boolean includeCustomerPii) {
        Product product = sale.getProduct();
        String customerName = includeCustomerPii
                ? sale.getCustomerName()
                : PiiRedactor.redact(sale.getCustomerName());
        String customerPhone = includeCustomerPii
                ? sale.getCustomerPhone()
                : PiiRedactor.redact(sale.getCustomerPhone());

        return new SaleResponse(
                sale.getId(),
                product.getProductCode(),
                product.getName(),
                sale.getPurchaseDate(),
                sale.getSalePrice(),
                customerName,
                customerPhone,
                !includeCustomerPii);
    }
}
