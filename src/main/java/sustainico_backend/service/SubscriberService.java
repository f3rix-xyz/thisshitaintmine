package sustainico_backend.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import sustainico_backend.Models.Device;
import sustainico_backend.Models.Home;
import sustainico_backend.Models.Subscriber;
import sustainico_backend.rep.SubscriberRepository;
import sustainico_backend.util.ApiResponse;

import java.util.List;

@Service
public class SubscriberService {

    private final SubscriberRepository subscriberRepository;

    @Autowired
    public SubscriberService(SubscriberRepository subscriberRepository) {
        this.subscriberRepository = subscriberRepository;
    }

    public Subscriber saveSubscriber(Subscriber subscriber) {
        return subscriberRepository.save(subscriber);
    }

    public Subscriber getSubscriberById(String subscriberId) {
        return subscriberRepository.findBySubscriberId(subscriberId);
    }

    public ApiResponse<Subscriber> createSubscriber(Subscriber subscriber) {
            Subscriber savedSubscriber = subscriberRepository.save(subscriber);
            return new ApiResponse<>(200, savedSubscriber, "Subscriber Registered successfully",null,null);
    }

    public void deleteSubscriber(Subscriber subscriber) {
        subscriberRepository.delete(subscriber);
    }

}
