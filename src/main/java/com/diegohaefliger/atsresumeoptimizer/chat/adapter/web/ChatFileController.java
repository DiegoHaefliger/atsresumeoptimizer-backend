package com.diegohaefliger.atsresumeoptimizer.chat.adapter.web;

import com.diegohaefliger.atsresumeoptimizer.chat.application.ChatFileService;
import com.diegohaefliger.atsresumeoptimizer.chat.domain.ChatFileContent;
import java.time.Duration;
import java.util.UUID;
import org.springframework.http.CacheControl;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/chat/files")
class ChatFileController {

	private final ChatFileService service;

	ChatFileController(ChatFileService service) {
		this.service = service;
	}

	@GetMapping("/{fileId}")
	ResponseEntity<byte[]> image(@PathVariable UUID fileId) {
		ChatFileContent file = service.image(fileId);
		return ResponseEntity.ok()
				.contentType(MediaType.parseMediaType(file.mimeType()))
				.header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.inline().filename(file.fileName()).build().toString())
				.header("X-Content-Type-Options", "nosniff")
				.cacheControl(CacheControl.maxAge(Duration.ofDays(1)).cachePrivate())
				.body(file.content());
	}
}
