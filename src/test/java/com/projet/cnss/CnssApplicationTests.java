package com.projet.cnss;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
@Disabled("Nécessite une vraie infra (MySQL + Minio) non disponible sur Jenkins — à activer avec un profil dédié")
class CnssApplicationTests {

	@Test
	void contextLoads() {
	}
}