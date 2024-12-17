package sustainico_backend.service;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBQueryExpression;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import sustainico_backend.Models.DeviceReportResponse;
import sustainico_backend.Models.SimplifiedWaterReading;
import sustainico_backend.Models.WaterReading;
import sustainico_backend.Models.WaterReadingPerDay;
import sustainico_backend.Models.WaterReadingPerHour;
import sustainico_backend.rep.WaterReadingPerHourRepository;
import sustainico_backend.rep.WaterReadingRepository;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.*;
import java.util.logging.Logger;
import java.util.stream.Collectors;

@Service
public class WaterReadingPerHourService {

    @Autowired
    private DynamoDBMapper dynamoDBMapper;

    @Autowired
    private WaterReadingService waterReadingService;

    @Autowired
    private WaterReadingPerHourRepository waterReadingPerHourRepository;

    private static final Logger logger = Logger.getLogger(WaterReadingPerHourService.class.getName());

    @Autowired
    public WaterReadingPerHourService(WaterReadingPerHourRepository waterReadingPerHourRepository) {
        this.waterReadingPerHourRepository = waterReadingPerHourRepository;
    }

    @Scheduled(cron = "0 0 * * * ?", zone = "Asia/Kolkata") // Every hour in IST
    public void aggregateWaterReadingPerHour() {
        try {
            List<WaterReading> latestReadings = waterReadingService.getLatestReadingsForAllDevices();
            for (WaterReading latestReading : latestReadings) {
                WaterReadingPerHour waterReadingPerHour = new WaterReadingPerHour();
                waterReadingPerHour.setDeviceId(latestReading.getDeviceId());
                waterReadingPerHour.setTimestamp(latestReading.getTimestamp());
                waterReadingPerHour.setFlowReading(latestReading.getFlowReading());
                waterReadingPerHour.setFetchTimestamp(String.valueOf(Instant.now().getEpochSecond()));
                waterReadingPerHourRepository.save(waterReadingPerHour);
            }
        } catch (Exception err) {
            System.out.println("Error during the aggregation task : " + err);
        }
        System.out.println(Calendar.getInstance(TimeZone.getTimeZone("Asia/Kolkata")).getTime() + " HOUR!");
    }

    public void saveWaterReading(WaterReadingPerHour waterReadingPerHour) {
        waterReadingPerHourRepository.save(waterReadingPerHour);
    }

    public List<WaterReadingPerHour> findAll() {
        return waterReadingPerHourRepository.findAll();
    }

    public List<WaterReadingPerHour> getWaterReadingsByDeviceId(String deviceId) {
        return waterReadingPerHourRepository.findWaterReadingByDeviceId(deviceId);
    }

