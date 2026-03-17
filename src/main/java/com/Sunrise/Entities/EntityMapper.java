package com.Sunrise.Entities;

import com.Sunrise.DTO.DBResults.MessageDBResult;
import com.Sunrise.Entities.Cache.*;
import com.Sunrise.Entities.DB.*;
import com.Sunrise.Entities.DTO.*;

import java.time.LocalDateTime;
import java.util.*;

public class EntityMapper {


    // ========== VERIFICATION_TOKEN ==========

    public static CacheVerificationToken toCache(VerificationToken verificationToken) {
        if (verificationToken == null) return null;

        return new CacheVerificationToken(
            verificationToken.getId(),
            verificationToken.getToken(),
            verificationToken.getUserId(),
            verificationToken.getExpiryDate(),
            verificationToken.getCreatedAt(),
            verificationToken.getTokenType()
        );
    }
    public static CacheVerificationToken toCache(VerificationTokenDTO verificationToken) {
        if (verificationToken == null) return null;

        return new CacheVerificationToken(
            verificationToken.getId(),
            verificationToken.getToken(),
            verificationToken.getUserId(),
            verificationToken.getExpiryDate(),
            verificationToken.getCreatedAt(),
            verificationToken.getTokenType()
        );
    }

    public static VerificationToken toEntity(CacheVerificationToken verificationToken) {
        if (verificationToken == null) return null;

        return new VerificationToken(
            verificationToken.getId(),
            verificationToken.getToken(),
            verificationToken.getUserId(),
            verificationToken.getExpiryDate(),
            verificationToken.getCreatedAt(),
            verificationToken.getTokenType()
        );
    }
    public static VerificationToken toEntity(VerificationTokenDTO verificationToken) {
        if (verificationToken == null) return null;

        return new VerificationToken(
            verificationToken.getId(),
            verificationToken.getToken(),
            verificationToken.getUserId(),
            verificationToken.getExpiryDate(),
            verificationToken.getCreatedAt(),
            verificationToken.getTokenType()
        );
    }

    public static VerificationTokenDTO toDTO(VerificationToken verificationToken) {
        if (verificationToken == null) return null;

        return new VerificationTokenDTO(
            verificationToken.getId(),
            verificationToken.getToken(),
            verificationToken.getUserId(),
            verificationToken.getExpiryDate(),
            verificationToken.getCreatedAt(),
            verificationToken.getTokenType()
        );
    }
    public static VerificationTokenDTO toDTO(CacheVerificationToken verificationToken) {
        if (verificationToken == null) return null;

        return new VerificationTokenDTO(
            verificationToken.getId(),
            verificationToken.getToken(),
            verificationToken.getUserId(),
            verificationToken.getExpiryDate(),
            verificationToken.getCreatedAt(),
            verificationToken.getTokenType()
        );
    }


    // ========== CHAT ==========

    public static CacheChat toCache(Chat chat) {
        if (chat == null) return null;

        return new CacheChat(
            chat.getId(),
            chat.getName(),
            chat.getMembersCount(),
            chat.getDeletedMembersCount(),
            chat.getCreatedBy(),
            chat.getCreatedAt(),
            chat.isGroup(),
            chat.getDeletedAt(),
            chat.isDeleted()
        );
    }
    public static CacheChat toCache(LightChatDTO chat) {
        if (chat == null) return null;

        return new CacheChat(
                chat.getId(),
                chat.getName(),
                chat.getMembersCount(),
                chat.getDeletedMembersCount(),
                chat.getCreatedBy(),
                chat.getCreatedAt(),
                chat.isGroup(),
                chat.getDeletedAt(),
                chat.isDeleted()
        );
    }

    public static Chat toEntity(CacheChat chat) {
        if (chat == null) return null;

        return new Chat(
            chat.getId(),
            chat.getName(),
            chat.getMembersCount(),
            chat.getDeletedMembersCount(),
            chat.getCreatedBy(),
            chat.getCreatedAt(),
            chat.isGroup(),
            chat.getDeletedAt(),
            chat.isDeleted()
        );
    }
    public static Chat toEntity(LightChatDTO chat) {
        if (chat == null) return null;

        return new Chat(
                chat.getId(),
                chat.getName(),
                chat.getMembersCount(),
                chat.getDeletedMembersCount(),
                chat.getCreatedBy(),
                chat.getCreatedAt(),
                chat.isGroup(),
                chat.getDeletedAt(),
                chat.isDeleted()
        );
    }

