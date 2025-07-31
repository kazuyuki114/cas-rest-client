package com.hust.restclient.service;

import java.io.ByteArrayInputStream;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import com.hust.restclient.config.CasConfig;
import com.hust.restclient.dto.CasAuthenResult;
import com.hust.restclient.dto.CasLoginResult;
import com.hust.restclient.dto.CasUserDetail;

import lombok.RequiredArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class CasRestClient {
    
    private final CasConfig casConfig;
    private final RestTemplate restTemplate;

    public CasConfig getCasConfig() {
        return casConfig;
    }

    /**
     * Step 1: Request TGT (Ticket Granting Ticket)
     */
    public String requestTgt(String username, String password) {
        String tgtUrl = casConfig.getServerUrl() + "v1/tickets";
        log.info("Requesting TGT from URL: {}", tgtUrl);
        log.info("CAS Server URL: {}", casConfig.getServerUrl());
        log.info("CAS Client Service URL: {}", casConfig.getClientServiceUrl());
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        
        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("username", username);
        body.add("password", password);
        // For REST protocol, we don't include service URL in TGT request
        // This avoids the SSO denial issue
        
        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);
        
        try {
            log.info("Sending TGT request with body: {}", body);
            ResponseEntity<String> response = restTemplate.exchange(tgtUrl, HttpMethod.POST, request, String.class);
            
            log.info("TGT response status: {}", response.getStatusCode());
            log.info("TGT response headers: {}", response.getHeaders());
            log.info("TGT response body: {}", response.getBody());
            
            if (response.getStatusCode().is2xxSuccessful()) {
                // Extract TGT from Location header
                String location = response.getHeaders().getFirst("Location");
                if (location != null && location.contains("TGT-")) {
                    String tgt = location.substring(location.lastIndexOf("/") + 1);
                    log.info("TGT obtained successfully: {}", tgt);
                    return tgt;
                } else {
                    log.error("TGT not found in Location header: {}", location);
                }
            }
            
            log.error("Failed to obtain TGT. Status: {}", response.getStatusCode());
            return null;
            
        } catch (Exception e) {
            log.error("Error requesting TGT", e);
            log.error("Error details: {}", e.getMessage());
            if (e.getCause() != null) {
                log.error("Root cause: {}", e.getCause().getMessage());
            }
            return null;
        }
    }
    
    /**
     * Step 2: Request ST (Service Ticket) using TGT
     */
    public String requestServiceTicket(String tgt, String service, String username, String password) {
        String stUrl = casConfig.getServerUrl() + "v1/tickets/" + tgt;
        log.info("Requesting Service Ticket from URL: {}", stUrl);
        log.info("Service URL being sent: {}", service);
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        
        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        if(username != null && !username.trim().isEmpty() && password != null && !password.trim().isEmpty()) {
            body.add("username", username);
            body.add("password", password);
        }
        // Don't URL-encode the service URL in the request body
        body.add("service", service);
        log.info("ST request body: {}", body);
        
        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);
        
        try {
            log.info("Sending ST request with headers: {}", headers);
            ResponseEntity<String> response = restTemplate.exchange(stUrl, HttpMethod.POST, request, String.class);
            
            log.info("ST response status: {}", response.getStatusCode());
            log.info("ST response headers: {}", response.getHeaders());
            log.info("ST response body: {}", response.getBody());
            
            if (response.getStatusCode().is2xxSuccessful()) {
                String serviceTicket = response.getBody();
                log.info("Service ticket obtained successfully: {}", serviceTicket);
                return serviceTicket;
            }
            
            log.error("Failed to obtain service ticket. Status: {}", response.getStatusCode());
            return null;
            
        } catch (Exception e) {
            log.error("Error requesting service ticket", e);
            return null;
        }
    }
    
    /**
     * Step 3: Validate Service Ticket
     */
    public CasUserDetail validateServiceTicket(String serviceTicket, String service) {
        String validateUrl = casConfig.getServerUrl() + "serviceValidate";
        String fullUrl = validateUrl + "?ticket=" + serviceTicket + "&service=" + service;
        log.info("Validating Service Ticket at URL: {}", fullUrl);
        
        try {
            ResponseEntity<String> response = restTemplate.getForEntity(fullUrl, String.class);
            
            if (response.getStatusCode().is2xxSuccessful()) {
                String responseBody = response.getBody();
                log.info("Validation response: {}", responseBody);
                
                if (responseBody != null && responseBody.contains("<cas:authenticationSuccess>")) {
                    return parseUserDetailFromXml(responseBody);
                } else {
                    log.warn("Authentication failed in CAS response");
                    return CasUserDetail.failure();
                }
            }
            
            log.error("Failed to validate service ticket. Status: {}", response.getStatusCode());
            return CasUserDetail.failure();
            
        } catch (Exception e) {
            log.error("Error validating service ticket", e);
            return CasUserDetail.failure();
        }
    }

    private CasUserDetail parseUserDetailFromXml(String xmlResponse) {
        try {
            log.info("=== PARSING CAS XML RESPONSE ===");
            log.info("Full XML Response: {}", xmlResponse);
            
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(new ByteArrayInputStream(xmlResponse.getBytes()));
            
            // Extract username
            NodeList userNodes = doc.getElementsByTagName("cas:user");
            String username = null;
            if (userNodes.getLength() > 0) {
                username = userNodes.item(0).getTextContent().trim();
                log.info("Extracted username: {}", username);
            } else {
                log.warn("No cas:user element found in XML");
            }
            
            // Extract role from groupMembership
            String role = null;
            NodeList attributeNodes = doc.getElementsByTagName("cas:attributes");
            log.info("Found {} cas:attributes nodes", attributeNodes.getLength());
            
            if (attributeNodes.getLength() > 0) {
                Element attributesElement = (Element) attributeNodes.item(0);
                NodeList groupNodes = attributesElement.getElementsByTagName("cas:groupMembership");
                log.info("Found {} cas:groupMembership nodes", groupNodes.getLength());
                
                if (groupNodes.getLength() > 0) {
                    role = groupNodes.item(0).getTextContent().trim();
                    log.info("Extracted role from cas:groupMembership: {}", role);
                } else {
                    log.warn("No cas:groupMembership element found in cas:attributes");
                    
                    // Debug: Let's see what attributes ARE available
                    NodeList allChildren = attributesElement.getChildNodes();
                    log.info("Available attribute elements:");
                    for (int i = 0; i < allChildren.getLength(); i++) {
                        if (allChildren.item(i).getNodeType() == org.w3c.dom.Node.ELEMENT_NODE) {
                            log.info("  - Element: {} = {}", 
                                allChildren.item(i).getNodeName(), 
                                allChildren.item(i).getTextContent());
                        }
                    }
                }
            } else {
                log.warn("No cas:attributes element found in XML - user may not have role information");
            }
            
            if (username != null) {
                log.info("Extracted user details: username={}, role={}", username, role);
                CasUserDetail userDetail = CasUserDetail.success(username, role);
                log.info("Created CasUserDetail: success={}, username={}, role={}", 
                    userDetail.isSuccess(), userDetail.getUsername(), userDetail.getRole());
                return userDetail;
            } else {
                log.warn("No username found in CAS response");
                return CasUserDetail.failure();
            }
            
        } catch (Exception e) {
            log.error("Error parsing XML response", e);
            return CasUserDetail.failure();
        }
    }
    
    /**
     * Complete CAS login flow
     */
    public CasLoginResult performCasLogin(String username, String password) {
        // Step 1: Request TGT
        String tgt = requestTgt(username, password);
        if (tgt == null) {
            return CasLoginResult.failure("Failed to obtain TGT");
        }
        
        // Step 2: Request ST
        String serviceTicket = requestServiceTicket(tgt, casConfig.getClientServiceUrl(), username, password);
        if (serviceTicket == null) {
            return CasLoginResult.failure("Failed to obtain service ticket");
        }
        
        // Step 3: Validate ST and get user details
        CasUserDetail userDetail = validateServiceTicket(serviceTicket, casConfig.getClientServiceUrl());
        if (!userDetail.isSuccess()) {
            return CasLoginResult.failure("Service ticket validation failed");
        }
        
        // Generate CASTGC cookie value (this would typically be set by the browser)
        String castgcCookie = "CASTGC=" + tgt + "; Path=/; Secure; HttpOnly";
        
        return CasLoginResult.success(serviceTicket, castgcCookie, userDetail);
    }

    /**
     * Complete CAS authen flows
     */
    public CasAuthenResult performAuthen(String tgt){
        if(tgt == null){
            return CasAuthenResult.failure("Failed to obtain TGT");
        }
        // Step 1: Request ST
        String serviceTicket = requestServiceTicket(tgt, casConfig.getClientServiceUrl(),null, null);
        if (serviceTicket == null) {
            return CasAuthenResult.failure("Failed to obtain service ticket");
        }
        // Step 2: Validate ST and get user details
        CasUserDetail userDetail = validateServiceTicket(serviceTicket, casConfig.getClientServiceUrl());
        if (!userDetail.isSuccess()) {
            return CasAuthenResult.failure("Service ticket validation failed");
        }
        return CasAuthenResult.success(serviceTicket);
    }
} 