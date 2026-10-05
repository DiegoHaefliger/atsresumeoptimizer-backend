package com.diegohaefliger.atsresumeoptimizer.rewrite.application;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class SkillLineCleanerTest {

	private final SkillLineCleaner cleaner = new SkillLineCleaner("""
			Banco de dados: PostgreSQL, Oracle, SQL Server, MySQL e MongoDB
			Arquitetura: Microsserviços, mensageria (Kafka)""");

	@Test
	void dropsTheJobAnnotationTheAiAttachedToOneItemAndTheRepeatedItem() {
		assertThat(cleaner.clean("PostgreSQL,PostgreSQL (bancos de dados relacionais), Oracle, SQL Server, "
				+ "MySQL e MongoDB (bancos de dados não relacionais)"))
				.isEqualTo("PostgreSQL, Oracle, SQL Server, MySQL e MongoDB");
	}

	@Test
	void keepsAParentheticalTheOriginalAlreadyHad() {
		assertThat(cleaner.clean("Microsserviços, mensageria (Kafka)")).isEqualTo("Microsserviços, mensageria (Kafka)");
	}

	@Test
	void dropsAJoinedItemWhenItsPartsAreAlreadyListed() {
		assertThat(cleaner.clean("PostgreSQL, Oracle, SQL Server, MySQL, MongoDB, MySQL e MongoDB"))
				.isEqualTo("PostgreSQL, Oracle, SQL Server, MySQL, MongoDB");
		assertThat(cleaner.clean("PostgreSQL, MySQL e MongoDB")).isEqualTo("PostgreSQL, MySQL e MongoDB");
	}
}
