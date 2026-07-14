package ict.thesis.booking.service;

import ict.thesis.booking.config.PayPalConfig;
import ict.thesis.booking.enties.Order;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Base64;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class PayPalService {

    private final PayPalConfig payPalConfig;
    private final RestTemplate rawRestTemplate = new RestTemplate(); // Standard RestTemplate for external calls

    private String getAccessToken() {
        String auth = payPalConfig.getClientId() + ":" + payPalConfig.getClientSecret();
        String encodedAuth = Base64.getEncoder().encodeToString(auth.getBytes());

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        headers.set("Authorization", "Basic " + encodedAuth);

        MultiValueMap<String, String> map = new LinkedMultiValueMap<>();
        map.add("grant_type", "client_credentials");

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(map, headers);
        String url = payPalConfig.getBaseUrl() + "/v1/oauth2/token";

        try {
            ResponseEntity<Map> response = rawRestTemplate.postForEntity(url, request, Map.class);
            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                return (String) response.getBody().get("access_token");
            }
        } catch (Exception e) {
            log.error("Failed to get PayPal access token", e);
        }
        return null;
    }

    public String createPayPalOrder(Order order) {
        String accessToken = getAccessToken();
        if (accessToken == null) {
            log.error("Cannot create PayPal order because access token is null");
            return null;
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(accessToken);

        // Convert VND to USD with fixed exchange rate (e.g. 25000)
        BigDecimal usdAmount = order.getTotalAmount().divide(new BigDecimal("25000"), 2, RoundingMode.HALF_UP);

        Map<String, Object> amountMap = Map.of(
                "currency_code", "USD",
                "value", usdAmount.toString()
        );

        Map<String, Object> purchaseUnit = Map.of(
                "reference_id", order.getId().toString(),
                "amount", amountMap,
                "description", "Thanh toan ve TicketHub cho don hang " + order.getOrderCode()
        );

        Map<String, Object> applicationContext = Map.of(
                "return_url", payPalConfig.getReturnUrl() + "?orderId=" + order.getId(),
                "cancel_url", "http://localhost:4200/checkout/" + order.getId() + "?error=payment_cancelled"
        );

        Map<String, Object> body = Map.of(
                "intent", "CAPTURE",
                "purchase_units", List.of(purchaseUnit),
                "application_context", applicationContext
        );

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);
        String url = payPalConfig.getBaseUrl() + "/v2/checkout/orders";

        try {
            ResponseEntity<Map> response = rawRestTemplate.postForEntity(url, request, Map.class);
            if (response.getStatusCode() == HttpStatus.CREATED && response.getBody() != null) {
                List<Map<String, Object>> links = (List<Map<String, Object>>) response.getBody().get("links");
                if (links != null) {
                    for (Map<String, Object> link : links) {
                        if ("approve".equals(link.get("rel"))) {
                            return (String) link.get("href");
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.error("Failed to create PayPal order", e);
        }
        return null;
    }

    public boolean capturePayPalOrder(String payPalOrderId) {
        String accessToken = getAccessToken();
        if (accessToken == null) {
            log.error("Cannot capture PayPal order because access token is null");
            return false;
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(accessToken);

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(Map.of(), headers);
        String url = payPalConfig.getBaseUrl() + "/v2/checkout/orders/" + payPalOrderId + "/capture";

        try {
            ResponseEntity<Map> response = rawRestTemplate.postForEntity(url, request, Map.class);
            if (response.getStatusCode() == HttpStatus.CREATED || response.getStatusCode() == HttpStatus.OK) {
                if (response.getBody() != null) {
                    String status = (String) response.getBody().get("status");
                    log.info("PayPal Capture status for order {}: {}", payPalOrderId, status);
                    return "COMPLETED".equalsIgnoreCase(status);
                }
            }
        } catch (Exception e) {
            log.error("Failed to capture PayPal order " + payPalOrderId, e);
        }
        return false;
    }
}
