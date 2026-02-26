package com.hybrid9.pg.Lipanasi.component;

import com.hybrid9.pg.Lipanasi.entities.payments.bank.CardPayment;
import com.hybrid9.pg.Lipanasi.entities.vendorx.MainAccount;
import com.hybrid9.pg.Lipanasi.entities.vendorx.SubAccount;
import com.hybrid9.pg.Lipanasi.entities.payments.paybill.PayBillPayment;
import com.hybrid9.pg.Lipanasi.entities.payments.pushussd.PushUssd;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BalanceLedger {
    private MainAccount mainAccount;
    private PushUssd pushUssd;
    private CardPayment cardPayment;
    private PayBillPayment payBillPayment;
    private SubAccount subAccount;

    private float commission;
    private float vat;
    private float netAmount;


}
