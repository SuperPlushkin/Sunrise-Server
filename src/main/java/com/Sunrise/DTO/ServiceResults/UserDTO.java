package com.Sunrise.DTO.ServiceResults;

import com.Sunrise.Entities.User;

@lombok.Getter
@lombok.AllArgsConstructor
@lombok.NoArgsConstructor
public class UserDTO {
    private Long id;
    private String username;
    private String name;

    public UserDTO(User user){
        this.id = user.getId();
        this.name = user.getName();
        this.username = user.getUsername();
    }
}
