package ru.inobitec.order.filters;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import ru.inobitec.order.model.OrderSessionEntity;

import javax.servlet.*;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Duration;
import java.util.Date;

@WebFilter("/orderServlet")
@RequiredArgsConstructor
@Log4j2
public class CheckSessionFilter implements Filter {

    private static final String INVALID_SESSION = "Invalid session";
    private static final String UNKNOWN_SESSION = "Unknown session";
    private static final String SESSION_EXPIRED = "Session expired";
    private static final String SESSION_ID = "SESSION-ID";

    @Autowired
    private final SessionCache sessionCache;

    @Override
    public void init(FilterConfig filterConfig) {

    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        try {
            HttpServletResponse response1 = (HttpServletResponse) response;
            String sessionId = request.getParameter(SESSION_ID);
            if (sessionId == null) {
                response1.setStatus(HttpStatus.NOT_ACCEPTABLE.value());
                response1.getWriter().println(INVALID_SESSION);
            } else {
                OrderSessionEntity orderSessionEntity = sessionCache.getSessions().get(sessionId);
                if (orderSessionEntity == null) {
                    response1.setStatus(HttpStatus.FORBIDDEN.value());
                    response1.getWriter().println(UNKNOWN_SESSION);
                } else {
                    if (checkExpiring(orderSessionEntity)) {
                        chain.doFilter(request, response);
                    } else {
                        response1.setStatus(HttpStatus.REQUEST_TIMEOUT.value());
                        response1.getWriter().println(SESSION_EXPIRED);
                    }
                }
            }
        } catch (RuntimeException ex) {
            log.error(ex.getCause());
        }
    }

    private boolean checkExpiring(OrderSessionEntity orderSessionEntity) {
        return (orderSessionEntity.getStartTime().getTime()
                + (Duration.ofMinutes(orderSessionEntity.getTimeoutMinutes()).toMillis())) > new Date().getTime();
    }

    @Override
    public void destroy() {

    }
}
