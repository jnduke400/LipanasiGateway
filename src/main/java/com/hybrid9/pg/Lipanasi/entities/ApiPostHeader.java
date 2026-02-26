package com.hybrid9.pg.Lipanasi.entities;

import com.hybrid9.pg.Lipanasi.models.BaseAudit;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "c2b_api_post_headers", indexes = @Index(columnList = "api_credential_id"))
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ApiPostHeader extends BaseAudit<String> {
    @ManyToOne
    @JoinColumn(name = "api_credential_id")
    private ApiCredential apiCredential;
    @Column(name = "header_key")
    private String headerKey;
    @Column(name = "header_value")
    private String headerValue;
}
