package com.hybrid9.pg.Lipanasi.entities.payments.logs;

import com.hybrid9.pg.Lipanasi.enums.RequestStatus;
import com.hybrid9.pg.Lipanasi.models.Auditable;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.*;

@Entity
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "c2b_cash_out_logs")
public class CashOutLog extends Auditable<String> {
    private String cashOutRequest;
    private int retryCount;
    @Builder.Default
    @Enumerated(EnumType.STRING)
    private RequestStatus status = RequestStatus.INITIATED;
}
