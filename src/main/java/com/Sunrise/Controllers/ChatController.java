package com.Sunrise.Controllers;

import com.Sunrise.Controllers.Annotations.CurrentUserId;
import com.Sunrise.DTO.Requests.AddGroupMemberRequest;
import com.Sunrise.DTO.Requests.CreateGroupChatRequest;
import com.Sunrise.DTO.Requests.CreatePersonalChatRequest;
import com.Sunrise.DTO.ServiceResults.*;
import com.Sunrise.Services.ChatService;
import com.Sunrise.Controllers.Annotations.ValidId;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/chat")
public class ChatController {

    private static final Logger log = LoggerFactory.getLogger(ChatController.class);

    private final ChatService chatService;
    public enum ClearType { FOR_ALL, FOR_SELF }

    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    @PostMapping("/create-personal")
    public ResponseEntity<?> createPersonalChat(@RequestBody @Valid CreatePersonalChatRequest request, @CurrentUserId Long userId) {

        var result = chatService.createPersonalChat(userId, request.getOtherUserId());

        if (result.isSuccess())
        {
            log.info("User created personal chat successfully --> id: {}", userId);
            return ResponseEntity.ok(Map.of(
                "chat_id", result.getChatId()
            ));
        }
        else
        {
            log.warn(result.getErrorMessage());
            return ResponseEntity.badRequest().body(result.getErrorMessage());
        }
    }

    @PostMapping("/create-group")
    public ResponseEntity<?> createGroupChat(@RequestBody @Valid CreateGroupChatRequest request, @CurrentUserId Long userId) {

        var result = chatService.createGroupChat(request.getChatName().trim(), userId, request.getUserIds());

        if (result.isSuccess())
        {
            log.info("User created group chat successfully --> id: {}", userId);
            return ResponseEntity.ok(Map.of(
                "info", "User created group chat successfully",
                "chat_id", result.getChatId()
            ));
        }
        else
        {
            log.warn(result.getErrorMessage());
            return ResponseEntity.badRequest().body(result.getErrorMessage());
        }
    }

    @PostMapping("/{chatId}/add-member")
    public ResponseEntity<?> addGroupMember(@PathVariable @ValidId Long chatId, @RequestBody @Valid AddGroupMemberRequest request, @CurrentUserId Long userId) {

        var result = chatService.addGroupMember(chatId, userId, request.getNewUserId());

        if (result.isSuccess())
        {
            log.info("User added to group successfully --> id: {}", userId);
            return ResponseEntity.ok("User added to group successfully");
        }
        else
        {
            log.warn(result.getErrorMessage());
            return ResponseEntity.badRequest().body(result.getErrorMessage());
        }
    }

    @PostMapping("/{chatId}/leave")
    public ResponseEntity<?> leaveChat(@PathVariable @ValidId Long chatId, @CurrentUserId Long userId) {

        var result = chatService.leaveChat(chatId, userId);

        if (result.isSuccess())
        {
            log.info("User leaved chat successfully --> id: {}", userId);
            return ResponseEntity.ok("User leaved chat successfully");
        }
        else
        {
            log.warn(result.getErrorMessage());
            return ResponseEntity.badRequest().body(result.getErrorMessage());
        }
    }

    @GetMapping("/{chatId}/stats")
    public ResponseEntity<?> getChatStats(@PathVariable @ValidId Long chatId, @CurrentUserId Long userId) {

        ChatStatsOperationResult result = chatService.getChatStats(chatId, userId);

        if (result.isSuccess())
        {
            log.info("User got chat stats successfully --> id: {}", userId);
            return ResponseEntity.ok(Map.of(
                "total_messages", result.getTotalMessages(),
                "deleted_for_all", result.getDeletedForAll(),
                "hidden_by_user", result.getHiddenByUser(),
                "can_clear_for_all", result.getCanClearForAll()
            ));
        }
        else
        {
            log.warn(result.getErrorMessage());
            return ResponseEntity.badRequest().body(result.getErrorMessage());
        }
    }

    @PostMapping("/{chatId}/clear-history")
    public ResponseEntity<?> clearChatHistory(@PathVariable @ValidId Long chatId, @RequestParam(defaultValue = "FOR_SELF") ClearType clearType, @CurrentUserId Long userId) {

        var result = chatService.clearChatHistory(chatId, clearType, userId);

        if (result.isSuccess())
        {
            log.info("User clear chat history successfully --> id: {}", userId);
            return ResponseEntity.ok(Map.of(
                "cleared_messages", result.getAffectedMessages()
            ));
        }
        else
        {
            log.warn(result.getErrorMessage());
            return ResponseEntity.badRequest().body(result.getErrorMessage());
        }
    }


    @GetMapping("/{chatId}/members")
    public ResponseEntity<?> getChatMembers(@PathVariable @ValidId Long chatId, @CurrentUserId Long userId) {

        var result = chatService.getChatMembers(chatId, userId);

        if (result.isSuccess())
        {
            log.info("User got chat members successfully --> id: {}", userId);
            return ResponseEntity.ok(Map.of(
                "members", result.getChatMembers(),
                "count", result.getChatMembersCount()
            ));
        }
        else
        {
            log.warn(result.getErrorMessage());
            return ResponseEntity.badRequest().body(result.getErrorMessage());
        }
    }
    @GetMapping
    public ResponseEntity<?> getUserChats(@CurrentUserId Long userId) {

        var result = chatService.getUserChats(userId);

        if (result.isSuccess()) {
            log.info("User got chats successfully --> id: {}", userId);
            return ResponseEntity.ok(Map.of(
                "chats", result.getUserChats(),
                "count", result.getUserChatsCount()
            ));
        }
        else {
            log.warn(result.getErrorMessage());
            return ResponseEntity.badRequest().body(result.getErrorMessage());
        }
    }
}
