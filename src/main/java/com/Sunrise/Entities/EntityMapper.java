package com.Sunrise.Entities;

import com.Sunrise.DTOs.DBResults.ChatOpponentResult;
import com.Sunrise.DTOs.DBResults.FullChatResult;
import com.Sunrise.DTOs.DBResults.MessageDBResult;
import com.Sunrise.DTOs.Paginations.ChatMemberResult;
import com.Sunrise.DTOs.Paginations.UserFullChatResult;
import com.Sunrise.DTOs.Paginations.UserMessageDBResult;
import com.Sunrise.DTOs.Paginations.UserResult;
import com.Sunrise.Entities.Caches.*;
import com.Sunrise.Entities.DBs.*;
import com.Sunrise.Entities.DTOs.*;

import java.time.LocalDateTime;
import java.util.*;

public class EntityMapper {


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


    // ========== CHAT ==========

    public static CacheChat toCache(LightChatDTO chat, Message message) {
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
            chat.isDeleted(),
            toCache(message),
            chat.getMessagesCount(),
            chat.getDeletedMessagesCount()
        );
    }
    public static CacheChat toCache(UserFullChatResult fullChat) {
        if (fullChat == null) return null;

        return new CacheChat(
            fullChat.getId(),
            fullChat.getName(),
            fullChat.getIsGroup(),
            fullChat.getOpponentId(),
            fullChat.getMembersCount(),
            fullChat.getDeletedMembersCount(),
            fullChat.getCreatedAt(),
            fullChat.getCreatedBy(),
            fullChat.getDeletedAt(),
            fullChat.getIsDeleted(),
            toCache(fullChat.getLastMessage()),
            fullChat.getMessagesCount(),
            fullChat.getDeletedMessagesCount()
        );
    }
    public static CacheChat toCache(FullChatResult fullChat) {
        if (fullChat == null) return null;

        return new CacheChat(
            fullChat.getId(),
            fullChat.getName(),
            fullChat.getIsGroup(),
            fullChat.getOpponentId(),
            fullChat.getMembersCount(),
            fullChat.getDeletedMembersCount(),
            fullChat.getCreatedAt(),
            fullChat.getCreatedBy(),
            fullChat.getDeletedAt(),
            fullChat.getIsDeleted(),
            toCache(fullChat.getLastMessage()),
            fullChat.getMessagesCount(),
            fullChat.getDeletedMessagesCount()
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
            chat.isDeleted(),
            toCache(chat.getLastMessage()),
            chat.getMessagesCount(),
            chat.getDeletedMessagesCount()
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

    public static LightChatDTO toLightDTO(UserFullChatResult fullChat) {
        if (fullChat == null) return null;

        return new LightChatDTO(
            fullChat.getId(),
            fullChat.getName(),
            fullChat.getIsGroup(),
            fullChat.getOpponentId(),
            fullChat.getMembersCount(),
            fullChat.getDeletedMembersCount(),
            fullChat.getMessagesCount(),
            fullChat.getDeletedMessagesCount(),
            fullChat.getCreatedAt(),
            fullChat.getCreatedBy(),
            fullChat.getDeletedAt(),
            fullChat.getIsDeleted()
        );
    }
    public static LightChatDTO toLightDTO(FullChatResult fullChat) {
        if (fullChat == null) return null;

        return new LightChatDTO(
            fullChat.getId(),
            fullChat.getName(),
            fullChat.getIsGroup(),
            fullChat.getOpponentId(),
            fullChat.getMembersCount(),
            fullChat.getDeletedMembersCount(),
            fullChat.getMessagesCount(),
            fullChat.getDeletedMessagesCount(),
            fullChat.getCreatedAt(),
            fullChat.getCreatedBy(),
            fullChat.getDeletedAt(),
            fullChat.getIsDeleted()
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
            chat.getMessagesCount(),
            chat.getDeletedMessagesCount(),
            chat.getCreatedAt(),
            chat.getCreatedBy(),
            chat.getDeletedAt(),
            chat.isDeleted()
        );
    }

    public static FullChatDTO toFullGroupChatDTO(LightChatDTO chat, LightMessageDTO message) {
        if (chat == null) return null;

        return new FullChatDTO(
            chat.getId(),
            chat.getName(),
            chat.isGroup(),
            null,
            chat.getMembersCount(),
            chat.getDeletedMembersCount(),
            message,
            chat.getMessagesCount(),
            chat.getDeletedMessagesCount(),
            chat.getCreatedAt(),
            chat.getCreatedBy(),
            chat.getDeletedAt(),
            chat.isDeleted()
        );
    }
    public static FullChatDTO toFullPersonalChatDTO(LightChatDTO chat, LightUserDTO opponent, long userId, LightMessageDTO message) {
        if (chat == null || opponent == null) return null;

        return new FullChatDTO(
            chat.getId(),
            opponent.getName(),
            chat.isGroup(),
            chat.getCreatedBy() == userId ? opponent.getId() : userId,
            chat.getMembersCount(),
            chat.getDeletedMembersCount(),
            message,
            chat.getMessagesCount(),
            chat.getDeletedMessagesCount(),
            chat.getCreatedAt(),
            chat.getCreatedBy(),
            chat.getDeletedAt(),
            chat.isDeleted()
        );
    }
    public static Map<Long, FullChatDTO> toFullDTOs(Collection<UserFullChatResult> chats, Map<Long, FullChatDTO> resultMap) {
        if (chats == null) return null;

        for (UserFullChatResult chat : chats){
            resultMap.put(chat.getId(), new FullChatDTO(
                chat.getId(),
                chat.getName(),
                chat.getIsGroup(),
                chat.getOpponentId(),
                chat.getMembersCount(),
                chat.getDeletedMembersCount(),
                toLightDTO(chat.getLastMessage()),
                chat.getMessagesCount(),
                chat.getDeletedMessagesCount(),
                chat.getCreatedAt(),
                chat.getCreatedBy(),
                chat.getDeletedAt(),
                chat.getIsDeleted()
            ));
        }
        return resultMap;
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
    public static CacheUser toCache(ChatOpponentResult user) {
        if (user == null) return null;

        return new CacheUser(
            user.getUserId(),
            user.getUsername(),
            user.getName(),
            user.getEmail(),
            user.getHashPassword(),
            user.getLastLogin(),
            user.getCreatedAt(),
            user.getIsEnabled(),
            user.getIsDeleted()
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

    public static LightUserDTO toLightDTO(CacheUser user) {
        if (user == null) return null;

        return new LightUserDTO(
            user.getId(),
            user.getUsername(),
            user.getName()
        );
    }
    public static LightUserDTO toCensoredLightUserDTO(ChatOpponentResult user) {
        if (user == null) return null;

        if (user.getIsDeleted()) {
            return new LightUserDTO(
                user.getUserId(),
                "Deleted user",
                "Deleted user"
            );
        }

        return new LightUserDTO(
            user.getUserId(),
            user.getUsername(),
            user.getName()
        );
    }
    public static Map<Long, LightUserDTO> toLightDTOs(Collection<UserResult> users, Map<Long, LightUserDTO> resultMap) {
        if (users == null) return null;

        for (UserResult user : users){
            resultMap.put(user.getUserId(), new LightUserDTO(
                user.getUserId(),
                user.getUsername(),
                user.getName()
            ));
        }
        return resultMap;
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
    public static Map<Long, FullChatMemberDTO> toFullDTOs(Collection<ChatMemberResult> members, long chatId, Map<Long, FullChatMemberDTO> resultMap) {
        if (members == null) return resultMap;

        for (ChatMemberResult member : members){
            resultMap.put(member.getUserId(), new FullChatMemberDTO(
                chatId,
                member.getUserId(),
                member.getUsername(),
                member.getName(),
                member.getJoinedAt(),
                member.getIsAdmin(),
                false,
                member.getUserIsDeleted()
            ));
        }
        return resultMap;
    }


    // ========== MESSAGE ==========

    public static CacheMessage toCache(Message message) {
        if (message == null) return null;

        return new CacheMessage(
            message.getId(),
            message.getChatId(),
            message.getSenderId(),
            message.getText(),
            message.getSentAt(),
            message.getReadCount(),
            message.isHiddenByAdmin()
        );
    }
    public static CacheMessage toCache(UserMessageDBResult message) {
        if (message == null) return null;

        return new CacheMessage(
            message.getId(),
            message.getChatId(),
            message.getSenderId(),
            message.getText(),
            message.getSentAt(),
            message.getReadCount(),
            message.getIsHiddenByAdmin()
        );
    }
    private static CacheMessage toCache(MessageDBResult message) {
        if (message == null) return null;

        return new CacheMessage(
            message.getId(),
            message.getChatId(),
            message.getSenderId(),
            message.getText(),
            message.getSentAt(),
            message.getReadCount(),
            message.getIsHiddenByAdmin()
        );
    }
    public static CacheMessage toCache(LightMessageDTO message) {
        if (message == null) return null;

        return new CacheMessage(
            message.getId(),
            message.getChatId(),
            message.getSenderId(),
            message.getText(),
            message.getSentAt(),
            message.getReadCount(),
            message.isHiddenByAdmin()
        );
    }

    public static Message toEntity(LightMessageDTO message) {
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

    public static LightMessageDTO toLightDTO(CacheMessage message, boolean isReadByUser) {
        if (message == null) return null;

        return new LightMessageDTO(
            message.getId(),
            message.getChatId(),
            message.getSenderId(),
            message.getText(),
            message.getSentAt(),
            message.getReadCount(),
            isReadByUser,
            message.isHiddenByAdmin()
        );
    }
    public static LightMessageDTO toLightDTO(UserMessageDBResult message) {
        if (message == null) return null;

        return new LightMessageDTO(
            message.getId(),
            message.getChatId(),
            message.getSenderId(),
            message.getText(),
            message.getSentAt(),
            message.getReadCount(),
            message.getIsReadByUser() != null && message.getIsReadByUser(),
            message.getIsHiddenByAdmin()
        );
    }


    // ========== VERIFICATION_TOKEN ==========

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

    public static List<CacheMessage> toCacheMessages(List<UserMessageDBResult> list) {
        if (list == null) return Collections.emptyList();

        List<CacheMessage> cached = new ArrayList<>();
        for (UserMessageDBResult row : list) {
            cached.add(EntityMapper.toCache(row));
        }
        return cached;
    }
    public static List<CacheChatMember> toCacheChatMembers(List<LightChatMemberDTO> list) {
        if (list == null) return Collections.emptyList();

        List<CacheChatMember> cached = new ArrayList<>();
        for (LightChatMemberDTO row : list) {
            cached.add(EntityMapper.toCache(row));
        }
        return cached;
    }
}
