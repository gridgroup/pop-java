package ch.icosys.popjava.core.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Make all POPSync* POPAsync* annotation also part of this annotation for
 * detection.
 * 
 * @author Davide Mazzoleni
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.ANNOTATION_TYPE)
public @interface POPSemantic {
}
