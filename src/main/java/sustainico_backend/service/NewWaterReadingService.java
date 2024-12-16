package sustainico_backend.service;

import com.amazonaws.services.dynamodbv2.datamodeling.AbstractDynamoDBMapper;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBQueryExpression;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;

import io.jsonwebtoken.lang.Collections;
import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import sustainico_backend.Models.LatestWaterReading;
import sustainico_backend.Models.NewWaterReading;
import sustainico_backend.Models.StatusChangeResponse;
import sustainico_backend.Models.WaterReading;
import sustainico_backend.rep.LatestWaterReadingRepository;
import sustainico_backend.rep.NewWaterReadingRepository;
import sustainico_backend.rep.WaterReadingRepository;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class NewWaterReadingService {

    private final NewWaterReadingRepository newWaterReadingRepository;
    private final DynamoDBMapper dynamoDBMapper;

    @Autowired
    public NewWaterReadingService(NewWaterReadingRepository newWaterReadingRepository, DynamoDBMapper dynamoDBMapper) {
        this.newWaterReadingRepository = newWaterReadingRepository;
        this.dynamoDBMapper = dynamoDBMapper;
    }

    @Getter
    @Autowired
    private WaterReadingRepository waterReadingRepository;

    @Autowired
    public LatestWaterReadingRepository latestWaterReadingRepository;

    public List<WaterReading> convertNewToWaterReadings(NewWaterReading newWaterReading) {
        List<WaterReading> waterReadings = new ArrayList<>();

        // Calculate startTimestamp based on the given formula
        long startTimestamp = (newWaterReading.getKey() * newWaterReading.getIndex() * 3600L) + 1577817000L;

        // Get the list of flow readings (intraDay) and key (duration multiplier in
        // minutes)
        List<Integer> intraDay = newWaterReading.getIntraDay();
        int key = newWaterReading.getKey();
        int totalizer = newWaterReading.getTotalizer();

        // Calculate flow readings in reverse order
        for (int i = intraDay.size() - 1; i >= 0; i--) {
            WaterReading waterReading = new WaterReading();

            // Set the deviceId and flowReading
            waterReading.setDeviceId(newWaterReading.getDeviceId());
            waterReading.setFlowReading(String.valueOf(totalizer));

            // Calculate the timestamp for each WaterReading
            long currentTimestamp = startTimestamp + (i * key * 5 * 60);
            waterReading.setTimestamp(String.valueOf(currentTimestamp));

            // Generate readingId
            waterReading.generateReadingId();

            waterReadings.add(waterReading);

            // Deduct the intraDay value from the totalizer
            totalizer -= intraDay.get(i);
        }

        return waterReadings;
    }

    public void saveNewWaterReading(NewWaterReading newWaterReading) {
        // Set the timestamp to the current epoch time in seconds (IST) if it's null
        if (newWaterReading.getTimestamp() == null) {
            ZonedDateTime now = ZonedDateTime.now(ZoneId.of("Asia/Kolkata"));
            long epochSeconds = now.toEpochSecond();
            newWaterReading.setTimestamp(String.valueOf(epochSeconds));
        }
        newWaterReading.generateReadingId();
        newWaterReadingRepository.save(newWaterReading);

        List<WaterReading> waterReadings = convertNewToWaterReadings(newWaterReading);

        // Batch save all WaterReading objects
        dynamoDBMapper.batchSave(waterReadings);

        // Update LatestWaterReading
        WaterReading waterReading = waterReadings.get(0);
        LatestWaterReading latestWaterReading = latestWaterReadingRepository.findByDeviceId(waterReading.getDeviceId());
        if (latestWaterReading == null) {
            latestWaterReading = new LatestWaterReading();
            latestWaterReading.setDeviceId(waterReading.getDeviceId());
            latestWaterReading.setLatestFlowReading(waterReading.getFlowReading());
            latestWaterReading.setTimestamp(waterReading.getTimestamp());
            latestWaterReading.setFirstReadingOfMonth(waterReading.getFlowReading());
            latestWaterReading.setFirstFlowReading(waterReading.getFlowReading());
            latestWaterReading.setTimestampOfMonth(waterReading.getTimestamp());
        }

        // Define the date format used in the timestamp string
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss"); // Adjust format if
                                                                                            // necessary

        // Parse the timestamp strings into LocalDateTime objects
        LocalDateTime latestTimestamp = LocalDateTime.parse(latestWaterReading.getTimestamp(), formatter);
        LocalDateTime waterReadingTimestamp = LocalDateTime.parse(waterReading.getTimestamp(), formatter);

        // Extract only the date (ignores the time)
        LocalDate latestDate = latestTimestamp.toLocalDate();
        LocalDate waterReadingDate = waterReadingTimestamp.toLocalDate();

        // If the latestWaterReading is from a previous date
        if (latestDate.isBefore(waterReadingDate)) {
            // Update the firstFlowReading with the new waterReading's flow reading
            latestWaterReading.setFirstFlowReading(waterReading.getFlowReading());
        }

        // Update the latestFlowReading and timestamp with the latest waterReading
        // values
        latestWaterReading.setLatestFlowReading(waterReading.getFlowReading());
        latestWaterReading.setTimestamp(waterReading.getTimestamp());

        // Parse the timestamp strings into LocalDateTime objects
        LocalDateTime timestampOfMonth = LocalDateTime.parse(latestWaterReading.getTimestampOfMonth(), formatter);
        // LocalDateTime waterReadingTimestamp =
        // LocalDateTime.parse(waterReading.getTimestamp(), formatter);

        // Compare year and month of timestampOfMonth with waterReadingTimestamp
        if (timestampOfMonth.getYear() < waterReadingTimestamp.getYear() ||
                (timestampOfMonth.getYear() == waterReadingTimestamp.getYear()
                        && timestampOfMonth.getMonthValue() < waterReadingTimestamp.getMonthValue())) {

            // Update firstReadingOfMonth with the new waterReading's flow reading
            latestWaterReading.setFirstReadingOfMonth(waterReading.getFlowReading());

            // Update timestampOfMonth with the new waterReading timestamp
            latestWaterReading.setTimestampOfMonth(waterReading.getTimestamp());

            // Set lastReadingOfMonth to the current latestFlowReading
            latestWaterReading.setLastReadingOfMonth(latestWaterReading.getLatestFlowReading());
        }

        // Save the updated latestWaterReading
        latestWaterReadingRepository.save(latestWaterReading);
    }

    public Optional<NewWaterReading> getNewWaterReading(String deviceId, String timestamp) {
        return newWaterReadingRepository.findById(deviceId, timestamp);
    }

    public void deleteNewWaterReading(String deviceId, String timestamp) {
        newWaterReadingRepository.delete(deviceId, timestamp);
    }

    public List<StatusChangeResponse> getStatusChangesForLastThreeDays(String deviceId) {
        try {
            // Calculate timestamp for 3 days ago
            ZonedDateTime now = ZonedDateTime.now(ZoneId.of("Asia/Kolkata"));
            ZonedDateTime threeDaysAgo = now.minusDays(3);

            // Convert to epoch seconds
            long startTimestamp = threeDaysAgo.toEpochSecond();
            long endTimestamp = now.toEpochSecond();

            Map<String, AttributeValue> eav = new HashMap<>();
            eav.put(":deviceId", new AttributeValue().withS(deviceId));
            eav.put(":startTimestamp", new AttributeValue().withS(String.valueOf(startTimestamp)));
            eav.put(":endTimestamp", new AttributeValue().withS(String.valueOf(endTimestamp)));

            DynamoDBQueryExpression<NewWaterReading> queryExpression = new DynamoDBQueryExpression<NewWaterReading>()
                    .withKeyConditionExpression(
                            "deviceId = :deviceId and timestamp between :startTimestamp and :endTimestamp")
                    .withExpressionAttributeValues(eav);

            List<NewWaterReading> readings = dynamoDBMapper.query(NewWaterReading.class, queryExpression);

            // Process and filter readings with status changes (N:1)
            List<StatusChangeResponse> statusChanges = new ArrayList<>();

            for (NewWaterReading reading : readings) {
                Map<String, Boolean> changes = new HashMap<>();
                boolean hasChange = false;

                // Check each status field for N:1
                for (Map.Entry<String, Boolean> entry : reading.getStatus().entrySet()) {
                    if (entry.getValue()) { // If status is true (N:1)
                        changes.put(entry.getKey(), true);
                        hasChange = true;
                    }
                }

                // If any status was N:1, add to response
                if (hasChange) {
                    // Convert epoch timestamp to readable format
                    ZonedDateTime readingTime = ZonedDateTime.ofInstant(
                            Instant.ofEpochSecond(Long.parseLong(reading.getTimestamp())),
                            ZoneId.of("Asia/Kolkata"));

                    String formattedTime = readingTime.format(
                            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

                    statusChanges.add(new StatusChangeResponse(formattedTime, changes));
                }
            }

            return statusChanges;

        } catch (Exception e) {
            // logger.warning("Error retrieving status changes: " + e.getMessage());
            return Collections.emptyList();
        }
    }
}
