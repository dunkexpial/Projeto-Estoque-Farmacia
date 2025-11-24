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
import javafx.stage.FileChooser;
import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import model.ImageService;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

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

    @FXML private Button selecionarImagemBtn;
    @FXML private ImageView previewImageView;
    @FXML private Label imagemSelecionadaLabel;

    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
    private LoteEstoque editingLote = null;
    private LoteEstoque selectedLote = null;
    private Fornecedor editingFornecedor = null;
    private String imagemSelecionada = null;

    // DAOs
    private FornecedorDAO fornecedorDAO = new FornecedorDAO();
    private LoteDAO loteDAO = new LoteDAO();
    private LogDAO logDAO = new LogDAO();

    // IN-MEMORY CACHE - Single source of truth
    private List<Fornecedor> fornecedoresCache = new ArrayList<>();
    private List<LoteEstoque> lotesCache = new ArrayList<>();
    private Map<Integer, String> imageUrlsCache = new HashMap<>();
    private boolean cacheLoaded = false;

    @FXML
    public void initialize() {
        gridPane.setHgap(10);
        gridPane.setVgap(10);
        gridPane.setAlignment(Pos.TOP_LEFT);

        // Load all data once at startup
        loadAllDataFromDatabase();

        setupUIComponents();
        setupEventListeners();

        mostrarPainelLote();
        updateGridDisplay();
        Platform.runLater(() -> verificarEGerarAlertas());
    }

    // ==================== DATABASE LOADING (ONLY ONCE AT STARTUP) ====================

    private void loadAllDataFromDatabase() {
        try {
            System.out.println("[CACHE] Carregando dados do banco de dados...");

            // Load fornecedores
            fornecedoresCache = fornecedorDAO.listarTodos();
            System.out.println("[CACHE] Fornecedores carregados: " + fornecedoresCache.size());

            // Load lotes with product names and images
            List<Object[]> lotesComNome = loteDAO.listarTodosComNome();
            lotesCache.clear();
            imageUrlsCache.clear();

            for (Object[] item : lotesComNome) {
                LoteEstoque lote = (LoteEstoque) item[0];
                String nomeProduto = (String) item[1];
                String urlImagem = (String) item[2];

                Produto produto = new Produto(lote.getIdProduto(), nomeProduto);
                produto.setUrlImagem(urlImagem != null ? urlImagem : "med.png");
                lote.setProduto(produto);

                // Link fornecedor from cache
                Fornecedor f = fornecedoresCache.stream()
                        .filter(forn -> forn.getIdFornecedor() == lote.getIdFornecedor())
                        .findFirst()
                        .orElse(null);
                lote.setFornecedor(f);

                lotesCache.add(lote);
                imageUrlsCache.put(lote.getIdLote(), urlImagem);
            }

            System.out.println("[CACHE] Lotes carregados: " + lotesCache.size());
            cacheLoaded = true;

        } catch (SQLException e) {
            mostrarAlerta("Erro", "Erro ao carregar dados do banco: " + e.getMessage());
            e.printStackTrace();
            cacheLoaded = false;
        }
    }

    private void reloadFromDatabase() {
        System.out.println("[CACHE] Recarregando dados do banco após alteração...");
        loadAllDataFromDatabase();
    }

    // ==================== UI SETUP ====================

    private void setupUIComponents() {
        setSupplierFieldsEditable(false);

        if (adicionarFornecedorBtn != null) {
            adicionarFornecedorBtn.setDisable(false);
        }

        sortCombo.getItems().addAll("Produto", "Quantidade", "Validade");
        sortCombo.setValue("Produto");
        filterCombo.getItems().addAll("Todos", "Vencidos", "Ativos");
        filterCombo.setValue("Todos");

        updateFornecedorComboBox();
        updateFornecedorListView();

        configurarFormatacaoData(validadeField);
        configurarFormatacaoData(dataEntradaField);
    }

    private void setupEventListeners() {
        // Fornecedor selection
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

        // Search and filter listeners - only update display, no DB access
        searchField.textProperty().addListener((obs, oldV, newV) -> updateGridDisplay());
        sortCombo.valueProperty().addListener((obs, oldV, newV) -> updateGridDisplay());
        filterCombo.valueProperty().addListener((obs, oldV, newV) -> updateGridDisplay());

        // Button actions
        editarSelecionadoBtn.setOnAction(e -> {
            if (selectedLote != null) editarLote(selectedLote);
        });

        excluirSelecionadoBtn.setOnAction(e -> {
            if (selectedLote != null) {
                excluirLote(selectedLote);
                selectedLote = null;
            }
        });
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
            limparLogsAntigos();
        } catch (SQLException e) {
            System.err.println("Falha ao salvar log: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void limparLogsAntigos() {
        try {
            int totalLogs = logDAO.contarTotal();

            if (totalLogs > 50) {
                int quantidadeParaRemover = totalLogs - 50;

                String sql = "DELETE FROM log_alteracoes WHERE id_log IN " +
                        "(SELECT id_log FROM (SELECT id_log FROM log_alteracoes " +
                        "ORDER BY data_hora ASC LIMIT ?) AS logs_antigos)";

                try (java.sql.Connection conn = model.DataBase.getConnection();
                     java.sql.PreparedStatement stmt = conn.prepareStatement(sql)) {

                    stmt.setInt(1, quantidadeParaRemover);
                    int removidos = stmt.executeUpdate();

                    System.out.println("[LOG] " + removidos + " logs antigos removidos. Total agora: " +
                            (totalLogs - removidos));
                }
            }
        } catch (SQLException e) {
            System.err.println("Falha ao limpar logs antigos: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    public void mostrarHistorico() {
        VBox content = new VBox(10);
        content.getStyleClass().add("notification-content");
        content.setAlignment(Pos.TOP_CENTER);
        content.setPadding(new Insets(15));

        Label title = new Label("Historico de Alteracoes");
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
                new Label("Operacao:"), tipoFiltro,
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
        stage.setTitle("Historico de Alteracoes");

        Scene scene = new Scene(scrollPane, 800, 600);
        scene.getStylesheets().add(getClass().getResource("styles.css").toExternalForm());
        stage.setScene(scene);
        stage.show();
    }

    private String getIconeOperacao(String operacao) {
        switch (operacao) {
            case "CRIAR": return "[+]";
            case "EDITAR": return "[~]";
            case "EXCLUIR": return "[x]";
            default: return "[-]";
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
    private void selecionarImagem() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Selecionar Imagem do Produto");

        FileChooser.ExtensionFilter imageFilter = new FileChooser.ExtensionFilter(
                "Arquivos de Imagem", "*.png", "*.jpg", "*.jpeg", "*.gif", "*.bmp"
        );
        fileChooser.getExtensionFilters().add(imageFilter);

        File selectedFile = fileChooser.showOpenDialog(nomeProdutoField.getScene().getWindow());

        if (selectedFile != null) {
            try {
                imagemSelecionada = ImageService.salvarImagem(selectedFile);

                if (imagemSelecionadaLabel != null) {
                    imagemSelecionadaLabel.setText("[OK] " + selectedFile.getName());
                    imagemSelecionadaLabel.setStyle("-fx-text-fill: green; -fx-font-weight: bold;");
                }

                System.out.println("Imagem salva: " + imagemSelecionada);

            } catch (IOException e) {
                mostrarAlerta("Erro", "Erro ao salvar imagem: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    @FXML
    private void removerImagem() {
        imagemSelecionada = null;

        if (imagemSelecionadaLabel != null) {
            imagemSelecionadaLabel.setText("Nenhuma imagem selecionada");
            imagemSelecionadaLabel.setStyle("-fx-text-fill: #666666; -fx-font-style: italic;");
        }
    }

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
                    // EDITING EXISTING LOTE
                    int qtdAnterior = editingLote.getQuantidadeAtual();
                    String imagemAnterior = editingLote.getProduto().getUrlImagem();
                    String imagemNova = imagemSelecionada != null ? imagemSelecionada : imagemAnterior;

                    StringBuilder descricaoLog = new StringBuilder();
                    descricaoLog.append(String.format("Lote '%s' do produto '%s' editado.", numeroLote, nomeProduto));

                    List<String> mudancas = new ArrayList<>();

                    if (qtdAnterior != qtd) {
                        mudancas.add(String.format("Quantidade: %d -> %d", qtdAnterior, qtd));
                    }

                    if (!editingLote.getDataValidade().equals(validade)) {
                        mudancas.add(String.format("Validade: %s -> %s",
                                editingLote.getDataValidade().format(formatter),
                                validade.format(formatter)));
                    }

                    if (editingLote.getIdFornecedor() != fornecedorSelecionado.getIdFornecedor()) {
                        mudancas.add(String.format("Fornecedor alterado para: %s", fornecedorSelecionado.getNome()));
                    }

                    if (imagemSelecionada != null && !imagemAnterior.equals(imagemNova)) {
                        mudancas.add("Imagem do produto atualizada");
                    }

                    if (!mudancas.isEmpty()) {
                        descricaoLog.append(" Alteracoes: ").append(String.join(", ", mudancas));
                    }

                    // Update in database
                    editingLote.setNumeroLote(numeroLote);
                    editingLote.setQuantidadeAtual(qtd);
                    editingLote.setDataEntrada(entrada);
                    editingLote.setDataValidade(validade);
                    editingLote.setAlertThreshold(threshold);
                    editingLote.setIdFornecedor(fornecedorSelecionado.getIdFornecedor());

                    if (imagemSelecionada != null) {
                        if (imagemAnterior != null && !imagemAnterior.equals("file:med.png") && !imagemAnterior.equals("med.png")) {
                            ImageService.excluirImagem(imagemAnterior);
                        }
                        editingLote.getProduto().setUrlImagem(imagemSelecionada);
                    }

                    loteDAO.atualizar(editingLote, nomeProduto, imagemNova);
                    registrarLog("EDITAR", "LOTE", descricaoLog.toString());

                    // Update in-memory cache
                    editingLote.getProduto().setNome(nomeProduto);
                    editingLote.setFornecedor(fornecedorSelecionado);
                    imageUrlsCache.put(editingLote.getIdLote(), imagemNova);

                    editingLote = null;

                } else {
                    // CREATING NEW LOTE
                    String urlImagem = imagemSelecionada != null ? imagemSelecionada : "med.png";

                    LoteEstoque novoLote = new LoteEstoque(
                            0, numeroLote, qtd, entrada, validade,
                            0,
                            fornecedorSelecionado.getIdFornecedor(),
                            threshold
                    );

                    loteDAO.inserir(novoLote, nomeProduto, urlImagem);

                    String descricaoLog = String.format(
                            "Lote '%s' do produto '%s' criado. Quantidade: %d, Fornecedor: %s, Validade: %s%s",
                            numeroLote, nomeProduto, qtd, fornecedorSelecionado.getNome(),
                            validade.format(formatter),
                            imagemSelecionada != null ? ", com imagem personalizada" : ""
                    );
                    registrarLog("CRIAR", "LOTE", descricaoLog);

                    // Reload from database to get the new ID and add to cache
                    reloadFromDatabase();
                }

                limparCamposLote();
                updateGridDisplay();

            } catch (SQLException e) {
                mostrarAlerta("Erro", "Erro ao salvar lote: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    private void excluirLote(LoteEstoque lote) {
        try {
            String descricaoLog = String.format(
                    "Lote '%s' do produto '%s' excluido. Quantidade: %d",
                    lote.getNumeroLote(), lote.getProduto().getNome(),
                    lote.getQuantidadeAtual()
            );

            if (lote.getProduto() != null) {
                String urlImagem = lote.getProduto().getUrlImagem();
                if (urlImagem != null && !urlImagem.equals("file:med.png") && !urlImagem.equals("med.png")) {
                    ImageService.excluirImagem(urlImagem);
                }
            }

            // Delete from database
            loteDAO.excluir(lote.getIdLote());
            registrarLog("EXCLUIR", "LOTE", descricaoLog);

            // Remove from in-memory cache
            lotesCache.removeIf(l -> l.getIdLote() == lote.getIdLote());
            imageUrlsCache.remove(lote.getIdLote());

            limparCamposLote();
            updateGridDisplay();

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
        selectedLote = null;

        imagemSelecionada = null;
        if (imagemSelecionadaLabel != null) {
            imagemSelecionadaLabel.setText("Nenhuma imagem selecionada");
            imagemSelecionadaLabel.setStyle("-fx-text-fill: #666666; -fx-font-style: italic;");
        }
    }

    @FXML
    private void editarLote(LoteEstoque lote) {
        if (editingLote != null && editingLote != lote) {
            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
            confirm.setTitle("Edicao em Andamento");
            confirm.setHeaderText("Voce esta editando outro lote");
            confirm.setContentText("Deseja cancelar a edicao atual e editar este lote?");

            if (confirm.showAndWait().get() != ButtonType.OK) {
                return;
            }
        }

        nomeProdutoField.setText(lote.getProduto().getNome());
        fornecedorCombo.setValue(lote.getFornecedor());
        numeroLoteField.setText(lote.getNumeroLote());
        qtdField.setText(String.valueOf(lote.getQuantidadeAtual()));
        dataEntradaField.setText(lote.getDataEntrada().format(formatter));
        validadeField.setText(lote.getDataValidade().format(formatter));
        thresholdField.setText(String.valueOf(lote.getAlertThreshold()));
        editingLote = lote;
        selectedLote = null;

        if (lote.getProduto() != null) {
            String urlImagem = lote.getProduto().getUrlImagem();
            if (imagemSelecionadaLabel != null) {
                if (urlImagem != null && !urlImagem.equals("med.png")) {
                    imagemSelecionadaLabel.setText("[OK] Imagem atual: " + urlImagem);
                    imagemSelecionadaLabel.setStyle("-fx-text-fill: green; -fx-font-weight: bold;");
                } else {
                    imagemSelecionadaLabel.setText("[IMG] Usando imagem padrao");
                    imagemSelecionadaLabel.setStyle("-fx-text-fill: #666666; -fx-font-style: italic;");
                }
            }
        }

        System.out.println("Editando lote: " + lote.getNumeroLote());
        updateGridDisplay();
    }

    @FXML
    private void desselecionarLote() {
        selectedLote = null;
        updateCardStyling();
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
                // EDITING EXISTING FORNECEDOR
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

                // Update in-memory cache - object is already updated by reference

                editingFornecedor = null;

            } else {
                // CREATING NEW FORNECEDOR
                if (!cnpj.isEmpty() && fornecedorDAO.cnpjJaExiste(cnpj, 0)) {
                    mostrarAlerta("Erro", "CNPJ ja cadastrado!");
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

                // Reload from database to get the new ID
                reloadFromDatabase();
            }

            limparCamposFornecedor();
            setSupplierFieldsEditable(false);
            updateFornecedorComboBox();
            updateFornecedorListView();

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
            // Check in memory cache first
            boolean temLotes = lotesCache.stream()
                    .anyMatch(lote -> lote.getIdFornecedor() == selecionado.getIdFornecedor());

            if (temLotes) {
                mostrarAlerta("Erro", "Nao e possivel excluir este fornecedor pois existem lotes vinculados a ele!");
                return;
            }

            String descricaoLog = String.format(
                    "Fornecedor '%s' excluido. CNPJ: %s",
                    selecionado.getNome(), selecionado.getCnpj()
            );

            // Delete from database
            fornecedorDAO.excluir(selecionado.getIdFornecedor());
            registrarLog("EXCLUIR", "FORNECEDOR", descricaoLog);

            // Remove from in-memory cache
            fornecedoresCache.removeIf(f -> f.getIdFornecedor() == selecionado.getIdFornecedor());

            limparCamposFornecedor();
            setSupplierFieldsEditable(false);
            updateFornecedorComboBox();
            updateFornecedorListView();

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

    private void updateFornecedorComboBox() {
        if (fornecedorCombo != null) {
            fornecedorCombo.setItems(javafx.collections.FXCollections.observableArrayList(fornecedoresCache));
        }
    }

    private void updateFornecedorListView() {
        if (fornecedoresListView != null) {
            fornecedoresListView.setItems(javafx.collections.FXCollections.observableArrayList(fornecedoresCache));
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

            // Update in database
            loteDAO.atualizarQuantidade(lote.getIdLote(), novaQuantidade);

            // Update in-memory cache
            lote.setQuantidadeAtual(novaQuantidade);

            String descricaoLog = String.format(
                    "Quantidade do lote '%s' (%s) incrementada: %d -> %d",
                    lote.getNumeroLote(), lote.getProduto().getNome(), qtdAnterior, novaQuantidade
            );
            registrarLog("EDITAR", "LOTE", descricaoLog);

            updateGridDisplay();

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

                // Update in database
                loteDAO.atualizarQuantidade(lote.getIdLote(), novaQuantidade);

                // Update in-memory cache
                lote.setQuantidadeAtual(novaQuantidade);

                String descricaoLog = String.format(
                        "Quantidade do lote '%s' (%s) decrementada: %d -> %d",
                        lote.getNumeroLote(), lote.getProduto().getNome(), qtdAnterior, novaQuantidade
                );
                registrarLog("EDITAR", "LOTE", descricaoLog);

                updateGridDisplay();

            } catch (SQLException e) {
                mostrarAlerta("Erro", "Erro ao atualizar quantidade: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    private void verificarEGerarAlertas() {
        List<String> alertas = new java.util.ArrayList<>();
        LocalDate hoje = LocalDate.now();

        for (LoteEstoque lote : lotesCache) {
            String nomeProduto = lote.getProduto().getNome();

            if (lote.getQuantidadeAtual() < lote.getAlertThreshold()) {
                alertas.add("[ALERTA] O insumo \"" + nomeProduto +
                        "\" (Lote: " + lote.getNumeroLote() +
                        ") esta com estoque baixo. Quantidade atual: " +
                        lote.getQuantidadeAtual() + ".");
            }

            if (lote.getDataValidade().isAfter(hoje) &&
                    lote.getDataValidade().isBefore(hoje.plusDays(7))) {
                alertas.add("[ALERTA] O insumo \"" + nomeProduto +
                        "\" (Lote: " + lote.getNumeroLote() +
                        ") esta proximo de vencer. Vence em: " +
                        lote.getDataValidade().format(formatter) + ".");
            }

            if (lote.getDataValidade().isBefore(hoje)) {
                alertas.add("[ALERTA] O insumo \"" + nomeProduto +
                        "\" (Lote: " + lote.getNumeroLote() +
                        ") está vencido. Venceu em: " +
                        lote.getDataValidade().format(formatter) + ".");
            }
        }

        if (!alertas.isEmpty()) {
            mostrarNotificacoes(alertas);
        }
    }

    // ==================== INTERFACE E UTILIDADES ====================

    private LocalDate parseDate(String str) {
        try {
            return LocalDate.parse(str, formatter);
        } catch (DateTimeParseException e) {
            mostrarAlerta("Erro", "Formato de data invalido! Use dd/mm/yyyy");
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

    // ==================== DISPLAY UPDATE (NO DATABASE ACCESS) ====================

    /**
     * Updates the grid display using only in-memory cache data.
     * NO DATABASE ACCESS - pure memory operations for instant response.
     */
    private void updateGridDisplay() {
        gridPane.getChildren().clear();

        String search = searchField.getText().toLowerCase();
        String filter = filterCombo.getValue();
        String sort = sortCombo.getValue();

        // Filter and sort from memory cache
        List<LoteEstoque> list = lotesCache.stream()
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
    }

    /**
     * Updates only the visual styling of cards without rebuilding the entire grid.
     * Used for selection changes - FASTEST possible update, no DOM manipulation.
     */
    private void updateCardStyling() {
        for (javafx.scene.Node node : gridPane.getChildren()) {
            if (node instanceof VBox) {
                VBox card = (VBox) node;
                LoteEstoque cardLote = (LoteEstoque) card.getUserData();

                if (cardLote == null) continue;

                card.getStyleClass().removeAll("selected", "editing");

                if (editingLote != null && cardLote.getIdLote() == editingLote.getIdLote()) {
                    card.getStyleClass().add("editing");
                } else if (selectedLote != null && cardLote.getIdLote() == selectedLote.getIdLote()) {
                    card.getStyleClass().add("selected");
                }
            }
        }
    }

    private VBox createCard(LoteEstoque lote) {
        VBox itemBox = new VBox(8);
        itemBox.setAlignment(Pos.CENTER);
        itemBox.getStyleClass().add("card");

        // Store lote reference for quick access
        itemBox.setUserData(lote);

        Produto produto = lote.getProduto();
        Fornecedor fornecedor = lote.getFornecedor();

        String imagePath = ImageService.getImagePath(produto.getUrlImagem());
        ImageView imageView = new ImageView(new Image(imagePath));
        imageView.setFitWidth(80);
        imageView.setFitHeight(80);
        imageView.setPreserveRatio(true);

        Label nameLabel = new Label(produto.getNome());
        nameLabel.getStyleClass().add("name");
        nameLabel.setWrapText(true);
        nameLabel.setMaxWidth(150);
        nameLabel.setAlignment(Pos.CENTER);

        Label batchLabel = new Label("Lote: " + lote.getNumeroLote());
        batchLabel.getStyleClass().add("batch");

        Label fornecedorLabel = new Label("Fornecedor: " + (fornecedor != null ? fornecedor.getNome() : "N/A"));
        fornecedorLabel.getStyleClass().add("fornecedor");
        fornecedorLabel.setWrapText(true);
        fornecedorLabel.setMaxWidth(150);
        fornecedorLabel.setAlignment(Pos.CENTER);

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
        Button delBtn = new Button("x");
        delBtn.getStyleClass().addAll("button", "delete");

        plusBtn.setOnAction(e -> {
            e.consume();
            incrementarQuantidade(lote, 1);
        });

        minusBtn.setOnAction(e -> {
            e.consume();
            decrementarQuantidade(lote, 1);
        });

        delBtn.setOnAction(e -> {
            e.consume();
            excluirLote(lote);
        });

        buttons.getChildren().addAll(plusBtn, minusBtn, delBtn);
        itemBox.getChildren().addAll(imageView, nameLabel, batchLabel,
                fornecedorLabel, qtyLabel, dateLabel, buttons);

        // OPTIMIZED CLICK HANDLER - Only updates styling, no database access
        itemBox.setCursor(javafx.scene.Cursor.HAND);

        itemBox.setOnMouseClicked(e -> {
            if (editingLote != null && editingLote.getIdLote() != lote.getIdLote()) {
                mostrarAlerta("Aviso", "Termine de editar o lote atual antes de selecionar outro!");
                return;
            }

            if (selectedLote != null && selectedLote.getIdLote() == lote.getIdLote()) {
                selectedLote = null;
                System.out.println("Lote desselecionado: " + lote.getNumeroLote());
            } else {
                selectedLote = lote;
                System.out.println("Lote selecionado: " + lote.getNumeroLote() + " - " + produto.getNome());
            }

            // INSTANT UPDATE - only changes CSS classes, no database or DOM rebuild
            updateCardStyling();
        });

        // Apply initial styling
        if (editingLote != null && lote.getIdLote() == editingLote.getIdLote()) {
            itemBox.getStyleClass().add("editing");
        } else if (selectedLote != null && lote.getIdLote() == selectedLote.getIdLote()) {
            itemBox.getStyleClass().add("selected");
        }

        return itemBox;
    }

    private void mostrarNotificacoes(List<String> alertas) {
        VBox content = new VBox(10);
        content.getStyleClass().add("notification-content");
        content.setAlignment(Pos.TOP_CENTER);

        Label title = new Label("Alertas de Estoque");
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