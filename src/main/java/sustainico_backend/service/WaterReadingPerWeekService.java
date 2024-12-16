package sustainico_backend.service;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBQueryExpression;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import sustainico_backend.Models.DeviceReportResponse;
import sustainico_backend.Models.WaterReadingPerDay;
import sustainico_backend.Models.WaterReadingPerHour;
import sustainico_backend.Models.WaterReadingPerWeek;
import sustainico_backend.rep.WaterReadingPerDayRepository;
import sustainico_backend.rep.WaterReadingPerWeekRepository;

import java.time.Instant;
import java.util.*;
import java.util.logging.Logger;
import java.util.stream.Collectors;

@Service
public class WaterReadingPerWeekService {

    @Autowired
    private DynamoDBMapper dynamoDBMapper;

    @Autowired
    private WaterReadingPerDayService waterReadingPerDayService;

    @Autowired
    private WaterReadingPerWeekRepository waterReadingPerWeekRepository;

    private static final Logger logger = Logger.getLogger(WaterReadingPerWeekService.class.getName());

    public void save(WaterReadingPerWeek waterReadingPerWeek) {
        waterReadingPerWeekRepository.save(waterReadingPerWeek);
    }

    public List<WaterReadingPerWeek> findAll() {
        return waterReadingPerWeekRepository.findAll();
    }

    public List<WaterReadingPerWeek> getWaterReadingsByDeviceId(String deviceId) {
        return waterReadingPerWeekRepository.findWaterReadingByDeviceId(deviceId);
    }

    private List<String> getDeviceIds() {
        return List.of("device1", "device2"); // Example
    }

    @Scheduled(cron = "0 0 0 ? * SUN", zone = "Asia/Kolkata") // Every week on Sunday at midnight in IST
    public void storeDailyReadings() {
        try {
            List<WaterReadingPerDay> latestReadings = waterReadingPerDayService.getLatestReadingsForAllDevices();
            for (WaterReadingPerDay latestReading : latestReadings) {
                WaterReadingPerWeek waterReadingPerWeek = new WaterReadingPerWeek();
                waterReadingPerWeek.setDeviceId(latestReading.getDeviceId());
                waterReadingPerWeek.setTimestamp(latestReading.getTimestamp());
                waterReadingPerWeek.setFlowReading(latestReading.getFlowReading());
                waterReadingPerWeek.setFetchTimestamp(String.valueOf(Instant.now().getEpochSecond()));
                waterReadingPerWeekRepository.save(waterReadingPerWeek);
            }
        } catch (Exception err){
            System.out.println("Error during the aggregation task : " + err);
        }
        System.out.println(Calendar.getInstance(TimeZone.getTimeZone("Asia/Kolkata")).getTime() + " WEEK!");
    }

