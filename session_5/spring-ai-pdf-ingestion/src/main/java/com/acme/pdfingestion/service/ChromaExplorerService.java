package com.acme.pdfingestion.service;

import com.acme.pdfingestion.dto.ChromaCollectionSummaryResponse;
import org.springframework.ai.chroma.vectorstore.ChromaApi;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ChromaExplorerService {

    private static final String TENANT = "default_tenant";
    private static final String DATABASE = "default_database";

    private final ChromaApi chromaApi;

    public ChromaExplorerService(
            ChromaApi chromaApi) {

        this.chromaApi = chromaApi;
    }

    public List<ChromaCollectionSummaryResponse> listCollections() {

        return chromaApi
                .listCollections(TENANT, DATABASE)
                .stream()
                .map(collection -> {

                    Long recordCount =
                            chromaApi.countEmbeddings(
                                    TENANT,
                                    DATABASE,
                                    collection.id()
                            );

                    return new ChromaCollectionSummaryResponse(
                            collection.name(),
                            recordCount
                    );
                })
                .toList();
    }

    public ChromaApi.Collection getCollection(
            String collectionName) {

        return chromaApi.getCollection(
                TENANT,
                DATABASE,
                collectionName
        );
    }

    public Long countRecords(
            String collectionName) {

        ChromaApi.Collection collection =
                chromaApi.getCollection(
                        TENANT,
                        DATABASE,
                        collectionName
                );

        return chromaApi.countEmbeddings(
                TENANT,
                DATABASE,
                collection.id()
        );
    }
}
