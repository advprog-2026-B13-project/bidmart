package id.ac.ui.cs.advprog.bidmartcore.infrastructure.adapter.input.rest;

import id.ac.ui.cs.advprog.bidmartcore.auth.infrastructure.adapter.input.rest.ApiResponse;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Path;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindingResult;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void handleValidation_returnsFirstFieldError() {
        BindingResult bindingResult = new BeanPropertyBindingResult(new Object(), "obj");
        bindingResult.rejectValue(null, "code", "must not be null");
        bindingResult.addError(new org.springframework.validation.FieldError("obj", "email", "must not be blank"));

        MethodArgumentNotValidException ex = new MethodArgumentNotValidException(
                mock(MethodParameter.class), bindingResult);

        ResponseEntity<ApiResponse<Void>> response = handler.handleValidation(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().isSuccess()).isFalse();
        assertThat(response.getBody().getMessage()).contains("email").contains("must not be blank");
    }

    @Test
    void handleValidation_fallsBackWhenNoFieldErrors() {
        BindingResult bindingResult = new BeanPropertyBindingResult(new Object(), "obj");
        MethodArgumentNotValidException ex = new MethodArgumentNotValidException(
                mock(MethodParameter.class), bindingResult);

        ResponseEntity<ApiResponse<Void>> response = handler.handleValidation(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().getMessage()).isEqualTo("Validation failed");
    }

    @Test
    @SuppressWarnings("unchecked")
    void handleConstraintViolation_returnsFirstViolation() {
        ConstraintViolation<Object> violation = mock(ConstraintViolation.class);
        Path path = mock(Path.class);
        when(path.toString()).thenReturn("amount");
        when(violation.getPropertyPath()).thenReturn(path);
        when(violation.getMessage()).thenReturn("must be positive");

        ConstraintViolationException ex = new ConstraintViolationException(Set.of(violation));

        ResponseEntity<ApiResponse<Void>> response = handler.handleConstraintViolation(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().getMessage()).contains("amount").contains("must be positive");
    }

    @Test
    void handleConstraintViolation_fallsBackWhenEmpty() {
        ConstraintViolationException ex = new ConstraintViolationException(Set.of());

        ResponseEntity<ApiResponse<Void>> response = handler.handleConstraintViolation(ex);

        assertThat(response.getBody().getMessage()).isEqualTo("Validation failed");
    }

    @Test
    void handleIllegalArgument_returns400() {
        ResponseEntity<ApiResponse<Void>> response =
                handler.handleIllegalArgument(new IllegalArgumentException("bad input"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().getMessage()).isEqualTo("bad input");
    }

    @Test
    void handleIllegalState_returns409() {
        ResponseEntity<ApiResponse<Void>> response =
                handler.handleIllegalState(new IllegalStateException("conflict"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody().getMessage()).isEqualTo("conflict");
    }

    @Test
    void handleSecurity_returns403() {
        ResponseEntity<ApiResponse<Void>> response =
                handler.handleSecurity(new SecurityException("denied"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(response.getBody().getMessage()).isEqualTo("denied");
    }

    @Test
    void handleMethodNotSupported_returns405() {
        ResponseEntity<ApiResponse<Void>> response =
                handler.handleMethodNotSupported(new HttpRequestMethodNotSupportedException("DELETE"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.METHOD_NOT_ALLOWED);
    }

    @Test
    void handleNoResource_returns404() {
        NoResourceFoundException ex = mock(NoResourceFoundException.class);
        when(ex.getMessage()).thenReturn("No static resource");

        ResponseEntity<ApiResponse<Void>> response = handler.handleNoResource(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void handleGeneral_returns500WithGenericMessage() {
        ResponseEntity<ApiResponse<Void>> response =
                handler.handleGeneral(new RuntimeException("boom"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody().getMessage()).isEqualTo("Terjadi kesalahan internal");
    }
}
