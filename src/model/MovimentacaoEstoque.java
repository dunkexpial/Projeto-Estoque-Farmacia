package model;

import javafx.beans.property.*;
import java.time.LocalDateTime;

public class MovimentacaoEstoque {
    private final IntegerProperty idMovimentacao;
    private final IntegerProperty quantidade;
    private final ObjectProperty<LocalDateTime> dataHora;
    private final StringProperty tipo; // "ENTRADA" ou "SAIDA"
    private final StringProperty observacao;
    private final IntegerProperty idLote;

    public MovimentacaoEstoque(int idMovimentacao, int quantidade, LocalDateTime dataHora,
                               String tipo, String observacao, int idLote) {
        this.idMovimentacao = new SimpleIntegerProperty(idMovimentacao);
        this.quantidade = new SimpleIntegerProperty(quantidade);
        this.dataHora = new SimpleObjectProperty<>(dataHora);
        this.tipo = new SimpleStringProperty(tipo);
        this.observacao = new SimpleStringProperty(observacao);
        this.idLote = new SimpleIntegerProperty(idLote);
    }

    // idMovimentacao
    public int getIdMovimentacao() { return idMovimentacao.get(); }
    public void setIdMovimentacao(int value) { idMovimentacao.set(value); }
    public IntegerProperty idMovimentacaoProperty() { return idMovimentacao; }

    // quantidade
    public int getQuantidade() { return quantidade.get(); }
    public void setQuantidade(int value) { quantidade.set(value); }
    public IntegerProperty quantidadeProperty() { return quantidade; }

    // dataHora
    public LocalDateTime getDataHora() { return dataHora.get(); }
    public void setDataHora(LocalDateTime value) { dataHora.set(value); }
    public ObjectProperty<LocalDateTime> dataHoraProperty() { return dataHora; }

    // tipo
    public String getTipo() { return tipo.get(); }
    public void setTipo(String value) { tipo.set(value); }
    public StringProperty tipoProperty() { return tipo; }

    // observacao
    public String getObservacao() { return observacao.get(); }
    public void setObservacao(String value) { observacao.set(value); }
    public StringProperty observacaoProperty() { return observacao; }

    // idLote
    public int getIdLote() { return idLote.get(); }
    public void setIdLote(int value) { idLote.set(value); }
    public IntegerProperty idLoteProperty() { return idLote; }
}