package com.Sunrise.Controllers;

import com.Sunrise.Controllers.Annotations.CurrentUserId;
import com.Sunrise.DTO.Requests.AddGroupMemberRequest;
import com.Sunrise.DTO.Requests.CreateGroupChatRequest;
import com.Sunrise.DTO.Requests.CreatePersonalChatRequest;
import com.Sunrise.DTO.ServiceResults.*;
import com.Sunrise.Services.ChatService;
import com.Sunrise.Controllers.Annotations.ValidId;
import com.Sunrise.Services.MessageService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
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
    private final MessageService messageService;
    public enum ClearType { FOR_ALL, FOR_SELF }

    public ChatController(ChatService chatService, MessageService messageService) {
        this.chatService = chatService;
        this.messageService = messageService;
    }

    @PostMapping("/create-personal")
    public ResponseEntity<?> createPersonalChat(@RequestBody @Valid CreatePersonalChatRequest request, @CurrentUserId Long userId) {

        var result = chatService.createPersonalChat(userId, request.getOtherUserId());

        if (result.isSuccess())
        {
            log.info("User created personal chat successfully --> id: {}", userId);
            return ResponseEntity.ok(Map.of(
                "info", "User created personal chat successfully",
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
                "info", "User got chat stats successfully",
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
                "info", "User clear chat history successfully",
                "cleared_messages", result.getAffectedMessages()
            ));
        }
        else
        {
            log.warn(result.getErrorMessage());
            return ResponseEntity.badRequest().body(result.getErrorMessage());
        }
    }

    @PostMapping("/{chatId}/restore-history")
    public ResponseEntity<?> restoreChatHistory(@PathVariable @ValidId Long chatId, @CurrentUserId Long userId) {

        var result = chatService.restoreChatHistory(chatId, userId);

        if (result.isSuccess())
        {
            log.info("User restored chat history successfully --> id: {}", userId);
            return ResponseEntity.ok(Map.of(
                "info", "User restored chat history successfully",
                "restored_messages", result.getAffectedMessages()
            ));
        }
        else
        {
            log.warn(result.getErrorMessage());
            return ResponseEntity.badRequest().body(result.getErrorMessage());
        }
    }

    @GetMapping("/{chatId}/messages")
    public ResponseEntity<?> getChatMessages(@PathVariable @ValidId Long chatId,
                                             @RequestParam(defaultValue = "50") @Min(value = 1, message = "limited must be positive") Integer limited,
                                             @RequestParam(defaultValue = "0") @Min(value = 1, message = "offset must be positive") Integer offset,
                                             @CurrentUserId Long userId) {

        var result = messageService.getChatMessages(chatId, userId, limited, offset);

        if (result.isSuccess())
        {
            var messages = result.getMessages();

            log.info("User got chat messages successfully --> id: {}", userId);
            return ResponseEntity.ok(Map.of(
                "info", "User got chat messages successfully",
                "messages", messages,
                "count", messages.size()
            ));
        }
        else
        {
            log.warn(result.getErrorMessage());
            return ResponseEntity.badRequest().body(result.getErrorMessage());
        }
    }

    @PostMapping("/{chatId}/mark-read/{messageId}")
    public ResponseEntity<?> markMessageAsRead(@PathVariable @ValidId Long chatId, @PathVariable @ValidId Long messageId, @CurrentUserId Long userId) {

        var result = messageService.markMessageAsRead(chatId, messageId, userId);

        if (result.isSuccess()) {
            log.info("Successfully marked message as read --> messageId: {}", messageId);
            return ResponseEntity.ok("Successfully marked message as read");
        }
        else {
            log.warn(result.getErrorMessage());
            return ResponseEntity.badRequest().body(result.getErrorMessage());
        }
    }

    @GetMapping("/{chatId}/message-count")
    public ResponseEntity<?> getVisibleMessageCount(@PathVariable @ValidId Long chatId, @CurrentUserId Long userId) {

        var result = messageService.getVisibleMessagesCount(chatId, userId);

        if (result.isSuccess())
        {
            log.info("User got chat visible messages count successfully --> id: {}", userId);
            return ResponseEntity.ok(Map.of(
                "info", "User got chat visible messages count successfully",
                "count", result.getVisibleMessagesCount()
            ));
        }
        else
        {
            log.warn(result.getErrorMessage());
            return ResponseEntity.badRequest().body(result.getErrorMessage());
        }
    }


    // Чтобы было, по факту бесполезно
    @GetMapping("/{chatId}/is-admin")
    public ResponseEntity<?> isChatAdmin(@PathVariable @ValidId Long chatId, @CurrentUserId Long userId) {

        var result = chatService.isChatAdmin(chatId, userId);

        if (result.isSuccess())
        {
            log.info("User got chat admin status successfully --> id: {}", userId);
            return ResponseEntity.ok(Map.of(
                "info", "User got chat admin status successfully",
                "is_admin", result.getIsChatAdmin()
            ));
        }
        else
        {
            log.warn(result.getErrorMessage());
            return ResponseEntity.badRequest().body(result.getErrorMessage());
        }
    }
    @GetMapping("/{chatId}/is-group")
    public ResponseEntity<?> isGroupChat(@PathVariable @ValidId Long chatId, @CurrentUserId Long userId) {

        var result = chatService.isGroupChat(chatId, userId);

        if (result.isSuccess())
        {
            log.info("User got chat IsGroup status successfully --> chatId: {}", chatId);
            return ResponseEntity.ok(Map.of(
                "info", "User got chat IsGroup status successfully",
                "is_group", result.getIsGroupChat()
            ));
        }
        else
        {
            log.warn(result.getErrorMessage());
            return ResponseEntity.badRequest().body(result.getErrorMessage());
        }
    }
}
