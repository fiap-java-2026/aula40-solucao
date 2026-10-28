package br.com.fiap.acl.adapter;

import br.com.fiap.acl.domain.Cliente;
import br.com.fiap.acl.domain.TraducaoException;
import jakarta.enterprise.context.ApplicationScoped;

/**
 * Tradução explícita e verificável (slides 10 e 15).
 * Regra de ouro: nunca inventar valor de negócio. Situação desconhecida é falha, não "inativo".
 */
@ApplicationScoped
public class ClienteMapper {

    public Cliente converter(ClienteLegado dto) {
        if (dto == null) {
            throw new TraducaoException("Resposta vazia do legado");
        }
        long id = converterId(dto.COD_CLI());
        String nome = converterNome(dto.NOME());
        boolean ativo = converterSituacao(dto.SIT());
        return new Cliente(id, nome, ativo);
    }

    private static long converterId(String codCli) {
        if (codCli == null || codCli.isBlank()) {
            throw new TraducaoException("COD_CLI ausente");
        }
        try {
            return Long.parseLong(codCli.trim());
        } catch (NumberFormatException e) {
            // armadilha nº 2 do briefing: NumberFormatException não pode vazar como 500
            throw new TraducaoException("COD_CLI não numérico: '" + codCli + "'", e);
        }
    }

    private static String converterNome(String nome) {
        if (nome == null || nome.isBlank()) {
            throw new TraducaoException("NOME ausente ou vazio");
        }
        return nome.trim();
    }

    private static boolean converterSituacao(String sit) {
        if (sit == null) {
            throw new TraducaoException("SIT ausente");
        }
        return switch (sit.trim()) {
            case "A" -> true;
            case "I" -> false;
            // slide 10: "Situação X não deve virar ativo=false silenciosamente"
            default -> throw new TraducaoException("Situação desconhecida: '" + sit + "'");
        };
    }
}
