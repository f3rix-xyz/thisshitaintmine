package sustainico_backend.controller;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import sustainico_backend.Models.*;
import sustainico_backend.service.DeviceService;
import sustainico_backend.service.UserService;
import sustainico_backend.util.JwtUtil;

import java.util.Base64;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

@RestController
@RequestMapping("/device")
@CrossOrigin(origins = "*")
public class DeviceController {

    private final DeviceService deviceService;

    @Autowired
    public DeviceController(DeviceService deviceService) {
        this.deviceService = deviceService;
    }

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private UserService userService;

    @PostMapping("/create")
    public ResponseEntity<?> createDevice(@RequestBody Device device) {
        Device createdDevice = deviceService.createDevice(device);
        return ResponseEntity.ok(createdDevice);
    }

    @PutMapping("/add")
    public ResponseEntity<?> addDevice(@RequestBody Map<String, String> request, @RequestHeader("Authorization") String jwttoken) {
        String userId = request.get("userId");
        String homeId = request.get("homeId");
        String deviceName = request.get("deviceName");
        String deviceId = request.get("deviceId");
        String pin = request.get("pin");

        String token = jwttoken.substring(7);
        String contactNoByToken = jwtUtil.extractContactNo(token);
        Optional<User> user = userService.getUserByContactNo(contactNoByToken);

        if (Objects.equals(user.get().getUserId(), userId)) {
            try {
                Device updatedDevice = deviceService.addDevice(userId, homeId, deviceName, deviceId, pin);
                return ResponseEntity.ok(updatedDevice);
            } catch (IllegalArgumentException e) {
                return ResponseEntity.badRequest().body(e.getMessage());
            }
        }
        return (ResponseEntity<?>) ResponseEntity.status(404);
    }

    @PutMapping("/updateName")
    public ResponseEntity<?> updateDeviceName(@RequestBody Map<String, String> request, @RequestHeader("Authorization") String jwttoken) {
        String deviceId = request.get("deviceId");
        String newName = request.get("newName");

        String token = jwttoken.substring(7);
        String contactNoByToken = jwtUtil.extractContactNo(token);
        Optional<User> user = userService.getUserByContactNo(contactNoByToken);

        if (user.isPresent()) {
            String userId = user.get().getUserId();
            Optional<Device> device = deviceService.getDeviceById(deviceId);

            if (device.isPresent() && Objects.equals(device.get().getOwnerId(), userId)) {
                try {
                    Device updatedDevice = deviceService.updateDeviceName(deviceId, newName);
                    return ResponseEntity.ok(updatedDevice);
                } catch (IllegalArgumentException e) {
                    return ResponseEntity.badRequest().body(e.getMessage());
                }
            } else {
                return ResponseEntity.status(403).body("User is not the owner of the device or device not found.");
            }
        }
        return ResponseEntity.status(404).body("User not found.");
    }


    @PostMapping("/createDigitalPin/{deviceId}")
    public ResponseEntity<?> createPin(@PathVariable String deviceId, @RequestHeader("Authorization") String jwttoken) {

        String token = jwttoken.substring(7);
        String contactNoByToken = jwtUtil.extractContactNo(token);
        Optional<User> user = userService.getUserByContactNo(contactNoByToken);
        Optional<Device> device = deviceService.getDeviceById(deviceId);
        if (Objects.equals(user.get().getUserId(), device.get().getOwnerId())) {
            Device updatedDevice = deviceService.createPinForDevice(deviceId);
            return ResponseEntity.ok(updatedDevice);
        }
        return (ResponseEntity<?>) ResponseEntity.status(404);
    }

    @GetMapping("/{deviceId}")
    public Optional<Device> getDeviceById(@PathVariable String deviceId, @RequestHeader("Authorization") String jwttoken) {

        String token = jwttoken.substring(7);
        String contactNoByToken = jwtUtil.extractContactNo(token);
        Optional<User> user = userService.getUserByContactNo(contactNoByToken);
        Optional<Device> device = deviceService.getDeviceById(deviceId);
        if (Objects.equals(user.get().getUserId(), device.get().getOwnerId())) {
            return device;
        }
        return Optional.empty();
    }

    @DeleteMapping("/delete")
    public ResponseEntity<?> deleteDevice(@RequestBody Map<String, String> request, @RequestHeader("Authorization") String jwttoken) {
        String deviceId = request.get("deviceId");
        String homeId = request.get("homeId");

        String token = jwttoken.substring(7);
        String contactNoByToken = jwtUtil.extractContactNo(token);
        Optional<User> user = userService.getUserByContactNo(contactNoByToken);

        if (user.isPresent()) {
            String userId = user.get().getUserId();
            try {
                Device updatedDevice = deviceService.deleteDevice(deviceId, userId, homeId);
                return ResponseEntity.ok(updatedDevice);
            } catch (IllegalArgumentException e) {
                return ResponseEntity.badRequest().body(e.getMessage());
            }
        }
        return ResponseEntity.status(404).body("User not found.");
    }

    @GetMapping("/details/{deviceId}")
    public DeviceDetailsResponse getDeviceDetails(@PathVariable String deviceId) {
        Optional<Device> device = deviceService.getDeviceById(deviceId);
        // Check if the device exists
        if (device.isEmpty()) {
            throw new ResourceNotFoundException("Device not found with id: " + deviceId);
        }

        User owner = userService.getUserById(device.get().getOwnerId());
        String ownerName = (owner != null) ? owner.getUserName() : "";

        return new DeviceDetailsResponse(
                device.get().getDeviceId(),
                device.get().getDeviceName(),
                device.get().getDeviceType(),
                device.get().getBillAccNo(),
                ownerName,
                device.get().getOwnerId()
        );
    }

    @PostMapping("/deviceLogin")
    public ResponseEntity<?> verifyDevicePin(@RequestBody Map<String, String> request) throws Exception {
        // Get the initial pin from the request
        String initialPin = request.get("initialPin");
        String deviceId = request.get("deviceId");

        // Get the device from the database
        Optional<Device> device = deviceService.getDeviceById(deviceId);

        // Check if the device exists
        if (device.isEmpty()) {
            throw new ResourceNotFoundException("Device not found with id: " + deviceId);
        }

        // Check if the initial pin matches
        if (!device.get().getInitialPin().equals(initialPin)) {
            throw new Exception("Incorrect initial pin");
        }
        // Generate a new encoded token using the deviceId
        String token = Base64.getEncoder().encodeToString((deviceId + "asdhd32xc#nmds#$5DFSGT").getBytes());

        // Return the token
        return ResponseEntity.ok(new AuthenticationResponse(token));

    }
}
