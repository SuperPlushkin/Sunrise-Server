package com.sunrise.controller;

import com.sunrise.config.annotation.CurrentUserId;
import com.sunrise.config.annotation.ValidId;
import com.sunrise.core.service.UserService;
import com.sunrise.controller.request.ProfileUpdateRequest;
import com.sunrise.core.service.result.GetProfileResult;
import com.sunrise.core.service.result.SimpleResult;
import com.sunrise.core.service.result.UpdateProfileResult;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Validated
@RestController
@RequestMapping("/profile")
public class ProfileController {
    private final UserService userService;

    public ProfileController(UserService userService){
        this.userService = userService;
    }

    @PostMapping
    public ResponseEntity<?> updateProfile(@RequestBody @Valid ProfileUpdateRequest request, @CurrentUserId long userId) {
        UpdateProfileResult result = userService.updateProfile(
            userId, request.getUsername(), request.getName()
        );

        if (result.isSuccess()) {
            return ResponseEntity.ok(result.getProfile());
        } else {
            return ResponseEntity.badRequest().body(result.getErrorMessage());
        }
    }
    @DeleteMapping
    public ResponseEntity<?> deleteProfile(@CurrentUserId long userId) {
        SimpleResult result = userService.deleteProfile(userId, userId);

        if (result.isSuccess()) {
            return ResponseEntity.ok("Profile successfully deleted");
        } else {
            return ResponseEntity.badRequest().body(result.getErrorMessage());
        }
    }

    @GetMapping
    public ResponseEntity<?> getMyProfile(@CurrentUserId long userId) {
        GetProfileResult result = userService.getMyProfile(userId);

        if (result.isSuccess()) {
            return ResponseEntity.ok(result.getProfile());
        } else {
            return ResponseEntity.badRequest().body(result.getErrorMessage());
        }
    }
    @GetMapping("/{otherUserId}")
    public ResponseEntity<?> getOtherProfile(@PathVariable @ValidId long otherUserId, @CurrentUserId long userId) {
        GetProfileResult result = userService.getOtherProfile(userId, otherUserId);

        if (result.isSuccess()) {
            return ResponseEntity.ok(result.getProfile());
        } else {
            return ResponseEntity.badRequest().body(result.getErrorMessage());
        }
    }
}
