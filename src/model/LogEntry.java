package model;

import javafx.beans.property.*;
import java.time.LocalDateTime;

public class LogEntry {
    private final IntegerProperty idLog;
    private final ObjectProperty<LocalDateTime> dataHora;
    private final StringProperty tipoOperacao; // "CRIAR", "EDITAR", "EXCLUIR"
    private final StringProperty entidade; // "LOTE", "FORNECEDOR"
    private final StringProperty descricao;
    private final StringProperty usuarioResponsavel;

    public LogEntry(int idLog, LocalDateTime dataHora, String tipoOperacao,
                    String entidade, String descricao, String usuarioResponsavel) {
        this.idLog = new SimpleIntegerProperty(idLog);
        this.dataHora = new SimpleObjectProperty<>(dataHora);
        this.tipoOperacao = new SimpleStringProperty(tipoOperacao);
        this.entidade = new SimpleStringProperty(entidade);
        this.descricao = new SimpleStringProperty(descricao);
        this.usuarioResponsavel = new SimpleStringProperty(usuarioResponsavel);
    }

    // idLog
    public int getIdLog() { return idLog.get(); }
    public void setIdLog(int value) { idLog.set(value); }
    public IntegerProperty idLogProperty() { return idLog; }

    // dataHora
    public LocalDateTime getDataHora() { return dataHora.get(); }
    public void setDataHora(LocalDateTime value) { dataHora.set(value); }
    public ObjectProperty<LocalDateTime> dataHoraProperty() { return dataHora; }

    // tipoOperacao
    public String getTipoOperacao() { return tipoOperacao.get(); }
    public void setTipoOperacao(String value) { tipoOperacao.set(value); }
    public StringProperty tipoOperacaoProperty() { return tipoOperacao; }

    // entidade
    public String getEntidade() { return entidade.get(); }
    public void setEntidade(String value) { entidade.set(value); }
    public StringProperty entidadeProperty() { return entidade; }

    // descricao
    public String getDescricao() { return descricao.get(); }
    public void setDescricao(String value) { descricao.set(value); }
    public StringProperty descricaoProperty() { return descricao; }

    // usuarioResponsavel
    public String getUsuarioResponsavel() { return usuarioResponsavel.get(); }
    public void setUsuarioResponsavel(String value) { usuarioResponsavel.set(value); }
    public StringProperty usuarioResponsavelProperty() { return usuarioResponsavel; }

    @Override
    public String toString() {
        return String.format("[%s] %s - %s: %s",
                dataHora.get().toString(),
                tipoOperacao.get(),
                entidade.get(),
                descricao.get());
    }
}