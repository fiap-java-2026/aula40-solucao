package br.com.fiap.acl.adapter;

import br.com.fiap.acl.domain.BuscaCliente;
import br.com.fiap.acl.domain.Cliente;
import br.com.fiap.acl.domain.ClienteNaoEncontradoException;
import br.com.fiap.acl.domain.FornecedorIndisponivelException;
import br.com.fiap.acl.domain.TraducaoException;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.ProcessingException;
import jakarta.ws.rs.WebApplicationException;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.logging.Logger;

import java.io.IOException;
import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.util.concurrent.TimeoutException;

/**
 * Adaptador (slide 23): concentra as particularidades do legado e traduz
 * dados E falhas. Separa erro de negócio (404), erro de tradução (502)
 * e erro de transporte (503) — slide 13.
 */
@ApplicationScoped
public class ClienteAdapter implements BuscaCliente {

    private static final Logger LOG = Logger.getLogger(ClienteAdapter.class);

    @Inject
    @RestClient
    LegadoClient client;

    @Inject
    ClienteMapper mapper;

    @Override
    public Cliente buscar(long id) {
        ClienteLegado dto;
        try {
            dto = client.buscar(id);
        } catch (WebApplicationException e) {
            int status = e.getResponse() != null ? e.getResponse().getStatus() : 0;
            if (status == 404) {
                throw new ClienteNaoEncontradoException(id);
            }
            LOG.warnf("legado respondeu HTTP %d para o cliente %d", status, id);
            throw new FornecedorIndisponivelException("Legado respondeu HTTP " + status, e);
        } catch (ProcessingException e) {
            if (ehFalhaDeTransporte(e)) {
                LOG.warnf("legado não respondeu para o cliente %d: %s", id, e.getMessage());
                throw new FornecedorIndisponivelException("Legado não respondeu a tempo", e);
            }
            // o legado respondeu, mas o corpo não é o contrato combinado (ex.: JSON inválido)
            throw new TraducaoException("Payload inválido do legado", e);
        }
        // Não logar o payload completo (slide 15, notas): dados pessoais.
        return mapper.converter(dto);
    }

    private static boolean ehFalhaDeTransporte(Throwable e) {
        for (Throwable t = e; t != null; t = t.getCause()) {
            if (t instanceof SocketTimeoutException || t instanceof ConnectException
                    || t instanceof TimeoutException) {
                return true;
            }
            if (t instanceof IOException && t.getMessage() != null
                    && t.getMessage().toLowerCase().contains("timeout")) {
                return true;
            }
            String nome = t.getClass().getSimpleName();
            if (nome.contains("Timeout") || nome.contains("Connect")) {
                return true;
            }
        }
        return false;
    }
}
