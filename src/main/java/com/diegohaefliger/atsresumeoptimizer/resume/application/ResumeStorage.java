package com.diegohaefliger.atsresumeoptimizer.resume.application;

interface ResumeStorage {

	void upload(String key, byte[] content, String contentType);

	byte[] download(String key);

	void delete(String key);
}
