/**
 * Copyright 2016-2017 the original author or authors.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.datagre.apps.omicron.client.util.http;

import com.datagre.apps.omicron.client.exceptions.OmicronConfigException;
import com.datagre.apps.omicron.client.exceptions.OmicronConfigStatusCodeException;
import com.datagre.apps.omicron.client.util.ConfigUtil;
import com.google.common.base.Charsets;
import com.google.common.base.Function;
import com.google.common.io.BaseEncoding;
import com.google.gson.Gson;
import org.unidal.helper.Files;
import org.unidal.lookup.annotation.Inject;
import org.unidal.lookup.annotation.Named;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Type;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Created by ycaihua on 2017/4/6.
 * https://github.com/ycaihua/omicron
 */
@Named(type = HttpUtil.class)
public class HttpUtil {
    @Inject
    private ConfigUtil m_configUtil;
    private Gson gson;
    private String basicAuth;

    /**
     * Constructor.
     */
    public HttpUtil() {
        gson = new Gson();
        try {
            basicAuth = "Basic " + BaseEncoding.base64().encode("user:".getBytes("UTF-8"));
        } catch (UnsupportedEncodingException ex) {
            ex.printStackTrace();
        }
    }

    /**
     * Do get operation for the http request.
     *
     * @param httpRequest  the request
     * @param responseType the response type
     * @return the response
     * @throws OmicronConfigException if any error happened or response code is neither 200 nor 304
     */
    public <T> HttpResponse<T> doGet(HttpRequest httpRequest, final Class<T> responseType) {
        Function<String, T> convertResponse = new Function<String, T>() {
            @Override
            public T apply(String input) {
                return gson.fromJson(input, responseType);
            }
        };

        return doGetWithSerializeFunction(httpRequest, convertResponse);
    }

    /**
     * Do get operation for the http request.
     *
     * @param httpRequest  the request
     * @param responseType the response type
     * @return the response
     * @throws OmicronConfigException if any error happened or response code is neither 200 nor 304
     */
    public <T> HttpResponse<T> doGet(HttpRequest httpRequest, final Type responseType) {
        Function<String, T> convertResponse = new Function<String, T>() {
            @Override
            public T apply(String input) {
                return gson.fromJson(input, responseType);
            }
        };

        return doGetWithSerializeFunction(httpRequest, convertResponse);
    }

    private <T> HttpResponse<T> doGetWithSerializeFunction(HttpRequest httpRequest,
                                                           Function<String, T> serializeFunction) {
        InputStream is = null;
        int statusCode;
        try {
            HttpURLConnection conn = (HttpURLConnection) new URL(httpRequest.getUrl()).openConnection();

            conn.setRequestMethod("GET");
            conn.setRequestProperty("Authorization", basicAuth);

            int connectTimeout = httpRequest.getConnectTimeout();
            if (connectTimeout < 0) {
                connectTimeout = m_configUtil.getConnectTimeout();
            }

            int readTimeout = httpRequest.getReadTimeout();
            if (readTimeout < 0) {
                readTimeout = m_configUtil.getReadTimeout();
            }

            conn.setConnectTimeout(connectTimeout);
            conn.setReadTimeout(readTimeout);

            conn.connect();

            statusCode = conn.getResponseCode();

            if (statusCode == 200) {
                is = conn.getInputStream();
                String content = Files.IO.INSTANCE.readFrom(is, Charsets.UTF_8.name());
                return new HttpResponse<>(statusCode, serializeFunction.apply(content));
            }

            if (statusCode == 304) {
                return new HttpResponse<>(statusCode, null);
            }

        } catch (Throwable ex) {
            throw new OmicronConfigException("Could not complete get operation", ex);
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (IOException ex) {
                    // ignore
                }
            }
        }

        throw new OmicronConfigStatusCodeException(statusCode,
                String.format("Get operation failed for %s", httpRequest.getUrl()));
    }
}
