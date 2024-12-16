package sustainico_backend.rep;

import com.amazonaws.services.dynamodbv2.AbstractAmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBScanExpression;
import com.amazonaws.services.dynamodbv2.datamodeling.KeyPair;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.BatchGetItemRequest;
import com.amazonaws.services.dynamodbv2.model.BatchGetItemResult;
import com.amazonaws.services.dynamodbv2.model.KeysAndAttributes;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import sustainico_backend.Models.Home;
import sustainico_backend.Models.UserHomes;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Repository
public class HomeRepository {

    private final DynamoDBMapper dynamoDBMapper;

    @Autowired
    public HomeRepository(DynamoDBMapper dynamoDBMapper) {
        this.dynamoDBMapper = dynamoDBMapper;
    }

    @Autowired
    public WaterReadingRepository waterReadingRepository;


//    public Home save(Home home) {
//        home.calculateBaselineUsage();
//        dynamoDBMapper.save(home);
//        return home;
//    }

    public Home save(Home home) {
        home.calculateBaselineUsage();
        dynamoDBMapper.save(home);

        String userId = home.getUserId(); // Assuming Home object has getUserId method
        String homeId = home.getHomeId(); // Assuming Home object has getHomeId method

        // Retrieve the existing UserHomes object
        UserHomes userHomes = dynamoDBMapper.load(UserHomes.class, userId);

        // If the UserHomes object doesn't exist, create a new one
        if (userHomes == null) {
            userHomes = new UserHomes();
            userHomes.setUserId(userId);
            userHomes.setHomes(new ArrayList<>());
        }

        // Add the homeId to the homes list
        List<String> homes = userHomes.getHomes();
        if (homes == null) {
            homes = new ArrayList<>();
        }
        homes.add(homeId);

        userHomes.setHomes(homes);

        // Save the updated UserHomes object
        dynamoDBMapper.save(userHomes);

        return home;
    }

    public Home findByHomeId(String homeId) {
        return dynamoDBMapper.load(Home.class, homeId);
    }

    public void delete(Home home) {
        // Fetch the UserHomes object for the given userId
        UserHomes userHomes = dynamoDBMapper.load(UserHomes.class, home.getUserId());

        if (userHomes != null && userHomes.getHomes() != null) {
            // Remove the homeId from the homes array
            List<String> homes = userHomes.getHomes();
            homes.remove(home.getHomeId());

            // Save the updated UserHomes object
            userHomes.setHomes(homes);
            dynamoDBMapper.save(userHomes);
        }

        // Delete the home from the Home table
        dynamoDBMapper.delete(home);
    }

//    public List<Home> findHomeByUserId(String userId) {
//        return dynamoDBMapper.scan(Home.class, new DynamoDBScanExpression())
//                .stream()
//                .filter(home -> userId.equals(home.getUserId()))
//                .collect(Collectors.toList());
//    }

    public List<Home> findHomeByUserId(String userId) {
// Fetch the UserHomes object for the given userId

//         Retrieve homes by userId
        List<Home> homes = dynamoDBMapper.scan(Home.class, new DynamoDBScanExpression())
                .stream()
                .filter(home -> userId.equals(home.getUserId()))
                .collect(Collectors.toList());

//        System.out.println("homes find in func" + LocalDateTime.now());

        // Update today's usage and current month's usage for each home
        homes.forEach(home -> {
            String todaysUsage = waterReadingRepository.updateTodaysUsage(home);
            home.setTodaysUsage(todaysUsage);  // Assuming there's a setTodaysUsage method in Home class
//            System.out.println("updateTodaysUsage set"+ LocalDateTime.now());

            String currentMonthUsage = waterReadingRepository.updateCurrentMonthUsage(home);
            home.setCurrentMonthUsage(currentMonthUsage);  // Assuming there's a setCurrentMonthUsage method in Home class
//            System.out.println("updateCurrentMonthUsage set"+ LocalDateTime.now());

            String LastMonthUsage = waterReadingRepository.updateLastMonthUsage(home);
            home.setLastMonthUsage(LastMonthUsage);  // Assuming there's a setLastMonthUsage method in Home class
//            System.out.println("updateLastMonthUsage set"+ LocalDateTime.now());
        });

        // Return the updated homes
        return homes;
    }
}
