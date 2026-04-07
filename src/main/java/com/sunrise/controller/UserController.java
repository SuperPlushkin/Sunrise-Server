package com.sunrise.controller;

import com.sunrise.config.annotation.CurrentUserId;
import com.sunrise.core.service.UserService;

import com.sunrise.controller.request.PaginationRequest;
import com.sunrise.core.service.result.FilteredUsersResult;
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
    public ResponseEntity<?> getActiveUsersPage(@RequestParam(defaultValue = "") @NotNull String filter, @Valid PaginationRequest pagination, @CurrentUserId long userId) {

        FilteredUsersResult result = userService.getActiveUsersPage(userId, filter, pagination.getCursor(), pagination.getLimit());

        if (result.isSuccess()) {
            return ResponseEntity.ok(result.getPagination());
        }
        else {
            return ResponseEntity.badRequest().body(result.getErrorMessage());
        }
    }
}
