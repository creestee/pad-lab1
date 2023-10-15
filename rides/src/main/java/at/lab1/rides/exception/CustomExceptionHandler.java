package at.lab1.rides.exception;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.async.AsyncRequestTimeoutException;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.DefaultHandlerExceptionResolver;

import java.io.IOException;

@Slf4j
@Component
@ControllerAdvice
public class CustomExceptionHandler extends DefaultHandlerExceptionResolver {

    @ExceptionHandler(value= AsyncRequestTimeoutException.class)
    @Override
    protected ModelAndView handleAsyncRequestTimeoutException(@NonNull
            AsyncRequestTimeoutException ex, @NonNull HttpServletRequest request, HttpServletResponse response, @Nullable Object handler)
            throws IOException {
        response.sendError(HttpServletResponse.SC_REQUEST_TIMEOUT);
        return new ModelAndView();
    }
}
