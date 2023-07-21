package com.cvcopilot.resumebuilding;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;

@SpringBootApplication(exclude = {DataSourceAutoConfiguration.class })
public class ResumeBuildingApplication {

	public static void main(String[] args) {
		SpringApplication.run(ResumeBuildingApplication.class, args);
	}

}
