package com.hybrid9.pg.Lipanasi.entities.operators;



import com.hybrid9.pg.Lipanasi.models.Auditable;
import com.hybrid9.pg.Lipanasi.entities.payments.pymtmethods.PaymentMethod;
import jakarta.persistence.*;
import lombok.*;
import org.antlr.v4.runtime.misc.NotNull;

;


@Entity
@Table(name = "c2b_mno_mapping", indexes = {@Index(name = "mapping_index", columnList = "username,is_routed")})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MnoMapping extends Auditable<String> {
    @NotNull
    @Column(nullable = false)
    private String mno;
    @NotNull
    @Column(nullable = false)
    private String ip;
    @NotNull
    @Column(nullable = false)
    private String port;
    @NotNull
    @Column(nullable = false)
    private String username;
    @NotNull
    @Column(nullable = false)
    private String password;
    private String prefix;
    @Builder.Default
    private Integer tps = 2;
    @Column(name = "mno_status",columnDefinition = "varchar(10) DEFAULT 'Active'",insertable = false)
    private String status;
    @Column(name = "route_to")
    private String routedTo;
    @Column(name = "is_routed",columnDefinition = "varchar(10) DEFAULT 'false'",insertable = false)
    private String isRouted;
    @ManyToOne
    @JoinColumn(name = "payment_method_id")
    private PaymentMethod paymentMethod;

}
