package model;

import javafx.beans.property.*;
import java.time.LocalDate;

public class Produto {
    private final IntegerProperty idProduto;
    private final StringProperty nome;
    private final StringProperty categoria;
    private final StringProperty unidadeMedida;
    private final ObjectProperty<LocalDate> dataCadastro;
    private final StringProperty urlImagem;

    // Construtor simplificado - produto é criado automaticamente ao adicionar lote
    public Produto(int idProduto, String nome) {
        this.idProduto = new SimpleIntegerProperty(idProduto);
        this.nome = new SimpleStringProperty(nome);
        this.categoria = new SimpleStringProperty("");
        this.unidadeMedida = new SimpleStringProperty("unidade");
        this.dataCadastro = new SimpleObjectProperty<>(LocalDate.now());
        this.urlImagem = new SimpleStringProperty("file:med.png");
    }

    // Construtor completo
    public Produto(int idProduto, String nome, String categoria, String unidadeMedida,
                   LocalDate dataCadastro, String urlImagem) {
        this.idProduto = new SimpleIntegerProperty(idProduto);
        this.nome = new SimpleStringProperty(nome);
        this.categoria = new SimpleStringProperty(categoria);
        this.unidadeMedida = new SimpleStringProperty(unidadeMedida);
        this.dataCadastro = new SimpleObjectProperty<>(dataCadastro);
        this.urlImagem = new SimpleStringProperty(urlImagem);
    }

    // idProduto
    public int getIdProduto() { return idProduto.get(); }
    public void setIdProduto(int value) { idProduto.set(value); }
    public IntegerProperty idProdutoProperty() { return idProduto; }

    // nome
    public String getNome() { return nome.get(); }
    public void setNome(String value) { nome.set(value); }
    public StringProperty nomeProperty() { return nome; }

    // categoria
    public String getCategoria() { return categoria.get(); }
    public void setCategoria(String value) { categoria.set(value); }
    public StringProperty categoriaProperty() { return categoria; }

    // unidadeMedida
    public String getUnidadeMedida() { return unidadeMedida.get(); }
    public void setUnidadeMedida(String value) { unidadeMedida.set(value); }
    public StringProperty unidadeMedidaProperty() { return unidadeMedida; }

    // dataCadastro
    public LocalDate getDataCadastro() { return dataCadastro.get(); }
    public void setDataCadastro(LocalDate value) { dataCadastro.set(value); }
    public ObjectProperty<LocalDate> dataCadastroProperty() { return dataCadastro; }

    // urlImagem
    public String getUrlImagem() { return urlImagem.get(); }
    public void setUrlImagem(String value) { urlImagem.set(value); }
    public StringProperty urlImagemProperty() { return urlImagem; }

    @Override
    public String toString() {
        return nome.get();
    }
}