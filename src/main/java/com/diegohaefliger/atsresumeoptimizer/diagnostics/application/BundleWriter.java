package com.diegohaefliger.atsresumeoptimizer.diagnostics.application;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.springframework.stereotype.Component;

@Component
class BundleWriter {

	byte[] zip(Map<String, String> files) {
		ByteArrayOutputStream bytes = new ByteArrayOutputStream();
		try (ZipOutputStream zip = new ZipOutputStream(bytes, StandardCharsets.UTF_8)) {
			for (Map.Entry<String, String> file : files.entrySet()) {
				zip.putNextEntry(new ZipEntry(file.getKey()));
				zip.write(file.getValue().getBytes(StandardCharsets.UTF_8));
				zip.closeEntry();
			}
		} catch (IOException exception) {
			throw new UncheckedIOException(exception);
		}
		return bytes.toByteArray();
	}
}
