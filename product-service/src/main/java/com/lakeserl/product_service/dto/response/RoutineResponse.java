package com.lakeserl.product_service.dto.response;

import java.util.List;

public class RoutineResponse {
    private RoutineSteps morning;
    private RoutineSteps evening;

    public RoutineResponse() {}

    public RoutineResponse(RoutineSteps morning, RoutineSteps evening) {
        this.morning = morning;
        this.evening = evening;
    }

    public RoutineSteps getMorning() {
        return morning;
    }

    public void setMorning(RoutineSteps morning) {
        this.morning = morning;
    }

    public RoutineSteps getEvening() {
        return evening;
    }

    public void setEvening(RoutineSteps evening) {
        this.evening = evening;
    }

    public static class RoutineSteps {
        private List<ProductSummaryDTO> cleanse;
        private List<ProductSummaryDTO> treat;
        private List<ProductSummaryDTO> moisturize;
        private List<ProductSummaryDTO> protect;

        public RoutineSteps() {}

        public RoutineSteps(List<ProductSummaryDTO> cleanse, List<ProductSummaryDTO> treat,
                            List<ProductSummaryDTO> moisturize, List<ProductSummaryDTO> protect) {
            this.cleanse = cleanse;
            this.treat = treat;
            this.moisturize = moisturize;
            this.protect = protect;
        }

        public List<ProductSummaryDTO> getCleanse() {
            return cleanse;
        }

        public void setCleanse(List<ProductSummaryDTO> cleanse) {
            this.cleanse = cleanse;
        }

        public List<ProductSummaryDTO> getTreat() {
            return treat;
        }

        public void setTreat(List<ProductSummaryDTO> treat) {
            this.treat = treat;
        }

        public List<ProductSummaryDTO> getMoisturize() {
            return moisturize;
        }

        public void setMoisturize(List<ProductSummaryDTO> moisturize) {
            this.moisturize = moisturize;
        }

        public List<ProductSummaryDTO> getProtect() {
            return protect;
        }

        public void setProtect(List<ProductSummaryDTO> protect) {
            this.protect = protect;
        }
    }
}
