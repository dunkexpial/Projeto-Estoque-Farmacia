import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.TilePane;
import javafx.scene.layout.VBox;
import javafx.scene.layout.HBox;
import javafx.geometry.Pos;
import javafx.stage.Stage;
import javafx.scene.Scene;
import javafx.geometry.Insets;
import javafx.application.Platform;
import javafx.collections.FXCollections;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Comparator;
import java.util.ArrayList;
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

    // Painel de alternância (Lote/Fornecedor)
    @FXML private VBox painelLote;
    @FXML private VBox painelFornecedor;
    @FXML private ToggleButton gerenciarLotesBtn;
    @FXML private ToggleButton gerenciarFornecedoresBtn;

    // Campos para gerenciar fornecedores
    @FXML private TextField nomeFornecedorField;
    @FXML private TextField cnpjField;
    @FXML private TextField telefoneField;
    @FXML private TextField emailField;
    @FXML private ListView<Fornecedor> fornecedoresListView;

    // Listas em memória
    private final List<Produto> produtos = new ArrayList<>();
    private final List<Fornecedor> fornecedores = new ArrayList<>();
    private final List<LoteEstoque> lotes = new ArrayList<>();
    private final List<MovimentacaoEstoque> movimentacoes = new ArrayList<>();
    private final List<Notificacao> notificacoes = new ArrayList<>();

    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private LoteEstoque editingLote = null;
    private LoteEstoque selectedLote = null;
    private Fornecedor editingFornecedor = null;

    // Contadores para IDs
    private int nextProdutoId = 1;
    private int nextFornecedorId = 1;
    private int nextLoteId = 1;
    private int nextMovimentacaoId = 1;
    private int nextNotificacaoId = 1;

    //BANCO DE DADOS
    private FornecedorDAO fornecedorDAO = new FornecedorDAO();
    private LoteDAO loteDAO = new LoteDAO();

    @FXML
    public void initialize() {
        gridPane.setHgap(10);
        gridPane.setVgap(10);
        gridPane.setAlignment(Pos.TOP_LEFT);

        // Inicializar dados de exemplo
        inicializarDadosExemplo();

        // Configurar ComboBox de fornecedores
        atualizarComboFornecedores();
        atualizarListaFornecedores();

        // Disable supplier fields by default
        setSupplierFieldsEditable(false);

        // Ensure Adicionar / Salvar Fornecedor button is enabled initially
        if (adicionarFornecedorBtn != null) {
            adicionarFornecedorBtn.setDisable(false);
        }

        // Add listener to fornecedoresListView for selection
        fornecedoresListView.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
            if (newSelection != null) {
                // Populate fields with selected supplier's data
                nomeFornecedorField.setText(newSelection.getNome());
                cnpjField.setText(newSelection.getCnpj());
                telefoneField.setText(newSelection.getTelefone());
                emailField.setText(newSelection.getEmail());
                // Ensure fields are not editable until "Editar" or "Novo" is clicked
                setSupplierFieldsEditable(false);
                if (adicionarFornecedorBtn != null) {
                    adicionarFornecedorBtn.setDisable(false);
                }
            } else {
                // Clear fields and disable editing when no supplier is selected
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

        // Listeners for search, sort, and filter
        searchField.textProperty().addListener((obs, oldV, newV) -> refreshGrid());
        sortCombo.valueProperty().addListener((obs, oldV, newV) -> refreshGrid());
        filterCombo.valueProperty().addListener((obs, oldV, newV) -> refreshGrid());

        // Formatação automática de datas
        configurarFormatacaoData(validadeField);
        configurarFormatacaoData(dataEntradaField);

        // Botões de ação - Lote
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

        // Inicialmente mostrar painel de lotes
        mostrarPainelLote();

        refreshGrid();
        Platform.runLater(() -> verificarEGerarAlertas());

        //Forncecedores do BD
        try {
            List<Fornecedor> dbFornecedores = fornecedorDAO.listarTodos();
            fornecedores.clear();
            fornecedores.addAll(dbFornecedores);
            atualizarComboFornecedores();
            atualizarListaFornecedores();
        } catch (SQLException e) {
            mostrarAlerta("Erro", "Erro ao carregar fornecedores: " + e.getMessage());
            e.printStackTrace();
        }

        //Lotes do BD
        try {
        fornecedores.clear();
        fornecedores.addAll(fornecedorDAO.listarTodos());
        atualizarComboFornecedores();
        atualizarListaFornecedores();
        
        // Carregar lotes
        List<Object[]> lotesComNome = loteDAO.listarTodosComNome();
        lotes.clear();
        
        for (Object[] item : lotesComNome) {
            LoteEstoque lote = (LoteEstoque) item[0];
            String nomeProduto = (String) item[1];
            
            // Buscar ou criar produto
            Produto produto = buscarOuCriarProduto(nomeProduto);
            lote.setProduto(produto);
            lote.setIdProduto(produto.getIdProduto());
            
            // Vincular fornecedor
            Fornecedor f = fornecedores.stream()
                .filter(forn -> forn.getIdFornecedor() == lote.getIdFornecedor())
                .findFirst()
                .orElse(null);
            lote.setFornecedor(f);
            
            lotes.add(lote);
        }
        
        refreshGrid();
        verificarEGerarAlertas();
        
    } catch (SQLException e) {
        mostrarAlerta("Erro", "Corrija aí cabaço: " + e.getMessage());
        e.printStackTrace();
    }
}

    private void inicializarDadosExemplo() {
        // Criar fornecedores de exemplo
        fornecedores.add(new Fornecedor(nextFornecedorId++, "Farmacorp LTDA", "12.345.678/0001-90", "(81) 3333-4444", "contato@farmacorp.com"));
        fornecedores.add(new Fornecedor(nextFornecedorId++, "MediSupply S.A.", "98.765.432/0001-10", "(81) 3555-6666", "vendas@medisupply.com"));
        fornecedores.add(new Fornecedor(nextFornecedorId++, "PharmaDist", "11.222.333/0001-44", "(81) 3777-8888", "info@pharmadist.com"));

        LocalDate hoje = LocalDate.now();

        // Quantidade normal, validade normal
        criarLoteExemplo("Paracetamol", "B001", 10, hoje.minusDays(10), hoje.plusDays(20), 0, 5);
        // Quantidade igual ao threshold, validade normal
        criarLoteExemplo("Ibuprofeno", "B002", 5, hoje.minusDays(5), hoje.plusDays(15), 1, 5);
        // Quantidade abaixo do threshold, validade normal
        criarLoteExemplo("Amoxicilina", "B003", 2, hoje.minusDays(7), hoje.plusDays(10), 2, 5);
        // Quantidade normal, validade próxima (menos de 7 dias)
        criarLoteExemplo("Dipirona", "B004", 8, hoje.minusDays(5), hoje.plusDays(3), 0, 5);
        // Quantidade igual ao threshold, validade próxima
        criarLoteExemplo("Loratadina", "B005", 3, hoje.minusDays(10), hoje.plusDays(2), 1, 3);
        // Quantidade abaixo do threshold, validade próxima
        criarLoteExemplo("Omeprazol", "B006", 1, hoje.minusDays(15), hoje.plusDays(5), 2, 5);
        // Quantidade normal, validade expirada
        criarLoteExemplo("Cetirizina", "B007", 7, hoje.minusDays(30), hoje.minusDays(1), 0, 5);
        // Quantidade igual ao threshold, validade expirada
        criarLoteExemplo("Metformina", "B008", 10, hoje.minusDays(40), hoje.minusDays(5), 1, 10);
        // Quantidade abaixo do threshold, validade expirada
        criarLoteExemplo("Clorfenamina", "B009", 0, hoje.minusDays(50), hoje.minusDays(10), 2, 5);
        // Extra: validade normal e quantidade muito alta (teste visual)
        criarLoteExemplo("Diclofenaco", "B010", 20, hoje.minusDays(1), hoje.plusDays(30), 0, 5);
    }

    private void criarLoteExemplo(String nomeProduto, String numeroLote, int qtd,
                                  LocalDate entrada, LocalDate validade, int fornIdx, int threshold) {
        // Buscar ou criar produto
        Produto produto = buscarOuCriarProduto(nomeProduto);
        Fornecedor f = fornecedores.get(fornIdx);

        LoteEstoque lote = new LoteEstoque(
                nextLoteId++, numeroLote, qtd, entrada, validade,
                produto.getIdProduto(), f.getIdFornecedor(), threshold
        );
        lote.setProduto(produto);
        lote.setFornecedor(f);
        lotes.add(lote);

        // Registrar movimentação de entrada inicial
        registrarMovimentacao(lote.getIdLote(), qtd, "ENTRADA", "Entrada inicial de estoque");
    }

    private Produto buscarOuCriarProduto(String nomeProduto) {
        return produtos.stream()
                .filter(p -> p.getNome().equalsIgnoreCase(nomeProduto))
                .findFirst()
                .orElseGet(() -> {
                    Produto novoProduto = new Produto(nextProdutoId++, nomeProduto);
                    produtos.add(novoProduto);
                    return novoProduto;
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
                    Produto produto = buscarOuCriarProduto(nomeProduto);

                    editingLote.setProduto(produto);
                    editingLote.setIdProduto(produto.getIdProduto());
                    editingLote.setFornecedor(fornecedorSelecionado);
                    editingLote.setIdFornecedor(fornecedorSelecionado.getIdFornecedor());
                    editingLote.setNumeroLote(numeroLote);
                    editingLote.setQuantidadeAtual(qtd);
                    editingLote.setDataEntrada(entrada);
                    editingLote.setDataValidade(validade);
                    editingLote.setAlertThreshold(threshold);

                    loteDAO.atualizar(editingLote, nomeProduto);

                    if (qtd != qtdAnterior) {
                        int diferenca = qtd - qtdAnterior;
                        String tipo = diferenca > 0 ? "ENTRADA" : "SAIDA";
                        registrarMovimentacao(editingLote.getIdLote(), Math.abs(diferenca), tipo, "Ajuste por edição");
                    }

                    editingLote = null;
                } else {
                    Produto produto = buscarOuCriarProduto(nomeProduto);

                    LoteEstoque novoLote = new LoteEstoque(
                            0, numeroLote, qtd, entrada, validade,
                            produto.getIdProduto(),
                            fornecedorSelecionado.getIdFornecedor(),
                            threshold
                    );
                    novoLote.setProduto(produto);
                    novoLote.setFornecedor(fornecedorSelecionado);
                    
                    loteDAO.inserir(novoLote, nomeProduto);
                    lotes.add(novoLote);

                    registrarMovimentacao(novoLote.getIdLote(), qtd, "ENTRADA", "Entrada inicial de estoque");
                }

                limparCamposLote();
                refreshGrid();
                verificarEGerarAlertas();
                
            } catch (SQLException e) {
                mostrarAlerta("Erro", "Erro ao salvar lote: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    @FXML
    private void excluirLote(LoteEstoque lote) {
        try {
            loteDAO.excluir(lote.getIdLote());
            lotes.remove(lote);
            limparCamposLote();
            refreshGrid();
            verificarEGerarAlertas();
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
                // Atualizar
                editingFornecedor.setNome(nome);
                editingFornecedor.setCnpj(cnpj);
                editingFornecedor.setTelefone(telefone);
                editingFornecedor.setEmail(email);
                
                fornecedorDAO.atualizar(editingFornecedor);
                editingFornecedor = null;
            } else {
                // Verificar duplicata
                if (!cnpj.isEmpty() && fornecedorDAO.cnpjJaExiste(cnpj, 0)) {
                    mostrarAlerta("Erro", "CNPJ já cadastrado!");
                    if (adicionarFornecedorBtn != null) {
                        adicionarFornecedorBtn.setDisable(false);
                    }
                    return;
                }

                // Inserir
                Fornecedor novoFornecedor = new Fornecedor(0, nome, cnpj, telefone, email);
                fornecedorDAO.inserir(novoFornecedor);
                fornecedores.add(novoFornecedor);
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

        boolean temLotes = lotes.stream()
                .anyMatch(l -> l.getIdFornecedor() == selecionado.getIdFornecedor());

        if (temLotes) {
            mostrarAlerta("Erro", "Não é possível excluir este fornecedor pois existem lotes vinculados a ele!");
            return;
        }

        try {
            fornecedorDAO.excluir(selecionado.getIdFornecedor());
            fornecedores.remove(selecionado);
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
            fornecedorCombo.setItems(javafx.collections.FXCollections.observableArrayList(fornecedores));
        }
    }

    private void atualizarListaFornecedores() {
        if (fornecedoresListView != null) {
            fornecedoresListView.setItems(javafx.collections.FXCollections.observableArrayList(fornecedores));
        }
    }

    // ==================== MOVIMENTAÇÕES E NOTIFICAÇÕES ====================

    private void registrarMovimentacao(int idLote, int quantidade, String tipo, String observacao) {
        MovimentacaoEstoque mov = new MovimentacaoEstoque(
                nextMovimentacaoId++,
                quantidade,
                LocalDateTime.now(),
                tipo,
                observacao,
                idLote
        );
        movimentacoes.add(mov);
    }

        //eerm esqueci, lembrei... é o update da quantidade do produto no banco
        private void incrementarQuantidade(LoteEstoque lote, int quantidade) {
        lote.setQuantidadeAtual(lote.getQuantidadeAtual() + quantidade);
        registrarMovimentacao(lote.getIdLote(), quantidade, "ENTRADA", "Ajuste manual (+)");
        
        // Update no banco ... err acho que só isso mesmo
        try {
            loteDAO.atualizarQuantidade(lote.getIdLote(), lote.getQuantidadeAtual());
        } catch (SQLException e) {
            mostrarAlerta("Erro", "Erro ao atualizar quantidade: " + e.getMessage());
            e.printStackTrace();
        }
        
        verificarEGerarAlertas();
        refreshGrid();
    }

    private void decrementarQuantidade(LoteEstoque lote, int quantidade) {
        if (lote.getQuantidadeAtual() >= quantidade) {
            lote.setQuantidadeAtual(lote.getQuantidadeAtual() - quantidade);
            registrarMovimentacao(lote.getIdLote(), quantidade, "SAIDA", "Ajuste manual (-)");
            
            // Update no banco ... err acho que só isso mesmo
            try {
                loteDAO.atualizarQuantidade(lote.getIdLote(), lote.getQuantidadeAtual());
            } catch (SQLException e) {
                mostrarAlerta("Erro", "Erro ao atualizar quantidade: " + e.getMessage());
                e.printStackTrace();
            }
            
            verificarEGerarAlertas();
            refreshGrid();
        }
    }

    private void verificarEGerarAlertas() {
        LocalDate hoje = LocalDate.now();

        for (LoteEstoque lote : lotes) {
            if (lote.getQuantidadeAtual() < lote.getAlertThreshold()) {
                gerarNotificacao("ESTOQUE_BAIXO", lote.getIdLote());
            }

            if (lote.getDataValidade().isAfter(hoje) &&
                    lote.getDataValidade().isBefore(hoje.plusDays(7))) {
                gerarNotificacao("VENCIMENTO_PROXIMO", lote.getIdLote());
            }

            if (lote.getDataValidade().isBefore(hoje)) {
                gerarNotificacao("VENCIDO", lote.getIdLote());
            }
        }

        List<Notificacao> pendentes = notificacoes.stream()
                .filter(n -> "PENDENTE".equals(n.getStatus()))
                .collect(Collectors.toList());

        if (!pendentes.isEmpty()) {
            mostrarNotificacoes(pendentes);
        }
    }

    private void gerarNotificacao(String tipo, int idLote) {
        boolean jaExiste = notificacoes.stream()
                .anyMatch(n -> n.getTipo().equals(tipo) &&
                        n.getIdLote() == idLote &&
                        n.getStatus().equals("PENDENTE"));

        if (!jaExiste) {
            Notificacao notif = new Notificacao(
                    nextNotificacaoId++,
                    tipo,
                    LocalDate.now(),
                    "PENDENTE",
                    idLote
            );
            notificacoes.add(notif);
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

        String search = searchField.getText().toLowerCase();
        String filter = filterCombo.getValue();
        String sort = sortCombo.getValue();

        List<LoteEstoque> list = lotes.stream()
                .filter(l -> l.getProduto().getNome().toLowerCase().contains(search) ||
                        l.getFornecedor().getNome().toLowerCase().contains(search))
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

        Label fornecedorLabel = new Label("Fornecedor: " + fornecedor.getNome());
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

    private void mostrarNotificacoes(List<Notificacao> notificacoesPendentes) {
        VBox content = new VBox(10);
        content.getStyleClass().add("notification-content");
        content.setAlignment(Pos.TOP_CENTER);

        Label title = new Label("⚠ Alertas de Estoque");
        title.getStyleClass().add("notification-title");
        content.getChildren().add(title);

        Separator separator = new Separator();
        content.getChildren().add(separator);

        for (Notificacao notif : notificacoesPendentes) {
            LoteEstoque lote = lotes.stream()
                    .filter(l -> l.getIdLote() == notif.getIdLote())
                    .findFirst()
                    .orElse(null);

            if (lote == null) continue;

            String mensagem = "";
            switch (notif.getTipo()) {
                case "ESTOQUE_BAIXO":
                    mensagem = "⚠ O insumo \"" + lote.getProduto().getNome() +
                            "\" (Lote: " + lote.getNumeroLote() +
                            ") está com estoque baixo. Quantidade atual: " +
                            lote.getQuantidadeAtual() + ".";
                    break;
                case "VENCIDO":
                    mensagem = "⚠ O insumo \"" + lote.getProduto().getNome() +
                            "\" (Lote: " + lote.getNumeroLote() +
                            ") está vencido. Venceu em: " +
                            lote.getDataValidade().format(formatter) + ".";
                    break;
                case "VENCIMENTO_PROXIMO":
                    mensagem = "⚠ O insumo \"" + lote.getProduto().getNome() +
                            "\" (Lote: " + lote.getNumeroLote() +
                            ") está próximo de vencer. Vence em: " +
                            lote.getDataValidade().format(formatter) + ".";
                    break;
            }

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
        int totalHeight = baseHeight + notificacoesPendentes.size() * itemHeight;
        totalHeight = Math.max(totalHeight, 120);
        totalHeight = Math.min(totalHeight, 400);

        Scene scene = new Scene(scrollPane, 700, totalHeight);
        scene.getStylesheets().add(getClass().getResource("styles.css").toExternalForm()); // Add stylesheet
        stage.setScene(scene);
        stage.show();
    }
}