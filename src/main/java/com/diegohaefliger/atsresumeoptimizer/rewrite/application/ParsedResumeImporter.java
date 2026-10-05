package com.diegohaefliger.atsresumeoptimizer.rewrite.application;

import com.diegohaefliger.atsresumeoptimizer.NormalizedText;
import com.diegohaefliger.atsresumeoptimizer.ai.KeyValueLine;
import com.diegohaefliger.atsresumeoptimizer.ai.ResumeEntry;
import com.diegohaefliger.atsresumeoptimizer.ai.ResumeSection;
import com.diegohaefliger.atsresumeoptimizer.ai.ResumeSectionKind;
import com.diegohaefliger.atsresumeoptimizer.ai.ResumeSectionSemanticType;
import com.diegohaefliger.atsresumeoptimizer.ai.StructuredResume;
import com.diegohaefliger.atsresumeoptimizer.ai.TextSpan;
import com.diegohaefliger.atsresumeoptimizer.parsing.domain.ContactInfo;
import com.diegohaefliger.atsresumeoptimizer.parsing.domain.ParsingResult;
import com.diegohaefliger.atsresumeoptimizer.parsing.domain.Section;
import com.diegohaefliger.atsresumeoptimizer.rewrite.ResumeContact;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/** Monta o conteúdo editável direto do texto extraído, sem IA: preserva o texto como o candidato escreveu. */
final class ParsedResumeImporter {

	private static final Set<ResumeSectionSemanticType> ENTRY_TYPES = Set.of(ResumeSectionSemanticType.EXPERIENCE,
			ResumeSectionSemanticType.EDUCATION, ResumeSectionSemanticType.PROJECTS);
	private static final Map<String, ResumeSectionSemanticType> TITLE_HINTS = new LinkedHashMap<>();

	static {
		TITLE_HINTS.put("resumo", ResumeSectionSemanticType.SUMMARY);
		TITLE_HINTS.put("perfil", ResumeSectionSemanticType.SUMMARY);
		TITLE_HINTS.put("objetivo", ResumeSectionSemanticType.SUMMARY);
		TITLE_HINTS.put("summary", ResumeSectionSemanticType.SUMMARY);
		TITLE_HINTS.put("experienc", ResumeSectionSemanticType.EXPERIENCE);
		TITLE_HINTS.put("formac", ResumeSectionSemanticType.EDUCATION);
		TITLE_HINTS.put("educa", ResumeSectionSemanticType.EDUCATION);
		TITLE_HINTS.put("habilidade", ResumeSectionSemanticType.SKILLS);
		TITLE_HINTS.put("competencia", ResumeSectionSemanticType.SKILLS);
		TITLE_HINTS.put("skill", ResumeSectionSemanticType.SKILLS);
		TITLE_HINTS.put("idioma", ResumeSectionSemanticType.LANGUAGES);
		TITLE_HINTS.put("language", ResumeSectionSemanticType.LANGUAGES);
		TITLE_HINTS.put("certific", ResumeSectionSemanticType.CERTIFICATIONS);
		TITLE_HINTS.put("curso", ResumeSectionSemanticType.CERTIFICATIONS);
		TITLE_HINTS.put("projet", ResumeSectionSemanticType.PROJECTS);
		TITLE_HINTS.put("project", ResumeSectionSemanticType.PROJECTS);
	}

	private static final String CONTACT_SEPARATOR = " | ";
	private static final int CONTACT_SLOTS = 6;
	private static final Pattern LANGUAGE_LEVEL_SEPARATOR = Pattern.compile("\\s*(?::|\\s[-–—]\\s)\\s*");
	private static final Pattern LANGUAGE_LEVEL_IN_PARENTHESES = Pattern.compile("^(.+?)\\s*\\((.+)\\)$");

	private ParsedResumeImporter() {
	}

	static StructuredResume content(ParsingResult parsingResult) {
		ResumeHeader header = ResumeHeader.of(parsingResult);
		String name = header.resolveName(null, parsingResult.document().rawText());
		String headline = header.lines().stream().skip(1).filter(ParsedResumeImporter::looksLikeHeadline).findFirst()
				.orElse(null);
		List<ResumeSection> sections = parsingResult.sections().stream().map(ParsedResumeImporter::section).toList();
		return new StructuredResume(name, headline, sections);
	}

