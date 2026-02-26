package com.hybrid9.pg.Lipanasi.models.pgmodels.mobilenetworks;

import com.hybrid9.pg.Lipanasi.entities.operators.MnoMapping;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OperatorMapping implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;
    private String operatorId;
    private String operatorName;
    private String operatorPrefix;
    private MnoMapping mnoMapping;
    private String operatorCountryCode;
    private LocalDateTime createdAt;
    private LocalDateTime lastAccessedAt;


    public void updateLastAccessed() {
        this.lastAccessedAt = LocalDateTime.now();
    }
}
