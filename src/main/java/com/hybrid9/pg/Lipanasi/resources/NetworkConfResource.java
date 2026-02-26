package com.hybrid9.pg.Lipanasi.resources;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.hybrid9.pg.Lipanasi.entities.order.Order;
import com.hybrid9.pg.Lipanasi.enums.MobileNetworkType;
import com.hybrid9.pg.Lipanasi.models.pgmodels.mobilenetworks.*;
import com.hybrid9.pg.Lipanasi.models.pgmodels.mobilenetworks.decrypted.AirtelMoneyConfigDTO;
import com.hybrid9.pg.Lipanasi.models.pgmodels.mobilenetworks.decrypted.HaloPesaConfigDTO;
import com.hybrid9.pg.Lipanasi.models.pgmodels.mobilenetworks.decrypted.MPesaConfigDTO;
import com.hybrid9.pg.Lipanasi.models.pgmodels.mobilenetworks.decrypted.MixxByYasConfigDTO;
import com.hybrid9.pg.Lipanasi.models.session.UserSession;
import com.hybrid9.pg.Lipanasi.resources.excpts.CustomExcpts;
import com.hybrid9.pg.Lipanasi.services.order.OrderService;
import com.hybrid9.pg.Lipanasi.services.payments.gw.MobileMoneyCollectionService;
import com.hybrid9.pg.Lipanasi.services.payments.gw.MobileNetworkConfigService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

@Slf4j
@Component
public class NetworkConfResource {

    @Value("${partner.validation.url:https://api.business.payscoop.com/api/validate}")
    private String partnerValidationUrl;

    @Value("${network.config.url:https://api.business.payscoop.com/api/collection-credential}")
    private String networkConfigUrl;

    private final EncryptedJsonDecryptor encryptedJsonDecryptor;
    private final MobileMoneyCollectionService mobileMoneyCollectionService;
    private final OrderService orderService;
    private final RestTemplate restTemplate;

    public NetworkConfResource(EncryptedJsonDecryptor encryptedJsonDecryptor, MobileMoneyCollectionService mobileMoneyCollectionService, OrderService orderService, RestTemplate restTemplate) {
        this.encryptedJsonDecryptor = encryptedJsonDecryptor;
        this.mobileMoneyCollectionService = mobileMoneyCollectionService;
        this.orderService = orderService;
        this.restTemplate = restTemplate;
    }


