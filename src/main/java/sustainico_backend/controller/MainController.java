package sustainico_backend.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import sustainico_backend.Models.Home;
import sustainico_backend.Models.ResourceNotFoundException;
import sustainico_backend.Models.User;
import sustainico_backend.Models.UserWithHomesDTO;
import sustainico_backend.rep.HomeRepository;
import sustainico_backend.rep.UserRepository;
import sustainico_backend.util.JwtUtil;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.Optional;
import java.util.List;

@RestController
@RequestMapping("/main")
public class MainController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private HomeRepository homeRepository;

    @Autowired
    private JwtUtil jwtUtil;

    @GetMapping("/{contactNo}")
    public UserWithHomesDTO getUserWithHomesByContactNo(@PathVariable String contactNo, @RequestHeader("Authorization") String jwttoken) {
        String token = jwttoken.substring(7);
        String contactNoByToken = jwtUtil.extractContactNo(token);
//        System.out.println("token decoded"+ LocalDateTime.now());
        if(Objects.equals(contactNo, contactNoByToken)){
            Optional<User> userOptional = userRepository.findByContactNo(contactNo);
//            System.out.println("user find"+ LocalDateTime.now());

            if (userOptional.isPresent()) {
                User user = userOptional.get();
                List<Home> homes = homeRepository.findHomeByUserId(user.getUserId());
//                System.out.println("homes find"+ LocalDateTime.now());
                return new UserWithHomesDTO(user, homes);
            } else {
                throw new ResourceNotFoundException("User not found with contact number: " + contactNo);
            }
        }
        return null;
    }
}
