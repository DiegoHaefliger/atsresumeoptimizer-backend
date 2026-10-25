package com.diegohaefliger.atsresumeoptimizer.jobsearch.application;

import java.math.BigDecimal;

record SalaryRange(BigDecimal min, BigDecimal max) {

	static final SalaryRange NONE = new SalaryRange(null, null);
}
