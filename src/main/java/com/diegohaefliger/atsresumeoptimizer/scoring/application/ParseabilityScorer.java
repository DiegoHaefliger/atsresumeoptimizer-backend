package com.diegohaefliger.atsresumeoptimizer.scoring.application;

import com.diegohaefliger.atsresumeoptimizer.parsing.domain.ParsingSignals;
import com.diegohaefliger.atsresumeoptimizer.scoring.domain.Dimension;
import com.diegohaefliger.atsresumeoptimizer.scoring.domain.DimensionResult;
import com.diegohaefliger.atsresumeoptimizer.scoring.domain.DimensionScorer;
import com.diegohaefliger.atsresumeoptimizer.scoring.domain.Finding;
import com.diegohaefliger.atsresumeoptimizer.scoring.domain.FindingCode;
import com.diegohaefliger.atsresumeoptimizer.scoring.domain.ScoringContext;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
class ParseabilityScorer implements DimensionScorer {

	private static final int MAX_SCORE = 100;

	@Override
	public Dimension dimension() {
		return Dimension.PARSEABILITY;
	}

	@Override
	public DimensionResult score(ScoringContext context) {
		ParsingSignals signals = context.parsing().signals();
		List<Finding> findings = new ArrayList<>();

		addIfPresent(findings, signals.noTextLayer(), FindingCode.NO_TEXT_LAYER,
				"O PDF não tem camada de texto (parece escaneado ou só imagem).",
				"Exporte o currículo como PDF gerado a partir de texto, não como imagem escaneada.");
		addIfPresent(findings, signals.multiColumn(), FindingCode.LAYOUT_MULTI_COLUMN,
				"Currículo em múltiplas colunas: o texto extraído pode misturar seções.",
				"Use layout de coluna única.");
		addIfPresent(findings, signals.hasTable(), FindingCode.LAYOUT_TABLE,
				"Currículo usa tabela para organizar conteúdo.",
				"Substitua tabelas por texto corrido com marcadores.");
		addIfPresent(findings, signals.contactInHeaderOrFooter(), FindingCode.CONTACT_IN_HEADER,
				"Dados de contato estão no cabeçalho ou rodapé.",
				"Coloque email e telefone no corpo principal do documento.");
		addIfPresent(findings, signals.hasTextBox(), FindingCode.LAYOUT_TEXTBOX,
				"Currículo usa caixas de texto fora do fluxo principal.",
				"Coloque o conteúdo direto no corpo do documento, sem caixas de texto.");
		addIfPresent(findings, signals.hasIconGlyphsWithoutUnicode(), FindingCode.ICON_WITHOUT_UNICODE,
				"Ícones sem equivalente Unicode (fonte de ícone ou área privada) — o ATS lê como caractere estranho.",
				"Troque ícones decorativos por texto simples.");
		addIfPresent(findings, signals.hasHiddenText(), FindingCode.HIDDEN_TEXT,
				"Foi detectado texto oculto no documento.",
				"Remova texto oculto ou com cor igual ao fundo — alguns ATS penalizam isso.");
		addIfPresent(findings, signals.hasInconsistentFonts(), FindingCode.FONT_INCONSISTENT,
				"Muitas famílias de fonte diferentes ou corpo de texto pequeno demais.",
				"Use no máximo 2-3 fontes e corpo de texto de pelo menos 10pt.");

		int score = MAX_SCORE - findings.stream().mapToInt(finding -> severityPenalty(finding.code())).sum();
		return DimensionResult.of(Dimension.PARSEABILITY, Math.max(0, score), findings);
	}

	private void addIfPresent(List<Finding> findings, boolean present, FindingCode code, String message, String suggestion) {
		if (present) {
			findings.add(Finding.of(code, message, suggestion));
		}
	}

	private int severityPenalty(FindingCode code) {
		return switch (code.defaultSeverity()) {
			case CRITICAL -> 60;
			case HIGH -> 25;
			case MEDIUM -> 15;
			case LOW -> 5;
		};
	}
}
