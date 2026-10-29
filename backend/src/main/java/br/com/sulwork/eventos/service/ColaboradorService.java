package br.com.sulwork.eventos.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.sulwork.eventos.domain.Colaborador;
import br.com.sulwork.eventos.dto.AtualizarColaboradorRequest;
import br.com.sulwork.eventos.dto.ColaboradorResponse;
import br.com.sulwork.eventos.exception.ResourceNotFoundException;
import br.com.sulwork.eventos.repository.ColaboradorRepository;
import br.com.sulwork.eventos.validation.CpfValidator;

@Service
@Transactional
public class ColaboradorService {

	private final ColaboradorRepository colaboradores;

	public ColaboradorService(ColaboradorRepository colaboradores) {
		this.colaboradores = colaboradores;
	}

	@Transactional(readOnly = true)
	public ColaboradorResponse buscarPorCpf(String cpf) {
		String normalizado = CpfValidator.normalizeAndValidate(cpf);
		return colaboradores.findByCpf(normalizado).map(ColaboradorResponse::de)
				.orElseThrow(this::naoEncontrado);
	}

	@Transactional(readOnly = true)
	public ColaboradorResponse buscar(Long id) {
		return ColaboradorResponse.de(buscarEntidade(id));
	}

	/** O único lugar que altera o cadastro de uma pessoa. O CPF não muda: mudar o CPF seria trocar de pessoa. */
	public ColaboradorResponse atualizar(Long id, AtualizarColaboradorRequest request) {
		Colaborador colaborador = buscarEntidade(id);
		colaborador.setNome(request.nome().strip());
		colaborador.getRestricoes().clear();
		colaborador.getRestricoes().addAll(request.restricoes());
		return ColaboradorResponse.de(colaboradores.saveAndFlush(colaborador));
	}

	private Colaborador buscarEntidade(Long id) {
		return colaboradores.findById(id).orElseThrow(this::naoEncontrado);
	}

	private ResourceNotFoundException naoEncontrado() {
		return new ResourceNotFoundException("Colaborador não encontrado.");
	}

}
