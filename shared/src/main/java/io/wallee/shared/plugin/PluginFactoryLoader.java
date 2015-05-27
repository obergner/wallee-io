package io.wallee.shared.plugin;

import io.wallee.spi.PluginFactory;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.ServiceLoader;

/**
 *
 */
public class PluginFactoryLoader<P, F extends PluginFactory<P>> {

    protected final Logger log = LogManager.getLogger(getClass());

    private final Class<F> pluginFactoryType;

    private final ClassLoader classLoader;

    private final F defaultPluginFactory;

    public PluginFactoryLoader(final Class<F> pluginFactoryType, final ClassLoader cl, final F defaultPluginFactory) {
        if (pluginFactoryType == null) {
            throw new IllegalArgumentException("Argument 'pluginFactoryType' must not be null");
        }
        if (cl == null) {
            throw new IllegalArgumentException("Argument 'cl' must not be null");
        }
        this.pluginFactoryType = pluginFactoryType;
        this.classLoader = cl;
        this.defaultPluginFactory = defaultPluginFactory;
    }

    public F load() {
        this.log.info("Loading PluginFactory of type [{}] ...", this.pluginFactoryType);
        final ServiceLoader<F> services = ServiceLoader.load(this.pluginFactoryType, this.classLoader);
        final List<F> matches = new ArrayList<>(3);
        for (final F pluginFactory : services) {
            matches.add(pluginFactory);
        }
        if (matches.size() > 1) {
            throw new IllegalStateException("Found more than one [" + matches.size()
                    + "] plugin factories of type [" + this.pluginFactoryType.getName() + "] on the classpath");
        }
        final F result;
        if (matches.size() > 0) {
            result = matches.get(0);
            this.log.info("Loaded PluginFactory [{}] from classpath", result);
        } else {
            result = this.defaultPluginFactory;
            this.log.info("Could not find any PluginFactory of type [{}] on classpath - will return default PluginFactory [{}] (may be null)",
                    this.pluginFactoryType, result);
        }
        return result;
    }
}
