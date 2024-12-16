package sustainico_backend.service;


import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBQueryExpression;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import sustainico_backend.Models.*;
import sustainico_backend.rep.WaterReadingPerMonthRepository;

import java.time.Instant;
import java.util.*;
import java.util.logging.Logger;
import java.util.stream.Collectors;

@Service
public class WaterReadingPerMonthService {

    @Autowired
    private DynamoDBMapper dynamoDBMapper;

    @Autowired
    private WaterReadingPerWeekService waterReadingPerWeekService;

    @Autowired
    private WaterReadingPerMonthRepository waterReadingPerMonthRepository;

    private static final Logger logger = Logger.getLogger(WaterReadingPerMonthService.class.getName());

    public void save(WaterReadingPerMonth waterReadingPerMonth) {
        waterReadingPerMonthRepository.save(waterReadingPerMonth);
    }

    public List<WaterReadingPerMonth> findAll() {
        return waterReadingPerMonthRepository.findAll();
    }

    public List<WaterReadingPerMonth> getWaterReadingsByDeviceId(String deviceId) {
        return waterReadingPerMonthRepository.findWaterReadingByDeviceId(deviceId);
    }

    private List<String> getDeviceIds() {
        // Fetch the list of device IDs from your database or configuration
        return List.of("device1", "device2"); // Example
    }

    @Scheduled(cron = "0 0 0 1 * ?", zone = "Asia/Kolkata") // Every month on the first day at midnight in IST
    public void storeDailyReadings() {
        try {
            List<WaterReadingPerWeek> latestReadings = waterReadingPerWeekService.getLatestReadingsForAllDevices();
            for (WaterReadingPerWeek latestReading : latestReadings) {
                WaterReadingPerMonth waterReadingPerMonth = new WaterReadingPerMonth();
                waterReadingPerMonth.setDeviceId(latestReading.getDeviceId());
                waterReadingPerMonth.setTimestamp(latestReading.getTimestamp());
                waterReadingPerMonth.setFlowReading(latestReading.getFlowReading());
                waterReadingPerMonth.setFetchTimestamp(String.valueOf(Instant.now().getEpochSecond()));
                waterReadingPerMonthRepository.save(waterReadingPerMonth);
            }
        } catch (Exception err){
            System.out.println("Error during the aggregation task : " + err);
        }
        System.out.println(Calendar.getInstance(TimeZone.getTimeZone("Asia/Kolkata")).getTime() + " MONTH!");
    }

