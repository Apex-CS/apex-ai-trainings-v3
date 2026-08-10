package com.owasp.it.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "app_restarts")
@Getter
@Setter
@NoArgsConstructor
public class AppRestart {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "app_name", nullable = false, length = 60)
    private String appName;

    @Column(name = "user_requested", nullable = false, length = 100)
    private String userRequested;

    @Column(name = "operation_date", nullable = false)
    private LocalDateTime operationDate;

    @Column(name = "operation_done", nullable = false)
    private boolean operationDone;
}
