/*
 * Copyright 2016 Red Hat, Inc.
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

package org.jboss.migration.core.jboss;

import org.jboss.migration.core.AbstractServer;
import org.jboss.migration.core.ProductInfo;
import org.jboss.migration.core.ServerMigrationFailureException;
import org.jboss.migration.core.env.MigrationEnvironment;
import org.jboss.migration.core.logger.ServerMigrationLogger;
import org.jboss.migration.core.util.xml.SimpleXMLFileMatcher;
import org.jboss.migration.core.util.xml.XMLFileMatcher;
import org.jboss.migration.core.util.xml.XMLFiles;

import javax.xml.stream.XMLStreamException;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * An abstract JBoss {@link org.jboss.migration.core.Server} impl, which is usable only as migration source.
 * @author emmartins
 */
public abstract class JBossServer<S extends JBossServer<S>> extends AbstractServer implements AbsolutePathResolver {

    public interface EnvironmentProperties {
        String PROPERTIES_PREFIX = "server.";

        String PROPERTIES_DOMAIN_PREFIX = "domain.";
        String PROPERTY_DOMAIN_BASE_DIR = PROPERTIES_DOMAIN_PREFIX + "domainDir";
        String PROPERTY_DOMAIN_CONFIG_DIR = PROPERTIES_DOMAIN_PREFIX + "configDir";
        String PROPERTY_DOMAIN_DATA_DIR = PROPERTIES_DOMAIN_PREFIX + "dataDir";
        String PROPERTY_DOMAIN_CONTENT_DIR = PROPERTIES_DOMAIN_PREFIX + "contentDir";

        String PROPERTY_DOMAIN_DOMAIN_CONFIG_FILES = PROPERTIES_DOMAIN_PREFIX + "domainConfigFiles";
        String PROPERTY_DOMAIN_HOST_CONFIG_FILES = PROPERTIES_DOMAIN_PREFIX + "hostConfigFiles";

        String PROPERTIES_STANDALONE_PREFIX = "standalone.";
        String PROPERTY_STANDALONE_SERVER_DIR = PROPERTIES_STANDALONE_PREFIX + "serverDir";
        String PROPERTY_STANDALONE_CONFIG_DIR = PROPERTIES_STANDALONE_PREFIX + "configDir";
        String PROPERTY_STANDALONE_DATA_DIR = PROPERTIES_STANDALONE_PREFIX + "dataDir";
        String PROPERTY_STANDALONE_CONTENT_DIR = PROPERTIES_STANDALONE_PREFIX + "contentDir";
        String PROPERTY_STANDALONE_CONFIG_FILES = PROPERTIES_STANDALONE_PREFIX + "configFiles";
    }

    private final Path domainBaseDir;
    private final Path domainConfigDir;
    private final Path domainDataDir;
    private final Path domainContentDir;

    private final Path standaloneServerDir;
    private final Path standaloneConfigDir;
    private final Path standaloneDataDir;
    private final Path standaloneContentDir;

    private final Map<String, Path> pathResolver;
    private final Modules modules;

