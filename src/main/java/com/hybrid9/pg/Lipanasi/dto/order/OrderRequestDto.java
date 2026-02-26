package com.hybrid9.pg.Lipanasi.dto.order;

import com.hybrid9.pg.Lipanasi.dto.customer.CustomerDto;
import com.hybrid9.pg.Lipanasi.entities.vendorx.Customer;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class OrderRequestDto {
    private int amount;
    private String currency;
    private String receipt;
    //private String paymentMethod;
    //private String paymentChannel;
    //private String email;
    private Map<String,Object> metadata;
    private CustomerDto customers;
}
