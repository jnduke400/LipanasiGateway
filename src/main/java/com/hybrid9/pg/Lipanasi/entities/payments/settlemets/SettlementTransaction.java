package com.hybrid9.pg.Lipanasi.entities.payments.settlemets;

import com.hybrid9.pg.Lipanasi.models.Auditable;
import com.hybrid9.pg.Lipanasi.entities.payments.activity.Transaction;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.*;

@Entity
@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "c2b_settlement_transactions")
public class SettlementTransaction extends Auditable<String> {
    @ManyToOne
    @JoinColumn(name = "transaction_id")
    private Transaction transaction;
    @ManyToOne
    @JoinColumn(name = "settlement_id")
    private Settlement settlement;
    private float amount;
}
