package com.sunrise.controller;

import com.sunrise.config.annotation.CurrentUserId;
import com.sunrise.controller.request.*;
import com.sunrise.core.service.result.*;
import com.sunrise.core.service.ChatService;
import com.sunrise.config.annotation.ValidId;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Validated
@RestController
@RequestMapping("/chats")
public class ChatController {

    private final ChatService chatService;

    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    @PostMapping("/create-personal")
    public ResponseEntity<?> createPersonalChat(@RequestBody @Valid CreatePersonalChatRequest request, @CurrentUserId long userId) {

        ChatCreationResult result = chatService.createPersonalChat(userId, request.getOtherUserId());

        if (result.isSuccess()) {
            return ResponseEntity.ok(Map.of(
                "chat_id", result.getChatId()
            ));
        }
        else {
            return ResponseEntity.badRequest().body(result.getErrorMessage());
        }
    }

    @PostMapping("/create-group")
    public ResponseEntity<?> createGroupChat(@RequestBody @Valid CreateGroupChatRequest request, @CurrentUserId long userId) {

        ChatCreationResult result = chatService.createGroupChat(
            userId,
            request.getChatName().trim(),
            request.getMembers()
        );

        if (result.isSuccess()) {
            return ResponseEntity.ok(Map.of(
                "chat_id", result.getChatId()
            ));
        } else {
            return ResponseEntity.badRequest().body(result.getErrorMessage());
        }
    }

    @PostMapping("/{chatId}/add-member")
    public ResponseEntity<?> addGroupMember(@PathVariable @ValidId long chatId, @RequestBody @Valid AddGroupMemberRequest request, @CurrentUserId long userId) {

        SimpleResult result = chatService.addGroupMember(chatId, userId, request.getNewUserId());

        if (result.isSuccess()) {
            return ResponseEntity.ok("User added to group successfully");
        } else {
            return ResponseEntity.badRequest().body(result.getErrorMessage());
        }
    }

    @PostMapping("/{chatId}/add-members")
    public ResponseEntity<?> addGroupMembers(@PathVariable @ValidId long chatId, @RequestBody @Valid AddGroupMembersRequest request, @CurrentUserId long userId) {

        SimpleResult result = chatService.addGroupMembers(chatId, userId, request.getMembers());

        if (result.isSuccess()) {
            return ResponseEntity.ok("User added to group successfully");
        } else {
            return ResponseEntity.badRequest().body(result.getErrorMessage());
        }
    }

    @PostMapping("/{chatId}/leave")
    public ResponseEntity<?> leaveChat(@PathVariable @ValidId long chatId, @CurrentUserId long userId) {

        SimpleResult result = chatService.leaveChat(chatId, userId);

        if (result.isSuccess()) {
            return ResponseEntity.ok("User leaved chat successfully");
        } else {
            return ResponseEntity.badRequest().body(result.getErrorMessage());
        }
    }

    @PostMapping("/{chatId}/admin-rights/{otherUserId}")
    public ResponseEntity<?> updateAdminRights(@PathVariable @ValidId long chatId, @PathVariable @ValidId long otherUserId, @RequestBody @Valid UpdateAdminRightsRequest request, @CurrentUserId long userId) {
        SimpleResult result = chatService.updateAdminRights(
            chatId, userId, otherUserId, request.getIsAdmin()
        );

        if (result.isSuccess()) {
            return ResponseEntity.ok("Successfully updated admin rights");
        } else {
            return ResponseEntity.badRequest().body(result.getErrorMessage());
        }
    }
    @DeleteMapping("/{chatId}")
    public ResponseEntity<?> deleteChat(@PathVariable @ValidId long chatId, @CurrentUserId long userId) {
        SimpleResult result = chatService.deleteChat(chatId, userId);

        if (result.isSuccess()) {
            return ResponseEntity.ok("Chat successfully deleted");
        } else {
            return ResponseEntity.badRequest().body(result.getErrorMessage());
        }
    }

    @GetMapping("/{chatId}/stats")
    public ResponseEntity<?> getChatStats(@PathVariable @ValidId long chatId, @CurrentUserId long userId) {

        ChatStatsResult result = chatService.getChatStats(chatId, userId);

        if (result.isSuccess()) {
            return ResponseEntity.ok(Map.of(
                "total_messages", result.getTotalMessages(),
                "deleted_for_all", result.getDeletedForAll(),
                "deleted_for_user", result.getDeletedForUser(),
                "can_delete_for_all", result.getCanDeleteForAll()
            ));
        } else {
            return ResponseEntity.badRequest().body(result.getErrorMessage());
        }
    }

    @GetMapping("/{chatId}/members")
    public ResponseEntity<?> getChatMembers(@PathVariable @ValidId long chatId, @Valid PaginationRequest request, @CurrentUserId long userId) {

        ChatMembersResult result = chatService.getChatMembers(chatId, userId, request.getCursor(), request.getLimit());

        if (result.isSuccess()) {
            return ResponseEntity.ok(result.getPagination());
        } else {
            return ResponseEntity.badRequest().body(result.getErrorMessage());
        }
    }

    @GetMapping
    public ResponseEntity<?> getUserChats(@Valid PaginationRequest request, @CurrentUserId long userId) {

        UserChatsResult result = chatService.getUserChats(userId, request.getCursor(), request.getLimit());

        if (result.isSuccess()) {
            return ResponseEntity.ok(result.getPagination());
        } else {
            return ResponseEntity.badRequest().body(result.getErrorMessage());
        }
    }
}