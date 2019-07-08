/*
 * ConnectBot: simple, powerful, open-source SSH client for Android
 * Copyright 2019 Dominik Schürmann <dominik@cotech.de>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.connectbot;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.interfaces.ECPublicKey;
import java.security.interfaces.RSAPublicKey;
import java.util.concurrent.CountDownLatch;

import org.connectbot.service.SecurityKeyService;

import com.trilead.ssh2.auth.SignatureProxy;
import com.trilead.ssh2.signature.ECDSASHA2Verify;
import com.trilead.ssh2.signature.Ed25519Verify;
import com.trilead.ssh2.signature.RSASHA1Verify;
import com.trilead.ssh2.signature.RSASHA256Verify;
import com.trilead.ssh2.signature.RSASHA512Verify;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import androidx.annotation.WorkerThread;
import de.cotech.hw.SecurityKeyAuthenticator;
import de.cotech.hw.ui.SecurityKeyDialogInterface;
import net.i2p.crypto.eddsa.EdDSAPublicKey;


public class SecurityKeySignatureProxy extends SignatureProxy {
	private static final String TAG = "CB.SKSignatureProxy";

	private CountDownLatch mResultReadyLatch;

	private SecurityKeyAuthenticator mSecurityKeyAuthenticator;
	private SecurityKeyDialogInterface dialogInterface;

	private SecurityKeyService mSecurityKeyService = null;
	private boolean cancelled;

	public SecurityKeySignatureProxy(PublicKey publicKey, String pubkeyNickname, Context appContext) {
		super(publicKey);

		mResultReadyLatch = new CountDownLatch(1);

		ServiceConnection mSecurityKeyServiceConnection = new ServiceConnection() {
			@Override
			public void onServiceConnected(ComponentName className, IBinder service) {
				mSecurityKeyService = ((SecurityKeyService.SecurityKeyServiceBinder) service).getService();
				mSecurityKeyService.setSignatureProxy(SecurityKeySignatureProxy.this);
				mSecurityKeyService.startActivity(pubkeyNickname);
			}

			@Override
			public void onServiceDisconnected(ComponentName className) {
				mSecurityKeyService = null;
			}
		};
		appContext.bindService(new Intent(appContext, SecurityKeyService.class), mSecurityKeyServiceConnection,
				Context.BIND_AUTO_CREATE);
	}

	public void setAuthenticator(SecurityKeyDialogInterface dialogInterface, SecurityKeyAuthenticator securityKeyAuthenticator) {
		this.mSecurityKeyAuthenticator = securityKeyAuthenticator;
		this.dialogInterface = dialogInterface;
		mResultReadyLatch.countDown();
	}

	public void cancel() {
		cancelled = true;
		mResultReadyLatch.countDown();
	}

	@Override
	public byte[] sign(final byte[] challenge, final String hashAlgorithm) throws IOException {
		while (true) {
			waitForSecurityKey();
			if (cancelled) {
				throw new IOException("Cancelled!");
			}
			byte[] signature = tryAuthOperation(challenge, hashAlgorithm);
			if (signature != null) {
				return signature;
			}
		}
	}

	@WorkerThread
	private byte[] tryAuthOperation(byte[] challenge, String hashAlgorithm) throws IOException {
		try {
			byte[] ds = mSecurityKeyAuthenticator.authenticateWithDigest(challenge, hashAlgorithm);
			byte[] encodedSignature = encodeSignature(ds, hashAlgorithm);

			dialogInterface.dismiss();
			return encodedSignature;
		} catch (IOException e) {
			dialogInterface.postError(e);
			return null;

		} catch (NoSuchAlgorithmException e) {
			throw new IOException("NoSuchAlgorithmException");
		}
	}

	private void waitForSecurityKey() {
		try {
			mResultReadyLatch.await();
			mResultReadyLatch = new CountDownLatch(1);
		} catch (InterruptedException e) {
			throw new RuntimeException("Error waitForSecurityKey(): interrupted");
		}
	}

	/**
	 * Based on methods from AuthenticationManager in sshlib
	 */
	private byte[] encodeSignature(byte[] ds, String hashAlgorithm) throws IOException {
		PublicKey publicKey = getPublicKey();
		if (publicKey instanceof RSAPublicKey) {
			switch (hashAlgorithm) {
			case SHA512: {
				return RSASHA512Verify.encodeRSASHA512Signature(ds);
			}
			case SHA256: {
				return RSASHA256Verify.encodeRSASHA256Signature(ds);
			}
			case SHA1: {
				return RSASHA1Verify.encodeSSHRSASignature(ds);
			}
			default:
				throw new IOException("Unsupported algorithm in SecurityKeySignatureProxy!");
			}
		} else if (publicKey instanceof ECPublicKey) {
			ECPublicKey ecPublicKey = (ECPublicKey) publicKey;
			return ECDSASHA2Verify.encodeSSHECDSASignature(ds, ecPublicKey.getParams());
		} else if (publicKey instanceof EdDSAPublicKey) {
			return Ed25519Verify.encodeSSHEd25519Signature(ds);
		} else {
			throw new IOException("Unsupported algorithm in SecurityKeySignatureProxy!");
		}
	}
}