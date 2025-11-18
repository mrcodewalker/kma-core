package com.example.KMALegend;

import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class KmaLegendApplication {

	public static void main(String[] args) {
		Dotenv dotenv = Dotenv.configure()
				.directory(".")
				.filename("SYSTEM32.env")
				.load();
		System.setProperty("server.port", dotenv.get("SERVER_PORT"));
		System.setProperty("api.prefix", dotenv.get("API_PREFIX"));

		System.setProperty("spring.datasource.url", dotenv.get("SPRING_DATASOURCE_URL"));
		System.setProperty("spring.datasource.username", dotenv.get("SPRING_DATASOURCE_USERNAME"));
		System.setProperty("spring.datasource.password", dotenv.get("SPRING_DATASOURCE_PASSWORD"));
		System.setProperty("spring.datasource.driver-class-name", dotenv.get("SPRING_DATASOURCE_DRIVER_CLASS_NAME"));
		System.setProperty("jwt.secret", dotenv.get("JWT_SECRET"));
		System.setProperty("jwt.expiration", dotenv.get("JWT_EXPIRATION"));
		System.setProperty("springdoc.enabled", dotenv.get("ENABLED_SWAGGER"));

		SpringApplication.run(KmaLegendApplication.class, args);
	}

}
