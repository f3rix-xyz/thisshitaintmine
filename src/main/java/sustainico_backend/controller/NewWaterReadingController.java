package sustainico_backend.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import sustainico_backend.Models.NewWaterReading;
import sustainico_backend.service.NewWaterReadingService;


import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/newWaterReading")
public class NewWaterReadingController {

    private final NewWaterReadingService newWaterReadingService;

    @Autowired
    public NewWaterReadingController(NewWaterReadingService newWaterReadingService) {
        this.newWaterReadingService = newWaterReadingService;
    }

//    @PostMapping("/send")
//    public ResponseEntity<Void> createNewWaterReading(@RequestBody NewWaterReading newWaterReading) {
//        newWaterReadingService.saveNewWaterReading(newWaterReading);
//        return new ResponseEntity<>(HttpStatus.CREATED);
//    }

    @PostMapping("/send")
    public ResponseEntity<Void> createNewWaterReading(@RequestBody Map<String, Object> webhookPayload) {
        // Extract the data from the webhookPayload
//        Map<String, Object> data = (Map<String, Object>) webhookPayload.get("data");

        // Extract the uplink_message from the data
        Map<String, Object> uplinkMessage = (Map<String, Object>) webhookPayload.get("uplink_message");

        // Extract the decoded_payload from the uplinkMessage
        Map<String, Object> decodedPayload = (Map<String, Object>) uplinkMessage.get("decoded_payload");
//        System.out.println("decodedPayload "+decodedPayload);

        // Map the decodedPayload to the NewWaterReading model
        NewWaterReading newWaterReading = mapToNewWaterReading(decodedPayload);

        // Generate readingId
        newWaterReading.generateReadingId();

        // Get the current epoch timestamp
        long currentEpochTimestamp = Instant.now().getEpochSecond();

//        System.out.println("currentEpochTimestamp "+currentEpochTimestamp);

        long startTimestamp = (newWaterReading.getKey() * newWaterReading.getIndex() * 3600L) + 1577817000L;
//        System.out.println("startTimestamp "+startTimestamp);

        // Check if the reading's timestamp is less than the current epoch timestamp
        if (startTimestamp < currentEpochTimestamp && newWaterReading.getTotalizer() > 0) {
//            System.out.println("startTimestamp "+startTimestamp);
            newWaterReadingService.saveNewWaterReading(newWaterReading);
        }

        return new ResponseEntity<>(HttpStatus.OK);
    }

    private NewWaterReading mapToNewWaterReading(Map<String, Object> decodedPayload) {
        NewWaterReading newWaterReading = new NewWaterReading();
        newWaterReading.setDeviceId((String) decodedPayload.get("deviceId"));
        newWaterReading.setTimestamp((String) decodedPayload.get("timestamp")); // Ensure this field is present
        newWaterReading.setIndex((Integer) decodedPayload.get("index"));
        newWaterReading.setAlerts((Map<String, Boolean>) decodedPayload.get("alerts"));
        newWaterReading.setStatus((Map<String, Boolean>) decodedPayload.get("status"));
        newWaterReading.setTotalizer((Integer) decodedPayload.get("totalizer"));
        newWaterReading.setIntraDay((List<Integer>) decodedPayload.get("intraDay"));
        newWaterReading.setKey((Integer) decodedPayload.get("key"));
        // Add any other necessary field mappings here

        return newWaterReading;
    }


    @GetMapping("/{deviceId}/{timestamp}")
    public ResponseEntity<NewWaterReading> getNewWaterReading(@PathVariable String deviceId, @PathVariable String timestamp) {
        Optional<NewWaterReading> newWaterReading = newWaterReadingService.getNewWaterReading(deviceId, timestamp);
        return newWaterReading.map(ResponseEntity::ok).orElseGet(() -> new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }

    @DeleteMapping("/{deviceId}/{timestamp}")
    public ResponseEntity<Void> deleteNewWaterReading(@PathVariable String deviceId, @PathVariable String timestamp) {
        newWaterReadingService.deleteNewWaterReading(deviceId, timestamp);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }
}
