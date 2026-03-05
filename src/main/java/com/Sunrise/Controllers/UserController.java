package com.Sunrise.Controllers;

import com.Sunrise.Controllers.Annotations.CurrentUserId;
import com.Sunrise.DTO.Requests.FilteredUsersRequest;
import com.Sunrise.Services.UserService;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService){
        this.userService = userService;
    }

    @GetMapping
    public ResponseEntity<?> getManyUsers(@ModelAttribute @Valid FilteredUsersRequest request, @CurrentUserId Long userId) {

        var result = userService.getFilteredUsers(userId, request.getFilter(), request.getOffset(), request.getLimit());

        if (result.isSuccess()) {
            return ResponseEntity.ok(result.getPage());
        }
        else {
            return ResponseEntity.badRequest().body(result.getErrorMessage());
        }
    }
}
