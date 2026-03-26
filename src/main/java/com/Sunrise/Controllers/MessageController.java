package com.Sunrise.Controllers;

import com.Sunrise.Configurations.Annotations.CurrentUserId;
import com.Sunrise.Configurations.Annotations.ValidId;

import com.Sunrise.DTOs.ServiceResults.ChatMessagesResult;
import com.Sunrise.DTOs.ServiceResults.CreateMessageResult;
import com.Sunrise.DTOs.ServiceResults.SimpleResult;

import com.Sunrise.Core.DataServices.DataOrchestrator.Direction;
import com.Sunrise.Core.Services.MessageService;

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

        ChatMessagesResult result = messageService.getChatMessages(chatId, userId, null, limited, Direction.BACKWARD);

        if (result.isSuccess()) {
            return ResponseEntity.ok(result.getPagination());
        }
        else {
            return ResponseEntity.badRequest().body(result.getErrorMessage());
        }
    }

    @GetMapping("/{chatId}/messages/before")
    public ResponseEntity<?> getChatMessagesBefore(@PathVariable @ValidId Long chatId, @RequestParam @ValidId Long fromMessageId,
                                                   @RequestParam(defaultValue = "50") @Min(value = 1, message = "limited must be positive") Integer limited,
                                                   @CurrentUserId Long userId) {

        ChatMessagesResult result = messageService.getChatMessages(chatId, userId, fromMessageId, limited, Direction.FORWARD);

        if (result.isSuccess()) {
            return ResponseEntity.ok(result.getPagination());
        }
        else {
            return ResponseEntity.badRequest().body(result.getErrorMessage());
        }
    }

    @GetMapping("/{chatId}/messages/after")
    public ResponseEntity<?> getChatMessagesAfter(@PathVariable @ValidId Long chatId, @RequestParam @ValidId Long fromMessageId,
                                                  @RequestParam(defaultValue = "50") @Min(value = 1, message = "limited must be positive") Integer limited,
                                                  @CurrentUserId Long userId) {

        ChatMessagesResult result = messageService.getChatMessages(chatId, userId, fromMessageId, limited, Direction.BACKWARD);

        if (result.isSuccess()) {
            return ResponseEntity.ok(result.getPagination());
        }
        else {
            return ResponseEntity.badRequest().body(result.getErrorMessage());
        }
    }

    @PostMapping("/{chatId}/messages/mark-read/{messageId}")
    public ResponseEntity<?> markMessageAsRead(@PathVariable @ValidId Long chatId, @PathVariable @ValidId Long messageId, @CurrentUserId Long userId) {

        SimpleResult result = messageService.markMessageAsRead(chatId, userId, messageId);

        if (result.isSuccess()) {
            return ResponseEntity.ok("Successfully marked message as read");
        }
        else {
            return ResponseEntity.badRequest().body(result.getErrorMessage());
        }
    }
}
