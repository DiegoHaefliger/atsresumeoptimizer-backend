package com.diegohaefliger.atsresumeoptimizer.parsing.application;

import java.io.InputStream;

interface HeaderFooterContactDetector {

	boolean hasContactInHeaderOrFooter(InputStream input);
}
