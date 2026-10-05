package com.diegohaefliger.atsresumeoptimizer.rewrite.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.diegohaefliger.atsresumeoptimizer.ai.TextSpan;
import java.util.List;
import org.junit.jupiter.api.Test;

class RichTextTest {

	@Test
	void parsesBoldItalicAndUnderline() {
		List<TextSpan> spans = RichText.inline("a **b** *c* ++d++ e");

		assertThat(spans).containsExactly(
				new TextSpan("a ", false, false, false),
				new TextSpan("b", true, false, false),
				new TextSpan(" ", false, false, false),
				new TextSpan("c", false, true, false),
				new TextSpan(" ", false, false, false),
				new TextSpan("d", false, false, true),
				new TextSpan(" e", false, false, false));
	}

	@Test
	void nestsStyles() {
		assertThat(RichText.inline("**a *b* c**")).containsExactly(
				new TextSpan("a ", true, false, false),
				new TextSpan("b", true, true, false),
				new TextSpan(" c", true, false, false));
	}

	@Test
	void keepsLoneAsterisksAndPlusesAsText() {
		assertThat(TextSpan.plainText(RichText.inline("5 * 3 e C++ e 2 ** 4"))).isEqualTo("5 * 3 e C++ e 2 ** 4");
	}

	@Test
	void detectsBulletAndNumberedLists() {
		List<RichText.Line> lines = RichText.fromMarkup("Intro\n- um\n2. dois\n\nfim");

		assertThat(lines).extracting(RichText.Line::marker).containsExactly("", "• ", "2. ", "", "");
		assertThat(lines.get(1).spans()).containsExactly(new TextSpan("um", false));
		assertThat(lines.get(3).isBlank()).isTrue();
	}

	@Test
	void detectsTheListMarkerInTheFirstSpanOfAStoredLine() {
		RichText.Line line = RichText.fromSpans(List.of(new TextSpan("- ", false), new TextSpan("item", true)));

		assertThat(line.marker()).isEqualTo("• ");
		assertThat(line.spans().get(1)).isEqualTo(new TextSpan("item", true));
	}
}
