/*
 * Copyright (C) 2011, the original authors
 *
 * ====================================================================
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
 * ====================================================================
 */

package org.jclouds.karaf.services.modules;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import javax.inject.Singleton;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import org.apache.log4j.FileAppender;
import org.jclouds.domain.Credentials;
import org.jclouds.rest.ConfiguresCredentialStore;
import org.jclouds.rest.annotations.Credential;
import org.osgi.service.cm.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ConfiguresCredentialStore
public class ConfigurationAdminCredentialStore extends AbstractModule {

    private static final Logger LOGGER = LoggerFactory.getLogger(ConfigurationAdminCredentialStore.class);
    public static final String CREDENTIAL_STORE_PID = "org.jclouds.credentials";


    private Configuration configuration;
    private ConfigurationAdminBacking backing;

    public ConfigurationAdminCredentialStore(Configuration configuration) {
      this.configuration = configuration;
      this.backing = new ConfigurationAdminBacking(configuration);
    }

    /**
     * Configures a {@link com.google.inject.Binder} via the exposed methods.
     */
    @Override
    protected void configure() {
    }

    @Provides
    @Singleton
    protected Map<String, Credentials> provideCredentialStore() {
        return backing;
    }

    private class ConfigurationAdminBacking implements Map<String, Credentials> {

        private final Configuration configuration;
        private final Map<String, Credentials> credentialsMap = new LinkedHashMap<String, Credentials>();

        private ConfigurationAdminBacking(Configuration configuration) {
            this.configuration = configuration;
            try {
                Dictionary dictionary = configuration.getProperties();
                if (dictionary == null) {
                    dictionary = newDisctionary();
                }
                Enumeration keys = dictionary.keys();
                while (keys.hasMoreElements()) {
                    String key = (String) keys.nextElement();
                    if (key != null && key.startsWith("node#")) {
                        String stripedKey = key.substring(0, key.lastIndexOf("/"));
                        if (!credentialsMap.containsKey(stripedKey)) {
                            String identityKey = stripedKey + "/identity";
                            String credentialKey = stripedKey + "/credential";
                            String identity = (String) dictionary.get(identityKey);
                            String credential = (String) dictionary.get(credentialKey);
                            Credentials credentials = new Credentials(identity, credential);
                            credentialsMap.put(stripedKey, credentials);
                        }
                    }
                }
                configuration.update(dictionary);
            } catch (IOException e) {
                LOGGER.warn("Failed to store jclouds credentials to configuration admin.",e);
            }
        }

        public Dictionary newDisctionary() {
            Dictionary dictionary = new Properties();
            return dictionary;
        }

        public int size() {
            return credentialsMap.size();
        }

        public boolean isEmpty() {
            return credentialsMap.isEmpty();
        }

        public boolean containsKey(Object o) {
            return credentialsMap.containsKey(o);
        }

        public boolean containsValue(Object o) {
            return credentialsMap.containsValue(o);
        }

        public Credentials get(Object o) {
            return credentialsMap.get(o);
        }

        public Credentials put(String s, Credentials credentials) {
            try {
                Dictionary dictionary = configuration.getProperties();
                if (dictionary == null) {
                }
                String identityKey = s + "/identity";
                String credentialKey = s + "/credential";
                dictionary.put(identityKey, credentials.identity);
                dictionary.put(credentialKey, credentials.credential);

                configuration.update(dictionary);
            } catch (IOException e) {
                LOGGER.warn("Failed to store jclouds credentials to configuration admin.",e);
            }
            return credentialsMap.put(s, credentials);
        }

        public Credentials remove(Object o) {
            try {
                Dictionary dictionary = configuration.getProperties();
                if (dictionary == null) {
                    dictionary = newDisctionary();
                }

                String identityKey = o + "/identity";
                dictionary.remove(identityKey);
                configuration.update(dictionary);
            } catch (IOException e) {
                LOGGER.warn("Failed to store jclouds credentials to configuration admin.", e);
            }
            return credentialsMap.remove(o);
        }

        public void putAll(Map<? extends String, ? extends Credentials> map) {
            for (Map.Entry<? extends String, ? extends Credentials> entry : map.entrySet()) {
                String s = entry.getKey();
                Credentials credential = entry.getValue();
                put(s,credential);
            }
        }

        public void clear() {
            try {
                configuration.update(newDisctionary());
            } catch (IOException e) {
                LOGGER.warn("Failed to clear configuration admin jclouds credentials store.", e);
            }
            credentialsMap.clear();
        }

        public Set<String> keySet() {
            return credentialsMap.keySet();
        }

        public Collection<Credentials> values() {
            return credentialsMap.values();
        }

        public Set<Map.Entry<String, Credentials>> entrySet() {
            return credentialsMap.entrySet();
        }

        @Override
        public boolean equals(Object o) {
            return credentialsMap.equals(o);
        }

        @Override
        public int hashCode() {
            return credentialsMap.hashCode();
        }
    }
}