package com.lakeserl.ai_skin_analysis_service.service;

import com.lakeserl.ai_skin_analysis_service.dto.response.RoutineDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
public class RoutineGeneratorServiceImpl implements RoutineGeneratorService {

    @Override
    public RoutineDTO generateMorningRoutine(String detectedSkinType, List<String> concerns) {
        List<String> steps = new ArrayList<>();
        steps.add("CLEANSE — Use a gentle cleanser suited for " + normalizeType(detectedSkinType) + " skin");
        steps.add("TONE — Apply an alcohol-free toner to balance pH");
        if (hasConcern(concerns, "ACNE") || hasConcern(concerns, "DARK_SPOTS")) {
            steps.add("SERUM — Vitamin C serum for brightening and acne control");
        } else if (hasConcern(concerns, "DEHYDRATION") || hasConcern(concerns, "DULLNESS")) {
            steps.add("SERUM — Hyaluronic acid serum for deep hydration");
        } else {
            steps.add("SERUM — Niacinamide serum for pore refinement");
        }
        if ("DRY".equalsIgnoreCase(detectedSkinType) || "SENSITIVE".equalsIgnoreCase(detectedSkinType)) {
            steps.add("MOISTURIZE — Rich moisturizer with ceramides and hyaluronic acid");
        } else if ("OILY".equalsIgnoreCase(detectedSkinType)) {
            steps.add("MOISTURIZE — Lightweight gel moisturizer, oil-free formula");
        } else {
            steps.add("MOISTURIZE — Balanced moisturizer with SPF-compatible formula");
        }
        steps.add("SPF — Broad-spectrum SPF 30+ sunscreen (essential for all skin types)");
        return RoutineDTO.builder().steps(steps).build();
    }

    @Override
    public RoutineDTO generateEveningRoutine(String detectedSkinType, List<String> concerns) {
        List<String> steps = new ArrayList<>();
        steps.add("CLEANSE — Double cleanse: micellar water then gentle facial wash");
        steps.add("TONE — Exfoliating toner (AHA/BHA) 2-3x per week, hydrating toner other nights");
        if (hasConcern(concerns, "WRINKLES")) {
            steps.add("TREATMENT — Retinol serum (start 2x/week, build tolerance gradually)");
        } else if (hasConcern(concerns, "ACNE")) {
            steps.add("TREATMENT — Benzoyl peroxide spot treatment or salicylic acid serum");
        } else if (hasConcern(concerns, "DARK_SPOTS")) {
            steps.add("TREATMENT — Alpha arbutin or tranexamic acid for hyperpigmentation");
        } else {
            steps.add("TREATMENT — Peptide serum for skin repair overnight");
        }
        if ("DRY".equalsIgnoreCase(detectedSkinType) || "SENSITIVE".equalsIgnoreCase(detectedSkinType)) {
            steps.add("MOISTURIZE — Thick night cream or sleeping mask for intense repair");
        } else {
            steps.add("MOISTURIZE — Night moisturizer with regenerating actives");
        }
        if (hasConcern(concerns, "ENLARGED_PORES") || hasConcern(concerns, "ACNE")) {
            steps.add("EYE CREAM — Caffeine eye cream to reduce puffiness");
        }
        return RoutineDTO.builder().steps(steps).build();
    }

    private boolean hasConcern(List<String> concerns, String concern) {
        if (concerns == null) return false;
        return concerns.stream().anyMatch(c -> concern.equalsIgnoreCase(c));
    }

    private String normalizeType(String skinType) {
        if (skinType == null) return "normal";
        return skinType.toLowerCase();
    }
}
