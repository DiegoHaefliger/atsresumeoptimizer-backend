package com.diegohaefliger.atsresumeoptimizer.jobsearch.application;

import com.diegohaefliger.atsresumeoptimizer.NormalizedText;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class JobDescriptionFormatter {

	private static final int MAX_HEADING_LENGTH = 60;
	private static final int MAX_ITEM_LENGTH = 250;
	private static final int MIN_ITEMS_FOR_LIST = 2;
	private static final Pattern BULLET = Pattern.compile("^(?:[•·▪●◦–—-]|\\*(?!\\*))\\s+(.+)$");
	private static final Pattern BOLD_LINE = Pattern.compile("^\\*\\*(.+)\\*\\*$");
	private static final Pattern HEADING_WITH_CONTENT = Pattern.compile("^([^:]{3,60}):\\s+(\\S.*)$");
	private static final Pattern BLANK_LINES = Pattern.compile("\\n{3,}");
	private static final Set<String> HEADINGS = Set.of("responsabilidades", "responsabilidades e atribuicoes",
			"atribuicoes", "atividades", "principais atividades", "requisitos", "requisitos e qualificacoes", "requisitos obrigatorios",
			"requisitos desejaveis", "qualificacoes", "pre requisitos", "diferenciais", "desejavel", "desejaveis", "beneficios",
			"sobre a vaga", "sobre a empresa", "sobre nos", "sobre o time", "descricao da vaga", "o que voce vai fazer",
			"o que esperamos", "o que buscamos", "o que oferecemos", "etapas do processo", "processo seletivo",
			"informacoes adicionais", "stack", "tecnologias", "habilidades", "conhecimentos", "conhecimentos tecnicos", "formacao",
			"local de trabalho", "remuneracao", "horario de trabalho", "missao", "desafios", "perfil", "perfil da vaga");
	private static final Set<String> COMMA_SEPARATED = Set.of("beneficios");

	private JobDescriptionFormatter() {
	}

	static String format(String description) {
		if (description == null || description.isBlank()) {
			return "";
		}
		List<String> lines = description.replace("\r", "").lines().map(String::strip).toList();
		List<String> out = new ArrayList<>();
		for (String line : lines) {
			appendLine(out, line);
		}
		return BLANK_LINES.matcher(String.join("\n", listed(out))).replaceAll("\n\n").strip();
	}

	private static void appendLine(List<String> out, String line) {
		Matcher bullet = BULLET.matcher(line);
		if (bullet.matches()) {
			out.add("- " + bullet.group(1).strip());
			return;
		}
		Matcher withContent = HEADING_WITH_CONTENT.matcher(line);
		if (withContent.matches() && isKnownHeading(withContent.group(1))) {
			addHeading(out, withContent.group(1));
			splitItems(withContent.group(1), withContent.group(2)).forEach(out::add);
			return;
		}
		if (isHeading(line)) {
			addHeading(out, line);
			return;
		}
		out.add(line);
	}

	private static List<String> splitItems(String heading, String content) {
		String separator = content.contains(";") ? ";" : COMMA_SEPARATED.contains(key(heading)) ? "," : null;
		if (separator == null) {
			return List.of(content);
		}
		return List.of(content.split(separator)).stream().map(String::strip).filter(item -> !item.isEmpty())
				.map(item -> "- " + item).toList();
	}

	private static void addHeading(List<String> out, String text) {
		String title = text.strip();
		Matcher bold = BOLD_LINE.matcher(title);
		title = bold.matches() ? bold.group(1) : title;
		title = title.replaceAll("[:\\s]+$", "");
		if (!out.isEmpty() && !out.getLast().isEmpty()) {
			out.add("");
		}
		out.add("**" + title + "**");
	}

	private static boolean isHeading(String line) {
		if (line.isEmpty() || line.length() > MAX_HEADING_LENGTH || line.startsWith("- ")) {
			return false;
		}
		return BOLD_LINE.matcher(line).matches() || isKnownHeading(line);
	}

	private static boolean isKnownHeading(String text) {
		return HEADINGS.contains(key(text));
	}

	private static String key(String text) {
		return NormalizedText.of(text.replaceAll("[*:]+", " ").replaceAll("[^\\p{L}\\p{N} ]+", " ")).replaceAll("\\s+", " ");
	}

	private static List<String> listed(List<String> lines) {
		List<String> result = new ArrayList<>();
		int index = 0;
		while (index < lines.size()) {
			String line = lines.get(index);
			boolean afterHeading = index > 0 && lines.get(index - 1).startsWith("**");
			if (!afterHeading) {
				result.add(line);
				index++;
				continue;
			}
			int end = index;
			while (end < lines.size() && isPlainItem(lines.get(end))) {
				end++;
			}
			if (end - index >= MIN_ITEMS_FOR_LIST) {
				lines.subList(index, end).forEach(item -> result.add("- " + item));
				index = end;
			} else {
				result.add(line);
				index++;
			}
		}
		return result;
	}

	private static boolean isPlainItem(String line) {
		return !line.isEmpty() && line.length() <= MAX_ITEM_LENGTH && !line.startsWith("- ") && !line.startsWith("**");
	}
}
