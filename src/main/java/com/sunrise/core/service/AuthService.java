package com.sunrise.core.service;

import com.sunrise.core.service.result.TokenConfirmationResult;
import com.sunrise.core.service.result.UserLoginResult;
import com.sunrise.core.service.result.UserRegistrationResult;
import com.sunrise.entity.dto.FullUserDTO;
import com.sunrise.entity.dto.LoginHistoryDTO;
import com.sunrise.entity.dto.VerificationTokenDTO;
import com.sunrise.core.dataservice.DataOrchestrator;
import com.sunrise.config.jwt.JwtUtil;
import com.sunrise.core.dataservice.LockManager;
import com.sunrise.core.notifier.EmailSender;
import com.sunrise.helpclass.SimpleSnowflakeId;
import com.sunrise.helpclass.ValidationException;

import jakarta.servlet.http.HttpServletRequest;

import lombok.extern.slf4j.Slf4j;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Optional;

@Slf4j
@Service
public class AuthService {

    private final EmailSender emailSender;
    private final DataOrchestrator dataOrchestrator;
    private final LockManager lockManager;
    private final JwtUtil jwtUtil;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public AuthService(DataOrchestrator dataOrchestrator, JwtUtil jwtUtil, EmailSender emailSender, LockManager lockManager) {
        this.dataOrchestrator = dataOrchestrator;
        this.jwtUtil = jwtUtil;
        this.emailSender = emailSender;
        this.lockManager = lockManager;
    }

    public UserRegistrationResult registerUser(String username, String name, String email, String password) {

        // пытаемся заблокировать регистрацию
        if (!lockManager.tryLockRegistration(username, email))
            return UserRegistrationResult.error("Try again later");

        try {
            if (dataOrchestrator.existsUserByUsername(username.trim()))
                throw new ValidationException("Username already exists");

            if (dataOrchestrator.existsUserByEmail(email.toLowerCase()))
                throw new ValidationException("Email already exists");

            long newUserId = SimpleSnowflakeId.nextId();
            FullUserDTO user = FullUserDTO.create(newUserId, username, name, email, passwordEncoder.encode(password));

            dataOrchestrator.saveUser(user);

            var verificationTokenDTO = new VerificationTokenDTO(
                SimpleSnowflakeId.nextId(),
                generate64CharString(),
                newUserId,
                "email_confirmation"
            );

            dataOrchestrator.saveVerificationToken(verificationTokenDTO);
            emailSender.sendVerificationEmail(email, verificationTokenDTO.getToken());

            log.info("[🔧] ✅ User registered successfully --> {}", username);
            return UserRegistrationResult.success(verificationTokenDTO.getToken());
        }
        catch (ValidationException e) {
            log.warn("[🔧] ☝️ Failed to register user: {}", e.getMessage());
            return UserRegistrationResult.error(e.getMessage());
        }
        catch (Exception e) {
            log.error("[🔧] ⚠️ Registration failed for user {}: {}", username, e.getMessage());
            return UserRegistrationResult.error("Registration failed due to server error");
        }
        finally {
            lockManager.unLockRegistration(username, email); // разблокируем регистрацию
        }
    }
    public UserLoginResult authenticateUser(String username, String password, HttpServletRequest httpRequest) {
        try
        {
            Optional<FullUserDTO> userOpt = dataOrchestrator.getUserByUsername(username);
            if (userOpt.isEmpty())
                throw new ValidationException("Invalid username or password");

            FullUserDTO user = userOpt.get();
            if (!user.isEnabled())
                throw new ValidationException("Please verify your email first");

            if(!passwordEncoder.matches(password, user.getHashPassword()))
                throw new ValidationException("Invalid username or password");

            dataOrchestrator.updateLastLogin(username, LocalDateTime.now());

            var loginHistory = LoginHistoryDTO.create(SimpleSnowflakeId.nextId(), user.getId(), extractClientIp(httpRequest), httpRequest.getHeader("User-Agent"));
            dataOrchestrator.saveLoginHistory(loginHistory);

            String token = jwtUtil.generateToken(user.getId());

            log.info("[🔧] ✅ User logged in successfully --> {}", username);
            return UserLoginResult.success(token, jwtUtil.getTokenExpirationTime(token));
        }
        catch (ValidationException e) {
            log.warn("[🔧] ☝️ Failed to authenticate user: {}", e.getMessage());
            return UserLoginResult.error(e.getMessage());
        }
        catch (Exception e) {
            log.error("[🔧] ⚠️ Authentication failed for user {}: {}", username, e.getMessage());
            return UserLoginResult.error("Authentication failed");
        }
    }
    public TokenConfirmationResult confirmToken(String type, String token) {
        try {
            if (token == null || token.trim().isEmpty())
                throw new ValidationException("Token cannot be empty");

            Optional<VerificationTokenDTO> tokenOpt = dataOrchestrator.getVerificationToken(token);
            if (tokenOpt.isEmpty())
                throw new ValidationException("Invalid token");

            VerificationTokenDTO verificationToken = tokenOpt.get();
            if (!type.equals(verificationToken.getTokenType()))
                throw new ValidationException("Invalid token");

            dataOrchestrator.deleteVerificationToken(token);

            if (verificationToken.isExpired())
                throw new ValidationException("Token expired");

            long userId = verificationToken.getUserId();
            dataOrchestrator.enableUser(userId);

            log.info("[🔧] ✅ Email verified successfully for user {}", userId);
            return TokenConfirmationResult.success("Email successfully verified");
        }
        catch (ValidationException e) {
            log.warn("[🔧] ☝️ Failed to confirm token: {}", e.getMessage());
            return TokenConfirmationResult.error(e.getMessage());
        }
        catch (Exception e) {
            log.error("[🔧] ⚠️ Token confirmation error: {}", e.getMessage());
            return TokenConfirmationResult.error("Error during Token Confirmation: " + e.getMessage());
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