package com.diegohaefliger.atsresumeoptimizer.resume.adapter.web;

import com.diegohaefliger.atsresumeoptimizer.resume.ResumeService;
import com.diegohaefliger.atsresumeoptimizer.resume.ResumeSummary;
import com.diegohaefliger.atsresumeoptimizer.resume.ResumeVersionCreated;
import com.diegohaefliger.atsresumeoptimizer.resume.ResumeVersionInfo;
import com.diegohaefliger.atsresumeoptimizer.resume.ResumeVersionSummary;
import java.io.IOException;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/resumes")
class ResumesController {

	private final ResumeService resumeService;

	ResumesController(ResumeService resumeService) {
		this.resumeService = resumeService;
	}

	@PostMapping(consumes = "multipart/form-data")
	ResponseEntity<ResumeVersionCreated> create(@RequestParam("file") MultipartFile file,
			@RequestParam(value = "title", required = false) String title) throws IOException {
		ResumeVersionCreated created =
				resumeService.upload(file.getBytes(), file.getOriginalFilename(), file.getContentType(), title);
		return ResponseEntity.status(HttpStatus.CREATED).body(created);
	}

	@GetMapping
	List<ResumeSummary> list() {
		return resumeService.listResumes();
	}

	@GetMapping("/{resumeId}/versions")
	List<ResumeVersionSummary> versions(@PathVariable UUID resumeId) {
		return resumeService.listVersions(resumeId);
	}

	@GetMapping("/{resumeId}/versions/{versionId}/download")
	ResponseEntity<byte[]> download(@PathVariable UUID resumeId, @PathVariable UUID versionId) {
		ResumeVersionInfo info = resumeService.getVersionInfo(resumeId, versionId);
		return ResponseEntity.ok()
				.contentType(MediaType.parseMediaType(info.mimeType()))
				.header(HttpHeaders.CONTENT_DISPOSITION,
						ContentDisposition.attachment().filename(info.fileName()).build().toString())
				.body(info.content());
	}

	@PutMapping("/{resumeId}/favorite")
	ResponseEntity<Void> markFavorite(@PathVariable UUID resumeId) {
		resumeService.setFavorite(resumeId, true);
		return ResponseEntity.noContent().build();
	}

	@DeleteMapping("/{resumeId}/favorite")
	ResponseEntity<Void> unmarkFavorite(@PathVariable UUID resumeId) {
		resumeService.setFavorite(resumeId, false);
		return ResponseEntity.noContent().build();
	}

	@DeleteMapping("/{resumeId}")
	ResponseEntity<Void> delete(@PathVariable UUID resumeId) {
		resumeService.delete(resumeId);
		return ResponseEntity.noContent().build();
	}

	@DeleteMapping("/{resumeId}/versions/{versionId}")
	ResponseEntity<Void> deleteVersion(@PathVariable UUID resumeId, @PathVariable UUID versionId) {
		resumeService.deleteVersion(resumeId, versionId);
		return ResponseEntity.noContent().build();
	}
}
