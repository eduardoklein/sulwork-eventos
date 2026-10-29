package br.com.sulwork.eventos.repository;

/** Os totais de uma categoria, somados pelo banco. Os itens se dividem em obrigatórios e opcionais. */
public interface CoberturaDaCategoria {

	Long getCategoriaId();

	String getNome();

	String getUnidade();

	Integer getMetaQuantidade();

	Long getTotalItens();

	Long getNecessarioObrigatorios();

	Long getNecessarioOpcionais();

	Long getContribuidoObrigatorios();

	Long getContribuidoOpcionais();

}
