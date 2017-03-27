package com.datagre.apps.omicron.common.exception;

import org.springframework.http.HttpStatus;

/**
 * Created by zengxiaobo on 2017/3/24.
 */
public class BadRequestException extends AbstractOmicronHttpException {
    public BadRequestException(String msg) {
        super(msg);
        setHttpStatus(HttpStatus.BAD_REQUEST);
    }
}
