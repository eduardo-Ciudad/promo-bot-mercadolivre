package com.eduar.promobot.scraperlocal.scraping;

public interface SessaoBusca extends AutoCloseable {

    PaginaPromocoes buscarPagina(int numeroPagina);

    @Override
    void close();
}
