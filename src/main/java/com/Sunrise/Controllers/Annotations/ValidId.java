package com.Sunrise.Controllers.Annotations;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = {})
@NotNull(message = "ID cannot be null")
@Min(value = 1, message = "ID must be greater than 0")
public @interface ValidId {

    String message() default "Invalid ID format";

    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}