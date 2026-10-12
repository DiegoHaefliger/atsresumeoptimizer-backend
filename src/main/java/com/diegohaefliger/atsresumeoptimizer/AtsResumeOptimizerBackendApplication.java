package com.diegohaefliger.atsresumeoptimizer;

import com.diegohaefliger.atsresumeoptimizer.desktop.DesktopLauncher;
import java.io.IOException;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@ConfigurationPropertiesScan
@EnableScheduling
public class AtsResumeOptimizerBackendApplication {

	public static void main(String[] args) throws IOException {
		if (DesktopLauncher.requested(args)) {
			new DesktopLauncher(AtsResumeOptimizerBackendApplication.class).launch(args);
			return;
		}
		SpringApplication.run(AtsResumeOptimizerBackendApplication.class, args);
	}

}
