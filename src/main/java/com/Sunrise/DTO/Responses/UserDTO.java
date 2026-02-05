package com.Sunrise.DTO.Responses;

import com.Sunrise.DTO.DBResults.ChatStatsDBResult;
import com.Sunrise.Entities.DB.User;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class UserDTO {
    public final Long id;
    public final String username;
    public final String name;
    public UserDTO(ChatStatsDBResult.GetUserResult userResult){
        this.id = userResult.getId();
        this.username = userResult.getUsername();
        this.name = userResult.getName();
    }
    public UserDTO(User user){
        this.id = user.getId();
        this.username = user.getUsername();
        this.name = user.getName();
    }
}
