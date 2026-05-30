package com.lakeserl.ai_skin_analysis_service.opencv;

import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class ZoneMapper {

    public Map<String, String> mapZones(byte[] imageBytes) {
        // Divide 512x512 image into standard facial zones by Y-coordinate percentage
        return Map.of(
                "forehead", "upper-30pct",
                "tzone", "center-vertical",
                "cheeks", "middle-sides",
                "chin", "lower-15pct"
        );
    }
}
