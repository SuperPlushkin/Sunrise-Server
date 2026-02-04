package com.Sunrise.Controllers;

import com.Sunrise.Controllers.Annotations.CurrentUserId;
import com.Sunrise.Controllers.Annotations.ValidId;
import com.Sunrise.Services.MessageService;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/chat")
public class MessageController {

    private static final Logger log = LoggerFactory.getLogger(MessageController.class);

    private final MessageService messageService;

    public MessageController(MessageService messageService){
        this.messageService = messageService;
    }

    @GetMapping("/{chatId}/messages/make-public")
    public ResponseEntity<?> makeChatMessagePublic(@PathVariable @ValidId Long chatId,
                                                   @RequestParam @NotBlank(message = "text must be blanked") String text,
                                                   @CurrentUserId Long userId) {

        var result = messageService.makePublicMessage(chatId, userId, text);

        if (result.isSuccess()) {
            log.info("User send public message in chat successfully --> id: {}", result.getMessageId());
            return ResponseEntity.ok(Map.of(
                "messageId", result.getMessageId(),
                "sentAt", result.getSentAt()
            ));
        }
        else {
            log.warn(result.getErrorMessage());
            return ResponseEntity.badRequest().body(result.getErrorMessage());
        }
    }

    @GetMapping("/{chatId}/messages/make-private")
    public ResponseEntity<?> makeChatMessagePrivate(@PathVariable @ValidId Long chatId,
                                                    @RequestParam @ValidId Long userToSendId,
                                                    @RequestParam @NotBlank(message = "text must be blanked") String text,
                                                    @CurrentUserId Long userId) {

        var result = messageService.makePrivateMessage(chatId, userId, userToSendId, text);

        if (result.isSuccess()) {
            log.info("User send private message in chat successfully --> id: {}", result.getMessageId());
            return ResponseEntity.ok(Map.of(
                    "messageId", result.getMessageId(),
                    "sentAt", result.getSentAt()
            ));
        }
        else {
            log.warn(result.getErrorMessage());
            return ResponseEntity.badRequest().body(result.getErrorMessage());
        }
    }


    @GetMapping("/{chatId}/messages")
    public ResponseEntity<?> getChatMessagesFirst(@PathVariable @ValidId Long chatId,
                                                  @RequestParam(defaultValue = "50") @Min(value = 1, message = "limited must be positive") Integer limited,
                                                  @CurrentUserId Long userId) {

        var result = messageService.getChatMessagesFirst(chatId, userId, limited);

        if (result.isSuccess()) {
            var messages = result.getMessages();

            log.info("User got first chat messages successfully --> id: {}", userId);
            return ResponseEntity.ok(Map.of(
                "messages", messages,
                "count", messages.size()
            ));
        }
        else {
            log.warn(result.getErrorMessage());
            return ResponseEntity.badRequest().body(result.getErrorMessage());
        }
    }

    @GetMapping("/{chatId}/messages/before")
    public ResponseEntity<?> getChatMessagesBefore(@PathVariable @ValidId Long chatId, @RequestParam @ValidId Long messageId,
                                                   @RequestParam(defaultValue = "50") @Min(value = 1, message = "limited must be positive") Integer limited,
                                                   @CurrentUserId Long userId) {

        var result = messageService.getChatMessagesBefore(chatId, userId, messageId, limited);

        if (result.isSuccess()) {
            var messages = result.getMessages();

            log.info("User got chat messages before msg successfully --> id: {}", userId);
            return ResponseEntity.ok(Map.of(
                    "messages", messages,
                    "count", messages.size()
            ));
        }
        else {
            log.warn(result.getErrorMessage());
            return ResponseEntity.badRequest().body(result.getErrorMessage());
        }
    }

    @GetMapping("/{chatId}/messages/after")
    public ResponseEntity<?> getChatMessagesAfter(@PathVariable @ValidId Long chatId, @RequestParam @ValidId Long messageId,
                                                  @RequestParam(defaultValue = "50") @Min(value = 1, message = "limited must be positive") Integer limited,
                                                  @CurrentUserId Long userId) {

        var result = messageService.getChatMessagesAfter(chatId, userId, messageId, limited);

        if (result.isSuccess()) {
            var messages = result.getMessages();

            log.info("User got chat messages after msg successfully --> id: {}", userId);
            return ResponseEntity.ok(Map.of(
                    "messages", messages,
                    "count", messages.size()
            ));
        }
        else {
            log.warn(result.getErrorMessage());
            return ResponseEntity.badRequest().body(result.getErrorMessage());
        }
    }

    @PostMapping("/{chatId}/messages/mark-read/{messageId}")
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

    @GetMapping("/{chatId}/messages/count")
    public ResponseEntity<?> getVisibleMessageCount(@PathVariable @ValidId Long chatId, @CurrentUserId Long userId) {

        var result = messageService.getVisibleMessagesCount(chatId, userId);

        if (result.isSuccess()) {
            log.info("User got chat visible messages count successfully --> id: {}", userId);
            return ResponseEntity.ok(Map.of(
                "count", result.getVisibleMessagesCount()
            ));
        }
        else {
            log.warn(result.getErrorMessage());
            return ResponseEntity.badRequest().body(result.getErrorMessage());
        }
    }

    @GetMapping("/status")
    public Map<String, String> getStatus() {
        return Map.of(
            "status", "🟢 Онлайн",
            "version", "1.0",
            "users", "1"
        );
    }
}
