package sustainico_backend.service;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBQueryExpression;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import sustainico_backend.Models.*;
import sustainico_backend.rep.LatestWaterReadingRepository;
import sustainico_backend.rep.WaterReadingPerDayRepository;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.*;
import java.util.logging.Logger;
import java.util.stream.Collectors;

@Service
public class WaterReadingPerDayService {

    @Autowired
    private DynamoDBMapper dynamoDBMapper;

    @Autowired
    private WaterReadingPerHourService waterReadingPerHourService;

    @Autowired
    private WaterReadingPerDayRepository waterReadingPerDayRepository;

    @Autowired
    public LatestWaterReadingRepository latestWaterReadingRepository;

    private static final Logger logger = Logger.getLogger(WaterReadingPerDayService.class.getName());

    public void saveWaterReading(WaterReadingPerDay waterReadingPerDay) {
        waterReadingPerDayRepository.save(waterReadingPerDay);
    }

    public List<WaterReadingPerDay> findAll() {
        return waterReadingPerDayRepository.findAll();
    }

    public List<WaterReadingPerDay> getWaterReadingsByDeviceId(String deviceId) {
        return waterReadingPerDayRepository.findWaterReadingByDeviceId(deviceId);
    }

    private List<String> getDeviceIds() {
        // Fetch the list of device IDs from your database or configuration
        return List.of("device1", "device2"); // Example
    }

    @Scheduled(cron = "0 0 0 * * ?", zone = "Asia/Kolkata") // Every day at midnight in IST
    public void storeDailyReadings() {
        try {
            List<WaterReadingPerHour> latestReadings = waterReadingPerHourService.getLatestReadingsForAllDevices();
            for (WaterReadingPerHour latestReading : latestReadings) {
                WaterReadingPerDay waterReadingPerDay = new WaterReadingPerDay();
                waterReadingPerDay.setDeviceId(latestReading.getDeviceId());
                waterReadingPerDay.setTimestamp(latestReading.getTimestamp());
                waterReadingPerDay.setFlowReading(latestReading.getFlowReading());
                waterReadingPerDay.setFetchTimestamp(String.valueOf(Instant.now().getEpochSecond()));

                // Update LatestWaterReading
                LatestWaterReading latestWaterReading = latestWaterReadingRepository
                        .findByDeviceId(latestReading.getDeviceId());
                if (latestWaterReading == null) {
                    latestWaterReading = new LatestWaterReading();
                    latestWaterReading.setDeviceId(latestReading.getDeviceId());
                    latestWaterReading.setFirstFlowReading(latestReading.getFlowReading());
                }
                latestWaterReading.setFirstFlowReading(latestReading.getFlowReading());

                latestWaterReadingRepository.save(latestWaterReading);

                waterReadingPerDayRepository.save(waterReadingPerDay);
            }
        } catch (Exception err) {
            System.out.println("Error during the aggregation task : " + err);
        }
        // System.out.println(Calendar.getInstance(TimeZone.getTimeZone("Asia/Kolkata")).getTime()
        // + " DAY!");
    }

