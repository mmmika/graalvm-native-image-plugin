/*
 * Copyright 2020 Shinya Mochida
 *
 * Licensed under the Apache License,Version2.0(the"License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,software
 * Distributed under the License is distributed on an"AS IS"BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.mikeneck.graalvm.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.hasItems;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class ReflectConfigTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    private final TestJsonReader reader = new TestJsonReader();

    @Test
    public void jsonWithContents() throws IOException {
        try (final InputStream inputStream = reader.configJsonResource("config/reflect-config-1.json")) {
            ReflectConfig reflectConfig = objectMapper.readValue(inputStream, ReflectConfig.class);
            assertThat(reflectConfig, hasItems(
                    new ClassUsage("com.fasterxml.jackson.databind.ext.Java7SupportImpl", new MethodUsage("<init>")),
                    new ClassUsage("java.sql.Date"),
                    new ClassUsage("java.sql.Timestamp"),
                    new ClassUsage("java.util.ArrayList", true, true),
                    new ClassUsage("java.util.LinkedHashMap", true, true),
                    new ClassUsage("com.example.App", true, true, true)
            ));
        }
    }

    @Test
    public void jsonWithoutContents() throws IOException {
        try (final InputStream inputStream = reader.configJsonResource("config/reflect-config-2.json")) {
            ReflectConfig reflectConfig = objectMapper.readValue(inputStream, ReflectConfig.class);
            assertThat(reflectConfig, is(Collections.emptyList()));
        }
        }
}
