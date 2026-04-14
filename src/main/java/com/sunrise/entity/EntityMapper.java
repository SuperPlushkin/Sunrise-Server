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
    public static CacheUser toCache(UserDTO user) {
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

    public static User toEntity(UserDTO user) {
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

    public static UserDTO toFullDTO(User user) {
        if (user == null) return null;

        return new UserDTO(
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
    public static UserDTO toFullDTO(CacheUser user) {
        if (user == null) return null;

        return new UserDTO(
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

    public static UserProfileDTO toUserProfileDTO(UserDTO user) {
        if (user == null) return null;

        return new UserProfileDTO(
            user.getId(),
            user.getUsername(),
            user.getName(),
            user.getProfileUpdatedAt(),
            user.getCreatedAt(),
            user.isEnabled(),
            user.getDeletedAt(),
            user.isDeleted()
        );
    }
    public static Map<Long, UserProfileDTO> toUserProfileDTOs(Collection<UserResult> items, Map<Long, UserProfileDTO> resultMap) {
        if (items == null) return resultMap;

        for (var item : items) {
            resultMap.put(item.getUserId(), new UserProfileDTO(
                    item.getUserId(),
                    item.getUsername(),
                    item.getName(),
                    item.getProfileUpdatedAt(),
                    item.getCreatedAt(),
                    item.getIsEnabled(),
                    item.getDeletedAt(),
                    item.getIsDeleted()
            ));
        }
        return resultMap;
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
    public static CacheChat toCache(ChatDTO chat) {
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
    public static CacheChat toCache(UserChatDTO chat) {
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
    public static List<CacheChat> toCaches(Collection<UserChatDTO> items) {
        if (items == null) return Collections.emptyList();

        List<CacheChat> cached = new ArrayList<>();
        for (UserChatDTO item : items) {
            cached.add(EntityMapper.toCache(item));
        }
        return cached;
    }

    public static Chat toEntity(ChatDTO chat) {
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

    public static ChatDTO toDTO(Chat chat) {
        if (chat == null) return null;

        return new ChatDTO(
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
    public static ChatDTO toDTO(CacheChat chat) {
        if (chat == null) return null;

        return new ChatDTO(
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

    public static UserChatDTO toFullDTO(UserChatResult chat) {
        if (chat == null) return null;

        MessageDTO msg = new MessageDTO(
                chat.getLastMessageId(),
                chat.getLastMessageChatId(),
                chat.getLastMessageSenderId(),
                chat.getLastMessageProfileUpdatedAt(),
                chat.getLastMessageText(),
                chat.getLastMessageReadCount(),
                chat.getLastMessageIsReadByUser(),
                chat.getLastMessageSentAt(),
                chat.getLastMessageUpdatedAt(),
                chat.getLastMessageDeletedAt(),
                chat.getLastMessageIsDeleted()
        );

        return new UserChatDTO(
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
    public static Map<Long, UserChatDTO> toFullDTOs(Collection<UserChatResult> chats, Map<Long, UserChatDTO> resultMap) {
        if (chats == null) return null;

        for (UserChatResult chat : chats){
            MessageDTO msg = new MessageDTO(
                chat.getLastMessageId(),
                chat.getLastMessageChatId(),
                chat.getLastMessageSenderId(),
                chat.getLastMessageProfileUpdatedAt(),
                chat.getLastMessageText(),
                chat.getLastMessageReadCount(),
                chat.getLastMessageIsReadByUser(),
                chat.getLastMessageSentAt(),
                chat.getLastMessageUpdatedAt(),
                chat.getLastMessageDeletedAt(),
                chat.getLastMessageIsDeleted()
            );

            resultMap.put(chat.getId(), new UserChatDTO(
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

    public static ChatMemberDTO toDTO(ChatMember member) {
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
    public static ChatMemberDTO toDTO(CacheChatMember member) {
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
    public static List<CacheChatMember> toCacheLightChatMembers(Collection<ChatMemberDTO> items) {
        if (items == null) return Collections.emptyList();

        List<CacheChatMember> cached = new ArrayList<>();
        for (ChatMemberDTO item : items) {
            cached.add(EntityMapper.toCache(item));
        }
        return cached;
    }

    public static ChatMemberProfileDTO toFullDTO(UserDTO user, ChatMemberDTO member) {
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

    public static MessageDTO toDTO(UserMessageDBResult message) {
        if (message == null) return null;

        return new MessageDTO(
            message.getId(),
            message.getChatId(),
            message.getSenderId(),
            message.getProfileUpdatedAt(),
            message.getText(),
            message.getReadCount(),
            message.getIsReadByUser() != null && message.getIsReadByUser(),
            message.getSentAt(),
            message.getUpdatedAt(),
            message.getDeletedAt(),
            message.getIsDeleted()
        );
    }


    // ========== MESSAGE READ STATUS ==========

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
}
