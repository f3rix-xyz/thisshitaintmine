package sustainico_backend.controller;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import sustainico_backend.Models.*;
import sustainico_backend.service.*;
import sustainico_backend.util.JwtUtil;

import java.util.*;
import java.util.logging.Logger;

@RestController
@RequestMapping("/water")
@CrossOrigin(origins = "*")
public class WaterReadingController {

    @Autowired
    private final WaterReadingService waterReadingService;

    @Autowired
    private WaterReadingPerHourService waterReadingPerHourService;

    @Autowired
    private WaterReadingPerDayService waterReadingPerDayService;

    @Autowired
    private WaterReadingPerWeekService waterReadingPerWeekService;

    @Autowired
    private WaterReadingPerMonthService waterReadingPerMonthService;

    @Autowired
    private WaterReadingAggregationService waterReadingAggregationService;

    private final DeviceService deviceService;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private UserService userService;

    private static final Logger logger = Logger.getLogger(WaterReadingController.class.getName());

    @Autowired
    public WaterReadingController(WaterReadingService waterReadingService, DeviceService deviceService) {
        this.waterReadingService = waterReadingService;
        this.deviceService = deviceService;
    }

    @GetMapping(value = "/hello", produces = MediaType.APPLICATION_JSON_VALUE)
    public WaterReading sayHello() {
        return new WaterReading("jguirx", "4435", "3214215", "reading_water");
    }

    @PostMapping("/reading/send")
    public WaterReading createWater(@RequestBody WaterReading waterReading) {
        return waterReadingService.saveWaterReading(waterReading);
    }

    @PostMapping("/reading/report/device")
    public ResponseEntity<?> getWaterReadings(@RequestBody WaterReadingDeviceRequest request, @RequestHeader("Authorization") String jwttoken) {
        String userId = request.getUserId();
        String token = jwttoken.substring(7);
        String contactNoByToken = jwtUtil.extractContactNo(token);
        Optional<User> user = userService.getUserByContactNo(contactNoByToken);

        if (user.isPresent() && Objects.equals(user.get().getUserId(), userId)) {
            try {
                DeviceReportResponse readings;
                switch (request.getResolution().toLowerCase()) {
                    case "day":
                        readings = waterReadingPerHourService.getReadings(request.getDeviceId(), request.getStartTimestamp(), request.getEndTimestamp());
                        break;
                    case "week":
                        readings = waterReadingPerDayService.getReadings(request.getDeviceId(), request.getStartTimestamp(), request.getEndTimestamp());
                        break;
                    case "month":
                        readings = waterReadingPerWeekService.getReadings(request.getDeviceId(), request.getStartTimestamp(), request.getEndTimestamp());
                        break;
                    case "year":
                        readings = waterReadingPerMonthService.getReadings(request.getDeviceId(), request.getStartTimestamp(), request.getEndTimestamp());
                        break;
                    default:
                        return ResponseEntity.badRequest().body("Invalid resolution specified.");
                }
                return ResponseEntity.ok(readings);
            } catch (IllegalArgumentException e) {
                return ResponseEntity.badRequest().body(e.getMessage());
            }
        }
        return ResponseEntity.badRequest().body("UnAuthorised Access. Please login again!");
    }

    @PostMapping("/reading/report/home")
    public ResponseEntity<HomeReportResponse> getCumulativeFlowReading(
            @RequestBody WaterReadingHomeRequest request,
            @RequestHeader("Authorization") String jwttoken) {

        String token = jwttoken.substring(7);
        String contactNoByToken = jwtUtil.extractContactNo(token);
        Optional<User> userOptional = userService.getUserByContactNo(contactNoByToken);

        // Check if user exists and userId matches the request
        if (userOptional.isPresent() && Objects.equals(userOptional.get().getUserId(), request.getUserId())) {
            try {
                logger.info("Received request for cumulative flow reading: " + request);

                HomeReportResponse cumulativeFlowReading = waterReadingAggregationService.fetchFlowReadingsForDevices(
                        request.getHomeId(),
                        request.getStartTimestamp(),
                        request.getEndTimestamp(),
                        request.getResolution()
                );

                return ResponseEntity.ok(cumulativeFlowReading);

            } catch (IllegalArgumentException e) {
                // Returning a more suitable response for errors
                return ResponseEntity.badRequest().body((HomeReportResponse) Collections.singletonMap("error", e.getMessage()));
            }
        }

        // Unauthorized access case
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body((HomeReportResponse) Collections.singletonMap("error", "Unauthorized Access. Please log in again!"));
    }


//    @GetMapping("latest/{homeId}")
//    public Object getTodayUsagebyHomeId(@PathVariable String homeId) {
//        return waterReadingService.updateTodaysUsageByHomeId(homeId);
//    }

//    @GetMapping("/latesttoday")
//    public Object getTodayUsagebyHomeId() {
//        return waterReadingService.updateTodaysUsageByHomeId("831ed47a-97fc-4942-84c4-286f47901b24");
//    }
//
//    @GetMapping("/latestmonth")
//    public Object getMonthUsagebyHomeId() {
//        return waterReadingService.updateMonthUsageByHomeId("831ed47a-97fc-4942-84c4-286f47901b24");
//    }
//
//    @GetMapping("/lastmonth")
//    public Object getLastMonthUsagebyHomeId() {
//        return waterReadingService.updateLastMonthUsageByHomeId("831ed47a-97fc-4942-84c4-286f47901b24");
//    }

//    @GetMapping("latest/{deviceId}")
//    public WaterReading getTodayUsagebyDeviceId(@PathVariable String deviceId) {
//        return waterReadingService.getLatestReadingById(deviceId);
//    }

    @GetMapping("/{deviceId}")
    public List<WaterReading> getReadingsByDeviceId(@PathVariable String deviceId) {
        return waterReadingService.getWaterReadingsByDeviceId(deviceId);
    }

    @GetMapping("/latest-readings")
    public List<WaterReading> getLatestReadingsForAllDevices() {
        return waterReadingService.getLatestReadingsForAllDevices();
    }

    @PostMapping("/deviceData")
    public ResponseEntity<?> getDeviceData(@RequestBody Map<String, String> request) throws Exception {
        // Get the initial pin from the request
        String initialPin = request.get("initialPin");
        String deviceId = request.get("deviceId");
        String startTimestamp = request.get("startTimestamp");
        String endTimestamp = request.get("endTimestamp");
        String resolution = request.get("resolution");

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

        DeviceReportResponse readings;
        switch (resolution.toLowerCase()) {
            case "day":
                readings = waterReadingPerHourService.getReadings(deviceId, startTimestamp, endTimestamp);
                break;
            case "week":
                readings = waterReadingPerDayService.getReadings(deviceId, startTimestamp, endTimestamp);
                break;
            case "month":
                readings = waterReadingPerWeekService.getReadings(deviceId, startTimestamp, endTimestamp);
                break;
            case "year":
                readings = waterReadingPerMonthService.getReadings(deviceId, startTimestamp, endTimestamp);
                break;
            default:
                return ResponseEntity.badRequest().body("Invalid resolution specified.");
        }
        return ResponseEntity.ok(readings);
    }


}
