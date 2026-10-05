package com.diegohaefliger.atsresumeoptimizer.ai.application;

import com.diegohaefliger.atsresumeoptimizer.Sha256;
import com.diegohaefliger.atsresumeoptimizer.ai.domain.AiSettingsSecretMissingException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.stereotype.Component;

@Component
class ApiKeyCipher {

	private static final String KEY_ALGORITHM = "AES";
	private static final String TRANSFORMATION = "AES/GCM/NoPadding";
	private static final int IV_BYTES = 12;
	private static final int TAG_BITS = 128;

	private final SecureRandom random = new SecureRandom();
	private final SettingsSecretStore secretStore;

	ApiKeyCipher(SettingsSecretStore secretStore) {
		this.secretStore = secretStore;
	}

	String encrypt(String plainText) {
		SecretKeySpec key = key(secretStore.currentOrCreate());
		try {
			byte[] iv = new byte[IV_BYTES];
			random.nextBytes(iv);
			Cipher cipher = Cipher.getInstance(TRANSFORMATION);
			cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(TAG_BITS, iv));
			byte[] encrypted = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));
			return Base64.getEncoder().encodeToString(ByteBuffer.allocate(iv.length + encrypted.length).put(iv).put(encrypted).array());
		} catch (GeneralSecurityException exception) {
			throw new IllegalStateException("Falha ao criptografar a chave da IA", exception);
		}
	}

	String decrypt(String encoded) {
		SecretKeySpec key = key(secretStore.current().orElseThrow(AiSettingsSecretMissingException::new));
		try {
			ByteBuffer buffer = ByteBuffer.wrap(Base64.getDecoder().decode(encoded));
			byte[] iv = new byte[IV_BYTES];
			buffer.get(iv);
			byte[] encrypted = new byte[buffer.remaining()];
			buffer.get(encrypted);
			Cipher cipher = Cipher.getInstance(TRANSFORMATION);
			cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(TAG_BITS, iv));
			return new String(cipher.doFinal(encrypted), StandardCharsets.UTF_8);
		} catch (GeneralSecurityException exception) {
			throw new IllegalStateException("Chave da IA salva não abre com o segredo atual (AI_SETTINGS_SECRET ou arquivo)", exception);
		}
	}

	private static SecretKeySpec key(String secret) {
		return new SecretKeySpec(Sha256.digest(secret), KEY_ALGORITHM);
	}
}
