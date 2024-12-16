package sustainico_backend.controller;


import com.google.api.Authentication;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.web.bind.annotation.*;
import sustainico_backend.Models.AuthenticationRequest;
import sustainico_backend.Models.AuthenticationResponse;
import sustainico_backend.Models.CreateUserResponse;
import sustainico_backend.Models.User;
import sustainico_backend.service.MyUserDetailsService;
import sustainico_backend.service.UserService;
import sustainico_backend.util.ApiResponse;
import sustainico_backend.util.JwtUtil;

import java.util.Objects;
import java.util.Optional;

@RestController
@RequestMapping("/user")
@CrossOrigin(origins = "*")
public class UserController {

    private final UserService userService;

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private MyUserDetailsService myUserDetailsService;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/authenticate")
    public ResponseEntity<?> createAuthenticationToken(@RequestBody User user, @RequestHeader("firebasetoken") String firebaseToken) throws Exception {
        String contactNo = user.getContactNo();
        User createdUser = userService.createUser(user);
        String jwt = userService.authenticate(contactNo, firebaseToken);
        return ResponseEntity.ok(new CreateUserResponse(jwt, createdUser));
    }

    @GetMapping(value = "/hello", produces = MediaType.APPLICATION_JSON_VALUE)
    public User sayHello() {
        return new User("wdceq", "sacrq", "23c1iyg12c", "xwqce43", "14c212", false, "1718793671");
    }

    @PutMapping("/update")
    public ResponseEntity<User> updateUser(@RequestBody User user, @RequestHeader("Authorization") String jwttoken) {
        String token = jwttoken.substring(7);
        String contactNoByToken = jwtUtil.extractContactNo(token);
        if (Objects.equals(user.getContactNo(), contactNoByToken)) {
            Optional<User> updatedUser = userService.updateUser(user);
            return updatedUser.map(ResponseEntity::ok)
                    .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND).body(null));
        }
        return null;
    }

    @DeleteMapping("/delete")
    public ResponseEntity<?> deleteUser(@RequestHeader("Authorization") String jwttoken) {
        String token = jwttoken.substring(7);
        String contactNoByToken = jwtUtil.extractContactNo(token);
        boolean isDeleted = userService.deleteUser(contactNoByToken);
        if (isDeleted) {
            return ResponseEntity.ok("User deleted successfully");
        } else {
            return ResponseEntity.badRequest().body("User not found");
        }
    }

    @GetMapping("/contactno")
    public Optional<User> getUserByContactNumber(@RequestHeader("Authorization") String jwttoken) {
        String token = jwttoken.substring(7);
        String contactNoByToken = jwtUtil.extractContactNo(token);
        return userService.getUserByContactNo(contactNoByToken);
    }

}
