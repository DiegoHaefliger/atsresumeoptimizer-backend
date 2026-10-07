package com.diegohaefliger.atsresumeoptimizer.ai;

public interface SecretCipher {

	String encrypt(String plainText);

	String decrypt(String encoded);
}
