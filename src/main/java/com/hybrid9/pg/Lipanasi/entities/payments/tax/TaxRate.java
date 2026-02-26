package com.hybrid9.pg.Lipanasi.entities.payments.tax;

import com.hybrid9.pg.Lipanasi.enums.TaxType;
import com.hybrid9.pg.Lipanasi.models.Auditable;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.*;

@Entity
@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "c2b_tax_rates")
public class TaxRate extends Auditable<String> {
    private String taxCode;
    private String taxName;
    private double rate;
    @Builder.Default
    @Enumerated(EnumType.STRING)
    private TaxType taxType = TaxType.VAT;
    private String state;
    private String country;
    private String effectiveFrom;
    private String effectiveTo;
    private String status;
}
