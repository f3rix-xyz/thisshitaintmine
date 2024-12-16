package sustainico_backend.service;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import sustainico_backend.Models.*;
import sustainico_backend.rep.HomeRepository;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.logging.Logger;
import java.util.stream.Collectors;

@Service
public class WaterReadingAggregationService {

    private static final Logger logger = Logger.getLogger(WaterReadingAggregationService.class.getName());

    @Autowired
    private HomeService homeService;

    @Autowired
    private HomeRepository homeRepository;

    @Autowired
    private WaterReadingPerHourService waterReadingPerHourService;

    @Autowired
    private WaterReadingPerDayService waterReadingPerDayService;

    @Autowired
    private WaterReadingPerWeekService waterReadingPerWeekService;

    @Autowired
    private WaterReadingPerMonthService waterReadingPerMonthService;

    public HomeReportResponse fetchFlowReadingsForDevices(String homeId, String startTimestamp, String endTimestamp, String resolution) {
        List<String> deviceIds = getDeviceIdsByHomeId(homeId);
        Map<String, Double> aggregatedReadings = new HashMap<>();

        double totalFlow = 0;
        double peakUsage = 0;
        int readingCount = 0;

        for (String deviceId : deviceIds) {
            Map<String, Double> deviceReadings = fetchDeviceReadings(deviceId, startTimestamp, endTimestamp, resolution);
            aggregatedReadings = mergeAggregatedReadings(aggregatedReadings, deviceReadings);
        }

        // Calculate metrics
        for (Map.Entry<String, Double> entry : aggregatedReadings.entrySet()) {
            double reading = entry.getValue();
            totalFlow += reading;
            readingCount++;
            if (reading > peakUsage) {
                peakUsage = reading;
            }
        }

        double averageUsage = totalFlow / readingCount;
        double continuesFlowPercentage = 0.0;
        double estimatedLeakage = 0.0; // Define this method based on your logic


        return new HomeReportResponse(aggregatedReadings, averageUsage, peakUsage, continuesFlowPercentage, estimatedLeakage);
    }

    private Map<String, Double> fetchDeviceReadings(String deviceId, String startTimestamp, String endTimestamp, String resolution) {
        switch (resolution.toLowerCase()) {
            case "day":
                List<WaterReadingPerHour> hourReadings = waterReadingPerHourService.getReadings2(deviceId, startTimestamp, endTimestamp);
                return aggregateHourReadings(hourReadings);
            case "week":
                List<WaterReadingPerDay> dayReadings = waterReadingPerDayService.getReadings2(deviceId, startTimestamp, endTimestamp);
                return aggregateDayReadings(dayReadings);
            case "month":
                List<WaterReadingPerWeek> weekReadings = waterReadingPerWeekService.getReadings2(deviceId, startTimestamp, endTimestamp);
                return aggregateWeekReadings(weekReadings);
            case "year":
                List<WaterReadingPerMonth> monthReadings = waterReadingPerMonthService.getReadings2(deviceId, startTimestamp, endTimestamp);
                return aggregateMonthReadings(monthReadings);
            default:
                throw new IllegalArgumentException("Invalid resolution specified.");
        }
    }

    private List<String> getDeviceIdsByHomeId(String homeId) {
        return homeService.getDevicesByHomeId(homeId)
                .stream()
                .map(DeviceArray::getDeviceId)
                .collect(Collectors.toList());
    }

    private Map<String, Double> aggregateHourReadings(List<WaterReadingPerHour> readings) {
        Map<String, Double> aggregatedReadings = new HashMap<>();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:00");

        for (WaterReadingPerHour reading : readings) {
            String formattedTime = formatTimestamp(reading.getFetchTimestamp(), formatter);
            aggregatedReadings.put(formattedTime, Double.parseDouble(reading.getFlowReading()));
        }
        return aggregatedReadings;
    }

    private Map<String, Double> aggregateDayReadings(List<WaterReadingPerDay> readings) {
        Map<String, Double> aggregatedReadings = new HashMap<>();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

        for (WaterReadingPerDay reading : readings) {
            String formattedTime = formatTimestamp(reading.getFetchTimestamp(), formatter);
            aggregatedReadings.put(formattedTime, Double.parseDouble(reading.getFlowReading()));
        }
        return aggregatedReadings;
    }

    private Map<String, Double> aggregateWeekReadings(List<WaterReadingPerWeek> readings) {
        Map<String, Double> aggregatedReadings = new HashMap<>();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-'W'ww");

        for (WaterReadingPerWeek reading : readings) {
            String formattedTime = formatTimestamp(reading.getFetchTimestamp(), formatter);
            aggregatedReadings.put(formattedTime, Double.parseDouble(reading.getFlowReading()));
        }
        return aggregatedReadings;
    }

    private Map<String, Double> aggregateMonthReadings(List<WaterReadingPerMonth> readings) {
        Map<String, Double> aggregatedReadings = new HashMap<>();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM");

        for (WaterReadingPerMonth reading : readings) {
            String formattedTime = formatTimestamp(reading.getFetchTimestamp(), formatter);
            aggregatedReadings.put(formattedTime, Double.parseDouble(reading.getFlowReading()));
        }
        return aggregatedReadings;
    }

    private String formatTimestamp(String timestamp, DateTimeFormatter formatter) {
        Instant instant = Instant.ofEpochSecond(Long.parseLong(timestamp));
        ZonedDateTime zdt = instant.atZone(ZoneId.systemDefault());
        return zdt.format(formatter);
    }

    private Map<String, Double> mergeAggregatedReadings(Map<String, Double> main, Map<String, Double> toAdd) {
        for (Map.Entry<String, Double> entry : toAdd.entrySet()) {
            main.merge(entry.getKey(), entry.getValue(), Double::sum);
        }
        return main;
    }

}
