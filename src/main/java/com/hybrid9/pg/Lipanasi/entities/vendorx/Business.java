package com.hybrid9.pg.Lipanasi.entities.vendorx;

import com.hybrid9.pg.Lipanasi.models.Auditable;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.*;

@Entity
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "c2b_business")
public class Business extends Auditable<String> {
    private String name;
    @Column(name = "business_type")
    private String businessType;
    private String registrationNumber;
}
