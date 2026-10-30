package br.com.sulwork.events.exception;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

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
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.exc.InvalidFormatException;

/** Every API error comes out as a ProblemDetail (RFC 7807), with the message in {@code detail}. */
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

	private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

	@ExceptionHandler(ResourceNotFoundException.class)
	ProblemDetail onNotFound(ResourceNotFoundException ex) {
		return problem(HttpStatus.NOT_FOUND, "Recurso não encontrado", ex.getMessage());
	}

	@ExceptionHandler(BusinessRuleException.class)
	ProblemDetail onRuleViolation(BusinessRuleException ex) {
		return problem(HttpStatus.UNPROCESSABLE_ENTITY, "Regra de negócio violada", ex.getMessage());
	}

	@ExceptionHandler(ConflictException.class)
	ProblemDetail onConflict(ConflictException ex) {
		return problem(HttpStatus.CONFLICT, "Conflito", ex.getMessage());
	}

	@ExceptionHandler(Exception.class)
	ProblemDetail onUnexpectedFailure(Exception ex) {
		log.error("Erro inesperado", ex);
		return problem(HttpStatus.INTERNAL_SERVER_ERROR, "Erro interno", "Erro interno do servidor.");
	}

	@Override
	protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
			HttpHeaders headers, HttpStatusCode status, WebRequest request) {
		List<FieldErrorDetail> errors = ex.getBindingResult().getFieldErrors().stream()
				.map(error -> new FieldErrorDetail(error.getField(), error.getDefaultMessage()))
				.sorted(Comparator.comparing(FieldErrorDetail::field).thenComparing(FieldErrorDetail::message))
				.toList();
		ProblemDetail problem = problem(HttpStatus.BAD_REQUEST, "Dados inválidos", "Dados inválidos.");
		problem.setProperty("errors", errors);
		return handleExceptionInternal(ex, problem, headers, status, request);
	}

	/** A value outside a closed list names the field and the accepted values, like a validation error. */
	@Override
	protected ResponseEntity<Object> handleHttpMessageNotReadable(HttpMessageNotReadableException ex,
			HttpHeaders headers, HttpStatusCode status, WebRequest request) {
		if (ex.getMostSpecificCause() instanceof InvalidFormatException invalid && invalid.getTargetType() != null
				&& invalid.getTargetType().isEnum()) {
			String accepted = Arrays.stream(invalid.getTargetType().getEnumConstants())
					.map(constant -> ((Enum<?>) constant).name())
					.collect(Collectors.joining(", "));
			ProblemDetail problem = problem(HttpStatus.BAD_REQUEST, "Dados inválidos", "Dados inválidos.");
			problem.setProperty("errors", List.of(new FieldErrorDetail(fieldPath(invalid.getPath()),
					"valor inválido '" + invalid.getValue() + "'. Use um destes: " + accepted + ".")));
			return handleExceptionInternal(ex, problem, headers, status, request);
		}
		return handleExceptionInternal(ex,
				problem(HttpStatus.BAD_REQUEST, "Requisição inválida", "Corpo da requisição ilegível ou mal formado."),
				headers, status, request);
	}

	/** Turns the JSON path into the field name the client sent, for example {@code restrictions[1]}. */
	private static String fieldPath(List<JacksonException.Reference> path) {
		StringBuilder field = new StringBuilder();
		for (JacksonException.Reference reference : path) {
			if (reference.getPropertyName() != null) {
				field.append(field.isEmpty() ? "" : ".").append(reference.getPropertyName());
			}
			else if (reference.getIndex() >= 0) {
				field.append('[').append(reference.getIndex()).append(']');
			}
		}
		return field.toString();
	}

	@Override
	protected ResponseEntity<Object> handleMissingServletRequestParameter(MissingServletRequestParameterException ex,
			HttpHeaders headers, HttpStatusCode status, WebRequest request) {
		return handleExceptionInternal(ex,
				problem(HttpStatus.BAD_REQUEST, "Requisição inválida",
						"Parâmetro obrigatório ausente: '" + ex.getParameterName() + "'."),
				headers, status, request);
	}

	@Override
	protected ResponseEntity<Object> handleTypeMismatch(TypeMismatchException ex,
			HttpHeaders headers, HttpStatusCode status, WebRequest request) {
		String parameter = ex instanceof MethodArgumentTypeMismatchException type ? type.getName() : ex.getPropertyName();
		return handleExceptionInternal(ex,
				problem(HttpStatus.BAD_REQUEST, "Requisição inválida", "Valor inválido para o parâmetro '" + parameter + "'."),
				headers, status, request);
	}

	@Override
	protected ResponseEntity<Object> handleHttpRequestMethodNotSupported(HttpRequestMethodNotSupportedException ex,
			HttpHeaders headers, HttpStatusCode status, WebRequest request) {
		return handleExceptionInternal(ex,
				problem(HttpStatus.METHOD_NOT_ALLOWED, "Método não permitido", "Método HTTP não permitido nesta rota."),
				headers, status, request);
	}

	@Override
	protected ResponseEntity<Object> handleNoResourceFoundException(NoResourceFoundException ex,
			HttpHeaders headers, HttpStatusCode status, WebRequest request) {
		return handleExceptionInternal(ex,
				problem(HttpStatus.NOT_FOUND, "Recurso não encontrado", "Rota não encontrada."),
				headers, status, request);
	}

	private static ProblemDetail problem(HttpStatus status, String title, String detail) {
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
		problem.setTitle(title);
		return problem;
	}

}
