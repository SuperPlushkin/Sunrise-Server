package com.sunrise.controller;

import com.sunrise.config.annotation.CurrentUserId;
import com.sunrise.config.annotation.ValidId;
import com.sunrise.core.service.UserService;
import com.sunrise.controller.request.ProfileUpdateRequest;
import com.sunrise.core.service.result.*;
import com.sunrise.entity.dto.UserProfileDTO;

import jakarta.validation.Valid;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/profile")
public class ProfileController {
    private final UserService userService;

    @PostMapping
    public ResponseEntity<?> updateProfile(@RequestBody @Valid ProfileUpdateRequest request, @CurrentUserId long userId) {
        ResultNoArgs result = userService.updateProfile(userId, request.getUsername(), request.getName());

        if (result.isSuccess()) {
            return ResponseEntity.ok(result.getOperationText());
        } else {
            return ResponseEntity.badRequest().body(result.getError());
        }
    }
    @DeleteMapping
    public ResponseEntity<?> deleteProfile(@CurrentUserId long userId) {
        ResultNoArgs result = userService.deleteUser(userId, userId);

        if (result.isSuccess()) {
            return ResponseEntity.ok(result.getOperationText());
        } else {
            return ResponseEntity.badRequest().body(result.getError());
        }
    }

    @GetMapping
    public ResponseEntity<?> getMyProfile(@CurrentUserId long userId) {
        ResultOneArg<UserProfileDTO> result = userService.getMyProfile(userId);

        if (result.isSuccess()) {
            return ResponseEntity.ok(result.getResult());
        } else {
            return ResponseEntity.badRequest().body(result.getError());
        }
    }

    @GetMapping("/{otherUserId}")
    public ResponseEntity<?> getOtherProfile(@PathVariable @ValidId long otherUserId, @CurrentUserId long userId) {
        ResultOneArg<UserProfileDTO> result = userService.getOtherProfile(userId, otherUserId);

        if (result.isSuccess()) {
            return ResponseEntity.ok(result.getResult());
        } else {
            return ResponseEntity.badRequest().body(result.getError());
        }
    }
}