    public JBossServer(String migrationName, ProductInfo productInfo, Path baseDir, MigrationEnvironment migrationEnvironment) {
        super(migrationName, productInfo, baseDir, migrationEnvironment);
        // build server paths from env
        Path domainBaseDir = Paths.get(migrationEnvironment.getPropertyAsString(getFullEnvironmentPropertyName(EnvironmentProperties.PROPERTY_DOMAIN_BASE_DIR), "domain"));
        if (!domainBaseDir.isAbsolute()) {
            domainBaseDir = baseDir.resolve(domainBaseDir);
        }
        this.domainBaseDir = domainBaseDir;
        Path domainConfigDir = Paths.get(migrationEnvironment.getPropertyAsString(getFullEnvironmentPropertyName(EnvironmentProperties.PROPERTY_DOMAIN_CONFIG_DIR), "configuration"));
        if (!domainConfigDir.isAbsolute()) {
            domainConfigDir = domainBaseDir.resolve(domainConfigDir);
        }
        this.domainConfigDir = domainConfigDir;
        Path domainDataDir = Paths.get(migrationEnvironment.getPropertyAsString(getFullEnvironmentPropertyName(EnvironmentProperties.PROPERTY_DOMAIN_DATA_DIR), "data"));
        if (!domainDataDir.isAbsolute()) {
            domainDataDir = domainBaseDir.resolve(domainDataDir);
        }
        this.domainDataDir = domainDataDir;
        Path domainContentDir = Paths.get(migrationEnvironment.getPropertyAsString(getFullEnvironmentPropertyName(EnvironmentProperties.PROPERTY_DOMAIN_CONTENT_DIR), "content"));
        if (!domainContentDir.isAbsolute()) {
            domainContentDir = domainDataDir.resolve(domainContentDir);
        }
        this.domainContentDir = domainContentDir;

        Path standaloneServerDir = Paths.get(migrationEnvironment.getPropertyAsString(getFullEnvironmentPropertyName(EnvironmentProperties.PROPERTY_STANDALONE_SERVER_DIR), "standalone"));
        if (!standaloneServerDir.isAbsolute()) {
            standaloneServerDir = baseDir.resolve(standaloneServerDir);
        }
        this.standaloneServerDir = standaloneServerDir;
        Path standaloneConfigDir = Paths.get(migrationEnvironment.getPropertyAsString(getFullEnvironmentPropertyName(EnvironmentProperties.PROPERTY_STANDALONE_CONFIG_DIR), "configuration"));
        if (!standaloneConfigDir.isAbsolute()) {
            standaloneConfigDir = standaloneServerDir.resolve(standaloneConfigDir);
        }
        this.standaloneConfigDir = standaloneConfigDir;
        Path standaloneDataDir = Paths.get(migrationEnvironment.getPropertyAsString(getFullEnvironmentPropertyName(EnvironmentProperties.PROPERTY_STANDALONE_DATA_DIR), "data"));
        if (!standaloneDataDir.isAbsolute()) {
            standaloneDataDir = standaloneServerDir.resolve(standaloneDataDir);
        }
        this.standaloneDataDir = standaloneDataDir;
        Path standaloneContentDir = Paths.get(migrationEnvironment.getPropertyAsString(getFullEnvironmentPropertyName(EnvironmentProperties.PROPERTY_STANDALONE_CONTENT_DIR), "content"));
        if (!standaloneContentDir.isAbsolute()) {
            standaloneContentDir = standaloneDataDir.resolve(standaloneContentDir);
        }
        this.standaloneContentDir = standaloneContentDir;

        this.pathResolver = new HashMap<>();
        this.pathResolver.put("jboss.server.base.dir", standaloneServerDir);
        this.pathResolver.put("jboss.server.config.dir", standaloneConfigDir);
        this.pathResolver.put("jboss.server.data.dir", standaloneDataDir);
        this.pathResolver.put("jboss.server.content.dir", standaloneContentDir);
        this.pathResolver.put("jboss.server.log.dir", standaloneServerDir.resolve("log"));
        this.pathResolver.put("jboss.domain.base.dir", domainBaseDir);
        this.pathResolver.put("jboss.domain.config.dir", domainConfigDir);
        this.pathResolver.put("jboss.domain.data.dir", domainDataDir);
        this.pathResolver.put("jboss.domain.content.dir", domainContentDir);
        this.modules = new Modules(baseDir);
    }

    protected String getFullEnvironmentPropertyName(String propertyName) {
        return EnvironmentProperties.PROPERTIES_PREFIX + getMigrationName() + "." + propertyName;
    }

    protected Collection<JBossServerConfigurationPath<S>> getConfigs(final Path configurationDir, final Path dataDir, final Path contentDir, final String xmlDocumentElementName, final String envPropertyName) {
        final List<JBossServerConfigurationPath<S>> configs = new ArrayList<>();
        final String fullEnvPropertyName = getFullEnvironmentPropertyName(envPropertyName);
        final List<String> envConfigs = getMigrationEnvironment().getPropertyAsList(fullEnvPropertyName);
        if (envConfigs != null && !envConfigs.isEmpty()) {
            for (String envConfig : envConfigs) {
                Path config = Paths.get(envConfig);
                if (!config.isAbsolute()) {
                    config = configurationDir.resolve(config);
                }
                if (Files.exists(config)) {
                    configs.add(new JBossServerConfigurationPath<>(config, (S) this, configurationDir, dataDir, contentDir));
                } else {
                    ServerMigrationLogger.ROOT_LOGGER.warnf("Config file %s, specified by the environment property %s, does not exists.", config, fullEnvPropertyName);
                }
            }
        } else {
            // scan config dir
            final XMLFileMatcher scanMatcher = new SimpleXMLFileMatcher() {
                @Override
                protected boolean documentElementLocalNameMatches(String localName) {
                    return xmlDocumentElementName.equals(localName);
                }
                @Override
                protected boolean documentNamespaceURIMatches(String namespaceURI) {
                    return namespaceURI.startsWith("urn:jboss:domain:");
                }
            };
            for (Path path : XMLFiles.scan(configurationDir, false, scanMatcher)) {
                configs.add(new JBossServerConfigurationPath<>(path, (S) this, configurationDir, dataDir, contentDir));
            }
        }
        return Collections.unmodifiableList(configs);
    }

    public Collection<JBossServerConfigurationPath<S>> getStandaloneConfigs() {
        return getConfigs(getStandaloneConfigurationDir(), getStandaloneDataDir(), getStandaloneContentDir(), "server", EnvironmentProperties.PROPERTY_STANDALONE_CONFIG_FILES);
    }

