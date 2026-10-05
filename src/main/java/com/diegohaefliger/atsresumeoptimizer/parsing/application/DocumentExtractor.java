package com.diegohaefliger.atsresumeoptimizer.parsing.application;

import com.diegohaefliger.atsresumeoptimizer.parsing.domain.NormalizedDocument;
import java.io.InputStream;

interface DocumentExtractor {

	NormalizedDocument extract(InputStream input);
}
