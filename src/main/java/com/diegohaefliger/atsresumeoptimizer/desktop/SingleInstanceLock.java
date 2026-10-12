package com.diegohaefliger.atsresumeoptimizer.desktop;

import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.channels.OverlappingFileLockException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Optional;

final class SingleInstanceLock {

	private static final String LOCK_FILE = "app.lock";
	private static final String PORT_FILE = "app.port";

	// Guardado só pra manter o lock vivo até a JVM sair; o sistema operacional solta se o processo cair.
	private final FileLock lock;
	private final Path portFile;

	private SingleInstanceLock(FileLock lock, Path portFile) {
		this.lock = lock;
		this.portFile = portFile;
	}

	static Optional<SingleInstanceLock> tryAcquire(Path dataDir) throws IOException {
		Files.createDirectories(dataDir);
		FileChannel channel = FileChannel.open(dataDir.resolve(LOCK_FILE), StandardOpenOption.CREATE, StandardOpenOption.WRITE);
		FileLock lock;
		try {
			lock = channel.tryLock();
		} catch (OverlappingFileLockException heldByThisJvm) {
			// O devtools roda o main de novo no mesmo processo a cada restart: continua sendo a mesma instância.
			channel.close();
			lock = null;
		}
		if (lock == null && channel.isOpen()) {
			channel.close();
			return Optional.empty();
		}
		Path portFile = dataDir.resolve(PORT_FILE);
		Files.deleteIfExists(portFile);
		return Optional.of(new SingleInstanceLock(lock, portFile));
	}

	void publishPort(int port) throws IOException {
		Files.writeString(portFile, Integer.toString(port), StandardCharsets.UTF_8);
	}

	void withdrawPort() throws IOException {
		Files.deleteIfExists(portFile);
	}

	static Optional<Integer> publishedPort(Path dataDir) throws IOException {
		Path portFile = dataDir.resolve(PORT_FILE);
		if (!Files.isRegularFile(portFile)) {
			return Optional.empty();
		}
		String content = Files.readString(portFile, StandardCharsets.UTF_8).strip();
		return content.isEmpty() ? Optional.empty() : Optional.of(Integer.parseInt(content));
	}
}
