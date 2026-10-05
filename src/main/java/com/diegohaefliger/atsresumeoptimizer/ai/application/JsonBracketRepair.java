package com.diegohaefliger.atsresumeoptimizer.ai.application;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Corrige o erro mais comum de JSON gerado por LLM: fechar um array/objeto aninhado com o
 * caractere errado ({@code }} no lugar de {@code ]}, ou vice-versa). Percorre o texto rastreando
 * o par de fechamento esperado a cada nível de aninhamento; quando o caractere de fechamento não
 * bate com o esperado, substitui pelo certo. Em JSON já válido não troca nada.
 */
final class JsonBracketRepair {

	private JsonBracketRepair() {
	}

	static String repair(String json) {
		char[] chars = json.toCharArray();
		Deque<Character> expectedClosers = new ArrayDeque<>();
		boolean inString = false;
		boolean escaped = false;
		for (int i = 0; i < chars.length; i++) {
			char current = chars[i];
			if (inString) {
				if (escaped) {
					escaped = false;
				} else if (current == '\\') {
					escaped = true;
				} else if (current == '"') {
					inString = false;
				}
				continue;
			}
			switch (current) {
				case '"' -> inString = true;
				case '{' -> expectedClosers.push('}');
				case '[' -> expectedClosers.push(']');
				case '}', ']' -> {
					if (!expectedClosers.isEmpty()) {
						char expected = expectedClosers.pop();
						if (current != expected) {
							chars[i] = expected;
						}
					}
				}
				default -> {
				}
			}
		}
		return new String(chars);
	}
}
