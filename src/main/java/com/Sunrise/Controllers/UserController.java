package com.Sunrise.Controllers;

import com.Sunrise.DTO.ServiceResults.UserDTO;
import com.Sunrise.DTO.Requests.UserFilterRequest;
import com.Sunrise.Services.UserService;

import jakarta.validation.Valid;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/user")
public class UserController {

    private static final Logger log = LoggerFactory.getLogger(UserController.class);

    private final UserService userService;

    public UserController(UserService userService){
        this.userService = userService;
    }

    @GetMapping("/getmany")
    public ResponseEntity<?> getManyUsers(@Valid @ModelAttribute UserFilterRequest request) {

        var result = userService.getFilteredUsers(request.getLimited(), request.getOffset(), request.getFilter());

        if (result.isSuccess())
        {
            List<UserDTO> users = result.getUsers();

            return ResponseEntity.ok(Map.of(
                "users", users,
                "count", users.size()
            ));
        }
        else
        {
            log.warn(result.getErrorMessage());
            return ResponseEntity.badRequest().body(result.getErrorMessage());
        }
    }
}
