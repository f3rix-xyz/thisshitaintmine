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
import sustainico_backend.Models.MaxFlowAlertResponse;
import sustainico_backend.Models.NewWaterReading;
import sustainico_backend.Models.SingleAlertResponse;
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
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

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

            long startTimestamp = threeDaysAgo.toEpochSecond();
            long endTimestamp = now.toEpochSecond();

            System.out.println("\n=== Time Range ===");
            System.out
                    .println("Start Time: " + threeDaysAgo.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
            System.out.println("End Time: " + now.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));

            Map<String, AttributeValue> eav = new HashMap<>();
            eav.put(":deviceId", new AttributeValue().withS(deviceId));
            eav.put(":startTimestamp", new AttributeValue().withS(String.valueOf(startTimestamp)));
            eav.put(":endTimestamp", new AttributeValue().withS(String.valueOf(endTimestamp)));

            Map<String, String> expressionAttributeNames = new HashMap<>();
            expressionAttributeNames.put("#ts", "timestamp");

            DynamoDBQueryExpression<NewWaterReading> queryExpression = new DynamoDBQueryExpression<NewWaterReading>()
                    .withKeyConditionExpression(
                            "deviceId = :deviceId and #ts between :startTimestamp and :endTimestamp")
                    .withExpressionAttributeNames(expressionAttributeNames)
                    .withExpressionAttributeValues(eav);

            List<NewWaterReading> readings = dynamoDBMapper.query(NewWaterReading.class, queryExpression);

            System.out.println("\n=== Query Results ===");
            System.out.println("Total readings found: " + readings.size());

            List<StatusChangeResponse> statusChanges = new ArrayList<>();

            for (NewWaterReading reading : readings) {
                System.out.println("\n--- Processing Reading ---");
                System.out.println("Reading Timestamp: " + reading.getTimestamp());
                System.out.println("Raw Status: " + reading.getStatus());

                Map<String, Boolean> changes = new HashMap<>();
                boolean hasChange = false;

                Map<String, Boolean> status = reading.getStatus();
                if (status != null) {
                    for (Map.Entry<String, Boolean> entry : status.entrySet()) {
                        String statusType = entry.getKey();
                        Boolean isActive = entry.getValue();

                        System.out.println("Checking Status: " + statusType + " = " + isActive);

                        if (isActive != null && isActive) {
                            changes.put(statusType, true);
                            hasChange = true;
                            System.out.println("Found active status: " + statusType);
                        }
                    }
                }

                if (hasChange) {
                    ZonedDateTime readingTime = ZonedDateTime.ofInstant(
                            Instant.ofEpochSecond(Long.parseLong(reading.getTimestamp())),
                            ZoneId.of("Asia/Kolkata"));
                    String formattedTime = readingTime.format(
                            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

                    statusChanges.add(new StatusChangeResponse(formattedTime, changes));
                    System.out.println("Added status change at: " + formattedTime);
                }
            }

            System.out.println("\n=== Final Results ===");
            System.out.println("Total status changes found: " + statusChanges.size());
            for (StatusChangeResponse status : statusChanges) {
                System.out.println("Time: " + status.getTimestamp() + ", Changes: " + status.getStatus());
            }

            return statusChanges;

        } catch (Exception e) {
            System.out.println("\n=== ERROR ===");
            System.out.println("Error retrieving status changes: " + e.getMessage());
            e.printStackTrace();
            return Collections.emptyList();
        }
    }

    private static final Set<String> EXCLUDED_ALERTS = new HashSet<>(Arrays.asList(
            "MaxFlow", "LeakFlow", "NoConsumption"));

    public List<SingleAlertResponse> getAlertChangesForLastThreeDays(String deviceId) {
        try {
            ZonedDateTime now = ZonedDateTime.now(ZoneId.of("Asia/Kolkata"));
            ZonedDateTime threeDaysAgo = now.minusDays(3);

            long startTimestamp = threeDaysAgo.toEpochSecond();
            long endTimestamp = now.toEpochSecond();

            Map<String, AttributeValue> eav = new HashMap<>();
            eav.put(":deviceId", new AttributeValue().withS(deviceId));
            eav.put(":startTimestamp", new AttributeValue().withS(String.valueOf(startTimestamp)));
            eav.put(":endTimestamp", new AttributeValue().withS(String.valueOf(endTimestamp)));

            Map<String, String> expressionAttributeNames = new HashMap<>();
            expressionAttributeNames.put("#ts", "timestamp");

            DynamoDBQueryExpression<NewWaterReading> queryExpression = new DynamoDBQueryExpression<NewWaterReading>()
                    .withKeyConditionExpression(
                            "deviceId = :deviceId and #ts between :startTimestamp and :endTimestamp")
                    .withExpressionAttributeNames(expressionAttributeNames)
                    .withExpressionAttributeValues(eav);

            List<NewWaterReading> readings = dynamoDBMapper.query(NewWaterReading.class, queryExpression);

            System.out.println("Total readings found: " + readings.size());

            List<SingleAlertResponse> alertChanges = new ArrayList<>();
            Set<String> excludedAlerts = new HashSet<>(Arrays.asList("MaxFlow", "LeakFlow", "NoConsumption"));

            for (NewWaterReading reading : readings) {
                System.out.println("\n--- Processing Reading ---");
                System.out.println("Reading Timestamp: " + reading.getTimestamp());
                System.out.println("Raw Alerts: " + reading.getAlerts());

                ZonedDateTime readingTime = ZonedDateTime.ofInstant(
                        Instant.ofEpochSecond(Long.parseLong(reading.getTimestamp())),
                        ZoneId.of("Asia/Kolkata"));
                String formattedTime = readingTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

                Map<String, Boolean> alerts = reading.getAlerts();
                if (alerts != null) {
                    for (Map.Entry<String, Boolean> entry : alerts.entrySet()) {
                        String alertType = entry.getKey();
                        Boolean isAlert = entry.getValue();

                        System.out.println("\nChecking Alert: " + alertType);
                        System.out.println("Alert Value: " + isAlert);
                        System.out.println("Is Excluded: " + excludedAlerts.contains(alertType));

                        // Check if the alert value is true (equivalent to N:1)
                        if (!excludedAlerts.contains(alertType) && isAlert != null && isAlert) {
                            System.out.println(">>> Adding alert to response: " + alertType);
                            alertChanges.add(new SingleAlertResponse(alertType, formattedTime));
                        }
                    }
                }
            }

            System.out.println("\n=== Final Results ===");
            System.out.println("Total alerts found: " + alertChanges.size());
            for (SingleAlertResponse alert : alertChanges) {
                System.out.println("Alert: " + alert.getAlert() + " at " + alert.getTimestamp());
            }

            return alertChanges;

        } catch (Exception e) {
            System.out.println("\n=== ERROR ===");
            System.out.println("Error retrieving alert changes: " + e.getMessage());
            e.printStackTrace();
            return Collections.emptyList();
        }
    }

    public MaxFlowAlertResponse getYesterdayMaxFlowPercentage(String deviceId) {
        try {
            // Calculate yesterday's start and end timestamps
            ZonedDateTime now = ZonedDateTime.now(ZoneId.of("Asia/Kolkata"));
            ZonedDateTime yesterdayStart = now.minusDays(1).withHour(0).withMinute(0).withSecond(0);
            ZonedDateTime yesterdayEnd = yesterdayStart.withHour(23).withMinute(59).withSecond(59);

            String dateStr = yesterdayStart.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            long startTimestamp = yesterdayStart.toEpochSecond();
            long endTimestamp = yesterdayEnd.toEpochSecond();

            System.out.println("Fetching data for: " + dateStr);
            System.out.println("Start timestamp: " + startTimestamp);
            System.out.println("End timestamp: " + endTimestamp);

            Map<String, AttributeValue> eav = new HashMap<>();
            eav.put(":deviceId", new AttributeValue().withS(deviceId));
            eav.put(":startTimestamp", new AttributeValue().withS(String.valueOf(startTimestamp)));
            eav.put(":endTimestamp", new AttributeValue().withS(String.valueOf(endTimestamp)));

            Map<String, String> expressionAttributeNames = new HashMap<>();
            expressionAttributeNames.put("#ts", "timestamp");

            DynamoDBQueryExpression<NewWaterReading> queryExpression = new DynamoDBQueryExpression<NewWaterReading>()
                    .withKeyConditionExpression(
                            "deviceId = :deviceId and #ts between :startTimestamp and :endTimestamp")
                    .withExpressionAttributeNames(expressionAttributeNames)
                    .withExpressionAttributeValues(eav);

            List<NewWaterReading> readings = dynamoDBMapper.query(NewWaterReading.class, queryExpression);

            System.out.println("Found " + readings.size() + " readings");

            // Calculate MaxFlow statistics
            int totalReadings = readings.size();
            int maxFlowAlerts = 0;

            for (NewWaterReading reading : readings) {
                Map<String, Boolean> alerts = reading.getAlerts();
                if (alerts != null &&
                        alerts.containsKey("MaxFlow") &&
                        alerts.get("MaxFlow") != null &&
                        alerts.get("MaxFlow")) {
                    maxFlowAlerts++;
                    System.out.println("Found MaxFlow alert at timestamp: " + reading.getTimestamp());
                }
            }

            double percentage = totalReadings > 0 ? ((double) maxFlowAlerts / totalReadings) * 100 : 0;

            System.out.println("Total Readings: " + totalReadings);
            System.out.println("MaxFlow Alerts: " + maxFlowAlerts);
            System.out.println("Percentage: " + percentage + "%");

            MaxFlowAlertResponse response = new MaxFlowAlertResponse();
            response.setPercentage(Math.round(percentage * 100.0) / 100.0);
            response.setTotalReadings(totalReadings);
            response.setMaxFlowAlerts(maxFlowAlerts);
            response.setDate(dateStr);

            return response;

        } catch (Exception e) {
            System.out.println("Error calculating MaxFlow percentage: " + e.getMessage());
            e.printStackTrace();

            MaxFlowAlertResponse response = new MaxFlowAlertResponse();
            response.setPercentage(0.0);
            response.setTotalReadings(0);
            response.setMaxFlowAlerts(0);
            response.setDate("Error");
            return response;
        }
    }

}
