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
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ReflectConfigTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    private final TestJsonReader reader = new TestJsonReader();

    @Test
    void jsonWithContents() throws IOException {
        try (final InputStream inputStream = reader.configJsonResource("config/reflect-config-1.json")) {
            ReflectConfig reflectConfig = objectMapper.readValue(inputStream, ReflectConfig.class);
            assertThat(reflectConfig).contains(
                    new ClassUsage("com.fasterxml.jackson.databind.ext.Java7SupportImpl", new MethodUsage("<init>")),
                    new ClassUsage("java.sql.Date"),
                    new ClassUsage("java.sql.Timestamp"),
                    new ClassUsage("java.util.ArrayList", true, true),
                    new ClassUsage("java.util.LinkedHashMap", true, true),
                    new ClassUsage("com.example.App", true, true, true)
            );
        }
    }

    @Test
    void jsonWithoutContents() throws IOException {
        try (final InputStream inputStream = reader.configJsonResource("config/reflect-config-2.json")) {
            ReflectConfig reflectConfig = objectMapper.readValue(inputStream, ReflectConfig.class);
            assertThat(reflectConfig).isEqualTo(Collections.emptySortedSet());
        }
    }

    @Test
    void mergeWithOther() {
        ReflectConfig left = new ReflectConfig(
                new ClassUsage("java.sql.Date", MethodUsage.of("getTime")),
                new ClassUsage("java.sql.Timestamp"));
        ReflectConfig right = new ReflectConfig(
                new ClassUsage(ArrayList.class, MethodUsage.ofInit(int.class)),
                new ClassUsage("com.example.App", MethodUsage.of("run")));

        ReflectConfig reflectConfig = left.mergeWith(right);

        assertThat(reflectConfig).contains(
                new ClassUsage("com.example.App", MethodUsage.of("run")),
                new ClassUsage("java.sql.Date", MethodUsage.of("getTime")),
                new ClassUsage("java.sql.Timestamp"),
                new ClassUsage(ArrayList.class, MethodUsage.ofInit(int.class)));
    }

    @Test
    void mergeWithOtherHavingSameClass() {
        ReflectConfig left = new ReflectConfig(
                new ClassUsage("java.sql.Date", MethodUsage.of("getTime")),
                new ClassUsage("java.sql.Timestamp"));
        ReflectConfig right = new ReflectConfig(
                new ClassUsage(ArrayList.class, MethodUsage.ofInit(int.class)),
                new ClassUsage("java.sql.Date", MethodUsage.of("getTime")),
                new ClassUsage("com.example.App", MethodUsage.of("run")));

        ReflectConfig reflectConfig = left.mergeWith(right);

        assertThat(reflectConfig).contains(
                new ClassUsage("com.example.App", MethodUsage.of("run")),
                new ClassUsage("java.sql.Date", MethodUsage.of("getTime")),
                new ClassUsage("java.sql.Timestamp"),
                new ClassUsage(ArrayList.class, MethodUsage.ofInit(int.class)));
    }

    @Test
    void mergeWithOtherHavingSameClassUsingAnotherMethodsAndFields() {
        ReflectConfig left = new ReflectConfig(
                new ClassUsage("java.sql.Date", MethodUsage.of("getTime")),
                new ClassUsage(ArrayList.class,
                        MethodUsage.ofInit(int.class),
                        MethodUsage.of("add", Object.class),
                        MethodUsage.of("addAll", Collection.class)),
                new ClassUsage("com.example.Bar", new FieldUsage("baz"), new FieldUsage("qux")),
                new ClassUsage("java.sql.Timestamp"));
        ReflectConfig right = new ReflectConfig(
                new ClassUsage(ArrayList.class, MethodUsage.ofInit()),
                new ClassUsage("com.example.Bar", new FieldUsage("quux"), new FieldUsage("baz")),
                new ClassUsage("java.sql.Date", MethodUsage.of("getTime")),
                new ClassUsage("com.example.App", MethodUsage.of("run")));

        ReflectConfig reflectConfig = left.mergeWith(right);

        assertThat(reflectConfig).contains(
                new ClassUsage("com.example.App", MethodUsage.of("run")),
                new ClassUsage("com.example.Bar", new FieldUsage("baz"), new FieldUsage("quux"), new FieldUsage("qux")),
                new ClassUsage("java.sql.Date", MethodUsage.of("getTime")),
                new ClassUsage("java.sql.Timestamp"),
                new ClassUsage(ArrayList.class,
                        MethodUsage.ofInit(),
                        MethodUsage.ofInit(int.class),
                        MethodUsage.of("add", Object.class),
                        MethodUsage.of("addAll", Collection.class)));
    }

    @Test
    void mergeWithEmptyBecomesSelf() {
        ReflectConfig left = new ReflectConfig(
                new ClassUsage("java.sql.Date", MethodUsage.of("getTime")),
                new ClassUsage(ArrayList.class,
                        MethodUsage.ofInit(int.class),
                        MethodUsage.of("add", Object.class),
                        MethodUsage.of("addAll", Collection.class)),
                new ClassUsage("java.sql.Timestamp"));
        ReflectConfig right = new ReflectConfig();

        ReflectConfig reflectConfig = left.mergeWith(right);

        assertThat(reflectConfig).isEqualTo(left);
    }

    @Test
    void mergeByEmptyWithEmptyBecomesEmpty() {
        ReflectConfig left = new ReflectConfig();
        ReflectConfig right = new ReflectConfig();

        ReflectConfig reflectConfig = left.mergeWith(right);

        assertThat(reflectConfig).isEqualTo(Collections.emptySortedSet());
    }

    @Test
    void mergeByEmptyWithOtherBecomesOther() {
        ReflectConfig left = new ReflectConfig();
        ReflectConfig right = new ReflectConfig(
                new ClassUsage(ArrayList.class, MethodUsage.ofInit(int.class)),
                new ClassUsage("com.example.App", MethodUsage.of("run")));

        ReflectConfig reflectConfig = left.mergeWith(right);

        assertThat(reflectConfig).isEqualTo(right);
    }

    // https://github.com/mike-neck/graalvm-native-image-plugin/issues/46
    @Test
    void parseErrorCase46() throws IOException {
        try (final InputStream inputStream = reader.configJsonResource("config/reflect-config-parse-error-46.json")) {
            ReflectConfig reflectConfig = objectMapper.readValue(inputStream, ReflectConfig.class);
            assertThat(reflectConfig).isNotNull();
        }
    }
}
