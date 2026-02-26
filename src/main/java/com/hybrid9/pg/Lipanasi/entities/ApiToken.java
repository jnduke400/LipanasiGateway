package com.hybrid9.pg.Lipanasi.entities;


import com.hybrid9.pg.Lipanasi.models.BaseAudit;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "c2b_api_token")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ApiToken extends BaseAudit<String> {
    @Column(name = "type")
    private String type;
    @Column(name = "header_key")
    private String headerKey;
    @Column(name = "token", columnDefinition = "longtext")
    private String token;

}
