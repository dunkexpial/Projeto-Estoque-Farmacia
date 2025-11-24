import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.TilePane;
import javafx.scene.layout.VBox;
import javafx.scene.layout.HBox;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.Region;
import javafx.scene.layout.Priority;
import javafx.geometry.Pos;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.scene.Scene;
import javafx.geometry.Insets;
import javafx.application.Platform;
import javafx.stage.FileChooser;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.TextAlignment;
import model.FornecedorDAO;
import model.LoteDAO;
import model.LogDAO;
import model.LoteEstoque;
import model.Fornecedor;
import model.Produto;
import model.LogEntry;
import model.ImageService;
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class Controller {

    // Componentes FXML
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
    @FXML private Button themeToggleBtn;
    @FXML private HBox titleBar;
    @FXML private BorderPane mainPane;

    // Formatadores e variáveis de estado
    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
    private LoteEstoque editingLote = null;
    private LoteEstoque selectedLote = null;
    private Fornecedor editingFornecedor = null;
    private String imagemSelecionada = null;

    // DAOs para acesso ao banco
    private FornecedorDAO fornecedorDAO = new FornecedorDAO();
    private LoteDAO loteDAO = new LoteDAO();
    private LogDAO logDAO = new LogDAO();

    // Cache em memória - fonte única de verdade para performance
    private List<Fornecedor> fornecedoresCache = new ArrayList<>();
    private List<LoteEstoque> lotesCache = new ArrayList<>();
    private Map<Integer, String> imageUrlsCache = new HashMap<>();
    private boolean cacheLoaded = false;

    private boolean isDarkTheme = true;
    private Stage stage;
    private boolean isMaximized = false;
    private boolean isResizing = false;
    private double resizeStartX = 0;
    private double resizeStartY = 0;
    private double resizeStartWidth = 0;
    private double resizeStartHeight = 0;
    private String resizeDirection = "";
    private static final int RESIZE_MARGIN = 10;
    private boolean isDraggingWindow = false;
    private javafx.scene.layout.Pane resizeOverlay;
    private List<Stage> childStages = new ArrayList<>();
    private double beforeMaxX;
    private double beforeMaxY;
    private double beforeMaxWidth;
    private double beforeMaxHeight;

    @FXML
    public void initialize() {
        gridPane.setHgap(10);
        gridPane.setVgap(10);
        gridPane.setAlignment(Pos.TOP_LEFT);

        // Carrega todos os dados do banco apenas uma vez na inicialização
        loadAllDataFromDatabase();

        setupUIComponents();
        setupEventListeners();

        mostrarPainelLote();
        updateGridDisplay();
        Platform.runLater(() -> verificarEGerarAlertas());
    }

    public void setStage(Stage stage) {
        this.stage = stage;

        Platform.runLater(() -> {
            setupRoundedCorners();
            setupWindowDragging();
            setupWindowResize();

            if (isMaximized) {
                updateRoundedCorners();
            }
        });
    }

    private void setupWindowResize() {
        Scene scene = stage.getScene();
        if (scene == null) return;

        final double[] startX = {0};
        final double[] startY = {0};
        final double[] startStageX = {0};
        final double[] startStageY = {0};
        final double[] startWidth = {0};
        final double[] startHeight = {0};

        // Usa addEventFilter apenas quando está nas bordas
        scene.addEventFilter(javafx.scene.input.MouseEvent.MOUSE_MOVED, event -> {
            if (isMaximized || isResizing || isDraggingWindow) return;

            double mouseX = event.getSceneX();
            double mouseY = event.getSceneY();
            double width = scene.getWidth();
            double height = scene.getHeight();

            boolean onLeft = mouseX < RESIZE_MARGIN;
            boolean onRight = mouseX > width - RESIZE_MARGIN;
            boolean onTop = mouseY < RESIZE_MARGIN;
            boolean onBottom = mouseY > height - RESIZE_MARGIN;

            // Só define cursor de resize se estiver nas bordas
            if ((onLeft || onRight) || (onTop || onBottom)) {
                if ((onLeft || onRight) && (onTop || onBottom)) {
                    if (onLeft && onTop) {
                        scene.setCursor(javafx.scene.Cursor.NW_RESIZE);
                        resizeDirection = "NW";
                    } else if (onRight && onTop) {
                        scene.setCursor(javafx.scene.Cursor.NE_RESIZE);
                        resizeDirection = "NE";
                    } else if (onLeft && onBottom) {
                        scene.setCursor(javafx.scene.Cursor.SW_RESIZE);
                        resizeDirection = "SW";
                    } else if (onRight && onBottom) {
                        scene.setCursor(javafx.scene.Cursor.SE_RESIZE);
                        resizeDirection = "SE";
                    }
                } else if (onLeft) {
                    scene.setCursor(javafx.scene.Cursor.W_RESIZE);
                    resizeDirection = "W";
                } else if (onRight) {
                    scene.setCursor(javafx.scene.Cursor.E_RESIZE);
                    resizeDirection = "E";
                } else if (onTop) {
                    scene.setCursor(javafx.scene.Cursor.N_RESIZE);
                    resizeDirection = "N";
                } else if (onBottom) {
                    scene.setCursor(javafx.scene.Cursor.S_RESIZE);
                    resizeDirection = "S";
                }
            } else {
                // Fora das bordas - limpa direção e deixa cursor padrão
                if (!resizeDirection.isEmpty() && !isResizing) {
                    scene.setCursor(javafx.scene.Cursor.DEFAULT);
                    resizeDirection = "";
                }
            }
        });

        scene.addEventFilter(javafx.scene.input.MouseEvent.MOUSE_PRESSED, event -> {
            // Só inicia resize se tiver uma direção definida (está na borda)
            if (!resizeDirection.isEmpty() && !isMaximized && !isDraggingWindow) {
                isResizing = true;
                startX[0] = event.getScreenX();
                startY[0] = event.getScreenY();
                startStageX[0] = stage.getX();
                startStageY[0] = stage.getY();
                startWidth[0] = stage.getWidth();
                startHeight[0] = stage.getHeight();
                event.consume(); // Consome o evento para não passar para outros elementos
            }
        });

        scene.addEventFilter(javafx.scene.input.MouseEvent.MOUSE_DRAGGED, event -> {
            // Só faz resize se isResizing estiver true
            if (!isResizing || isMaximized) return;

            double deltaX = event.getScreenX() - startX[0];
            double deltaY = event.getScreenY() - startY[0];

            double minWidth = 800;
            double minHeight = 600;

            switch (resizeDirection) {
                case "E":
                    stage.setWidth(Math.max(minWidth, startWidth[0] + deltaX));
                    break;

                case "W":
                    double newWidthW = startWidth[0] - deltaX;
                    if (newWidthW >= minWidth) {
                        stage.setX(startStageX[0] + deltaX);
                        stage.setWidth(newWidthW);
                    }
                    break;

                case "S":
                    stage.setHeight(Math.max(minHeight, startHeight[0] + deltaY));
                    break;

                case "N":
                    double newHeightN = startHeight[0] - deltaY;
                    if (newHeightN >= minHeight) {
                        stage.setY(startStageY[0] + deltaY);
                        stage.setHeight(newHeightN);
                    }
                    break;

                case "SE":
                    stage.setWidth(Math.max(minWidth, startWidth[0] + deltaX));
                    stage.setHeight(Math.max(minHeight, startHeight[0] + deltaY));
                    break;

                case "SW":
                    double newWidthSW = startWidth[0] - deltaX;
                    if (newWidthSW >= minWidth) {
                        stage.setX(startStageX[0] + deltaX);
                        stage.setWidth(newWidthSW);
                    }
                    stage.setHeight(Math.max(minHeight, startHeight[0] + deltaY));
                    break;

                case "NE":
                    stage.setWidth(Math.max(minWidth, startWidth[0] + deltaX));
                    double newHeightNE = startHeight[0] - deltaY;
                    if (newHeightNE >= minHeight) {
                        stage.setY(startStageY[0] + deltaY);
                        stage.setHeight(newHeightNE);
                    }
                    break;

                case "NW":
                    double newWidthNW = startWidth[0] - deltaX;
                    double newHeightNW = startHeight[0] - deltaY;
                    if (newWidthNW >= minWidth) {
                        stage.setX(startStageX[0] + deltaX);
                        stage.setWidth(newWidthNW);
                    }
                    if (newHeightNW >= minHeight) {
                        stage.setY(startStageY[0] + deltaY);
                        stage.setHeight(newHeightNW);
                    }
                    break;
            }

            event.consume(); // Consome o evento durante o resize
        });

        scene.addEventFilter(javafx.scene.input.MouseEvent.MOUSE_RELEASED, event -> {
            if (isResizing) {
                isResizing = false;
                resizeDirection = "";
                scene.setCursor(javafx.scene.Cursor.DEFAULT);
                event.consume();
            }
        });
    }

    private void createResizeBorders() {
        Scene scene = stage.getScene();
        if (scene == null) return;

        final double[] startX = {0};
        final double[] startY = {0};
        final double[] startStageX = {0};
        final double[] startStageY = {0};
        final double[] startWidth = {0};
        final double[] startHeight = {0};

        // Listener global na Scene para detectar quando está nas bordas
        scene.addEventFilter(javafx.scene.input.MouseEvent.MOUSE_MOVED, event -> {
            if (isMaximized || isResizing) return;

            double mouseX = event.getSceneX();
            double mouseY = event.getSceneY();
            double width = scene.getWidth();
            double height = scene.getHeight();

            boolean onLeft = mouseX < RESIZE_MARGIN;
            boolean onRight = mouseX > width - RESIZE_MARGIN;
            boolean onTop = mouseY < RESIZE_MARGIN;
            boolean onBottom = mouseY > height - RESIZE_MARGIN;

            if ((onLeft || onRight) || (onTop || onBottom)) {
                // Está na borda - define cursor e direção
                if ((onLeft || onRight) && (onTop || onBottom)) {
                    if (onLeft && onTop) {
                        scene.setCursor(javafx.scene.Cursor.NW_RESIZE);
                        resizeDirection = "NW";
                    } else if (onRight && onTop) {
                        scene.setCursor(javafx.scene.Cursor.NE_RESIZE);
                        resizeDirection = "NE";
                    } else if (onLeft && onBottom) {
                        scene.setCursor(javafx.scene.Cursor.SW_RESIZE);
                        resizeDirection = "SW";
                    } else if (onRight && onBottom) {
                        scene.setCursor(javafx.scene.Cursor.SE_RESIZE);
                        resizeDirection = "SE";
                    }
                } else if (onLeft) {
                    scene.setCursor(javafx.scene.Cursor.W_RESIZE);
                    resizeDirection = "W";
                } else if (onRight) {
                    scene.setCursor(javafx.scene.Cursor.E_RESIZE);
                    resizeDirection = "E";
                } else if (onTop) {
                    scene.setCursor(javafx.scene.Cursor.N_RESIZE);
                    resizeDirection = "N";
                } else if (onBottom) {
                    scene.setCursor(javafx.scene.Cursor.S_RESIZE);
                    resizeDirection = "S";
                }
            } else {
                scene.setCursor(javafx.scene.Cursor.DEFAULT);
                resizeDirection = "";
            }
        });

        scene.addEventFilter(javafx.scene.input.MouseEvent.MOUSE_PRESSED, event -> {
            if (!resizeDirection.isEmpty() && !isMaximized) {
                isResizing = true;
                isDraggingWindow = false;
                startX[0] = event.getScreenX();
                startY[0] = event.getScreenY();
                startStageX[0] = stage.getX();
                startStageY[0] = stage.getY();
                startWidth[0] = stage.getWidth();
                startHeight[0] = stage.getHeight();
                event.consume();
            }
        });

        scene.addEventFilter(javafx.scene.input.MouseEvent.MOUSE_DRAGGED, event -> {
            if (!isResizing || isMaximized) return;

            double deltaX = event.getScreenX() - startX[0];
            double deltaY = event.getScreenY() - startY[0];

            double minWidth = 800;
            double minHeight = 600;

            switch (resizeDirection) {
                case "E":
                    stage.setWidth(Math.max(minWidth, startWidth[0] + deltaX));
                    break;

                case "W":
                    double newWidthW = startWidth[0] - deltaX;
                    if (newWidthW >= minWidth) {
                        stage.setX(startStageX[0] + deltaX);
                        stage.setWidth(newWidthW);
                    }
                    break;

                case "S":
                    stage.setHeight(Math.max(minHeight, startHeight[0] + deltaY));
                    break;

                case "N":
                    double newHeightN = startHeight[0] - deltaY;
                    if (newHeightN >= minHeight) {
                        stage.setY(startStageY[0] + deltaY);
                        stage.setHeight(newHeightN);
                    }
                    break;

                case "SE":
                    stage.setWidth(Math.max(minWidth, startWidth[0] + deltaX));
                    stage.setHeight(Math.max(minHeight, startHeight[0] + deltaY));
                    break;

                case "SW":
                    double newWidthSW = startWidth[0] - deltaX;
                    if (newWidthSW >= minWidth) {
                        stage.setX(startStageX[0] + deltaX);
                        stage.setWidth(newWidthSW);
                    }
                    stage.setHeight(Math.max(minHeight, startHeight[0] + deltaY));
                    break;

                case "NE":
                    stage.setWidth(Math.max(minWidth, startWidth[0] + deltaX));
                    double newHeightNE = startHeight[0] - deltaY;
                    if (newHeightNE >= minHeight) {
                        stage.setY(startStageY[0] + deltaY);
                        stage.setHeight(newHeightNE);
                    }
                    break;

                case "NW":
                    double newWidthNW = startWidth[0] - deltaX;
                    double newHeightNW = startHeight[0] - deltaY;
                    if (newWidthNW >= minWidth) {
                        stage.setX(startStageX[0] + deltaX);
                        stage.setWidth(newWidthNW);
                    }
                    if (newHeightNW >= minHeight) {
                        stage.setY(startStageY[0] + deltaY);
                        stage.setHeight(newHeightNW);
                    }
                    break;
            }

            event.consume();
        });

        scene.addEventFilter(javafx.scene.input.MouseEvent.MOUSE_RELEASED, event -> {
            if (isResizing) {
                isResizing = false;
                resizeDirection = "";
                event.consume();
            }
        });
    }

    private void setupWindowDragging() {
        final double[] xOffset = {0};
        final double[] yOffset = {0};

        titleBar.setOnMousePressed(event -> {
            if (!isResizing) { // Só permite drag se não estiver resizing
                isDraggingWindow = true;
                xOffset[0] = event.getSceneX();
                yOffset[0] = event.getSceneY();
            }
        });

        titleBar.setOnMouseDragged(event -> {
            if (!isMaximized && isDraggingWindow && !isResizing) {
                stage.setX(event.getScreenX() - xOffset[0]);
                stage.setY(event.getScreenY() - yOffset[0]);
            }
        });

        titleBar.setOnMouseReleased(event -> {
            isDraggingWindow = false;
        });

        // Duplo clique para maximizar/restaurar
        titleBar.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2 && !isResizing) {
                maximizeWindow();
            }
        });
    }

    @FXML
    private void minimizeWindow() {
        if (stage != null) {
            stage.setIconified(true);
        }
    }

    @FXML
    public void maximizeWindow() {
        if (stage != null) {
            if (isMaximized) {
                // RESTAURAR TAMANHO E POSIÇÃO ANTERIORES
                stage.setX(beforeMaxX);
                stage.setY(beforeMaxY);
                stage.setWidth(beforeMaxWidth);
                stage.setHeight(beforeMaxHeight);
                isMaximized = false;
                updateRoundedCorners(); // ATUALIZA BORDAS
            } else {
                // GUARDAR TAMANHO E POSIÇÃO ATUAIS
                beforeMaxX = stage.getX();
                beforeMaxY = stage.getY();
                beforeMaxWidth = stage.getWidth();
                beforeMaxHeight = stage.getHeight();

                // MAXIMIZAR RESPEITANDO A BARRA DE TAREFAS
                javafx.stage.Screen screen = javafx.stage.Screen.getPrimary();
                javafx.geometry.Rectangle2D visualBounds = screen.getVisualBounds();

                stage.setX(visualBounds.getMinX());
                stage.setY(visualBounds.getMinY());
                stage.setWidth(visualBounds.getWidth());
                stage.setHeight(visualBounds.getHeight());

                isMaximized = true;
                updateRoundedCorners(); // ATUALIZA BORDAS
            }
        }
    }

    private void updateRoundedCorners() {
        Scene scene = stage.getScene();
        if (scene == null || scene.getRoot() == null) return;

        StackPane root = (StackPane) scene.getRoot();

        if (isMaximized) {
            root.setClip(null);
            if (mainPane != null) {
                mainPane.setStyle("-fx-background-radius: 0; -fx-border-radius: 0;");
            }
        } else {
            javafx.scene.shape.Rectangle clip = new javafx.scene.shape.Rectangle();
            clip.setArcWidth(24);
            clip.setArcHeight(24);
            clip.widthProperty().bind(root.widthProperty());
            clip.heightProperty().bind(root.heightProperty());
            root.setClip(clip);
            // Restaura border-radius do CSS
            if (mainPane != null) {
                mainPane.setStyle("-fx-background-radius: 12; -fx-border-radius: 12;");
            }
        }
    }

    @FXML
    private void closeWindow() {
        if (stage != null) {
            stage.close();
        }
    }

    private void setupRoundedCorners() {
        Scene scene = stage.getScene();
        if (scene == null || scene.getRoot() == null) return;

        StackPane root = (StackPane) scene.getRoot();

        javafx.scene.shape.Rectangle clip = new javafx.scene.shape.Rectangle();
        clip.setArcWidth(24);
        clip.setArcHeight(24);

        clip.widthProperty().bind(root.widthProperty());
        clip.heightProperty().bind(root.heightProperty());

        root.setClip(clip);
    }

    // ==================== CARREGAMENTO DO BANCO (APENAS UMA VEZ NA INICIALIZAÇÃO) ====================

    private void loadAllDataFromDatabase() {
        try {
            System.out.println("[CACHE] Carregando dados do banco de dados...");

            // Carrega fornecedores
            fornecedoresCache = fornecedorDAO.listarTodos();
            System.out.println("[CACHE] Fornecedores carregados: " + fornecedoresCache.size());

            // Carrega lotes com nomes de produtos e imagens
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

                // Vincula fornecedor do cache
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

    // Recarrega dados após operações de alteração no banco
    private void reloadFromDatabase() {
        System.out.println("[CACHE] Recarregando dados do banco após alteração...");
        loadAllDataFromDatabase();
    }

    // ==================== CONFIGURAÇÃO DA UI ====================

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
        configurarFormatacaoTelefone(telefoneField);
    }

    @FXML
    private void toggleTheme() {
        Scene scene = gridPane.getScene();

        if (scene != null) {
            scene.getStylesheets().clear();

            if (isDarkTheme) {
                scene.getStylesheets().add(getClass().getResource("styles-light.css").toExternalForm());
                isDarkTheme = false;
                if (themeToggleBtn != null) {
                    themeToggleBtn.setText("☀");
                }
                System.out.println("[TEMA] Alterado para tema claro");
            } else {
                scene.getStylesheets().add(getClass().getResource("styles.css").toExternalForm());
                isDarkTheme = true;
                if (themeToggleBtn != null) {
                    themeToggleBtn.setText("🌙");
                }
                System.out.println("[TEMA] Alterado para tema escuro");
            }

            updateGridDisplay();
            updateChildStagesTheme(); // NOVA LINHA - ATUALIZA JANELAS FILHAS
        }
    }

    private void updateChildStagesTheme() {
        childStages.removeIf(stage -> !stage.isShowing());

        String stylesheet = isDarkTheme ?
                getClass().getResource("styles.css").toExternalForm() :
                getClass().getResource("styles-light.css").toExternalForm();

        for (Stage childStage : childStages) {
            Scene childScene = childStage.getScene();
            if (childScene != null) {
                childScene.getStylesheets().clear();
                childScene.getStylesheets().add(stylesheet);
            }
        }
    }

    private void setupEventListeners() {
        // Listener de seleção de fornecedor
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

        // Listeners de busca e filtros - apenas atualizam display, sem acesso ao BD
        searchField.textProperty().addListener((obs, oldV, newV) -> updateGridDisplay());
        sortCombo.valueProperty().addListener((obs, oldV, newV) -> updateGridDisplay());
        filterCombo.valueProperty().addListener((obs, oldV, newV) -> updateGridDisplay());

        // Ações dos botões de lote
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

    // Formata campos de data para dd/MM/yyyy automaticamente
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

    // Registra operação no log com limite de 50 entradas
    private void registrarLog(String tipoOperacao, String entidade, String descricao) {
        LogEntry log = new LogEntry(0, LocalDateTime.now(), tipoOperacao, entidade, descricao, "Sistema");

        try {
            logDAO.inserir(log);
            System.out.println("[LOG SALVO] " + tipoOperacao + " em " + entidade + ": " + descricao);
            limparLogsAntigos();
        } catch (SQLException e) {
            System.err.println("Falha ao salvar log: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // Mantém apenas os 50 logs mais recentes
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

                    System.out.println("[LOG] " + removidos + " logs antigos removidos. Total agora: " + (totalLogs - removidos));
                }
            }
        } catch (SQLException e) {
            System.err.println("Falha ao limpar logs antigos: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // Exibe janela com histórico de alterações filtráveis
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
                            boolean tipoMatch = tipoFiltro.getValue().equals("Todas") || log.getTipoOperacao().equals(tipoFiltro.getValue());
                            boolean entidadeMatch = entidadeFiltro.getValue().equals("Todas") || log.getEntidade().equals(entidadeFiltro.getValue());
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

        // USA O NOVO MÉTODO PARA CRIAR JANELA CUSTOMIZADA
        Stage stage = createCustomStage("Historico de Alteracoes", scrollPane, 800, 600);
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

        // Evita alternar se já está no painel ativo
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

        FileChooser.ExtensionFilter imageFilter = new FileChooser.ExtensionFilter("Arquivos de Imagem", "*.png", "*.jpg", "*.jpeg", "*.gif", "*.bmp");
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

    // Verifica se o lote está vencido
    private boolean loteEstaVencido(LoteEstoque lote) {
        return lote.getDataValidade().isBefore(LocalDate.now());
    }

    // Adiciona ou edita lote no banco e atualiza cache
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
            // Impede edição de lotes vencidos
            if (editingLote != null && loteEstaVencido(editingLote)) {
                mostrarAlerta("Erro", "Não é possível editar lotes vencidos!");
                limparCamposLote();
                return;
            }

            try {
                if (editingLote != null) {
                    // Editando lote existente
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

                    // Atualiza no banco
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

                    // Atualiza cache em memória
                    editingLote.getProduto().setNome(nomeProduto);
                    editingLote.setFornecedor(fornecedorSelecionado);
                    imageUrlsCache.put(editingLote.getIdLote(), imagemNova);

                    editingLote = null;

                } else {
                    // Criando novo lote
                    String urlImagem = imagemSelecionada != null ? imagemSelecionada : "med.png";

                    LoteEstoque novoLote = new LoteEstoque(0, numeroLote, qtd, entrada, validade, 0, fornecedorSelecionado.getIdFornecedor(), threshold);

                    loteDAO.inserir(novoLote, nomeProduto, urlImagem);

                    String descricaoLog = String.format("Lote '%s' do produto '%s' criado. Quantidade: %d, Fornecedor: %s, Validade: %s%s",
                            numeroLote, nomeProduto, qtd, fornecedorSelecionado.getNome(), validade.format(formatter),
                            imagemSelecionada != null ? ", com imagem personalizada" : "");
                    registrarLog("CRIAR", "LOTE", descricaoLog);

                    // Recarrega do banco para obter o ID e adicionar ao cache
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

    // Exclui lote do banco e remove do cache
    private void excluirLote(LoteEstoque lote) {
        try {
            String descricaoLog = String.format("Lote '%s' do produto '%s' excluido. Quantidade: %d",
                    lote.getNumeroLote(), lote.getProduto().getNome(), lote.getQuantidadeAtual());
            if (lote.getProduto() != null) {
                String urlImagem = lote.getProduto().getUrlImagem();
                if (urlImagem != null && !urlImagem.equals("file:med.png") && !urlImagem.equals("med.png")) {
                    ImageService.excluirImagem(urlImagem);
                }
            }
            loteDAO.excluir(lote.getIdLote());
            registrarLog("EXCLUIR", "LOTE", descricaoLog);
            // Remove do cache em memória
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

    // Carrega dados do lote nos campos para edição
    @FXML
    private void editarLote(LoteEstoque lote) {
        // Impede edição de lotes vencidos
        if (loteEstaVencido(lote)) {
            mostrarAlerta("Erro", "Não é possível editar lotes vencidos!");
            return;
        }

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

    // Adiciona ou edita fornecedor no banco e atualiza cache
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
                // Editando fornecedor existente
                String descricaoLog = String.format("Fornecedor '%s' editado. CNPJ: %s, Telefone: %s, Email: %s", nome, cnpj, telefone, email);

                editingFornecedor.setNome(nome);
                editingFornecedor.setCnpj(cnpj);
                editingFornecedor.setTelefone(telefone);
                editingFornecedor.setEmail(email);

                fornecedorDAO.atualizar(editingFornecedor);
                registrarLog("EDITAR", "FORNECEDOR", descricaoLog);

                editingFornecedor = null;

            } else {
                // Criando novo fornecedor
                if (!cnpj.isEmpty() && fornecedorDAO.cnpjJaExiste(cnpj, 0)) {
                    mostrarAlerta("Erro", "CNPJ ja cadastrado!");
                    if (adicionarFornecedorBtn != null) {
                        adicionarFornecedorBtn.setDisable(false);
                    }
                    return;
                }

                Fornecedor novoFornecedor = new Fornecedor(0, nome, cnpj, telefone, email);
                fornecedorDAO.inserir(novoFornecedor);

                String descricaoLog = String.format("Fornecedor '%s' criado. CNPJ: %s, Telefone: %s, Email: %s", nome, cnpj, telefone, email);
                registrarLog("CRIAR", "FORNECEDOR", descricaoLog);

                // Recarrega para obter o ID
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

    // Exclui fornecedor se não tiver lotes vinculados
    @FXML
    private void excluirFornecedor() {
        Fornecedor selecionado = fornecedoresListView.getSelectionModel().getSelectedItem();
        if (selecionado == null) {
            mostrarAlerta("Aviso", "Selecione um fornecedor para excluir!");
            return;
        }

        try {
            // Verifica no cache em memória primeiro
            boolean temLotes = lotesCache.stream().anyMatch(lote -> lote.getIdFornecedor() == selecionado.getIdFornecedor());

            if (temLotes) {
                mostrarAlerta("Erro", "Nao e possivel excluir este fornecedor pois existem lotes vinculados a ele!");
                return;
            }

            String descricaoLog = String.format("Fornecedor '%s' excluido. CNPJ: %s", selecionado.getNome(), selecionado.getCnpj());

            fornecedorDAO.excluir(selecionado.getIdFornecedor());
            registrarLog("EXCLUIR", "FORNECEDOR", descricaoLog);

            // Remove do cache em memória
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

    // Incrementa quantidade do lote no banco e cache
    private void incrementarQuantidade(LoteEstoque lote, int quantidade) {
        // Impede incrementar lotes vencidos
        if (loteEstaVencido(lote)) {
            mostrarAlerta("Erro", "Não é possível incrementar a quantidade de lotes vencidos!");
            return;
        }

        try {
            int qtdAnterior = lote.getQuantidadeAtual();
            int novaQuantidade = qtdAnterior + quantidade;

            loteDAO.atualizarQuantidade(lote.getIdLote(), novaQuantidade);

            // Atualiza cache em memória
            lote.setQuantidadeAtual(novaQuantidade);

            String descricaoLog = String.format("Quantidade do lote '%s' (%s) incrementada: %d -> %d",
                    lote.getNumeroLote(), lote.getProduto().getNome(), qtdAnterior, novaQuantidade);
            registrarLog("EDITAR", "LOTE", descricaoLog);

            updateGridDisplay();

        } catch (SQLException e) {
            mostrarAlerta("Erro", "Erro ao atualizar quantidade: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // Decrementa quantidade do lote no banco e cache
    private void decrementarQuantidade(LoteEstoque lote, int quantidade) {
        // Impede decrementar lotes vencidos
        if (loteEstaVencido(lote)) {
            mostrarAlerta("Erro", "Não é possível decrementar a quantidade de lotes vencidos!");
            return;
        }

        if (lote.getQuantidadeAtual() >= quantidade) {
            try {
                int qtdAnterior = lote.getQuantidadeAtual();
                int novaQuantidade = qtdAnterior - quantidade;

                loteDAO.atualizarQuantidade(lote.getIdLote(), novaQuantidade);

                // Atualiza cache em memória
                lote.setQuantidadeAtual(novaQuantidade);

                String descricaoLog = String.format("Quantidade do lote '%s' (%s) decrementada: %d -> %d",
                        lote.getNumeroLote(), lote.getProduto().getNome(), qtdAnterior, novaQuantidade);
                registrarLog("EDITAR", "LOTE", descricaoLog);

                updateGridDisplay();

            } catch (SQLException e) {
                mostrarAlerta("Erro", "Erro ao atualizar quantidade: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    // Verifica alertas de estoque baixo e validade próxima/vencida
    private void verificarEGerarAlertas() {
        List<String> alertas = new java.util.ArrayList<>();
        LocalDate hoje = LocalDate.now();

        for (LoteEstoque lote : lotesCache) {
            String nomeProduto = lote.getProduto().getNome();

            if (lote.getQuantidadeAtual() < lote.getAlertThreshold()) {
                alertas.add("[ALERTA] O insumo \"" + nomeProduto + "\" (Lote: " + lote.getNumeroLote() +
                        ") esta com estoque baixo. Quantidade atual: " + lote.getQuantidadeAtual() + ".");
            }

            if (lote.getDataValidade().isAfter(hoje) && lote.getDataValidade().isBefore(hoje.plusDays(30))) {
                alertas.add("[ALERTA] O insumo \"" + nomeProduto + "\" (Lote: " + lote.getNumeroLote() +
                        ") esta proximo de vencer. Vence em: " + lote.getDataValidade().format(formatter) + ".");
            }

            if (lote.getDataValidade().isBefore(hoje)) {
                alertas.add("[ALERTA] O insumo \"" + nomeProduto + "\" (Lote: " + lote.getNumeroLote() +
                        ") está vencido. Venceu em: " + lote.getDataValidade().format(formatter) + ".");
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

        // Disable the fields completely when not editable
        nomeFornecedorField.setDisable(!editable);
        cnpjField.setDisable(!editable);
        telefoneField.setDisable(!editable);
        emailField.setDisable(!editable);

        // Prevent focus on disabled fields
        nomeFornecedorField.setFocusTraversable(editable);
        cnpjField.setFocusTraversable(editable);
        telefoneField.setFocusTraversable(editable);
        emailField.setFocusTraversable(editable);
    }

    // FORMATAÇÃO DO CAMPO TELEFONE DO FORNECEDOR
    private void configurarFormatacaoTelefone(TextField field) {
        field.setTextFormatter(new TextFormatter<>(change -> {
            if (change.isAdded() || change.isReplaced()) {
                String digits = change.getControlNewText().replaceAll("[^\\d]", "");
                if (digits.length() > 11) digits = digits.substring(0, 11);

                StringBuilder formatted = new StringBuilder();
                int caretPos = change.getCaretPosition();

                for (int i = 0; i < digits.length(); i++) {
                    // Adiciona '(' antes do DDD
                    if (i == 0) {
                        formatted.append("(");
                        if (caretPos > i) caretPos++;
                    }

                    formatted.append(digits.charAt(i));

                    // Adiciona ') ' após o DDD (2 dígitos)
                    if (i == 1 && digits.length() > 2) {
                        formatted.append(") ");
                        if (caretPos > i + 1) caretPos += 2;
                    }

                    // Adiciona '-' após os primeiros 5 dígitos do número (posição 7 total)
                    if (i == 6 && digits.length() > 7) {
                        formatted.append("-");
                        if (caretPos > i + 1) caretPos++;
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

    private void mostrarAlerta(String titulo, String mensagem) {
        Alert alert = new Alert(Alert.AlertType.ERROR, mensagem);
        alert.setTitle(titulo);
        alert.showAndWait();
    }

    // Atualiza grid com filtros e ordenação do cache em memória
    private void updateGridDisplay() {
        gridPane.getChildren().clear();

        String search = searchField.getText().toLowerCase();
        String filter = filterCombo.getValue();
        String sort = sortCombo.getValue();

        // Filtra e ordena do cache em memória
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

    // Atualiza apenas o estilo visual dos cards (mais rápido, sem reconstruir DOM)
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

    // Método auxiliar para criar janelas com barra de título customizada
    private Stage createCustomStage(String title, javafx.scene.Node content, double width, double height) {
        Stage customStage = new Stage();
        customStage.initStyle(StageStyle.TRANSPARENT);

        StackPane root = new StackPane();
        root.setStyle("-fx-background-color: transparent;");

        BorderPane mainPane = new BorderPane();
        mainPane.getStyleClass().add("border-pane");

        HBox customTitleBar = new HBox(10);
        customTitleBar.getStyleClass().add("custom-titlebar");
        customTitleBar.setAlignment(Pos.CENTER_LEFT);

        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().add("titlebar-title");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button closeBtn = new Button("✕");
        closeBtn.getStyleClass().addAll("titlebar-btn", "titlebar-close");
        closeBtn.setOnAction(e -> {
            childStages.remove(customStage); // NOVA LINHA
            customStage.close();
        });

        customTitleBar.getChildren().addAll(titleLabel, spacer, closeBtn);

        final double[] xOffset = {0};
        final double[] yOffset = {0};

        customTitleBar.setOnMousePressed(event -> {
            xOffset[0] = event.getSceneX();
            yOffset[0] = event.getSceneY();
        });

        customTitleBar.setOnMouseDragged(event -> {
            customStage.setX(event.getScreenX() - xOffset[0]);
            customStage.setY(event.getScreenY() - yOffset[0]);
        });

        mainPane.setTop(customTitleBar);
        mainPane.setCenter(content);

        root.getChildren().add(mainPane);

        Rectangle clip = new Rectangle();
        clip.setArcWidth(24);
        clip.setArcHeight(24);
        clip.widthProperty().bind(root.widthProperty());
        clip.heightProperty().bind(root.heightProperty());
        root.setClip(clip);

        Scene scene = new Scene(root, width, height);
        scene.setFill(javafx.scene.paint.Color.TRANSPARENT);

        if (isDarkTheme) {
            scene.getStylesheets().add(getClass().getResource("styles.css").toExternalForm());
        } else {
            scene.getStylesheets().add(getClass().getResource("styles-light.css").toExternalForm());
        }

        customStage.setScene(scene);

        childStages.add(customStage); // NOVA LINHA
        customStage.setOnHidden(e -> childStages.remove(customStage)); // NOVA LINHA

        setupChildWindowResize(customStage, scene);
        return customStage;
    }

    private void setupChildWindowResize(Stage childStage, Scene scene) {
        final double[] startX = {0};
        final double[] startY = {0};
        final double[] startStageX = {0};
        final double[] startStageY = {0};
        final double[] startWidth = {0};
        final double[] startHeight = {0};
        final String[] childResizeDirection = {""};
        final boolean[] isChildResizing = {false};

        scene.addEventFilter(javafx.scene.input.MouseEvent.MOUSE_MOVED, event -> {
            if (isChildResizing[0]) return;

            double mouseX = event.getSceneX();
            double mouseY = event.getSceneY();
            double width = scene.getWidth();
            double height = scene.getHeight();

            boolean onLeft = mouseX < RESIZE_MARGIN;
            boolean onRight = mouseX > width - RESIZE_MARGIN;
            boolean onTop = mouseY < RESIZE_MARGIN;
            boolean onBottom = mouseY > height - RESIZE_MARGIN;

            if ((onLeft || onRight) || (onTop || onBottom)) {
                if ((onLeft || onRight) && (onTop || onBottom)) {
                    if (onLeft && onTop) {
                        scene.setCursor(javafx.scene.Cursor.NW_RESIZE);
                        childResizeDirection[0] = "NW";
                    } else if (onRight && onTop) {
                        scene.setCursor(javafx.scene.Cursor.NE_RESIZE);
                        childResizeDirection[0] = "NE";
                    } else if (onLeft && onBottom) {
                        scene.setCursor(javafx.scene.Cursor.SW_RESIZE);
                        childResizeDirection[0] = "SW";
                    } else if (onRight && onBottom) {
                        scene.setCursor(javafx.scene.Cursor.SE_RESIZE);
                        childResizeDirection[0] = "SE";
                    }
                } else if (onLeft) {
                    scene.setCursor(javafx.scene.Cursor.W_RESIZE);
                    childResizeDirection[0] = "W";
                } else if (onRight) {
                    scene.setCursor(javafx.scene.Cursor.E_RESIZE);
                    childResizeDirection[0] = "E";
                } else if (onTop) {
                    scene.setCursor(javafx.scene.Cursor.N_RESIZE);
                    childResizeDirection[0] = "N";
                } else if (onBottom) {
                    scene.setCursor(javafx.scene.Cursor.S_RESIZE);
                    childResizeDirection[0] = "S";
                }
            } else {
                if (!childResizeDirection[0].isEmpty() && !isChildResizing[0]) {
                    scene.setCursor(javafx.scene.Cursor.DEFAULT);
                    childResizeDirection[0] = "";
                }
            }
        });

        scene.addEventFilter(javafx.scene.input.MouseEvent.MOUSE_PRESSED, event -> {
            if (!childResizeDirection[0].isEmpty()) {
                isChildResizing[0] = true;
                startX[0] = event.getScreenX();
                startY[0] = event.getScreenY();
                startStageX[0] = childStage.getX();
                startStageY[0] = childStage.getY();
                startWidth[0] = childStage.getWidth();
                startHeight[0] = childStage.getHeight();
                event.consume();
            }
        });

        scene.addEventFilter(javafx.scene.input.MouseEvent.MOUSE_DRAGGED, event -> {
            if (!isChildResizing[0]) return;

            double deltaX = event.getScreenX() - startX[0];
            double deltaY = event.getScreenY() - startY[0];

            double minWidth = 400;
            double minHeight = 300;

            switch (childResizeDirection[0]) {
                case "E":
                    childStage.setWidth(Math.max(minWidth, startWidth[0] + deltaX));
                    break;

                case "W":
                    double newWidthW = startWidth[0] - deltaX;
                    if (newWidthW >= minWidth) {
                        childStage.setX(startStageX[0] + deltaX);
                        childStage.setWidth(newWidthW);
                    }
                    break;

                case "S":
                    childStage.setHeight(Math.max(minHeight, startHeight[0] + deltaY));
                    break;

                case "N":
                    double newHeightN = startHeight[0] - deltaY;
                    if (newHeightN >= minHeight) {
                        childStage.setY(startStageY[0] + deltaY);
                        childStage.setHeight(newHeightN);
                    }
                    break;

                case "SE":
                    childStage.setWidth(Math.max(minWidth, startWidth[0] + deltaX));
                    childStage.setHeight(Math.max(minHeight, startHeight[0] + deltaY));
                    break;

                case "SW":
                    double newWidthSW = startWidth[0] - deltaX;
                    if (newWidthSW >= minWidth) {
                        childStage.setX(startStageX[0] + deltaX);
                        childStage.setWidth(newWidthSW);
                    }
                    childStage.setHeight(Math.max(minHeight, startHeight[0] + deltaY));
                    break;

                case "NE":
                    childStage.setWidth(Math.max(minWidth, startWidth[0] + deltaX));
                    double newHeightNE = startHeight[0] - deltaY;
                    if (newHeightNE >= minHeight) {
                        childStage.setY(startStageY[0] + deltaY);
                        childStage.setHeight(newHeightNE);
                    }
                    break;

                case "NW":
                    double newWidthNW = startWidth[0] - deltaX;
                    double newHeightNW = startHeight[0] - deltaY;
                    if (newWidthNW >= minWidth) {
                        childStage.setX(startStageX[0] + deltaX);
                        childStage.setWidth(newWidthNW);
                    }
                    if (newHeightNW >= minHeight) {
                        childStage.setY(startStageY[0] + deltaY);
                        childStage.setHeight(newHeightNW);
                    }
                    break;
            }

            event.consume();
        });

        scene.addEventFilter(javafx.scene.input.MouseEvent.MOUSE_RELEASED, event -> {
            if (isChildResizing[0]) {
                isChildResizing[0] = false;
                childResizeDirection[0] = "";
                scene.setCursor(javafx.scene.Cursor.DEFAULT);
                event.consume();
            }
        });
    }

    // Cria card visual para cada lote
    private VBox createCard(LoteEstoque lote) {
        VBox itemBox = new VBox(8);
        itemBox.setAlignment(Pos.CENTER);
        itemBox.getStyleClass().add("card");

        // Armazena referência do lote para acesso rápido
        itemBox.setUserData(lote);

        Produto produto = lote.getProduto();
        Fornecedor fornecedor = lote.getFornecedor();

        String imagePath = ImageService.getImagePath(produto.getUrlImagem());
        Image originalImage = new Image(imagePath);

        // Inverte a cor da imagem padrão no tema claro
        boolean isDefaultImage = produto.getUrlImagem().equals("med.png") ||
                produto.getUrlImagem().equals("file:med.png") ||
                produto.getUrlImagem() == null;

        Image displayImage = originalImage;
        if (!isDarkTheme && isDefaultImage) {
            displayImage = invertImage(originalImage);
        }

        ImageView imageView = new ImageView(displayImage);
        imageView.setFitWidth(80);
        imageView.setFitHeight(80);
        imageView.setPreserveRatio(true);

        Label nameLabel = new Label(produto.getNome());
        nameLabel.getStyleClass().add("name");
        nameLabel.setMaxWidth(145);
        nameLabel.setAlignment(Pos.CENTER);
        nameLabel.setTextAlignment(TextAlignment.CENTER);

        Label batchLabel = new Label("Lote: " + lote.getNumeroLote());
        batchLabel.getStyleClass().add("batch");

        Label fornecedorLabel = new Label("Fornecedor: " + (fornecedor != null ? fornecedor.getNome() : "N/A"));
        fornecedorLabel.getStyleClass().add("fornecedor");
        fornecedorLabel.setMaxWidth(145);
        fornecedorLabel.setAlignment(Pos.CENTER);
        fornecedorLabel.setTextAlignment(TextAlignment.CENTER);

        Label qtyLabel = new Label("Quantidade: " + lote.getQuantidadeAtual());
        qtyLabel.getStyleClass().add("quantity");

        // Estilização por nível de estoque
        if (lote.getQuantidadeAtual() < lote.getAlertThreshold()) {
            qtyLabel.getStyleClass().add("low");
        } else if (lote.getQuantidadeAtual() == lote.getAlertThreshold()) {
            qtyLabel.getStyleClass().add("warning");
        }

        Label dateLabel = new Label("Validade: " + lote.getDataValidade().format(formatter));
        dateLabel.getStyleClass().add("validity");

        LocalDate hoje = LocalDate.now();
        boolean loteVencido = lote.getDataValidade().isBefore(hoje);

        if (loteVencido) {
            dateLabel.getStyleClass().add("expired");
        } else if (lote.getDataValidade().isBefore(hoje.plusDays(30))) {
            dateLabel.getStyleClass().add("near");
        }

        HBox buttons = new HBox(5);
        buttons.setAlignment(Pos.CENTER);
        Button plusBtn = new Button("✚");
        plusBtn.getStyleClass().add("button");
        Button minusBtn = new Button("−");
        minusBtn.getStyleClass().add("button");
        Button delBtn = new Button("✖");
        delBtn.getStyleClass().addAll("button");

        // Desabilita botões para lotes vencidos
        if (loteVencido) {
            plusBtn.setDisable(true);
            minusBtn.setDisable(true);
            plusBtn.setStyle("-fx-opacity: 0.5;");
            minusBtn.setStyle("-fx-opacity: 0.5;");
        }
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
        itemBox.getChildren().addAll(imageView, nameLabel, batchLabel, fornecedorLabel, qtyLabel, dateLabel, buttons);

        // Handler de clique otimizado - apenas atualiza estilo
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
            // Atualização instantânea - apenas muda classes CSS
            updateCardStyling();
        });

        // Aplica estilo inicial
        if (editingLote != null && lote.getIdLote() == editingLote.getIdLote()) {
            itemBox.getStyleClass().add("editing");
        } else if (selectedLote != null && lote.getIdLote() == selectedLote.getIdLote()) {
            itemBox.getStyleClass().add("selected");
        }
        return itemBox;
    }

    private Image invertImage(Image image) {
        int width = (int) image.getWidth();
        int height = (int) image.getHeight();

        javafx.scene.image.WritableImage invertedImage = new javafx.scene.image.WritableImage(width, height);
        javafx.scene.image.PixelReader pixelReader = image.getPixelReader();
        javafx.scene.image.PixelWriter pixelWriter = invertedImage.getPixelWriter();

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                javafx.scene.paint.Color color = pixelReader.getColor(x, y);

                // Inverte as cores RGB mantendo o alpha (transparência)
                javafx.scene.paint.Color invertedColor = new javafx.scene.paint.Color(
                        1.0 - color.getRed(),
                        1.0 - color.getGreen(),
                        1.0 - color.getBlue(),
                        color.getOpacity()
                );

                pixelWriter.setColor(x, y, invertedColor);
            }
        }

        return invertedImage;
    }

    // Exibe janela com lista de alertas
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

        int itemHeight = 45;
        int baseHeight = 70;
        int totalHeight = baseHeight + alertas.size() * itemHeight;
        totalHeight = Math.max(totalHeight, 120);
        totalHeight = Math.min(totalHeight, 400);

        Stage stage = createCustomStage("Alertas de Estoque", scrollPane, 700, totalHeight);
        stage.show();
    }
}