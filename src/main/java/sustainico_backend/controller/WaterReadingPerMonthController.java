package sustainico_backend.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import sustainico_backend.Models.SimplifiedWaterReading;
import sustainico_backend.Models.WaterReadingPerMonth;
import sustainico_backend.Models.WaterReadingPerWeek;
import sustainico_backend.service.WaterReadingPerMonthService;
import sustainico_backend.service.WaterReadingPerWeekService;

import java.util.List;

@RestController
@RequestMapping("/readingpermonth")
@CrossOrigin(origins = "*")

public class WaterReadingPerMonthController {

    @Autowired
    private WaterReadingPerMonthService waterReadingPerMonthService;

    @GetMapping
    public List<WaterReadingPerMonth> getAllReadings() {
        return waterReadingPerMonthService.findAll();
    }

    @GetMapping("/{deviceId}/yearly/{year}")
    public List<SimplifiedWaterReading> getYearlyMonthlyReadings(
            @PathVariable String deviceId,
            @PathVariable String year) {
        return waterReadingPerMonthService.getMonthlyReadingsForYear(deviceId, year);
    }
}
