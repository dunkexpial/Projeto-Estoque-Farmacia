import javafx.beans.property.*;
import java.time.LocalDate;

public class LoteEstoque {
    private final IntegerProperty idLote;
    private final StringProperty numeroLote;
    private final IntegerProperty quantidadeAtual;
    private final ObjectProperty<LocalDate> dataEntrada;
    private final ObjectProperty<LocalDate> dataValidade;
    private final IntegerProperty idProduto;
    private final IntegerProperty idFornecedor;
    private final IntegerProperty alertThreshold;

    // Referências para navegação (não persistidas no BD)
    private Produto produto;
    private Fornecedor fornecedor;

    public LoteEstoque(int idLote, String numeroLote, int quantidadeAtual,
                       LocalDate dataEntrada, LocalDate dataValidade,
                       int idProduto, int idFornecedor, int alertThreshold) {
        this.idLote = new SimpleIntegerProperty(idLote);
        this.numeroLote = new SimpleStringProperty(numeroLote);
        this.quantidadeAtual = new SimpleIntegerProperty(quantidadeAtual);
        this.dataEntrada = new SimpleObjectProperty<>(dataEntrada);
        this.dataValidade = new SimpleObjectProperty<>(dataValidade);
        this.idProduto = new SimpleIntegerProperty(idProduto);
        this.idFornecedor = new SimpleIntegerProperty(idFornecedor);
        this.alertThreshold = new SimpleIntegerProperty(alertThreshold);
    }

    // idLote
    public int getIdLote() { return idLote.get(); }
    public void setIdLote(int value) { idLote.set(value); }
    public IntegerProperty idLoteProperty() { return idLote; }

    // numeroLote
    public String getNumeroLote() { return numeroLote.get(); }
    public void setNumeroLote(String value) { numeroLote.set(value); }
    public StringProperty numeroLoteProperty() { return numeroLote; }

    // quantidadeAtual
    public int getQuantidadeAtual() { return quantidadeAtual.get(); }
    public void setQuantidadeAtual(int value) { quantidadeAtual.set(value); }
    public IntegerProperty quantidadeAtualProperty() { return quantidadeAtual; }

    // dataEntrada
    public LocalDate getDataEntrada() { return dataEntrada.get(); }
    public void setDataEntrada(LocalDate value) { dataEntrada.set(value); }
    public ObjectProperty<LocalDate> dataEntradaProperty() { return dataEntrada; }

    // dataValidade
    public LocalDate getDataValidade() { return dataValidade.get(); }
    public void setDataValidade(LocalDate value) { dataValidade.set(value); }
    public ObjectProperty<LocalDate> dataValidadeProperty() { return dataValidade; }

    // idProduto
    public int getIdProduto() { return idProduto.get(); }
    public void setIdProduto(int value) { idProduto.set(value); }
    public IntegerProperty idProdutoProperty() { return idProduto; }

    // idFornecedor
    public int getIdFornecedor() { return idFornecedor.get(); }
    public void setIdFornecedor(int value) { idFornecedor.set(value); }
    public IntegerProperty idFornecedorProperty() { return idFornecedor; }

    // alertThreshold
    public int getAlertThreshold() { return alertThreshold.get(); }
    public void setAlertThreshold(int value) { alertThreshold.set(value); }
    public IntegerProperty alertThresholdProperty() { return alertThreshold; }

    // Produto (referência)
    public Produto getProduto() { return produto; }
    public void setProduto(Produto value) { produto = value; }

    // Fornecedor (referência)
    public Fornecedor getFornecedor() { return fornecedor; }
    public void setFornecedor(Fornecedor value) { fornecedor = value; }
}