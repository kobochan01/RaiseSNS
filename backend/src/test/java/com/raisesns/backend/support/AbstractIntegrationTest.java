package com.raisesns.backend.support;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
@SpringBootTest
// Windows版Docker Desktopのポートフォワーディングが長時間の接続で不安定になることがあるため、
// HikariCPの接続タイムアウトを既定の30秒より延ばしてテストのflakinessを抑える。
@TestPropertySource(properties = {
        "spring.datasource.hikari.connection-timeout=60000",
        "spring.datasource.hikari.initialization-fail-timeout=60000"
})
public abstract class AbstractIntegrationTest {
}
