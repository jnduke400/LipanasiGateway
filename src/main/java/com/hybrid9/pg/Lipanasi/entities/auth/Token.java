package com.hybrid9.pg.Lipanasi.entities.auth;





import com.hybrid9.pg.Lipanasi.models.BaseAudit;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "granted_tokens")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class Token extends BaseAudit<String> {
    @Column(name = "user_id" ,unique = true)
    private String userId;
    @Column(length = 500)
    private String accessToken;
    @Column(length = 500)
    private String refreshToken;
}
