package com.hybrid9.pg.Lipanasi.entities.vendorx;

import com.hybrid9.pg.Lipanasi.models.Auditable;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "c2b_customers",indexes = {@Index(name="idx_customers_table", columnList = "email,phone_number,vendor_id")},uniqueConstraints = {@UniqueConstraint(columnNames = "email,phone_number,vendor_id")})
public class Customer extends Auditable<String> {
    private String firstName;
    private String lastName;
    @Column(name = "phone_number")
    private String phoneNumber;
    private String email;
    @ManyToOne
    @JoinColumn(name = "vendor_id")
    private VendorDetails vendorDetails;

}
