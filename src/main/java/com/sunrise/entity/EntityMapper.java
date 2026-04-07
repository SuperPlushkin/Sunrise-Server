package com.sunrise.entity;

import com.sunrise.core.dataservice.type.*;
import com.sunrise.entity.cache.*;
import com.sunrise.entity.db.*;
import com.sunrise.entity.dto.*;

import java.util.*;

public class EntityMapper {


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


    public static UserProfileDTO toUserProfileDTO(FullUserDTO user) {
        if (user == null) return null;

        return new UserProfileDTO(
                user.getId(),
                user.getUsername(),
                user.getName(),
                user.getCreatedAt()
        );
    }


    // ========== CHAT ==========

    public static CacheChat toCache(LightChatDTO chat) {
        if (chat == null) return null;

        return new CacheChat(
            chat.getId(),
            chat.getName(),
            chat.isGroup(),
            chat.getOpponentId(),
            chat.getMembersCount(),
            chat.getDeletedMembersCount(),
            chat.getCreatedAt(),
            chat.getCreatedBy(),
            chat.getDeletedAt(),
            chat.isDeleted()
        );
    }
    public static CacheChat toCache(Chat chat) {
        if (chat == null) return null;

        return new CacheChat(
            chat.getId(),
            chat.getName(),
            chat.isGroup(),
            chat.getOpponentId(),
            chat.getMembersCount(),
            chat.getDeletedMembersCount(),
            chat.getCreatedAt(),
            chat.getCreatedBy(),
            chat.getDeletedAt(),
            chat.isDeleted()
        );
    }
    public static CacheChat toCache(FullChatDTO chat) {
        if (chat == null) return null;

        return new CacheChat(
            chat.getId(),
            chat.getName(),
            chat.isGroup(),
            chat.getOpponentId(),
            chat.getMembersCount(),
            chat.getDeletedMembersCount(),
            chat.getCreatedAt(),
            chat.getCreatedBy(),
            chat.getDeletedAt(),
            chat.isDeleted()
        );
    }

    public static Chat toEntity(LightChatDTO chat) {
        if (chat == null) return null;

        return new Chat(
            chat.getId(),
            chat.getName(),
            chat.isGroup(),
            chat.getOpponentId(),
            chat.getMembersCount(),
            chat.getDeletedMembersCount(),
            chat.getCreatedAt(),
            chat.getCreatedBy(),
            chat.getDeletedAt(),
            chat.isDeleted()
        );
    }

    public static LightChatDTO toLightDTO(Chat chat) {
        if (chat == null) return null;

        return new LightChatDTO(
            chat.getId(),
            chat.getName(),
            chat.isGroup(),
            chat.getOpponentId(),
            chat.getMembersCount(),
            chat.getDeletedMembersCount(),
            chat.getCreatedAt(),
            chat.getCreatedBy(),
            chat.getDeletedAt(),
            chat.isDeleted()
        );
    }
    public static LightChatDTO toLightDTO(CacheChat chat) {
        if (chat == null) return null;

        return new LightChatDTO(
            chat.getId(),
            chat.getName(),
            chat.isGroup(),
            chat.getOpponentId(),
            chat.getMembersCount(),
            chat.getDeletedMembersCount(),
            chat.getCreatedAt(),
            chat.getCreatedBy(),
            chat.getDeletedAt(),
            chat.isDeleted()
        );
    }

    public static Map<Long, FullChatDTO> toFullDTOs(Collection<UserFullChatResult> chats, Map<Long, FullChatDTO> resultMap) {
        if (chats == null) return null;

        for (UserFullChatResult chat : chats){
            MessageDTO msg = new MessageDTO(
                chat.getLastMessageId(),
                chat.getLastMessageChatId(),
                chat.getLastMessageSenderId(),
                chat.getLastMessageText(),
                chat.getLastMessageSentAt(),
                chat.getLastMessageReadCount(),
                chat.getLastMessageIsReadByUser(),
                chat.getLastMessageIsDeleted()
            );

            resultMap.put(chat.getId(), new FullChatDTO(
                chat.getId(),
                chat.getName(),
                chat.getIsGroup(),
                chat.getOpponentId(),
                chat.getMembersCount(),
                chat.getDeletedMembersCount(),
                msg,
                chat.getCreatedAt(),
                chat.getCreatedBy(),
                chat.getDeletedAt(),
                chat.getIsDeleted()
            ));
        }
        return resultMap;
    }


    // ========== CHAT MEMBER ==========

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
    public static CacheChatMember toCache(FullChatMemberDTO member) {
        if (member == null) return null;

        return new CacheChatMember(
            member.getChatId(),
            member.getUserId(),
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
            member.getChatId(),
            member.getUserId(),
            member.getJoinedAt(),
            member.isAdmin(),
            member.isDeleted()
        );
    }
    public static LightChatMemberDTO toLightDTO(CacheChatMember member) {
        if (member == null) return null;

        return new LightChatMemberDTO(
            member.getChatId(),
            member.getUserId(),
            member.getJoinedAt(),
            member.isAdmin(),
            member.isDeleted()
        );
    }

    public static FullChatMemberDTO toFullDTO(FullUserDTO user, LightChatMemberDTO member) {
        if (user == null || member == null) return null;

        return new FullChatMemberDTO(
            member.getChatId(),
            member.getUserId(),
            user.getUsername(),
            user.getName(),
            member.getJoinedAt(),
            member.isAdmin(),
            member.isDeleted(),
            user.isDeleted()
        );
    }


