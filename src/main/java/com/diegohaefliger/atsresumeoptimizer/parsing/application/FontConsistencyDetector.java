package com.diegohaefliger.atsresumeoptimizer.parsing.application;

import java.io.InputStream;

interface FontConsistencyDetector {

	boolean hasInconsistentFonts(InputStream input);
}
