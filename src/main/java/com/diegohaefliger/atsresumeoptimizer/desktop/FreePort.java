package com.diegohaefliger.atsresumeoptimizer.desktop;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;

final class FreePort {

	private FreePort() {
	}

	static int preferring(int preferred) throws IOException {
		if (isFree(preferred)) {
			return preferred;
		}
		try (ServerSocket socket = new ServerSocket(0, 0, InetAddress.getLoopbackAddress())) {
			return socket.getLocalPort();
		}
	}

	private static boolean isFree(int port) {
		try (ServerSocket socket = new ServerSocket(port, 0, InetAddress.getLoopbackAddress())) {
			return true;
		} catch (IOException inUse) {
			return false;
		}
	}
}
