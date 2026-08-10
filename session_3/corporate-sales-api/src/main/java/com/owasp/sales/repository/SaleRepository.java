package com.owasp.sales.repository;

import com.owasp.sales.domain.Sale;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface SaleRepository extends JpaRepository<Sale, Integer> {

    @Query("""
            SELECT s FROM Sale s
            JOIN FETCH s.product
            WHERE (:productCode IS NULL OR LOWER(s.product.productCode) = LOWER(:productCode))
            ORDER BY s.purchaseDate DESC
            """)
    List<Sale> findSales(@Param("productCode") String productCode);
}
