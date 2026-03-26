package com.Sunrise.Controllers;

import com.Sunrise.Configurations.Annotations.CurrentUserId;
import com.Sunrise.DTOs.Requests.AddGroupMemberRequest;
import com.Sunrise.DTOs.Requests.CreateGroupChatRequest;
import com.Sunrise.DTOs.Requests.CreatePersonalChatRequest;
import com.Sunrise.DTOs.ServiceResults.*;
import com.Sunrise.Core.Services.ChatService;
import com.Sunrise.Configurations.Annotations.ValidId;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/chats")
public class ChatController {

    private final ChatService chatService;

    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    @PostMapping("/create-personal")
    public ResponseEntity<?> createPersonalChat(@RequestBody @Valid CreatePersonalChatRequest request, @CurrentUserId Long userId) {

//        if (creatorId.equals(userToAddId))
//            return ChatCreationResult.error("[🔧] Cannot create personal chat with yourself");

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
    public ResponseEntity<?> createGroupChat(@RequestBody @Valid CreateGroupChatRequest request, @CurrentUserId Long userId) {

        ChatCreationResult result = chatService.createGroupChat(
            request.getChatName().trim(),
            userId,
            request.getUserIds()
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
    public ResponseEntity<?> addGroupMember(@PathVariable @ValidId Long chatId, @RequestBody @Valid AddGroupMemberRequest request, @CurrentUserId Long userId) {

        SimpleResult result = chatService.addGroupMember(chatId, userId, request.getNewUserId());

        if (result.isSuccess()) {
            return ResponseEntity.ok("User added to group successfully");
        } else {
            return ResponseEntity.badRequest().body(result.getErrorMessage());
        }
    }

    @PostMapping("/{chatId}/leave")
    public ResponseEntity<?> leaveChat(@PathVariable @ValidId Long chatId, @CurrentUserId Long userId) {

        SimpleResult result = chatService.leaveChat(chatId, userId);

        if (result.isSuccess()) {
            return ResponseEntity.ok("User leaved chat successfully");
        } else {
            return ResponseEntity.badRequest().body(result.getErrorMessage());
        }
    }

    @GetMapping("/{chatId}/stats")
    public ResponseEntity<?> getChatStats(@PathVariable @ValidId Long chatId, @CurrentUserId Long userId) {

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
    public ResponseEntity<?> getChatMembers(@PathVariable @ValidId Long chatId, @RequestParam Long cursor, @CurrentUserId Long userId) {

        ChatMembersResult result = chatService.getChatMembers(chatId, cursor, userId);

        if (result.isSuccess()) {
            return ResponseEntity.ok(result.getPagination());
        } else {
            return ResponseEntity.badRequest().body(result.getErrorMessage());
        }
    }

    @GetMapping
    public ResponseEntity<?> getUserChats(@RequestParam(defaultValue = "0") Long cursor, @RequestParam(defaultValue = "10") int limit, @CurrentUserId Long userId) {

        UserChatsResult result = chatService.getUserChats(userId, cursor, limit);

        if (result.isSuccess()) {
            return ResponseEntity.ok(result.getPagination());
        } else {
            return ResponseEntity.badRequest().body(result.getErrorMessage());
        }
    }
}