package com.diegohaefliger.atsresumeoptimizer;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import jakarta.validation.ReportAsSingleViolation;
import jakarta.validation.constraints.Pattern;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Documented
@Constraint(validatedBy = {})
@ReportAsSingleViolation
@Pattern(regexp = "^\\s*(https?://\\S+)?\\s*$")
@Target({ ElementType.METHOD, ElementType.FIELD, ElementType.ANNOTATION_TYPE, ElementType.CONSTRUCTOR, ElementType.PARAMETER,
		ElementType.TYPE_USE })
@Retention(RetentionPolicy.RUNTIME)
public @interface HttpUrl {

	String message() default "precisa ser um link http(s)";

	Class<?>[] groups() default {};

	Class<? extends Payload>[] payload() default {};
}
