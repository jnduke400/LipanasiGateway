package com.hybrid9.pg.Lipanasi.component.halotelcollection.response;

import com.gtl.pg.scoop.component.halotelcollection.request.CommonHeaderDTO;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TransactionResultPayloadDTO {
    private CommonHeaderDTO header;
    private TransactionResultBodyDTO body;
}
