package com.diegohaefliger.atsresumeoptimizer.ai;

import java.util.List;
import java.util.Map;

/** Ferramenta de consulta oferecida à IA durante o chat; nunca altera dados. */
public interface ChatTool {

	String name();

	String description();

	/** Todos os argumentos são texto e obrigatórios. */
	List<Parameter> parameters();

	/** Frase curta para o usuário ver o que está sendo consultado. */
	String label(Map<String, String> arguments);

	String run(Map<String, String> arguments);

	record Parameter(String name, String description) {
	}
}
