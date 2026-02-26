package com.hybrid9.pg.Lipanasi.entities.payments.activity;

import com.hybrid9.pg.Lipanasi.models.Auditable;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "c2b_transaction_details", indexes = {@Index(columnList = "payment_reference,original_reference")})
public class TransactionDetail extends Auditable<String> {
    @ManyToOne
    @JoinColumn(name = "transaction_id")
    private Transaction transaction;
    @Column(name = "original_reference")
    private String originalReference;
    @Column(name = "payment_reference")
    private String paymentReference;
    private String rawRequest;
    private String rawResponse;
}
