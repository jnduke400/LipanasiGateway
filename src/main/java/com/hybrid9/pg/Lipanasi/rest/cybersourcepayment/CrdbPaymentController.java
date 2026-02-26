package com.hybrid9.pg.Lipanasi.rest.cybersourcepayment;


import com.hybrid9.pg.Lipanasi.dto.crdb.cybersource.CaptureContextRequest;
import com.hybrid9.pg.Lipanasi.dto.crdb.cybersource.PaymentRequest;
import com.hybrid9.pg.Lipanasi.dto.crdb.cybersource.PaymentResponse;
import com.hybrid9.pg.Lipanasi.resources.excpts.PaymentException;
import com.hybrid9.pg.Lipanasi.services.payments.crdb.cybersorce.CyberSourceMicroformService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;


//@RestController
//@RequestMapping("/api/v1/crdb/payment")
@Validated
public class CrdbPaymentController {

    private static final Logger logger = LoggerFactory.getLogger(CrdbPaymentController.class);

    @Autowired
    private CyberSourceMicroformService paymentService;

    @PostMapping("/capture-context")
    public ResponseEntity<?> generateCaptureContext(@RequestBody @Valid CaptureContextRequest request) {
        try {
            String captureContext = paymentService.generateCaptureContext(request);
            Map<String, String> response = new HashMap<>();
            response.put("captureContext", captureContext);
            return ResponseEntity.ok(response);
        } catch (PaymentException e) {
            logger.error("Failed to generate capture context", e);
            return ResponseEntity.badRequest()
                    .body(Collections.singletonMap("error", e.getMessage()));
        }
    }

    @PostMapping("/process")
    public ResponseEntity<?> processPayment(@RequestBody @Valid PaymentRequest request) {
        try {
            PaymentResponse response = paymentService.processPayment(request);
            return ResponseEntity.ok(response);
        } catch (PaymentException e) {
            logger.error("Payment processing failed", e);
            return ResponseEntity.badRequest()
                    .body(Collections.singletonMap("error", e.getMessage()));
        }
    }
}
