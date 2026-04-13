package com.sunrise.core.service;

import com.sunrise.core.dataservice.type.TokenType;
import com.sunrise.core.service.result.*;
import com.sunrise.entity.dto.FullUserDTO;
import com.sunrise.entity.dto.LoginHistoryDTO;
import com.sunrise.entity.dto.VerificationTokenDTO;
import com.sunrise.core.dataservice.DataOrchestrator;
import com.sunrise.config.jwt.JwtUtil;
import com.sunrise.core.dataservice.LockManager;
import com.sunrise.core.notifier.EmailNotifier;
import com.sunrise.helpclass.SimpleSnowflakeId;
import com.sunrise.helpclass.ValidationException;

import jakarta.servlet.http.HttpServletRequest;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Optional;

@Slf4j
@RequiredArgsConstructor
@Service
public class AuthService {

    private final EmailNotifier emailNotifier;
    private final DataOrchestrator dataOrchestrator;
    private final LockManager lockManager;
    private final JwtUtil jwtUtil;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public ResultOneArg<String> registerUser(String username, String name, String email, String password) {

        // пытаемся заблокировать регистрацию
        if (!lockManager.tryLockRegistration(username, email))
            return ResultOneArg.error("Try again later");

        try {
            if (dataOrchestrator.existsUserByUsername(username.trim()))
                throw new ValidationException("Username already exists");

            if (dataOrchestrator.existsUserByEmail(email.toLowerCase()))
                throw new ValidationException("Email already exists");

            LocalDateTime createdAt = LocalDateTime.now();

            FullUserDTO user = FullUserDTO.create(
                SimpleSnowflakeId.nextId(), username, name, email,
                passwordEncoder.encode(password), createdAt
            );
            dataOrchestrator.saveUser(user);

            String token = generate64CharString();
            TokenType tokenType = TokenType.EMAIL_UPDATE;
            VerificationTokenDTO verificationTokenDTO = new VerificationTokenDTO(
                SimpleSnowflakeId.nextId(), user.getId(), token, tokenType, createdAt, 24 // 24 часа
            );
            dataOrchestrator.saveVerificationToken(verificationTokenDTO);

            // отправляем подтверждение активации аккаунта на почту
            emailNotifier.sendVerificationTokenMail(email, tokenType, token);

            log.info("[🔧] ✅ User registered successfully --> {}", username);
            return ResultOneArg.success("User registered successfully. Check your mail to activate your account!!!");
        }
        catch (ValidationException e) {
            log.warn("[🔧] ☝️ Failed to register user: {}", e.getMessage());
            return ResultOneArg.error(e.getMessage());
        }
        catch (Exception e) {
            log.error("[🔧] ⚠️ Registration failed for user {}: {}", username, e.getMessage());
            return ResultOneArg.error("registerUser failed due to server error");
        }
        finally {
            lockManager.unLockRegistration(username, email); // разблокируем регистрацию
        }
    } // TODO: ПРОБЛЕМА С ВЫЗОВОМ НЕСКОЛЬКИХ ФУНКЦИЙ ПО СОХРАНЕНИЮ В БД И КЕШ
    public ResultOneArg<UserLoginResult> authenticateUser(String username, String password, HttpServletRequest httpRequest) {
        try {
            Optional<FullUserDTO> userOpt = dataOrchestrator.getUserByUsername(username);
            if (userOpt.isEmpty())
                throw new ValidationException("Invalid username or password");

            FullUserDTO user = userOpt.get();
            if (!user.isEnabled())
                throw new ValidationException("Please verify your email first");

            if(!passwordEncoder.matches(password, user.getHashPassword()))
                throw new ValidationException("Invalid username or password");

            LocalDateTime updatedAt = LocalDateTime.now();
            dataOrchestrator.updateLastLogin(username, updatedAt);

            var loginHistory = LoginHistoryDTO.create(
                SimpleSnowflakeId.nextId(), user.getId(),
                extractClientIp(httpRequest), httpRequest.getHeader("User-Agent"), updatedAt
            );
            dataOrchestrator.saveLoginHistory(loginHistory);

            String token = jwtUtil.generateToken(user.getId(), user.getJwtVersion());

            log.info("[🔧] ✅ User logged in successfully --> {}", username);
            return ResultOneArg.success(new UserLoginResult(token, jwtUtil.getTokenExpirationTime(token)));
        }
        catch (ValidationException e) {
            log.warn("[🔧] ☝️ Failed to authenticate user: {}", e.getMessage());
            return ResultOneArg.error(e.getMessage());
        }
        catch (Exception e) {
            log.error("[🔧] ⚠️ Error on authentication for user {}: {}", username, e.getMessage());
            return ResultOneArg.error("authenticateUser failed due to server error");
        }
    }

