package com.hybrid9.pg.Lipanasi.dto.orange;



import java.io.Serializable;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class S2MResponseDto implements Serializable {

    private static final long serialVersionUID = 1L;

    private String partnId;

    private String resultCode;

    private String resultDesc;

    private String systemId;

    private String transId;
/*
    // Custom method to convert Entity to DTO
    public static S2MResponseDto fromEntity(S2MResponse entity) {
        return S2MResponseDto.builder()
                .partnId(entity.getPartnId())
                .resultCode(entity.getResultCode())
                .resultDesc(entity.getResultDesc())
                .systemId(entity.getSystemId())
                .transId(entity.getTransId())
                .build();
    }

    // Custom method to convert DTO to Entity
    public S2MResponse toEntity() {
        S2MResponse entity = new S2MResponse();
        entity.setPartnId(this.partnId);
        entity.setResultCode(this.resultCode);
        entity.setResultDesc(this.resultDesc);
        entity.setSystemId(this.systemId);
        entity.setTransId(this.transId);
        return entity;
    }*/

    // Additional validation method if needed
    public boolean isSuccessful() {
        return "0".equals(this.resultCode);
    }

    // Custom toString method for better logging
    @Override
    public String toString() {
        return "S2MResponseDto{" +
                "partnId='" + partnId + '\'' +
                ", resultCode='" + resultCode + '\'' +
                ", resultDesc='" + resultDesc + '\'' +
                ", systemId='" + systemId + '\'' +
                ", transId='" + transId + '\'' +
                '}';
    }
}
