package de.clique.westwood.converter;

import static org.assertj.core.api.Assertions.assertThat;
import de.clique.westwood.converter.service.ConverterService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class AppTests {

	@Autowired
	private ConverterService service;

	@Test
	void contextLoads() {
		assertThat(service).isNotNull();
	}

}
