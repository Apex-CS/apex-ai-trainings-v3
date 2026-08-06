package com.owasp.aiassistant.config;

import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;
import java.io.InputStream;
import java.security.KeyStore;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Configuration
public class WebClientSslConfig {

    @Bean
    @ConditionalOnProperty(name = "app.http.corporate-ca-enabled", havingValue = "true", matchIfMissing = true)
    WebClient.Builder corporateWebClientBuilder(
            @Value("${app.http.corporate-ca-pem:classpath:nike-ca-certs-2027.pem}") Resource corporateCaPem)
            throws Exception {
        SslContext sslContext = buildSslContext(corporateCaPem);
        HttpClient httpClient = HttpClient.create().secure(ssl -> ssl.sslContext(sslContext));
        return WebClient.builder().clientConnector(new ReactorClientHttpConnector(httpClient));
    }

    private static SslContext buildSslContext(Resource corporateCaPem) throws Exception {
        X509TrustManager defaultTrustManager = defaultTrustManager();
        X509TrustManager corporateTrustManager = corporateTrustManager(corporateCaPem);
        X509TrustManager composite = new CompositeTrustManager(defaultTrustManager, corporateTrustManager);
        return SslContextBuilder.forClient().trustManager(composite).build();
    }

    private static X509TrustManager defaultTrustManager() throws Exception {
        TrustManagerFactory factory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        factory.init((KeyStore) null);
        return extractX509TrustManager(factory);
    }

    private static X509TrustManager corporateTrustManager(Resource corporateCaPem) throws Exception {
        CertificateFactory certificateFactory = CertificateFactory.getInstance("X.509");
        Collection<? extends Certificate> certificates;
        try (InputStream inputStream = corporateCaPem.getInputStream()) {
            certificates = certificateFactory.generateCertificates(inputStream);
        }

        KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
        keyStore.load(null, null);

        int index = 0;
        for (Certificate certificate : certificates) {
            keyStore.setCertificateEntry("corporate-ca-" + index++, certificate);
        }

        TrustManagerFactory factory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        factory.init(keyStore);
        return extractX509TrustManager(factory);
    }

    private static X509TrustManager extractX509TrustManager(TrustManagerFactory factory) {
        for (TrustManager trustManager : factory.getTrustManagers()) {
            if (trustManager instanceof X509TrustManager x509TrustManager) {
                return x509TrustManager;
            }
        }
        throw new IllegalStateException("No X509TrustManager available");
    }

    private static final class CompositeTrustManager implements X509TrustManager {

        private final List<X509TrustManager> delegates;

        private CompositeTrustManager(X509TrustManager... delegates) {
            this.delegates = List.of(delegates);
        }

        @Override
        public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {
            CertificateException last = null;
            for (X509TrustManager delegate : delegates) {
                try {
                    delegate.checkClientTrusted(chain, authType);
                    return;
                } catch (CertificateException e) {
                    last = e;
                }
            }
            if (last != null) {
                throw last;
            }
        }

        @Override
        public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
            CertificateException last = null;
            for (X509TrustManager delegate : delegates) {
                try {
                    delegate.checkServerTrusted(chain, authType);
                    return;
                } catch (CertificateException e) {
                    last = e;
                }
            }
            if (last != null) {
                throw last;
            }
        }

        @Override
        public X509Certificate[] getAcceptedIssuers() {
            List<X509Certificate> issuers = new ArrayList<>();
            for (X509TrustManager delegate : delegates) {
                for (X509Certificate issuer : delegate.getAcceptedIssuers()) {
                    issuers.add(issuer);
                }
            }
            return issuers.toArray(X509Certificate[]::new);
        }
    }
}
