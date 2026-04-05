package com.sunrise.controller;

import com.sunrise.config.annotation.CurrentUserId;
import com.sunrise.config.annotation.ValidId;

import com.sunrise.controller.request.PaginationRequest;
import com.sunrise.core.service.result.ChatMessagesResult;
import com.sunrise.core.service.result.CreateMessageResult;
import com.sunrise.core.service.result.SimpleResult;

import com.sunrise.core.dataservice.type.Direction;
import com.sunrise.core.service.MessageService;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;

import jakarta.validation.constraints.NotNull;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Validated
@RestController
@RequestMapping("/chats")
public class MessageController {

    private final MessageService messageService;

    public MessageController(MessageService messageService){
        this.messageService = messageService;
    }

    @PostMapping("/{chatId}/messages")
    public ResponseEntity<?> makeChatMessagePublic(@PathVariable @ValidId long chatId,
                                                   @RequestParam @NotBlank(message = "must not be blank") String text,
                                                   @CurrentUserId long userId) {

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

    @PostMapping("/{chatId}/messages/private")
    public ResponseEntity<?> makeChatMessagePrivate(@PathVariable @ValidId long chatId,
                                                    @RequestParam @ValidId long userToSendId,
                                                    @RequestParam @NotBlank(message = "must not be blank") String text,
                                                    @CurrentUserId long userId) {

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
    public ResponseEntity<?> getChatMessages(@PathVariable @ValidId long chatId,
                                             @Valid PaginationRequest pagination,
                                             @RequestParam(defaultValue = "BACKWARD") @NotNull Direction type,
                                             @CurrentUserId long userId) {

        ChatMessagesResult result = messageService.getMessagePagination(chatId, userId, pagination.getCursor(), pagination.getLimit(), type);

        if (result.isSuccess()) {
            return ResponseEntity.ok(result.getPagination());
        }
        else {
            return ResponseEntity.badRequest().body(result.getErrorMessage());
        }
    }

    @PostMapping("/{chatId}/messages/mark-read/{messageId}")
    public ResponseEntity<?> markMessageAsRead(@PathVariable @ValidId long chatId,
                                               @PathVariable @ValidId long messageId,
                                               @CurrentUserId long userId) {

        SimpleResult result = messageService.markMessageAsRead(chatId, userId, messageId);

        if (result.isSuccess()) {
            return ResponseEntity.ok("Successfully marked message as read");
        }
        else {
            return ResponseEntity.badRequest().body(result.getErrorMessage());
        }
    }
}
