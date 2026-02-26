package com.hybrid9.pg.Lipanasi.entities.banks;

import com.hybrid9.pg.Lipanasi.models.Auditable;
import com.hybrid9.pg.Lipanasi.entities.payments.pymtmethods.PaymentMethod;
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
@Table(name = "c2b_banks")
public class Bank extends Auditable<String> {
    private String bankName;
    private String bankCode;
    private String bankBranch;
    private String bankBranchCode;
    private String bankAccountNumber;
    private String bankAccountName;
    private String bankAccountType;
    private String bankAccountStatus;
    @ManyToOne
    @JoinColumn(name = "payment_method_id")
    private PaymentMethod paymentMethod;

}
