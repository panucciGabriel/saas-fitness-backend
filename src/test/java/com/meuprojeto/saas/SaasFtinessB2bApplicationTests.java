package com.meuprojeto.saas;

import com.meuprojeto.saas.config.TenantMigrationRunner;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class SaasFtinessB2bApplicationTests {

	@MockBean
	private TenantMigrationRunner tenantMigrationRunner;

	@Test
	void contextLoads() {
	}

}
