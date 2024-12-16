package sustainico_backend.service;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import sustainico_backend.Models.LatestWaterReading;
import sustainico_backend.Models.WaterReading;
import sustainico_backend.rep.DeviceRepository;
import sustainico_backend.rep.LatestWaterReadingRepository;
import sustainico_backend.rep.WaterReadingRepository;

import java.text.DecimalFormat;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.stream.Collectors;

@Service
public class WaterReadingService {


    private final WaterReadingRepository waterReadingRepository;

    @Autowired
    public LatestWaterReadingRepository latestWaterReadingRepository;


    @Autowired
    public WaterReadingService(WaterReadingRepository waterReadingRepository) {
        this.waterReadingRepository = waterReadingRepository;
    }


    public WaterReading saveWaterReading(WaterReading waterReading){
        waterReading.generateReadingId();
        WaterReading savedReading = waterReadingRepository.save(waterReading);

        // Update LatestWaterReading
        LatestWaterReading latestWaterReading = latestWaterReadingRepository.findByDeviceId(waterReading.getDeviceId());
        if (latestWaterReading == null) {
            latestWaterReading = new LatestWaterReading();
            latestWaterReading.setDeviceId(waterReading.getDeviceId());
            latestWaterReading.setLatestFlowReading(waterReading.getFlowReading());
            latestWaterReading.setTimestamp(waterReading.getTimestamp());
        }
        latestWaterReading.setLatestFlowReading(waterReading.getFlowReading());
        latestWaterReading.setTimestamp(waterReading.getTimestamp());

        latestWaterReadingRepository.save(latestWaterReading);

        return savedReading;
    }



    public List<WaterReading> getWaterReadingsByDeviceId(String deviceId) {
        return waterReadingRepository.findWaterReadingByDeviceId(deviceId);
    }

    public List<WaterReading> getLatestReadingsForAllDevices() {
        List<String> deviceIds = getAllDeviceIds(); // Implement this method to get all device IDs
        return deviceIds.stream()
                .map(this::getLatestReading)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toList());
    }

    public Optional<WaterReading> getLatestReading(String deviceId) {
        List<WaterReading> readings = waterReadingRepository.findWaterReadingByDeviceId(deviceId);
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
        List<WaterReading> readings = waterReadingRepository.findAll();
        return readings.stream()
                .map(WaterReading::getDeviceId)
                .distinct()
                .collect(Collectors.toList());
    }

//    public WaterReading getLatestReadingById(String deviceId) {
//        return waterReadingRepository.findLatestWaterReadingByDeviceId(deviceId);
//    }

//    public Object updateTodaysUsageByHomeId(String homeId) {
//        return waterReadingRepository.updateTodaysUsage(homeId);
//    }

//    public Object updateMonthUsageByHomeId(String homeId) {
//        return waterReadingRepository.updateCurrentMonthUsage(homeId);
//    }

//    public Object updateLastMonthUsageByHomeId(String homeId) {
//        return waterReadingRepository.updateLastMonthUsage(homeId);
//    }


//    private static final String DEVICE_ID_1 = "AEX4000";
//    private static final String DEVICE_ID_2 = "AEX4001";
//    private static final String DEVICE_ID_3 = "AEX4002";
//    private static final Random RANDOM = new Random();
//    private double flowReading1 = 119400; // Starting flow reading for device AEX4000
//    private double flowReading2 = 25218; // Starting flow reading for device AEX4001
//    private double flowReading3 = 24373; // Starting flow reading for device AEX4002
//
//    @Autowired
//    private DynamoDBMapper dynamoDBMapper;
//
//    @Scheduled(fixedRate = 10000)
//    public void generateDummyReadingForAEX4000() {
//        WaterReading waterReading = new WaterReading();
//        waterReading.setDeviceId(DEVICE_ID_1);
//
//        // Increment flow reading by a random value between 1.5 and 2.0 liters
//        flowReading1 += getRandomDoubleInRange(1.5, 2.0);
//        DecimalFormat df = new DecimalFormat("#.#");
//        waterReading.setFlowReading(df.format(flowReading1));
//
//        // Generate timestamp in epoch seconds
//        long epochSeconds = Instant.now().getEpochSecond();
//        waterReading.setTimestamp(String.valueOf(epochSeconds));
//
//        // Generate reading ID
//        waterReading.generateReadingId();
//
//        // Save to DynamoDB
//        saveWaterReading(waterReading);
//    }
//
//    @Scheduled(fixedRate = 300000)
//    public void generateDummyReadingForAEX4001() {
//        WaterReading waterReading = new WaterReading();
//        waterReading.setDeviceId(DEVICE_ID_2);
//
//        // Increment flow reading by a random value between 33 and 41 liters
//        flowReading2 += getRandomDoubleInRange(33, 41);
//        waterReading.setFlowReading(String.valueOf((int)flowReading2));
//
//        // Generate timestamp in epoch seconds
//        long epochSeconds = Instant.now().getEpochSecond();
//        waterReading.setTimestamp(String.valueOf(epochSeconds));
//
//        // Generate reading ID
//        waterReading.generateReadingId();
//
//        // Save to DynamoDB
//        saveWaterReading(waterReading);
//    }
//
//    @Scheduled(fixedRate = 600000)
//    public void generateDummyReadingForAEX4002() {
//        WaterReading waterReading = new WaterReading();
//        waterReading.setDeviceId(DEVICE_ID_3);
//
//        // Increment flow reading by a random value between 65 and 80 liters
//        flowReading3 += getRandomDoubleInRange(65, 80);
//        waterReading.setFlowReading(String.valueOf((int)flowReading3));
//
//        // Generate timestamp in epoch seconds
//        long epochSeconds = Instant.now().getEpochSecond();
//        waterReading.setTimestamp(String.valueOf(epochSeconds));
//
//        // Generate reading ID
//        waterReading.generateReadingId();
//
//        // Save to DynamoDB
//        saveWaterReading(waterReading);
//    }
//
//    private double getRandomDoubleInRange(double min, double max) {
//        return min + (max - min) * RANDOM.nextDouble();
//    }
}