    /**
     * Get Mobile Network Config
     */
    public MobileNetworkConfig getMobileNetworkConfig(String operatorName, UserSession session) {

        AtomicReference<MobileNetworkConfig> mobileNetworkConfig = new AtomicReference<>();
        String newOperatorName = operatorName.equalsIgnoreCase("Mpesa-Tanzania") ? "Vodacom" : operatorName.equalsIgnoreCase("AirtelMoney-Tanzania") ? "Airtel" : operatorName.equalsIgnoreCase("Halopesa-Tanzania") ? "Halopesa" : operatorName.equalsIgnoreCase("Mixx_by_yas-Tanzania") ? "Mixx" : operatorName;
        this.mobileMoneyCollectionService.getMobileMoneyCollection(operatorName).ifPresentOrElse(mobileMoneyCollection -> {
            try {
                String decrypted = encryptedJsonDecryptor.decrypt(mobileMoneyCollection.getCredential());
                ObjectMapper mapper = new ObjectMapper();
                if (newOperatorName.equalsIgnoreCase("Vodacom")) {
                    MPesaConfigDTO decryptedConfig = mapper.readValue(decrypted, MPesaConfigDTO.class);
                    mobileNetworkConfig.set(MPesaConfig.builder()
                            .apiUrl(decryptedConfig.getApiUrl())
                            .callbackUrl(decryptedConfig.getCallbackUrl())
                            .country(decryptedConfig.getCountry())
                            .username(decryptedConfig.getUsername())
                            .password(decryptedConfig.getPassword())
                            .tokenEventId(decryptedConfig.getTokenEventId())
                            .requestEventId(decryptedConfig.getRequestEventId())
                            .businessName(decryptedConfig.getBusinessName())
                            .businessNumber(decryptedConfig.getBusinessNumber())
                            .paybillApiBaseUrl(decryptedConfig.getPaybillApiBaseUrl())
                            .paybillCallbackUrl(decryptedConfig.getPaybillCallbackUrl())
                            .paybillSpId(decryptedConfig.getPaybillSpId())
                            .paybillSpPassword(decryptedConfig.getPaybillSpPassword())
                            .tokenApiUrl(decryptedConfig.getTokenApiUrl())
                            .status(decryptedConfig.getStatus())
                            .build());

                }
                if (newOperatorName.equalsIgnoreCase("Mixx")) {
                    MixxByYasConfigDTO decryptedConfig = mapper.readValue(decrypted, MixxByYasConfigDTO.class);
                    mobileNetworkConfig.set(MixxByYasConfig.builder()
                            .apiUrl(decryptedConfig.getApiUrl())
                            .callbackUrl(decryptedConfig.getCallbackUrl())
                            .tokenUrl(decryptedConfig.getTokenUrl())
                            .billerMsisdn(decryptedConfig.getBillerMsisdn())
                            .username(decryptedConfig.getUsername())
                            .password(decryptedConfig.getPassword())
                            .tqsApiUrl(decryptedConfig.getTqsApiUrl())
                            .tqsPassword(decryptedConfig.getTqsPassword())
                            .status(decryptedConfig.getStatus())
                            .build());
                }
                if (newOperatorName.equalsIgnoreCase("Airtel")) {
                    AirtelMoneyConfigDTO decryptedConfig = mapper.readValue(decrypted, AirtelMoneyConfigDTO.class);
                    mobileNetworkConfig.set(AirtelMoneyConfig.builder()
                            .apiUrl(decryptedConfig.getPush_url())
                            .tokenUrl(decryptedConfig.getAuth_url())
                            .baseUrl("https://api.airtel.com")
                            .country("TZ")
                            .currency("TZS")
                            .clientId(decryptedConfig.getClient_id())
                            .clientSecret(decryptedConfig.getClient_secret())
                            .status(decryptedConfig.getStatus())
                            .build());
                }
                if (newOperatorName.equalsIgnoreCase("Halopesa")) {
                    HaloPesaConfigDTO decryptedConfig = mapper.readValue(decrypted, HaloPesaConfigDTO.class);
                    mobileNetworkConfig.set(HaloPesaConfig.builder()
                            .spId(decryptedConfig.getSpId())
                            .beneficiaryAccountId(decryptedConfig.getBeneficiaryAccountId())
                            .username(decryptedConfig.getUsername())
                            .password(decryptedConfig.getPassword())
                            .businessNumber(decryptedConfig.getBusinessNumber())
                            .secretKey(decryptedConfig.getSecretKey())
                            .merchantCode(decryptedConfig.getMerchantCode())
                            .apiUrl(decryptedConfig.getApiUrl())
                            .tqsUrl(decryptedConfig.getTqsUrl())
                            .status(decryptedConfig.getStatus())
                            .build());
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }


        }, () -> {

            String decrypted = null;
            try {
                decrypted = encryptedJsonDecryptor.decrypt(getConfigurations(session, newOperatorName));
                ObjectMapper mapper = new ObjectMapper();
                if (newOperatorName.equalsIgnoreCase("Vodacom")) {
                    MPesaConfigDTO decryptedConfig = mapper.readValue(decrypted, MPesaConfigDTO.class);
                    mobileNetworkConfig.set(MPesaConfig.builder()
                            .apiUrl(decryptedConfig.getApiUrl())
                            .callbackUrl(decryptedConfig.getCallbackUrl())
                            .country(decryptedConfig.getCountry())
                            .username(decryptedConfig.getUsername())
                            .password(decryptedConfig.getPassword())
                            .tokenEventId(decryptedConfig.getTokenEventId())
                            .requestEventId(decryptedConfig.getRequestEventId())
                            .businessName(decryptedConfig.getBusinessName())
                            .businessNumber(decryptedConfig.getBusinessNumber())
                            .paybillApiBaseUrl(decryptedConfig.getPaybillApiBaseUrl())
                            .paybillCallbackUrl(decryptedConfig.getPaybillCallbackUrl())
                            .paybillSpId(decryptedConfig.getPaybillSpId())
                            .paybillSpPassword(decryptedConfig.getPaybillSpPassword())
                            .tokenApiUrl(decryptedConfig.getTokenApiUrl())
                            .status(decryptedConfig.getStatus())
                            .build());

                }
                if (newOperatorName.equalsIgnoreCase("Mixx")) {
                    MixxByYasConfigDTO decryptedConfig = mapper.readValue(decrypted, MixxByYasConfigDTO.class);
                    mobileNetworkConfig.set(MixxByYasConfig.builder()
                            .apiUrl(decryptedConfig.getApiUrl())
                            .callbackUrl(decryptedConfig.getCallbackUrl())
                            .tokenUrl(decryptedConfig.getTokenUrl())
                            .billerMsisdn(decryptedConfig.getBillerMsisdn())
                            .username(decryptedConfig.getUsername())
                            .password(decryptedConfig.getPassword())
                            .tqsApiUrl(decryptedConfig.getTqsApiUrl())
                            .tqsPassword(decryptedConfig.getTqsPassword())
                            .status(decryptedConfig.getStatus())
                            .build());
                }
                if (newOperatorName.equalsIgnoreCase("Airtel")) {
                    AirtelMoneyConfigDTO decryptedConfig = mapper.readValue(decrypted, AirtelMoneyConfigDTO.class);
                    mobileNetworkConfig.set(AirtelMoneyConfig.builder()
                            .apiUrl(decryptedConfig.getPush_url())
                            .tokenUrl(decryptedConfig.getAuth_url())
                            .baseUrl("https://api.airtel.com")
                            .country("TZ")
                            .currency("TZS")
                            .clientId(decryptedConfig.getClient_id())
                            .clientSecret(decryptedConfig.getClient_secret())
                            .status(decryptedConfig.getStatus())
                            .build());
                }
                if (newOperatorName.equalsIgnoreCase("Halopesa")) {
                    HaloPesaConfigDTO decryptedConfig = mapper.readValue(decrypted, HaloPesaConfigDTO.class);
                    mobileNetworkConfig.set(HaloPesaConfig.builder()
                            .spId(decryptedConfig.getSpId())
                            .beneficiaryAccountId(decryptedConfig.getBeneficiaryAccountId())
                            .username(decryptedConfig.getUsername())
                            .password(decryptedConfig.getPassword())
                            .businessNumber(decryptedConfig.getBusinessNumber())
                            .secretKey(decryptedConfig.getSecretKey())
                            .merchantCode(decryptedConfig.getMerchantCode())
                            .apiUrl(decryptedConfig.getApiUrl())
                            .tqsUrl(decryptedConfig.getTqsUrl())
                            .status(decryptedConfig.getStatus())
                            .build());
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }

        });
        return mobileNetworkConfig.get();
    }

    private String getConfigurations(UserSession session, String operatorName) {
        // Get the network config from Rest API if not found in Redis
        AtomicReference<Order> orderReference = new AtomicReference<>();
        orderService.findByOrderNumber(session.getOrderNumber()).ifPresentOrElse(orderReference::set, () -> {
            throw new CustomExcpts.OrderNotFoundException("Order not found: " + session.getOrderNumber());
        });

        // Decode the order signature to get the api key and secret
        String orderSignature = orderReference.get().getSignature().trim();
        log.debug("Order Signature: {}", orderSignature);

        String apiKey;
        String apiSecret;

        try {
            // First, check if the signature is already in JSON format
            if (orderSignature.startsWith("{") && orderSignature.endsWith("}")) {
                // Handle JSON format signature
                log.debug("Signature appears to be in JSON format");
                JsonNode jsonNode = new ObjectMapper().readTree(orderSignature);
                apiKey = jsonNode.get("api_key").asText();
                apiSecret = jsonNode.get("api_secret").asText();
            } else {
                // Handle Base64 encoded format
                log.debug("Attempting to decode Base64 signature");

                // Validate Base64 format before decoding
                if (!isValidBase64(orderSignature)) {
                    log.error("Invalid Base64 signature format: {}", orderSignature);
                    throw new RuntimeException("Invalid signature format - not valid Base64 or JSON");
                }

                Base64.Decoder decoder = Base64.getDecoder();
                byte[] decodedBytes = decoder.decode(orderSignature);
                String decodedString = new String(decodedBytes);
                log.debug("Decoded signature: {}", decodedString);

                String[] credentials = decodedString.split(":");
                if (credentials.length != 2) {
                    log.error("Invalid credential format after decoding: {}", decodedString);
                    throw new RuntimeException("Invalid credential format - expected 'apikey:secret'");
                }

                apiKey = credentials[0];
                apiSecret = credentials[1];
            }

            log.debug("Extracted API Key: {}, API Secret: [REDACTED]", apiKey);

        } catch (JsonProcessingException e) {
            log.error("Failed to parse JSON signature: {}", orderSignature, e);
            throw new RuntimeException("Failed to parse signature as JSON", e);
        } catch (IllegalArgumentException e) {
            log.error("Failed to decode Base64 signature: {}", orderSignature, e);
            throw new RuntimeException("Failed to decode Base64 signature: " + e.getMessage(), e);
        } catch (Exception e) {
            log.error("Unexpected error processing signature: {}", orderSignature, e);
            throw new RuntimeException("Unexpected error processing signature", e);
        }

        // Validate extracted credentials
        if (apiKey == null || apiKey.trim().isEmpty() || apiSecret == null || apiSecret.trim().isEmpty()) {
            log.error("Invalid API credentials extracted - API Key: {}, API Secret: [{}]",
                    apiKey, (apiSecret == null || apiSecret.trim().isEmpty()) ? "EMPTY" : "PRESENT");
            throw new RuntimeException("Invalid API credentials extracted from signature");
        }

        Map<String, String> requestBody = new HashMap<>();
        requestBody.put("api_key", apiKey.trim());
        requestBody.put("api_secret", apiSecret.trim());
        String requestJson = new Gson().toJson(requestBody);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(org.springframework.http.MediaType.APPLICATION_JSON);
        HttpEntity<String> entity = new HttpEntity<>(requestJson, headers);

        try {
            ResponseEntity<String> response = restTemplate.exchange(partnerValidationUrl, HttpMethod.POST, entity, String.class);

            // Check if the response is successful
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                //Use JsonNode to parse the response
                try {
                    JsonNode jsonNode = new ObjectMapper().readTree(response.getBody());
                    if (!jsonNode.has("token")) {
                        log.error("Token not found in validation response: {}", response.getBody());
                        throw new RuntimeException("Token not found in validation response");
                    }

                    String token = jsonNode.get("token").asText();
                    if (token == null || token.trim().isEmpty()) {
                        log.error("Empty token received in validation response");
                        throw new RuntimeException("Empty token received in validation response");
                    }

                    // Call Rest API to get the Mobile Network config after successful validation
                    Map<String, String> requestBodyForConfig = new HashMap<>();
                    requestBodyForConfig.put("mno", operatorName);
                    String requestJsonForConfig = new Gson().toJson(requestBodyForConfig);

                    // Set the headers for the request to get the Mobile Network config
                    HttpHeaders configHeaders = new HttpHeaders();
                    configHeaders.setContentType(org.springframework.http.MediaType.APPLICATION_JSON);
                    configHeaders.set("Authorization", "Bearer " + token);
                    HttpEntity<String> configEntity = new HttpEntity<>(requestJsonForConfig, configHeaders);

                    ResponseEntity<String> config_response = restTemplate.exchange(networkConfigUrl, HttpMethod.POST, configEntity, String.class);
                    if (config_response.getStatusCode().is2xxSuccessful() && config_response.getBody() != null) {
                        log.debug("Successfully retrieved network configuration for operator: {}", operatorName);
                        JsonNode credentialNode = new ObjectMapper().readTree(config_response.getBody());
                        return credentialNode.get("credential").asText();
                    } else {
                        log.error("Failed to retrieve network configuration. Status: {}, Body: {}",
                                config_response.getStatusCode(), config_response.getBody());
                        throw new RuntimeException("Failed to retrieve network configuration");
                    }
                } catch (JsonProcessingException e) {
                    log.error("Failed to parse validation response: {}", response.getBody(), e);
                    throw new RuntimeException("Failed to parse validation response", e);
                }
            } else {
                log.error("Partner validation failed. Status: {}, Body: {}",
                        response.getStatusCode(), response.getBody());
                throw new RuntimeException("Failed to validate the partner details");
            }
        } catch (Exception e) {
            log.error("Error during API calls for operator: {}", operatorName, e);
            throw new RuntimeException("Error during network configuration retrieval", e);
        }
    }

    /**
     * Validates if a string is valid Base64 format
     * @param str the string to validate
     * @return true if valid Base64, false otherwise
     */
    private boolean isValidBase64(String str) {
        if (str == null || str.trim().isEmpty()) {
            return false;
        }

        try {
            // Base64 strings should only contain A-Z, a-z, 0-9, +, /, and = for padding
            // Length should be multiple of 4 (with padding)
            String cleaned = str.replaceAll("\\s", ""); // Remove whitespace

            if (cleaned.length() % 4 != 0) {
                return false;
            }

            // Check for valid Base64 characters
            if (!cleaned.matches("^[A-Za-z0-9+/]*={0,2}$")) {
                return false;
            }

            // Try to decode to verify it's valid
            Base64.getDecoder().decode(cleaned);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    public MPesaConfig getMpesaConfigAsJson(UserSession session) {
        AtomicReference<MPesaConfig> mpesaConfig = new AtomicReference<>();
        this.mobileMoneyCollectionService.getMobileMoneyCollection("Vodacom").ifPresentOrElse(mobileMoneyCollection -> {
            try {
                String decrypted = encryptedJsonDecryptor.decrypt(mobileMoneyCollection.getCredential());
                ObjectMapper mapper = new ObjectMapper();
                MPesaConfigDTO decryptedConfig = mapper.readValue(decrypted, MPesaConfigDTO.class);
                MPesaConfig config = MPesaConfig.builder()
                        .apiUrl(decryptedConfig.getApiUrl())
                        .callbackUrl(decryptedConfig.getCallbackUrl())
                        .country(decryptedConfig.getCountry())
                        .username(decryptedConfig.getUsername())
                        .password(decryptedConfig.getPassword())
                        .tokenEventId(decryptedConfig.getTokenEventId())
                        .requestEventId(decryptedConfig.getRequestEventId())
                        .businessName(decryptedConfig.getBusinessName())
                        .businessNumber(decryptedConfig.getBusinessNumber())
                        .paybillApiBaseUrl(decryptedConfig.getPaybillApiBaseUrl())
                        .paybillCallbackUrl(decryptedConfig.getPaybillCallbackUrl())
                        .paybillSpId(decryptedConfig.getPaybillSpId())
                        .paybillSpPassword(decryptedConfig.getPaybillSpPassword())
                        .tokenApiUrl(decryptedConfig.getTokenApiUrl())
                        .status(decryptedConfig.getStatus())
                        .build();

                mpesaConfig.set(config);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }

        }, () -> {
            // Get the network config from Rest API if not found in Redis

            try {
                String decrypted = encryptedJsonDecryptor.decrypt(getConfigurations(session, "Vodacom"));
                ObjectMapper mapper = new ObjectMapper();
                MPesaConfigDTO decryptedConfig = mapper.readValue(decrypted, MPesaConfigDTO.class);
                MPesaConfig config = MPesaConfig.builder()
                        .apiUrl(decryptedConfig.getApiUrl())
                        .callbackUrl(decryptedConfig.getCallbackUrl())
                        .country(decryptedConfig.getCountry())
                        .username(decryptedConfig.getUsername())
                        .password(decryptedConfig.getPassword())
                        .tokenEventId(decryptedConfig.getTokenEventId())
                        .requestEventId(decryptedConfig.getRequestEventId())
                        .businessName(decryptedConfig.getBusinessName())
                        .businessNumber(decryptedConfig.getBusinessNumber())
                        .paybillApiBaseUrl(decryptedConfig.getPaybillApiBaseUrl())
                        .paybillCallbackUrl(decryptedConfig.getPaybillCallbackUrl())
                        .paybillSpId(decryptedConfig.getPaybillSpId())
                        .paybillSpPassword(decryptedConfig.getPaybillSpPassword())
                        .tokenApiUrl(decryptedConfig.getTokenApiUrl())
                        .status(decryptedConfig.getStatus())
                        .build();

                mpesaConfig.set(config);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }

        });

        MPesaConfig config = mpesaConfig.get();


        /**
         * Constructor for M-Pesa configuration
         *
         * @param mpesaApiBaseUrl Base API URL for M-Pesa
         * @param mpesaCountry Country code for M-Pesa
         * @param mpesaUsername Username for M-Pesa API
         * @param mpesaPassword Password for M-Pesa API
         * @param mpesaTokenEventId Token event ID for M-Pesa
         * @param mpesaRequestEventId Request event ID for M-Pesa
         * @param mpesaBusinessName Business name for M-Pesa
         * @param mpesaBusinessNumber Business number for M-Pesa
         * @param mpesaCallbackUrl Callback URL for M-Pesa
         * @param mpesaPaybillApiBaseUrl Paybill API base URL
         * @param mpesaPaybillCallbackUrl Paybill callback URL
         * @param mpesaPaybillSpId Paybill service provider ID
         * @param mpesaPaybillSpPassword Paybill service provider password
         */

        return new MobileNetworkConfigService(
                config.getApiUrl(),
                config.getCountry(),
                config.getUsername(),
                config.getPassword(),
                config.getTokenEventId(),
                config.getRequestEventId(),
                config.getBusinessName(),
                config.getBusinessNumber(),
                config.getCallbackUrl(),
                config.getPaybillApiBaseUrl(),
                config.getPaybillCallbackUrl(),
                config.getPaybillSpId(),
                config.getPaybillSpPassword(),
                config.getTokenApiUrl(),
                config.getStatus()
        ).getMPesaConfig();

    }

    public AirtelMoneyConfig getAirtelConfigAsJson(UserSession session) {
        AtomicReference<AirtelMoneyConfig> airtelMoneyConfig = new AtomicReference<>();
        this.mobileMoneyCollectionService.getMobileMoneyCollection("Airtel").ifPresentOrElse(mobileMoneyCollection -> {
            try {
                String decrypted = encryptedJsonDecryptor.decrypt(mobileMoneyCollection.getCredential());
                log.debug("Decrypted Config: {}", decrypted);
                AirtelMoneyConfigDTO decryptedConfig = new Gson().fromJson(decrypted, AirtelMoneyConfigDTO.class);
                log.debug(">>>>>>>>>> Decrypted Config: {}", decryptedConfig.toString());
                AirtelMoneyConfig config = AirtelMoneyConfig.builder()
                        .apiUrl(decryptedConfig.getPush_url())
                        .tokenUrl(decryptedConfig.getAuth_url())
                        .baseUrl("https://api.airtel.com")
                        .country("TZ")
                        .currency("TZS")
                        .clientId(decryptedConfig.getClient_id())
                        .clientSecret(decryptedConfig.getClient_secret())
                        .status(decryptedConfig.getStatus())
                        .build();
                airtelMoneyConfig.set(config);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }

        }, () -> {
            // Get the network config from Rest API if not found in Redis

            try {
                String decrypted = encryptedJsonDecryptor.decrypt(getConfigurations(session, "Airtel"));
                log.debug("Decrypted Config: {}", decrypted);
                AirtelMoneyConfigDTO decryptedConfig = new Gson().fromJson(decrypted, AirtelMoneyConfigDTO.class);
                log.debug(">>>>>>>>>> Decrypted Config: {}", decryptedConfig.toString());
                AirtelMoneyConfig config = AirtelMoneyConfig.builder()
                        .apiUrl(decryptedConfig.getPush_url())
                        .tokenUrl(decryptedConfig.getAuth_url())
                        .baseUrl("https://api.airtel.com")
                        .country("TZ")
                        .currency("TZS")
                        .clientId(decryptedConfig.getClient_id())
                        .clientSecret(decryptedConfig.getClient_secret())
                        .status(decryptedConfig.getStatus())
                        .build();
                airtelMoneyConfig.set(config);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }

        });


        AirtelMoneyConfig config = airtelMoneyConfig.get();

        /**
         * Constructor for Airtel Money configuration
         *
         * @param airtelMoneyApiUrl API URL for Airtel Money
         * @param airtelMoneyTokenUrl Token URL for Airtel Money
         * @param airtelMoneyBaseUrl Base URL for Airtel Money
         * @param airtelMoneyCountry Country code for Airtel Money
         * @param airtelMoneyCurrency Currency code for Airtel Money
         * @param airtelMoneyClientId Client ID for Airtel Money
         * @param airtelMoneyClientSecret Client Secret for Airtel Money
         */

        return new MobileNetworkConfigService(
                config.getApiUrl(),
                config.getTokenUrl(),
                config.getBaseUrl(),
                config.getCountry(),
                config.getCurrency(),
                config.getClientId(),
                config.getClientSecret(),
                config.getStatus()).getAirtelMoneyConfig();


    }

    public MixxByYasConfig getMixxConfigAsJson(UserSession session) {
        AtomicReference<MixxByYasConfig> mixxByYasConfig = new AtomicReference<>();
        this.mobileMoneyCollectionService.getMobileMoneyCollection("Mixx").ifPresentOrElse(mobileMoneyCollection -> {
            try {
                String decrypted = encryptedJsonDecryptor.decrypt(mobileMoneyCollection.getCredential());
                ObjectMapper mapper = new ObjectMapper();
                MixxByYasConfigDTO decryptedConfig = mapper.readValue(decrypted, MixxByYasConfigDTO.class);
                MixxByYasConfig config = MixxByYasConfig.builder()
                        .apiUrl(decryptedConfig.getApiUrl())
                        .callbackUrl(decryptedConfig.getCallbackUrl())
                        .tokenUrl(decryptedConfig.getTokenUrl())
                        .billerMsisdn(decryptedConfig.getBillerMsisdn())
                        .username(decryptedConfig.getUsername())
                        .password(decryptedConfig.getPassword())
                        .tqsApiUrl(decryptedConfig.getTqsApiUrl())
                        .tqsPassword(decryptedConfig.getTqsPassword())
                        .status(decryptedConfig.getStatus())
                        .build();

                mixxByYasConfig.set(config);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }

        }, () -> {
            // Get the network config from Rest API if not found in Redis

            try {
                String decrypted = encryptedJsonDecryptor.decrypt(getConfigurations(session, "Mixx"));
                ObjectMapper mapper = new ObjectMapper();
                MixxByYasConfigDTO decryptedConfig = mapper.readValue(decrypted, MixxByYasConfigDTO.class);
                MixxByYasConfig config = MixxByYasConfig.builder()
                        .apiUrl(decryptedConfig.getApiUrl())
                        .callbackUrl(decryptedConfig.getCallbackUrl())
                        .tokenUrl(decryptedConfig.getTokenUrl())
                        .billerMsisdn(decryptedConfig.getBillerMsisdn())
                        .username(decryptedConfig.getUsername())
                        .password(decryptedConfig.getPassword())
                        .tqsApiUrl(decryptedConfig.getTqsApiUrl())
                        .tqsPassword(decryptedConfig.getTqsPassword())
                        .status(decryptedConfig.getStatus())
                        .build();

                mixxByYasConfig.set(config);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }

        });

        MixxByYasConfig config = mixxByYasConfig.get();

        /**
         * Constructor for MixxByYas configuration
         *
         * @param mixxbyyasApiUrl API URL for MixxByYas
         * @param mixxbyyasTokenUrl Token URL for MixxByYas
         * @param mixxbyyasBillerMsisdn Biller MSISDN for MixxByYas
         * @param mixxbyyasCallbackUrl Callback URL for MixxByYas
         * @param mixxbyyasUsername Username for MixxByYas
         * @param mixxbyyasPassword Password for MixxByYas
         * @param mixxbyyasTqsApiUrl TQS API URL for MixxByYas
         * @param mixxbyyasTqsPassword TQS Password for MixxByYas
         */

        return new MobileNetworkConfigService(
                config.getApiUrl(),
                config.getTokenUrl(),
                config.getBillerMsisdn(),
                config.getCallbackUrl(),
                config.getUsername(),
                config.getPassword(),
                config.getTqsApiUrl(),
                config.getTqsPassword()
                , config.getStatus()
        ).getMixxByYasConfig();


    }

    public HaloPesaConfig getHalopesaigAsJson(UserSession session) {
        AtomicReference<HaloPesaConfig> haloPesaConfig = new AtomicReference<>();
        this.mobileMoneyCollectionService.getMobileMoneyCollection("Halopesa").ifPresentOrElse(mobileMoneyCollection -> {
            try {
                String decrypted = encryptedJsonDecryptor.decrypt(mobileMoneyCollection.getCredential());
                ObjectMapper mapper = new ObjectMapper();
                HaloPesaConfigDTO decryptedConfig = mapper.readValue(decrypted, HaloPesaConfigDTO.class);
                HaloPesaConfig config = HaloPesaConfig.builder()
                        .spId(decryptedConfig.getSpId())
                        .beneficiaryAccountId(decryptedConfig.getBeneficiaryAccountId())
                        .username(decryptedConfig.getUsername())
                        .password(decryptedConfig.getPassword())
                        .businessNumber(decryptedConfig.getBusinessNumber())
                        .secretKey(decryptedConfig.getSecretKey())
                        .merchantCode(decryptedConfig.getMerchantCode())
                        .apiUrl(decryptedConfig.getApiUrl())
                        .tqsUrl(decryptedConfig.getTqsUrl())
                        .status(decryptedConfig.getStatus())
                        .build();
                haloPesaConfig.set(config);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
                },()->{

            // Get the network config from Rest API if not found in Redis

            try {
                String decrypted = encryptedJsonDecryptor.decrypt(getConfigurations(session, "Halopesa"));
                ObjectMapper mapper = new ObjectMapper();
                HaloPesaConfigDTO decryptedConfig = mapper.readValue(decrypted, HaloPesaConfigDTO.class);
                HaloPesaConfig config = HaloPesaConfig.builder()
                        .spId(decryptedConfig.getSpId())
                        .beneficiaryAccountId(decryptedConfig.getBeneficiaryAccountId())
                        .username(decryptedConfig.getUsername())
                        .password(decryptedConfig.getPassword())
                        .businessNumber(decryptedConfig.getBusinessNumber())
                        .secretKey(decryptedConfig.getSecretKey())
                        .merchantCode(decryptedConfig.getMerchantCode())
                        .apiUrl(decryptedConfig.getApiUrl())
                        .tqsUrl(decryptedConfig.getTqsUrl())
                        .status(decryptedConfig.getStatus())
                        .build();

                haloPesaConfig.set(config);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }

        });

        HaloPesaConfig config = haloPesaConfig.get();

        // create HaloPesaConfig constructor
        return new MobileNetworkConfigService(config.getApiUrl(), config.getSpId(), config.getSecretKey(), config.getMerchantCode(), config.getTqsUrl(), config.getStatus(), config.getUsername(), config.getPassword(), config.getBusinessNumber(), config.getBeneficiaryAccountId()).getHaloPesaConfig();


    }


    public MobileNetworkConfig getOperatorConf(String operatorName, UserSession session) {
        return switch (operatorName) {
            case "Mpesa-Tanzania" -> {
                yield getMpesaConfigAsJson(session);
            }
            case "AirtelMoney-Tanzania" -> {
                yield getAirtelConfigAsJson(session);
            }
            case "Mixx_by_yas-Tanzania" -> {
                yield getMixxConfigAsJson(session);
            }
            case "Halopesa-Tanzania" -> {
                yield getHalopesaigAsJson(session);
            }
            default -> throw new CustomExcpts.OperatorNotFoundException("Unsupported operator: " + operatorName);
        };
    }


    public MobileNetworkType getNetworkType(String operatorName, UserSession session) {
        return switch (operatorName) {
            case "Mpesa-Tanzania" -> {
                yield MobileNetworkType.MPESA;
            }
            case "AirtelMoney-Tanzania" -> {
                yield MobileNetworkType.AIRTEL_MONEY;
            }
            case "Mixx_by_yas-Tanzania" -> {
                yield MobileNetworkType.MIXXBYYAS;
            }
            case "Halopesa-Tanzania" -> {
                yield MobileNetworkType.HALOPESA;
            }
            default -> throw new CustomExcpts.OperatorNotFoundException("Unsupported operator: " + operatorName);
        };
    }
}
