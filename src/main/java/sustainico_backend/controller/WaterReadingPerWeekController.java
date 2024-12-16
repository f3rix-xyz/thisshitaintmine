package sustainico_backend.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import sustainico_backend.Models.WaterReadingPerHour;
import sustainico_backend.Models.WaterReadingPerWeek;
import sustainico_backend.service.WaterReadingPerHourService;
import sustainico_backend.service.WaterReadingPerWeekService;

import java.util.List;


@RestController
@RequestMapping("/readingperweek")
public class WaterReadingPerWeekController {

    @Autowired
    private WaterReadingPerWeekService waterReadingPerWeekService;

    @GetMapping
    public List<WaterReadingPerWeek> getAllReadings() {
        return waterReadingPerWeekService.findAll();
    }
//
//    @GetMapping("/{deviceId}")
//    public List<WaterReadingPerWeek> getReadingsByDeviceId(@PathVariable String deviceId) {
//        return waterReadingPerWeekService.getWaterReadingsByDeviceId(deviceId);
//    }
}
