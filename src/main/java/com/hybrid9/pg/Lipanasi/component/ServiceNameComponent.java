package com.hybrid9.pg.Lipanasi.component;

import com.hybrid9.pg.Lipanasi.enums.ServiceName;
import lombok.Getter;
import lombok.Setter;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
public class ServiceNameComponent {
    private ServiceName serviceName;
}
