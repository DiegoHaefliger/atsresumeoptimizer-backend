package com.diegohaefliger.atsresumeoptimizer.linkedin.application;

import com.diegohaefliger.atsresumeoptimizer.careerprofile.CareerProfile;
import com.diegohaefliger.atsresumeoptimizer.linkedin.domain.LinkedInEntry;
import com.diegohaefliger.atsresumeoptimizer.linkedin.domain.LinkedInProfile;
import com.diegohaefliger.atsresumeoptimizer.linkedin.domain.LinkedInSnapshot;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

final class LinkedInPromptText {

	private static final String NOT_PROVIDED = "não informado";

	private LinkedInPromptText() {
	}

	static String careerProfile(CareerProfile career) {
		List<String> lines = new ArrayList<>();
		line(lines, "Área de atuação", career.areaOfExpertise());
		line(lines, "Cargo atual", career.currentJobTitle());
		line(lines, "Senioridade atual", career.currentSeniority());
		line(lines, "Próximo cargo desejado (cargo-alvo)", career.targetJobTitle());
		line(lines, "Senioridade alvo", career.targetSeniority());
		line(lines, "Prazo para a transição", career.targetTimeframe());
		line(lines, "Setores alvo", String.join(", ", career.targetIndustries()));
		line(lines, "Empresas alvo", String.join(", ", career.targetCompanies()));
		line(lines, "Localização", career.location());
		line(lines, "Idiomas", String.join(", ", career.languages()));
		line(lines, "Pontos fortes", String.join(", ", career.strengths()));
		line(lines, "Diferenciais", career.differentiators());
		line(lines, "Tom desejado do perfil", career.tone().name().toLowerCase());
		line(lines, "Idioma do perfil", career.profileLanguage());
		line(lines, "Observações", career.notes());
		return String.join("\n", lines);
	}

	static String profile(LinkedInSnapshot snapshot) {
		LinkedInProfile profile = snapshot.profile();
		List<String> lines = new ArrayList<>();
		line(lines, "Nome", profile.fullName());
		line(lines, "Endereço do perfil", profile.profileUrl());
		line(lines, "URL personalizada", profile.customUrl());
		lines.add("Foto de perfil: " + (snapshot.hasPhoto() ? "enviada (anexada)" : "não enviada"));
		lines.add("Banner: " + (snapshot.hasBanner() ? "enviado (anexado)" : "não enviado"));
		line(lines, "Título (headline)", profile.headline());
		line(lines, "Localização", profile.location());
		line(lines, "Seguidores", profile.followers());
		line(lines, "Conexões", profile.connections());
		lines.add("Sobre:\n" + (profile.about() == null ? NOT_PROVIDED : profile.about()));
		entries(lines, "Experiência", profile.experiences());
		entries(lines, "Formação", profile.educations());
		list(lines, "Competências (na ordem do perfil, as 3 primeiras são as fixadas)", profile.skills());
		entries(lines, "Licenças e certificados", profile.certifications());
		list(lines, "Idiomas", profile.languages());
		entries(lines, "Projetos", profile.projects());
		entries(lines, "Publicações", profile.publications());
		list(lines, "Recomendações recebidas", profile.recommendations());
		list(lines, "Em destaque", profile.featured());
		list(lines, "Atividade recente (posts)", profile.recentActivity());
		return String.join("\n", lines);
	}

	private static void line(List<String> lines, String label, String value) {
		if (value != null && !value.isBlank()) {
			lines.add(label + ": " + value);
		}
	}

	private static void list(List<String> lines, String label, List<String> values) {
		lines.add(label + ":" + (values.isEmpty() ? " " + NOT_PROVIDED : "\n"
				+ values.stream().map(value -> "- " + value).collect(Collectors.joining("\n"))));
	}

	private static void entries(List<String> lines, String label, List<LinkedInEntry> entries) {
		lines.add(label + ":" + (entries.isEmpty() ? " " + NOT_PROVIDED : "\n"
				+ entries.stream().map(LinkedInPromptText::entry).collect(Collectors.joining("\n"))));
	}

	private static String entry(LinkedInEntry entry) {
		List<String> parts = new ArrayList<>();
		for (String part : new String[] {entry.title(), entry.organization(), entry.period(), entry.location()}) {
			if (part != null) {
				parts.add(part);
			}
		}
		String head = "- " + String.join(" | ", parts);
		return entry.description() == null ? head : head + "\n  " + entry.description().replace("\n", "\n  ");
	}
}
