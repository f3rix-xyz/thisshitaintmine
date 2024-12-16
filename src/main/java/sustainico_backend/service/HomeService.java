package sustainico_backend.service;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import sustainico_backend.Models.DeviceArray;
import sustainico_backend.Models.Home;
import sustainico_backend.Models.User;
import sustainico_backend.rep.HomeRepository;
import sustainico_backend.rep.UserRepository;
import sustainico_backend.rep.WaterReadingRepository;
import sustainico_backend.util.ApiResponse;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class HomeService {

    @Autowired
    DynamoDBMapper dynamoDBMapper;

    @Autowired
    private WaterReadingRepository waterReadingRepository;

    @Autowired
    private DeviceService deviceService;

    private final HomeRepository homeRepository;
    private final UserRepository userRepository;

    @Autowired
    public HomeService(WaterReadingRepository waterReadingRepository, HomeRepository homeRepository, UserRepository userRepository) {
        this.waterReadingRepository = waterReadingRepository;
        this.homeRepository = homeRepository;
        this.userRepository = userRepository;
    }

    public Home saveHome(Home home) {
        return homeRepository.save(home);
    }

    public Home getHomeById(String homeId) {
        return homeRepository.findByHomeId(homeId);
    }

    public Home homeExists(String homeId){
        return getHomeById(homeId);
    }

    public Home createHome(Home home) throws Exception {
        homeRepository.save(home);
        return home; // Return the saved home entity with generated homeId
    }

    public Optional<Home> getHomeByHomeId(String homeId) {
        Home home = dynamoDBMapper.load(Home.class, homeId);
        return Optional.ofNullable(home);
    }

    public List<DeviceArray> getDevicesByHomeId(String homeId) {
        Optional<Home> home = getHomeByHomeId(homeId);
        return home.map(Home::getDevices).orElse(List.of());
    }


    public Optional<Home> updateHome(Home home) {
        if (homeExists(home.getHomeId())!=null) {
            Home updateHome = homeExists(home.getHomeId());
            updateHome.setHomeName(home.getHomeName());
            updateHome.setChildren(home.getChildren());
            updateHome.setAdults(home.getAdults());
            updateHome.setAddressLine1(home.getAddressLine1());
            updateHome.setAddressLine2(home.getAddressLine2());
            updateHome.setPincode(home.getPincode());
            updateHome.setCity(home.getCity());
            updateHome.setState(home.getState());
            updateHome.setArea(home.getArea());
            updateHome.setUserId(home.getUserId());
            updateHome.setDevices(home.getDevices());
            updateHome.setBaselineUsage(home.getBaselineUsage());
            return Optional.of(homeRepository.save(updateHome));
        } else {
            return Optional.empty();
        }
    }

//    public void deleteHome(Home home) {
//        homeRepository.delete(home);
//    }


    public void deleteHome(Home home) {
        // Delete all devices associated with this home
        if (home.getDevices() != null) {
            for (DeviceArray deviceArray : home.getDevices()) {
                String deviceId = deviceArray.getDeviceId();
                String userId = home.getUserId();
                deviceService.deleteDevice2(deviceId, userId);
            }
        }

        // Delete the home itself
        homeRepository.delete(home);
    }

    public List<Home> fetchAllHomesByUserId(String userId) {
        System.out.println("fetching by userId from Service");
        return homeRepository.findHomeByUserId(userId);
    }

    public void moveDevice(String deviceId, String currentHomeId, String newHomeId, String deviceName) {
        // Find the current home
        Optional<Home> currentHomeOptional = Optional.ofNullable(homeRepository.findByHomeId(currentHomeId));
        if (!currentHomeOptional.isPresent()) {
            throw new IllegalArgumentException("Current home not found");
        }
        Home currentHome = currentHomeOptional.get();

        // Find the new home
        Optional<Home> newHomeOptional = Optional.ofNullable(homeRepository.findByHomeId(newHomeId));
        if (!newHomeOptional.isPresent()) {
            throw new IllegalArgumentException("New home not found");
        }
        Home newHome = newHomeOptional.get();

        // Find the device in the current home's device list and remove it
        boolean deviceFound = false;
        if (currentHome.getDevices() != null) {
            deviceFound = currentHome.getDevices().removeIf(deviceArray -> deviceArray.getDeviceId().equals(deviceId));
        }

        if (!deviceFound) {
            throw new IllegalArgumentException("Device not found in the current home");
        }

        // Add the device to the new home's device list
        if (newHome.getDevices() == null) {
            newHome.setDevices(new ArrayList<>());
        }
        newHome.getDevices().add(new DeviceArray(deviceId, deviceName));

        // Save both homes
        homeRepository.save(currentHome);
        homeRepository.save(newHome);
    }

}
