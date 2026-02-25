package com.Sunrise.Controllers;

import com.Sunrise.Controllers.Annotations.CurrentUserId;
import com.Sunrise.DTO.Requests.FilteredUsersRequest;
import com.Sunrise.DTO.Responses.UserDTO;
import com.Sunrise.Services.UserService;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/user")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService){
        this.userService = userService;
    }

    @GetMapping("/getmany")
    public ResponseEntity<?> getManyUsers(@Valid @ModelAttribute FilteredUsersRequest request, @CurrentUserId Long userId) {

        var result = userService.getFilteredUsers(userId, request.getFilter(), request.getOffset(), request.getLimited());

        if (result.isSuccess()) {
            List<UserDTO> users = result.getUsers();
            return ResponseEntity.ok(Map.of(
                "users", users,
                "count", users.size()
            ));
        }
        else {
            return ResponseEntity.badRequest().body(result.getErrorMessage());
        }
    }
}
