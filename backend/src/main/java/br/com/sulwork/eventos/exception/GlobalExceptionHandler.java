package br.com.sulwork.eventos.exception;

import java.util.Comparator;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.TypeMismatchException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/** Todo erro da API sai como ProblemDetail (RFC 7807), com a mensagem em {@code detail}. */
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

	private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

	@ExceptionHandler(ResourceNotFoundException.class)
	ProblemDetail aoNaoEncontrar(ResourceNotFoundException ex) {
		return problema(HttpStatus.NOT_FOUND, "Recurso não encontrado", ex.getMessage());
	}

	@ExceptionHandler(BusinessRuleException.class)
	ProblemDetail aoQuebrarRegra(BusinessRuleException ex) {
		return problema(HttpStatus.UNPROCESSABLE_ENTITY, "Regra de negócio violada", ex.getMessage());
	}

	@ExceptionHandler(ConflictException.class)
	ProblemDetail aoEntrarEmConflito(ConflictException ex) {
		return problema(HttpStatus.CONFLICT, "Conflito", ex.getMessage());
	}

	@ExceptionHandler(Exception.class)
	ProblemDetail aoFalharSemExplicacao(Exception ex) {
		log.error("Erro inesperado", ex);
		return problema(HttpStatus.INTERNAL_SERVER_ERROR, "Erro interno", "Erro interno do servidor.");
	}

	@Override
	protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
			HttpHeaders headers, HttpStatusCode status, WebRequest request) {
		List<ErroCampo> erros = ex.getBindingResult().getFieldErrors().stream()
				.map(erro -> new ErroCampo(erro.getField(), erro.getDefaultMessage()))
				.sorted(Comparator.comparing(ErroCampo::campo).thenComparing(ErroCampo::mensagem))
				.toList();
		ProblemDetail problema = problema(HttpStatus.BAD_REQUEST, "Dados inválidos", "Dados inválidos.");
		problema.setProperty("erros", erros);
		return handleExceptionInternal(ex, problema, headers, status, request);
	}

	@Override
	protected ResponseEntity<Object> handleHttpMessageNotReadable(HttpMessageNotReadableException ex,
			HttpHeaders headers, HttpStatusCode status, WebRequest request) {
		return handleExceptionInternal(ex,
				problema(HttpStatus.BAD_REQUEST, "Requisição inválida", "Corpo da requisição ilegível ou mal formado."),
				headers, status, request);
	}

	@Override
	protected ResponseEntity<Object> handleTypeMismatch(TypeMismatchException ex,
			HttpHeaders headers, HttpStatusCode status, WebRequest request) {
		String parametro = ex instanceof MethodArgumentTypeMismatchException tipo ? tipo.getName() : ex.getPropertyName();
		return handleExceptionInternal(ex,
				problema(HttpStatus.BAD_REQUEST, "Requisição inválida", "Valor inválido para o parâmetro '" + parametro + "'."),
				headers, status, request);
	}

	@Override
	protected ResponseEntity<Object> handleHttpRequestMethodNotSupported(HttpRequestMethodNotSupportedException ex,
			HttpHeaders headers, HttpStatusCode status, WebRequest request) {
		return handleExceptionInternal(ex,
				problema(HttpStatus.METHOD_NOT_ALLOWED, "Método não permitido", "Método HTTP não permitido nesta rota."),
				headers, status, request);
	}

	@Override
	protected ResponseEntity<Object> handleNoResourceFoundException(NoResourceFoundException ex,
			HttpHeaders headers, HttpStatusCode status, WebRequest request) {
		return handleExceptionInternal(ex,
				problema(HttpStatus.NOT_FOUND, "Recurso não encontrado", "Rota não encontrada."),
				headers, status, request);
	}

	private static ProblemDetail problema(HttpStatus status, String titulo, String detalhe) {
		ProblemDetail problema = ProblemDetail.forStatusAndDetail(status, detalhe);
		problema.setTitle(titulo);
		return problema;
	}

}
