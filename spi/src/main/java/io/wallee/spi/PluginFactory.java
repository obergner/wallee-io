package io.wallee.spi;

import com.typesafe.config.Config;

import java.util.function.Function;

/**
 *
 */
public interface PluginFactory<P> extends Function<Config, P> {
}
