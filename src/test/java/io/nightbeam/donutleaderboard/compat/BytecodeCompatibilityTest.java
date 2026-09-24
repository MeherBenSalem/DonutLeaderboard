package io.nightbeam.donutleaderboard.compat;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BytecodeCompatibilityTest {

    @Test
    void mainPluginClassesTargetJava17() throws Exception {
        Path classFile = Path.of("build/classes/java/main/io/nightbeam/donutleaderboard/DonutLeaderboardPlugin.class");
        assertTrue(Files.exists(classFile), "Run compileJava before this test");
        byte[] bytes = Files.readAllBytes(classFile);
        assertEquals(0xCAFEBABE, readInt(bytes, 0));
        int major = readShort(bytes, 6);
        assertEquals(61, major, "Major class file version 61 = Java 17");
    }

    private static int readShort(byte[] bytes, int offset) {
        return ((bytes[offset] & 0xFF) << 8) | (bytes[offset + 1] & 0xFF);
    }

    private static int readInt(byte[] bytes, int offset) {
        return ((bytes[offset] & 0xFF) << 24)
                | ((bytes[offset + 1] & 0xFF) << 16)
                | ((bytes[offset + 2] & 0xFF) << 8)
                | (bytes[offset + 3] & 0xFF);
    }
}
