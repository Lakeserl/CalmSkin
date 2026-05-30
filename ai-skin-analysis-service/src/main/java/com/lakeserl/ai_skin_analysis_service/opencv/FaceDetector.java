package com.lakeserl.ai_skin_analysis_service.opencv;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.bytedeco.opencv.opencv_core.Mat;
import org.bytedeco.opencv.opencv_core.RectVector;
import org.bytedeco.opencv.opencv_objdetect.CascadeClassifier;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

import static org.bytedeco.opencv.global.opencv_imgcodecs.IMREAD_COLOR;
import static org.bytedeco.opencv.global.opencv_imgcodecs.imdecode;

@Slf4j
@Component
public class FaceDetector {

    private CascadeClassifier classifier;

    @PostConstruct
    public void init() {
        try {
            InputStream in = getClass().getClassLoader().getResourceAsStream("opencv/haarcascade_frontalface_default.xml");
            if (in == null) {
                log.warn("Haarcascade not found in resources — face detection disabled, all images will pass");
                return;
            }
            byte[] bytes = in.readAllBytes();
            in.close();
            // Check if file has meaningful content (real XML, not a placeholder)
            if (bytes.length < 100) {
                log.warn("Haarcascade file is a placeholder — face detection disabled, all images will pass");
                return;
            }
            Path tmpFile = Files.createTempFile("haarcascade", ".xml");
            Files.write(tmpFile, bytes);
            classifier = new CascadeClassifier(tmpFile.toString());
            if (classifier.empty()) {
                log.warn("CascadeClassifier loaded but is empty — face detection disabled");
                classifier = null;
            } else {
                log.info("FaceDetector initialized with haarcascade classifier");
            }
        } catch (Exception e) {
            log.warn("Failed to load haarcascade: {} — face detection disabled", e.getMessage());
        }
    }

    public boolean hasFace(byte[] imageBytes) {
        if (classifier == null) {
            log.debug("Face detection disabled — treating all images as containing a face");
            return true;
        }
        Mat matWrapper = null;
        Mat image = null;
        RectVector faces = null;
        try {
            matWrapper = new Mat(1, imageBytes.length, org.bytedeco.opencv.global.opencv_core.CV_8UC1);
            matWrapper.data().put(imageBytes);
            image = imdecode(matWrapper, IMREAD_COLOR);
            if (image.empty()) {
                log.warn("Could not decode image for face detection");
                return false;
            }
            faces = new RectVector();
            classifier.detectMultiScale(image, faces);
            return faces.size() > 0;
        } catch (Exception e) {
            log.error("Error during face detection: {} — allowing image to pass", e.getMessage());
            return true;
        } finally {
            if (faces != null) faces.close();
            if (image != null) image.close();
            if (matWrapper != null) matWrapper.close();
        }
    }
}
