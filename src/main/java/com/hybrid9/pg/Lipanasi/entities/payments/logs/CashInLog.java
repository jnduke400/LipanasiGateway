package com.hybrid9.pg.Lipanasi.entities.payments.logs;

import com.hybrid9.pg.Lipanasi.enums.RequestStatus;
import com.hybrid9.pg.Lipanasi.models.Auditable;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "c2b_cash_in_logs")
public class CashInLog extends Auditable<String> {
    @Column(name = "cash_in_request",length = 1000)
    private String cashInRequest;
    private int retryCount;
    private String errorMessage;
    private String paymentReference;
    @Builder.Default
    @Enumerated(EnumType.STRING)
    private RequestStatus status = RequestStatus.INITIATED;
}
