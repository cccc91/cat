package cclerc.cat.view;

import cclerc.services.Utilities;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.Cursor;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.Stage;



public class WaitDialog {

    class WaitThread implements Runnable {

        private int period = 1000;
        private boolean running = true;
        private String dots = "";

        private WaitThread(int aInPeriod) {
            period = aInPeriod;
        }

        private WaitThread() {
        }

        // THREADS

        /**
         * Runs wait thread:
         * - Periodically displays a dot in a dialog
         */
        @Override
        public void run() {

            // Name thread
            Thread.currentThread().setName("Wait job Thread");

            // Run the thread
            while (running) {
                Platform.runLater(() -> {
                    dotsLabel.setText(dots += '.');
                });
                Utilities.sleep(period);
            }

        }

        public synchronized void terminate() {
            running = false;
        }

    }

    class RunAction implements Runnable {

        WaitDialogInterface waitDialogInterface;

        private RunAction(WaitDialogInterface aInWaitDialogInterface) {
            waitDialogInterface = aInWaitDialogInterface;
        }

        // THREADS

        @Override
        public void run() {
            Thread.currentThread().setName("Run Action Thread");
            waitDialogInterface.runAction();
        }

    }

    // Display management
    private Stage dialogStage;

    // FXML
    @FXML private ImageView logoImageView;
    @FXML private Label header;
    @FXML private Label dotsLabel;

    private WaitThread waitThread = new WaitThread();

    // SETTERS

    /**
     * Sets the stage of this dialog
     * @param aInDialogStage Stage
     */
    public void initializeContext(Stage aInDialogStage, String aInHeader, Image aInLogo) {
        dialogStage = aInDialogStage;
        header.setText(aInHeader);
        logoImageView.setImage(aInLogo);
    }

    // METHODS

    /**
     * Displays dialog box
     */
    public void show(WaitDialogInterface aInWaitDialogInterface) {
        dialogStage.setResizable(false);
        Thread lWaitThread = new Thread(waitThread);
        lWaitThread.start();
        Thread lRunThread = new Thread(new RunAction(aInWaitDialogInterface));
        lRunThread.start();

        dialogStage.getScene().setCursor(Cursor.WAIT);
        dialogStage.getOwner().getScene().setCursor(Cursor.WAIT);
        dialogStage.showAndWait();
    }

    /**
     * Closes dialog box
     */
    public void close() {
        waitThread.terminate();
        dialogStage.getScene().setCursor(Cursor.DEFAULT);
        dialogStage.getOwner().getScene().setCursor(Cursor.DEFAULT);
        dialogStage.close();
    }

}
