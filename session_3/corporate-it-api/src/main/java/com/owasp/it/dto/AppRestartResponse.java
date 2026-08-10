package com.owasp.it.dto;

import java.time.LocalDateTime;

public record AppRestartResponse(
        Integer id,
        String appName,
        String userRequested,
        LocalDateTime operationDate,
        boolean operationDone) {
}
