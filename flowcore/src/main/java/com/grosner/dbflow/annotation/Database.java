package com.grosner.dbflow.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Author: andrewgrosner
 * Description: Creates a new database to use in the application.
 * <p>
 *     If we specify one DB, then all models do not need to specify a DB. As soon as we specify two, then each
 *     model needs to define what DB it points to.
 * </p>
 *
 * <p>
 *     Models will specify which DB it belongs to,
 *     but they currently can only belong to one DB.
 * </p>
 *
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.SOURCE)
public @interface Database {

    /**
     * @return The current version of the DB. Increment it to trigger a DB update.
     */
    int version();

    /**
     * @return The name of the DB. Optional as it will default to the class name.
     */
    String name() default "";

    /**
     * In order to use Foreign keys, set this to true.
     *
     * @return if key constraints are enabled.
     */
    boolean foreignKeysSupported() default false;

    /**
     * @return Checks for consistency in the DB, if true it will recopy over the prepackage database.
     */
    boolean consistencyCheckEnabled() default false;
}
