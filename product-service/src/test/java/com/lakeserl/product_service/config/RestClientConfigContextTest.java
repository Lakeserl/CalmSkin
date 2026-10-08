package com.lakeserl.product_service.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.lakeserl.product_service.client.OrderServiceClient;
import com.lakeserl.product_service.client.UserServiceClient;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

class RestClientConfigContextTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(
                    RestClientConfig.class,
                    UserServiceClient.class,
                    OrderServiceClient.class)
            .withPropertyValues("app.internal-secret=test-secret");

    @Test
    void registersServiceClientsAndTheirRestClientsWithoutNameCollisions() {
        contextRunner.run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context).hasSingleBean(UserServiceClient.class);
            assertThat(context).hasSingleBean(OrderServiceClient.class);
            assertThat(context).hasBean("userServiceRestClient");
            assertThat(context).hasBean("orderServiceRestClient");
        });
    }
}
