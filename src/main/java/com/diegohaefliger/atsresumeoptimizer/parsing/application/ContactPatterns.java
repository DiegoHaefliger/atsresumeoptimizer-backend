package com.diegohaefliger.atsresumeoptimizer.parsing.application;

import java.util.regex.Pattern;

final class ContactPatterns {

	static final Pattern EMAIL = Pattern.compile("[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}");
	static final Pattern PHONE_BR =
			Pattern.compile("(?:\\+55\\s?)?\\(?\\d{2}\\)?[\\s.-]?9?[\\s.-]?\\d{4}[\\s.-]?\\d{4}");
	static final Pattern LINKEDIN = Pattern.compile("(?:https?://)?(?:www\\.)?linkedin\\.com/in/[A-Za-z0-9\\-_%]+", Pattern.CASE_INSENSITIVE);
	static final Pattern GITHUB = Pattern.compile("(?:https?://)?(?:www\\.)?github\\.com/[A-Za-z0-9\\-_%]+", Pattern.CASE_INSENSITIVE);
	static final Pattern LOCATION = Pattern.compile(
			"\\b([A-ZÀ-Ú][a-zà-ú]+(?:[ \\t]+(?:d[aeo]s?[ \\t]+)?[A-ZÀ-Ú][a-zà-ú]+)*),[ \\t]*"
					+ "(AC|AL|AP|AM|BA|CE|DF|ES|GO|MA|MT|MS|MG|PA|PB|PR|PE|PI|RJ|RN|RS|RO|RR|SC|SP|SE|TO)\\b");
	/** Domínio sem esquema nem "www." é ambíguo demais pra extrair sem falso positivo. */
	static final Pattern PORTFOLIO = Pattern.compile("(?:https?://[^\\s|]+|www\\.[^\\s|]+)", Pattern.CASE_INSENSITIVE);

	private ContactPatterns() {
	}

	static boolean containsContactInfo(String text) {
		return EMAIL.matcher(text).find() || PHONE_BR.matcher(text).find();
	}
}
