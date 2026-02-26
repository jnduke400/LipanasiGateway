package com.hybrid9.pg.Lipanasi.dto.airtelmoney;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class StatusDto {
    private String code;
    private String message;
    private String result_code;
    private String response_code;
    private Boolean success;
}
