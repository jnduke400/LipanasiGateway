package com.hybrid9.pg.Lipanasi.entities;

import com.hybrid9.pg.Lipanasi.models.BaseAudit;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.*;

@Entity
@Table(name = "c2b_api", indexes = @Index(columnList = "type"))
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Api extends BaseAudit<String> {
    @Column(name = "type")
    private String type;
    @Column(name = "url")
    private String url;
    @Column(name = "header_key")
    private String headerKey;
    @Column(name = "header_value")
    private String headerValue;
}
