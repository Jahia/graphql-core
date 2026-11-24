package org.jahia.modules.graphql.provider.dxm.config;

import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertTrue;

/**
 * Checks that every .cfg file the modules of this repository ship contains only ASCII bytes.
 *
 * <p>Felix FileInstall reads a .cfg file as ISO-8859-1. A UTF-8 character outside ASCII is therefore read as several
 * Latin-1 characters, and Jahia writes them back to the file in that form. A value that needs a character outside
 * ASCII must use a Unicode escape: a backslash, the letter u and four hexadecimal digits.
 *
 * <p>The test runs in this module, and reads the configurations of every module of the repository, from its parent
 * directory.
 */
public class ShippedConfigurationEncodingTest {

    private static final String CONFIGURATIONS = "src/main/resources/META-INF/configurations";

    @Test
    public void shippedConfigurationsContainOnlyAsciiBytes() throws IOException {
        File module = new File(System.getProperty("basedir", ".")).getAbsoluteFile();
        File repository = module.getParentFile();
        assertTrue("No .cfg file found in " + new File(module, CONFIGURATIONS), cfgFiles(module).length > 0);

        List<String> offending = new ArrayList<>();
        File[] modules = repository.listFiles(File::isDirectory);
        for (File sibling : modules == null ? new File[0] : modules) {
            for (File file : cfgFiles(sibling)) {
                String line = firstNonAsciiLine(file);
                if (line != null) {
                    offending.add(sibling.getName() + "/" + CONFIGURATIONS + "/" + file.getName() + ":" + line);
                }
            }
        }
        assertTrue("Non-ASCII byte in a shipped .cfg file, at " + offending
                + ". Use ASCII in a comment and a \\uXXXX escape in a value.", offending.isEmpty());
    }

    private static File[] cfgFiles(File module) {
        File[] files = new File(module, CONFIGURATIONS).listFiles((dir, name) -> name.endsWith(".cfg"));
        return files == null ? new File[0] : files;
    }

    private static String firstNonAsciiLine(File file) throws IOException {
        int line = 1;
        for (byte b : Files.readAllBytes(file.toPath())) {
            if (b == '\n') {
                line++;
            } else if ((b & 0x80) != 0) {
                return String.valueOf(line);
            }
        }
        return null;
    }
}
