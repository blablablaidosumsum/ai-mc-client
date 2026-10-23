package dev.client26.hud;

import java.lang.annotation.*;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface Setting {
    String value() default "";
    float min() default 0f;
    float max() default 1f;
    boolean color() default false;
}
