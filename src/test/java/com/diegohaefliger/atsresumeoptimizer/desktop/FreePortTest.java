package com.diegohaefliger.atsresumeoptimizer.desktop;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.InetAddress;
import java.net.ServerSocket;
import org.junit.jupiter.api.Test;

class FreePortTest {

	@Test
	void keepsThePreferredPortWhenItIsFree() throws Exception {
		int free;
		try (ServerSocket socket = new ServerSocket(0, 0, InetAddress.getLoopbackAddress())) {
			free = socket.getLocalPort();
		}

		assertThat(FreePort.preferring(free)).isEqualTo(free);
	}

	@Test
	void picksAnotherPortWhenThePreferredOneIsTaken() throws Exception {
		try (ServerSocket taken = new ServerSocket(0, 0, InetAddress.getLoopbackAddress())) {
			int port = FreePort.preferring(taken.getLocalPort());

			assertThat(port).isPositive().isNotEqualTo(taken.getLocalPort());
		}
	}
}