    public ResultNoArgs requestEmailUpdate(long userId, String newEmail) {
        try {
            FullUserDTO user = dataOrchestrator.getUser(userId)
                    .orElseThrow(() -> new ValidationException("User not found"));

            if (dataOrchestrator.existsUserByEmail(newEmail)){
                throw new ValidationException("Email already taken");
            }

            // Генерация токена
            String token = generate64CharString();
            TokenType tokenType = TokenType.EMAIL_UPDATE;
            LocalDateTime now = LocalDateTime.now();
            VerificationTokenDTO verificationToken = new VerificationTokenDTO(
                SimpleSnowflakeId.nextId(), userId, token, tokenType, now, 24 // 24 часа
            );
            dataOrchestrator.saveVerificationToken(verificationToken);

            // Отправляем письмо на старый email
            emailNotifier.sendVerificationTokenMail(user.getEmail(), tokenType, token);
            return ResultNoArgs.success();
        }
        catch (ValidationException e) {
            log.warn("[🔧] ☝️ Failed to send email update token: {}", e.getMessage());
            return ResultNoArgs.error(e.getMessage());
        }
        catch (Exception e) {
            log.error("[🔧] ⚠️ Error on sending token confirmation for email update: {}", e.getMessage());
            return ResultNoArgs.error("requestEmailUpdate failed due to server error");
        }
    }
    public ResultNoArgs requestPasswordUpdate(String username) {
        try {
            FullUserDTO user = dataOrchestrator.getUserByUsername(username)
                    .orElseThrow(() -> new ValidationException("Email already exists"));

            // Генерация токена
            String token = generate64CharString();
            TokenType tokenType = TokenType.PASSWORD_UPDATE;
            LocalDateTime now = LocalDateTime.now();
            VerificationTokenDTO verificationToken = new VerificationTokenDTO(
                SimpleSnowflakeId.nextId(), user.getId(), token, tokenType, now, 1 // 1 час
            );
            dataOrchestrator.saveVerificationToken(verificationToken);

            // Отправляем письмо на старый email
            emailNotifier.sendVerificationTokenMail(user.getEmail(), tokenType, token);
            return ResultNoArgs.success();
        }
        catch (ValidationException e) {
            log.warn("[🔧] ☝️ Failed to send password reset token: {}", e.getMessage());
            return ResultNoArgs.error(e.getMessage());
        }
        catch (Exception e) {
            log.error("[🔧] ⚠️ Error on sending token confirmation for password reset: {}", e.getMessage());
            return ResultNoArgs.error("requestEmailUpdate failed due to server error");
        }
    }

