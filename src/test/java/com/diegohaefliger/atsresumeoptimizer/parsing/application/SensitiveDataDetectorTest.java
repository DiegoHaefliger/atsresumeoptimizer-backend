package com.diegohaefliger.atsresumeoptimizer.parsing.application;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class SensitiveDataDetectorTest {

	private final SensitiveDataDetector detector = new SensitiveDataDetector();

	@Test
	void detectsAndMasksAValidCpf() {
		String text = "Ana Silva, CPF 111.444.777-35, desenvolvedora backend.";

		assertThat(detector.containsSensitiveData(text)).isTrue();
		assertThat(detector.mask(text)).contains("[DADO REMOVIDO]").doesNotContain("111.444.777-35");
	}

	@Test
	void ignoresAnElevenDigitNumberThatIsNotAValidCpf() {
		String text = "Telefone (11) 91234-5678, CPF 123.456.789-00";

		assertThat(detector.containsSensitiveData(text)).isFalse();
		assertThat(detector.mask(text)).isEqualTo(text);
	}

	@Test
	void detectsAndMasksAnRgShapedNumber() {
		String text = "RG 12.345.678-9";

		assertThat(detector.containsSensitiveData(text)).isTrue();
		assertThat(detector.mask(text)).contains("[DADO REMOVIDO]").doesNotContain("12.345.678-9");
	}

	@Test
	void detectsAndMasksADateOfBirthNearTheWordNascimento() {
		String text = "Data de nascimento: 15/03/1990";

		assertThat(detector.containsSensitiveData(text)).isTrue();
		assertThat(detector.mask(text)).contains("[DADO REMOVIDO]").doesNotContain("15/03/1990");
	}

	@Test
	void aBareDateWithoutBirthContextIsNotFlagged() {
		String text = "Experiência de 05/2020 a 03/2023 na Empresa X";

		assertThat(detector.containsSensitiveData(text)).isFalse();
	}
}
