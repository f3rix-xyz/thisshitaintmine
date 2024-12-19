package sustainico_backend.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import sustainico_backend.Models.SimplifiedWaterReading;
import sustainico_backend.Models.WaterReadingPerDay;
import sustainico_backend.Models.WaterReadingPerHour;
import sustainico_backend.service.WaterReadingPerDayService;
import sustainico_backend.service.WaterReadingPerHourService;

import java.util.List;

@RestController
@RequestMapping("/readingperday")
@CrossOrigin(origins = "*")

public class WaterReadingPerDayController {

    @Autowired
    private WaterReadingPerDayService waterReadingPerDayService;

    @GetMapping
    public List<WaterReadingPerDay> getAllReadings() {
        return waterReadingPerDayService.findAll();
    }

    @GetMapping("/{deviceId}/monthly/{date}")
    public List<SimplifiedWaterReading> getMonthlyDailyReadings(
            @PathVariable String deviceId,
            @PathVariable String date) {
        return waterReadingPerDayService.getDailyReadingsForMonth(deviceId, date);
    }
}
