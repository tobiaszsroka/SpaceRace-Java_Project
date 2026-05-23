package com.spacerace.lwjgl3;

import org.lwjgl.system.macosx.LibC;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.lang.management.ManagementFactory;
import java.util.ArrayList;

/**
 * StartupHelper — ensures the JVM launches on the first thread on macOS.
 *
 * On macOS, LWJGL3 requires the main thread to be the first thread
 * (for native UI events). This helper re-launches the JVM with
 * the {@code -XstartOnFirstThread} flag if needed.
 *
 * On Windows and Linux this class has no effect.
 */
public class StartupHelper {

    private static final String JVM_RESTARTED_ARG = "jvmIsRestarted";

    private StartupHelper() {
        // utility class, no instances
    }

    /**
     * Checks if the JVM was already restarted. If not (and on macOS),
     * starts a new JVM with the correct flags and returns {@code true},
     * meaning the caller should exit. Returns {@code false} if no
     * restart is needed.
     */
    public static boolean startNewJvmIfRequired() {
        // Only relevant on macOS
        String osName = System.getProperty("os.name", "").toLowerCase();
        if (!osName.contains("mac")) {
            return false;
        }

        // Check if already restarted
        long pid = LibC.getpid();
        if (System.getenv(JVM_RESTARTED_ARG) != null) {
            return false;
        }

        // Build the new JVM command
        try {
            ArrayList<String> command = new ArrayList<>();
            String javaHome = System.getProperty("java.home");
            command.add(javaHome + File.separator + "bin" + File.separator + "java");
            command.add("-XstartOnFirstThread");

            // Carry over existing JVM args
            for (String arg : ManagementFactory.getRuntimeMXBean().getInputArguments()) {
                if (!arg.contains("-XstartOnFirstThread")) {
                    command.add(arg);
                }
            }

            command.add("-cp");
            command.add(System.getProperty("java.class.path"));
            command.add(System.getenv("JAVA_MAIN_CLASS_" + pid));

            ProcessBuilder pb = new ProcessBuilder(command);
            pb.environment().put(JVM_RESTARTED_ARG, "true");
            pb.redirectErrorStream(true);
            pb.inheritIO();
            Process process = pb.start();
            process.waitFor();
        } catch (Exception e) {
            System.err.println("StartupHelper: Failed to restart JVM.");
            e.printStackTrace();
        }

        return true;
    }
}
