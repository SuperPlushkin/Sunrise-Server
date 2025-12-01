package com.Sunrise.Services;

import com.Sunrise.DTO.ServiceResults.TokenConfirmationResult;
import com.Sunrise.DTO.ServiceResults.UserConfirmOperationResult;
import com.Sunrise.DTO.ServiceResults.UserInsertOperationResult;
import com.Sunrise.Entities.User;
import com.Sunrise.Entities.VerificationToken;
import com.Sunrise.JWT.JwtUtil;

import com.Sunrise.Services.DataServices.DataAccessService;
import jakarta.servlet.http.HttpServletRequest;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
public class AuthService {

    private final EmailService emailService;
    private final DataAccessService dataAccessService;
    private final JwtUtil jwtUtil;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public AuthService(DataAccessService dataAccessService, JwtUtil jwtUtil, EmailService emailService) {
        this.dataAccessService = dataAccessService;
        this.jwtUtil = jwtUtil;
        this.emailService = emailService;
    }

    public UserInsertOperationResult registerUser(String username, String name, String email, String password) {
        try
        {
            // валидация данных
            if (username == null || username.trim().length() < 4)
                return new UserInsertOperationResult(false, "Username must be at least 4 characters", null);

            if (name == null || name.trim().length() < 4)
                return new UserInsertOperationResult(false, "Name must be at least 4 characters", null);

            if (password == null || password.length() < 8)
                return new UserInsertOperationResult(false, "Password must be at least 8 characters", null);

            if (email == null || !email.matches("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$"))
                return new UserInsertOperationResult(false, "Invalid email format", null);

            // проверка на уникальность
            if (dataAccessService.existsUserByUsername(username.trim()))
                return new UserInsertOperationResult(false, "Username already exists", null);

            if (dataAccessService.existsUserByEmail(email.toLowerCase()))
                return new UserInsertOperationResult(false, "Email already exists", null);

            String hashPassword = passwordEncoder.encode(password);

            Long userId = dataAccessService.makeUser(username.trim(), name.trim(), email.toLowerCase(), hashPassword, false);

            String token = dataAccessService.makeVerificationToken(userId, "email_confirmation");

            emailService.sendVerificationEmail(email, token);

            return new UserInsertOperationResult(true, null, token);
        }
        catch (Exception e) {
            return new UserInsertOperationResult(false, "Registration failed due to server error", null);
        }
    }
    public UserConfirmOperationResult authenticateUser(String username, String password, HttpServletRequest httpRequest) {
        try
        {
            Optional<User> userOpt = dataAccessService.getUserByUsername(username);

            if (userOpt.isEmpty())
                return new UserConfirmOperationResult(false, "Invalid username or password", null);

            User user = userOpt.get();

            if (!user.getIsEnabled())
                return new UserConfirmOperationResult(false, "Please verify your email first", null);

            if (passwordEncoder.matches(password, user.getHashPassword())) {
                dataAccessService.updateLastLogin(username, LocalDateTime.now());
                dataAccessService.saveLoginHistory(user.getId(), extractClientIp(httpRequest), httpRequest.getHeader("User-Agent"));

                return new UserConfirmOperationResult(true, null, jwtUtil.generateToken(username, user.getId()));
            }
            else return new UserConfirmOperationResult(false, "Invalid username or password", null);
        }
        catch (Exception e) {
            return new UserConfirmOperationResult(false, "Authentication failed", null);
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

            if (verificationToken.getExpiryDate().isBefore(LocalDateTime.now()))
            {
                dataAccessService.deleteVerificationToken(token);
                return new TokenConfirmationResult(false, "Token expired");
            }

            dataAccessService.enableUser(verificationToken.getUser_id());
            dataAccessService.deleteVerificationToken(token);

            return new TokenConfirmationResult(true, "Email successfully verified");
        }
        catch (Exception e) {
            return new TokenConfirmationResult(false, "Error during Token Confirmation: " + e.getMessage());
        }
    }

    private String extractClientIp(HttpServletRequest request) {
        try
        {
            if (request.getHeader("X-Forwarded-For") instanceof String xfHeader && !xfHeader.isEmpty())
            {
                return xfHeader.split(",")[0].trim();
            }
            else return request.getRemoteAddr();
        }
        catch (Exception e) {
            return "unknown";
        }
    }
}
