package edu.itc.se.shop.service;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class TimingAspect {
    private static final Logger log = LoggerFactory.getLogger(TimingAspect.class);

    @Around("execution(public * edu.itc.se.shop.service.*Service.*(..))")
    public Object time(ProceedingJoinPoint pjp) throws Throwable {
        long start = System.nanoTime();
        try {
            return pjp.proceed();
        } finally {
            if (log.isDebugEnabled()) {
                long micros = (System.nanoTime() - start) / 1_000;
                log.debug("{}.{}(..) took {} \u00b5s",
                        pjp.getSignature().getDeclaringType().getSimpleName(),
                        pjp.getSignature().getName(), micros);
            }
        }
    }
}
