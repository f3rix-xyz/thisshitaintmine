package sustainico_backend.rep;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBScanExpression;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.QueryRequest;
import com.amazonaws.services.dynamodbv2.model.QueryResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import sustainico_backend.Models.User;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;


@Repository
public class UserRepository{

    private final DynamoDBMapper dynamoDBMapper;

    private final AmazonDynamoDB buildAmazonDynamoDB;

    @Autowired
    public UserRepository(DynamoDBMapper dynamoDBMapper, AmazonDynamoDB buildAmazonDynamoDB){
        this.dynamoDBMapper = dynamoDBMapper;
        this.buildAmazonDynamoDB = buildAmazonDynamoDB;
    }

    public User save(User user){
        dynamoDBMapper.save(user);
        return user;
    }

    public User findByUserId(String userId) {
        return dynamoDBMapper.load(User.class, userId);
    }

    public Optional<User> findByContactNo(String contactNo) {
        DynamoDBScanExpression scanExpression = new DynamoDBScanExpression()
                .withFilterExpression("contactNo = :val1")
                .withExpressionAttributeValues(Map.of(":val1", new AttributeValue().withS(contactNo)));

        List<User> result = dynamoDBMapper.scan(User.class, scanExpression);
        if (result != null && !result.isEmpty()) {
            return Optional.of(result.get(0));
        }
        return Optional.empty();
    }


    public void delete(User user) {
        dynamoDBMapper.delete(user);
    }

    public long count(){
        return dynamoDBMapper.count(User.class,new DynamoDBScanExpression());
    }

}

