import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.Screen;
import javafx.geometry.Rectangle2D;

public class App extends Application {
    @Override
    public void start(Stage stage) throws Exception {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("layout.fxml"));
        Scene scene = new Scene(loader.load(), 1350, 720);
        scene.getStylesheets().add(getClass().getResource("styles.css").toExternalForm());

        stage.initStyle(StageStyle.TRANSPARENT);
        scene.setFill(javafx.scene.paint.Color.TRANSPARENT);

        stage.setTitle("Farmácia - Estoque de Remédios");
        stage.setScene(scene);

        stage.setMinWidth(600);
        stage.setMinHeight(400);

        Screen screen = Screen.getPrimary();
        Rectangle2D bounds = screen.getVisualBounds();

        double width = bounds.getWidth() * 0.8;
        double height = bounds.getHeight() * 0.8;
        stage.setWidth(width);
        stage.setHeight(height);

        stage.setX((bounds.getWidth() - width) / 2 + bounds.getMinX());
        stage.setY((bounds.getHeight() - height) / 2 + bounds.getMinY());

        Controller controller = loader.getController();
        controller.setStage(stage);

        stage.show();

        controller.maximizeWindow();
    }

    public static void main(String[] args) {
        launch(args);
    }
}