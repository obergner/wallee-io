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
