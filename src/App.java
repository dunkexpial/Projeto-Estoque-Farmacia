import javafx.application.Application;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;

public class App extends Application {

    // Modelo de Medicamento
    public static class Medicamento {
        private final SimpleStringProperty nome;
        private final SimpleIntegerProperty quantidade;
        private final SimpleStringProperty validade;

        public Medicamento(String nome, int quantidade, String validade) {
            this.nome = new SimpleStringProperty(nome);
            this.quantidade = new SimpleIntegerProperty(quantidade);
            this.validade = new SimpleStringProperty(validade);
        }

        public String getNome() { return nome.get(); }
        public void setNome(String value) { nome.set(value); }

        public int getQuantidade() { return quantidade.get(); }
        public void setQuantidade(int value) { quantidade.set(value); }

        public String getValidade() { return validade.get(); }
        public void setValidade(String value) { validade.set(value); }
    }

    private final ObservableList<Medicamento> dados = FXCollections.observableArrayList();

    @SuppressWarnings("deprecation")
    @Override
    public void start(Stage stage) {
        // ---------- TABELA PRINCIPAL ----------
        TableView<Medicamento> table = new TableView<>(dados);

        TableColumn<Medicamento, String> nomeCol = new TableColumn<>("Nome do Remédio");
        nomeCol.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getNome()));

        TableColumn<Medicamento, Number> qtdCol = new TableColumn<>("Quantidade");
        qtdCol.setCellValueFactory(c -> new SimpleIntegerProperty(c.getValue().getQuantidade()));

        TableColumn<Medicamento, String> validadeCol = new TableColumn<>("Validade");
        validadeCol.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getValidade()));

        TableColumn<Medicamento, Void> acoesCol = new TableColumn<>("Ações");
        acoesCol.setCellFactory(col -> new TableCell<>() {
            private final Button maisBtn = new Button("+");
            private final Button menosBtn = new Button("-");
            private final Button excluirBtn = new Button("Excluir");
            private final HBox box = new HBox(5, maisBtn, menosBtn, excluirBtn);

            {
                maisBtn.setOnAction(e -> {
                    Medicamento med = getTableView().getItems().get(getIndex());
                    med.setQuantidade(med.getQuantidade() + 1);
                    table.refresh();
                });

                menosBtn.setOnAction(e -> {
                    Medicamento med = getTableView().getItems().get(getIndex());
                    if (med.getQuantidade() > 0) {
                        med.setQuantidade(med.getQuantidade() - 1);
                        table.refresh();
                    }
                });

                excluirBtn.setOnAction(e -> {
                    Medicamento med = getTableView().getItems().get(getIndex());
                    dados.remove(med);
                });

                box.setAlignment(Pos.CENTER);
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    setGraphic(box);
                }
            }
        });

        table.getColumns().addAll(nomeCol, qtdCol, validadeCol, acoesCol);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        Label titulo = new Label("📋 Estoque de Remédios");
        titulo.setStyle("-fx-font-size: 18px; -fx-text-fill: white; -fx-font-weight: bold;");

        VBox tabelaBox = new VBox(10, titulo, table);
        tabelaBox.setPadding(new Insets(15));
        tabelaBox.setStyle("-fx-background-color: #FF5C00; -fx-background-radius: 10;");
        tabelaBox.setPrefWidth(600);

        // ---------- FORMULÁRIO LATERAL ----------
        Label formTitulo = new Label("+ Novo Medicamento");
        formTitulo.setStyle("-fx-font-size: 16px; -fx-text-fill: white; -fx-font-weight: bold;");

        TextField nomeField = new TextField();
        nomeField.setPromptText("Nome do Remédio");

        TextField qtdField = new TextField();
        qtdField.setPromptText("Quantidade");

        TextField validadeField = new TextField();
        validadeField.setPromptText("Validade");

        Button adicionarBtn = new Button("✔ Adicionar Remédio");
        adicionarBtn.setStyle("-fx-background-color: #FF5C00; -fx-text-fill: white; -fx-font-weight: bold;");

        adicionarBtn.setOnAction(e -> {
            String nome = nomeField.getText();
            String validade = validadeField.getText();
            int qtd = 0;
            try {
                qtd = Integer.parseInt(qtdField.getText());
            } catch (NumberFormatException ex) {
                qtd = 0;
            }
            if (!nome.isEmpty() && !validade.isEmpty()) {
                dados.add(new Medicamento(nome, qtd, validade));
                nomeField.clear();
                qtdField.clear();
                validadeField.clear();
            }
        });

        VBox formBox = new VBox(12, formTitulo, nomeField, qtdField, validadeField, adicionarBtn);
        formBox.setPadding(new Insets(15));
        formBox.setAlignment(Pos.TOP_CENTER);
        formBox.setStyle("-fx-background-color: #0000FF; -fx-background-radius: 10;");
        formBox.setPrefWidth(250);

        // ---------- LAYOUT PRINCIPAL ----------
        HBox root = new HBox(15, tabelaBox, formBox);
        root.setPadding(new Insets(20));
        root.setStyle("-fx-background-color: #f4f4f4;");

        Scene scene = new Scene(root, 900, 500);
        stage.setTitle("Farmácia - Estoque de Remédios");
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
