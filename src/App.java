import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.stage.Stage;

public class App extends Application {

    @Override
    public void start(Stage stage) {
        Label label = new Label("Eu não Preciso de Maven para fazer um app");
        Scene scene = new Scene(label, 400, 200); 


        stage.setTitle("Farmacia");
        stage.setScene(scene);
        stage.show(); 
    }

    public static void main(String[] args) {
        launch(args);
    }
}


