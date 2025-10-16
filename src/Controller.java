import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.TilePane;
import javafx.scene.layout.VBox;
import javafx.scene.layout.HBox;
import javafx.geometry.Pos;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.scene.Scene;
import javafx.geometry.Insets;
import javafx.application.Platform;
// import javafx.scene.control.TextField;
// import javafx.util.StringConverter; não tava usando mais

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Comparator;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class Controller {

    @FXML private TilePane gridPane;
    @FXML private TextField nomeField;
    @FXML private TextField batchField;
    @FXML private TextField qtdField;
    @FXML private TextField validadeField;
    @FXML private TextField searchField;
    @FXML private ComboBox<String> sortCombo;
    @FXML private ComboBox<String> filterCombo;
    @FXML private Label totalLabel;
    @FXML private Button editarSelecionadoBtn;
    @FXML private Button excluirSelecionadoBtn;
    @FXML private TextField inputField;
    @FXML private Label statusLabel;
    @FXML private TextField thresholdField;



    private final List<Medicamento> medicamentos = new ArrayList<>();
    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private Medicamento editingMed = null; // reference to currently edited medicine
    private Medicamento selectedMed = null;

    @FXML
    public void initialize() {
        gridPane.setHgap(10);
        gridPane.setVgap(10);

        // Example medicines
        medicamentos.add(new Medicamento("Paracetamol", "B001", 20, LocalDate.of(2025, 12, 15), 5));
        medicamentos.add(new Medicamento("Ibuprofeno", "B002", 8, LocalDate.of(2025, 10, 25), 10));
        medicamentos.add(new Medicamento("Amoxicilina", "B003", 3, LocalDate.of(2025, 10, 10), 5));
        medicamentos.add(new Medicamento("Dipirona", "B004", 15, LocalDate.of(2025, 11, 5), 5));
        medicamentos.add(new Medicamento("Loratadina", "B005", 2, LocalDate.of(2025, 10, 18), 3));
        medicamentos.add(new Medicamento("Omeprazol", "B006", 12, LocalDate.of(2026, 1, 1), 5));
        medicamentos.add(new Medicamento("Cetirizina", "B007", 7, LocalDate.of(2025, 10, 20), 5));
        medicamentos.add(new Medicamento("Metformina", "B008", 25, LocalDate.of(2026, 3, 15), 10));
        medicamentos.add(new Medicamento("Clorfenamina", "B009", 0, LocalDate.of(2025, 9, 30), 5));
        medicamentos.add(new Medicamento("Diclofenaco", "B010", 18, LocalDate.of(2025, 12, 5), 5));

        // Initialize sort and filter
        sortCombo.getItems().addAll("Nome", "Quantidade", "Validade");
        sortCombo.setValue("Nome");
        filterCombo.getItems().addAll("Todos", "Vencidos", "Ativos");
        filterCombo.setValue("Todos");

        // Listeners for search, sort, filter
        searchField.textProperty().addListener((obs, oldV, newV) -> refreshGrid());
        sortCombo.valueProperty().addListener((obs, oldV, newV) -> refreshGrid());
        filterCombo.valueProperty().addListener((obs, oldV, newV) -> refreshGrid());

        // Automatic '/' insertion for validadeField
        validadeField.setTextFormatter(new TextFormatter<>(change -> {
            if (change.isAdded() || change.isReplaced()) {
                String digits = change.getControlNewText().replaceAll("[^\\d]", "");
                if (digits.length() > 8) digits = digits.substring(0, 8);

                StringBuilder formatted = new StringBuilder();
                int caretPos = change.getCaretPosition();

                for (int i = 0; i < digits.length(); i++) {
                    formatted.append(digits.charAt(i));
                    if ((i == 1 || i == 3) && i != digits.length() - 1) {
                        formatted.append("/");
                        if (caretPos > i) caretPos++;
                    }
                }

                change.setText(formatted.toString());
                change.setRange(0, change.getControlText().length());
                caretPos = Math.min(caretPos, formatted.length());
                change.selectRange(caretPos, caretPos);
            }
            return change;
        }));
        editarSelecionadoBtn.setOnAction(e -> {
            if (selectedMed != null) {
                editarMedicamento(selectedMed);
            }
        });

        excluirSelecionadoBtn.setOnAction(e -> {
            if (selectedMed != null) {
                excluirMedicamento(selectedMed);
                selectedMed = null;
                refreshGrid();
            }
        });

        refreshGrid();
        Platform.runLater(() -> verificarAlertas());
    }

    @FXML
    private void adicionarMedicamento() {
        String nome = nomeField.getText();
        String batch = batchField.getText();
        String validadeStr = validadeField.getText();
        int threshold = 5;
        int qtd = 0;

        try { threshold = Integer.parseInt(thresholdField.getText()); } catch (NumberFormatException ignored) {}
        try { qtd = Integer.parseInt(qtdField.getText()); } catch (NumberFormatException ignored) {}

        if (!nome.isEmpty() && !batch.isEmpty() && !validadeStr.isEmpty()) {
            LocalDate validade = parseDate(validadeStr);
            if (validade != null) {
                if (editingMed != null) {
                    editingMed.setNome(nome);
                    editingMed.setBatch(batch);
                    editingMed.setQuantidade(qtd);
                    editingMed.setValidade(validade);
                    editingMed.setAlertThreshold(threshold);
                    editingMed = null;
                } else {
                    Medicamento med = new Medicamento(nome, batch, qtd, validade, threshold);
                    med.setAlertThreshold(threshold);
                    medicamentos.add(med);
                }
                refreshGrid();
                nomeField.clear();
                batchField.clear();
                qtdField.clear();
                validadeField.clear();
                thresholdField.clear();
            }
        }
        refreshGrid();
        verificarAlertas();
    }

    @FXML
    private void editarMedicamento(Medicamento med) {
        nomeField.setText(med.getNome());
        batchField.setText(med.getBatch());
        qtdField.setText(String.valueOf(med.getQuantidade()));
        validadeField.setText(med.getValidade().format(formatter));
        thresholdField.setText(String.valueOf(med.getAlertThreshold()));
        editingMed = med;
    }

    @FXML
    private void excluirMedicamento(Medicamento med) {
        medicamentos.remove(med);
        refreshGrid();
    }

    private LocalDate parseDate(String str) {
        try {
            return LocalDate.parse(str, formatter);
        } catch (DateTimeParseException e) {
            Alert alert = new Alert(Alert.AlertType.ERROR, "Formato de data inválido! Use dd/mm/yyyy");
            alert.showAndWait();
            return null;
        }
    }

    private void refreshGrid() {
        gridPane.getChildren().clear();

        String search = searchField.getText().toLowerCase();
        String filter = filterCombo.getValue();
        String sort = sortCombo.getValue();

        List<Medicamento> list = medicamentos.stream()
                .filter(m -> m.getNome().toLowerCase().contains(search))
                .filter(m -> {
                    if ("Vencidos".equals(filter)) return m.getValidade().isBefore(LocalDate.now());
                    if ("Ativos".equals(filter)) return !m.getValidade().isBefore(LocalDate.now());
                    return true;
                })
                .collect(Collectors.toList());

        Comparator<Medicamento> comparator;
        switch (sort) {
            case "Quantidade": comparator = Comparator.comparingInt(Medicamento::getQuantidade); break;
            case "Validade": comparator = Comparator.comparing(Medicamento::getValidade); break;
            default: comparator = Comparator.comparing(Medicamento::getNome); break;
        }
        list.sort(comparator);

        for (Medicamento med : list) {
            gridPane.getChildren().add(createCard(med));
        }

        int total = list.stream().mapToInt(Medicamento::getQuantidade).sum();
        totalLabel.setText("Total: " + total);
    }

    private VBox createCard(Medicamento med) {
        VBox itemBox = new VBox(8);
        itemBox.setAlignment(Pos.CENTER);
        itemBox.setStyle(
                "-fx-background-color: #EEEEEE; " +
                        "-fx-padding: 15; " +
                        "-fx-border-radius: 10; " +
                        "-fx-background-radius: 10; " +
                        "-fx-effect: dropshadow(two-pass-box, rgba(0,0,0,0.2), 5, 0, 0, 3);"
        );

        ImageView imageView = new ImageView(new Image("file:med.png"));
        imageView.setFitWidth(80);
        imageView.setFitHeight(80);

        Label nameLabel = new Label(med.getNome());
        nameLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px; -fx-text-fill: black;");

        Label batchLabel = new Label("Lote: " + med.getBatch());
        batchLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: gray;");

        Label qtyLabel = new Label("Quantidade: " + med.getQuantidade());
        qtyLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: black;");
        if (med.getQuantidade() < med.getAlertThreshold()) qtyLabel.setTextFill(Color.RED);

        Label dateLabel = new Label("Validade: " + med.getValidade().format(formatter));
        dateLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: black;");
        if (!med.getValidade().isBefore(LocalDate.now()) &&
                med.getValidade().isBefore(LocalDate.now().plusDays(7))) dateLabel.setTextFill(Color.ORANGE);

        HBox buttons = new HBox(5);
        buttons.setAlignment(Pos.CENTER);
        Button plusBtn = new Button("+");
        Button minusBtn = new Button("-");
        Button delBtn = new Button("🗑");

        String btnStyle = "-fx-background-color: #FF5C00; -fx-text-fill: black; -fx-font-weight: bold;";
        plusBtn.setStyle(btnStyle);
        minusBtn.setStyle(btnStyle);
        delBtn.setStyle("-fx-background-color: #FF0000; -fx-text-fill: black; -fx-font-weight: bold;");

        plusBtn.setOnAction(e -> { med.setQuantidade(med.getQuantidade()+1); refreshGrid(); });
        minusBtn.setOnAction(e -> { if (med.getQuantidade()>0) med.setQuantidade(med.getQuantidade()-1); refreshGrid(); });
        delBtn.setOnAction(e -> excluirMedicamento(med));

        buttons.getChildren().addAll(plusBtn, minusBtn, delBtn);
        itemBox.getChildren().addAll(imageView, nameLabel, batchLabel, qtyLabel, dateLabel, buttons);

        itemBox.setOnMouseClicked(e -> {
            selectedMed = med;
            refreshGrid();
        });

        if (med == selectedMed) {
            itemBox.setStyle(
                    "-fx-background-color: white; " +
                            "-fx-padding: 15; " +
                            "-fx-border-radius: 10; " +
                            "-fx-background-radius: 10;" +
                            "-fx-effect: dropshadow(two-pass-box, rgba(0,0,0,0.3), 10, 0, 0, 0);"
            );
        }

        return itemBox;
    }

    @FXML
    private void verificarAlertas() {
        List<Medicamento> alertas = medicamentos.stream()
                .filter(m -> m.getQuantidade() < m.getAlertThreshold() || // <-- use threshold
                        m.getValidade().isBefore(LocalDate.now()) ||
                        (m.getValidade().isAfter(LocalDate.now()) && m.getValidade().isBefore(LocalDate.now().plusDays(7))))
                .collect(Collectors.toList());

        if (!alertas.isEmpty()) {
            mostrarNotificacoes(alertas);
        }
    }

    private void mostrarNotificacoes(List<Medicamento> alertas) {
        VBox root = new VBox(10);
        root.setPadding(new Insets(15));
        root.setAlignment(Pos.TOP_CENTER);
        root.setStyle(
                "-fx-background-color: white;" +
                        "-fx-border-radius: 10;" +
                        "-fx-background-radius: 10;" +
                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.15), 8, 0, 0, 2);" +
                        "-fx-focus-color: transparent;" +
                        "-fx-faint-focus-color: transparent;"
        );
        root.setFocusTraversable(false);

        // Title
        Label title = new Label("⚠ Alertas de Medicamentos");
        title.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #FF6600;");
        root.getChildren().add(title);

        Separator separator = new Separator();
        root.getChildren().add(separator);

        // List of alerts
        for (Medicamento med : alertas) {
            List<String> messages = new ArrayList<>();

            // Low stock alert
            if (med.getQuantidade() < med.getAlertThreshold()) {
                messages.add("O medicamento \"" + med.getNome() + "\" está com estoque baixo. Quantidade atual: " + med.getQuantidade() + ".");
            }

            // Expired
            if (med.getValidade().isBefore(LocalDate.now())) {
                messages.add("O medicamento \"" + med.getNome() + "\" está vencido. Venceu em: " + med.getValidade().format(formatter) + ".");
            }
            // About to expire
            else if (med.getValidade().isBefore(LocalDate.now().plusDays(7))) {
                messages.add("O medicamento \"" + med.getNome() + "\" está próximo de vencer. Vence em: " + med.getValidade().format(formatter) + ".");
            }

            // Add each message as a separate label
            for (String msg : messages) {
                Label label = new Label(msg);
                label.setStyle("-fx-font-size: 14px; -fx-text-fill: black;");
                root.getChildren().add(label);
            }
        }

        // Create the stage
        Stage stage = new Stage();
        stage.setTitle("Alertas de Medicamentos");

        // Calculate dynamic height
        int itemHeight = 30;
        int baseHeight = 60;
        int totalHeight = baseHeight + root.getChildren().size() * itemHeight;
        totalHeight = Math.min(totalHeight, 600);

        Scene scene = new Scene(root, 650, totalHeight);
        stage.setScene(scene);
        stage.show();
    }
}
