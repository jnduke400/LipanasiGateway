package com.hybrid9.pg.Lipanasi.models.pgmodels.mobilenetworks;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public abstract class MobileNetworkConfig implements Serializable {
    @Serial
    private static final long serialVersionUID = 1824351L;
    private String apiUrl;
    private String callbackUrl;
    private String status;

    private LocalDateTime createdAt;
    private LocalDateTime lastAccessedAt;


    public void updateLastAccessed() {
        this.lastAccessedAt = LocalDateTime.now();
    }
}
