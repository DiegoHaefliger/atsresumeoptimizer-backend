package com.diegohaefliger.atsresumeoptimizer;

import org.springframework.boot.SpringApplication;

public class TestAtsResumeOptimizerBackendApplication {

	public static void main(String[] args) {
		SpringApplication.from(AtsResumeOptimizerBackendApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
