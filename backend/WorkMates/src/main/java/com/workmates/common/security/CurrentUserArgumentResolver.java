package com.workmates.common.security;

import com.workmates.common.BusinessException;
import com.workmates.common.ErrorCode;
import org.springframework.core.MethodParameter;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

/**
 * Passes the {@link CurrentUser} that {@link AuthInterceptor} stored to any controller method
 * that declares a {@code CurrentUser} parameter.
 */
@Component
public class CurrentUserArgumentResolver implements HandlerMethodArgumentResolver {

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return CurrentUser.class.equals(parameter.getParameterType());
    }

    /** @throws BusinessException UNAUTHORIZED if there is no user, which happens on a public endpoint */
    @Override
    public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
                                  NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {
        Object user = webRequest.getAttribute(AuthInterceptor.CURRENT_USER_ATTR, RequestAttributes.SCOPE_REQUEST);
        if (user == null) {
            // A public endpoint asked for CurrentUser, which is a coding mistake
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        return user;
    }
}
