package com.hybrid9.pg.Lipanasi.dto.auth;

import com.hybrid9.pg.Lipanasi.entities.AppUser;
import lombok.Builder;
import lombok.Data;


@Builder
@Data
public class UserDTO {
    private Long id;
    private String username;

    public static UserDTO from(AppUser user) {
        return builder()
                .id(user.getId())
                .username(user.getUsername())
                .build();
    }
}
