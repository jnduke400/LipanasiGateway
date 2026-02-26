package com.hybrid9.pg.Lipanasi.entities.operators;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.hybrid9.pg.Lipanasi.models.Auditable;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "c2b_mno_prefixies", indexes = @Index(name = "mno_prefix_index", columnList = "prefix"))
public class MnoPrefix extends Auditable<String> {
    private String prefix;
    @Column(name = "dial_code")
    private String dialCode;
    @Column(name = "country_name")
    private String countryName;
    @JsonProperty(access = JsonProperty.Access.READ_WRITE)
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "mno_mapping_id")
    private MnoMapping mnoMapping;

}
