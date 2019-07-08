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

import android.app.Application;
import de.cotech.hw.SecurityKeyManager;
import de.cotech.hw.SecurityKeyManagerConfig;

public class ConnectBotApplication extends Application {

	@Override
	public void onCreate() {
		super.onCreate();

		SecurityKeyManager securityKeyManager = SecurityKeyManager.getInstance();
		SecurityKeyManagerConfig config = new SecurityKeyManagerConfig.Builder()
				.setEnableDebugLogging(BuildConfig.DEBUG)
				.build();

		securityKeyManager.init(this, config);
	}
}
