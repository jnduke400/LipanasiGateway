package com.hybrid9.pg.Lipanasi.entities.order;

import com.hybrid9.pg.Lipanasi.enums.OrderSessionStatus;
import com.hybrid9.pg.Lipanasi.models.Auditable;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "c2b_order_sessions",indexes = {@Index(name = "idx_order_session",columnList = "partner_id,session_id,order_email")})
public class OrderSession extends Auditable<String> {
    @Column(name = "partner_id")
    private String partnerId;
    @Column(name = "session_id")
    private String sessionId;
    private String credential;
    private String expiryDate;
    private String duration;
    @Column(name = "order_email")
    private String orderEmail;
    @Builder.Default
    @Enumerated(EnumType.STRING)
    private OrderSessionStatus status = OrderSessionStatus.ACTIVE;

}
