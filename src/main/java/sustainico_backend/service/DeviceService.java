package sustainico_backend.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import sustainico_backend.Models.Device;
import sustainico_backend.Models.DeviceArray;
import sustainico_backend.Models.Home;
import sustainico_backend.Models.DeviceArray;
import sustainico_backend.Models.User;
import sustainico_backend.rep.DeviceRepository;
import sustainico_backend.rep.HomeRepository;
import sustainico_backend.rep.UserRepository;
import sustainico_backend.util.ApiResponse;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.stream.Collectors;

@Service
public class DeviceService {

    @Autowired
    private final DeviceRepository deviceRepository;

    @Autowired
    private HomeRepository homeRepository;


    @Autowired
    public DeviceService(DeviceRepository deviceRepository) {
        this.deviceRepository = deviceRepository;
    }
    public Optional<Device> getDeviceById(String deviceId){
        return deviceRepository.findByDeviceId(deviceId);
    }

    public boolean deviceExists(String deviceId){
        return getDeviceById(deviceId).isPresent();
    }

    public Device createDevice(Device device){
            if(deviceExists(device.getDeviceId())){
                return device;
            }
            device.setStatus("NEW");
            return  deviceRepository.save(device);
    }

    public Optional<Device> updateDevice(Device device){
        Optional<Device> existingDevice = deviceRepository.findByDeviceId(device.getDeviceId());
        if(existingDevice.isPresent()){
            Device updatedDevice = existingDevice.get();
            updatedDevice.setDeviceName(device.getDeviceName());
            updatedDevice.setStatus(device.getStatus());
            updatedDevice.setBillAccNo(device.getBillAccNo());
            updatedDevice.setDigitalPin(device.getDigitalPin());
            return Optional.of(deviceRepository.save(updatedDevice));

        }else{
            return Optional.empty();
        }
    }

    public Device createPinForDevice(String deviceId) {
        Optional<Device> optionalDevice = deviceRepository.findByDeviceId(deviceId);
        if (optionalDevice.isPresent()) {
            Device device = optionalDevice.get();
            long currentTimestamp = System.currentTimeMillis();
            long expiryTimestamp = currentTimestamp + 24 * 60 * 60 * 1000; // 24 hours from now

            if (device.getDigitalPin() != null && device.getPinExpiryTimestamp() > currentTimestamp) {
                // If there's an existing pin and it is still valid, update only the expiry timestamp
                device.setPinExpiryTimestamp(expiryTimestamp);
            } else {
                // If the pin has expired or doesn't exist, generate a new pin
                String pin = generatePin();
                device.setDigitalPin(pin);
                device.setPinExpiryTimestamp(expiryTimestamp);
            }

            return deviceRepository.save(device);
        }
        throw new IllegalArgumentException("Device not found");
    }

    private String generatePin() {
        Random random = new Random();
        int pin = 1000 + random.nextInt(9000);
        return String.valueOf(pin);
    }

    public Device addDevice(String userId, String homeId, String deviceName, String deviceId, String pin) {
        Optional<Device> optionalDevice = deviceRepository.findByDeviceId(deviceId);
        if (optionalDevice.isPresent()) {
            Device device = optionalDevice.get();

            if (device.getOwnerId() == null) {
                if (device.getInitialPin().equals(pin)) {
                    device.setOwnerId(userId);
                    device.setDeviceName(deviceName);
                    device.setStatus("OLD");
                    updateHomeWithDevice(homeId, deviceId, deviceName);

                    // Add homeId to homeIds list
                    List<String> homeIds = device.getHomeIds();
                    if (homeIds == null) {
                        homeIds = new ArrayList<>();
                    }
                    if (!homeIds.contains(homeId)) {
                        homeIds.add(homeId);
                        device.setHomeIds(homeIds);
                    }

                    return deviceRepository.save(device);
                } else {
                    throw new IllegalArgumentException("Invalid initial pin");
                }
            } else if (device.getOwnerId() != null && device.getDigitalPin() != null && System.currentTimeMillis() <= device.getPinExpiryTimestamp()) {
                if (device.getDigitalPin().equals(pin)) {
                    List<String> viewers = device.getViewerId();
                    if (viewers == null) {
                        viewers = new ArrayList<>();
                    }
                    if (!viewers.contains(userId)) {
                        viewers.add(userId);
                        device.setViewerId(viewers);
                    }

                    // Add homeId to homeIds list
                    List<String> homeIds = device.getHomeIds();
                    if (homeIds == null) {
                        homeIds = new ArrayList<>();
                    }
                    if (!homeIds.contains(homeId)) {
                        homeIds.add(homeId);
                        device.setHomeIds(homeIds);
                    }

                    updateHomeWithDevice(homeId, deviceId, deviceName);
                    return deviceRepository.save(device);
                } else {
                    throw new IllegalArgumentException("Invalid digital pin");
                }
            } else {
                throw new IllegalArgumentException("Digital pin is expired or not set");
            }
        } else {
            throw new IllegalArgumentException("Device not found");
        }
    }

