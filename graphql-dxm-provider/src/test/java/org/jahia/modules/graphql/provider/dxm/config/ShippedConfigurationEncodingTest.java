package org.jahia.modules.graphql.provider.dxm.config;

import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertTrue;

/**
 * Checks that every .cfg file this module ships contains only ASCII bytes.
 *
 * <p>Felix FileInstall reads a .cfg file as ISO-8859-1. A UTF-8 character outside ASCII is therefore read as several
 * Latin-1 characters, and Jahia writes them back to the file in that form. A value that needs a character outside
 * ASCII must use a Unicode escape: a backslash, the letter u and four hexadecimal digits.
 */
public class ShippedConfigurationEncodingTest {

    private static final File CONFIGURATIONS = new File("src/main/resources/META-INF/configurations");

    @Test
    public void shippedConfigurationsContainOnlyAsciiBytes() throws IOException {
        File[] files = CONFIGURATIONS.listFiles((dir, name) -> name.endsWith(".cfg"));
        assertTrue("No .cfg file found in " + CONFIGURATIONS.getAbsolutePath(), files != null && files.length > 0);

        List<String> offending = new ArrayList<>();
        for (File file : files) {
            byte[] bytes = Files.readAllBytes(file.toPath());
            int line = 1;
            for (byte b : bytes) {
                if (b == '\n') {
                    line++;
                } else if ((b & 0x80) != 0) {
                    offending.add(file.getName() + ":" + line);
                    break;
                }
            }
        }
        assertTrue("Non-ASCII byte in a shipped .cfg file, at " + offending
                + ". Use ASCII in a comment and a \\uXXXX escape in a value.", offending.isEmpty());
    }
}
