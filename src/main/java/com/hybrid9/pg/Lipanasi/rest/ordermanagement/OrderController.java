package com.hybrid9.pg.Lipanasi.rest.ordermanagement;

import com.hybrid9.pg.Lipanasi.annotationx.RateLimit;
import com.hybrid9.pg.Lipanasi.dto.order.OrderResponse;
import com.hybrid9.pg.Lipanasi.dto.order.OrderStatusDto;
import com.hybrid9.pg.Lipanasi.dto.order.OrderRequestDto;
import com.hybrid9.pg.Lipanasi.dto.order.VendorInfo;
import com.hybrid9.pg.Lipanasi.resources.OrderResource;
import com.hybrid9.pg.Lipanasi.resources.OrderStatusResource;
import com.hybrid9.pg.Lipanasi.resources.excpts.CustomExcpts;
import com.hybrid9.pg.Lipanasi.resources.excpts.OrderErrorHandler;
import com.hybrid9.pg.Lipanasi.serviceImpl.UserManager;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

@Slf4j
//@RestController
//@RequestMapping(value = "/api/v1/order")
public class OrderController {
    private final UserManager userManager;

    /**
     * Create an order and validate the vendorx information
     */
    private final OrderResource orderResource;
    private final OrderStatusResource orderStatusResource;
    private final OrderErrorHandler orderErrorHandler;

    public OrderController(OrderResource orderResource, UserManager userManager,
                           OrderStatusResource orderStatusResource,
                           OrderErrorHandler orderErrorHandler) {
        this.orderResource = orderResource;
        this.userManager = userManager;
        this.orderStatusResource = orderStatusResource;
        this.orderErrorHandler = orderErrorHandler;
    }

    @Autowired
    @Qualifier("ioExecutor")
    private Executor ioExecutor;

    @Autowired
    @Qualifier("cpuExecutor")
    private Executor cpuExecutor;

    @RateLimit(maxRequests = 20, timeWindow = 60, type = RateLimit.RateLimitType.IP)
    @PostMapping(value = "/create", produces = "application/json", consumes = "application/json")
    public CompletableFuture<ResponseEntity<Object>> createOrder(
            @RequestHeader(value = "Authorization", required = false) Optional<String> authorizationHeader,
            @RequestBody OrderRequestDto orderRequestDto,
            HttpServletRequest httpRequest) {

        try {
            // validate order request synchronously first
            this.orderResource.validateOrderRequest(orderRequestDto, authorizationHeader);

            // Then proceed with async operations
            return this.orderResource.checkIfSessionIsValid(authorizationHeader, orderRequestDto)
                    .thenCompose(hasValidSession -> {
                        if (hasValidSession) {
                            // If session is valid, try to create order directly
                            return this.orderResource.validateVendor(orderRequestDto, authorizationHeader)
                                    .thenCompose(vendorInfo -> {
                                        if (vendorInfo != null) {
                                            VendorInfo vendorData = this.orderResource.parseVendorInfo(vendorInfo);
                                            return this.orderResource.createOrder(orderRequestDto, authorizationHeader, httpRequest, vendorData)
                                                    .thenApply(orderResponse ->
                                                            ResponseEntity.ok().body(orderResponse)
                                                    );
                                        }
                                        return CompletableFuture.completedFuture(
                                                ResponseEntity.unprocessableEntity()
                                                        .body(this.orderResource.composeInvalidResponse("Vendor validation failed"))
                                        );
                                    });
                        }

                        // If no active session, validate vendor and create session
                        return this.orderResource.validateVendor(orderRequestDto, authorizationHeader)
                                .thenCompose(vendorInfo -> {
                                    VendorInfo vendorData = this.orderResource.parseVendorInfo(vendorInfo);

                                    if (vendorData.getStatus().equalsIgnoreCase("active")) {
                                        return this.orderResource.createSession(orderRequestDto, authorizationHeader, vendorData)
                                                .thenCompose(__ ->
                                                        this.orderResource.createOrder(orderRequestDto, authorizationHeader, httpRequest, vendorData)
                                                )
                                                .thenApply(orderResponse ->
                                                        ResponseEntity.ok().body(orderResponse)
                                                );
                                    }

                                    return CompletableFuture.completedFuture(
                                            ResponseEntity.unprocessableEntity()
                                                    .body(this.orderResource.composeInvalidResponse("VendorDetails is not active"))
                                    );
                                });
                    })
                    .exceptionally(e -> {
                        Throwable cause = e.getCause() != null ? e.getCause() : e;
                        return orderErrorHandler.handleError(cause);
                    });
        } catch (Exception e) {
            return CompletableFuture.completedFuture(
                    orderErrorHandler.handleError(e)
            );
        }
    }

    @RateLimit(maxRequests = 5, timeWindow = 60, type = RateLimit.RateLimitType.IP)
    @GetMapping(value = "/status/{orderNumber}", produces = MediaType.APPLICATION_JSON_VALUE)
    public CompletableFuture<ResponseEntity<Object>> getOrder(
            @PathVariable("orderNumber") String orderNumber) {

        try {
            // Validate order request synchronously first
            this.orderStatusResource.validateOrderStatus(orderNumber);

            return this.orderStatusResource.checkIfSessionIsValid(orderNumber)
                    .thenCompose(isValid -> {
                        return this.orderStatusResource.getOrder(orderNumber,
                                        isValid)
                                .thenApply(orderResponse -> {
                                    return ResponseEntity.ok().body(orderResponse);
                                });
                    })
                    .exceptionally(e -> {
                        Throwable cause = e.getCause() != null ? e.getCause() : e;
                        return orderErrorHandler.handleError(cause);
                    });

        } catch (Exception e) {
            return CompletableFuture.completedFuture(
                    orderErrorHandler.handleError(e)
            );
        }
    }


}