    public List<WaterReadingPerMonth> getLatestReadingsForAllDevices() {
        List<String> deviceIds = getAllDeviceIds(); // Implement this method to get all device IDs
        return deviceIds.stream()
                .map(this::getLatestReading)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toList());
    }

    public Optional<WaterReadingPerMonth> getLatestReading(String deviceId) {
        List<WaterReadingPerMonth> readings = waterReadingPerMonthRepository.findWaterReadingByDeviceId(deviceId);
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
        List<WaterReadingPerMonth> readings = waterReadingPerMonthRepository.findAll();
        return readings.stream()
                .map(WaterReadingPerMonth::getDeviceId)
                .distinct()
                .collect(Collectors.toList());
    }

    public List<WaterReadingPerMonth> getReadings2(String deviceId, String startTimestamp, String endTimestamp) {
        // Parse the start and end timestamps
        long startTimeInSeconds = Long.parseLong(startTimestamp);
        long endTimeInSeconds = Long.parseLong(endTimestamp);

        Map<String, AttributeValue> eav = new HashMap<>();
        eav.put(":deviceId", new AttributeValue().withS(deviceId));
        eav.put(":startTimestamp", new AttributeValue().withS(Long.toString(startTimeInSeconds)));
        eav.put(":endTimestamp", new AttributeValue().withS(Long.toString(endTimeInSeconds)));

        DynamoDBQueryExpression<WaterReadingPerMonth> queryExpression = new DynamoDBQueryExpression<WaterReadingPerMonth>()
                .withKeyConditionExpression("deviceId = :deviceId and #ts between :startTimestamp and :endTimestamp")
                .withExpressionAttributeNames(Collections.singletonMap("#ts", "fetchTimestamp")) // Using the 'timestamp' attribute
                .withExpressionAttributeValues(eav);

        List<WaterReadingPerMonth> result = dynamoDBMapper.query(WaterReadingPerMonth.class, queryExpression);
        return result != null ? result : Collections.emptyList(); // Return an empty list if the result is null
    }

    public DeviceReportResponse getReadings(String deviceId, String startTimestamp, String endTimestamp) {
        // Convert timestamps to long
        long startTimeInSeconds = Long.parseLong(startTimestamp);
        long endTimeInSeconds = Long.parseLong(endTimestamp);

        // Adjust the timestamps (similar logic can be applied here if needed)
        long oneMonthInSeconds = 2592000; // 2592000 seconds in a month (approximation)
        long adjustedStartTimeInSeconds = startTimeInSeconds - oneMonthInSeconds;
        long adjustedEndTimeInSeconds = endTimeInSeconds + oneMonthInSeconds;

        // Directly querying between the adjusted start and end timestamps
        Map<String, AttributeValue> eav = new HashMap<>();
        eav.put(":deviceId", new AttributeValue().withS(deviceId));
        eav.put(":startTimestamp", new AttributeValue().withS(Long.toString(adjustedStartTimeInSeconds)));
        eav.put(":endTimestamp", new AttributeValue().withS(Long.toString(adjustedEndTimeInSeconds)));

        DynamoDBQueryExpression<WaterReadingPerMonth> queryExpression = new DynamoDBQueryExpression<WaterReadingPerMonth>()
                .withKeyConditionExpression("deviceId = :deviceId and fetchTimestamp between :startTimestamp and :endTimestamp")
                .withExpressionAttributeValues(eav);

        List<WaterReadingPerMonth> result = dynamoDBMapper.query(WaterReadingPerMonth.class, queryExpression);


        DeviceReportResponse report = new DeviceReportResponse();

        // Compute flow readings as differences and omit the first reading
        List<WaterReadingPerMonth> modifiedReadings = new ArrayList<>();
        double peakUsage = 0;
        double totalUsage = 0;

        for (int i = 1; i < result.size(); i++) {
            WaterReadingPerMonth prevReading = result.get(i - 1);
            WaterReadingPerMonth currentReading = result.get(i);

            double prevFlowReading = Double.parseDouble(prevReading.getFlowReading());
            double currentFlowReading = Double.parseDouble(currentReading.getFlowReading());
            double flowDifference = currentFlowReading - prevFlowReading;

            // Ensure flowDifference is not less than 0
            flowDifference = Math.max(flowDifference, 0);
            totalUsage = totalUsage + flowDifference;


            WaterReadingPerMonth newMonthReading = new WaterReadingPerMonth();

            newMonthReading.setDeviceId(currentReading.getDeviceId());
            newMonthReading.setFlowReading(Double.toString(flowDifference));
            newMonthReading.setFetchTimestamp(currentReading.getFetchTimestamp());
            newMonthReading.setTimestamp(currentReading.getTimestamp());
            newMonthReading.setReadingId(currentReading.getReadingId());

            // Track peak usage
            if (flowDifference > peakUsage) {
                peakUsage = flowDifference;
            }

            // Add modified reading to the list
            modifiedReadings.add(newMonthReading);
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


    private WaterReadingPerMonth getNearestMonthlyReading(String deviceId, String timestamp, boolean isStart) {
        long timestampInSeconds = Long.parseLong(timestamp);
        long startRange = isStart ? roundToNearestMonth(timestampInSeconds) : timestampInSeconds - 2592000; // 2592000 seconds in 30 days (approximate month)
        long endRange = isStart ? startRange + 2592000 : timestampInSeconds;

        Map<String, AttributeValue> eav = new HashMap<>();
        eav.put(":deviceId", new AttributeValue().withS(deviceId));
        eav.put(":startRange", new AttributeValue().withS(Long.toString(startRange)));
        eav.put(":endRange", new AttributeValue().withS(Long.toString(endRange)));

        DynamoDBQueryExpression<WaterReadingPerMonth> queryExpression = new DynamoDBQueryExpression<WaterReadingPerMonth>()
                .withKeyConditionExpression("deviceId = :deviceId and fetchTimestamp between :startRange and :endRange")
                .withExpressionAttributeValues(eav)
                .withScanIndexForward(!isStart); // Scan forward for start and backward for end

        List<WaterReadingPerMonth> readings = dynamoDBMapper.query(WaterReadingPerMonth.class, queryExpression);
        if (readings.isEmpty()) {
            return null;
        }

        // Find the reading closest to the given timestamp
        WaterReadingPerMonth nearestReading = readings.get(0);
        long nearestDiff = Math.abs(Long.parseLong(nearestReading.getFetchTimestamp()) - timestampInSeconds);
        for (WaterReadingPerMonth reading : readings) {
            long diff = Math.abs(Long.parseLong(reading.getFetchTimestamp()) - timestampInSeconds);
            if (diff < nearestDiff) {
                nearestReading = reading;
                nearestDiff = diff;
            }
        }

        return nearestReading;
    }

    private long roundToNearestMonth(long timestampInSeconds) {
        // Assuming a month has 30 days for simplicity
        return (timestampInSeconds / 2592000) * 2592000;
    }

}
