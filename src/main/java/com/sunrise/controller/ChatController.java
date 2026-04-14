package com.sunrise.controller;

import com.sunrise.config.annotation.CurrentUserId;
import com.sunrise.controller.request.*;
import com.sunrise.core.service.result.*;
import com.sunrise.core.service.ChatService;
import com.sunrise.config.annotation.ValidId;

import com.sunrise.entity.dto.UserChatDTO;
import com.sunrise.entity.pagination.UserChatsPageDTO;
import jakarta.validation.Valid;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/chats")
public class ChatController {

    private final ChatService chatService;

    @PostMapping("/create-personal")
    public ResponseEntity<?> createPersonalChat(@RequestBody @Valid CreatePersonalChatRequest request, @CurrentUserId long userId) {

        ResultOneArg<Long> result = chatService.createPersonalChat(request.getTempId(), userId, request.getOtherUserId());

        if (result.isSuccess()) {
            return ResponseEntity.ok(result.getResult());
        } else {
            return ResponseEntity.badRequest().body(result.getError());
        }
    }

    @PostMapping("/create-group")
    public ResponseEntity<?> createGroupChat(@RequestBody @Valid CreateGroupChatRequest request, @CurrentUserId long userId) {

        ResultOneArg<Long> result = chatService.createGroupChat(
            request.getTempId(), userId,
            request.getChatName().trim(),
            request.getChatDescription(),
            request.getGroupType(),
            request.getMembers()
        );

        if (result.isSuccess()) {
            return ResponseEntity.ok(result.getResult());
        } else {
            return ResponseEntity.badRequest().body(result.getError());
        }
    }

    @PostMapping("/{chatId}/info")
    public ResponseEntity<?> updateChatInfo(@PathVariable @ValidId long chatId, @RequestBody @Valid UpdateChatInfoRequest request, @CurrentUserId long userId) {

        ResultNoArgs result = chatService.updateChatInfo(
            chatId, userId, request.getChatName(), request.getChatDescription()
        );

        if (result.isSuccess()) {
            return ResponseEntity.ok(result.getOperationText());
        } else {
            return ResponseEntity.badRequest().body(result.getError());
        }
    }

    @PostMapping("/{chatId}/chat-type")
    public ResponseEntity<?> updateChatType(@PathVariable @ValidId long chatId, @RequestBody @Valid UpdateChatTypeRequest request, @CurrentUserId long userId) {

        ResultNoArgs result = chatService.updateChatType(
            chatId, userId, request.getGroupType()
        );

        if (result.isSuccess()) {
            return ResponseEntity.ok(result.getOperationText());
        } else {
            return ResponseEntity.badRequest().body(result.getError());
        }
    }

    @DeleteMapping("/{chatId}")
    public ResponseEntity<?> deleteChat(@PathVariable @ValidId long chatId, @CurrentUserId long userId) {
        ResultNoArgs result = chatService.deleteChat(chatId, userId);

        if (result.isSuccess()) {
            return ResponseEntity.ok(result.getOperationText());
        } else {
            return ResponseEntity.badRequest().body(result.getError());
        }
    }

    @GetMapping("/{chatId}/stats")
    public ResponseEntity<?> getChatStats(@PathVariable @ValidId long chatId, @CurrentUserId long userId) {

        ResultOneArg<ChatStatsResult> result = chatService.getChatStats(chatId, userId);

        if (result.isSuccess()) {
            return ResponseEntity.ok(result.getResult());
        } else {
            return ResponseEntity.badRequest().body(result.getError());
        }
    }

    @GetMapping("/ids")
    public ResponseEntity<?> getUserChatIds(@CurrentUserId long userId) {
        ResultOneArg<List<Long>> result = chatService.getUserChatIds(userId);

        if (result.isSuccess()) {
            return ResponseEntity.ok(result.getResult());
        } else {
            return ResponseEntity.badRequest().body(result.getError());
        }
    }
    @GetMapping("/{chatId}")
    public ResponseEntity<?> getUserChat(@PathVariable @ValidId long chatId, @CurrentUserId long userId) {

        ResultOneArg<UserChatDTO> result = chatService.getUserChat(chatId, userId);

        if (result.isSuccess()) {
            return ResponseEntity.ok(result.getResult());
        } else {
            return ResponseEntity.badRequest().body(result.getError());
        }
    }
    @GetMapping
    public ResponseEntity<?> getUserChatsPage(@Valid ChatPaginationRequest request, @CurrentUserId long userId) {

        ResultOneArg<UserChatsPageDTO> result = chatService.getUserChatsPage(
            userId, request.getIsPinnedCursor(), request.getLastMsgIdCursor(),
            request.getChatIdCursor(), request.getLimit()
        );

        if (result.isSuccess()) {
            return ResponseEntity.ok(result.getResult());
        } else {
            return ResponseEntity.badRequest().body(result.getError());
        }
    }
}