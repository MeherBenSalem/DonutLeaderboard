package io.nightbeam.donutleaderboard.smoke;

import ch.vorburger.mariadb4j.DB;
import java.nio.file.Files;
import java.nio.file.Path;

/** Starts embedded MariaDB for smoke-server-test.sh; writes port to the path in argv[0]. */
public final class EmbeddedMariaDbLauncher {

    public static void main(String[] args) throws Exception {
        if (args.length < 1) {
            System.err.println("Usage: EmbeddedMariaDbLauncher <portFile>");
            System.exit(2);
        }
        Path portFile = Path.of(args[0]);
        DB db = DB.newEmbeddedDB(0);
        db.start();
        db.createDB("donut_leaderboard");
        int port = db.getConfiguration().getPort();
        Files.writeString(portFile, String.valueOf(port));
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                db.stop();
            } catch (Exception ignored) {
            }
        }));
        Thread.currentThread().join();
    }

    private EmbeddedMariaDbLauncher() {
    }
}