    private void updateHomeWithDevice(String homeId, String deviceId, String deviceName) {
        Optional<Home> optionalHome = Optional.ofNullable(homeRepository.findByHomeId(homeId));
        if (optionalHome.isPresent()) {
            Home home = optionalHome.get();
            List<DeviceArray> devices = home.getDevices();
            if (devices == null) {
                devices = new ArrayList<>();
            }
            devices.add(new DeviceArray(deviceId,deviceName));
            home.setDevices(devices);
            homeRepository.save(home);
        } else {
            throw new IllegalArgumentException("Home not found");
        }
    }

    public Device updateDeviceName(String deviceId, String newName) {
        Optional<Device> deviceOptional = getDeviceById(deviceId);
        if (deviceOptional.isPresent()) {
            Device device = deviceOptional.get();
            device.setDeviceName(newName);

            // Update device name in the device table
            Device updatedDevice = deviceRepository.save(device);

            // If the user is the owner, update the device name in all homes in homeIds
            if (device.getHomeIds() != null) {
                for (String id : device.getHomeIds()) {
                    updateDeviceNameInHome(id, deviceId, newName);
                }
            }
            return updatedDevice;
        } else {
            throw new IllegalArgumentException("Device not found");
        }
    }

    // Helper method to update the device name in a specific home by homeId
    private void updateDeviceNameInHome(String homeId, String deviceId, String newName) {
        Home home = homeRepository.findByHomeId(homeId);
        if (home != null) {
            List<DeviceArray> deviceArray = home.getDevices();

            if (deviceArray != null) {
                for (DeviceArray device : deviceArray) {
                    if (device.getDeviceId().equals(deviceId)) {
                        device.setDeviceName(newName);
                        break;
                    }
                }

                // Save the updated home back to the repository
                home.setDevices(deviceArray);
                homeRepository.save(home);
            }
        } else {
            throw new IllegalArgumentException("Home not found");
        }
    }

    public Device deleteDevice(String deviceId, String userId, String homeId) {
        Optional<Device> deviceOptional = getDeviceById(deviceId);
        if (deviceOptional.isPresent()) {
            Device device = deviceOptional.get();
            if (device.getOwnerId().equals(userId)) {
                // Remove device from deviceArray of all homes in homeIds
                if (device.getHomeIds() != null) {
                    for (String id : device.getHomeIds()) {
                        removeDeviceFromHome(id, deviceId);
                    }
                }

                // Reset device attributes
                device.setDeviceName(null);
                device.setStatus("NEW");
                device.setBillAccNo(null);
                device.setOwnerId(null);
                device.setViewerId(null);
                device.setInitialPin(null);
                device.setDigitalPin(null);
                device.setPinExpiryTimestamp(null);
                device.setHomeIds(null);  // Clear homeIds after deletion
            } else {
                // Remove userId from viewerId if it exists
                if (device.getViewerId() != null && device.getViewerId().contains(userId)) {
                    device.getViewerId().remove(userId);

                    // Remove device from the specific homeId provided
                    if (homeId != null) {
                        removeDeviceFromHome(homeId, deviceId);
                    }
                }
            }
            return deviceRepository.save(device);
        } else {
            throw new IllegalArgumentException("Device not found");
        }
    }

    // Helper method to remove the device from the home by homeId
    private void removeDeviceFromHome(String homeId, String deviceId) {
        // Fetch the home object by homeId
        Home home = homeRepository.findByHomeId(homeId);
        if (home != null) {
            List<DeviceArray> deviceArray = home.getDevices();

            // If deviceArray is not null, iterate and remove the device
            if (deviceArray != null) {
                deviceArray.removeIf(device -> device.getDeviceId().equals(deviceId));

                // Set the updated device list back to the home object
                home.setDevices(deviceArray);
                homeRepository.save(home);
            }
        } else {
            throw new IllegalArgumentException("Home not found");
        }
    }


    public Device deleteDevice2(String deviceId, String userId) {
        Optional<Device> deviceOptional = getDeviceById(deviceId);
        if (deviceOptional.isPresent()) {
            Device device = deviceOptional.get();
            if (device.getOwnerId().equals(userId)) {
                device.setDeviceName(null);
                device.setStatus("NEW");
                device.setBillAccNo(null);
                device.setOwnerId(null);
                device.setViewerId(null);
                device.setInitialPin(null);
                device.setDigitalPin(null);
                device.setPinExpiryTimestamp(null);
            } else {
                // Remove userId from viewerId if it exists
                if (device.getViewerId() != null && device.getViewerId().contains(userId)) {
                    device.getViewerId().remove(userId);
                }
            }
            return deviceRepository.save(device);
        } else {
            throw new IllegalArgumentException("Device not found");
        }
    }

//    public void deleteDevice(Device device) {
//        deviceRepository.delete(device);
//    }
}
