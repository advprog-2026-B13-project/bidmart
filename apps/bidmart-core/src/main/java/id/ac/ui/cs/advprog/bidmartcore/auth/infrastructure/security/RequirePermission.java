package id.ac.ui.cs.advprog.bidmartcore.auth.infrastructure.security;

import id.ac.ui.cs.advprog.bidmartcore.auth.domain.model.enums.PermissionValue;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface RequirePermission {
    PermissionValue[] value();
}

