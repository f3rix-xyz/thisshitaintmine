package sustainico_backend.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import sustainico_backend.Models.Home;
import sustainico_backend.Models.User;
import sustainico_backend.rep.WaterReadingRepository;
import sustainico_backend.service.HomeService;
import sustainico_backend.service.UserService;
import sustainico_backend.service.WaterReadingService;
import sustainico_backend.util.ApiResponse;
import sustainico_backend.util.JwtUtil;

import java.util.*;

@RestController
@RequestMapping("/home")
@CrossOrigin(origins = "*")
public class HomeController {

    private final HomeService homeService;

    @Autowired
    public HomeController(HomeService homeService){
        this.homeService = homeService;
    }

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private UserService userService;

    @Autowired
    private WaterReadingService waterReadingService;

    @Autowired
    private WaterReadingRepository waterReadingRepository;


    @PostMapping("/create")
    public ResponseEntity<?> createHome(@RequestBody Home home, @RequestHeader("Authorization") String jwttoken) {
        String token = jwttoken.substring(7);
        String contactNoByToken = jwtUtil.extractContactNo(token);
        Optional<User> user = userService.getUserByContactNo(contactNoByToken);
        try {
            home.calculateBaselineUsage(); // Calculate baseline usage before saving
            if (Objects.equals(user.get().getUserId(), home.getUserId())) {
                Home savedHome = homeService.createHome(home);
                return ResponseEntity.ok(savedHome); // Return the saved home entity with generated homeId
            }
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error creating home: " + e.getMessage());
        }
        return (ResponseEntity<?>) ResponseEntity.status(404);
    }

    @PutMapping("/update")
    public ResponseEntity<?> updateHome(@RequestBody Home home, @RequestHeader("Authorization") String jwttoken) {
        String token = jwttoken.substring(7);
        String contactNoByToken = jwtUtil.extractContactNo(token);
        Optional<User> user = userService.getUserByContactNo(contactNoByToken);
        if (Objects.equals(user.get().getUserId(), home.getUserId())) {
            Optional<Home> updatedHome = homeService.updateHome(home);
            return updatedHome.map(ResponseEntity::ok)
                    .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND).body(null));
        }
        return (ResponseEntity<?>) ResponseEntity.status(404);
    }

    @GetMapping("/{homeId}")
    public Home getHomeById(@PathVariable String homeId, @RequestHeader("Authorization") String jwttoken) {
//        String todayuse = waterReadingRepository.updateTodaysUsage(homeId);
        String token = jwttoken.substring(7);
        String contactNoByToken = jwtUtil.extractContactNo(token);
        Optional<User> user = userService.getUserByContactNo(contactNoByToken);
        Home home = homeService.getHomeById(homeId);
        if(Objects.equals(user.get().getUserId(), home.getUserId())) return home;
        return null;
    }

    @DeleteMapping("/delete/{homeId}")
    public ResponseEntity<String> deleteHomeById(@PathVariable String homeId, @RequestHeader("Authorization") String jwttoken) {
        String token = jwttoken.substring(7);
        String contactNoByToken = jwtUtil.extractContactNo(token);
        Optional<User> user = userService.getUserByContactNo(contactNoByToken);
        Home home = homeService.getHomeById(homeId);

        if(user.isPresent() && Objects.equals(user.get().getUserId(), home.getUserId())){
            homeService.deleteHome(home);
            return new ResponseEntity<>("Home deleted successfully", HttpStatus.OK);
        }
        return new ResponseEntity<>("Invalid data", HttpStatus.BAD_REQUEST);
    }


    @GetMapping("/user/{userId}")
    public ResponseEntity<List<Home>> getHomeByUserID(@PathVariable String userId, @RequestHeader("Authorization") String jwttoken) {
        String token = jwttoken.substring(7);
        String contactNoByToken = jwtUtil.extractContactNo(token);
        Optional<User> user = userService.getUserByContactNo(contactNoByToken);
        if(Objects.equals(user.get().getUserId(), userId)){
            List<Home> homes = homeService.fetchAllHomesByUserId(userId);
            return ResponseEntity.ok(homes);
        }
        return null;
    }

    @PutMapping("/moveDevice")
    public ResponseEntity<?> moveDevice(@RequestBody Map<String, String> request, @RequestHeader("Authorization") String jwttoken) {
        String deviceId = request.get("deviceId");
        String currentHomeId = request.get("currentHomeId");
        String newHomeId = request.get("newHomeId");
        String userId = request.get("userId");
        String deviceName = request.get("deviceName");

        String token = jwttoken.substring(7);
        String contactNoByToken = jwtUtil.extractContactNo(token);
        Optional<User> user = userService.getUserByContactNo(contactNoByToken);

        if (user.isPresent() && Objects.equals(user.get().getUserId(), userId)) {
            try {
                homeService.moveDevice(deviceId, currentHomeId, newHomeId, deviceName);
                return ResponseEntity.ok("Device moved successfully");
            } catch (IllegalArgumentException e) {
                return ResponseEntity.badRequest().body(e.getMessage());
            }
        }
        return ResponseEntity.status(404).body("User not found or userId mismatch.");
    }
    
}
