package net.smackem.zlang.pad;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;

import java.io.IOException;
import java.util.prefs.Preferences;

/**
 * JavaFX App
 */
public class App extends Application {
    private static final String PREF_WIDTH = "mainStage.width";
    private static final String PREF_HEIGHT = "mainStage.height";
    private static final String PREF_LEFT = "mainStage.left";
    private static final String PREF_TOP = "mainStage.top";
    private static Scene scene;

    @Override
    public void start(Stage stage) throws IOException {
        final Preferences prefs = Preferences.userNodeForPackage(App.class);
        final double width = prefs.getDouble(PREF_WIDTH, 800);
        final double height = prefs.getDouble(PREF_HEIGHT, 600);
        final double x = prefs.getDouble(PREF_LEFT, Double.NaN);
        final double y = prefs.getDouble(PREF_TOP, Double.NaN);
        scene = new Scene(loadFXML("primary"), width, height);
        if (Double.isNaN(x) == false && Double.isNaN(y) == false) {
            stage.setX(x);
            stage.setY(y);
        }
        stage.setScene(scene);
        stage.setTitle("zlang.pad");
        stage.show();
        stage.addEventHandler(WindowEvent.WINDOW_CLOSE_REQUEST, ignored -> {
            prefs.putDouble(PREF_WIDTH, stage.getWidth());
            prefs.putDouble(PREF_HEIGHT, stage.getHeight());
            prefs.putDouble(PREF_LEFT, stage.getX());
            prefs.putDouble(PREF_TOP, stage.getY());
        });
    }

    static void setRoot(String fxml) throws IOException {
        scene.setRoot(loadFXML(fxml));
    }

    private static Parent loadFXML(String fxml) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(App.class.getResource(fxml + ".fxml"));
        return fxmlLoader.load();
    }

    public static void main(String[] args) {
        launch();
    }
}
