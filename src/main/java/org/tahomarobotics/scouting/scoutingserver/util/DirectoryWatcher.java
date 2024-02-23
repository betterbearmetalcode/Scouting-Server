package org.tahomarobotics.scouting.scoutingserver.util;

import com.google.zxing.NotFoundException;
import org.tahomarobotics.scouting.scoutingserver.Constants;
import org.tahomarobotics.scouting.scoutingserver.controller.QRScannerController;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static java.nio.file.StandardWatchEventKinds.*;

public class DirectoryWatcher {
    private static WatchService watchService;
    private boolean keepWatching = true;
    private String basePath;
    private Thread thread;
    static {
        try {
            watchService = FileSystems.getDefault().newWatchService();
        } catch (IOException e) {
            Logging.logError(e, "Failed to created file watcher");
        }
    }
    private Path dir;
    public DirectoryWatcher(String absPath) {
        dir = new File(absPath).toPath();
        basePath = absPath;
        try {
           dir.register(watchService, ENTRY_CREATE);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        //this thread will loop waiting for files to appear and procesing them
        thread = new Thread(new Runnable() {
            @Override
            public void run() {
                while (keepWatching) {
                    try {
                        WatchKey key = watchService.take();
                        for (WatchEvent<?> event: key.pollEvents()) {

                            // The filename is the
                            // context of the event.
                            WatchEvent<Path> ev = (WatchEvent<Path>) event;
                            Path filename = ev.context();

                            if (filename.toString().toLowerCase().endsWith(".png") || filename.toString().toLowerCase().endsWith(".jpg")) {
                                Thread.sleep(100);
                                QRScannerController.readStoredImage(basePath + filename.getFileName(), QRScannerController.activeTable);
                            }else {

                                break;
                            }

                        }
                        boolean valid = key.reset();
                        if (!valid) {
                            break;
                        }

                    } catch (InterruptedException e) {
                        Logging.logError(e, "File watcher interrupted possile cause is app closing");
                    } catch (NotFoundException e) {
                        Logging.logError(e, "Could not read qr code");
                    } catch (IOException e) {
                        Logging.logError(e);
                    } catch (ClosedWatchServiceException e) {
                        continue;
                    }
                }
            }
        });
        thread.start();


    }

    public void endWatcher() {
        keepWatching = false;
        try {
            watchService.close();
        } catch (IOException e) {
            thread.interrupt();
        }
    }
}
