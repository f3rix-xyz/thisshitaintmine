package sustainico_backend.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import sustainico_backend.Models.Subscriber;
import sustainico_backend.service.SubscriberService;
import sustainico_backend.util.ApiResponse;

@RestController
@RequestMapping("/subscriber")
@CrossOrigin(origins = "*")
public class SubscriberController {

    public final SubscriberService subscriberService;

    public SubscriberController(SubscriberService subscriberService) {
        this.subscriberService = subscriberService;
    }

    @PostMapping("/create")
    public ResponseEntity<ApiResponse<Subscriber>> createSubscriber(@RequestBody Subscriber subscriber){
        ApiResponse<Subscriber> subscriberResponse = subscriberService.createSubscriber(subscriber);
        return ResponseEntity.status(subscriberResponse.getStatusCode()).body(subscriberResponse);
    }
}
