package com.Sunrise.Services;

import com.Sunrise.DTO.ServiceResults.TokenConfirmationResult;
import com.Sunrise.DTO.ServiceResults.UserLoginResult;
import com.Sunrise.DTO.ServiceResults.UserRegistrationResult;
import com.Sunrise.Entities.DB.LoginHistory;
import com.Sunrise.Entities.DTO.FullUserDTO;
import com.Sunrise.Entities.DTO.VerificationTokenDTO;
import com.Sunrise.Services.DataServices.DataAccessService;
import com.Sunrise.Entities.DB.User;
import com.Sunrise.Entities.DB.VerificationToken;
import com.Sunrise.JWT.JwtUtil;
import com.Sunrise.Services.DataServices.LockService;
import com.Sunrise.Subclasses.ValidationException;

import jakarta.servlet.http.HttpServletRequest;

import lombok.extern.slf4j.Slf4j;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;

import static com.Sunrise.Services.DataServices.DataAccessService.randomId;

@Slf4j
@Service
public class AuthService {

    private final EmailService emailService;
    private final DataAccessService dataAccessService;
    private final LockService lockService;
    private final JwtUtil jwtUtil;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public AuthService(DataAccessService dataAccessService, JwtUtil jwtUtil, EmailService emailService, LockService lockService) {
        this.dataAccessService = dataAccessService;
        this.jwtUtil = jwtUtil;
        this.emailService = emailService;
        this.lockService = lockService;
    }

    public UserRegistrationResult registerUser(String username, String name, String email, String password) {

        // пытаемся заблокировать регистрацию
        if (!lockService.tryLockRegistration(username, email))
            return UserRegistrationResult.error("Try again later");

        long newUserId = -1;

        try {
            if (dataAccessService.existsUserByUsername(username.trim()))
                throw new ValidationException("Username already exists");

            if (dataAccessService.existsUserByEmail(email.toLowerCase()))
                throw new ValidationException("Email already exists");

            newUserId = randomId();

            // пытаемся заблокировать профиль юзера
            if (!lockService.tryLockUserProfileForWrite(newUserId))
                throw new RuntimeException("Try later");

            dataAccessService.saveUser(newUserId, username, name, email, passwordEncoder.encode(password), false);

            var verificationTokenDTO = new VerificationTokenDTO(
                randomId(),
                DataAccessService.generate64CharString(),
                newUserId,
                "email_confirmation"
            );

            dataAccessService.saveVerificationToken(verificationTokenDTO);
            emailService.sendVerificationEmail(email, verificationTokenDTO.getToken());

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
            if (newUserId != -1) {
                lockService.unLockUserProfileForWrite(newUserId); // разблокируем профиль юзера
            }
            lockService.unLockRegistration(username, email); // разблокируем регистрацию
        }
    }

    public UserLoginResult authenticateUser(String username, String password, HttpServletRequest httpRequest) {
        try
        {
            Optional<FullUserDTO> userOpt = dataAccessService.getUserByUsername(username);
            if (userOpt.isEmpty())
                throw new ValidationException("Invalid username or password");

            FullUserDTO user = userOpt.get();
            if (!user.isEnabled())
                throw new ValidationException("Please verify your email first");

            if(!passwordEncoder.matches(password, user.getHashPassword()))
                throw new ValidationException("Invalid username or password");

            dataAccessService.updateLastLogin(username, LocalDateTime.now());

            var loginHistory = new LoginHistory(randomId(), user.getId(), extractClientIp(httpRequest), httpRequest.getHeader("User-Agent"), LocalDateTime.now());
            dataAccessService.saveLoginHistory(loginHistory);

            String token = jwtUtil.generateToken(username, user.getId());

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

        long userId = -1;

        try {
            if (token == null || token.trim().isEmpty())
                throw new ValidationException("Token cannot be empty");

            Optional<VerificationTokenDTO> tokenOpt = dataAccessService.getVerificationToken(token);
            if (tokenOpt.isEmpty())
                throw new ValidationException("Invalid token");

            VerificationTokenDTO verificationToken = tokenOpt.get();
            if (!type.equals(verificationToken.getTokenType()))
                throw new ValidationException("Invalid token");

            userId = verificationToken.getUserId();

            // пытаемся заблокировать профиль юзера
            if (!lockService.tryLockUserProfileForWrite(userId))
                throw new RuntimeException("Try later");

            dataAccessService.deleteVerificationToken(token);

            if (verificationToken.isExpired())
                throw new ValidationException("Token expired");

            dataAccessService.enableUser(verificationToken.getUserId());

            log.info("[🔧] ✅ Email verified successfully for user {}", verificationToken.getUserId());
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
        finally {
            if(userId != -1){
                lockService.unLockUserProfileForWrite(userId); // разблокируем профиль юзера
            }
        }
    }

    private String extractClientIp(HttpServletRequest request) {
        try
        {
            if (request.getHeader("X-Forwarded-For") instanceof String xfHeader && !xfHeader.isEmpty()) {
                return xfHeader.split(",")[0].trim();
            }
            return request.getRemoteAddr();
        }
        catch (Exception e) {
            return "unknown";
        }
    }
}