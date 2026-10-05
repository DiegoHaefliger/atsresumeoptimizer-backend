package com.diegohaefliger.atsresumeoptimizer.parsing.application;

import java.io.InputStream;

interface TableDetector {

	boolean hasTable(InputStream input);
}
