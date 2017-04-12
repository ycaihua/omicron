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
package com.datagre.apps.omicron.client.ds;

import com.datagre.apps.omicron.client.ConfigFile;
import com.datagre.apps.omicron.client.ConfigService;
import com.datagre.apps.omicron.core.enums.ConfigFileFormat;
import com.datagre.framework.foundation.Foundation;
import org.codehaus.plexus.logging.LogEnabled;
import org.codehaus.plexus.logging.Logger;
import org.unidal.dal.jdbc.datasource.DataSourceProvider;
import org.unidal.dal.jdbc.datasource.model.entity.DataSourcesDef;
import org.unidal.dal.jdbc.datasource.model.transform.DefaultSaxParser;
import org.unidal.lookup.annotation.Named;

/**
 * Created by ycaihua on 2017/4/7.
 * https://github.com/ycaihua/omicron
 */
@Named(type = DataSourceProvider.class,value = "omicron")
public class OmicronDataSourceProvider implements DataSourceProvider,LogEnabled{
    private Logger logger;
    private DataSourcesDef dataSourceDef;
    @Override
    public void enableLogging(Logger logger) {
        logger=logger;
    }

    @Override
    public DataSourcesDef defineDatasources() {
        if (dataSourceDef==null){
            ConfigFile file= ConfigService.getConfigFile("datasources", ConfigFileFormat.XML);
            String appId= Foundation.app().getAppId();
            String envType = Foundation.server().getEnvType();
            if (file != null && file.hasContent()) {
                String content = file.getContent();

                logger.info(String.format("Found datasources.xml from Apollo(env=%s, app.id=%s)!", envType, appId));

                try {
                    dataSourceDef = DefaultSaxParser.parse(content);
                } catch (Exception e) {
                    throw new IllegalStateException(String.format("Error when parsing datasources.xml from Apollo(env=%s, app.id=%s)!", envType, appId), e);
                }
            } else {
                logger.warn(String.format("Can't get datasources.xml from Apollo(env=%s, app.id=%s)!", envType, appId));
                dataSourceDef = new DataSourcesDef();
            }
        }
        return dataSourceDef;
    }
}
