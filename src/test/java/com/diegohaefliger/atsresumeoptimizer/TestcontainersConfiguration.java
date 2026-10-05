package com.diegohaefliger.atsresumeoptimizer;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.DynamicPropertyRegistrar;
import org.testcontainers.containers.MinIOContainer;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

@TestConfiguration(proxyBeanMethods = false)
public class TestcontainersConfiguration {

	@Bean
	@ServiceConnection
	PostgreSQLContainer postgresContainer() {
		return new PostgreSQLContainer(DockerImageName.parse("postgres:17"));
	}

	@Bean
	MinIOContainer minioContainer() {
		DockerImageName minioImage = DockerImageName.parse("cgr.dev/chainguard/minio:latest")
				.asCompatibleSubstituteFor("minio/minio");
		return new MinIOContainer(minioImage);
	}

	@Bean
	DynamicPropertyRegistrar storageProperties(MinIOContainer minioContainer) {
		return registry -> {
			registry.add("app.storage.endpoint", minioContainer::getS3URL);
			registry.add("app.storage.access-key", minioContainer::getUserName);
			registry.add("app.storage.secret-key", minioContainer::getPassword);
		};
	}

}
