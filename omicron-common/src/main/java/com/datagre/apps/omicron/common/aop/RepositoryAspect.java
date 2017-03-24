package com.datagre.apps.omicron.common.aop;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;

/**
 * Created by zengxiaobo on 2017/3/24.
 */
@Aspect
@Component
public class RepositoryAspect {
    @Pointcut("execution(public * org.springframework.data.repository.Repository+.*(..))")
    public void anyRepositoryMethod(){
    }
    @Around("anyRepositoryMethod()")
    public Object invokeWithCatTransaction(ProceedingJoinPoint joinPoint)throws Throwable{
       String name=joinPoint.getSignature().getDeclaringType().getSimpleName()+ "." + joinPoint.getSignature();
        try {
            Object result=joinPoint.proceed();
            return result;
        }catch (Throwable ex){
            throw ex;
        }finally {

        }
    }
}
