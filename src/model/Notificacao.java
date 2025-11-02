package model;

import javafx.beans.property.*;
import java.time.LocalDate;

public class Notificacao {
    private final IntegerProperty idNotificacao;
    private final StringProperty tipo; // "ESTOQUE_BAIXO", "VENCIMENTO_PROXIMO", "VENCIDO"
    private final ObjectProperty<LocalDate> dataGeracao;
    private final StringProperty status; // "PENDENTE", "LIDA"
    private final IntegerProperty idLote;

    public Notificacao(int idNotificacao, String tipo, LocalDate dataGeracao,
                       String status, int idLote) {
        this.idNotificacao = new SimpleIntegerProperty(idNotificacao);
        this.tipo = new SimpleStringProperty(tipo);
        this.dataGeracao = new SimpleObjectProperty<>(dataGeracao);
        this.status = new SimpleStringProperty(status);
        this.idLote = new SimpleIntegerProperty(idLote);
    }

    // idNotificacao
    public int getIdNotificacao() { return idNotificacao.get(); }
    public void setIdNotificacao(int value) { idNotificacao.set(value); }
    public IntegerProperty idNotificacaoProperty() { return idNotificacao; }

    // tipo
    public String getTipo() { return tipo.get(); }
    public void setTipo(String value) { tipo.set(value); }
    public StringProperty tipoProperty() { return tipo; }

    // dataGeracao
    public LocalDate getDataGeracao() { return dataGeracao.get(); }
    public void setDataGeracao(LocalDate value) { dataGeracao.set(value); }
    public ObjectProperty<LocalDate> dataGeracaoProperty() { return dataGeracao; }

    // status
    public String getStatus() { return status.get(); }
    public void setStatus(String value) { status.set(value); }
    public StringProperty statusProperty() { return status; }

    // idLote
    public int getIdLote() { return idLote.get(); }
    public void setIdLote(int value) { idLote.set(value); }
    public IntegerProperty idLoteProperty() { return idLote; }
}