    // ========== MESSAGE ==========

    public static CacheMessage toCache(Message message) {
        if (message == null) return null;

        return new CacheMessage(
            message.getId(),
            message.getChatId(),
            message.getSenderId(),
            message.getSentAt(),
            message.isDeleted()
        );
    }
    public static CacheMessage toCache(UserMessageDBResult message) {
        if (message == null) return null;

        return new CacheMessage(
                message.getId(),
                message.getChatId(),
                message.getSenderId(),
                message.getSentAt(),
                message.getIsDeleted()
        );
    }
    public static CacheMessage toCache(MessageDTO message) {
        if (message == null) return null;

        return new CacheMessage(
            message.getId(),
            message.getChatId(),
            message.getSenderId(),
            message.getSentAt(),
            message.isHiddenByAdmin()
        );
    }

    public static Message toEntity(MessageDTO message) {
        if (message == null) return null;

        return new Message(
            message.getId(),
            message.getChatId(),
            message.getSenderId(),
            message.getText(),
            message.getSentAt(),
            message.getReadCount(),
            message.isHiddenByAdmin()
        );
    }

    public static MessageDTO toLightDTO(UserMessageDBResult message) {
        if (message == null) return null;

        return new MessageDTO(
            message.getId(),
            message.getChatId(),
            message.getSenderId(),
            message.getText(),
            message.getSentAt(),
            message.getReadCount(),
            message.getIsReadByUser() != null && message.getIsReadByUser(),
            message.getIsDeleted()
        );
    }


    // ========== VERIFICATION_TOKEN ==========

    public static CacheVerificationToken toCache(VerificationToken verificationToken) {
        if (verificationToken == null) return null;

        return new CacheVerificationToken(
                verificationToken.getId(),
                verificationToken.getUserId(),
                verificationToken.getToken(),
                verificationToken.getExpiryDate(),
                verificationToken.getCreatedAt(),
                verificationToken.getTokenType()
        );
    }
    public static CacheVerificationToken toCache(VerificationTokenDTO verificationToken) {
        if (verificationToken == null) return null;

        return new CacheVerificationToken(
                verificationToken.getId(),
                verificationToken.getUserId(),
                verificationToken.getToken(),
                verificationToken.getExpiryDate(),
                verificationToken.getCreatedAt(),
                verificationToken.getTokenType()
        );
    }

    public static VerificationToken toEntity(VerificationTokenDTO verificationToken) {
        if (verificationToken == null) return null;

        return new VerificationToken(
                verificationToken.getId(),
                verificationToken.getUserId(),
                verificationToken.getToken(),
                verificationToken.getExpiryDate(),
                verificationToken.getCreatedAt(),
                verificationToken.getTokenType()
        );
    }

    public static VerificationTokenDTO toDTO(VerificationToken verificationToken) {
        if (verificationToken == null) return null;

        return new VerificationTokenDTO(
                verificationToken.getId(),
                verificationToken.getUserId(),
                verificationToken.getToken(),
                verificationToken.getExpiryDate(),
                verificationToken.getCreatedAt(),
                verificationToken.getTokenType()
        );
    }
    public static VerificationTokenDTO toDTO(CacheVerificationToken verificationToken) {
        if (verificationToken == null) return null;

        return new VerificationTokenDTO(
                verificationToken.getId(),
                verificationToken.getUserId(),
                verificationToken.getToken(),
                verificationToken.getExpiryDate(),
                verificationToken.getCreatedAt(),
                verificationToken.getTokenType()
        );
    }


    // ========== LOGIN HISTORY ==========

    public static LoginHistory toEntity(LoginHistoryDTO loginHistory) {
        if (loginHistory == null) return null;

        return new LoginHistory(
            loginHistory.getId(),
            loginHistory.getUserId(),
            loginHistory.getIpAddress(),
            loginHistory.getDeviceInfo(),
            loginHistory.getLoginAt()
        );
    }


    // ========== BATCH MAPPING ==========

    public static List<CacheChat> toCacheChats(Collection<FullChatDTO> items) {
        if (items == null) return Collections.emptyList();

        List<CacheChat> cached = new ArrayList<>();
        for (FullChatDTO item : items) {
            cached.add(EntityMapper.toCache(item));
        }
        return cached;
    }
    public static List<CacheChatMember> toCacheLightChatMembers(Collection<LightChatMemberDTO> items) {
        if (items == null) return Collections.emptyList();

        List<CacheChatMember> cached = new ArrayList<>();
        for (LightChatMemberDTO item : items) {
            cached.add(EntityMapper.toCache(item));
        }
        return cached;
    }

    public static Map<Long, LightUserDTO> toLightUserDTOs(Collection<UserResult> items, Map<Long, LightUserDTO> resultMap) {
        if (items == null) return resultMap;

        for (var item : items) {
            resultMap.put(item.getUserId(), new LightUserDTO(
                item.getUserId(),
                item.getUsername(),
                item.getName()
            ));
        }
        return resultMap;
    }
    public static Map<Long, MessageReadStatusDTO> toMessageReadDTOs(Collection<MessageReadStatusResult> items, Map<Long, MessageReadStatusDTO> resultMap) {
        if (items == null) return resultMap;

        for (var item : items) {
            resultMap.put(item.getUserId(), new MessageReadStatusDTO(
                item.getUserId(),
                item.getReadAt()
            ));
        }
        return resultMap;
    }
}
