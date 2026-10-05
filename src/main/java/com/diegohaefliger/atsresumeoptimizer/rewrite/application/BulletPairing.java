package com.diegohaefliger.atsresumeoptimizer.rewrite.application;

import com.diegohaefliger.atsresumeoptimizer.NormalizedText;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/** Pareia por sobreposição de palavras, não por posição: com destaque de vaga a IA reordena bullets. */
final class BulletPairing {

	private static final int MIN_WORD_LENGTH = 3;
	private static final Pattern WORD_SEPARATOR = Pattern.compile("[^\\p{Alnum}]+");

	record Pair(String original, String rewritten) {
	}

	List<Pair> pair(List<String> originals, List<String> finals) {
		List<Set<String>> originalWords = originals.stream().map(this::words).toList();
		List<Set<String>> finalWords = finals.stream().map(this::words).toList();
		Integer[] match = new Integer[originals.size()];
		boolean[] used = new boolean[finals.size()];

		List<int[]> candidates = new ArrayList<>();
		for (int i = 0; i < originals.size(); i++) {
			for (int j = 0; j < finals.size(); j++) {
				if (overlap(originalWords.get(i), finalWords.get(j)) > 0) {
					candidates.add(new int[] {i, j});
				}
			}
		}
		candidates.sort((a, b) -> Double.compare(
				overlap(originalWords.get(b[0]), finalWords.get(b[1])),
				overlap(originalWords.get(a[0]), finalWords.get(a[1]))));
		for (int[] candidate : candidates) {
			if (match[candidate[0]] == null && !used[candidate[1]]) {
				match[candidate[0]] = candidate[1];
				used[candidate[1]] = true;
			}
		}

		int nextFree = 0;
		List<Pair> pairs = new ArrayList<>();
		for (int i = 0; i < originals.size(); i++) {
			if (match[i] == null) {
				while (nextFree < finals.size() && used[nextFree]) {
					nextFree++;
				}
				if (nextFree == finals.size()) {
					continue;
				}
				match[i] = nextFree;
				used[nextFree] = true;
			}
			pairs.add(new Pair(originals.get(i), finals.get(match[i])));
		}
		return pairs;
	}

	Optional<String> closestOriginal(String rewritten, List<String> originals) {
		Set<String> rewrittenWords = words(rewritten);
		return originals.stream()
				.filter(original -> overlap(words(original), rewrittenWords) > 0)
				.max(Comparator.comparingDouble(original -> overlap(words(original), rewrittenWords)));
	}

	double coverage(String original, String rewritten) {
		Set<String> originalWords = words(original);
		if (originalWords.isEmpty()) {
			return 0;
		}
		Set<String> kept = new HashSet<>(originalWords);
		kept.retainAll(words(rewritten));
		return (double) kept.size() / originalWords.size();
	}

	private double overlap(Set<String> left, Set<String> right) {
		if (left.isEmpty() || right.isEmpty()) {
			return 0;
		}
		Set<String> intersection = new HashSet<>(left);
		intersection.retainAll(right);
		Set<String> union = new HashSet<>(left);
		union.addAll(right);
		return (double) intersection.size() / union.size();
	}

	private Set<String> words(String text) {
		return Arrays.stream(WORD_SEPARATOR.split(NormalizedText.of(text)))
				.filter(word -> word.length() >= MIN_WORD_LENGTH)
				.collect(Collectors.toSet());
	}
}
