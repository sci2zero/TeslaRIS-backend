package rs.teslaris.core.configuration;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.MalformedJwtException;
import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.ValidationException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.StaleStateException;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.SchedulingException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.BindException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import rs.teslaris.core.util.email.EmailUtil;
import rs.teslaris.core.util.exceptionhandling.ErrorObject;
import rs.teslaris.core.util.exceptionhandling.exception.AssessmentClassificationReferenceConstraintViolationException;
import rs.teslaris.core.util.exceptionhandling.exception.BackupException;
import rs.teslaris.core.util.exceptionhandling.exception.BookSeriesReferenceConstraintViolationException;
import rs.teslaris.core.util.exceptionhandling.exception.CantConstructRestTemplateException;
import rs.teslaris.core.util.exceptionhandling.exception.CantEditException;
import rs.teslaris.core.util.exceptionhandling.exception.CaptchaException;
import rs.teslaris.core.util.exceptionhandling.exception.ConferenceReferenceConstraintViolationException;
import rs.teslaris.core.util.exceptionhandling.exception.DocumentHarvestException;
import rs.teslaris.core.util.exceptionhandling.exception.IdempotencyException;
import rs.teslaris.core.util.exceptionhandling.exception.IdentifierException;
import rs.teslaris.core.util.exceptionhandling.exception.IndicatorReferenceConstraintViolationException;
import rs.teslaris.core.util.exceptionhandling.exception.InvalidApiKeyException;
import rs.teslaris.core.util.exceptionhandling.exception.InvalidFileSectionException;
import rs.teslaris.core.util.exceptionhandling.exception.JournalReferenceConstraintViolationException;
import rs.teslaris.core.util.exceptionhandling.exception.LoadingException;
import rs.teslaris.core.util.exceptionhandling.exception.MissingDataException;
import rs.teslaris.core.util.exceptionhandling.exception.MonographReferenceConstraintViolationException;
import rs.teslaris.core.util.exceptionhandling.exception.NetworkException;
import rs.teslaris.core.util.exceptionhandling.exception.NonExistingRefreshTokenException;
import rs.teslaris.core.util.exceptionhandling.exception.NotFoundException;
import rs.teslaris.core.util.exceptionhandling.exception.OrganisationUnitReferenceConstraintViolationException;
import rs.teslaris.core.util.exceptionhandling.exception.PasswordException;
import rs.teslaris.core.util.exceptionhandling.exception.PersonReferenceConstraintViolationException;
import rs.teslaris.core.util.exceptionhandling.exception.ProceedingsReferenceConstraintViolationException;
import rs.teslaris.core.util.exceptionhandling.exception.PromotionException;
import rs.teslaris.core.util.exceptionhandling.exception.PublisherReferenceConstraintViolationException;
import rs.teslaris.core.util.exceptionhandling.exception.RecordAlreadyLoadedException;
import rs.teslaris.core.util.exceptionhandling.exception.ReferenceConstraintException;
import rs.teslaris.core.util.exceptionhandling.exception.RegistrationException;
import rs.teslaris.core.util.exceptionhandling.exception.RegistryBookException;
import rs.teslaris.core.util.exceptionhandling.exception.ResearchAreaReferenceConstraintViolationException;
import rs.teslaris.core.util.exceptionhandling.exception.ScopusIdMissingException;
import rs.teslaris.core.util.exceptionhandling.exception.SelfRelationException;
import rs.teslaris.core.util.exceptionhandling.exception.StorageException;
import rs.teslaris.core.util.exceptionhandling.exception.TakeOfRoleNotPermittedException;
import rs.teslaris.core.util.exceptionhandling.exception.ThesisException;
import rs.teslaris.core.util.exceptionhandling.exception.TypeNotAllowedException;
import rs.teslaris.core.util.exceptionhandling.exception.UserAlreadyExistsException;
import rs.teslaris.core.util.exceptionhandling.exception.UserIsNotResearcherException;
import rs.teslaris.core.util.tracing.TraceMDCKeys;

@RestControllerAdvice
@Slf4j
@RequiredArgsConstructor
public class ErrorHandlerConfiguration {

