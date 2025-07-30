package com.hust.restclient.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.web.client.RestTemplate;

import javax.net.ssl.*;
import java.io.IOException;
import java.io.InputStream;
import java.security.KeyStore;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;

@Slf4j
@Configuration
public class SslConfig {

    @Bean
    public RestTemplate restTemplate() throws Exception {
        // Create a custom SSL context that trusts the server certificate
        SSLContext sslContext = createSSLContext();
        
        // Create a custom SSL socket factory
        SSLSocketFactory sslSocketFactory = sslContext.getSocketFactory();
        
        // Create a custom hostname verifier that accepts all hostnames
        HostnameVerifier hostnameVerifier = (hostname, session) -> true;
        
        // Create a custom HTTP client factory
        org.springframework.http.client.SimpleClientHttpRequestFactory requestFactory = 
            new org.springframework.http.client.SimpleClientHttpRequestFactory() {
                @Override
                protected void prepareConnection(java.net.HttpURLConnection connection, String httpMethod) throws IOException {
                    super.prepareConnection(connection, httpMethod);
                    if (connection instanceof HttpsURLConnection) {
                        HttpsURLConnection httpsConnection = (HttpsURLConnection) connection;
                        httpsConnection.setSSLSocketFactory(sslSocketFactory);
                        httpsConnection.setHostnameVerifier(hostnameVerifier);
                    }
                }
            };
        
        return new RestTemplate(requestFactory);
    }
    
    private SSLContext createSSLContext() throws Exception {
        try {
            // Load the server certificate
            CertificateFactory cf = CertificateFactory.getInstance("X.509");
            ClassPathResource resource = new ClassPathResource("server.crt");
            
            if (!resource.exists()) {
                log.warn("server.crt not found in classpath, creating trust-all SSL context for development");
                return createTrustAllSSLContext();
            }
            
            try (InputStream certInputStream = resource.getInputStream()) {
                X509Certificate cert = (X509Certificate) cf.generateCertificate(certInputStream);
                
                // Create a trust store with the server certificate
                KeyStore trustStore = KeyStore.getInstance(KeyStore.getDefaultType());
                trustStore.load(null, null);
                trustStore.setCertificateEntry("cas-server", cert);
                
                // Create a trust manager that trusts our certificate
                TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
                tmf.init(trustStore);
                
                // Create SSL context
                SSLContext sslContext = SSLContext.getInstance("TLS");
                sslContext.init(null, tmf.getTrustManagers(), null);
                
                log.info("SSL context created successfully with server certificate");
                return sslContext;
                
            }
        } catch (Exception e) {
            log.error("Error creating SSL context, using trust-all for development", e);
            return createTrustAllSSLContext();
        }
    }
    
    private SSLContext createTrustAllSSLContext() throws Exception {
        // Create a trust manager that trusts all certificates (for development only)
        TrustManager[] trustAllCerts = new TrustManager[] {
            new X509TrustManager() {
                public X509Certificate[] getAcceptedIssuers() { return null; }
                public void checkClientTrusted(X509Certificate[] certs, String authType) { }
                public void checkServerTrusted(X509Certificate[] certs, String authType) { }
            }
        };
        
        SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(null, trustAllCerts, new java.security.SecureRandom());
        
        log.warn("Created trust-all SSL context for development (INSECURE - only use for development)");
        return sslContext;
    }
} 