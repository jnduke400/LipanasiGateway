package com.hybrid9.pg.Lipanasi.dto.airtelmoney;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ResponseDto {
    private DataDto data;
    private StatusDto status;
}
