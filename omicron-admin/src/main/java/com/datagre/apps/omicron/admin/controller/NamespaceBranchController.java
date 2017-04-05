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
package com.datagre.apps.omicron.admin.controller;

import com.datagre.apps.omicron.biz.entity.GrayReleaseRule;
import com.datagre.apps.omicron.biz.entity.Namespace;
import com.datagre.apps.omicron.biz.message.MessageSender;
import com.datagre.apps.omicron.biz.message.Topics;
import com.datagre.apps.omicron.biz.service.NamespaceBranchService;
import com.datagre.apps.omicron.biz.service.NamespaceService;
import com.datagre.apps.omicron.biz.utils.ReleaseMessageKeyGenerator;
import com.datagre.apps.omicron.common.constants.NamespaceBranchStatus;
import com.datagre.apps.omicron.common.dto.GrayReleaseRuleDTO;
import com.datagre.apps.omicron.common.dto.NamespaceDTO;
import com.datagre.apps.omicron.common.exception.BadRequestException;
import com.datagre.apps.omicron.common.utils.BeanUtils;
import com.datagre.apps.omicron.common.utils.GrayReleaseRuleItemTransformer;
import lombok.extern.apachecommons.CommonsLog;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * Created by ycaihua on 2017/4/5.
 * https://github.com/ycaihua/omicron
 */
@RestController
@CommonsLog
public class NamespaceBranchController {
    @Autowired
    private MessageSender messageSender;
    @Autowired
    private NamespaceBranchService namespaceBranchService;
    @Autowired
    private NamespaceService namespaceService;
    @RequestMapping(value = "/apps/{appId}/clusters/{clusterName}/namespaces/{namespaceName}/branches", method = RequestMethod.POST)
    public NamespaceDTO createBranch(@PathVariable String appId,
                                     @PathVariable String clusterName,
                                     @PathVariable String namespaceName,
                                     @RequestParam("operator") String operator) {
        checkNamespace(appId, clusterName, namespaceName);
        Namespace createdBranch=namespaceBranchService.createBranch(appId, clusterName, namespaceName, operator);
        return BeanUtils.transfrom(NamespaceDTO.class, createdBranch);
    }
    @RequestMapping(value = "/apps/{appId}/clusters/{clusterName}/namespaces/{namespaceName}/branches/{branchName}/rules",
            method = RequestMethod.GET)
    public GrayReleaseRuleDTO findBranchGrayRules(@PathVariable String appId,
                                                  @PathVariable String clusterName,
                                                  @PathVariable String namespaceName,
                                                  @PathVariable String branchName) {

        checkBranch(appId, clusterName, namespaceName, branchName);

        GrayReleaseRule rules = namespaceBranchService.findBranchGrayRules(appId, clusterName, namespaceName, branchName);
        if (rules == null) {
            return null;
        }
        GrayReleaseRuleDTO ruleDTO =
                new GrayReleaseRuleDTO(rules.getAppId(), rules.getClusterName(), rules.getNamespaceName(),
                        rules.getBranchName());

        ruleDTO.setReleaseId(rules.getReleaseId());

        ruleDTO.setRuleItems(GrayReleaseRuleItemTransformer.batchTransformFromJSON(rules.getRules()));

        return ruleDTO;
    }

    @RequestMapping(value = "/apps/{appId}/clusters/{clusterName}/namespaces/{namespaceName}/branches/{branchName}/rules", method = RequestMethod.PUT)
    public void updateBranchGrayRules(@PathVariable String appId, @PathVariable String clusterName,
                                      @PathVariable String namespaceName, @PathVariable String branchName,
                                      @RequestBody GrayReleaseRuleDTO newRuleDto) {

        checkBranch(appId, clusterName, namespaceName, branchName);

        GrayReleaseRule newRules = BeanUtils.transfrom(GrayReleaseRule.class, newRuleDto);
        newRules.setRules(GrayReleaseRuleItemTransformer.batchTransformToJSON(newRuleDto.getRuleItems()));
        newRules.setBranchStatus(NamespaceBranchStatus.ACTIVE);

        namespaceBranchService.updateBranchGrayRules(appId, clusterName, namespaceName, branchName, newRules);

        messageSender.sendMessage(ReleaseMessageKeyGenerator.generate(appId, clusterName, namespaceName),
                Topics.OMICRON_RELEASE_TOPIC);
    }

    @RequestMapping(value = "/apps/{appId}/clusters/{clusterName}/namespaces/{namespaceName}/branches/{branchName}", method = RequestMethod.DELETE)
    public void deleteBranch(@PathVariable String appId, @PathVariable String clusterName,
                             @PathVariable String namespaceName, @PathVariable String branchName,
                             @RequestParam("operator") String operator) {

        checkBranch(appId, clusterName, namespaceName, branchName);

        namespaceBranchService
                .deleteBranch(appId, clusterName, namespaceName, branchName, NamespaceBranchStatus.DELETED, operator);

        messageSender.sendMessage(ReleaseMessageKeyGenerator.generate(appId, clusterName, namespaceName),
                Topics.OMICRON_RELEASE_TOPIC);

    }

    @RequestMapping(value = "/apps/{appId}/clusters/{clusterName}/namespaces/{namespaceName}/branches", method = RequestMethod.GET)
    public NamespaceDTO loadNamespaceBranch(@PathVariable String appId, @PathVariable String clusterName,
                                            @PathVariable String namespaceName) {

        checkNamespace(appId, clusterName, namespaceName);

        Namespace childNamespace = namespaceBranchService.findBranch(appId, clusterName, namespaceName);
        if (childNamespace == null) {
            return null;
        }

        return BeanUtils.transfrom(NamespaceDTO.class, childNamespace);
    }

    private void checkBranch(String appId, String clusterName, String namespaceName, String branchName) {
        //1. check parent namespace
        checkNamespace(appId, clusterName, namespaceName);

        //2. check child namespace
        Namespace childNamespace = namespaceService.findOne(appId, branchName, namespaceName);
        if (childNamespace == null) {
            throw new BadRequestException(String.format("Namespace's branch not exist. AppId = %s, ClusterName = %s, "
                            + "NamespaceName = %s, BranchName = %s",
                    appId, clusterName, namespaceName, branchName));
        }

    }
    private void checkNamespace(String appId, String clusterName, String namespaceName) {
        Namespace parentNamespace = namespaceService.findOne(appId, clusterName, namespaceName);
        if (parentNamespace == null) {
            throw new BadRequestException(String.format("Namespace not exist. AppId = %s, ClusterName = %s, NamespaceName = %s", appId,
                    clusterName, namespaceName));
        }
    }
}
