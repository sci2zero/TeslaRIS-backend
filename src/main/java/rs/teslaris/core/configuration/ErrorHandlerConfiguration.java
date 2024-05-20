package rs.teslaris.core.configuration;

import io.jsonwebtoken.MalformedJwtException;
import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import java.util.HashMap;
import java.util.Map;
import org.hibernate.StaleStateException;
import org.springframework.http.HttpStatus;
import org.springframework.validation.BindException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import rs.teslaris.core.util.exceptionhandling.ErrorObject;
import rs.teslaris.core.util.exceptionhandling.exception.CantConstructRestTemplateException;
import rs.teslaris.core.util.exceptionhandling.exception.CantEditPersonException;
import rs.teslaris.core.util.exceptionhandling.exception.CantEditPublicationException;
import rs.teslaris.core.util.exceptionhandling.exception.IdempotencyException;
import rs.teslaris.core.util.exceptionhandling.exception.JournalReferenceConstraintViolationException;
import rs.teslaris.core.util.exceptionhandling.exception.LoadingException;
import rs.teslaris.core.util.exceptionhandling.exception.NonExistingRefreshTokenException;
import rs.teslaris.core.util.exceptionhandling.exception.NotFoundException;
import rs.teslaris.core.util.exceptionhandling.exception.PasswordException;
import rs.teslaris.core.util.exceptionhandling.exception.PublisherReferenceConstraintViolationException;
import rs.teslaris.core.util.exceptionhandling.exception.ResearchAreaReferenceConstraintViolationException;
import rs.teslaris.core.util.exceptionhandling.exception.SelfRelationException;
import rs.teslaris.core.util.exceptionhandling.exception.StorageException;
import rs.teslaris.core.util.exceptionhandling.exception.TakeOfRoleNotPermittedException;
import rs.teslaris.core.util.exceptionhandling.exception.UserAlreadyExistsException;

