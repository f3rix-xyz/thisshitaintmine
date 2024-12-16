package sustainico_backend.service;

import org.springframework.beans.factory.annotation.Autowired;
import sustainico_backend.Models.LatestWaterReading;
import sustainico_backend.rep.LatestWaterReadingRepository;

public class LatestWaterReadingService {
    @Autowired
    private final LatestWaterReadingRepository latestWaterReadingRepository;


    @Autowired
    public LatestWaterReadingService(LatestWaterReadingRepository latestWaterReadingRepository){
        this.latestWaterReadingRepository = latestWaterReadingRepository;
    }


    public void saveLatestWaterReading(LatestWaterReading latestWaterReading){
        latestWaterReadingRepository.save(latestWaterReading);
        return;
    }

    public LatestWaterReading getLatestReadingByDeviceId(String DeviceId){
        return latestWaterReadingRepository.findByDeviceId(DeviceId);
    }


//    public User createLatestWaterReading(LatestWaterReading latestWaterReading) {
//        Optional<User> existingUser = getExistingUser(user.getContactNo());
//        if (existingUser.isPresent()) {
//            User oldUser = existingUser.get();
//            oldUser.setNewUser(false);
//            return oldUser;
//        }
//        user.setNewUser(true);
//        return userRepository.save(user);
//    }
}
