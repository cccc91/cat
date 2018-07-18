package cclerc.cat;

import cclerc.services.Utilities;
import org.apache.commons.net.io.Util;

public class PeriodicSpeedTest implements Runnable {

    private boolean running = true;

    // Periodic speed test instance
    private static PeriodicSpeedTest periodicSpeedTest = new PeriodicSpeedTest();

    private PeriodicSpeedTest() {
    }

    public void start() {
        Thread lThread = new Thread(periodicSpeedTest);
        lThread.start();
    }

    @Override
    public void run() {

        // Name thread
        Thread.currentThread().setName("Periodic SpeedTest Thread");

        while (running) {

            System.out.println(">>> PERIODIC SPEED TEST");
            Utilities.sleep(1000);

        }

    }

    // GETTERS

    public static PeriodicSpeedTest getPeriodicSpeedTest() {
        return periodicSpeedTest;
    }

}
