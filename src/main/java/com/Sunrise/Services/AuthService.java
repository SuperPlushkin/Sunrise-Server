package com.Sunrise.Services;

import com.Sunrise.DTO.ServiceResults.TokenConfirmationResult;
import com.Sunrise.DTO.ServiceResults.UserLoginResult;
import com.Sunrise.DTO.ServiceResults.UserRegistrationResult;
import com.Sunrise.Entities.DB.ChatMember;
import com.Sunrise.Services.DataServices.DataAccessService;
import com.Sunrise.Entities.DB.User;
import com.Sunrise.Entities.DB.VerificationToken;
import com.Sunrise.JWT.JwtUtil;

import com.Sunrise.Services.DataServices.LockService;
import jakarta.servlet.http.HttpServletRequest;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;

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

        if (!lockService.lockRegistration(username, email))
            return UserRegistrationResult.error("Try again later");

        try
        {
            // проверка на уникальность
            if (dataAccessService.existsUserByUsername(username.trim()))
                return UserRegistrationResult.error("Username already exists");

            if (dataAccessService.existsUserByEmail(email.toLowerCase()))
                return UserRegistrationResult.error("Email already exists");

            User user = new User(DataAccessService.generateRandomId(), username, name, email, passwordEncoder.encode(password), false);

            dataAccessService.saveUser(user);

            VerificationToken verifToken = new VerificationToken(DataAccessService.generateRandomId(), DataAccessService.generate64CharString(), user.getId(), "email_confirmation");

            dataAccessService.saveVerificationToken(verifToken);
            emailService.sendVerificationEmail(email, verifToken.getToken());

            return UserRegistrationResult.success(verifToken.getToken());
        }
        catch (Exception e) {
            return UserRegistrationResult.error("Registration failed due to server error");
        }
        finally {
            lockService.unlockRegistration(username, email);
        }
    }
    public UserLoginResult authenticateUser(String username, String password, HttpServletRequest httpRequest) {
        try
        {
            Optional<User> userOpt = dataAccessService.getUserByUsername(username);

            if (userOpt.isEmpty())
                return UserLoginResult.error("Invalid username or password");

            User user = userOpt.get();

            if (!user.getIsEnabled())
                return UserLoginResult.error("Please verify your email first");

            if (passwordEncoder.matches(password, user.getHashPassword())) {
                dataAccessService.updateLastLogin(username, LocalDateTime.now());
                dataAccessService.saveLoginHistory(user.getId(), extractClientIp(httpRequest), httpRequest.getHeader("User-Agent"));

                String token = jwtUtil.generateToken(username, user.getId());

                return UserLoginResult.success(token, jwtUtil.getTokenExpirationTime(token));
            }
            else return UserLoginResult.error("Invalid username or password");
        }
        catch (Exception e) {
            return UserLoginResult.error("Authentication failed");
        }
    }
    public TokenConfirmationResult confirmToken(String type, String token) {
        try
        {
            if (token == null || token.trim().isEmpty())
                return new TokenConfirmationResult(false, "Token cannot be empty");

            Optional<VerificationToken> tokenOpt = dataAccessService.getVerificationToken(token);

            if (tokenOpt.isEmpty())
                return new TokenConfirmationResult(false, "Invalid token");

            VerificationToken verificationToken = tokenOpt.get();

            if (!type.equals(verificationToken.getTokenType()))
                return new TokenConfirmationResult(false, "Invalid token");

            dataAccessService.deleteVerificationToken(token);

            if (verificationToken.isExpired())
                return new TokenConfirmationResult(false, "Token expired");

            dataAccessService.enableUser(verificationToken.getUser_id());

            return new TokenConfirmationResult(true, "Email successfully verified");
        }
        catch (Exception e) {
            return new TokenConfirmationResult(false, "Error during Token Confirmation: " + e.getMessage());
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
