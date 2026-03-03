package com.hybrid9.pg.Lipanasi.services.lipanasiconfig;

import com.hybrid9.pg.Lipanasi.dto.lipanasiconfig.MerchantChargesResponse;
import com.hybrid9.pg.Lipanasi.models.pgmodels.vendorx.VendorNetworkCharges;
import com.hybrid9.pg.Lipanasi.services.payments.vendorx.VendorNetworkChargesService;
//import org.jetbrains.annotations.NotNull;
import jakarta.validation.constraints.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

@Service
public class PayScoopApiService {

    private static final Logger logger = LoggerFactory.getLogger(PayScoopApiService.class);

    @Autowired
    private VendorNetworkChargesService vendorNetworkChargesService;

    @Autowired
    private RestTemplate restTemplate;

    @Value("${lipanasi.api.business.base-url:http://75.119.130.98:3032}")
    private String baseUrl;

    public MerchantChargesResponse getMerchantCharges(String mno, String chargeType, String bearerToken) {
        try {
            // Build URL with query parameters
            UriComponentsBuilder uriBuilder = UriComponentsBuilder
                    .fromHttpUrl(baseUrl)
                    .path("/api/merchant-charges")
                    .queryParam("mno", mno)
                    .queryParam("charge_type", chargeType);

            String url = uriBuilder.toUriString();

            // Create headers
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(bearerToken.replace("Bearer ", "")); // Remove "Bearer " if present

            // Create HTTP entity
            HttpEntity<?> entity = new HttpEntity<>(headers);

            logger.info("Making API call to: {}", url);

            // Make the API call
            ResponseEntity<MerchantChargesResponse> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    entity,
                    MerchantChargesResponse.class
            );

            logger.info("API call successful. Status: {}", response.getStatusCode());
            return response.getBody();

        } catch (HttpClientErrorException e) {
            logger.error("Client error occurred: {} - {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new RuntimeException("API call failed with client error: " + e.getMessage());
        } catch (HttpServerErrorException e) {
            logger.error("Server error occurred: {} - {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new RuntimeException("API call failed with server error: " + e.getMessage());
        } catch (Exception e) {
            logger.error("Unexpected error occurred", e);
            throw new RuntimeException("API call failed: " + e.getMessage());
        }
    }

    // Convenience method with default parameters
    public MerchantChargesResponse getVodacomCollectionCharges(String token) {
        MerchantChargesResponse merchantCharges = getMerchantCharges("vodacom", "collection",token);

        // Compose a vendor object from the response
        VendorNetworkCharges vendor = getVendorNetworkCharges(merchantCharges);


        // Store the response in Redis
        this.vendorNetworkChargesService.storeVendor(vendor);

        return merchantCharges;

    }

    // Convenience method with default parameters
    public MerchantChargesResponse getHalopesaCollectionCharges(String token) {
        MerchantChargesResponse merchantCharges = getMerchantCharges("halopesa", "collection",token);

        // Compose a vendor object from the response
        VendorNetworkCharges vendor = getVendorNetworkCharges(merchantCharges);


        // Store the response in Redis
        this.vendorNetworkChargesService.storeVendor(vendor);

        return merchantCharges;
    }

    // Convenience method with default parameters
    public MerchantChargesResponse getMixxCollectionCharges(String token) {
        MerchantChargesResponse merchantCharges = getMerchantCharges("mixx", "collection",token);

        // Compose a vendor object from the response
        VendorNetworkCharges vendor = getVendorNetworkCharges(merchantCharges);


        // Store the response in Redis
        this.vendorNetworkChargesService.storeVendor(vendor);

        return merchantCharges;
    }

    // Convenience method with default parameters
    public MerchantChargesResponse getAirtelCollectionCharges(String token) {
        MerchantChargesResponse merchantCharges = getMerchantCharges("airtel", "collection",token);

        // Compose a vendor object from the response
        VendorNetworkCharges vendor = getVendorNetworkCharges(merchantCharges);


        // Store the response in Redis
        this.vendorNetworkChargesService.storeVendor(vendor);

        return merchantCharges;
    }

    // Convenience method with default parameters
    public MerchantChargesResponse getcrdbCollectionCharges(String token) {
        MerchantChargesResponse merchantCharges = getMerchantCharges("CRDB", "collection",token);

        // Compose a vendor object from the response
        VendorNetworkCharges vendor = getVendorNetworkCharges(merchantCharges);


        // Store the response in Redis
        this.vendorNetworkChargesService.storeVendor(vendor);

        return merchantCharges;
    }

    @NotNull
    private static VendorNetworkCharges getVendorNetworkCharges(MerchantChargesResponse merchantCharges) {
        // Create a Vendor object from the response
        VendorNetworkCharges vendor = new VendorNetworkCharges();
        vendor.setIsActive(merchantCharges.getData().get(0).getIsActive());
        vendor.setMno(merchantCharges.getData().get(0).getMno());
        vendor.setMerchantId(merchantCharges.getData().get(0).getMerchantId());
        vendor.setRate(merchantCharges.getData().get(0).getRate());
        vendor.setChargeType(merchantCharges.getData().get(0).getChargeType());
        vendor.setId(merchantCharges.getData().get(0).getId());
        return vendor;
    }
}
