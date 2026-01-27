package com.Sunrise.Services.DataServices.Interfaces;

import com.Sunrise.Entities.Chat;
import com.Sunrise.Entities.VerificationToken;

import java.util.Optional;

public interface IStorageService {


    // ========== USER METHODS ==========

    boolean existsUser(Long userId);
    void deleteUser(Long userId);


    // ========== CHAT METHODS ==========

    boolean existsChat(Long chatId);
    boolean isUserInChat(Long chatId, Long userId);
    void removeUserFromChat(Long userId, Long chatId);
    void saveChat(Chat chat);
    void deleteChat(Long chatId);


    // ========== VERIFICATION TOKEN METHODS ==========

    Optional<VerificationToken> getVerificationToken(String token);
    void saveVerificationToken(VerificationToken token);
    void deleteVerificationToken(String token);
    int cleanupExpiredVerificationTokens();
}
