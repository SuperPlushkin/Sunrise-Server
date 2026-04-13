package com.sunrise.controller;

import com.sunrise.config.annotation.CurrentUserId;
import com.sunrise.config.annotation.ValidId;
import com.sunrise.controller.request.AddGroupMemberRequest;
import com.sunrise.controller.request.AddGroupMembersRequest;
import com.sunrise.controller.request.PaginationRequest;
import com.sunrise.controller.request.UpdateAdminRightsRequest;
import com.sunrise.core.service.ChatMemberService;
import com.sunrise.core.service.result.ResultNoArgs;
import com.sunrise.core.service.result.ResultOneArg;
import com.sunrise.entity.pagination.ChatMembersPageDTO;

import jakarta.validation.Valid;

import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/chats/{chatId}")
public class ChatMemberController {

    private final ChatMemberService chatMemberService;

    @PostMapping("/add-member")
    public ResponseEntity<?> addGroupMember(@PathVariable @ValidId long chatId, @RequestBody @Valid AddGroupMemberRequest request, @CurrentUserId long userId) {

        ResultNoArgs result = chatMemberService.addOrRestoreChatMember(chatId, userId, request.getNewUserId());

        if (result.isSuccess()) {
            return ResponseEntity.ok(result.getOperationText());
        } else {
            return ResponseEntity.badRequest().body(result.getError());
        }
    }

    @PostMapping("/add-members")
    public ResponseEntity<?> addGroupMembers(@PathVariable @ValidId long chatId, @RequestBody @Valid AddGroupMembersRequest request, @CurrentUserId long userId) {

        ResultNoArgs result = chatMemberService.addOrRestoreChatMembers(chatId, userId, request.getMembers());

        if (result.isSuccess()) {
            return ResponseEntity.ok(result.getOperationText());
        } else {
            return ResponseEntity.badRequest().body(result.getError());
        }
    }

    @PostMapping("/admin-rights/{otherUserId}")
    public ResponseEntity<?> updateAdminRights(@PathVariable @ValidId long chatId, @PathVariable @ValidId long otherUserId, @RequestBody @Valid UpdateAdminRightsRequest request, @CurrentUserId long userId) {
        ResultNoArgs result = chatMemberService.updateChatMemberAdminRight(
                chatId, userId, otherUserId, request.getIsAdmin()
        );

        if (result.isSuccess()) {
            return ResponseEntity.ok(result.getOperationText());
        } else {
            return ResponseEntity.badRequest().body(result.getError());
        }
    }

    @PostMapping("/leave")
    public ResponseEntity<?> leaveChat(@PathVariable @ValidId long chatId, @CurrentUserId long userId) {

        ResultNoArgs result = chatMemberService.leaveChat(chatId, userId);

        if (result.isSuccess()) {
            return ResponseEntity.ok(result.getOperationText());
        } else {
            return ResponseEntity.badRequest().body(result.getError());
        }
    }

    @GetMapping("/members")
    public ResponseEntity<?> getChatMembersPage(@PathVariable @ValidId long chatId, @Valid PaginationRequest request, @CurrentUserId long userId) {

        ResultOneArg<ChatMembersPageDTO> result = chatMemberService.getChatMembersPage(chatId, userId, request.getCursor(), request.getLimit());

        if (result.isSuccess()) {
            return ResponseEntity.ok(result.getResult());
        } else {
            return ResponseEntity.badRequest().body(result.getError());
        }
    }
}
