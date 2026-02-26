package com.hybrid9.pg.Lipanasi.entities.order;

import com.hybrid9.pg.Lipanasi.convertorx.MapToJsonConverter;
import com.hybrid9.pg.Lipanasi.entities.vendorx.Customer;
import com.hybrid9.pg.Lipanasi.enums.OrderStatus;
import com.hybrid9.pg.Lipanasi.models.Auditable;
import com.hybrid9.pg.Lipanasi.entities.payments.pymtmethods.PaymentMethod;
import jakarta.persistence.*;
import lombok.*;

import java.util.Map;

@Entity
@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "c2b_orders", indexes = {@Index(name = "idx_order", columnList = "order_number"),@Index(name = "index_2_order",columnList = "partner_id"),@Index(name = "idx_3_order", columnList = "order_token"),@Index(name = "idx_4_order", columnList = "customer_id"),@Index(name = "idx_5_order", columnList = "payment_session_id"),@Index(name = "idx_6_order", columnList = "pay_bill_reference")})
public class Order extends Auditable<String> {
    @Column(name = "order_number")
    private String orderNumber;
    private String orderItem;
    @Column(name = "partner_id")
    private String partnerId;
    private String payBillReference;
    private float amount;
    private String currency;
    private String receipt;
    private String cardToken;
    private String deviceInfo;
    private String ipAddress;
    @Column(columnDefinition = "json")
    @Convert(converter = MapToJsonConverter.class)
    private Map<String, Object> metadata;
    @Column(name= "order_token")
    private String orderToken;
    private String description;
    private String signature;
    @Column(name = "payment_session_id")
    private String paymentSessionId;
    @ManyToOne
    @JoinColumn(name = "payment_method_id")
    private PaymentMethod paymentMethod;
    private String channel;
    @Builder.Default
    @Enumerated(EnumType.STRING)
    private OrderStatus status = OrderStatus.PENDING;
    @ManyToOne
    @JoinColumn(name = "customer_id")
    private Customer customer;
}
