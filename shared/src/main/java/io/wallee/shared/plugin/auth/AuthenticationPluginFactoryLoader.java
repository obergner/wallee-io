/*
 * Copyright 2015 Olaf Bergner
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

package io.wallee.shared.plugin.auth;

import io.wallee.shared.plugin.PluginFactoryLoader;
import io.wallee.shared.plugin.auth.noop.NoopAuthenticationPluginFactory;
import io.wallee.spi.auth.AuthenticationPlugin;
import io.wallee.spi.auth.AuthenticationPluginFactory;

/**
 *
 */
public class AuthenticationPluginFactoryLoader extends PluginFactoryLoader<AuthenticationPlugin, AuthenticationPluginFactory> {

    public AuthenticationPluginFactoryLoader(final ClassLoader cl) {
        super(AuthenticationPluginFactory.class, cl, new NoopAuthenticationPluginFactory());
    }
}
