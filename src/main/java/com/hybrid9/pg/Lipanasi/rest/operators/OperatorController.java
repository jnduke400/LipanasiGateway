package com.hybrid9.pg.Lipanasi.rest.operators;

import com.hybrid9.pg.Lipanasi.annotationx.RateLimit;
import com.hybrid9.pg.Lipanasi.dto.operator.OperatorResponseDto;
import com.hybrid9.pg.Lipanasi.dto.orderpayment.PaymentResponse;
import com.hybrid9.pg.Lipanasi.models.pgmodels.mobilenetworks.OperatorMapping;
import com.hybrid9.pg.Lipanasi.resources.ExternalResources;
import com.hybrid9.pg.Lipanasi.resources.excpts.CustomExcpts;
import com.hybrid9.pg.Lipanasi.services.payments.gw.SessionManagementService;
import com.hybrid9.pg.Lipanasi.utilities.PaymentUtilities;
import lombok.AllArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

//@RestController
//@RequestMapping("/api/v1/operator")
public class OperatorController {
    @Autowired
    @Qualifier("ioExecutor")
    private Executor ioExecutor;
    private final ExternalResources externalResources;
    private final PaymentUtilities paymentUtilities;

    public OperatorController(ExternalResources externalResources, PaymentUtilities paymentUtilities) {
        this.externalResources = externalResources;
        this.paymentUtilities = paymentUtilities;
    }

    @RateLimit(maxRequests = 20, timeWindow = 60, type = RateLimit.RateLimitType.IP)
    @GetMapping(value = "/{phoneNumber}", produces = MediaType.APPLICATION_JSON_VALUE)
    public CompletableFuture<ResponseEntity<OperatorResponseDto>> getOperator(
            @PathVariable("phoneNumber") String phoneNumber) {
        try {
            return CompletableFuture.supplyAsync(() -> {
                        this.externalResources.validatePhoneNumber(this.paymentUtilities.formatPhoneNumber("255",phoneNumber));
                        OperatorMapping operatorMapping = externalResources.getOperator(this.paymentUtilities.formatPhoneNumber("255",phoneNumber)).orElseThrow(() -> new CustomExcpts.OperatorNotFoundException("Operator not found for phone number: " + phoneNumber));
                        OperatorResponseDto operatorResponseDto = OperatorResponseDto.builder()
                                .operatorName(operatorMapping.getOperatorName())
                                .operatorPrefix(operatorMapping.getOperatorPrefix())
                                .operatorCountryCode(operatorMapping.getOperatorCountryCode())
                                .operatorPhoneNumber(this.paymentUtilities.formatPhoneNumber("255",phoneNumber))
                                .message("Operator found")
                                .status("SUCCESS")
                                .build();
                        return ResponseEntity.ok(operatorResponseDto);
                    }, ioExecutor)
                    .exceptionally(e -> {
                        Throwable cause = e.getCause();
                        if (cause == null) {
                            cause = e;
                        }


                        if (cause instanceof CustomExcpts.OperatorNotFoundException) {
                            return ResponseEntity.status(404).body(externalResources.prepareOperatorResponse(cause.getMessage(), "FAILED"));
                        } else if (cause instanceof CustomExcpts.PhoneNumberException) {
                            return ResponseEntity.status(400).body(externalResources.prepareOperatorResponse(cause.getMessage(), "FAILED"));

                        } else {
                            OperatorResponseDto operatorResponseDto = OperatorResponseDto.builder()
                                    .message("An error occurred while preparing the mobile operator: " + e.getMessage())
                                    .status("FAILED")
                                    .build();
                            return
                                    ResponseEntity.status(500).body(operatorResponseDto);
                        }
                    });

        } catch (Exception e) {
            OperatorResponseDto operatorResponseDto = OperatorResponseDto.builder()
                    .message("An error occurred while preparing the mobile operator: " + e.getMessage())
                    .status("FAILED")
                    .build();
            return CompletableFuture.completedFuture(
                    ResponseEntity.status(500).body(operatorResponseDto)
            );
        }
    }
}
