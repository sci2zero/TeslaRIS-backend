package rs.teslaris.core.configuration;

import io.jsonwebtoken.MalformedJwtException;
import java.util.HashMap;
import java.util.Map;
import javax.persistence.EntityNotFoundException;
import javax.servlet.http.HttpServletRequest;
import javax.validation.ConstraintViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.validation.BindException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import rs.teslaris.core.util.exceptionhandling.ErrorObject;
import rs.teslaris.core.util.exceptionhandling.exception.CantEditPersonException;
import rs.teslaris.core.util.exceptionhandling.exception.CantEditPublicationException;
import rs.teslaris.core.util.exceptionhandling.exception.CantRegisterAdminException;
import rs.teslaris.core.util.exceptionhandling.exception.IdempotencyException;
import rs.teslaris.core.util.exceptionhandling.exception.NonExistingRefreshTokenException;
import rs.teslaris.core.util.exceptionhandling.exception.NotFoundException;
import rs.teslaris.core.util.exceptionhandling.exception.PublisherInUseException;
import rs.teslaris.core.util.exceptionhandling.exception.ResearchAreaInUseException;
import rs.teslaris.core.util.exceptionhandling.exception.SelfRelationException;
import rs.teslaris.core.util.exceptionhandling.exception.StorageException;
import rs.teslaris.core.util.exceptionhandling.exception.TakeOfRoleNotPermittedException;
import rs.teslaris.core.util.exceptionhandling.exception.WrongPasswordProvidedException;

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
    @ExceptionHandler(CantRegisterAdminException.class)
    @ResponseBody
    ErrorObject handleCantRegisterAdminException(HttpServletRequest request,
                                                 CantRegisterAdminException ex) {
        return new ErrorObject(request, ex.getMessage(), HttpStatus.BAD_REQUEST);
    }

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(WrongPasswordProvidedException.class)
    @ResponseBody
    ErrorObject handleWrongPasswordProvidedException(HttpServletRequest request,
                                                     WrongPasswordProvidedException ex) {
        return new ErrorObject(request, ex.getMessage(), HttpStatus.BAD_REQUEST);
    }

    @ResponseStatus(HttpStatus.CONFLICT)
    @ExceptionHandler(ResearchAreaInUseException.class)
    @ResponseBody
    ErrorObject handleResearchAreaInUseException(HttpServletRequest request,
                                                 ResearchAreaInUseException ex) {
        return new ErrorObject(request, ex.getMessage(), HttpStatus.CONFLICT);
    }

    @ResponseStatus(HttpStatus.CONFLICT)
    @ExceptionHandler(PublisherInUseException.class)
    @ResponseBody
    ErrorObject handlePublisherInUseException(HttpServletRequest request,
                                              PublisherInUseException ex) {
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
}
