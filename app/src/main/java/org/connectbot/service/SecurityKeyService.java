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

package org.connectbot.service;

import org.connectbot.SecurityKeySignatureProxy;
import org.connectbot.SecurityKeyActivity;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import androidx.annotation.Nullable;
import de.cotech.hw.SecurityKeyAuthenticator;
import de.cotech.hw.ui.SecurityKeyDialogInterface;

/**
 * This service is used to share data between SecurityKeySignatureProxy and SecurityKeyActivity
 */
public class SecurityKeyService extends Service {
	private static final String TAG = "CB.SKService";

	SecurityKeySignatureProxy mSignatureProxy;

	public class SecurityKeyServiceBinder extends Binder {
		public SecurityKeyService getService() {
			return SecurityKeyService.this;
		}
	}

	private final IBinder mSecurityKeyServiceBinder = new SecurityKeyServiceBinder();

	@Nullable
	@Override
	public IBinder onBind(Intent intent) {
		return mSecurityKeyServiceBinder;
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		/*
		 * The lifecycle of this service is bound to the lifecycle of TerminalManager, since
		 * authentication might need to occur in the background if connectivity is temporarily
		 * lost, so this service needs to run as long as there are TerminalBridges active in
		 * TerminalManager
		 */
		return START_STICKY;
	}

	public void startActivity(String pubKeyNickname) {
		Intent intent = new Intent(this, SecurityKeyActivity.class);
		intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		intent.putExtra(SecurityKeyActivity.EXTRA_PUBKEY_NICKNAME, pubKeyNickname);
		startActivity(intent);
	}

	public void setSignatureProxy(SecurityKeySignatureProxy signatureProxy) {
		mSignatureProxy = signatureProxy;
	}

	public void setAuthenticator(SecurityKeyDialogInterface dialogInterface, SecurityKeyAuthenticator securityKeyAuthenticator) {
		mSignatureProxy.setAuthenticator(dialogInterface, securityKeyAuthenticator);
	}

	public void cancel() {
		mSignatureProxy.cancel();
	}

}