    public List<WaterReadingPerWeek> getLatestReadingsForAllDevices() {
        List<String> deviceIds = getAllDeviceIds(); // Implement this method to get all device IDs
        return deviceIds.stream()
                .map(this::getLatestReading)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toList());
    }

    public Optional<WaterReadingPerWeek> getLatestReading(String deviceId) {
        List<WaterReadingPerWeek> readings = waterReadingPerWeekRepository.findWaterReadingByDeviceId(deviceId);
        return readings.stream()
                .max((r1, r2) -> {
                    try {
                        return Long.compare(Long.parseLong(r1.getTimestamp()), Long.parseLong(r2.getTimestamp()));
                    } catch (NumberFormatException e) {
                        return 0;
                    }
                });
    }

    private List<String> getAllDeviceIds() {
        List<WaterReadingPerWeek> readings = waterReadingPerWeekRepository.findAll();
        return readings.stream()
                .map(WaterReadingPerWeek::getDeviceId)
                .distinct()
                .collect(Collectors.toList());
    }

    public List<WaterReadingPerWeek> getReadings2(String deviceId, String startTimestamp, String endTimestamp) {
        long startTimeInSeconds = roundToNearestWeek(Long.parseLong(startTimestamp));
        long endTimeInSeconds = Long.parseLong(endTimestamp);

        WaterReadingPerWeek startReading = getNearestWeeklyReading(deviceId, Long.toString(startTimeInSeconds), true);
        WaterReadingPerWeek endReading = getNearestWeeklyReading(deviceId, endTimestamp, false);

        if (startReading == null || endReading == null) {
            logger.warning("No start or end reading found.");
            return Collections.emptyList(); // Return an empty list instead of null
        }

        Map<String, AttributeValue> eav = new HashMap<>();
        eav.put(":deviceId", new AttributeValue().withS(deviceId));
        eav.put(":startTimestamp", new AttributeValue().withS(startReading.getFetchTimestamp()));
        eav.put(":endTimestamp", new AttributeValue().withS(endReading.getFetchTimestamp()));

        DynamoDBQueryExpression<WaterReadingPerWeek> queryExpression = new DynamoDBQueryExpression<WaterReadingPerWeek>()
                .withKeyConditionExpression("deviceId = :deviceId and fetchTimestamp between :startTimestamp and :endTimestamp")
                .withExpressionAttributeValues(eav);

        List<WaterReadingPerWeek> result = dynamoDBMapper.query(WaterReadingPerWeek.class, queryExpression);
//        return result != null ? result : Collections.emptyList(); // Return an empty list if the result is null

        List<WaterReadingPerWeek> modifiedReadings = new ArrayList<>();

        for (int i = 1; i < result.size(); i++) {
            WaterReadingPerWeek prevReading = result.get(i - 1);
            WaterReadingPerWeek currentReading = result.get(i);

            double prevFlowReading = Double.parseDouble(prevReading.getFlowReading());
            double currentFlowReading = Double.parseDouble(currentReading.getFlowReading());
            double flowDifference = currentFlowReading - prevFlowReading;

            // Update the flowReading and timestamp of the currentReading
//            prevReading.setFlowReading(Double.toString(flowDifference));
//            prevReading.setFetchTimestamp(currentReading.getFetchTimestamp());
//            prevReading.setTimestamp(currentReading.getTimestamp());

            WaterReadingPerWeek newWeekReading = new WaterReadingPerWeek();

            newWeekReading.setDeviceId(currentReading.getDeviceId());
            newWeekReading.setFlowReading(Double.toString(flowDifference));
            newWeekReading.setFetchTimestamp(currentReading.getFetchTimestamp());
            newWeekReading.setTimestamp(currentReading.getTimestamp());
            newWeekReading.setReadingId(currentReading.getReadingId());

            // Add modified reading to the list
            modifiedReadings.add(newWeekReading);
        }

        return modifiedReadings; // Return an empty list if the result is null
    }

    public DeviceReportResponse getReadings(String deviceId, String startTimestamp, String endTimestamp) {
        // Convert timestamps to long
        long startTimeInSeconds = Long.parseLong(startTimestamp);
        long endTimeInSeconds = Long.parseLong(endTimestamp);

        // Adjust the timestamps
        long oneWeekInSeconds = 604800; // 604800 seconds in a week
        long adjustedStartTimeInSeconds = startTimeInSeconds - oneWeekInSeconds;
        long adjustedEndTimeInSeconds = endTimeInSeconds + oneWeekInSeconds;

        // Directly querying between the adjusted start and end timestamps
        Map<String, AttributeValue> eav = new HashMap<>();
        eav.put(":deviceId", new AttributeValue().withS(deviceId));
        eav.put(":startTimestamp", new AttributeValue().withS(Long.toString(adjustedStartTimeInSeconds)));
        eav.put(":endTimestamp", new AttributeValue().withS(Long.toString(adjustedEndTimeInSeconds)));

        DynamoDBQueryExpression<WaterReadingPerWeek> queryExpression = new DynamoDBQueryExpression<WaterReadingPerWeek>()
                .withKeyConditionExpression("deviceId = :deviceId and fetchTimestamp between :startTimestamp and :endTimestamp")
                .withExpressionAttributeValues(eav);

        List<WaterReadingPerWeek> result = dynamoDBMapper.query(WaterReadingPerWeek.class, queryExpression);


        DeviceReportResponse report = new DeviceReportResponse();

        // Compute flow readings as differences and omit the first reading
        List<WaterReadingPerWeek> modifiedReadings = new ArrayList<>();
        double peakUsage = 0;
        double totalUsage = 0;

        for (int i = 1; i < result.size(); i++) {
            WaterReadingPerWeek prevReading = result.get(i - 1);
            WaterReadingPerWeek currentReading = result.get(i);

            double prevFlowReading = Double.parseDouble(prevReading.getFlowReading());
            double currentFlowReading = Double.parseDouble(currentReading.getFlowReading());
            double flowDifference = currentFlowReading - prevFlowReading;

            // Ensure flowDifference is not less than 0
            flowDifference = Math.max(flowDifference, 0);
            totalUsage = totalUsage + flowDifference;


            // Update the flowReading and timestamp of the currentReading
//            prevReading.setFlowReading(Double.toString(flowDifference));
//            prevReading.setFetchTimestamp(currentReading.getFetchTimestamp());
//            prevReading.setTimestamp(currentReading.getTimestamp());

            WaterReadingPerWeek newWeekReading = new WaterReadingPerWeek();

            newWeekReading.setDeviceId(currentReading.getDeviceId());
            newWeekReading.setFlowReading(Double.toString(flowDifference));
            newWeekReading.setFetchTimestamp(currentReading.getFetchTimestamp());
            newWeekReading.setTimestamp(currentReading.getTimestamp());
            newWeekReading.setReadingId(currentReading.getReadingId());

            // Track peak usage
            if (flowDifference > peakUsage) {
                peakUsage = flowDifference;
            }

            // Add modified reading to the list
            modifiedReadings.add(newWeekReading);
        }

        report.setReadings(modifiedReadings);

        if (modifiedReadings.size() > 0) {
//            double totalUsage = Double.parseDouble(result.get(result.size()-1).getFlowReading()) - Double.parseDouble(result.get(0).getFlowReading());
            totalUsage = Math.max(totalUsage, 0);
            report.setAverageUsage(totalUsage / modifiedReadings.size());
        } else {
            report.setAverageUsage(0); // Handle the case of insufficient data
        }

        peakUsage = Math.max(peakUsage, 0);
        report.setPeakUsage(peakUsage);

        // Placeholders for other calculations
        report.setContinuesFlowPercentage(0); // Placeholder
        report.setEstimatedLeakage(0); // Placeholder

        return report;
    }


    private WaterReadingPerWeek getNearestWeeklyReading(String deviceId, String timestamp, boolean isStart) {
        long timestampInSeconds = Long.parseLong(timestamp);
        long startRange = isStart ? roundToNearestWeek(timestampInSeconds) : timestampInSeconds - 604800;
        long endRange = isStart ? startRange + 604800 : timestampInSeconds;

        Map<String, AttributeValue> eav = new HashMap<>();
        eav.put(":deviceId", new AttributeValue().withS(deviceId));
        eav.put(":startRange", new AttributeValue().withS(Long.toString(startRange)));
        eav.put(":endRange", new AttributeValue().withS(Long.toString(endRange)));

        DynamoDBQueryExpression<WaterReadingPerWeek> queryExpression = new DynamoDBQueryExpression<WaterReadingPerWeek>()
                .withKeyConditionExpression("deviceId = :deviceId and fetchTimestamp between :startRange and :endRange")
                .withExpressionAttributeValues(eav)
                .withScanIndexForward(!isStart); // Scan forward for start and backward for end

        List<WaterReadingPerWeek> readings = dynamoDBMapper.query(WaterReadingPerWeek.class, queryExpression);
        if (readings.isEmpty()) {
            return null;
        }

        // Find the reading closest to the given timestamp
        WaterReadingPerWeek nearestReading = readings.get(0);
        long nearestDiff = Math.abs(Long.parseLong(nearestReading.getFetchTimestamp()) - timestampInSeconds);
        for (WaterReadingPerWeek reading : readings) {
            long diff = Math.abs(Long.parseLong(reading.getFetchTimestamp()) - timestampInSeconds);
            if (diff < nearestDiff) {
                nearestReading = reading;
                nearestDiff = diff;
            }
        }

        return nearestReading;
    }

    private long roundToNearestWeek(long timestampInSeconds) {
        return (timestampInSeconds / 604800) * 604800; // 604800 seconds in a week
    }
}
