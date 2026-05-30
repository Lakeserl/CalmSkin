package com.lakeserl.ai_skin_analysis_service.opencv;

import lombok.extern.slf4j.Slf4j;
import org.bytedeco.opencv.opencv_core.Mat;
import org.bytedeco.opencv.opencv_core.Size;
import org.springframework.stereotype.Component;

import static org.bytedeco.opencv.global.opencv_imgcodecs.*;
import static org.bytedeco.opencv.global.opencv_imgproc.resize;

@Slf4j
@Component
public class ImageNormalizer {

    private static final int TARGET_SIZE = 512;
    // 25 megapixels: decompression bomb protection — a 10MB file should never need more
    private static final long MAX_PIXELS = 25_000_000L;

    public byte[] normalize(byte[] imageBytes) {
        Mat matWrapper = null;
        Mat src = null;
        Mat resized = null;
        Mat outputMat = null;
        try {
            matWrapper = new Mat(1, imageBytes.length, org.bytedeco.opencv.global.opencv_core.CV_8UC1);
            matWrapper.data().put(imageBytes);
            src = imdecode(matWrapper, IMREAD_COLOR);
            matWrapper.close();
            matWrapper = null;

            if (src.empty()) {
                log.warn("Could not decode image for normalization, returning original bytes");
                return imageBytes;
            }

            if ((long) src.rows() * src.cols() > MAX_PIXELS) {
                log.warn("Image dimensions {}x{} exceed {} pixel limit — rejecting", src.cols(), src.rows(), MAX_PIXELS);
                throw new IllegalArgumentException("Image dimensions exceed maximum allowed size");
            }

            resized = new Mat();
            Size targetSize = new Size(TARGET_SIZE, TARGET_SIZE);
            resize(src, resized, targetSize);
            targetSize.close();

            outputMat = new Mat();
            imencode(".jpg", resized, outputMat);
            byte[] result = new byte[(int) outputMat.total()];
            outputMat.data().get(result);
            return result;

        } catch (Exception e) {
            log.error("Image normalization failed: {} — returning original bytes", e.getMessage());
            return imageBytes;
        } finally {
            if (outputMat != null) outputMat.close();
            if (resized != null) resized.close();
            if (src != null) src.close();
            if (matWrapper != null) matWrapper.close();
        }
    }
}