@ControllerAdvice
public class ErrorHandlerConfiguration {

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(BindException.class)
    @ResponseBody
    public ErrorObject handleMethodBindingException(HttpServletRequest request, BindException ex) {
        Map<String, String> errors = new HashMap<>();

        ex.getBindingResult().getFieldErrors()
            .forEach(e -> errors.put(e.getField(), e.getDefaultMessage()));

        ex.getBindingResult().getGlobalErrors()
            .forEach(e -> errors.put(e.getObjectName(), e.getDefaultMessage()));

        return new ErrorObject(request, ex.getLocalizedMessage(), HttpStatus.BAD_REQUEST, errors);
    }

    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    @ExceptionHandler(StorageException.class)
    @ResponseBody
    ErrorObject handleStorageException(HttpServletRequest request, StorageException ex) {
        return new ErrorObject(request, ex.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(ConstraintViolationException.class)
    @ResponseBody
    ErrorObject handleConstraintViolationException(HttpServletRequest request,
                                                   ConstraintViolationException ex) {
        return new ErrorObject(request, ex.getMessage(), HttpStatus.BAD_REQUEST);
    }

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(IdempotencyException.class)
    @ResponseBody
    ErrorObject handleIdempotencyException(HttpServletRequest request, IdempotencyException ex) {
        return new ErrorObject(request, ex.getMessage(), HttpStatus.BAD_REQUEST);
    }

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseBody
    ErrorObject handleIllegalArgumentException(HttpServletRequest request,
                                               IllegalArgumentException ex) {
        return new ErrorObject(request, ex.getMessage(), HttpStatus.BAD_REQUEST);
    }

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(CantEditPersonException.class)
    @ResponseBody
    ErrorObject handleCantEditPersonException(HttpServletRequest request,
                                              CantEditPersonException ex) {
        return new ErrorObject(request, ex.getMessage(), HttpStatus.BAD_REQUEST);
    }

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(CantEditPublicationException.class)
    @ResponseBody
    ErrorObject handleCantEditPublicationException(HttpServletRequest request,
                                                   CantEditPublicationException ex) {
        return new ErrorObject(request, ex.getMessage(), HttpStatus.BAD_REQUEST);
    }

    @ResponseStatus(HttpStatus.NOT_FOUND)
    @ExceptionHandler(NotFoundException.class)
    @ResponseBody
    ErrorObject handleNotFoundException(HttpServletRequest request, NotFoundException ex) {
        return new ErrorObject(request, ex.getMessage(), HttpStatus.NOT_FOUND);
    }

    @ResponseStatus(HttpStatus.NOT_FOUND)
    @ExceptionHandler(EntityNotFoundException.class)
    @ResponseBody
    ErrorObject handleEntityNotFoundException(HttpServletRequest request,
                                              EntityNotFoundException ex) {
        return new ErrorObject(request, ex.getMessage(), HttpStatus.NOT_FOUND);
    }

    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    @ExceptionHandler(NonExistingRefreshTokenException.class)
    @ResponseBody
    ErrorObject handleNonExistingRefreshTokenException(HttpServletRequest request,
                                                       NonExistingRefreshTokenException ex) {
        return new ErrorObject(request, ex.getMessage(), HttpStatus.UNAUTHORIZED);
    }

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(TakeOfRoleNotPermittedException.class)
    @ResponseBody
    ErrorObject handleTakeOfRoleNotPermittedException(HttpServletRequest request,
                                                      TakeOfRoleNotPermittedException ex) {
        return new ErrorObject(request, ex.getMessage(), HttpStatus.BAD_REQUEST);
    }

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(PasswordException.class)
    @ResponseBody
    ErrorObject handleWrongPasswordProvidedException(HttpServletRequest request,
                                                     PasswordException ex) {
        return new ErrorObject(request, ex.getMessage(), HttpStatus.BAD_REQUEST);
    }

    @ResponseStatus(HttpStatus.CONFLICT)
    @ExceptionHandler(ResearchAreaReferenceConstraintViolationException.class)
    @ResponseBody
    ErrorObject handleResearchAreaInUseException(HttpServletRequest request,
                                                 ResearchAreaReferenceConstraintViolationException ex) {
        return new ErrorObject(request, ex.getMessage(), HttpStatus.CONFLICT);
    }

    @ResponseStatus(HttpStatus.CONFLICT)
    @ExceptionHandler(PublisherReferenceConstraintViolationException.class)
    @ResponseBody
    ErrorObject handlePublisherInUseException(HttpServletRequest request,
                                              PublisherReferenceConstraintViolationException ex) {
        return new ErrorObject(request, ex.getMessage(), HttpStatus.CONFLICT);
    }

    @ResponseStatus(HttpStatus.CONFLICT)
    @ExceptionHandler(JournalReferenceConstraintViolationException.class)
    @ResponseBody
    ErrorObject handleJournalInUseException(HttpServletRequest request,
                                            JournalReferenceConstraintViolationException ex) {
        return new ErrorObject(request, ex.getMessage(), HttpStatus.CONFLICT);
    }

    @ResponseStatus(HttpStatus.CONFLICT)
    @ExceptionHandler(SelfRelationException.class)
    @ResponseBody
    ErrorObject handleSelfRelationException(HttpServletRequest request, SelfRelationException ex) {
        return new ErrorObject(request, ex.getMessage(), HttpStatus.CONFLICT);
    }

    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    @ExceptionHandler(MalformedJwtException.class)
    @ResponseBody
    ErrorObject handleMalformedJwtException(HttpServletRequest request, MalformedJwtException ex) {
        return new ErrorObject(request, ex.getMessage(), HttpStatus.UNAUTHORIZED);
    }

    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    @ExceptionHandler(CantConstructRestTemplateException.class)
    @ResponseBody
    ErrorObject handleCantConstructRestTemplateException(HttpServletRequest request,
                                                         CantConstructRestTemplateException ex) {
        return new ErrorObject(request, ex.getMessage(), HttpStatus.UNAUTHORIZED);
    }

    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    @ExceptionHandler(LoadingException.class)
    @ResponseBody
    ErrorObject handleLoadingException(HttpServletRequest request, LoadingException ex) {
        return new ErrorObject(request, ex.getMessage(), HttpStatus.UNAUTHORIZED);
    }

    @ResponseStatus(HttpStatus.CONFLICT)
    @ExceptionHandler(UserAlreadyExistsException.class)
    @ResponseBody
    ErrorObject handleUserAlreadyExistsException(HttpServletRequest request,
                                                 UserAlreadyExistsException ex) {
        return new ErrorObject(request, ex.getMessage(), HttpStatus.CONFLICT);
    }

    @ResponseStatus(HttpStatus.CONFLICT)
    @ExceptionHandler(StaleStateException.class)
    @ResponseBody
    ErrorObject handleStaleStateException(HttpServletRequest request, StaleStateException ex) {
        return new ErrorObject(request, ex.getMessage(), HttpStatus.CONFLICT);
    }
}
