package com.hybrid9.pg.Lipanasi.entities.payments.pushussd;


import com.hybrid9.pg.Lipanasi.models.Auditable;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.*;

@Entity
@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "c2b_push_ussd_ref", indexes = @Index(name = "push_ussd_ref_index", columnList = "map_reference"))
public class PushUssdRef extends Auditable<String> {
    private String reference;
    @Column(name = "map_reference",unique = true)
    private String mapReference;
    private String collectionRef;
}
