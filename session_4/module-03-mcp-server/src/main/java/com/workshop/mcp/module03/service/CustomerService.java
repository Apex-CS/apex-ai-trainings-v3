package com.workshop.mcp.module03.service;

import com.workshop.mcp.module03.dto.CustomerDTO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;
import java.util.Optional;

/**
 * CustomerService wraps the legacy Customer REST API using Spring RestClient.
 *
 * <p>RestClient (Spring Boot 3.2+) offers a fluent, synchronous API ideal
 * for MCP tool implementations that need to call downstream services.
 * Combined with Virtual Threads, blocking HTTP calls scale efficiently.
 */
@Service
public class CustomerService {

    private final RestClient restClient;

    public CustomerService(@Value("${customer.api.base-url}") String baseUrl) {
        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader("Accept", MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    public CustomerDTO create(CustomerDTO dto) {
        return restClient.post()
                .uri("/api/customers")
                .body(dto)
                .retrieve()
                .body(CustomerDTO.class);
    }

    public Optional<CustomerDTO> findById(String customerId) {
        try {
            CustomerDTO customer = restClient.get()
                    .uri("/api/customers/{id}", customerId)
                    .retrieve()
                    .body(CustomerDTO.class);
            return Optional.ofNullable(customer);
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    public List<CustomerDTO> findAll(String status, String tier) {
        var uriBuilder = UriComponentsBuilder.fromPath("/api/customers");
        if (status != null && !status.isBlank()) {
            uriBuilder.queryParam("status", status);
        }
        if (tier != null && !tier.isBlank()) {
            uriBuilder.queryParam("tier", tier);
        }

        return restClient.get()
                .uri(uriBuilder.toUriString())
                .retrieve()
                .body(new ParameterizedTypeReference<List<CustomerDTO>>() {});
    }

    public CustomerDTO update(String customerId, String name, String email, String tier) {
        CustomerDTO patch = CustomerDTO.forUpdate(name, email, tier);
        return restClient.patch()
                .uri("/api/customers/{id}", customerId)
                .body(patch)
                .retrieve()
                .body(CustomerDTO.class);
    }
}
