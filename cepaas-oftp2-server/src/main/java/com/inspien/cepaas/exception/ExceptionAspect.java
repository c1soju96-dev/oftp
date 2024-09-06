package com.inspien.cepaas.exception;

import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Aspect
@Component
public class ExceptionAspect {
    private static final Logger logger = LoggerFactory.getLogger(ExceptionAspect.class);

    @AfterThrowing(pointcut = "execution(* com.inspien.cepaas.core..*(..))", throwing = "e")
    public void handleException(Throwable e) {
        if (e instanceof OftpException) {
            OftpException ae = (OftpException) e;
            logger.error("Error Code: {}, Message: {}", ae.getErrorCode().getCode(), ae.getErrorCode().getDetail());
        } else {
            logger.error("An unexpected error occurred: {}", e.getMessage());
        }
    }
    @AfterThrowing(pointcut = "execution(* com.inspien.cepaas.service..*(..))", throwing = "e")
    public void handleSchedulerException(Throwable e) {
        if (e instanceof OftpException) {
            OftpException oftpException = (OftpException) e;
            logger.error("OftpException occurred - Error Code: {}, Message: {}", 
                oftpException.getErrorCode().getCode(), oftpException.getMessage());
        } else {
            logger.error("An unexpected error occurred: {}", e.getMessage(), e);
        }
    }
}
