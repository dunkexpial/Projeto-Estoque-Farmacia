import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.TilePane;
import javafx.scene.layout.VBox;
import javafx.scene.layout.HBox;
import javafx.geometry.Pos;
import javafx.scene.paint.Color;
import javafx.util.StringConverter;

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
    @FXML private TextField qtdField;
    @FXML private TextField validadeField;
    @FXML private TextField searchField;
    @FXML private ComboBox<String> sortCombo;
    @FXML private ComboBox<String> filterCombo;
    @FXML private Label totalLabel;
    @FXML private Button editarSelecionadoBtn;
    @FXML private Button excluirSelecionadoBtn;

    private final List<Medicamento> medicamentos = new ArrayList<>();
    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private Medicamento editingMed = null; // reference to currently edited medicine
    private Medicamento selectedMed = null;

    @FXML
    public void initialize() {
        gridPane.setHgap(10);
        gridPane.setVgap(10);

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
                selectedMed = null; // clear selection
                refreshGrid();
            }
        });
    }

    @FXML
    private void adicionarMedicamento() {
        String nome = nomeField.getText();
        String validadeStr = validadeField.getText();
        int qtd = 0;

        try { qtd = Integer.parseInt(qtdField.getText()); } catch (NumberFormatException ignored) {}

        if (!nome.isEmpty() && !validadeStr.isEmpty()) {
            LocalDate validade = parseDate(validadeStr);
            if (validade != null) {
                if (editingMed != null) {
                    // Update existing medicine
                    editingMed.setNome(nome);
                    editingMed.setQuantidade(qtd);
                    editingMed.setValidade(validade);
                    editingMed = null;
                } else {
                    // Add new medicine
                    Medicamento med = new Medicamento(nome, qtd, validade);
                    medicamentos.add(med);
                }
                refreshGrid();
                nomeField.clear();
                qtdField.clear();
                validadeField.clear();
            }
        }
    }

    @FXML
    private void editarMedicamento(Medicamento med) {
        nomeField.setText(med.getNome());
        qtdField.setText(String.valueOf(med.getQuantidade()));
        validadeField.setText(med.getValidade().format(formatter));
        editingMed = med; // set reference for updating
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
            Alert alert = new Alert(Alert.AlertType.ERROR, "Formato de data inválido! Use dd/MM/yyyy");
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

        // Sorting
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
                "-fx-background-color: white; " +
                        "-fx-padding: 15; " +
                        "-fx-border-radius: 10; " +
                        "-fx-background-radius: 10; " +
                        "-fx-effect: dropshadow(two-pass-box, rgba(0,0,0,0.2), 5, 0, 0, 3);"
        );

        ImageView imageView = new ImageView(new Image("file:med.png"));
        imageView.setFitWidth(80);
        imageView.setFitHeight(80);

        Label nameLabel = new Label(med.getNome());
        nameLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");

        Label qtyLabel = new Label("Qtd: " + med.getQuantidade());
        qtyLabel.setStyle("-fx-font-size: 13px;");
        if (med.getQuantidade() < 5) qtyLabel.setTextFill(Color.RED);

        Label dateLabel = new Label("Validade: " + med.getValidade().format(formatter));
        dateLabel.setStyle("-fx-font-size: 13px;");
        if (!med.getValidade().isBefore(LocalDate.now()) &&
                med.getValidade().isBefore(LocalDate.now().plusDays(7))) dateLabel.setTextFill(Color.ORANGE);

        HBox buttons = new HBox(5);
        buttons.setAlignment(Pos.CENTER);
        Button plusBtn = new Button("+");
        Button minusBtn = new Button("-");
        Button editBtn = new Button("✎");
        Button delBtn = new Button("🗑");

        String btnStyle = "-fx-background-color: #FF5C00; -fx-text-fill: white; -fx-font-weight: bold;";
        plusBtn.setStyle(btnStyle);
        minusBtn.setStyle(btnStyle);
        editBtn.setStyle("-fx-background-color: #FFA500; -fx-text-fill: white; -fx-font-weight: bold;");
        delBtn.setStyle("-fx-background-color: #FF0000; -fx-text-fill: white; -fx-font-weight: bold;");

        plusBtn.setOnAction(e -> { med.setQuantidade(med.getQuantidade()+1); refreshGrid(); });
        minusBtn.setOnAction(e -> { if (med.getQuantidade()>0) med.setQuantidade(med.getQuantidade()-1); refreshGrid(); });
        editBtn.setOnAction(e -> editarMedicamento(med));
        delBtn.setOnAction(e -> excluirMedicamento(med));

        buttons.getChildren().addAll(plusBtn, minusBtn, editBtn, delBtn);
        itemBox.getChildren().addAll(imageView, nameLabel, qtyLabel, dateLabel, buttons);

        itemBox.setOnMouseClicked(e -> {
            selectedMed = med;
            refreshGrid(); // redraw to highlight selected card
        });

        // Optional: highlight if selected
        if (med == selectedMed) {
            itemBox.setStyle(
                    "-fx-background-color: #FFD700; " +
                            "-fx-padding: 15; " +
                            "-fx-border-radius: 10; " +
                            "-fx-background-radius: 10;" +
                            "-fx-effect: dropshadow(two-pass-box, rgba(0,0,0,0.3), 10, 0, 0, 0);"
            );
        }

        return itemBox;
    }

    public static class Medicamento {
        private String nome;
        private int quantidade;
        private LocalDate validade;

        public Medicamento(String nome, int quantidade, LocalDate validade) {
            this.nome = nome;
            this.quantidade = quantidade;
            this.validade = validade;
        }

        public String getNome() { return nome; }
        public void setNome(String nome) { this.nome = nome; }
        public int getQuantidade() { return quantidade; }
        public void setQuantidade(int quantidade) { this.quantidade = quantidade; }
        public LocalDate getValidade() { return validade; }
        public void setValidade(LocalDate validade) { this.validade = validade; }
    }
}
