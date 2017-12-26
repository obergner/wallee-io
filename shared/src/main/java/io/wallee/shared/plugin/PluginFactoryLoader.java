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
