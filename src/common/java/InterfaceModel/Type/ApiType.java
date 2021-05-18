package common.java.InterfaceModel.Type;

import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;


@Retention(RetentionPolicy.RUNTIME)
@Repeatable(ApiTypes.class)
public @interface ApiType {
    type value() default type.PublicApi;

    enum type {PublicApi, SessionApi, OauthApi, CloseApi, PrivateApi}
}