package com.Sunrise.Entities;

import com.Sunrise.Entities.Cache.CacheVerificationToken;
import com.Sunrise.Entities.DB.*;
import com.Sunrise.Entities.DTO.*;
import com.Sunrise.Entities.Cache.CacheChat;
import com.Sunrise.Entities.Cache.CacheChatMember;
import com.Sunrise.Entities.Cache.CacheUser;

import java.util.Collections;
import java.util.List;

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
    public static CacheChat toCache(ChatDTO chat) {
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

    public static ChatDTO toDTO(Chat chat) {
        if (chat == null) return null;

        return new ChatDTO(
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
    public static ChatDTO toDTO(CacheChat chat) {
        if (chat == null) return null;

        return new ChatDTO(
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
    public static User toEntity(CacheUser user) {
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
    public static ChatMember toEntity(CacheChatMember member) {
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
                member.getJoinedAt(),
                member.isAdmin(),
                member.isActive()
        );
    }
    public static LightChatMemberDTO toLightDTO(CacheChatMember member) {
        if (member == null) return null;

        return new LightChatMemberDTO(
            member.getUserId(),
            member.getJoinedAt(),
            member.isAdmin(),
            member.isActive()
        );
    }

    public static FullChatMemberDTO toFullDTO(FullUserDTO user, LightChatMemberDTO member) {
        if (user == null || member == null) return null;

        return new FullChatMemberDTO(
            member.getUserId(),
            user.getUsername(),
            user.getName(),
            member.getJoinedAt(),
            member.getIsAdmin(),
            member.getIsDeleted(),
            user.isDeleted()
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
