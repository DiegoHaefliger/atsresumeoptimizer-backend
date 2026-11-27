package com.diegohaefliger.atsresumeoptimizer.jobsearch.application;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import java.text.Normalizer;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import org.springframework.data.jpa.domain.Specification;
import com.diegohaefliger.atsresumeoptimizer.jobsearch.domain.SuggestionQuery;
import com.diegohaefliger.atsresumeoptimizer.jobsearch.domain.SuggestionStatus;

final class SuggestionSpecification {

	// translate() em vez de unaccent: a extensão não existe no Postgres embutido do desktop.
	private static final String ACCENTED = "áàâãäéèêëíìîïóòôõöúùûüçñÁÀÂÃÄÉÈÊËÍÌÎÏÓÒÔÕÖÚÙÛÜÇÑ";
	private static final String PLAIN = "aaaaaeeeeiiiiooooouuuucnAAAAAEEEEIIIIOOOOOUUUUCN";
	private static final char LIKE_ESCAPE = '\\';
	private static final String SEPARATOR = " ";

	private SuggestionSpecification() {
	}

	static Specification<SuggestionEntity> matching(SuggestionStatus status, SuggestionQuery query, LocalDate today) {
		return (root, criteria, builder) -> {
			List<Predicate> predicates = new ArrayList<>();
			predicates.add(builder.equal(root.get("status"), status));
			if (hasText(query.source())) {
				predicates.add(builder.equal(root.get("source"), query.source()));
			}
			if (query.minScore() != null && query.minScore() > 0) {
				predicates.add(builder.greaterThanOrEqualTo(root.get("score"), query.minScore()));
			}
			if (query.postedWithinDays() != null) {
				predicates.add(builder.greaterThanOrEqualTo(root.get("postedOn"), today.minusDays(query.postedWithinDays())));
			}
			Expression<String> haystack = normalizedHaystack(root, builder);
			words(query.text()).forEach(word -> predicates.add(builder.like(haystack, "%" + escaped(word) + "%", LIKE_ESCAPE)));
			return builder.and(predicates.toArray(Predicate[]::new));
		};
	}

	private static Expression<String> normalizedHaystack(Root<SuggestionEntity> root, CriteriaBuilder builder) {
		Expression<String> text = builder.literal("");
		for (String field : List.of("title", "company", "location", "description")) {
			text = builder.concat(builder.concat(text, SEPARATOR), builder.coalesce(root.get(field), ""));
		}
		Expression<String> plain = builder.function("translate", String.class, text, builder.literal(ACCENTED),
				builder.literal(PLAIN));
		return builder.lower(plain);
	}

	private static List<String> words(String text) {
		if (!hasText(text)) {
			return List.of();
		}
		String plain = Normalizer.normalize(text, Normalizer.Form.NFD).replaceAll("\\p{M}", "").toLowerCase(Locale.ROOT);
		return Arrays.stream(plain.trim().split("\\s+")).toList();
	}

	private static String escaped(String word) {
		return word.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
	}

	private static boolean hasText(String value) {
		return value != null && !value.isBlank();
	}
}
