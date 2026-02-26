package com.hybrid9.pg.Lipanasi.entities.payments.pushussd;

import com.hybrid9.pg.Lipanasi.models.Auditable;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.*;



@Entity
@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "c2b_push_ussd_callback")
public class PushUssdCallback extends Auditable<String> {
    private String reference;
    private String receipt;
    private float amount;
    private String message;
    private String status;
    @Column(name = "response_message")
    private String responseMessage;
    private String dateTime;
    private String transactionNumber;

}
