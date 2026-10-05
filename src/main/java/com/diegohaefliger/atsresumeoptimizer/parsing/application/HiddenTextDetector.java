package com.diegohaefliger.atsresumeoptimizer.parsing.application;

import java.io.InputStream;

interface HiddenTextDetector {

	boolean hasHiddenText(InputStream input);
}
