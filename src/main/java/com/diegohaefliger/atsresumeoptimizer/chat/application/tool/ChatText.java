package com.diegohaefliger.atsresumeoptimizer.chat.application.tool;

final class ChatText {

	private static final String CUT_MARK = "\n[...texto cortado]";

	private ChatText() {
	}

	static String limit(String text, int maxChars) {
		if (text == null) {
			return "";
		}
		return text.length() <= maxChars ? text : text.substring(0, maxChars) + CUT_MARK;
	}
}
