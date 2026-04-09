package com.sunrise.controller;

import com.sunrise.config.annotation.CurrentUserId;
import com.sunrise.controller.request.*;
import com.sunrise.core.service.result.*;
import com.sunrise.core.service.ChatService;
import com.sunrise.config.annotation.ValidId;

import com.sunrise.entity.dto.ChatMembersPageDTO;
import com.sunrise.entity.dto.UserChatsPageDTO;
import jakarta.validation.Valid;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/chats")
public class ChatController {

    private final ChatService chatService;

    @PostMapping("/create-personal")
    public ResponseEntity<?> createPersonalChat(@RequestBody @Valid CreatePersonalChatRequest request, @CurrentUserId long userId) {

        ResultOneArg<Long> result = chatService.createPersonalChat(userId, request.getOtherUserId());

        if (result.isSuccess()) {
            return ResponseEntity.ok(result.getResult());
        } else {
            return ResponseEntity.badRequest().body(result.getError());
        }
    }

    @PostMapping("/create-group")
    public ResponseEntity<?> createGroupChat(@RequestBody @Valid CreateGroupChatRequest request, @CurrentUserId long userId) {

        ResultOneArg<Long> result = chatService.createGroupChat(
            userId,
            request.getChatName().trim(),
            request.getMembers()
        );

        if (result.isSuccess()) {
            return ResponseEntity.ok(result.getResult());
        } else {
            return ResponseEntity.badRequest().body(result.getError());
        }
    }

    @PostMapping("/{chatId}/add-member")
    public ResponseEntity<?> addGroupMember(@PathVariable @ValidId long chatId, @RequestBody @Valid AddGroupMemberRequest request, @CurrentUserId long userId) {

        ResultNoArgs result = chatService.addGroupMember(chatId, userId, request.getNewUserId());

        if (result.isSuccess()) {
            return ResponseEntity.ok(result.getOperationText());
        } else {
            return ResponseEntity.badRequest().body(result.getError());
        }
    }

    @PostMapping("/{chatId}/add-members")
    public ResponseEntity<?> addGroupMembers(@PathVariable @ValidId long chatId, @RequestBody @Valid AddGroupMembersRequest request, @CurrentUserId long userId) {

        ResultNoArgs result = chatService.addGroupMembers(chatId, userId, request.getMembers());

        if (result.isSuccess()) {
            return ResponseEntity.ok(result.getOperationText());
        } else {
            return ResponseEntity.badRequest().body(result.getError());
        }
    }

    @PostMapping("/{chatId}/admin-rights/{otherUserId}")
    public ResponseEntity<?> updateAdminRights(@PathVariable @ValidId long chatId, @PathVariable @ValidId long otherUserId, @RequestBody @Valid UpdateAdminRightsRequest request, @CurrentUserId long userId) {
        ResultNoArgs result = chatService.updateAdminRights(
            chatId, userId, otherUserId, request.getIsAdmin()
        );

        if (result.isSuccess()) {
            return ResponseEntity.ok(result.getOperationText());
        } else {
            return ResponseEntity.badRequest().body(result.getError());
        }
    }

    @PostMapping("/{chatId}/leave")
    public ResponseEntity<?> leaveChat(@PathVariable @ValidId long chatId, @CurrentUserId long userId) {

        ResultNoArgs result = chatService.leaveChat(chatId, userId);

        if (result.isSuccess()) {
            return ResponseEntity.ok(result.getOperationText());
        } else {
            return ResponseEntity.badRequest().body(result.getError());
        }
    }

    @DeleteMapping("/{chatId}")
    public ResponseEntity<?> deleteChat(@PathVariable @ValidId long chatId, @CurrentUserId long userId) {
        ResultNoArgs result = chatService.deleteChat(chatId, userId);

        if (result.isSuccess()) {
            return ResponseEntity.ok(result.getOperationText());
        } else {
            return ResponseEntity.badRequest().body(result.getError());
        }
    }

    @GetMapping("/{chatId}/stats")
    public ResponseEntity<?> getChatStats(@PathVariable @ValidId long chatId, @CurrentUserId long userId) {

        ResultOneArg<ChatStatsResult> result = chatService.getChatStats(chatId, userId);

        if (result.isSuccess()) {
            return ResponseEntity.ok(result.getResult());
        } else {
            return ResponseEntity.badRequest().body(result.getError());
        }
    }

    @GetMapping("/{chatId}/members")
    public ResponseEntity<?> getChatMembersPage(@PathVariable @ValidId long chatId, @Valid PaginationRequest request, @CurrentUserId long userId) {

        ResultOneArg<ChatMembersPageDTO> result = chatService.getChatMembersPage(chatId, userId, request.getCursor(), request.getLimit());

        if (result.isSuccess()) {
            return ResponseEntity.ok(result.getResult());
        } else {
            return ResponseEntity.badRequest().body(result.getError());
        }
    }

    @GetMapping
    public ResponseEntity<?> getUserChatsPage(@Valid PaginationRequest request, @CurrentUserId long userId) {

        ResultOneArg<UserChatsPageDTO> result = chatService.getUserChatsPage(userId, request.getCursor(), request.getLimit());

        if (result.isSuccess()) {
            return ResponseEntity.ok(result.getResult());
        } else {
            return ResponseEntity.badRequest().body(result.getError());
        }
    }
}