	static ResumeContact contact(ParsingResult parsingResult) {
		ContactInfo found = parsingResult.contact();
		List<String> slots = contactLineSlots(ResumeHeader.of(parsingResult));
		boolean positional = slots.size() == CONTACT_SLOTS;
		return new ResumeContact(
				found.email().orElseGet(() -> slot(positional, slots, 0)),
				found.phone().orElseGet(() -> slot(positional, slots, 1)),
				found.linkedInProfile().orElseGet(() -> slot(positional, slots, 2)),
				found.githubProfile().orElseGet(() -> slot(positional, slots, 3)),
				found.portfolio().orElseGet(() -> slot(positional, slots, 4)),
				found.location().orElseGet(() -> slot(positional, slots, 5)));
	}

	private static String slot(boolean positional, List<String> slots, int index) {
		return positional ? slots.get(index) : null;
	}

	/** O sistema grava o cabeçalho sempre na ordem e-mail, telefone, LinkedIn, GitHub, portfólio, localização. */
	private static List<String> contactLineSlots(ResumeHeader header) {
		String joined = header.lines().stream().dropWhile(line -> !line.contains(CONTACT_SEPARATOR.strip()))
				.collect(Collectors.joining(" "));
		return Arrays.stream(joined.split(Pattern.quote(CONTACT_SEPARATOR.strip()))).map(String::strip)
				.filter(token -> !token.isEmpty()).toList();
	}

	private static ResumeSection section(Section section) {
		ResumeSectionSemanticType type = semanticType(section.title());
		List<ResumeTextLine> textLines = ResumeTextLine.of(section.content());
		if (ENTRY_TYPES.contains(type)) {
			Optional<List<ResumeEntry>> entries = EntrySectionParser.parse(textLines, type);
			if (entries.isPresent()) {
				return new ResumeSection(section.title(), type, ResumeSectionKind.ENTRIES, null, List.of(), List.of(),
						entries.get());
			}
		}
		List<String> lines = textLines.stream().map(ResumeTextLine::text).toList();
		if (type == ResumeSectionSemanticType.SUMMARY) {
			return new ResumeSection(section.title(), type, ResumeSectionKind.PARAGRAPH, String.join(" ", lines),
					List.<KeyValueLine>of(), List.of(), List.of());
		}
		if (type == ResumeSectionSemanticType.LANGUAGES) {
			return new ResumeSection(section.title(), type, ResumeSectionKind.KEY_VALUE, null,
					lines.stream().map(ParsedResumeImporter::language).toList(), List.of(), List.of());
		}
		List<List<TextSpan>> richLines = lines.stream().map(line -> List.of(new TextSpan(line, false))).toList();
		return new ResumeSection(section.title(), type, ResumeSectionKind.RICH_LINES, null, List.of(), richLines, List.of());
	}

	private static KeyValueLine language(String line) {
		String[] parts = LANGUAGE_LEVEL_SEPARATOR.split(line, 2);
		if (parts.length == 2) {
			return new KeyValueLine(parts[0].strip(), parts[1].strip());
		}
		var parenthesized = LANGUAGE_LEVEL_IN_PARENTHESES.matcher(line);
		return parenthesized.matches()
				? new KeyValueLine(parenthesized.group(1).strip(), parenthesized.group(2).strip())
				: new KeyValueLine(line.strip(), "");
	}

	private static ResumeSectionSemanticType semanticType(String title) {
		String normalized = NormalizedText.of(title);
		return TITLE_HINTS.entrySet().stream()
				.filter(hint -> normalized.contains(hint.getKey()))
				.map(Map.Entry::getValue)
				.findFirst()
				.orElse(ResumeSectionSemanticType.OTHER);
	}

	private static boolean looksLikeHeadline(String line) {
		return !line.contains("@") && line.chars().filter(Character::isDigit).count() < 3;
	}
}
