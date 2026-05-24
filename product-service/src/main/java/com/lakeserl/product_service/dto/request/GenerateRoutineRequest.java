package com.lakeserl.product_service.dto.request;

import java.util.List;

public class GenerateRoutineRequest {
    private String skinType;
    private List<String> skinConcerns;

    public GenerateRoutineRequest() {}

    public String getSkinType() {
        return skinType;
    }

    public void setSkinType(String skinType) {
        this.skinType = skinType;
    }

    public List<String> getSkinConcerns() {
        return skinConcerns;
    }

    public void setSkinConcerns(List<String> skinConcerns) {
        this.skinConcerns = skinConcerns;
    }
}
