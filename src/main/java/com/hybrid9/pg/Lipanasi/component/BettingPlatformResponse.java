package com.hybrid9.pg.Lipanasi.component;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class BettingPlatformResponse {
    private String status;
    private String message;
    private String channel;
}
