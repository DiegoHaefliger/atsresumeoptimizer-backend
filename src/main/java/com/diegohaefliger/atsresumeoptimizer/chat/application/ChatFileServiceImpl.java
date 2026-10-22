package com.diegohaefliger.atsresumeoptimizer.chat.application;

import com.diegohaefliger.atsresumeoptimizer.chat.domain.ChatFileContent;
import com.diegohaefliger.atsresumeoptimizer.chat.domain.ChatFileKind;
import com.diegohaefliger.atsresumeoptimizer.chat.domain.UnknownChatFileException;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class ChatFileServiceImpl implements ChatFileService {

	private final ChatFileRepository files;

	ChatFileServiceImpl(ChatFileRepository files) {
		this.files = files;
	}

	@Override
	@Transactional(readOnly = true)
	public ChatFileContent image(UUID fileId) {
		return files.findById(fileId)
				.filter(file -> file.getKind() == ChatFileKind.IMAGE && file.getContent() != null)
				.map(file -> new ChatFileContent(file.getMimeType(), file.getFileName(), file.getContent()))
				.orElseThrow(() -> new UnknownChatFileException(fileId));
	}
}
