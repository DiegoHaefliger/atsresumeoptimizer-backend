package com.diegohaefliger.atsresumeoptimizer.chat.application.tool;

import com.diegohaefliger.atsresumeoptimizer.job.JobListing;
import com.diegohaefliger.atsresumeoptimizer.job.JobOffer;
import com.diegohaefliger.atsresumeoptimizer.job.JobStructuringService;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

@Component
class ChatJobLookup {

	private static final Pattern CODE_FORMAT = Pattern.compile("^[v#]?(\\d{1,18})$", Pattern.CASE_INSENSITIVE);
	private static final String CODE_PREFIX = "V";
	private static final int CODE_DIGITS = 4;

	private final JobStructuringService jobService;

	ChatJobLookup(JobStructuringService jobService) {
		this.jobService = jobService;
	}

	List<JobOffer> all() {
		return jobService.listJobs().stream().map(JobListing::offer).toList();
	}

	/** Aceita V0012, v12, #12 ou 12: o usuário escreve o código do jeito que o app mostra. */
	Optional<JobOffer> byCode(String code) {
		Matcher matcher = CODE_FORMAT.matcher(code.strip());
		if (!matcher.matches()) {
			return Optional.empty();
		}
		long number = Long.parseLong(matcher.group(1));
		return all().stream().filter(offer -> offer.code() != null && offer.code() == number).findFirst();
	}

	static String display(Long code) {
		return CODE_PREFIX + String.format("%0" + CODE_DIGITS + "d", code);
	}
}
