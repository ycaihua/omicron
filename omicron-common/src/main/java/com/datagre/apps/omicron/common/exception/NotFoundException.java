package com.datagre.apps.omicron.common.exception;

import org.springframework.http.HttpStatus;

/**
 * Created by zengxiaobo on 2017/3/24.
 */
public class NotFoundException extends AbstractOmicronHttpException {
    public NotFoundException(String msg) {
        super(msg);
        setHttpStatus(HttpStatus.NOT_FOUND);
    }
}
