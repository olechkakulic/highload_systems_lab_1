package ru.lab.shelter.exception;

import jakarta.validation.ConstraintViolationException;
import java.util.LinkedHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.http.*;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {
  private static final Logger LOG = LoggerFactory.getLogger(GlobalExceptionHandler.class);

  private ResponseEntity<Object> error(HttpStatusCode status, String detail) {
    ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
    return ResponseEntity.status(status).body(problem);
  }

  @ExceptionHandler(ApiException.class)
  ResponseEntity<Object> business(ApiException ex) {
    return error(ex.getStatus(), ex.getMessage());
  }

  @ExceptionHandler(DataIntegrityViolationException.class)
  ResponseEntity<Object> integrity(DataIntegrityViolationException ex) {
    return error(
        HttpStatus.CONFLICT,
        "Операция нарушает уникальность данных или запись используется другими сущностями");
  }

  @ExceptionHandler(PessimisticLockingFailureException.class)
  ResponseEntity<Object> locked(PessimisticLockingFailureException ex) {
    return error(HttpStatus.CONFLICT, "Данные изменяются другим запросом. Повторите операцию");
  }

  @ExceptionHandler(ConstraintViolationException.class)
  ResponseEntity<Object> constraints(ConstraintViolationException ex) {
    return error(HttpStatus.BAD_REQUEST, "Данные не прошли проверку ограничений");
  }

  @Override
  protected ResponseEntity<Object> handleMethodArgumentNotValid(
      MethodArgumentNotValidException ex,
      HttpHeaders headers,
      HttpStatusCode status,
      WebRequest request) {
    ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, "Проверьте поля запроса");
    var fields = new LinkedHashMap<String, String>();
    ex.getBindingResult()
        .getFieldErrors()
        .forEach(e -> fields.putIfAbsent(e.getField(), e.getDefaultMessage()));
    problem.setProperty("errors", fields);
    return ResponseEntity.status(status).body(problem);
  }

  @Override
  protected ResponseEntity<Object> handleExceptionInternal(
      Exception ex, Object body, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
    ProblemDetail problem =
        ProblemDetail.forStatusAndDetail(
            status,
            status.value() == 404
                ? "Ресурс не найден"
                : "Некорректный запрос: проверьте адрес, метод, параметры и JSON");
    return new ResponseEntity<>(problem, headers, status);
  }

  @ExceptionHandler(Exception.class)
  ResponseEntity<Object> unexpected(Exception ex) {
    LOG.error("Unexpected request error", ex);
    return error(HttpStatus.INTERNAL_SERVER_ERROR, "Внутренняя ошибка сервера");
  }
}
