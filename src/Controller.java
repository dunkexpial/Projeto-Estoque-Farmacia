    import javafx.fxml.FXML;
    import javafx.scene.control.*;
    import javafx.scene.image.Image;
    import javafx.scene.image.ImageView;
    import javafx.scene.layout.TilePane;
    import javafx.scene.layout.VBox;
    import javafx.scene.layout.HBox;
    import javafx.geometry.Pos;
    import javafx.stage.Stage;
    import model.FornecedorDAO;
    import model.LoteDAO;
    import model.LogDAO;
    import model.LoteEstoque;
    import model.Fornecedor;
    import model.Produto;
    import model.LogEntry;
    import javafx.scene.Scene;
    import javafx.geometry.Insets;
    import javafx.application.Platform;

    import java.sql.SQLException;
    import java.time.LocalDate;
    import java.time.LocalDateTime;
    import java.time.format.DateTimeFormatter;
    import java.time.format.DateTimeParseException;
    import java.util.Comparator;
    import java.util.List;
    import java.util.stream.Collectors;

    public class Controller {

        @FXML private TilePane gridPane;
        @FXML private TextField nomeProdutoField;
        @FXML private ComboBox<Fornecedor> fornecedorCombo;
        @FXML private TextField numeroLoteField;
        @FXML private TextField qtdField;
        @FXML private TextField dataEntradaField;
        @FXML private TextField validadeField;
        @FXML private TextField searchField;
        @FXML private ComboBox<String> sortCombo;
        @FXML private ComboBox<String> filterCombo;
        @FXML private Label totalLabel;
        @FXML private Button editarSelecionadoBtn;
        @FXML private Button excluirSelecionadoBtn;
        @FXML private TextField thresholdField;
        @FXML private Button novoFornecedorBtn;
        @FXML private Button adicionarFornecedorBtn;

        @FXML private VBox painelLote;
        @FXML private VBox painelFornecedor;
        @FXML private ToggleButton gerenciarLotesBtn;
        @FXML private ToggleButton gerenciarFornecedoresBtn;

        @FXML private TextField nomeFornecedorField;
        @FXML private TextField cnpjField;
        @FXML private TextField telefoneField;
        @FXML private TextField emailField;
        @FXML private ListView<Fornecedor> fornecedoresListView;

        private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        private final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
        private LoteEstoque editingLote = null;
        private LoteEstoque selectedLote = null;
        private Fornecedor editingFornecedor = null;

        // DAOs
        private FornecedorDAO fornecedorDAO = new FornecedorDAO();
        private LoteDAO loteDAO = new LoteDAO();
        private LogDAO logDAO = new LogDAO();

        @FXML
        public void initialize() {
            gridPane.setHgap(10);
            gridPane.setVgap(10);
            gridPane.setAlignment(Pos.TOP_LEFT);

            atualizarComboFornecedores();
            atualizarListaFornecedores();

            setSupplierFieldsEditable(false);

            if (adicionarFornecedorBtn != null) {
                adicionarFornecedorBtn.setDisable(false);
            }

            fornecedoresListView.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
                if (newSelection != null) {
                    nomeFornecedorField.setText(newSelection.getNome());
                    cnpjField.setText(newSelection.getCnpj());
                    telefoneField.setText(newSelection.getTelefone());
                    emailField.setText(newSelection.getEmail());
                    setSupplierFieldsEditable(false);
                    if (adicionarFornecedorBtn != null) {
                        adicionarFornecedorBtn.setDisable(false);
                    }
                } else {
                    limparCamposFornecedor();
                    setSupplierFieldsEditable(false);
                    if (adicionarFornecedorBtn != null) {
                        adicionarFornecedorBtn.setDisable(false);
                    }
                }
            });

            sortCombo.getItems().addAll("Produto", "Quantidade", "Validade");
            sortCombo.setValue("Produto");
            filterCombo.getItems().addAll("Todos", "Vencidos", "Ativos");
            filterCombo.setValue("Todos");

            searchField.textProperty().addListener((obs, oldV, newV) -> refreshGrid());
            sortCombo.valueProperty().addListener((obs, oldV, newV) -> refreshGrid());
            filterCombo.valueProperty().addListener((obs, oldV, newV) -> refreshGrid());

            configurarFormatacaoData(validadeField);
            configurarFormatacaoData(dataEntradaField);

            editarSelecionadoBtn.setOnAction(e -> {
                if (selectedLote != null) editarLote(selectedLote);
            });

            excluirSelecionadoBtn.setOnAction(e -> {
                if (selectedLote != null) {
                    excluirLote(selectedLote);
                    selectedLote = null;
                    refreshGrid();
                }
            });

            mostrarPainelLote();
            refreshGrid();
            Platform.runLater(() -> verificarEGerarAlertas());
        }

        private void configurarFormatacaoData(TextField field) {
            field.setTextFormatter(new TextFormatter<>(change -> {
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
        }

        // ==================== SISTEMA DE LOG ====================

        private void registrarLog(String tipoOperacao, String entidade, String descricao) {
            LogEntry log = new LogEntry(
                    0,
                    LocalDateTime.now(),
                    tipoOperacao,
                    entidade,
                    descricao,
                    "Sistema"
            );

            try {
                logDAO.inserir(log);
                System.out.println("[LOG SALVO] " + tipoOperacao + " em " + entidade + ": " + descricao);
            } catch (SQLException e) {
                System.err.println("Falha ao salvar log: " + e.getMessage());
                e.printStackTrace();
            }
        }

        @FXML
        public void mostrarHistorico() {
            VBox content = new VBox(10);
            content.getStyleClass().add("notification-content");
            content.setAlignment(Pos.TOP_CENTER);
            content.setPadding(new Insets(15));

            Label title = new Label("📋 Histórico de Alterações");
            title.getStyleClass().add("notification-title");
            content.getChildren().add(title);

            Separator separator = new Separator();
            content.getChildren().add(separator);

            HBox filterBox = new HBox(10);
            filterBox.setAlignment(Pos.CENTER);

            ComboBox<String> tipoFiltro = new ComboBox<>();
            tipoFiltro.getItems().addAll("Todas", "CRIAR", "EDITAR", "EXCLUIR");
            tipoFiltro.setValue("Todas");
            tipoFiltro.setPrefWidth(120);

            ComboBox<String> entidadeFiltro = new ComboBox<>();
            entidadeFiltro.getItems().addAll("Todas", "LOTE", "FORNECEDOR");
            entidadeFiltro.setValue("Todas");
            entidadeFiltro.setPrefWidth(150);

            Button aplicarFiltro = new Button("Aplicar Filtro");
            aplicarFiltro.getStyleClass().addAll("button", "primary-color");

            filterBox.getChildren().addAll(
                    new Label("Operação:"), tipoFiltro,
                    new Label("Entidade:"), entidadeFiltro,
                    aplicarFiltro
            );
            content.getChildren().add(filterBox);

            Separator separator2 = new Separator();
            content.getChildren().add(separator2);

            VBox logsContainer = new VBox(5);
            logsContainer.setAlignment(Pos.TOP_CENTER);

            Runnable atualizarLogs = () -> {
                logsContainer.getChildren().clear();

                try {
                    List<LogEntry> logs = logDAO.listarTodos();

                    List<LogEntry> logsFiltrados = logs.stream()
                            .filter(log -> {
                                boolean tipoMatch = tipoFiltro.getValue().equals("Todas") ||
                                        log.getTipoOperacao().equals(tipoFiltro.getValue());
                                boolean entidadeMatch = entidadeFiltro.getValue().equals("Todas") ||
                                        log.getEntidade().equals(entidadeFiltro.getValue());
                                return tipoMatch && entidadeMatch;
                            })
                            .sorted((l1, l2) -> l2.getDataHora().compareTo(l1.getDataHora()))
                            .collect(Collectors.toList());

                    if (logsFiltrados.isEmpty()) {
                        Label emptyLabel = new Label("Nenhum registro encontrado.");
                        emptyLabel.getStyleClass().add("alert-label");
                        logsContainer.getChildren().add(emptyLabel);
                    } else {
                        for (LogEntry log : logsFiltrados) {
                            VBox logBox = new VBox(3);
                            logBox.getStyleClass().add("alert-container");
                            logBox.setPadding(new Insets(10));

                            HBox header = new HBox(10);
                            header.setAlignment(Pos.CENTER_LEFT);

                            Label tipoLabel = new Label(getIconeOperacao(log.getTipoOperacao()) + " " + log.getTipoOperacao());
                            tipoLabel.getStyleClass().add("text-color");

                            Label dataLabel = new Label(log.getDataHora().format(dateTimeFormatter));
                            dataLabel.getStyleClass().add("gray-text");

                            header.getChildren().addAll(tipoLabel, dataLabel);

                            Label entidadeLabel = new Label("Entidade: " + log.getEntidade());
                            entidadeLabel.getStyleClass().add("gray-text");

                            Label descricaoLabel = new Label(log.getDescricao());
                            descricaoLabel.setWrapText(true);
                            descricaoLabel.getStyleClass().add("alert-label");

                            Label usuarioLabel = new Label("Por: " + log.getUsuarioResponsavel());
                            usuarioLabel.getStyleClass().add("gray-text");

                            logBox.getChildren().addAll(header, entidadeLabel, descricaoLabel, usuarioLabel);
                            logsContainer.getChildren().add(logBox);
                        }
                    }
                } catch (SQLException e) {
                    mostrarAlerta("Erro", "Erro ao carregar logs: " + e.getMessage());
                    e.printStackTrace();
                }
            };

            aplicarFiltro.setOnAction(e -> atualizarLogs.run());
            atualizarLogs.run();

            content.getChildren().add(logsContainer);

            ScrollPane scrollPane = new ScrollPane(content);
            scrollPane.setFitToWidth(true);
            scrollPane.getStyleClass().add("notification-scroll");
            scrollPane.setFocusTraversable(false);

            Stage stage = new Stage();
            stage.setTitle("Histórico de Alterações");

            Scene scene = new Scene(scrollPane, 800, 600);
            scene.getStylesheets().add(getClass().getResource("styles.css").toExternalForm());
            stage.setScene(scene);
            stage.show();
        }

        private String getIconeOperacao(String operacao) {
            switch (operacao) {
                case "CRIAR": return "✅";
                case "EDITAR": return "✏️";
                case "EXCLUIR": return "🗑️";
                default: return "📝";
            }
        }

        // ==================== GERENCIAMENTO DE PAINÉIS ====================

        @FXML
        private void alternarPainel(javafx.event.ActionEvent event) {
            ToggleButton source = (ToggleButton) event.getSource();

            if (source == gerenciarLotesBtn && painelLote.isVisible()) {
                gerenciarLotesBtn.setSelected(true);
                return;
            } else if (source == gerenciarFornecedoresBtn && painelFornecedor.isVisible()) {
                gerenciarFornecedoresBtn.setSelected(true);
                return;
            }

            if (source == gerenciarLotesBtn) {
                mostrarPainelLote();
            } else if (source == gerenciarFornecedoresBtn) {
                mostrarPainelFornecedor();
            }
        }

        private void mostrarPainelLote() {
            if (painelLote != null && painelFornecedor != null) {
                painelLote.setVisible(true);
                painelLote.setManaged(true);
                painelFornecedor.setVisible(false);
                painelFornecedor.setManaged(false);
                gerenciarLotesBtn.setSelected(true);
                gerenciarFornecedoresBtn.setSelected(false);
            }
        }

        private void mostrarPainelFornecedor() {
            if (painelLote != null && painelFornecedor != null) {
                painelLote.setVisible(false);
                painelLote.setManaged(false);
                painelFornecedor.setVisible(true);
                painelFornecedor.setManaged(true);
                gerenciarLotesBtn.setSelected(false);
                gerenciarFornecedoresBtn.setSelected(true);
            }
        }

        // ==================== GERENCIAMENTO DE LOTES ====================

        @FXML
        private void adicionarLote() {
            String nomeProduto = nomeProdutoField.getText().trim();
            Fornecedor fornecedorSelecionado = fornecedorCombo.getValue();
            String numeroLote = numeroLoteField.getText().trim();
            String validadeStr = validadeField.getText();
            String entradaStr = dataEntradaField.getText();

            int qtd = 0;
            int threshold = 5;

            try { qtd = Integer.parseInt(qtdField.getText()); } catch (NumberFormatException ignored) {}
            try { threshold = Integer.parseInt(thresholdField.getText()); } catch (NumberFormatException ignored) {}

            if (nomeProduto.isEmpty()) {
                mostrarAlerta("Erro", "Digite o nome do produto!");
                return;
            }

            if (fornecedorSelecionado == null) {
                mostrarAlerta("Erro", "Selecione um fornecedor!");
                return;
            }

            if (numeroLote.isEmpty() || validadeStr.isEmpty() || entradaStr.isEmpty()) {
                mostrarAlerta("Erro", "Preencha todos os campos!");
                return;
            }

            LocalDate validade = parseDate(validadeStr);
            LocalDate entrada = parseDate(entradaStr);

            if (validade != null && entrada != null) {
                try {
                    if (editingLote != null) {
                        int qtdAnterior = editingLote.getQuantidadeAtual();

                        String descricaoLog = String.format(
                                "Lote '%s' do produto '%s' editado. Quantidade: %d → %d, Validade: %s",
                                numeroLote, nomeProduto, qtdAnterior, qtd, validade.format(formatter)
                        );

                        editingLote.setNumeroLote(numeroLote);
                        editingLote.setQuantidadeAtual(qtd);
                        editingLote.setDataEntrada(entrada);
                        editingLote.setDataValidade(validade);
                        editingLote.setAlertThreshold(threshold);
                        editingLote.setIdFornecedor(fornecedorSelecionado.getIdFornecedor());

                        loteDAO.atualizar(editingLote, nomeProduto);
                        registrarLog("EDITAR", "LOTE", descricaoLog);

                        editingLote = null;
                    } else {
                        LoteEstoque novoLote = new LoteEstoque(
                                0, numeroLote, qtd, entrada, validade,
                                0,
                                fornecedorSelecionado.getIdFornecedor(),
                                threshold
                        );

                        loteDAO.inserir(novoLote, nomeProduto);

                        String descricaoLog = String.format(
                                "Lote '%s' do produto '%s' criado. Quantidade: %d, Fornecedor: %s, Validade: %s",
                                numeroLote, nomeProduto, qtd, fornecedorSelecionado.getNome(), validade.format(formatter)
                        );
                        registrarLog("CRIAR", "LOTE", descricaoLog);
                    }

                    limparCamposLote();
                    refreshGrid();

                } catch (SQLException e) {
                    mostrarAlerta("Erro", "Erro ao salvar lote: " + e.getMessage());
                    e.printStackTrace();
                }
            }
        }

        @FXML
        private void excluirLote(LoteEstoque lote) {
            try {
                String descricaoLog = String.format(
                        "Lote '%s' do produto '%s' excluído. Quantidade: %d",
                        lote.getNumeroLote(), lote.getProduto().getNome(),
                        lote.getQuantidadeAtual()
                );

                loteDAO.excluir(lote.getIdLote());
                registrarLog("EXCLUIR", "LOTE", descricaoLog);

                limparCamposLote();
                refreshGrid();
            } catch (SQLException e) {
                mostrarAlerta("Erro", "Erro ao excluir lote: " + e.getMessage());
                e.printStackTrace();
            }
        }

        private void limparCamposLote() {
            nomeProdutoField.clear();
            fornecedorCombo.setValue(null);
            numeroLoteField.clear();
            qtdField.clear();
            dataEntradaField.clear();
            validadeField.clear();
            thresholdField.clear();
            editingLote = null;
        }

        @FXML
        private void editarLote(LoteEstoque lote) {
            nomeProdutoField.setText(lote.getProduto().getNome());
            fornecedorCombo.setValue(lote.getFornecedor());
            numeroLoteField.setText(lote.getNumeroLote());
            qtdField.setText(String.valueOf(lote.getQuantidadeAtual()));
            dataEntradaField.setText(lote.getDataEntrada().format(formatter));
            validadeField.setText(lote.getDataValidade().format(formatter));
            thresholdField.setText(String.valueOf(lote.getAlertThreshold()));
            editingLote = lote;
            refreshGrid();
        }

        // ==================== GERENCIAMENTO DE FORNECEDORES ====================

        @FXML
        private void adicionarFornecedor() {
            String nome = nomeFornecedorField.getText().trim();
            String cnpj = cnpjField.getText().trim();
            String telefone = telefoneField.getText().trim();
            String email = emailField.getText().trim();

            if (nome.isEmpty()) {
                mostrarAlerta("Erro", "Digite o nome do fornecedor!");
                if (adicionarFornecedorBtn != null) {
                    adicionarFornecedorBtn.setDisable(false);
                }
                return;
            }

            try {
                if (editingFornecedor != null) {
                    String descricaoLog = String.format(
                            "Fornecedor '%s' editado. CNPJ: %s, Telefone: %s, Email: %s",
                            nome, cnpj, telefone, email
                    );

                    editingFornecedor.setNome(nome);
                    editingFornecedor.setCnpj(cnpj);
                    editingFornecedor.setTelefone(telefone);
                    editingFornecedor.setEmail(email);

                    fornecedorDAO.atualizar(editingFornecedor);
                    registrarLog("EDITAR", "FORNECEDOR", descricaoLog);
                    editingFornecedor = null;
                } else {
                    if (!cnpj.isEmpty() && fornecedorDAO.cnpjJaExiste(cnpj, 0)) {
                        mostrarAlerta("Erro", "CNPJ já cadastrado!");
                        if (adicionarFornecedorBtn != null) {
                            adicionarFornecedorBtn.setDisable(false);
                        }
                        return;
                    }

                    Fornecedor novoFornecedor = new Fornecedor(0, nome, cnpj, telefone, email);
                    fornecedorDAO.inserir(novoFornecedor);

                    String descricaoLog = String.format(
                            "Fornecedor '%s' criado. CNPJ: %s, Telefone: %s, Email: %s",
                            nome, cnpj, telefone, email
                    );
                    registrarLog("CRIAR", "FORNECEDOR", descricaoLog);
                }

                limparCamposFornecedor();
                setSupplierFieldsEditable(false);
                atualizarComboFornecedores();
                atualizarListaFornecedores();

            } catch (SQLException e) {
                mostrarAlerta("Erro", "Erro ao salvar fornecedor: " + e.getMessage());
                e.printStackTrace();
            } finally {
                if (adicionarFornecedorBtn != null) {
                    adicionarFornecedorBtn.setDisable(false);
                }
            }
        }

        @FXML
        private void excluirFornecedor() {
            Fornecedor selecionado = fornecedoresListView.getSelectionModel().getSelectedItem();
            if (selecionado == null) {
                mostrarAlerta("Aviso", "Selecione um fornecedor para excluir!");
                return;
            }

            try {
                List<Object[]> lotes = loteDAO.listarTodosComNome();
                boolean temLotes = lotes.stream()
                        .anyMatch(item -> {
                            LoteEstoque lote = (LoteEstoque) item[0];
                            return lote.getIdFornecedor() == selecionado.getIdFornecedor();
                        });

                if (temLotes) {
                    mostrarAlerta("Erro", "Não é possível excluir este fornecedor pois existem lotes vinculados a ele!");
                    return;
                }

                String descricaoLog = String.format(
                        "Fornecedor '%s' excluído. CNPJ: %s",
                        selecionado.getNome(), selecionado.getCnpj()
                );

                fornecedorDAO.excluir(selecionado.getIdFornecedor());
                registrarLog("EXCLUIR", "FORNECEDOR", descricaoLog);

                limparCamposFornecedor();
                setSupplierFieldsEditable(false);
                atualizarComboFornecedores();
                atualizarListaFornecedores();
            } catch (SQLException e) {
                mostrarAlerta("Erro", "Erro ao excluir fornecedor: " + e.getMessage());
                e.printStackTrace();
            }
        }

        @FXML
        private void novoFornecedor() {
            limparCamposFornecedor();
            setSupplierFieldsEditable(true);
            editingFornecedor = null;
            fornecedoresListView.getSelectionModel().clearSelection();
            if (adicionarFornecedorBtn != null) {
                adicionarFornecedorBtn.setDisable(false);
            }
        }

        @FXML
        private void editarFornecedor() {
            Fornecedor selecionado = fornecedoresListView.getSelectionModel().getSelectedItem();
            if (selecionado == null) {
                mostrarAlerta("Aviso", "Selecione um fornecedor para editar!");
                return;
            }

            setSupplierFieldsEditable(true);
            editingFornecedor = selecionado;
            if (adicionarFornecedorBtn != null) {
                adicionarFornecedorBtn.setDisable(false);
            }
        }

        private void limparCamposFornecedor() {
            nomeFornecedorField.clear();
            cnpjField.clear();
            telefoneField.clear();
            emailField.clear();
            editingFornecedor = null;
            fornecedoresListView.getSelectionModel().clearSelection();
            if (adicionarFornecedorBtn != null) {
                adicionarFornecedorBtn.setDisable(false);
            }
        }

        private void atualizarComboFornecedores() {
            if (fornecedorCombo != null) {
                try {
                    List<Fornecedor> fornecedores = fornecedorDAO.listarTodos();
                    fornecedorCombo.setItems(javafx.collections.FXCollections.observableArrayList(fornecedores));
                } catch (SQLException e) {
                    mostrarAlerta("Erro", "Erro ao carregar fornecedores: " + e.getMessage());
                    e.printStackTrace();
                }
            }
        }

        private void atualizarListaFornecedores() {
            if (fornecedoresListView != null) {
                try {
                    List<Fornecedor> fornecedores = fornecedorDAO.listarTodos();
                    fornecedoresListView.setItems(javafx.collections.FXCollections.observableArrayList(fornecedores));
                } catch (SQLException e) {
                    mostrarAlerta("Erro", "Erro ao carregar fornecedores: " + e.getMessage());
                    e.printStackTrace();
                }
            }
        }

        // ==================== MOVIMENTAÇÕES ====================

        @FXML
        public void mostrarAlertas() {
            verificarEGerarAlertas();
        }

        private void incrementarQuantidade(LoteEstoque lote, int quantidade) {
            try {
                int qtdAnterior = lote.getQuantidadeAtual();
                int novaQuantidade = qtdAnterior + quantidade;

                loteDAO.atualizarQuantidade(lote.getIdLote(), novaQuantidade);

                String descricaoLog = String.format(
                        "Quantidade do lote '%s' (%s) incrementada: %d → %d",
                        lote.getNumeroLote(), lote.getProduto().getNome(), qtdAnterior, novaQuantidade
                );
                registrarLog("EDITAR", "LOTE", descricaoLog);

                refreshGrid();
            } catch (SQLException e) {
                mostrarAlerta("Erro", "Erro ao atualizar quantidade: " + e.getMessage());
                e.printStackTrace();
            }
        }

        private void decrementarQuantidade(LoteEstoque lote, int quantidade) {
            if (lote.getQuantidadeAtual() >= quantidade) {
                try {
                    int qtdAnterior = lote.getQuantidadeAtual();
                    int novaQuantidade = qtdAnterior - quantidade;

                    loteDAO.atualizarQuantidade(lote.getIdLote(), novaQuantidade);

                    String descricaoLog = String.format(
                            "Quantidade do lote '%s' (%s) decrementada: %d → %d",
                            lote.getNumeroLote(), lote.getProduto().getNome(), qtdAnterior, novaQuantidade
                    );
                    registrarLog("EDITAR", "LOTE", descricaoLog);

                    refreshGrid();
                } catch (SQLException e) {
                    mostrarAlerta("Erro", "Erro ao atualizar quantidade: " + e.getMessage());
                    e.printStackTrace();
                }
            }
        }

        private void verificarEGerarAlertas() {
            try {
                List<Object[]> lotesComNome = loteDAO.listarTodosComNome();
                List<String> alertas = new java.util.ArrayList<>();
                LocalDate hoje = LocalDate.now();

                for (Object[] item : lotesComNome) {
                    LoteEstoque lote = (LoteEstoque) item[0];
                    String nomeProduto = (String) item[1];

                    if (lote.getQuantidadeAtual() < lote.getAlertThreshold()) {
                        alertas.add("⚠ O insumo \"" + nomeProduto +
                                "\" (Lote: " + lote.getNumeroLote() +
                                ") está com estoque baixo. Quantidade atual: " +
                                lote.getQuantidadeAtual() + ".");
                    }

                    if (lote.getDataValidade().isAfter(hoje) &&
                            lote.getDataValidade().isBefore(hoje.plusDays(7))) {
                        alertas.add("⚠ O insumo \"" + nomeProduto +
                                "\" (Lote: " + lote.getNumeroLote() +
                                ") está próximo de vencer. Vence em: " +
                                lote.getDataValidade().format(formatter) + ".");
                    }

                    if (lote.getDataValidade().isBefore(hoje)) {
                        alertas.add("⚠ O insumo \"" + nomeProduto +
                                "\" (Lote: " + lote.getNumeroLote() +
                                ") está vencido. Venceu em: " +
                                lote.getDataValidade().format(formatter) + ".");
                    }
                }

                if (!alertas.isEmpty()) {
                    mostrarNotificacoes(alertas);
                }
            } catch (SQLException e) {
                mostrarAlerta("Erro", "Erro ao verificar alertas: " + e.getMessage());
                e.printStackTrace();
            }
        }

        // ==================== INTERFACE E UTILIDADES ====================

        private LocalDate parseDate(String str) {
            try {
                return LocalDate.parse(str, formatter);
            } catch (DateTimeParseException e) {
                mostrarAlerta("Erro", "Formato de data inválido! Use dd/mm/yyyy");
                return null;
            }
        }

        private void setSupplierFieldsEditable(boolean editable) {
            nomeFornecedorField.setEditable(editable);
            cnpjField.setEditable(editable);
            telefoneField.setEditable(editable);
            emailField.setEditable(editable);
        }

        private void mostrarAlerta(String titulo, String mensagem) {
            Alert alert = new Alert(Alert.AlertType.ERROR, mensagem);
            alert.setTitle(titulo);
            alert.showAndWait();
        }

        private void refreshGrid() {
            gridPane.getChildren().clear();

            try {
                List<Fornecedor> fornecedores = fornecedorDAO.listarTodos();
                List<Object[]> lotesComNome = loteDAO.listarTodosComNome();

                // Processar lotes e vincular objetos
                List<LoteEstoque> lotes = new java.util.ArrayList<>();
                for (Object[] item : lotesComNome) {
                    LoteEstoque lote = (LoteEstoque) item[0];
                    String nomeProduto = (String) item[1];

                    // Criar produto temporário para exibição
                    Produto produto = new Produto(lote.getIdProduto(), nomeProduto);
                    lote.setProduto(produto);

                    // Vincular fornecedor
                    Fornecedor f = fornecedores.stream()
                            .filter(forn -> forn.getIdFornecedor() == lote.getIdFornecedor())
                            .findFirst()
                            .orElse(null);
                    lote.setFornecedor(f);

                    lotes.add(lote);
                }

                String search = searchField.getText().toLowerCase();
                String filter = filterCombo.getValue();
                String sort = sortCombo.getValue();

                List<LoteEstoque> list = lotes.stream()
                        .filter(l -> l.getProduto().getNome().toLowerCase().contains(search) ||
                                (l.getFornecedor() != null && l.getFornecedor().getNome().toLowerCase().contains(search)))
                        .filter(l -> {
                            if ("Vencidos".equals(filter)) return l.getDataValidade().isBefore(LocalDate.now());
                            if ("Ativos".equals(filter)) return !l.getDataValidade().isBefore(LocalDate.now());
                            return true;
                        })
                        .collect(Collectors.toList());

                Comparator<LoteEstoque> comparator;
                switch (sort) {
                    case "Quantidade": comparator = Comparator.comparingInt(LoteEstoque::getQuantidadeAtual); break;
                    case "Validade": comparator = Comparator.comparing(LoteEstoque::getDataValidade); break;
                    default: comparator = Comparator.comparing(l -> l.getProduto().getNome()); break;
                }
                list.sort(comparator);

                for (LoteEstoque lote : list) {
                    gridPane.getChildren().add(createCard(lote));
                }

                int total = list.stream().mapToInt(LoteEstoque::getQuantidadeAtual).sum();
                totalLabel.setText("Total: " + total);

            } catch (SQLException e) {
                mostrarAlerta("Erro", "Erro ao carregar dados: " + e.getMessage());
                e.printStackTrace();
            }
        }

        private VBox createCard(LoteEstoque lote) {
            VBox itemBox = new VBox(8);
            itemBox.setAlignment(Pos.CENTER);
            itemBox.getStyleClass().add("card");

            Produto produto = lote.getProduto();
            Fornecedor fornecedor = lote.getFornecedor();

            ImageView imageView = new ImageView(new Image(produto.getUrlImagem()));
            imageView.setFitWidth(80);
            imageView.setFitHeight(80);

            Label nameLabel = new Label(produto.getNome());
            nameLabel.getStyleClass().add("name");

            Label batchLabel = new Label("Lote: " + lote.getNumeroLote());
            batchLabel.getStyleClass().add("batch");

            Label fornecedorLabel = new Label("Fornecedor: " + (fornecedor != null ? fornecedor.getNome() : "N/A"));
            fornecedorLabel.getStyleClass().add("fornecedor");

            Label qtyLabel = new Label("Quantidade: " + lote.getQuantidadeAtual());
            qtyLabel.getStyleClass().add("quantity");

            if (lote.getQuantidadeAtual() < lote.getAlertThreshold()) {
                qtyLabel.getStyleClass().add("low");
            } else if (lote.getQuantidadeAtual() == lote.getAlertThreshold()) {
                qtyLabel.getStyleClass().add("warning");
            }

            Label dateLabel = new Label("Validade: " + lote.getDataValidade().format(formatter));
            dateLabel.getStyleClass().add("validity");

            LocalDate hoje = LocalDate.now();
            if (lote.getDataValidade().isBefore(hoje)) {
                dateLabel.getStyleClass().add("expired");
            } else if (lote.getDataValidade().isBefore(hoje.plusDays(7))) {
                dateLabel.getStyleClass().add("near");
            }

            HBox buttons = new HBox(5);
            buttons.setAlignment(Pos.CENTER);
            Button plusBtn = new Button("+");
            plusBtn.getStyleClass().add("button");
            Button minusBtn = new Button("-");
            minusBtn.getStyleClass().add("button");
            Button delBtn = new Button("🗑");
            delBtn.getStyleClass().addAll("button", "delete");

            plusBtn.setOnAction(e -> incrementarQuantidade(lote, 1));
            minusBtn.setOnAction(e -> decrementarQuantidade(lote, 1));
            delBtn.setOnAction(e -> excluirLote(lote));

            buttons.getChildren().addAll(plusBtn, minusBtn, delBtn);
            itemBox.getChildren().addAll(imageView, nameLabel, batchLabel,
                    fornecedorLabel, qtyLabel, dateLabel, buttons);

            itemBox.setOnMouseClicked(e -> {
                if (editingLote == null) {
                    selectedLote = lote;
                    refreshGrid();
                }
            });

            if (lote == editingLote) {
                itemBox.getStyleClass().add("editing");
            } else if (lote == selectedLote) {
                itemBox.getStyleClass().add("selected");
            }

            return itemBox;
        }

        private void mostrarNotificacoes(List<String> alertas) {
            VBox content = new VBox(10);
            content.getStyleClass().add("notification-content");
            content.setAlignment(Pos.TOP_CENTER);

            Label title = new Label("⚠ Alertas de Estoque");
            title.getStyleClass().add("notification-title");
            content.getChildren().add(title);

            Separator separator = new Separator();
            content.getChildren().add(separator);

            for (String mensagem : alertas) {
                VBox alertContainer = new VBox(5);
                alertContainer.getStyleClass().add("alert-container");

                Label label = new Label(mensagem);
                label.getStyleClass().add("alert-label");
                label.setWrapText(true);
                alertContainer.getChildren().add(label);

                content.getChildren().add(alertContainer);
            }

            ScrollPane scrollPane = new ScrollPane(content);
            scrollPane.setFitToWidth(true);
            scrollPane.getStyleClass().add("notification-scroll");
            scrollPane.setFocusTraversable(false);

            Stage stage = new Stage();
            stage.setTitle("Alertas de Estoque");

            int itemHeight = 45;
            int baseHeight = 70;
            int totalHeight = baseHeight + alertas.size() * itemHeight;
            totalHeight = Math.max(totalHeight, 120);
            totalHeight = Math.min(totalHeight, 400);

            Scene scene = new Scene(scrollPane, 700, totalHeight);
            scene.getStylesheets().add(getClass().getResource("styles.css").toExternalForm());
            stage.setScene(scene);
            stage.show();
        }
    }