    public List<WaterReadingPerHour> getLatestReadingsForAllDevices() {
        List<String> deviceIds = getAllDeviceIds(); // Implement this method to get all device IDs
        return deviceIds.stream()
                .map(this::getLatestReading)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toList());
    }

    public Optional<WaterReadingPerHour> getLatestReading(String deviceId) {
        List<WaterReadingPerHour> readings = waterReadingPerHourRepository.findWaterReadingByDeviceId(deviceId);
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
        List<WaterReadingPerHour> readings = waterReadingPerHourRepository.findAll();
        return readings.stream()
                .map(WaterReadingPerHour::getDeviceId)
                .distinct()
                .collect(Collectors.toList());
    }

    public List<WaterReadingPerHour> getReadings2(String deviceId, String startTimestamp, String endTimestamp) {
        long startTimeInSeconds = roundToNearestHour(Long.parseLong(startTimestamp));
        long endTimeInSeconds = Long.parseLong(endTimestamp);

        WaterReadingPerHour startReading = getNearestReading(deviceId, Long.toString(startTimeInSeconds), true);
        WaterReadingPerHour endReading = getNearestReading(deviceId, endTimestamp, false);

        if (startReading == null || endReading == null) {
            logger.warning("No start or end reading found.");
            return Collections.emptyList(); // Return an empty list instead of null
        }

        Map<String, AttributeValue> eav = new HashMap<>();
        eav.put(":deviceId", new AttributeValue().withS(deviceId));
        eav.put(":startTimestamp", new AttributeValue().withS(startReading.getFetchTimestamp()));
        eav.put(":endTimestamp", new AttributeValue().withS(endReading.getFetchTimestamp()));

        DynamoDBQueryExpression<WaterReadingPerHour> queryExpression = new DynamoDBQueryExpression<WaterReadingPerHour>()
                .withKeyConditionExpression(
                        "deviceId = :deviceId and fetchTimestamp between :startTimestamp and :endTimestamp")
                .withExpressionAttributeValues(eav);

        List<WaterReadingPerHour> result = dynamoDBMapper.query(WaterReadingPerHour.class, queryExpression);

        List<WaterReadingPerHour> modifiedReadings = new ArrayList<>();

        for (int i = 1; i < result.size(); i++) {
            WaterReadingPerHour prevReading = result.get(i - 1);
            WaterReadingPerHour currentReading = result.get(i);

            double prevFlowReading = Double.parseDouble(prevReading.getFlowReading());
            double currentFlowReading = Double.parseDouble(currentReading.getFlowReading());
            double flowDifference = currentFlowReading - prevFlowReading;

            // Update the flowReading and timestamp of the currentReading
            // prevReading.setFlowReading(Double.toString(flowDifference));
            // prevReading.setFetchTimestamp(currentReading.getFetchTimestamp());
            // prevReading.setTimestamp(currentReading.getTimestamp());

            WaterReadingPerHour newHourReading = new WaterReadingPerHour();

            newHourReading.setDeviceId(currentReading.getDeviceId());
            newHourReading.setFlowReading(Double.toString(flowDifference));
            newHourReading.setFetchTimestamp(currentReading.getFetchTimestamp());
            newHourReading.setTimestamp(currentReading.getTimestamp());
            newHourReading.setReadingId(currentReading.getReadingId());

            // Add modified reading to the list
            modifiedReadings.add(newHourReading);
        }

        return modifiedReadings; // Return an empty list if the result is null
    }

    public DeviceReportResponse getReadings(String deviceId, String startTimestamp, String endTimestamp) {
        long startTimeInSeconds = roundToNearestHour(Long.parseLong(startTimestamp));
        long endTimeInSeconds = Long.parseLong(endTimestamp);

        WaterReadingPerHour startReading = getNearestReading(deviceId, Long.toString(startTimeInSeconds), true);
        WaterReadingPerHour endReading = getNearestReading(deviceId, endTimestamp, false);

        if (startReading == null || endReading == null) {
            logger.warning("No start or end reading found.");
            return new DeviceReportResponse(); // Return an empty response
        }

        Map<String, AttributeValue> eav = new HashMap<>();
        eav.put(":deviceId", new AttributeValue().withS(deviceId));
        eav.put(":startTimestamp", new AttributeValue().withS(startReading.getFetchTimestamp()));
        eav.put(":endTimestamp", new AttributeValue().withS(endReading.getFetchTimestamp()));

        DynamoDBQueryExpression<WaterReadingPerHour> queryExpression = new DynamoDBQueryExpression<WaterReadingPerHour>()
                .withKeyConditionExpression(
                        "deviceId = :deviceId and fetchTimestamp between :startTimestamp and :endTimestamp")
                .withExpressionAttributeValues(eav);

        List<WaterReadingPerHour> result = dynamoDBMapper.query(WaterReadingPerHour.class, queryExpression);

        DeviceReportResponse report = new DeviceReportResponse();
        report.setReadings(result);

        if (result.size() > 1) {
            double hourlyUsage = (Double.parseDouble(endReading.getFlowReading())
                    - Double.parseDouble(startReading.getFlowReading())) / (result.size() - 1);
            report.setAverageUsage(hourlyUsage);
        } else {
            report.setAverageUsage(0); // Handle the case of insufficient data
        }

        // Calculate peak usage
        double peakUsage = 0;
        for (int i = 1; i < result.size(); i++) {
            double prevFlowReading = Double.parseDouble(result.get(i - 1).getFlowReading());
            double currentFlowReading = Double.parseDouble(result.get(i).getFlowReading());
            double usageDifference = currentFlowReading - prevFlowReading;

            if (usageDifference > peakUsage) {
                peakUsage = usageDifference;
            }
        }
        report.setPeakUsage(peakUsage);

        // Placeholders for other calculations
        report.setContinuesFlowPercentage(0); // Placeholder
        report.setEstimatedLeakage(0); // Placeholder

        return report;
    }

    private WaterReadingPerHour getNearestReading(String deviceId, String timestamp, boolean isStart) {
        long timestampInSeconds = Long.parseLong(timestamp);
        long startRange = isStart ? roundToNearestHour(timestampInSeconds) : timestampInSeconds - 3600;
        long endRange = isStart ? startRange + 3600 : timestampInSeconds;

        Map<String, AttributeValue> eav = new HashMap<>();
        eav.put(":deviceId", new AttributeValue().withS(deviceId));
        eav.put(":startRange", new AttributeValue().withS(Long.toString(startRange)));
        eav.put(":endRange", new AttributeValue().withS(Long.toString(endRange)));

        DynamoDBQueryExpression<WaterReadingPerHour> queryExpression = new DynamoDBQueryExpression<WaterReadingPerHour>()
                .withKeyConditionExpression("deviceId = :deviceId and fetchTimestamp between :startRange and :endRange")
                .withExpressionAttributeValues(eav)
                .withScanIndexForward(!isStart); // Scan forward for start and backward for end

        List<WaterReadingPerHour> readings = dynamoDBMapper.query(WaterReadingPerHour.class, queryExpression);
        if (readings.isEmpty()) {
            return null;
        }

        // Find the reading closest to the given timestamp
        WaterReadingPerHour nearestReading = readings.get(0);
        long nearestDiff = Math.abs(Long.parseLong(nearestReading.getFetchTimestamp()) - timestampInSeconds);
        for (WaterReadingPerHour reading : readings) {
            long diff = Math.abs(Long.parseLong(reading.getFetchTimestamp()) - timestampInSeconds);
            if (diff < nearestDiff) {
                nearestReading = reading;
                nearestDiff = diff;
            }
        }

        return nearestReading;
    }

    private long roundToNearestHour(long timestampInSeconds) {
        return (timestampInSeconds / 3600) * 3600;
    }

    public List<SimplifiedWaterReading> getHourlyReadingsForDate(String deviceId, String date) {
        try {
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
            dateFormat.setTimeZone(TimeZone.getTimeZone("Asia/Kolkata"));

            Date parsedDate = dateFormat.parse(date);
            Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("Asia/Kolkata"));
            calendar.setTime(parsedDate);

            // Set time to start of day (00:00:00)
            calendar.set(Calendar.HOUR_OF_DAY, 0);
            calendar.set(Calendar.MINUTE, 0);
            calendar.set(Calendar.SECOND, 0);
            long startTimestamp = calendar.getTimeInMillis() / 1000;

            // Set time to start of next day (00:00:00) to get readings for last hour
            calendar.add(Calendar.DAY_OF_MONTH, 1);
            long endTimestamp = calendar.getTimeInMillis() / 1000;

            Map<String, AttributeValue> eav = new HashMap<>();
            eav.put(":deviceId", new AttributeValue().withS(deviceId));
            eav.put(":startTimestamp", new AttributeValue().withS(String.valueOf(startTimestamp)));
            eav.put(":endTimestamp", new AttributeValue().withS(String.valueOf(endTimestamp)));

            Map<String, String> expressionAttributeNames = new HashMap<>();
            expressionAttributeNames.put("#ts", "timestamp");

            DynamoDBQueryExpression<WaterReadingPerHour> queryExpression = new DynamoDBQueryExpression<WaterReadingPerHour>()
                    .withKeyConditionExpression("deviceId = :deviceId")
                    .withFilterExpression("#ts between :startTimestamp and :endTimestamp")
                    .withExpressionAttributeNames(expressionAttributeNames)
                    .withExpressionAttributeValues(eav);

            List<WaterReadingPerHour> readings = new ArrayList<>(
                    dynamoDBMapper.query(WaterReadingPerHour.class, queryExpression));

            // Sort readings by timestamp
            Collections.sort(readings, (r1, r2) -> {
                try {
                    return Long.compare(
                            Long.parseLong(r1.getTimestamp()),
                            Long.parseLong(r2.getTimestamp()));
                } catch (NumberFormatException e) {
                    return 0;
                }
            });

            // Remove duplicates
            List<WaterReadingPerHour> uniqueReadings = new ArrayList<>();
            String lastTimestamp = null;
            for (WaterReadingPerHour reading : readings) {
                if (!reading.getTimestamp().equals(lastTimestamp)) {
                    uniqueReadings.add(reading);
                    lastTimestamp = reading.getTimestamp();
                }
            }

            // Format for displaying time
            SimpleDateFormat timeFormat = new SimpleDateFormat("hh:mm a");
            timeFormat.setTimeZone(TimeZone.getTimeZone("Asia/Kolkata"));

            Map<Integer, SimplifiedWaterReading> hourlyConsumption = new TreeMap<>();

            // Calculate consumption between consecutive readings
            for (int i = 0; i < uniqueReadings.size() - 1; i++) {
                WaterReadingPerHour currentReading = uniqueReadings.get(i);
                WaterReadingPerHour nextReading = uniqueReadings.get(i + 1);

                // Get hour of current reading (consumption will be attributed to this hour)
                long timestamp = Long.parseLong(currentReading.getTimestamp());
                Calendar readingCal = Calendar.getInstance(TimeZone.getTimeZone("Asia/Kolkata"));
                readingCal.setTimeInMillis(timestamp * 1000);
                int hour = readingCal.get(Calendar.HOUR_OF_DAY);

                // Calculate consumption
                double currentValue = Double.parseDouble(currentReading.getFlowReading());
                double nextValue = Double.parseDouble(nextReading.getFlowReading());
                double consumption = nextValue - currentValue;

                System.out.println("\nCalculating for hour " + hour + ":");
                System.out.println("Current hour reading: " + currentValue);
                System.out.println("Next hour reading: " + nextValue);
                System.out.println("Water flown between " + hour + "-" + (hour + 1) + ": " + consumption);

                // Format time for display
                Calendar displayCal = (Calendar) calendar.clone();
                displayCal.add(Calendar.DAY_OF_MONTH, -1); // Go back to original date
                displayCal.set(Calendar.HOUR_OF_DAY, hour);
                displayCal.set(Calendar.MINUTE, 0);
                String timeStr = timeFormat.format(displayCal.getTime());

                hourlyConsumption.put(hour, new SimplifiedWaterReading(
                        timeStr,
                        String.format("%.2f", Math.max(consumption, 0))));
            }

            // Fill in all hours
            List<SimplifiedWaterReading> result = new ArrayList<>();
            for (int hour = 0; hour < 24; hour++) {
                Calendar displayCal = (Calendar) calendar.clone();
                displayCal.add(Calendar.DAY_OF_MONTH, -1); // Go back to original date
                displayCal.set(Calendar.HOUR_OF_DAY, hour);
                displayCal.set(Calendar.MINUTE, 0);
                String timeStr = timeFormat.format(displayCal.getTime());

                SimplifiedWaterReading reading = hourlyConsumption.getOrDefault(hour,
                        new SimplifiedWaterReading(timeStr, "0.00"));
                result.add(reading);
            }

            return result;

        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
            e.printStackTrace();
            return Collections.emptyList();
        }
    }

}