    public Collection<JBossServerConfigurationPath<S>> getDomainDomainConfigs() {
        return getConfigs(getDomainConfigurationDir(), getDomainDataDir(), getDomainContentDir(), "domain", EnvironmentProperties.PROPERTY_DOMAIN_DOMAIN_CONFIG_FILES);
    }

    public Collection<JBossServerConfigurationPath<S>> getDomainHostConfigs() {
        return getConfigs(getDomainConfigurationDir(), getDomainDataDir(), getDomainContentDir(), "host", EnvironmentProperties.PROPERTY_DOMAIN_HOST_CONFIG_FILES);
    }

    public Path getDomainDir() {
        return domainBaseDir;
    }

    public Path getDomainConfigurationDir() {
        return domainConfigDir;
    }

    public Path getDomainDataDir() {
        return domainDataDir;
    }

    public Path getDomainContentDir() {
        return domainContentDir;
    }

    public Path getStandaloneDir() {
        return standaloneServerDir;
    }

    public Path getStandaloneConfigurationDir() {
        return standaloneConfigDir;
    }

    public Path getStandaloneDataDir() {
        return standaloneDataDir;
    }

    public Path getStandaloneContentDir() {
        return standaloneContentDir;
    }

    @Override
    public Path resolveNamedPath(String path) {
        Path resolved = pathResolver.get(path);
        if (resolved == null) {
            try {
                final String property = System.getProperty(path);
                if (property != null) {
                    final Path propertyPath = Paths.get(property);
                    if (Files.isDirectory(propertyPath)) {
                        resolved = propertyPath;
                    }
                }
            } catch (InvalidPathException e) {
                // ignore, was a long shot...
            }
        }
        return resolved;
    }

    public Modules getModules() {
        return modules;
    }

    public static class Module {
        private final Path moduleDir;
        private final ModuleSpecification moduleSpecification;

        public Module(Path moduleDir, ModuleSpecification moduleSpecification) {
            this.moduleDir = moduleDir;
            this.moduleSpecification = moduleSpecification;
        }

        public ModuleSpecification getModuleSpecification() {
            return moduleSpecification;
        }

        public Path getModuleDir() {
            return moduleDir;
        }
    }

    public static class Modules {

        private final Path modulesDir;
        private final Path systemLayersBaseDir;
        private final Path overlayDir;

        public Modules(Path serverBaseDir) {
            this.modulesDir = serverBaseDir.resolve("modules");
            this.systemLayersBaseDir = modulesDir.resolve("system").resolve("layers").resolve("base");
            final Path overlaysDir = systemLayersBaseDir.resolve(".overlays");
            final Path overlaysFile = overlaysDir.resolve(".overlays");
            if (Files.exists(overlaysFile)) {
                try {
                    final String activeOverlayFileName = new String(Files.readAllBytes(overlaysFile)).trim();
                    if (!activeOverlayFileName.isEmpty()) {
                        overlayDir = overlaysDir.resolve(activeOverlayFileName);
                    } else {
                        overlayDir = null;
                    }
                } catch (IOException e) {
                    throw new ServerMigrationFailureException("failed to read overlays file", e);
                }
            } else {
                overlayDir = null;
            }
        }

        public Path getModulesDir() {
            return modulesDir;
        }

        public Module getModule(ModuleIdentifier moduleId) throws ServerMigrationFailureException {
            final Path moduleDir = getModuleDir(moduleId);
            if (!Files.exists(moduleDir)) {
                return null;
            }
            final Path moduleSpecPath = moduleDir.resolve("module.xml");
            if (!Files.exists(moduleSpecPath)) {
                return null;
            }
            final ModuleSpecification moduleSpecification;
            try {
                moduleSpecification = ModuleSpecification.Parser.parse(moduleSpecPath);
            } catch (XMLStreamException | IOException e) {
                throw new ServerMigrationFailureException(e);
            }
            return new Module(moduleDir, moduleSpecification);
        }

        public Module getModule(String moduleId) throws ServerMigrationFailureException {
            return getModule(ModuleIdentifier.fromString(moduleId));
        }

        public Path getModuleDir(ModuleIdentifier moduleId) {
            if (moduleId == null) {
                throw new IllegalArgumentException("The module identifier cannot be null.");
            }
            final Path modulePath = Paths.get(moduleId.getName().replace('.', File.separatorChar) +
                    File.separator +
                    moduleId.getSlot());
            if (overlayDir != null) {
                final Path overlayModuleDir = overlayDir.resolve(modulePath);
                if (Files.exists(overlayModuleDir)) {
                    return overlayModuleDir;
                }
            }
            final Path systemLayersBaseModuleDir = systemLayersBaseDir.resolve(modulePath);
            return systemLayersBaseModuleDir;
        }
    }
}
