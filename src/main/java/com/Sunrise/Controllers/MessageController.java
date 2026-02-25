package com.Sunrise.Controllers;

import com.Sunrise.Controllers.Annotations.CurrentUserId;
import com.Sunrise.Controllers.Annotations.ValidId;
import com.Sunrise.DTO.ServiceResults.ChatMessagesResult;
import com.Sunrise.DTO.ServiceResults.CreateMessageResult;
import com.Sunrise.DTO.ServiceResults.SimpleResult;
import com.Sunrise.DTO.ServiceResults.VisibleMessagesCountResult;
import com.Sunrise.Services.MessageService;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/chats")
public class MessageController {

    private final MessageService messageService;

    public MessageController(MessageService messageService){
        this.messageService = messageService;
    }

    @GetMapping("/{chatId}/messages/make-public")
    public ResponseEntity<?> makeChatMessagePublic(@PathVariable @ValidId Long chatId,
                                                   @RequestParam @NotBlank(message = "text must be blanked") String text,
                                                   @CurrentUserId Long userId) {

        CreateMessageResult result = messageService.makePublicMessage(chatId, userId, text);

        if (result.isSuccess()) {
            return ResponseEntity.ok(Map.of(
                "messageId", result.getMessageId(),
                "sentAt", result.getSentAt()
            ));
        }
        else {
            return ResponseEntity.badRequest().body(result.getErrorMessage());
        }
    }

    @GetMapping("/{chatId}/messages/make-private")
    public ResponseEntity<?> makeChatMessagePrivate(@PathVariable @ValidId Long chatId,
                                                    @RequestParam @ValidId Long userToSendId,
                                                    @RequestParam @NotBlank(message = "text must be blanked") String text,
                                                    @CurrentUserId Long userId) {

        CreateMessageResult result = messageService.makePrivateMessage(chatId, userId, userToSendId, text);

        if (result.isSuccess()) {
            return ResponseEntity.ok(Map.of(
                "messageId", result.getMessageId(),
                "sentAt", result.getSentAt()
            ));
        }
        else {
            return ResponseEntity.badRequest().body(result.getErrorMessage());
        }
    }


    @GetMapping("/{chatId}/messages")
    public ResponseEntity<?> getChatMessagesFirst(@PathVariable @ValidId Long chatId,
                                                  @RequestParam(defaultValue = "50") @Min(value = 1, message = "limited must be positive") Integer limited,
                                                  @CurrentUserId Long userId) {

        ChatMessagesResult result = messageService.getChatMessagesUpToDate(chatId, userId, limited);

        if (result.isSuccess()) {
            var messages = result.getMessages();
            return ResponseEntity.ok(Map.of(
                "messages", messages,
                "count", messages.size()
            ));
        }
        else {
            return ResponseEntity.badRequest().body(result.getErrorMessage());
        }
    }

    @GetMapping("/{chatId}/messages/before")
    public ResponseEntity<?> getChatMessagesBefore(@PathVariable @ValidId Long chatId, @RequestParam @ValidId Long messageId,
                                                   @RequestParam(defaultValue = "50") @Min(value = 1, message = "limited must be positive") Integer limited,
                                                   @CurrentUserId Long userId) {

        ChatMessagesResult result = messageService.getChatMessagesBefore(chatId, userId, messageId, limited);

        if (result.isSuccess()) {
            var messages = result.getMessages();
            return ResponseEntity.ok(Map.of(
                "messages", messages,
                "count", messages.size()
            ));
        }
        else {
            return ResponseEntity.badRequest().body(result.getErrorMessage());
        }
    }

    @GetMapping("/{chatId}/messages/after")
    public ResponseEntity<?> getChatMessagesAfter(@PathVariable @ValidId Long chatId, @RequestParam @ValidId Long messageId,
                                                  @RequestParam(defaultValue = "50") @Min(value = 1, message = "limited must be positive") Integer limited,
                                                  @CurrentUserId Long userId) {

        ChatMessagesResult result = messageService.getChatMessagesAfter(chatId, userId, messageId, limited);

        if (result.isSuccess()) {
            var messages = result.getMessages();
            return ResponseEntity.ok(Map.of(
                "messages", messages,
                "count", messages.size()
            ));
        }
        else {
            return ResponseEntity.badRequest().body(result.getErrorMessage());
        }
    }

    @PostMapping("/{chatId}/messages/mark-read/{messageId}")
    public ResponseEntity<?> markMessageAsRead(@PathVariable @ValidId Long chatId, @PathVariable @ValidId Long messageId, @CurrentUserId Long userId) {

        SimpleResult result = messageService.markMessageAsRead(chatId, messageId, userId);

        if (result.isSuccess()) {
            return ResponseEntity.ok("Successfully marked message as read");
        }
        else {
            return ResponseEntity.badRequest().body(result.getErrorMessage());
        }
    }

    @GetMapping("/{chatId}/messages/count")
    public ResponseEntity<?> getVisibleMessageCount(@PathVariable @ValidId Long chatId, @CurrentUserId Long userId) {

        VisibleMessagesCountResult result = messageService.getVisibleMessagesCount(chatId, userId);

        if (result.isSuccess()) {
            return ResponseEntity.ok(Map.of(
                "count", result.getVisibleMessagesCount()
            ));
        } else {
            return ResponseEntity.badRequest().body(result.getErrorMessage());
        }
    }
}