    public ResultOneArg<String> confirmRegistrationToken(String token) {
        try {
            if (token == null || token.trim().isEmpty()) {
                throw new ValidationException("Token cannot be empty");
            }

            Optional<VerificationTokenDTO> tokenOpt = dataOrchestrator.getVerificationToken(token);
            if (tokenOpt.isEmpty()) {
                throw new ValidationException("Invalid token");
            }

            VerificationTokenDTO verificationToken = tokenOpt.get();
            if (verificationToken.isExpired()) {
                throw new ValidationException("Token expired");
            } else if (verificationToken.getTokenType() != TokenType.REGISTRATION) {
                throw new ValidationException("Invalid token type");
            }

            long userId = verificationToken.getUserId();
            LocalDateTime updatedAt = LocalDateTime.now();

            dataOrchestrator.enableUser(userId, updatedAt);
            dataOrchestrator.deleteVerificationToken(token);

            log.info("[🔧] ✅ Registration verified successfully for user {}", userId);
            return ResultOneArg.success("Registration successfully verified");
        }
        catch (ValidationException e) {
            log.warn("[🔧] ☝️ Failed to confirm registration token: {}", e.getMessage());
            return ResultOneArg.error(e.getMessage());
        }
        catch (Exception e) {
            log.error("[🔧] ⚠️ Error on registration token confirmation: {}", e.getMessage());
            return ResultOneArg.error("confirmRegistrationToken failed due to server error");
        }
    }
    public ResultOneArg<String> confirmEmailUpdateToken(String token, String email) {
        try {
            if (token == null || token.trim().isEmpty()) {
                throw new ValidationException("Token cannot be empty");
            }

            Optional<VerificationTokenDTO> tokenOpt = dataOrchestrator.getVerificationToken(token);
            if (tokenOpt.isEmpty()) {
                throw new ValidationException("Invalid token");
            }

            VerificationTokenDTO verificationToken = tokenOpt.get();
            if (verificationToken.isExpired()) {
                throw new ValidationException("Token expired");
            } else if (verificationToken.getTokenType() != TokenType.EMAIL_UPDATE) {
                throw new ValidationException("Invalid token type");
            }

            long userId = verificationToken.getUserId();
            LocalDateTime updatedAt = LocalDateTime.now();

            dataOrchestrator.updateUserEmail(userId, email, updatedAt);
            dataOrchestrator.deleteVerificationToken(token);

            log.info("[🔧] ✅ Email changed successfully for user {}", userId);
            return ResultOneArg.success("Email successfully changed");
        }
        catch (ValidationException e) {
            log.warn("[🔧] ☝️ Failed to confirm email change token: {}", e.getMessage());
            return ResultOneArg.error(e.getMessage());
        }
        catch (Exception e) {
            log.error("[🔧] ⚠️ Error on email change token confirmation: {}", e.getMessage());
            return ResultOneArg.error("confirmEmailUpdateToken failed due to server error");
        }
    }
    public ResultOneArg<String> confirmPasswordUpdateToken(String token, String password) {
        try {
            if (token == null || token.trim().isEmpty()) {
                throw new ValidationException("Token cannot be empty");
            }

            Optional<VerificationTokenDTO> tokenOpt = dataOrchestrator.getVerificationToken(token);
            if (tokenOpt.isEmpty()) {
                throw new ValidationException("Invalid token");
            }

            VerificationTokenDTO verificationToken = tokenOpt.get();
            if (verificationToken.isExpired()) {
                throw new ValidationException("Token expired");
            } else if (verificationToken.getTokenType() != TokenType.PASSWORD_UPDATE) {
                throw new ValidationException("Invalid token type");
            }

            long userId = verificationToken.getUserId();
            LocalDateTime updatedAt = LocalDateTime.now();

            dataOrchestrator.updateUserPassword(userId, password, updatedAt);
            dataOrchestrator.deleteVerificationToken(token);

            log.info("[🔧] ✅ Password changed successfully for user {}", userId);
            return ResultOneArg.success("Password successfully changed");
        }
        catch (ValidationException e) {
            log.warn("[🔧] ☝️ Failed to confirm password change token: {}", e.getMessage());
            return ResultOneArg.error(e.getMessage());
        }
        catch (Exception e) {
            log.error("[🔧] ⚠️ Error on password change token confirmation: {}", e.getMessage());
            return ResultOneArg.error("confirmPasswordUpdateToken failed due to server error");
        }
    }

    private String extractClientIp(HttpServletRequest request) {
        try {
            if (request.getHeader("X-Forwarded-For") instanceof String xfHeader && !xfHeader.isEmpty()) {
                return xfHeader.split(",")[0].trim();
            }
            return request.getRemoteAddr();
        } catch (Exception e) {
            return "unknown";
        }
    }
    private static String generate64CharString() {
        SecureRandom random = new SecureRandom();
        byte[] bytes = new byte[48]; // 48 bytes = 64 base64 characters
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}