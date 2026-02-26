package com.hybrid9.pg.Lipanasi.dto.mpesa.congo;

import lombok.Data;

import java.util.List;
@Data
public class CallbackRequest {
    private List<DataItem> request;
}
