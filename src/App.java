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


//Esse não precisa, O Runtime já foi criado
// "C:\Program Files\Java\jdk-17\bin\jlink" ^--module-path "C:\Program Files\Java\jdk-17\jmods;C:\Program Files\Java\javafx-jmods-21.0.8" ^--add-modules java.base,java.desktop,javafx.controls,javafx.fxml ^--output MyRuntime


//Esse cria o executável só copiar e colar no cmd
// "C:\Program Files\Java\jdk-17\bin\jpackage" ^--name "MeuApp" ^--input . ^--main-jar Pharmacy.jar ^--main-class App ^--type exe ^--dest ./dist ^--runtime-image MyRuntime ^--win-shortcut ^--win-menu
