package com.hybrid9.pg.Lipanasi.component;

import com.hybrid9.pg.Lipanasi.dto.airtelmoney.AirtelMoneyCallbackResponse;
import com.hybrid9.pg.Lipanasi.dto.airtelmoney.tz.AirtelMoneyTzCallbackResponse;
import com.hybrid9.pg.Lipanasi.dto.halopesa.HalopesaCallbackResponse;
import com.hybrid9.pg.Lipanasi.dto.mixxbyyas.CallbackRequest;
import com.hybrid9.pg.Lipanasi.dto.mpesa.USSDCallback;
import com.hybrid9.pg.Lipanasi.dto.mpesa.congo.MpesaCongoCallback;
import com.hybrid9.pg.Lipanasi.dto.orange.callback.OrangeCallbackRequest;
import com.hybrid9.pg.Lipanasi.entities.vendorx.VendorDetails;
import com.hybrid9.pg.Lipanasi.entities.payments.pushussd.PushUssd;
import com.nimbusds.jose.shaded.gson.JsonObject;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class GeneralCallbackResponse {
    private String reference;
    private JsonObject jsonObject;
    private PushUssd pushUssd;
    private VendorDetails vendorDetails;
    private USSDCallback ussdCallback;
    private CallbackRequest callbackRequest;
    private AirtelMoneyCallbackResponse airtelMoneyCallbackResponse;
    private AirtelMoneyTzCallbackResponse airtelMoneyTzCallbackResponse;
    private OrangeCallbackRequest.DoCallback orangeCallback;
    private MpesaCongoCallback mpesaCongoCallback;
    private HalopesaCallbackResponse halopesaCallbackResponse;
}
