package com.lakeserl.ai_skin_analysis_service.opencv;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bytedeco.opencv.opencv_core.Mat;
import org.bytedeco.opencv.opencv_core.MatVector;
import org.bytedeco.opencv.opencv_core.Scalar;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.bytedeco.opencv.global.opencv_core.*;
import static org.bytedeco.opencv.global.opencv_imgcodecs.IMREAD_COLOR;
import static org.bytedeco.opencv.global.opencv_imgcodecs.imdecode;
import static org.bytedeco.opencv.global.opencv_imgproc.*;

@Slf4j
@Component
@RequiredArgsConstructor
public class FeatureExtractor {

    private final ObjectMapper objectMapper;

    public String extractFeatures(byte[] imageBytes) {
        Mat matWrapper = null;
        Mat src = null;
        Mat hsv = null;
        MatVector channels = null;
        try {
            matWrapper = new Mat(1, imageBytes.length, CV_8UC1);
            matWrapper.data().put(imageBytes);
            src = imdecode(matWrapper, IMREAD_COLOR);
            matWrapper.close();
            matWrapper = null;

            if (src.empty()) {
                log.warn("Could not decode image for feature extraction");
                return "{}";
            }

            hsv = new Mat();
            cvtColor(src, hsv, COLOR_BGR2HSV);

            channels = new MatVector(3);
            split(hsv, channels);

            Mat hChannel = channels.get(0);
            Mat sChannel = channels.get(1);
            Mat vChannel = channels.get(2);

            Scalar meanV = mean(vChannel);
            Scalar meanH = mean(hChannel);
            Scalar meanS = mean(sChannel);

            Map<String, Object> features = new LinkedHashMap<>();
            features.put("brightness", Math.round(meanV.get(0)));
            features.put("hue", Math.round(meanH.get(0)));
            features.put("saturation", Math.round(meanS.get(0)));
            features.put("width", src.cols());
            features.put("height", src.rows());

            double hueVal = meanH.get(0);
            double satVal = meanS.get(0);
            boolean hasRedness = (hueVal < 15 || hueVal > 165) && satVal > 80;
            features.put("hasRedness", hasRedness);
            features.put("brightnessLevel", meanV.get(0) > 180 ? "HIGH" : meanV.get(0) > 120 ? "MEDIUM" : "LOW");

            return objectMapper.writeValueAsString(features);

        } catch (Exception e) {
            log.error("Feature extraction failed: {}", e.getMessage());
            return "{}";
        } finally {
            // close() on MatVector destroys the C++ vector and all channel Mats it owns.
            // Do NOT release individual channels separately — channels.get(i) returns a view,
            // so calling close() on both the view and the parent vector is a double-free.
            if (channels != null) channels.close();
            if (hsv != null) hsv.close();
            if (src != null) src.close();
            if (matWrapper != null) matWrapper.close();
        }
    }
}
