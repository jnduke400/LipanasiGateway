package com.hybrid9.pg.Lipanasi.entities.vendorx;

import com.hybrid9.pg.Lipanasi.models.Auditable;
import jakarta.persistence.Entity;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.*;

@Entity
@Getter
@Setter
@Builder
@Table(name = "c2b_addresses")
@AllArgsConstructor
@NoArgsConstructor
public class Address extends Auditable<String> {
    private String street;
    private String city;
    private String state;
    private String zip;
    private String country;
    private String houseNumber;
    @OneToOne(mappedBy = "address")
    private VendorDetails vendorDetails;


}
