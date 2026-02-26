package com.hybrid9.pg.Lipanasi.entities.payments.bank;

import com.hybrid9.pg.Lipanasi.models.Auditable;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.*;

@Entity
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "c2b_billing_information", indexes = {@Index(name = "idx_phone", columnList = "phone"), @Index(name = "idx_email", columnList = "email")})
public class BillingInformation extends Auditable<String> {
    private String firstName;
    private String lastName;
    private String address1;
    private String city;            // Corresponds to locality
    private String state;  // Correspond to administrativeArea
    private String postalCode;
    private String country;
    @Column(name = "email")
    private String email;
    @Column(name = "phone")
    private String phone;
}
