package com.sunrise.controller;

import com.sunrise.config.annotation.CurrentUserId;
import com.sunrise.config.annotation.ValidId;

import com.sunrise.controller.request.PaginationRequest;
import com.sunrise.controller.request.PrivateMessageRequest;
import com.sunrise.controller.request.PublicMessageRequest;
import com.sunrise.core.service.result.*;

import com.sunrise.core.dataservice.type.Direction;
import com.sunrise.core.service.MessageService;

import jakarta.validation.Valid;

import jakarta.validation.constraints.NotNull;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Validated
@RestController
@RequestMapping("/chats/{chatId}/messages")
public class MessageController {

    private final MessageService messageService;

    public MessageController(MessageService messageService){
        this.messageService = messageService;
    }

    @PostMapping
    public ResponseEntity<?> sendPublicMessage(@PathVariable @ValidId long chatId,
                                               @RequestBody @Valid PublicMessageRequest request, @CurrentUserId long userId) {

        CreateMessageResult result = messageService.makePublicMessage(chatId, userId, request.getText());

        if (result.isSuccess()) {
            return ResponseEntity.ok(Map.of(
                "messageId", result.getMessageId()
            ));
        }
        else {
            return ResponseEntity.badRequest().body(result.getErrorMessage());
        }
    }

    @PostMapping("/private")
    public ResponseEntity<?> sendPrivateMessage(@PathVariable @ValidId long chatId,
                                                @RequestBody @Valid PrivateMessageRequest request, @CurrentUserId long userId) {

        CreateMessageResult result = messageService.makePrivateMessage(chatId, userId, request.getUserToSendId(), request.getText());

        if (result.isSuccess()) {
            return ResponseEntity.ok(Map.of(
                "messageId", result.getMessageId()
            ));
        }
        else {
            return ResponseEntity.badRequest().body(result.getErrorMessage());
        }
    }

    @GetMapping
    public ResponseEntity<?> getMessagesPage(@PathVariable @ValidId long chatId, @Valid PaginationRequest pagination,
                                             @RequestParam(defaultValue = "BACKWARD") @NotNull Direction direction, @CurrentUserId long userId) {

        ChatMessagesResult result = messageService.getMessagePagination(chatId, userId, pagination.getCursor(), pagination.getLimit(), direction);

        if (result.isSuccess()) {
            return ResponseEntity.ok(result.getPagination());
        }
        else {
            return ResponseEntity.badRequest().body(result.getErrorMessage());
        }
    }

    @PostMapping("/{messageId}/mark-up-to-read")
    public ResponseEntity<?> markMessagesUpToRead(@PathVariable @ValidId long chatId, @PathVariable @ValidId long messageId, @CurrentUserId long userId) {

        SimpleResult result = messageService.markMessagesUpToRead(chatId, userId, messageId);

        if (result.isSuccess()) {
            return ResponseEntity.ok("Successfully marked message as read");
        }
        else {
            return ResponseEntity.badRequest().body(result.getErrorMessage());
        }
    }

    @DeleteMapping("/{messageId}")
    public ResponseEntity<?> deleteMessage(@PathVariable @ValidId long chatId, @PathVariable @ValidId long messageId, @CurrentUserId long userId) {

        SimpleResult result = messageService.deleteMessage(chatId, userId, messageId);

        if (result.isSuccess()) {
            return ResponseEntity.ok("Message successfully deleted");
        }
        else {
            return ResponseEntity.badRequest().body(result.getErrorMessage());
        }
    }

    @GetMapping("/{messageId}")
    public ResponseEntity<?> getMessage(@PathVariable @ValidId long chatId, @PathVariable @ValidId long messageId, @CurrentUserId long userId) {

        ChatMessageResult result = messageService.getMessage(chatId, userId, messageId);

        if (result.isSuccess()) {
            return ResponseEntity.ok(result.getMessage());
        }
        else {
            return ResponseEntity.badRequest().body(result.getErrorMessage());
        }
    }

    @GetMapping("/{messageId}/reads")
    public ResponseEntity<?> getMessageReads(@PathVariable @ValidId long chatId, @PathVariable @ValidId long messageId, @CurrentUserId long userId) {

        MessageReadsResult result = messageService.getMessageReads(chatId, userId, messageId);

        if (result.isSuccess()) {
            return ResponseEntity.ok(result.getReads());
        }
        else {
            return ResponseEntity.badRequest().body(result.getErrorMessage());
        }
    }
}