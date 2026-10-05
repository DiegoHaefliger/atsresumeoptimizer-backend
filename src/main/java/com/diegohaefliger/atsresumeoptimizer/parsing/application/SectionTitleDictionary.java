package com.diegohaefliger.atsresumeoptimizer.parsing.application;

import com.diegohaefliger.atsresumeoptimizer.NormalizedText;
import java.util.Set;
import java.util.regex.Pattern;

final class SectionTitleDictionary {

	private static final Set<String> NORMALIZED_TITLES = Set.of(
		"experiencia profissional", "experiencia", "experiencias profissionais", "experiencias",
		"professional experience", "work experience",
		"formacao academica", "formacao", "formacoes academicas", "formacoes", "educacao", "education",
		"habilidades", "habilidades tecnicas", "competencias", "competencias tecnicas", "skills",
		"idiomas", "languages",
		"certificacoes", "certifications", "cursos", "cursos e certificacoes",
		"resumo", "resumo profissional", "summary", "perfil", "profile", "objetivo", "objetivo profissional",
		"projetos", "projects",
		"referencias", "references",
		"contato", "informacoes de contato", "contact"
	);

	private static final Pattern LEADING_DECORATION = Pattern.compile("^[\\d.)\\-•*◦–\\s]+");
	private static final Pattern TRAILING_DECORATION = Pattern.compile("[\\s:\\-–]+$");

	private SectionTitleDictionary() {
	}

	static boolean matches(String line) {
		return NORMALIZED_TITLES.contains(normalize(line));
	}

	private static String normalize(String text) {
		String withoutLeadingDecoration = LEADING_DECORATION.matcher(NormalizedText.of(text)).replaceFirst("");
		return TRAILING_DECORATION.matcher(withoutLeadingDecoration).replaceFirst("");
	}
}
