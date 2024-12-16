package sustainico_backend.rep;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import sustainico_backend.Models.Subscriber;


@Repository
public class SubscriberRepository {

    private final DynamoDBMapper dynamoDBMapper;

    @Autowired
    public SubscriberRepository(DynamoDBMapper dynamoDBMapper) {
        this.dynamoDBMapper = dynamoDBMapper;
    }

    public Subscriber save(Subscriber subscriber) {
        dynamoDBMapper.save(subscriber);
        return subscriber;
    }

    public Subscriber findBySubscriberId(String subscriberId) {
        return dynamoDBMapper.load(Subscriber.class, subscriberId);
    }

    public void delete(Subscriber subscriber) {
        dynamoDBMapper.delete(subscriber);
    }

}
