package com.diegohaefliger.atsresumeoptimizer.desktop.application;

import com.diegohaefliger.atsresumeoptimizer.desktop.domain.DatabaseMode;
import com.diegohaefliger.atsresumeoptimizer.desktop.domain.DatabaseStatus;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.Optional;
import java.util.stream.Stream;
import javax.sql.DataSource;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.boot.info.BuildProperties;
import org.springframework.boot.jdbc.autoconfigure.JdbcConnectionDetails;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration(proxyBeanMethods = false)
@Profile("desktop")
class DesktopDatabaseConfig {

	private static final String INSTANCE_LOCK_BEAN = "databaseInstanceLock";
	private static final String APP_NAME = "ATS Resume Optimizer";
	private static final String UNKNOWN_VERSION = "versão de desenvolvimento";
	private static final String UNKNOWN_HOST = "computador sem nome";

	@Bean
	ExternalDatabaseStore externalDatabaseStore(DesktopProperties properties) {
		return new ExternalDatabaseStore(properties.dataDir());
	}

	@Bean
	DatabaseStatus runningDatabase(ExternalDatabaseStore store) {
		return store.load()
				.map(external -> new DatabaseStatus(DatabaseMode.EXTERNAL, Optional.of(external)))
				.orElseGet(() -> new DatabaseStatus(DatabaseMode.EMBEDDED, Optional.empty()));
	}

	@Bean
	JdbcConnectionDetails desktopDatabase(DatabaseStatus running, DesktopProperties properties) throws IOException {
		if (running.external().isPresent()) {
			return new ExternalDatabaseConnectionDetails(running.external().get());
		}
		return new EmbeddedPostgresServer(properties);
	}

	@Bean(name = INSTANCE_LOCK_BEAN, destroyMethod = "close")
	DatabaseInstanceLock databaseInstanceLock(JdbcConnectionDetails desktopDatabase, ObjectProvider<BuildProperties> build) {
		String version = Optional.ofNullable(build.getIfAvailable()).map(BuildProperties::getVersion).orElse(UNKNOWN_VERSION);
		return DatabaseInstanceLock.acquire(desktopDatabase, "%s %s em %s".formatted(APP_NAME, version, hostName()));
	}

	@Bean
	static BeanFactoryPostProcessor dataSourceAfterInstanceLock() {
		return factory -> Arrays.stream(factory.getBeanNamesForType(DataSource.class, true, false))
				.map(factory::getBeanDefinition)
				.forEach(definition -> definition.setDependsOn(withInstanceLock(definition)));
	}

	private static String[] withInstanceLock(BeanDefinition definition) {
		String[] current = definition.getDependsOn() == null ? new String[0] : definition.getDependsOn();
		return Stream.concat(Arrays.stream(current), Stream.of(INSTANCE_LOCK_BEAN)).distinct().toArray(String[]::new);
	}

	private static String hostName() {
		return Stream.of(System.getenv("COMPUTERNAME"), System.getenv("HOSTNAME"))
				.filter(name -> name != null && !name.isBlank())
				.findFirst()
				.orElseGet(DesktopDatabaseConfig::resolvedHostName);
	}

	private static String resolvedHostName() {
		try {
			return InetAddress.getLocalHost().getHostName();
		} catch (UnknownHostException exception) {
			return UNKNOWN_HOST;
		}
	}
}