    public static LightChatDTO toLightDTO(Chat chat) {
        if (chat == null) return null;

        return new LightChatDTO(
            chat.getId(),
            chat.getName(),
            chat.getMembersCount(),
            chat.getDeletedMembersCount(),
            chat.getCreatedBy(),
            chat.getCreatedAt(),
            chat.isGroup(),
            chat.getDeletedAt(),
            chat.isDeleted()
        );
    }
    public static LightChatDTO toLightDTO(CacheChat chat) {
        if (chat == null) return null;

        return new LightChatDTO(
            chat.getId(),
            chat.getName(),
            chat.getMembersCount(),
            chat.getDeletedMembersCount(),
            chat.getCreatedBy(),
            chat.getCreatedAt(),
            chat.isGroup(),
            chat.getDeletedAt(),
            chat.isDeleted()
        );
    }

    public static FullChatDTO toFullGroupChatDTO(LightChatDTO chat, LightMessageDTO message) {
        if (chat == null) return null;

        return new FullChatDTO(
            chat.getId(),
            chat.getName(),
            chat.getMembersCount(),
            chat.getDeletedMembersCount(),
            chat.isGroup(),
            chat.getCreatedBy(),
            chat.getCreatedAt(),
            null,
            message,
            chat.isDeleted(),
            chat.getDeletedAt()
        );
    }
    public static FullChatDTO toFullPersonalChatDTO(LightChatDTO chat, LightUserDTO user, LightMessageDTO message) {
        if (chat == null || user == null) return null;

        return new FullChatDTO(
            chat.getId(),
            user.getUsername(),
            chat.getMembersCount(),
            chat.getDeletedMembersCount(),
            chat.isGroup(),
            chat.getCreatedBy(),
            chat.getCreatedAt(),
            user.getId(),
            message,
            chat.isDeleted(),
            chat.getDeletedAt()
        );
    }


    // ========== USER ==========

    public static CacheUser toCache(User user) {
        if (user == null) return null;

        return new CacheUser(
            user.getId(),
            user.getUsername(),
            user.getName(),
            user.getEmail(),
            user.getHashPassword(),
            user.getLastLogin(),
            user.getCreatedAt(),
            user.isEnabled(),
            user.isDeleted()
        );
    }
    public static CacheUser toCache(FullUserDTO user) {
        if (user == null) return null;

        return new CacheUser(
                user.getId(),
                user.getUsername(),
                user.getName(),
                user.getEmail(),
                user.getHashPassword(),
                user.getLastLogin(),
                user.getCreatedAt(),
                user.isEnabled(),
                user.isDeleted()
        );
    }
    public static User toEntity(FullUserDTO user) {
        if (user == null) return null;

        return new User(
            user.getId(),
            user.getUsername(),
            user.getName(),
            user.getEmail(),
            user.getHashPassword(),
            user.getLastLogin(),
            user.getCreatedAt(),
            user.isEnabled(),
            user.isDeleted()
        );
    }

    public static LightUserDTO toLightDTO(User user) {
        if (user == null) return null;

        return new LightUserDTO(
            user.getId(),
            user.getUsername(),
            user.getName()
        );
    }
    public static LightUserDTO toLightDTO(CacheUser user) {
        if (user == null) return null;

        return new LightUserDTO(
            user.getId(),
            user.getUsername(),
            user.getName()
        );
    }

    public static FullUserDTO toFullDTO(User user) {
        if (user == null) return null;

        return new FullUserDTO(
            user.getId(),
            user.getUsername(),
            user.getName(),
            user.getEmail(),
            user.getHashPassword(),
            user.getLastLogin(),
            user.getCreatedAt(),
            user.isEnabled(),
            user.isDeleted()
        );
    }
    public static FullUserDTO toFullDTO(CacheUser user) {
        if (user == null) return null;

        return new FullUserDTO(
            user.getId(),
            user.getUsername(),
            user.getName(),
            user.getEmail(),
            user.getHashPassword(),
            user.getLastLogin(),
            user.getCreatedAt(),
            user.isEnabled(),
            user.isDeleted()
        );
    }


    // ========== CHAT_MEMBER ==========

    public static CacheChatMember toCache(ChatMember member) {
        if (member == null) return null;

        return new CacheChatMember(
            member.getChatId(),
            member.getUserId(),
            member.getJoinedAt(),
            member.isAdmin(),
            member.isDeleted()
        );
    }
    public static CacheChatMember toCache(LightChatMemberDTO member) {
        if (member == null) return null;

        return new CacheChatMember(
            member.getChatId(),
            member.getUserId(),
            member.getJoinedAt(),
            member.isAdmin(),
            member.isDeleted()
        );
    }
    public static ChatMember toEntity(CacheChatMember member) {
        if (member == null) return null;

        return new ChatMember(
            new ChatMemberId(member.getChatId(), member.getUserId()),
            member.getJoinedAt(),
            member.isAdmin(),
            member.isDeleted()
        );
    }
    public static ChatMember toEntity(LightChatMemberDTO member) {
        if (member == null) return null;

        return new ChatMember(
            new ChatMemberId(member.getChatId(), member.getUserId()),
            member.getJoinedAt(),
            member.isAdmin(),
            member.isDeleted()
        );
    }

