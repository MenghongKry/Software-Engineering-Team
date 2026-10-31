package edu.itc.se.shop.order;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class OrderAuditAspect {

    private static final Logger log =
            LoggerFactory.getLogger(OrderAuditAspect.class);

    @Around("@annotation(edu.itc.se.shop.order.Audited)")
    public Object audit(ProceedingJoinPoint joinPoint)
            throws Throwable {

        String requestId = MDC.get("requestId");
        long start = System.nanoTime();
        String result = "SUCCESS";

        try {
            return joinPoint.proceed();
        } catch (Throwable ex) {
            result = "FAILURE:" + ex.getClass().getSimpleName();
            throw ex;
        } finally {
            long durationMs =
                    (System.nanoTime() - start) / 1_000_000;

            log.info(
                "AUDIT requestId={} who={} method={} result={} durationMs={}",
                requestId == null ? "none" : requestId,
                "anonymous",
                joinPoint.getSignature().toShortString(),
                result,
                durationMs
            );
        }
    }
}