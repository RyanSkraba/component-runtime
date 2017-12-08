/**
 * Copyright (C) 2006-2017 Talend Inc. - www.talend.com
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.talend.sdk.component.tools;

import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.joining;
import static org.apache.ziplock.JarLocation.jarLocation;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.stream.Stream;

import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.rules.TestName;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class AsciidocDocumentationGeneratorTest {

    @ClassRule
    public static final TemporaryFolder TEMPORARY_FOLDER = new TemporaryFolder();

    @Rule
    public final TestName testName = new TestName();

    @Test
    public void generateAdoc() throws IOException {
        final File output = new File(TEMPORARY_FOLDER.getRoot(), testName.getMethodName() + ".asciidoc");
        new AsciidocDocumentationGenerator(new File[] { copyBinaries("org.talend.test.valid") }, output, null, 2, null,
                null, null, null, log, findWorkDir(), "1.0").run();
        assertTrue(output.exists());
        try (final BufferedReader reader = new BufferedReader(new FileReader(output))) {
            assertEquals(
                    "== my\n" + "\n" + "super my component\n" + "\n" + "=== Configuration\n" + "\n" + "|===\n"
                            + "|Path|Description|Default Value\n" + "|configuration|configuration configuration|-\n"
                            + "|configuration.input|the input value|-\n" + "|configuration.nested|it is nested|-\n"
                            + "|configuration.nested.user|the user to log in|unknown\n"
                            + "|configuration.undocumented|undocumented configuration|0\n" + "|===\n",
                    reader.lines().collect(joining("\n")));
        }
    }

    @Test
    public void generateHtmlPdf() throws IOException {
        final File output = new File(TEMPORARY_FOLDER.getRoot(), testName.getMethodName() + ".asciidoc");
        final File outputHtml = new File(TEMPORARY_FOLDER.getRoot(), testName.getMethodName() + ".html");
        final File outputPdf = new File(TEMPORARY_FOLDER.getRoot(), testName.getMethodName() + ".pdf");
        new AsciidocDocumentationGenerator(new File[] { copyBinaries("org.talend.test.valid") }, output, "SuperTitle",
                2, new HashMap<String, String>() {

                    {
                        put("html", outputHtml.getAbsolutePath());
                        put("pdf", outputPdf.getAbsolutePath());
                    }
                }, null, null, null, log, findWorkDir(), "1.0").run();
        assertTrue(outputHtml.exists());
        assertTrue(outputPdf.exists());
        try (final BufferedReader reader = new BufferedReader(new FileReader(outputHtml))) {
            assertEquals("<!DOCTYPE html>", reader.lines().limit(1).findFirst().get());
        }
    }

    private File findWorkDir() {
        return new File(jarLocation(AsciidocDocumentationGeneratorTest.class).getParentFile(),
                getClass().getSimpleName() + "_workdir");
    }

    private File copyBinaries(final String pck) {
        final String pckPath = pck.replace('.', '/');
        final File root = new File(jarLocation(getClass()), pckPath);
        final File scannable =
                new File(TEMPORARY_FOLDER.getRoot(), getClass().getName() + "_" + testName.getMethodName());
        final File classDir = new File(scannable, pckPath);
        classDir.mkdirs();
        ofNullable(root.listFiles())
                .map(Stream::of)
                .orElseGet(Stream::empty)
                .filter(c -> c.getName().endsWith(".class"))
                .forEach(c -> {
                    try {
                        Files.copy(c.toPath(), new File(classDir, c.getName()).toPath());
                    } catch (final IOException e) {
                        fail("cant create test plugin");
                    }
                });
        return scannable;
    }
}
