package io.wallee.shared.plugin.auth.noop;

import com.typesafe.config.Config;
import io.wallee.spi.auth.AuthenticationPlugin;
import io.wallee.spi.auth.AuthenticationPluginFactory;

/**
 * A {@link io.wallee.spi.PluginFactory PluginFactory} for {@link NoopAuthenticationPlugin}s
 */
public class NoopAuthenticationPluginFactory implements AuthenticationPluginFactory {

    private final NoopAuthenticationPlugin instance = new NoopAuthenticationPlugin();

    @Override
    public AuthenticationPlugin apply(final Config config) {
        return instance;
    }
}
