package com.datagre.apps.omicron.common.exception;

import lombok.Data;
import org.springframework.http.HttpStatus;

/**
 * Created by zengxiaobo on 2017/3/24.
 */
@Data
public abstract class AbstractOmicronHttpException extends RuntimeException{
    private static final long serialVersionUID = -1713129594004951820L;
    protected HttpStatus httpStatus;
    public AbstractOmicronHttpException(String msg){
        super(msg);
    }
    public AbstractOmicronHttpException(String msg, Exception e){
        super(msg,e);
    }
}
