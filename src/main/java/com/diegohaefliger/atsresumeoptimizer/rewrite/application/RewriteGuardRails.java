package com.diegohaefliger.atsresumeoptimizer.rewrite.application;

import com.diegohaefliger.atsresumeoptimizer.NormalizedText;
import com.diegohaefliger.atsresumeoptimizer.ai.JobStructured;
import com.diegohaefliger.atsresumeoptimizer.ai.RequirementEvidence;
import com.diegohaefliger.atsresumeoptimizer.ai.ResumeEntry;
import com.diegohaefliger.atsresumeoptimizer.ai.ResumeSectionSemanticType;
import com.diegohaefliger.atsresumeoptimizer.ai.StructuredResume;
import com.diegohaefliger.atsresumeoptimizer.ai.TextSpan;
import com.diegohaefliger.atsresumeoptimizer.parsing.application.BulletTextExtractor;
import com.diegohaefliger.atsresumeoptimizer.parsing.domain.Section;
import com.diegohaefliger.atsresumeoptimizer.scoring.domain.TechVocabularyProvider;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

@Component
class RewriteGuardRails {

	private static final Set<String> EXPERIENCE_SECTION_TITLES = Set.of(
			"experiencia profissional", "experiencia", "experiencias profissionais", "experiencias",
			"professional experience", "work experience", "projetos", "projects");
	private static final Set<String> PROJECT_SECTION_TITLES = Set.of("projetos", "projects");
	private static final Pattern TRAILING_DECORATION = Pattern.compile("[\\s:\\-–]+$");

	private final TechVocabularyProvider vocabularyProvider;
	private final BulletTextExtractor bulletTextExtractor;
	private final SkillPreservationGuard skillPreservationGuard = new SkillPreservationGuard();
	private final DroppedBulletRestorer droppedBulletRestorer = new DroppedBulletRestorer();

	RewriteGuardRails(TechVocabularyProvider vocabularyProvider, BulletTextExtractor bulletTextExtractor) {
		this.vocabularyProvider = vocabularyProvider;
		this.bulletTextExtractor = bulletTextExtractor;
	}

	StructuredResume enforce(StructuredResume aiOutput, String originalText, List<Section> originalSections,
			JobStructured job, boolean jobHighlighted, List<RequirementEvidence> evidences) {
		List<List<String>> originalGroups = experienceBulletGroups(originalSections);
		List<String> originalBullets = originalGroups.stream().flatMap(List::stream).toList();
		var vocabulary = vocabularyProvider.forJob(job);
		var fabricatedTermGuard = new FabricatedTermGuard(vocabulary, originalText, originalBullets, evidences);
		var entryGroundingGuard =
				new EntryGroundingGuard(vocabulary, experienceText(originalSections), originalBullets, evidences);
		StructuredResume grounded = entryGroundingGuard.apply(fabricatedTermGuard.apply(aiOutput));
		StructuredResume guarded = droppedBulletRestorer.restore(
				skillPreservationGuard.apply(grounded, originalSections, jobHighlighted), originalGroups);
		ensureNoBulletWasDropped(guarded, originalBullets.size());
		ensureProjectsWereKept(guarded, originalSections);
		return new SkillLineCleaner(originalText).apply(withoutBoldBullets(guarded));
	}

	private StructuredResume withoutBoldBullets(StructuredResume content) {
		return content.withSections(content.sections().stream()
				.map(section -> section.semanticType().holdsBullets()
						? section.withEntries(section.entries().stream().map(RewriteGuardRails::withPlainBullets).toList())
						: section)
				.toList());
	}

	private static ResumeEntry withPlainBullets(ResumeEntry entry) {
		return entry.withBullets(entry.bullets().stream()
				.map(bullet -> bullet.stream().map(span -> new TextSpan(span.text(), false)).toList())
				.toList());
	}

	private List<List<String>> experienceBulletGroups(List<Section> sections) {
		return bulletTextExtractor.extractGroups(experienceSections(sections)).stream()
				.map(group -> group.stream().map(bulletTextExtractor::stripMarker).toList())
				.toList();
	}

	static boolean isExperienceSection(Section section) {
		return EXPERIENCE_SECTION_TITLES.contains(normalizedTitle(section));
	}

	private List<Section> experienceSections(List<Section> sections) {
		return sections.stream().filter(section -> EXPERIENCE_SECTION_TITLES.contains(normalizedTitle(section))).toList();
	}

	private String experienceText(List<Section> sections) {
		return experienceSections(sections).stream()
				.map(section -> section.title() + "\n" + section.content())
				.collect(Collectors.joining("\n"));
	}

	private void ensureProjectsWereKept(StructuredResume content, List<Section> originalSections) {
		boolean originalHasProjects = originalSections.stream()
				.anyMatch(section -> PROJECT_SECTION_TITLES.contains(normalizedTitle(section)) && !section.content().isBlank());
		boolean outputHasProjects = content.sections().stream()
				.anyMatch(section -> section.semanticType() == ResumeSectionSemanticType.PROJECTS && !section.entries().isEmpty());
		if (originalHasProjects && !outputHasProjects) {
			throw new DroppedContentException("a IA descartou a seção de projetos");
		}
	}

	static String normalizedTitle(Section section) {
		return TRAILING_DECORATION.matcher(NormalizedText.of(section.title())).replaceFirst("");
	}

	private void ensureNoBulletWasDropped(StructuredResume content, int originalBulletCount) {
		long outputBulletCount = content.sections().stream()
				.filter(section -> section.semanticType().holdsBullets())
				.flatMap(section -> section.entries().stream())
				.mapToLong(entry -> entry.bullets().size())
				.sum();
		if (outputBulletCount < originalBulletCount) {
			throw new DroppedContentException(
					"a IA descartou %d de %d itens de experiência".formatted(originalBulletCount - outputBulletCount,
							originalBulletCount));
		}
	}
}