    public List<WaterReadingPerDay> getLatestReadingsForAllDevices() {
        List<String> deviceIds = getAllDeviceIds(); // Implement this method to get all device IDs
        return deviceIds.stream()
                .map(this::getLatestReading)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toList());
    }

    public Optional<WaterReadingPerDay> getLatestReading(String deviceId) {
        List<WaterReadingPerDay> readings = waterReadingPerDayRepository.findWaterReadingByDeviceId(deviceId);
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
        List<WaterReadingPerDay> readings = waterReadingPerDayRepository.findAll();
        return readings.stream()
                .map(WaterReadingPerDay::getDeviceId)
                .distinct()
                .collect(Collectors.toList());
    }

    public List<WaterReadingPerDay> getReadings2(String deviceId, String startTimestamp, String endTimestamp) {
        long startTimeInSeconds = roundToNearestDay(Long.parseLong(startTimestamp));
        long endTimeInSeconds = Long.parseLong(endTimestamp);

        WaterReadingPerDay startReading = getNearestDailyReading(deviceId, Long.toString(startTimeInSeconds), true);
        WaterReadingPerDay endReading = getNearestDailyReading(deviceId, endTimestamp, false);

        if (startReading == null || endReading == null) {
            logger.warning("No start or end reading found.");
            return Collections.emptyList(); // Return an empty list instead of null
        }

        Map<String, AttributeValue> eav = new HashMap<>();
        eav.put(":deviceId", new AttributeValue().withS(deviceId));
        eav.put(":startTimestamp", new AttributeValue().withS(startReading.getFetchTimestamp()));
        eav.put(":endTimestamp", new AttributeValue().withS(endReading.getFetchTimestamp()));

        DynamoDBQueryExpression<WaterReadingPerDay> queryExpression = new DynamoDBQueryExpression<WaterReadingPerDay>()
                .withKeyConditionExpression(
                        "deviceId = :deviceId and fetchTimestamp between :startTimestamp and :endTimestamp")
                .withExpressionAttributeValues(eav);

        List<WaterReadingPerDay> result = dynamoDBMapper.query(WaterReadingPerDay.class, queryExpression);

        List<WaterReadingPerDay> modifiedReadings = new ArrayList<>();

        for (int i = 1; i < result.size(); i++) {
            WaterReadingPerDay prevReading = result.get(i - 1);
            WaterReadingPerDay currentReading = result.get(i);

            double prevFlowReading = Double.parseDouble(prevReading.getFlowReading());
            double currentFlowReading = Double.parseDouble(currentReading.getFlowReading());
            double flowDifference = currentFlowReading - prevFlowReading;

            // Update the flowReading and timestamp of the currentReading
            // prevReading.setFlowReading(Double.toString(flowDifference));
            // prevReading.setFetchTimestamp(currentReading.getFetchTimestamp());
            // prevReading.setTimestamp(currentReading.getTimestamp());

            WaterReadingPerDay newDayReading = new WaterReadingPerDay();

            newDayReading.setDeviceId(currentReading.getDeviceId());
            newDayReading.setFlowReading(Double.toString(flowDifference));
            newDayReading.setFetchTimestamp(currentReading.getFetchTimestamp());
            newDayReading.setTimestamp(currentReading.getTimestamp());
            newDayReading.setReadingId(currentReading.getReadingId());

            // Add modified reading to the list
            modifiedReadings.add(newDayReading);
        }

        return modifiedReadings; // Return an empty list if the result is null
    }

    public DeviceReportResponse getReadings(String deviceId, String startTimestamp, String endTimestamp) {
        // Convert timestamps to long
        long startTimeInSeconds = Long.parseLong(startTimestamp);
        long endTimeInSeconds = Long.parseLong(endTimestamp);

        // Adjust the timestamps
        long oneDayInSeconds = 86400; // 86400 seconds in a day
        long adjustedStartTimeInSeconds = startTimeInSeconds - oneDayInSeconds;
        long adjustedEndTimeInSeconds = endTimeInSeconds + oneDayInSeconds;

        // Directly querying between the adjusted start and end timestamps
        Map<String, AttributeValue> eav = new HashMap<>();
        eav.put(":deviceId", new AttributeValue().withS(deviceId));
        eav.put(":startTimestamp", new AttributeValue().withS(Long.toString(adjustedStartTimeInSeconds)));
        eav.put(":endTimestamp", new AttributeValue().withS(Long.toString(adjustedEndTimeInSeconds)));

        DynamoDBQueryExpression<WaterReadingPerDay> queryExpression = new DynamoDBQueryExpression<WaterReadingPerDay>()
                .withKeyConditionExpression(
                        "deviceId = :deviceId and fetchTimestamp between :startTimestamp and :endTimestamp")
                .withExpressionAttributeValues(eav);

        List<WaterReadingPerDay> result = dynamoDBMapper.query(WaterReadingPerDay.class, queryExpression);

        DeviceReportResponse report = new DeviceReportResponse();

        // Compute flow readings as differences and omit the first reading
        List<WaterReadingPerDay> modifiedReadings = new ArrayList<>();
        double peakUsage = 0;
        double totalUsage = 0;

        for (int i = 1; i < result.size(); i++) {
            WaterReadingPerDay prevReading = result.get(i - 1);
            WaterReadingPerDay currentReading = result.get(i);

            double prevFlowReading = Double.parseDouble(prevReading.getFlowReading());
            double currentFlowReading = Double.parseDouble(currentReading.getFlowReading());
            double flowDifference = currentFlowReading - prevFlowReading;

            // Ensure flowDifference is not less than 0
            flowDifference = Math.max(flowDifference, 0);
            totalUsage = totalUsage + flowDifference;

            WaterReadingPerDay newDayReading = new WaterReadingPerDay();

            newDayReading.setDeviceId(currentReading.getDeviceId());
            newDayReading.setFlowReading(Double.toString(flowDifference));
            newDayReading.setFetchTimestamp(currentReading.getFetchTimestamp());
            newDayReading.setTimestamp(currentReading.getTimestamp());
            newDayReading.setReadingId(currentReading.getReadingId());

            // Track peak usage
            if (flowDifference > peakUsage) {
                peakUsage = flowDifference;
            }

            // Add modified reading to the list
            modifiedReadings.add(newDayReading);
        }

        report.setReadings(modifiedReadings);

        if (modifiedReadings.size() > 0) {
            // double totalUsage =
            // Double.parseDouble(result.get(result.size()-1).getFlowReading()) -
            // Double.parseDouble(result.get(0).getFlowReading());
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

    private WaterReadingPerDay getNearestDailyReading(String deviceId, String timestamp, boolean isStart) {
        long timestampInSeconds = Long.parseLong(timestamp);
        long startRange = isStart ? roundToNearestDay(timestampInSeconds) : timestampInSeconds - 86400;
        long endRange = isStart ? startRange + 86400 : timestampInSeconds;

        Map<String, AttributeValue> eav = new HashMap<>();
        eav.put(":deviceId", new AttributeValue().withS(deviceId));
        eav.put(":startRange", new AttributeValue().withS(Long.toString(startRange)));
        eav.put(":endRange", new AttributeValue().withS(Long.toString(endRange)));

        DynamoDBQueryExpression<WaterReadingPerDay> queryExpression = new DynamoDBQueryExpression<WaterReadingPerDay>()
                .withKeyConditionExpression("deviceId = :deviceId and fetchTimestamp between :startRange and :endRange")
                .withExpressionAttributeValues(eav)
                .withScanIndexForward(!isStart); // Scan forward for start and backward for end

        List<WaterReadingPerDay> readings = dynamoDBMapper.query(WaterReadingPerDay.class, queryExpression);
        if (readings.isEmpty()) {
            return null;
        }

        // Find the reading closest to the given timestamp
        WaterReadingPerDay nearestReading = readings.get(0);
        long nearestDiff = Math.abs(Long.parseLong(nearestReading.getFetchTimestamp()) - timestampInSeconds);
        for (WaterReadingPerDay reading : readings) {
            long diff = Math.abs(Long.parseLong(reading.getFetchTimestamp()) - timestampInSeconds);
            if (diff < nearestDiff) {
                nearestReading = reading;
                nearestDiff = diff;
            }
        }

        return nearestReading;
    }

    private long roundToNearestDay(long timestampInSeconds) {
        return (timestampInSeconds / 86400) * 86400; // 86400 seconds in a day
    }

    public List<SimplifiedWaterReading> getDailyReadingsForMonth(String deviceId, String date) {
        try {
            SimpleDateFormat yearMonthFormat = new SimpleDateFormat("yyyy-MM");
            yearMonthFormat.setTimeZone(TimeZone.getTimeZone("Asia/Kolkata"));

            Date parsedDate = yearMonthFormat.parse(date);
            Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("Asia/Kolkata"));
            calendar.setTime(parsedDate);

            // Set time to start of month
            calendar.set(Calendar.DAY_OF_MONTH, 1);
            calendar.set(Calendar.HOUR_OF_DAY, 0);
            calendar.set(Calendar.MINUTE, 0);
            calendar.set(Calendar.SECOND, 0);
            long startTimestamp = calendar.getTimeInMillis() / 1000;

            // Set time to start of next month to get readings for last day
            calendar.add(Calendar.MONTH, 1);
            long endTimestamp = calendar.getTimeInMillis() / 1000;

            Map<String, AttributeValue> eav = new HashMap<>();
            eav.put(":deviceId", new AttributeValue().withS(deviceId));
            eav.put(":startTimestamp", new AttributeValue().withS(String.valueOf(startTimestamp)));
            eav.put(":endTimestamp", new AttributeValue().withS(String.valueOf(endTimestamp)));

            Map<String, String> expressionAttributeNames = new HashMap<>();
            expressionAttributeNames.put("#ts", "timestamp");

            DynamoDBQueryExpression<WaterReadingPerDay> queryExpression = new DynamoDBQueryExpression<WaterReadingPerDay>()
                    .withKeyConditionExpression("deviceId = :deviceId")
                    .withFilterExpression("#ts between :startTimestamp and :endTimestamp")
                    .withExpressionAttributeNames(expressionAttributeNames)
                    .withExpressionAttributeValues(eav);

            List<WaterReadingPerDay> readings = new ArrayList<>(
                    dynamoDBMapper.query(WaterReadingPerDay.class, queryExpression));

            // Sort readings by timestamp
            Collections.sort(readings, (r1, r2) -> Long.compare(
                    Long.parseLong(r1.getTimestamp()),
                    Long.parseLong(r2.getTimestamp())));

            // Remove duplicates
            List<WaterReadingPerDay> uniqueReadings = new ArrayList<>();
            String lastTimestamp = null;
            for (WaterReadingPerDay reading : readings) {
                if (!reading.getTimestamp().equals(lastTimestamp)) {
                    uniqueReadings.add(reading);
                    lastTimestamp = reading.getTimestamp();
                }
            }

            // Format for displaying date
            SimpleDateFormat dateFormat = new SimpleDateFormat("dd MMM yyyy");
            dateFormat.setTimeZone(TimeZone.getTimeZone("Asia/Kolkata"));

            Map<Integer, SimplifiedWaterReading> dailyConsumption = new TreeMap<>();

            // Calculate consumption between consecutive readings
            for (int i = 0; i < uniqueReadings.size() - 1; i++) {
                WaterReadingPerDay currentReading = uniqueReadings.get(i);
                WaterReadingPerDay nextReading = uniqueReadings.get(i + 1);

                long timestamp = Long.parseLong(currentReading.getTimestamp());
                Calendar readingCal = Calendar.getInstance(TimeZone.getTimeZone("Asia/Kolkata"));
                readingCal.setTimeInMillis(timestamp * 1000);
                int day = readingCal.get(Calendar.DAY_OF_MONTH);

                // Calculate consumption
                double currentValue = Double.parseDouble(currentReading.getFlowReading());
                double nextValue = Double.parseDouble(nextReading.getFlowReading());
                double consumption = nextValue - currentValue;

                System.out.println("\nCalculating for day " + day + ":");
                System.out.println("Current day reading: " + currentValue);
                System.out.println("Next day reading: " + nextValue);
                System.out.println("Water flown between day " + day + "-" + (day + 1) + ": " + consumption);

                Calendar displayCal = (Calendar) calendar.clone();
                displayCal.add(Calendar.MONTH, -1); // Go back to original month
                displayCal.set(Calendar.DAY_OF_MONTH, day);
                String dateStr = dateFormat.format(displayCal.getTime());

                dailyConsumption.put(day, new SimplifiedWaterReading(
                        dateStr,
                        String.format("%.2f", Math.max(consumption, 0))));
            }

            // Fill in all days
            List<SimplifiedWaterReading> result = new ArrayList<>();
            calendar.add(Calendar.MONTH, -1); // Go back to original month
            int daysInMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH);
            for (int day = 1; day <= daysInMonth; day++) {
                Calendar displayCal = (Calendar) calendar.clone();
                displayCal.set(Calendar.DAY_OF_MONTH, day);
                String dateStr = dateFormat.format(displayCal.getTime());

                result.add(dailyConsumption.getOrDefault(day,
                        new SimplifiedWaterReading(dateStr, "0.00")));
            }

            return result;

        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
            e.printStackTrace();
            return Collections.emptyList();
        }
    }

}
