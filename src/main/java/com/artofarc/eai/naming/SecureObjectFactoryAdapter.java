/*
 * Copyright 2021 Andre Karalus
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.artofarc.eai.naming;

import java.security.GeneralSecurityException;
import java.security.Key;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.Hashtable;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import javax.naming.Context;
import javax.naming.Name;
import javax.naming.RefAddr;
import javax.naming.Reference;
import javax.naming.StringRefAddr;
import javax.naming.spi.ObjectFactory;

/**
 * Delegates to factory2 while decrypting passwords.
 * More general solution for: https://github.com/AKSarav/SecureTomcatJDBC
 * Worth a read: https://cwiki.apache.org/confluence/display/TOMCAT/Password
 */
public class SecureObjectFactoryAdapter implements ObjectFactory {

	private static final String ALGORITHM = "AES";
	private static final String defaultSecretKey = "PHRASETOREPLACE";

	private final Key secretKeySpec;

	public SecureObjectFactoryAdapter() throws GeneralSecurityException {
		byte[] key = defaultSecretKey.getBytes();
		MessageDigest sha = MessageDigest.getInstance("SHA-1");
		key = sha.digest(key);
		key = Arrays.copyOf(key, 16); // use only the first 128 bit
		secretKeySpec = new SecretKeySpec(key, ALGORITHM);
	}

	private String decrypt(String encryptedString) throws Exception {
		Cipher cipher = Cipher.getInstance(ALGORITHM);
		cipher.init(Cipher.DECRYPT_MODE, secretKeySpec);
		byte[] original = cipher.doFinal(toByteArray(encryptedString));
		return new String(original, "UTF-8");
	}

	private static final byte[] toByteArray(String hexString) {
		int arrLength = hexString.length() >> 1;
		byte buf[] = new byte[arrLength];
		for (int i = 0; i < arrLength; i++) {
			int index = i << 1;
			String l_digit = hexString.substring(index, index + 2);
			buf[i] = (byte) Integer.parseInt(l_digit, 16);
		}
		return buf;
	}

	public static String encrypt(String plainText) throws Exception {
		SecureObjectFactoryAdapter secureObjectFactoryAdapter = new SecureObjectFactoryAdapter();
		Cipher cipher = Cipher.getInstance(ALGORITHM);
		cipher.init(Cipher.ENCRYPT_MODE, secureObjectFactoryAdapter.secretKeySpec);
		byte[] encrypted = cipher.doFinal(plainText.getBytes("UTF-8"));
		return asHexString(encrypted);
	}

	private final static String asHexString(byte buf[]) {
		StringBuilder strbuf = new StringBuilder(buf.length * 2);
		for (int i = 0; i < buf.length; i++) {
			if (((int) buf[i] & 0xff) < 0x10) {
				strbuf.append("0");
			}
			strbuf.append(Long.toString((int) buf[i] & 0xff, 16));
		}
		return strbuf.toString();
	}

	@Override
	public Object getObjectInstance(Object obj, Name name, Context nameCtx, Hashtable<?, ?> env) throws Exception {
		Reference ref = (Reference) obj;
		RefAddr refAddr = ref.get("factory2");
		if (refAddr == null) {
			throw new IllegalArgumentException("factory2 must be set");
		}
		Class<?> objectFactoryClass = Class.forName(refAddr.getContent().toString());
		ObjectFactory objectFactory = (ObjectFactory) objectFactoryClass.newInstance();
		Reference reference = new Reference(ref.getClassName());
		for (Enumeration<RefAddr> enumeration = ref.getAll(); enumeration.hasMoreElements();) {
			RefAddr element = enumeration.nextElement();
			String type = element.getType();
			if (type.contains("password")) {
				reference.add(new StringRefAddr(type, decrypt(element.getContent().toString())));
			} else {
				reference.add(element);
			}
		}
		return objectFactory.getObjectInstance(reference, name, nameCtx, env);
	}

	public static void main(String[] args) throws Exception {
		System.out.println("=> ENCRYPTED PASSWORD : " + SecureObjectFactoryAdapter.encrypt(args[0]));
	}

}