    public static LightChatMemberDTO toLightDTO(ChatMember member) {
        if (member == null) return null;

        return new LightChatMemberDTO(
            member.getUserId(),
            member.getChatId(),
            member.getJoinedAt(),
            member.isAdmin(),
            member.isActive()
        );
    }
    public static LightChatMemberDTO toLightDTO(CacheChatMember member) {
        if (member == null) return null;

        return new LightChatMemberDTO(
            member.getUserId(),
            member.getChatId(),
            member.getJoinedAt(),
            member.isAdmin(),
            member.isActive()
        );
    }

    public static FullChatMemberDTO toFullDTO(FullUserDTO user, LightChatMemberDTO member) {
        if (user == null || member == null) return null;

        return new FullChatMemberDTO(
            member.getUserId(),
            member.getChatId(),
            user.getUsername(),
            user.getName(),
            member.getJoinedAt(),
            member.isAdmin(),
            member.isDeleted(),
            user.isDeleted()
        );
    }


    // ========== MESSAGE ==========

    public static LightMessageDTO toLightDTO(CacheMessage message, long currentUserId) {
        if (message == null) return null;

        return new LightMessageDTO(
            message.getId(),
            message.getSenderId(),
            message.getChatId(),
            message.getText(),
            message.getSentAt(),
            message.getReadCount(),
            message.isReadByUser(currentUserId),
            message.isReadByExcludeUser(currentUserId),
            message.isHiddenByAdmin()
        );
    }
    public static LightMessageDTO toLightDTO(MessageDBResult message, long currentUserId, long chatId) {
        if (message == null) return null;

        return new LightMessageDTO(
            message.getMessageId(),
            message.getSenderId(),
            chatId,
            message.getText(),
            LocalDateTime.parse(message.getSentAt()),
            message.getReadCount(),
            Arrays.stream(message.getReadByUsers()).anyMatch(id -> id == currentUserId),
            Arrays.stream(message.getReadByUsers()).anyMatch(id -> id != currentUserId),
            message.getIsHiddenByAdmin()
        );
    }
    public static Message toEntity(FullMessageDTO message) {
        if (message == null) return null;

        return new Message(
            message.getId(),
            message.getSenderId(),
            message.getChatId(),
            message.getText(),
            message.getSentAt(),
            message.getReadCount(),
            message.isHiddenByAdmin()
        );
    }

    public static CacheMessage toCache(Message message) {
        if (message == null) return null;

        return new CacheMessage(
            message.getId(),
            message.getSenderId(),
            message.getChatId(),
            message.getText(),
            message.getSentAt(),
            message.getReadCount(),
            Collections.emptySet(),
            message.isHiddenByAdmin()
        );
    }
    public static CacheMessage toCache(FullMessageDTO message) {
        if (message == null) return null;

        return new CacheMessage(
            message.getId(),
            message.getSenderId(),
            message.getChatId(),
            message.getText(),
            message.getSentAt(),
            message.getReadCount(),
            message.getReadByUsers(),
            message.isHiddenByAdmin()
        );
    }
    public static CacheMessage toCache(MessageDBResult message, long chatId) {
        if (message == null) return null;

        return new CacheMessage(
            message.getMessageId(),
            message.getSenderId(),
            chatId,
            message.getText(),
            LocalDateTime.parse(message.getSentAt()),
            message.getReadCount(),
            List.of(message.getReadByUsers()),
            message.getIsHiddenByAdmin()
        );
    }


    // ========== BATCH MAPPING ==========

    public static List<CacheChatMember> toCacheMembers(List<ChatMember> members) {
        if (members == null)
            return Collections.emptyList();

        return members.stream().map(EntityMapper::toCache).toList();
    }
    public static List<CacheChat> toCacheChats(List<Chat> chats) {
        if (chats == null) return Collections.emptyList();

        return chats.stream().map(EntityMapper::toCache).toList();
    }
    public static List<Chat> toEntityChats(List<CacheChat> chats) {
        if (chats == null) return Collections.emptyList();

        return chats.stream().map(EntityMapper::toEntity).toList();
    }
}
