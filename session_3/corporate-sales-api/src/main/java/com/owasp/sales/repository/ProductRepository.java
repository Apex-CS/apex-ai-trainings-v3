package com.owasp.sales.repository;

import com.owasp.sales.domain.Product;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Integer> {

    List<Product> findAllByOrderByNameAsc();

    Optional<Product> findByProductCodeIgnoreCase(String productCode);
}