    private final EmailUtil emailUtil;


    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(BindException.class)
    @ResponseBody
    public ErrorObject handleMethodBindingException(HttpServletRequest request, BindException ex) {
        Map<String, String> errors = new HashMap<>();

        ex.getBindingResult().getFieldErrors()
            .forEach(e -> errors.put(e.getField(), e.getDefaultMessage()));

        ex.getBindingResult().getGlobalErrors()
            .forEach(e -> errors.put(e.getObjectName(), e.getDefaultMessage()));

        return buildErrorObject(request, ex.getLocalizedMessage(), HttpStatus.BAD_REQUEST, errors);
    }

    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    @ExceptionHandler(StorageException.class)
    @ResponseBody
    ErrorObject handleStorageException(HttpServletRequest request, StorageException ex) {
        return buildErrorObject(request, ex.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(ConstraintViolationException.class)
    @ResponseBody
    ErrorObject handleConstraintViolationException(HttpServletRequest request,
                                                   ConstraintViolationException ex) {
        return buildErrorObject(request, ex.getMessage(), HttpStatus.BAD_REQUEST);
    }

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(IdempotencyException.class)
    @ResponseBody
    ErrorObject handleIdempotencyException(HttpServletRequest request, IdempotencyException ex) {
        return buildErrorObject(request, ex.getMessage(), HttpStatus.BAD_REQUEST);
    }

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseBody
    ErrorObject handleIllegalArgumentException(HttpServletRequest request,
                                               IllegalArgumentException ex) {
        return buildErrorObject(request, ex.getMessage(), HttpStatus.BAD_REQUEST);
    }

    @ResponseStatus(HttpStatus.NOT_FOUND)
    @ExceptionHandler(NotFoundException.class)
    @ResponseBody
    ErrorObject handleNotFoundException(HttpServletRequest request, NotFoundException ex) {
        return buildErrorObject(request, ex.getMessage(), HttpStatus.NOT_FOUND);
    }

    @ResponseStatus(HttpStatus.NOT_FOUND)
    @ExceptionHandler(EntityNotFoundException.class)
    @ResponseBody
    ErrorObject handleEntityNotFoundException(HttpServletRequest request,
                                              EntityNotFoundException ex) {
        return buildErrorObject(request, ex.getMessage(), HttpStatus.NOT_FOUND);
    }

    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    @ExceptionHandler(NonExistingRefreshTokenException.class)
    @ResponseBody
    ErrorObject handleNonExistingRefreshTokenException(HttpServletRequest request,
                                                       NonExistingRefreshTokenException ex) {
        return buildErrorObject(request, ex.getMessage(), HttpStatus.UNAUTHORIZED);
    }

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(TakeOfRoleNotPermittedException.class)
    @ResponseBody
    ErrorObject handleTakeOfRoleNotPermittedException(HttpServletRequest request,
                                                      TakeOfRoleNotPermittedException ex) {
        return buildErrorObject(request, ex.getMessage(), HttpStatus.BAD_REQUEST);
    }

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(PasswordException.class)
    @ResponseBody
    ErrorObject handleWrongPasswordProvidedException(HttpServletRequest request,
                                                     PasswordException ex) {
        return buildErrorObject(request, ex.getMessage(), HttpStatus.BAD_REQUEST);
    }

    @ResponseStatus(HttpStatus.CONFLICT)
    @ExceptionHandler(ResearchAreaReferenceConstraintViolationException.class)
    @ResponseBody
    ErrorObject handleResearchAreaInUseException(HttpServletRequest request,
                                                 ResearchAreaReferenceConstraintViolationException ex) {
        return buildErrorObject(request, ex.getMessage(), HttpStatus.CONFLICT);
    }

    @ResponseStatus(HttpStatus.CONFLICT)
    @ExceptionHandler(PublisherReferenceConstraintViolationException.class)
    @ResponseBody
    ErrorObject handlePublisherInUseException(HttpServletRequest request,
                                              PublisherReferenceConstraintViolationException ex) {
        return buildErrorObject(request, ex.getMessage(), HttpStatus.CONFLICT);
    }

    @ResponseStatus(HttpStatus.CONFLICT)
    @ExceptionHandler(JournalReferenceConstraintViolationException.class)
    @ResponseBody
    ErrorObject handleJournalInUseException(HttpServletRequest request,
                                            JournalReferenceConstraintViolationException ex) {
        return buildErrorObject(request, ex.getMessage(), HttpStatus.CONFLICT);
    }

    @ResponseStatus(HttpStatus.CONFLICT)
    @ExceptionHandler(SelfRelationException.class)
    @ResponseBody
    ErrorObject handleSelfRelationException(HttpServletRequest request, SelfRelationException ex) {
        return buildErrorObject(request, ex.getMessage(), HttpStatus.CONFLICT);
    }

    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    @ExceptionHandler(MalformedJwtException.class)
    @ResponseBody
    ErrorObject handleMalformedJwtException(HttpServletRequest request, MalformedJwtException ex) {
        return buildErrorObject(request, ex.getMessage(), HttpStatus.UNAUTHORIZED);
    }

    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    @ExceptionHandler(ExpiredJwtException.class)
    @ResponseBody
    ErrorObject handleExpiredJwtException(HttpServletRequest request, ExpiredJwtException ex) {
        return buildErrorObject(request, ex.getMessage(), HttpStatus.UNAUTHORIZED);
    }

    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    @ExceptionHandler(CantConstructRestTemplateException.class)
    @ResponseBody
    ErrorObject handleCantConstructRestTemplateException(HttpServletRequest request,
                                                         CantConstructRestTemplateException ex) {
        return buildErrorObject(request, ex.getMessage(), HttpStatus.UNAUTHORIZED);
    }

    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    @ExceptionHandler(LoadingException.class)
    @ResponseBody
    ErrorObject handleLoadingException(HttpServletRequest request, LoadingException ex) {
        return buildErrorObject(request, ex.getMessage(), HttpStatus.UNAUTHORIZED);
    }

    @ResponseStatus(HttpStatus.CONFLICT)
    @ExceptionHandler(UserAlreadyExistsException.class)
    @ResponseBody
    ErrorObject handleUserAlreadyExistsException(HttpServletRequest request,
                                                 UserAlreadyExistsException ex) {
        return buildErrorObject(request, ex.getMessage(), HttpStatus.CONFLICT);
    }

    @ResponseStatus(HttpStatus.CONFLICT)
    @ExceptionHandler(StaleStateException.class)
    @ResponseBody
    ErrorObject handleStaleStateException(HttpServletRequest request, StaleStateException ex) {
        return buildErrorObject(request, ex.getMessage(), HttpStatus.CONFLICT);
    }

    @ResponseStatus(HttpStatus.CONFLICT)
    @ExceptionHandler(UserIsNotResearcherException.class)
    @ResponseBody
    ErrorObject handleUserIsNotResearcherException(HttpServletRequest request,
                                                   UserIsNotResearcherException ex) {
        return buildErrorObject(request, ex.getMessage(), HttpStatus.CONFLICT);
    }

    @ResponseStatus(HttpStatus.CONFLICT)
    @ExceptionHandler(RecordAlreadyLoadedException.class)
    @ResponseBody
    ErrorObject handleRecordAlreadyLoadedException(HttpServletRequest request,
                                                   RecordAlreadyLoadedException ex) {
        return buildErrorObject(request, ex.getMessage(), HttpStatus.CONFLICT);
    }

    @ResponseStatus(HttpStatus.CONFLICT)
    @ExceptionHandler(ProceedingsReferenceConstraintViolationException.class)
    @ResponseBody
    ErrorObject handleProceedingsReferenceConstraintViolationException(HttpServletRequest request,
                                                                       ProceedingsReferenceConstraintViolationException ex) {
        return buildErrorObject(request, ex.getMessage(), HttpStatus.CONFLICT);
    }

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(MissingDataException.class)
    @ResponseBody
    ErrorObject handleMissingDataException(HttpServletRequest request, MissingDataException ex) {
        return buildErrorObject(request, ex.getMessage(), HttpStatus.BAD_REQUEST);
    }

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(ConferenceReferenceConstraintViolationException.class)
    @ResponseBody
    ErrorObject handleConferenceReferenceConstraintViolationException(HttpServletRequest request,
                                                                      ConferenceReferenceConstraintViolationException ex) {
        return buildErrorObject(request, ex.getMessage(), HttpStatus.BAD_REQUEST);
    }

    @ResponseStatus(HttpStatus.CONFLICT)
    @ExceptionHandler(IdentifierException.class)
    @ResponseBody
    ErrorObject handleIdentifierException(HttpServletRequest request,
                                          IdentifierException ex) {
        return buildErrorObject(request, ex.getMessage(), HttpStatus.CONFLICT);
    }

    @ResponseStatus(HttpStatus.CONFLICT)
    @ExceptionHandler(PersonReferenceConstraintViolationException.class)
    @ResponseBody
    ErrorObject handlePersonReferenceConstraintViolationException(HttpServletRequest request,
                                                                  PersonReferenceConstraintViolationException ex) {
        return buildErrorObject(request, ex.getMessage(), HttpStatus.CONFLICT);
    }

    @ResponseStatus(HttpStatus.CONFLICT)
    @ExceptionHandler(OrganisationUnitReferenceConstraintViolationException.class)
    @ResponseBody
    ErrorObject handleOrganisationUnitReferenceConstraintViolationException(
        HttpServletRequest request,
        OrganisationUnitReferenceConstraintViolationException ex) {
        return buildErrorObject(request, ex.getMessage(), HttpStatus.CONFLICT);
    }

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(TypeNotAllowedException.class)
    @ResponseBody
    ErrorObject handleTypeNotAllowedException(HttpServletRequest request,
                                              TypeNotAllowedException ex) {
        return buildErrorObject(request, ex.getMessage(), HttpStatus.BAD_REQUEST);
    }

    @ResponseStatus(HttpStatus.CONFLICT)
    @ExceptionHandler(AssessmentClassificationReferenceConstraintViolationException.class)
    @ResponseBody
    ErrorObject handleAssessmentClassificationReferenceConstraintViolationException(
        HttpServletRequest request,
        AssessmentClassificationReferenceConstraintViolationException ex) {
        return buildErrorObject(request, ex.getMessage(), HttpStatus.CONFLICT);
    }

    @ResponseStatus(HttpStatus.CONFLICT)
    @ExceptionHandler(IndicatorReferenceConstraintViolationException.class)
    @ResponseBody
    ErrorObject handleIndicatorReferenceConstraintViolationException(
        HttpServletRequest request,
        IndicatorReferenceConstraintViolationException ex) {
        return buildErrorObject(request, ex.getMessage(), HttpStatus.CONFLICT);
    }

    @ResponseStatus(HttpStatus.CONFLICT)
    @ExceptionHandler(MonographReferenceConstraintViolationException.class)
    @ResponseBody
    ErrorObject handleMonographReferenceConstraintViolationException(HttpServletRequest request,
                                                                     MonographReferenceConstraintViolationException ex) {
        return buildErrorObject(request, ex.getMessage(), HttpStatus.CONFLICT);
    }

    @ResponseStatus(HttpStatus.CONFLICT)
    @ExceptionHandler(BookSeriesReferenceConstraintViolationException.class)
    @ResponseBody
    ErrorObject handleBookSeriesReferenceConstraintViolationException(HttpServletRequest request,
                                                                      BookSeriesReferenceConstraintViolationException ex) {
        return buildErrorObject(request, ex.getMessage(), HttpStatus.CONFLICT);
    }

    @ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
    @ExceptionHandler(ScopusIdMissingException.class)
    @ResponseBody
    ErrorObject handleScopusIdMissingException(HttpServletRequest request,
                                               ScopusIdMissingException ex) {
        return buildErrorObject(request, ex.getMessage(), HttpStatus.SERVICE_UNAVAILABLE);
    }

    @ResponseStatus(HttpStatus.FORBIDDEN)
    @ExceptionHandler(CantEditException.class)
    @ResponseBody
    ErrorObject handleCantEditException(HttpServletRequest request,
                                        CantEditException ex) {
        return buildErrorObject(request, ex.getMessage(), HttpStatus.FORBIDDEN);
    }

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(SchedulingException.class)
    @ResponseBody
    ErrorObject handleSchedulingException(HttpServletRequest request,
                                          SchedulingException ex) {
        return buildErrorObject(request, ex.getMessage(), HttpStatus.BAD_REQUEST);
    }

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(ValidationException.class)
    @ResponseBody
    ErrorObject handleValidationException(HttpServletRequest request,
                                          ValidationException ex) {
        return buildErrorObject(request, ex.getMessage(), HttpStatus.BAD_REQUEST);
    }

    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    @ExceptionHandler(InvalidApiKeyException.class)
    @ResponseBody
    ErrorObject handleInvalidApiKeyException(HttpServletRequest request,
                                             InvalidApiKeyException ex) {
        return buildErrorObject(request, ex.getMessage(), HttpStatus.UNAUTHORIZED);
    }

    @ResponseStatus(HttpStatus.CONFLICT)
    @ExceptionHandler(ThesisException.class)
    @ResponseBody
    ErrorObject handleThesisException(HttpServletRequest request,
                                      ThesisException ex) {
        return buildErrorObject(request, ex.getMessage(), HttpStatus.CONFLICT);
    }

    @ResponseStatus(HttpStatus.CONFLICT)
    @ExceptionHandler(PromotionException.class)
    @ResponseBody
    ErrorObject handlePromotionException(HttpServletRequest request,
                                         PromotionException ex) {
        return buildErrorObject(request, ex.getMessage(), HttpStatus.CONFLICT);
    }

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(RegistryBookException.class)
    @ResponseBody
    ErrorObject handleRegistryBookException(HttpServletRequest request,
                                            RegistryBookException ex) {
        return buildErrorObject(request, ex.getMessage(), HttpStatus.BAD_REQUEST);
    }

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(BackupException.class)
    @ResponseBody
    ErrorObject handleBackupException(HttpServletRequest request,
                                      BackupException ex) {
        return buildErrorObject(request, ex.getMessage(), HttpStatus.BAD_REQUEST);
    }

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(InvalidFileSectionException.class)
    @ResponseBody
    ErrorObject handleInvalidFileSectionException(HttpServletRequest request,
                                                  InvalidFileSectionException ex) {
        return buildErrorObject(request, ex.getMessage(), HttpStatus.BAD_REQUEST);
    }

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(CaptchaException.class)
    @ResponseBody
    ErrorObject handleCaptchaException(HttpServletRequest request,
                                       CaptchaException ex) {
        return buildErrorObject(request, ex.getMessage(), HttpStatus.BAD_REQUEST);
    }

    @ResponseStatus(HttpStatus.CONFLICT)
    @ExceptionHandler(ReferenceConstraintException.class)
    @ResponseBody
    ErrorObject handleReferenceConstraintException(HttpServletRequest request,
                                                   ReferenceConstraintException ex) {
        return buildErrorObject(request, ex.getMessage(), HttpStatus.CONFLICT);
    }

    @ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
    @ExceptionHandler(NetworkException.class)
    @ResponseBody
    ErrorObject handleNetworkException(HttpServletRequest request, NetworkException ex) {
        return buildErrorObject(request, ex.getMessage(), HttpStatus.SERVICE_UNAVAILABLE);
    }

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(BadCredentialsException.class)
    @ResponseBody
    ErrorObject handleBadCredentialsException(HttpServletRequest request,
                                              BadCredentialsException ex) {
        return buildErrorObject(request, ex.getMessage(), HttpStatus.BAD_REQUEST);
    }

    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    @ExceptionHandler(AccessDeniedException.class)
    @ResponseBody
    ErrorObject handleAccessDeniedException(HttpServletRequest request,
                                            AccessDeniedException ex) {
        return buildErrorObject(request, ex.getMessage(), HttpStatus.UNAUTHORIZED);
    }

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(DocumentHarvestException.class)
    @ResponseBody
    ErrorObject handleDocumentHarvestException(HttpServletRequest request,
                                               DocumentHarvestException ex) {
        return buildErrorObject(request, ex.getMessage(), HttpStatus.BAD_REQUEST);
    }

    @ResponseStatus(HttpStatus.FORBIDDEN)
    @ExceptionHandler(RegistrationException.class)
    @ResponseBody
    ErrorObject handleRegistrationException(HttpServletRequest request,
                                            RegistrationException ex) {
        return buildErrorObject(request, ex.getMessage(), HttpStatus.FORBIDDEN);
    }

    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    @ExceptionHandler(Exception.class)
    @ResponseBody
    public ErrorObject handleAllUncaughtExceptions(HttpServletRequest request, Exception ex) {
        var exceptionId = UUID.randomUUID().toString();
        log.error("Unhandled exception (ID:{}) occurred: {}, Request path: {}\nStack trace:",
            exceptionId,
            ex.getMessage(), request.getRequestURI(), ex);

        var tracingContextId = MDC.get(TraceMDCKeys.UNHANDLED_TRACING_CONTEXT_ID);
        emailUtil.sendUnhandledExceptionEmail(exceptionId, tracingContextId,
            request.getRequestURI(), ex);
        return buildErrorObject(request, ex.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
    }

    private ErrorObject buildErrorObject(HttpServletRequest request, String message,
                                         HttpStatus status) {
        try {
            return new ErrorObject(request, message, status);
        } finally {
            MDC.remove(TraceMDCKeys.UNHANDLED_TRACING_CONTEXT_ID);
        }
    }

    private ErrorObject buildErrorObject(HttpServletRequest request, String message,
                                         HttpStatus status, Map<String, String> errors) {
        try {
            return new ErrorObject(request, message, status, errors);
        } finally {
            MDC.remove(TraceMDCKeys.UNHANDLED_TRACING_CONTEXT_ID);
        }
    }
}
