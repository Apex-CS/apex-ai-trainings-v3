package com.workshop.mcp.module03.tools;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.workshop.mcp.module03.dto.CustomerDTO;
import com.workshop.mcp.module03.service.CustomerService;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Customer Management MCP Tools — Module 03.
 *
 * <p>Each method wraps a legacy REST API call. The @Tool description is what
 * the LLM reads to decide when and how to call this tool. Write descriptions
 * as if explaining to a non-technical person what the action does and what
 * values are valid for each parameter.
 */
@Service
public class CustomerTools {

    private final CustomerService customerService;
    private final ObjectMapper objectMapper;

    public CustomerTools(CustomerService customerService, ObjectMapper objectMapper) {
        this.customerService = customerService;
        this.objectMapper = objectMapper;
    }

    @Tool(description = """
            Creates a new customer account in the system.
            Required: name (full name), email (unique email address),
            phone (E.164 format e.g. +12025551234),
            tier (service level: STANDARD, PREMIUM, or ENTERPRISE).
            Returns the created customer object including the generated ID.""")
    public String createCustomer(
            @ToolParam(description = "Customer's full name") String name,
            @ToolParam(description = "Email address — must be unique across all customers") String email,
            @ToolParam(description = "Phone number in E.164 international format, e.g. +12025551234") String phone,
            @ToolParam(description = "Service tier: STANDARD, PREMIUM, or ENTERPRISE") String tier) {
        CustomerDTO created = customerService.create(CustomerDTO.forCreation(name, email, phone, tier));
        return toJson(created);
    }

    @Tool(description = """
            Retrieves a single customer by their unique ID.
            Returns the customer object if found, or the string "null" if no customer exists with that ID.""")
    public String getCustomer(
            @ToolParam(description = "Customer unique identifier, e.g. cust-a1b2c3d4") String customerId) {
        return customerService.findById(customerId)
                .map(this::toJson)
                .orElse("null");
    }

    @Tool(description = """
            Lists customers, optionally filtered by account status or service tier.
            Leave status or tier blank to include all values.
            Valid status values: ACTIVE, INACTIVE, SUSPENDED.
            Valid tier values: STANDARD, PREMIUM, ENTERPRISE.
            Returns a JSON array of customer objects.""")
    public String listCustomers(
            @ToolParam(description = "Filter by account status: ACTIVE, INACTIVE, or SUSPENDED. Leave blank for all.") String status,
            @ToolParam(description = "Filter by tier: STANDARD, PREMIUM, or ENTERPRISE. Leave blank for all.") String tier) {
        List<CustomerDTO> customers = customerService.findAll(status, tier);
        return toJson(customers);
    }

    @Tool(description = """
            Updates an existing customer's details (partial update — only provided fields change).
            Provide null or empty string for fields you do not want to change.
            Returns the updated customer object.""")
    public String updateCustomer(
            @ToolParam(description = "ID of the customer to update") String customerId,
            @ToolParam(description = "New full name, or null to keep the current name") String name,
            @ToolParam(description = "New email address, or null to keep the current email") String email,
            @ToolParam(description = "New tier (STANDARD, PREMIUM, ENTERPRISE), or null to keep current") String tier) {
        CustomerDTO updated = customerService.update(customerId, name, email, tier);
        return toJson(updated);
    }

    private String toJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (Exception e) {
            return "{\"error\": \"Serialization failed: " + e.getMessage() + "\"}";
        }
    }
}
