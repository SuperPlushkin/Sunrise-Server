package com.Sunrise.Controllers;

import com.Sunrise.Controllers.Annotations.CurrentUserId;
import com.Sunrise.DTO.Requests.AddGroupMemberRequest;
import com.Sunrise.DTO.Requests.CreateGroupChatRequest;
import com.Sunrise.DTO.Requests.CreatePersonalChatRequest;
import com.Sunrise.DTO.ServiceResults.*;
import com.Sunrise.Services.ChatService;
import com.Sunrise.Controllers.Annotations.ValidId;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/chats")
public class ChatController {

    private final ChatService chatService;
    public enum ClearType { FOR_ALL, FOR_SELF }

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
                "info", "User created group chat successfully",
                "chat_id", result.getChatId()
            ));
        }
        else {
            return ResponseEntity.badRequest().body(result.getErrorMessage());
        }
    }

    @PostMapping("/{chatId}/add-member")
    public ResponseEntity<?> addGroupMember(@PathVariable @ValidId Long chatId, @RequestBody @Valid AddGroupMemberRequest request, @CurrentUserId Long userId) {

        SimpleResult result = chatService.addGroupMember(chatId, userId, request.getNewUserId());

        if (result.isSuccess()) {
            return ResponseEntity.ok("User added to group successfully");
        }
        else {
            return ResponseEntity.badRequest().body(result.getErrorMessage());
        }
    }

    @PostMapping("/{chatId}/leave")
    public ResponseEntity<?> leaveChat(@PathVariable @ValidId Long chatId, @CurrentUserId Long userId) {

        SimpleResult result = chatService.leaveChat(chatId, userId);

        if (result.isSuccess()) {
            return ResponseEntity.ok("User leaved chat successfully");
        }
        else {
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
        }
        else {
            return ResponseEntity.badRequest().body(result.getErrorMessage());
        }
    }

    @PostMapping("/{chatId}/delete-all-messages")
    public ResponseEntity<?> clearChatHistory(@PathVariable @ValidId Long chatId, @RequestParam(defaultValue = "FOR_SELF") ClearType clearType, @CurrentUserId Long userId) {

        HistoryOperationResult result = chatService.deleteAllChatMessages(chatId, clearType, userId);

        if (result.isSuccess()) {
            return ResponseEntity.ok(Map.of(
                "cleared_messages", result.getAffectedMessages()
            ));
        }
        else {
            return ResponseEntity.badRequest().body(result.getErrorMessage());
        }
    }


    @GetMapping("/{chatId}/members")
    public ResponseEntity<?> getChatMembers(@PathVariable @ValidId Long chatId, @CurrentUserId Long userId) {

        ChatMembersResult result = chatService.getChatMembers(chatId, userId);

        if (result.isSuccess()) {
            return ResponseEntity.ok(Map.of(
                "members", result.getChatMembers(),
                "count", result.getChatMembersCount()
            ));
        }
        else {
            return ResponseEntity.badRequest().body(result.getErrorMessage());
        }
    }

    @GetMapping
    public ResponseEntity<?> getUserChats(@RequestParam(defaultValue = "0") int offset, @RequestParam(defaultValue = "10") int limit, @CurrentUserId Long userId) {

        UserChatsResult result = chatService.getUserChats(userId, offset, limit);

        if (result.isSuccess()) {
            return ResponseEntity.ok(Map.of(
                "chats", result.getUserChats(),
                "count", result.getUserChatsCount()
            ));
        }
        else {
            return ResponseEntity.badRequest().body(result.getErrorMessage());
        }
    }
}