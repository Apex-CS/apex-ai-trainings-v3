package com.workshop.mcp.module03.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Customer domain object — used both as input to create/update tool parameters
 * and as the response type from the legacy REST API.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public record CustomerDTO(

        @JsonProperty("id")
        String id,

        @JsonProperty("name")
        String name,

        @JsonProperty("email")
        String email,

        @JsonProperty("phone")
        String phone,

        /** Service tier: STANDARD, PREMIUM, or ENTERPRISE */
        @JsonProperty("tier")
        String tier,

        /** Account status: ACTIVE, INACTIVE, or SUSPENDED */
        @JsonProperty("status")
        String status,

        @JsonProperty("createdAt")
        String createdAt,

        @JsonProperty("updatedAt")
        String updatedAt
) {
    /**
     * Factory method for creation requests — id, timestamps are server-assigned.
     */
    public static CustomerDTO forCreation(String name, String email, String phone, String tier) {
        return new CustomerDTO(null, name, email, phone, tier, "ACTIVE", null, null);
    }

    /**
     * Factory method for partial update — only non-null fields are included in the PATCH body.
     */
    public static CustomerDTO forUpdate(String name, String email, String tier) {
        return new CustomerDTO(null, name, email, null, tier, null, null, null);
    }
}
