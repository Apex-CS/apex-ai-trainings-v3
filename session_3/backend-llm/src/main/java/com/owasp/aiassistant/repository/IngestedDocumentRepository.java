package com.owasp.aiassistant.repository;

import com.owasp.aiassistant.domain.IngestedDocument;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface IngestedDocumentRepository extends JpaRepository<IngestedDocument, Long> {

    Optional<IngestedDocument> findByDocumentId(String documentId);
}
