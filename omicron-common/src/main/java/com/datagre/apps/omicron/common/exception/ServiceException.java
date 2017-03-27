package com.datagre.apps.omicron.common.exception;

import org.springframework.http.HttpStatus;

/**
 * Created by zengxiaobo on 2017/3/24.
 */
public class ServiceException extends AbstractOmicronHttpException {
    public ServiceException(String msg) {
        super(msg);
        setHttpStatus(HttpStatus.INTERNAL_SERVER_ERROR);
    }

    public ServiceException(String msg, Exception e) {
        super(msg, e);
        setHttpStatus(HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
