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
            user.getProfileUpdatedAt(),
            user.getUpdatedAt(),
            user.getCreatedAt(),
            user.getJwtVersion(),
            user.isEnabled(),
            user.getDeletedAt(),
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
            user.getProfileUpdatedAt(),
            user.getUpdatedAt(),
            user.getCreatedAt(),
            user.getJwtVersion(),
            user.isEnabled(),
            user.getDeletedAt(),
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
            user.getProfileUpdatedAt(),
            user.getUpdatedAt(),
            user.getCreatedAt(),
            user.getJwtVersion(),
            user.isEnabled(),
            user.getDeletedAt(),
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
            user.getProfileUpdatedAt(),
            user.getUpdatedAt(),
            user.getCreatedAt(),
            user.getJwtVersion(),
            user.isEnabled(),
            user.getDeletedAt(),
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
            user.getProfileUpdatedAt(),
            user.getUpdatedAt(),
            user.getCreatedAt(),
            user.getJwtVersion(),
            user.isEnabled(),
            user.getDeletedAt(),
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

    public static CacheChat toCache(Chat chat) {
        if (chat == null) return null;

        return new CacheChat(
            chat.getId(),
            chat.getName(),
            chat.getDescription(),
            chat.getChatType(),
            chat.getOpponentId(),
            chat.getMembersCount(),
            chat.getDeletedMembersCount(),
            chat.getUpdatedAt(),
            chat.getCreatedAt(),
            chat.getCreatedBy(),
            chat.getDeletedAt(),
            chat.isDeleted()
        );
    }
    public static CacheChat toCache(LightChatDTO chat) {
        if (chat == null) return null;

        return new CacheChat(
                chat.getId(),
                chat.getName(),
                chat.getDescription(),
                chat.getChatType(),
                chat.getOpponentId(),
                chat.getMembersCount(),
                chat.getDeletedMembersCount(),
                chat.getUpdatedAt(),
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
            chat.getDescription(),
            chat.getChatType(),
            chat.getOpponentId(),
            chat.getMembersCount(),
            chat.getDeletedMembersCount(),
            chat.getUpdatedAt(),
            chat.getCreatedAt(),
            chat.getCreatedBy(),
            chat.getDeletedAt(),
            chat.isDeleted()
        );
    }
    public static CacheChat toCache(UserChatResult chat) {
        if (chat == null) return null;

        return new CacheChat(
            chat.getId(),
            chat.getName(),
            chat.getDescription(),
            ChatType.valueOf(chat.getChatType()),
            chat.getOpponentId(),
            chat.getMembersCount(),
            chat.getDeletedMembersCount(),
            chat.getUpdatedAt(),
            chat.getCreatedAt(),
            chat.getCreatedBy(),
            chat.getDeletedAt(),
            chat.getIsDeleted()
        );
    }

    public static Chat toEntity(LightChatDTO chat) {
        if (chat == null) return null;

        return new Chat(
            chat.getId(),
            chat.getName(),
            chat.getDescription(),
            chat.getChatType(),
            chat.getOpponentId(),
            chat.getMembersCount(),
            chat.getDeletedMembersCount(),
            chat.getUpdatedAt(),
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
            chat.getDescription(),
            chat.getChatType(),
            chat.getOpponentId(),
            chat.getMembersCount(),
            chat.getDeletedMembersCount(),
            chat.getUpdatedAt(),
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
            chat.getDescription(),
            chat.getChatType(),
            chat.getOpponentId(),
            chat.getMembersCount(),
            chat.getDeletedMembersCount(),
            chat.getUpdatedAt(),
            chat.getCreatedAt(),
            chat.getCreatedBy(),
            chat.getDeletedAt(),
            chat.isDeleted()
        );
    }

    public static Map<Long, FullChatDTO> toFullDTOs(Collection<UserChatResult> chats, Map<Long, FullChatDTO> resultMap) {
        if (chats == null) return null;

        for (UserChatResult chat : chats){
            MessageDTO msg = new MessageDTO(
                chat.getLastMessageId(),
                chat.getLastMessageChatId(),
                chat.getLastMessageSenderId(),
                chat.getLastMessageText(),
                chat.getLastMessageReadCount(),
                chat.getLastMessageIsReadByUser(),
                chat.getLastMessageSentAt(),
                chat.getLastMessageUpdatedAt(),
                chat.getLastMessageDeletedAt(),
                chat.getLastMessageIsDeleted()
            );

            resultMap.put(chat.getId(), new FullChatDTO(
                chat.getId(),
                chat.getName(),
                chat.getDescription(),
                ChatType.valueOf(chat.getChatType()),
                chat.getOpponentId(),
                chat.getMembersCount(),
                chat.getDeletedMembersCount(),
                msg,
                chat.getUnreadMessagesCount(),
                chat.getUpdatedAt(),
                chat.getCreatedAt(),
                chat.getCreatedBy(),
                chat.getDeletedAt(),
                chat.getIsDeleted()
            ));
        }
        return resultMap;
    }
    public static FullChatDTO toFullDTO(UserChatResult chat) {
        if (chat == null) return null;

        MessageDTO msg = new MessageDTO(
            chat.getLastMessageId(),
            chat.getLastMessageChatId(),
            chat.getLastMessageSenderId(),
            chat.getLastMessageText(),
            chat.getLastMessageReadCount(),
            chat.getLastMessageIsReadByUser(),
            chat.getLastMessageSentAt(),
            chat.getLastMessageUpdatedAt(),
            chat.getLastMessageDeletedAt(),
            chat.getLastMessageIsDeleted()
        );

        return new FullChatDTO(
            chat.getId(),
            chat.getName(),
            chat.getDescription(),
            ChatType.valueOf(chat.getChatType()),
            chat.getOpponentId(),
            chat.getMembersCount(),
            chat.getDeletedMembersCount(),
            msg,
            chat.getUnreadMessagesCount(),
            chat.getUpdatedAt(),
            chat.getCreatedAt(),
            chat.getCreatedBy(),
            chat.getDeletedAt(),
            chat.getIsDeleted()
        );
    }


    // ========== CHAT MEMBER ==========

    public static CacheChatMember toCache(ChatMember member) {
        if (member == null) return null;

        return new CacheChatMember(
            member.getChatId(),
            member.getUserId(),
            member.getTag(),
            member.getSettingsUpdatedAt(),
            member.getUpdatedAt(),
            member.getJoinedAt(),
            member.isPinned(),
            member.isAdmin(),
            member.getDeletedAt(),
            member.isDeleted()
        );
    }
    public static CacheChatMember toCache(ChatMemberDTO member) {
        if (member == null) return null;

        return new CacheChatMember(
            member.getChatId(),
            member.getUserId(),
            member.getTag(),
            member.getSettingsUpdatedAt(),
            member.getUpdatedAt(),
            member.getJoinedAt(),
            member.isPinned(),
            member.isAdmin(),
            member.getDeletedAt(),
            member.isDeleted()
        );
    }

    public static ChatMember toEntity(ChatMemberDTO member) {
        if (member == null) return null;

        return new ChatMember(
            new ChatMemberId(member.getChatId(), member.getUserId()),
            member.getTag(),
            member.getSettingsUpdatedAt(),
            member.getUpdatedAt(),
            member.getJoinedAt(),
            member.isPinned(),
            member.isAdmin(),
            member.getDeletedAt(),
            member.isDeleted()
        );
    }

    public static ChatMemberDTO toLightDTO(ChatMember member) {
        if (member == null) return null;

        return new ChatMemberDTO(
            member.getChatId(),
            member.getUserId(),
            member.getTag(),
            member.getSettingsUpdatedAt(),
            member.getUpdatedAt(),
            member.getJoinedAt(),
            member.isPinned(),
            member.isAdmin(),
            member.getDeletedAt(),
            member.isDeleted()
        );
    }
    public static ChatMemberDTO toLightDTO(CacheChatMember member) {
        if (member == null) return null;

        return new ChatMemberDTO(
            member.getChatId(),
            member.getUserId(),
            member.getTag(),
            member.getSettingsUpdatedAt(),
            member.getUpdatedAt(),
            member.getJoinedAt(),
            member.isPinned(),
            member.isAdmin(),
            member.getDeletedAt(),
            member.isDeleted()
        );
    }

    public static ChatMemberProfileDTO toFullDTO(FullUserDTO user, ChatMemberDTO member) {
        if (user == null || member == null) return null;

        return new ChatMemberProfileDTO(
            member.getChatId(),
            member.getUserId(),
            user.getUsername(),
            user.getName(),
            member.getTag(),
            member.getUpdatedAt(),
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
            message.getDeletedAt(),
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
            message.getDeletedAt(),
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
            message.getDeletedAt(),
            message.isDeleted()
        );
    }

    public static Message toEntity(MessageDTO message) {
        if (message == null) return null;

        return new Message(
            message.getId(),
            message.getChatId(),
            message.getSenderId(),
            message.getText(),
            message.getReadCount(),
            message.getSentAt(),
            message.getUpdatedAt(),
            message.getDeletedAt(),
            message.isDeleted()
        );
    }

    public static MessageDTO toLightDTO(UserMessageDBResult message) {
        if (message == null) return null;

        return new MessageDTO(
            message.getId(),
            message.getChatId(),
            message.getSenderId(),
            message.getText(),
            message.getReadCount(),
            message.getIsReadByUser() != null && message.getIsReadByUser(),
            message.getSentAt(),
            message.getUpdatedAt(),
            message.getDeletedAt(),
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
            verificationToken.getTokenType(),
            verificationToken.getExpiryDate(),
            verificationToken.getCreatedAt()
        );
    }
    public static CacheVerificationToken toCache(VerificationTokenDTO verificationToken) {
        if (verificationToken == null) return null;

        return new CacheVerificationToken(
            verificationToken.getId(),
            verificationToken.getUserId(),
            verificationToken.getToken(),
            verificationToken.getTokenType(),
            verificationToken.getExpiryDate(),
            verificationToken.getCreatedAt()
        );
    }

    public static VerificationToken toEntity(VerificationTokenDTO verificationToken) {
        if (verificationToken == null) return null;

        return new VerificationToken(
            verificationToken.getId(),
            verificationToken.getUserId(),
            verificationToken.getToken(),
            verificationToken.getTokenType(),
            verificationToken.getExpiryDate(),
            verificationToken.getCreatedAt()
        );
    }

    public static VerificationTokenDTO toDTO(VerificationToken verificationToken) {
        if (verificationToken == null) return null;

        return new VerificationTokenDTO(
            verificationToken.getId(),
            verificationToken.getUserId(),
            verificationToken.getToken(),
            verificationToken.getTokenType(),
            verificationToken.getExpiryDate(),
            verificationToken.getCreatedAt()
        );
    }
    public static VerificationTokenDTO toDTO(CacheVerificationToken verificationToken) {
        if (verificationToken == null) return null;

        return new VerificationTokenDTO(
            verificationToken.getId(),
            verificationToken.getUserId(),
            verificationToken.getToken(),
            verificationToken.getTokenType(),
            verificationToken.getExpiryDate(),
            verificationToken.getCreatedAt()
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
    public static List<CacheChatMember> toCacheLightChatMembers(Collection<ChatMemberDTO> items) {
        if (items == null) return Collections.emptyList();

        List<CacheChatMember> cached = new ArrayList<>();
        for (ChatMemberDTO item : items) {
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
