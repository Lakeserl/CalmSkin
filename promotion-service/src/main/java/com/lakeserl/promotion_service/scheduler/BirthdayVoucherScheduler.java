package com.lakeserl.promotion_service.scheduler;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.lakeserl.promotion_service.client.UserServiceClient;
import com.lakeserl.promotion_service.config.properties.PromotionProperties;
import com.lakeserl.promotion_service.service.VoucherService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Grants the configured birthday voucher every day at midnight to users whose
 * date of birth is today. The user lookup is an external call kept outside the
 * DB transaction; the assignment itself runs transactionally in VoucherService.
 * Disabled when {@code app.promotion.birthday-voucher-code} is unset.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BirthdayVoucherScheduler {

    private final UserServiceClient userServiceClient;
    private final VoucherService voucherService;
    private final PromotionProperties properties;

    @Scheduled(cron = "0 0 0 * * *")
    public void grantBirthdayVouchers() {
        String code = properties.birthdayVoucherCode();
        if (code == null || code.isBlank()) {
            return;
        }
        LocalDate today = LocalDate.now();
        List<UUID> userIds = userServiceClient.findUserIdsByBirthday(
                today.getMonthValue(), today.getDayOfMonth());
        if (userIds.isEmpty()) {
            return;
        }
        int granted = voucherService.grantByCode(code, userIds, "BIRTHDAY");
        log.info("Birthday voucher: granted {} of {} users with a birthday today",
                granted, userIds.size());
    }
}
