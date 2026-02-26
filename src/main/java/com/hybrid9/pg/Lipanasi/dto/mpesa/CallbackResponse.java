package com.hybrid9.pg.Lipanasi.dto.mpesa;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CallbackResponse {
    private String originatorConversationID;
    private String transID;
    private String responseCode;
    private String responseDesc;
}
