package com.Sunrise.Controllers;

import com.Sunrise.Configurations.Annotations.CurrentUserId;
import com.Sunrise.Configurations.Annotations.ValidId;
import com.Sunrise.Core.Services.UserService;
import com.Sunrise.DTOs.Requests.ProfileUpdateRequest;
import com.Sunrise.DTOs.ServiceResults.GetProfileResult;
import com.Sunrise.DTOs.ServiceResults.UpdateProfileResult;
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

    @GetMapping
    public ResponseEntity<?> getMyProfile(@CurrentUserId long userId) {
        GetProfileResult result = userService.getMyProfile(userId);

        if (result.isSuccess()) {
            return ResponseEntity.ok(result.getProfile());
        } else {
            return ResponseEntity.status(404).body(result.getErrorMessage());
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
