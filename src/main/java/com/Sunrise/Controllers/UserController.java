package com.Sunrise.Controllers;

import com.Sunrise.Configurations.Annotations.CurrentUserId;
import com.Sunrise.Core.Services.UserService;

import com.Sunrise.DTOs.Requests.PaginationRequest;
import com.Sunrise.DTOs.ServiceResults.FilteredUsersResult;
import jakarta.validation.Valid;

import jakarta.validation.constraints.NotNull;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Validated
@RestController
@RequestMapping("/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService){
        this.userService = userService;
    }

    @GetMapping
    public ResponseEntity<?> getUsers(@RequestParam @NotNull String filter, @Valid PaginationRequest pagination, @CurrentUserId long userId) {

        FilteredUsersResult result = userService.getFilteredUsers(userId, filter, pagination.getCursor(), pagination.getLimit());

        if (result.isSuccess()) {
            return ResponseEntity.ok(result.getPagination());
        }
        else {
            return ResponseEntity.badRequest().body(result.getErrorMessage());
        }
    }
}
