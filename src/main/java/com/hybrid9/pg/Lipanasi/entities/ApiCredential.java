package com.hybrid9.pg.Lipanasi.entities;

import com.hybrid9.pg.Lipanasi.models.BaseAudit;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.*;

@Entity
@Table(name = "c2b_api_credentials", indexes = @Index(columnList = "type"))
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ApiCredential extends BaseAudit<String> {
    private String appName;
    @Column(name = "type")
    private String type;
    @Column(name = "url")
    private String url;
    @Column(name = "user_key")
    private String userKey;
    @Column(name = "pass_key")
    private String passKey;
    @Column(name = "username")
    private String username;
    @Column(name = "password")
    private String password;
}
