package rs.teslaris.core.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import rs.teslaris.core.model.commontypes.ApiKeyType;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface ApiKeyValidation {

    ApiKeyType value() default ApiKeyType.M_SERVICE;
}
