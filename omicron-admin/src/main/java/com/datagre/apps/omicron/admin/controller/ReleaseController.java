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

import com.datagre.apps.omicron.biz.entity.Namespace;
import com.datagre.apps.omicron.biz.entity.Release;
import com.datagre.apps.omicron.biz.message.MessageSender;
import com.datagre.apps.omicron.biz.message.Topics;
import com.datagre.apps.omicron.biz.service.NamespaceBranchService;
import com.datagre.apps.omicron.biz.service.NamespaceService;
import com.datagre.apps.omicron.biz.service.ReleaseService;
import com.datagre.apps.omicron.biz.utils.ReleaseMessageKeyGenerator;
import com.datagre.apps.omicron.common.constants.NamespaceBranchStatus;
import com.datagre.apps.omicron.common.dto.ItemChangeSets;
import com.datagre.apps.omicron.common.dto.ReleaseDTO;
import com.datagre.apps.omicron.common.exception.NotFoundException;
import com.datagre.apps.omicron.common.utils.BeanUtils;
import com.google.common.base.Splitter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Created by ycaihua on 2017/4/5.
 * https://github.com/ycaihua/omicron
 */
@RestController
public class ReleaseController {
    private static final Splitter RELEASES_SPLITTER=Splitter.on(",").omitEmptyStrings();
    @Autowired
    private ReleaseService releaseService;
    @Autowired
    private NamespaceService namespaceService;
    @Autowired
    private MessageSender messageSender;
    @Autowired
    private NamespaceBranchService namespaceBranchService;
    @RequestMapping(value = "/releases/{releaseId}",method = RequestMethod.GET)
    public ReleaseDTO get(@PathVariable("releaseId") long releaseId) {
        Release release = releaseService.findOne(releaseId);
        if (release == null) {
            throw new NotFoundException(String.format("release not found for %s", releaseId));
        }
        return BeanUtils.transfrom(ReleaseDTO.class, release);
    }
    @RequestMapping(value = "/releases", method = RequestMethod.GET)
    public List<ReleaseDTO> findReleaseByIds(@RequestParam("releaseIds") String releaseIds) {
        Set<Long> releaseIdSet=RELEASES_SPLITTER.splitToList(releaseIds).stream().map(Long::parseLong).collect(Collectors.toSet());
        List<Release> releases = releaseService.findByReleaseIds(releaseIdSet);

        return BeanUtils.batchTransform(ReleaseDTO.class, releases);
    }
    @RequestMapping(value = "/apps/{appId}/clusters/{clusterName}/namespaces/{namespaceName}/releases/all", method = RequestMethod.GET)
    public List<ReleaseDTO> findAllReleases(@PathVariable("appId") String appId,
                                            @PathVariable("clusterName") String clusterName,
                                            @PathVariable("namespaceName") String namespaceName,
                                            Pageable page) {
        List<Release> releases = releaseService.findAllReleases(appId, clusterName, namespaceName, page);
        return BeanUtils.batchTransform(ReleaseDTO.class, releases);
    }


    @RequestMapping(value = "/apps/{appId}/clusters/{clusterName}/namespaces/{namespaceName}/releases/active", method = RequestMethod.GET)
    public List<ReleaseDTO> findActiveReleases(@PathVariable("appId") String appId,
                                               @PathVariable("clusterName") String clusterName,
                                               @PathVariable("namespaceName") String namespaceName,
                                               Pageable page) {
        List<Release> releases = releaseService.findActiveReleases(appId, clusterName, namespaceName, page);
        return BeanUtils.batchTransform(ReleaseDTO.class, releases);
    }

    @RequestMapping(value = "/apps/{appId}/clusters/{clusterName}/namespaces/{namespaceName}/releases/latest", method = RequestMethod.GET)
    public ReleaseDTO getLatest(@PathVariable("appId") String appId,
                                @PathVariable("clusterName") String clusterName,
                                @PathVariable("namespaceName") String namespaceName) {
        Release release = releaseService.findLatestActiveRelease(appId, clusterName, namespaceName);
        return BeanUtils.transfrom(ReleaseDTO.class, release);
    }

    @RequestMapping(path = "/apps/{appId}/clusters/{clusterName}/namespaces/{namespaceName}/releases", method = RequestMethod.POST)
    public ReleaseDTO publish(@PathVariable("appId") String appId,
                              @PathVariable("clusterName") String clusterName,
                              @PathVariable("namespaceName") String namespaceName,
                              @RequestParam("name") String releaseName,
                              @RequestParam(name = "comment", required = false) String releaseComment,
                              @RequestParam("operator") String operator,
                              @RequestParam(name = "isEmergencyPublish", defaultValue = "false") boolean isEmergencyPublish) {
        Namespace namespace = namespaceService.findOne(appId, clusterName, namespaceName);
        if (namespace == null) {
            throw new NotFoundException(String.format("Could not find namespace for %s %s %s", appId,
                    clusterName, namespaceName));
        }
        Release release = releaseService.publish(namespace, releaseName, releaseComment, operator, isEmergencyPublish);

        //send release message
        Namespace parentNamespace = namespaceService.findParentNamespace(namespace);
        String messageCluster;
        if (parentNamespace != null) {
            messageCluster = parentNamespace.getClusterName();
        } else {
            messageCluster = clusterName;
        }
        messageSender.sendMessage(ReleaseMessageKeyGenerator.generate(appId, messageCluster, namespaceName),
                Topics.OMICRON_RELEASE_TOPIC);
        return BeanUtils.transfrom(ReleaseDTO.class, release);
    }


    /**
     * merge branch items to master and publish master
     *
     * @return published result
     */
    @Transactional
    @RequestMapping(path = "/apps/{appId}/clusters/{clusterName}/namespaces/{namespaceName}/updateAndPublish", method = RequestMethod.POST)
    public ReleaseDTO updateAndPublish(@PathVariable("appId") String appId,
                                       @PathVariable("clusterName") String clusterName,
                                       @PathVariable("namespaceName") String namespaceName,
                                       @RequestParam("releaseName") String releaseName,
                                       @RequestParam("branchName") String branchName,
                                       @RequestParam(value = "deleteBranch", defaultValue = "true") boolean deleteBranch,
                                       @RequestParam(name = "releaseComment", required = false) String releaseComment,
                                       @RequestParam(name = "isEmergencyPublish", defaultValue = "false") boolean isEmergencyPublish,
                                       @RequestBody ItemChangeSets changeSets) {
        Namespace namespace = namespaceService.findOne(appId, clusterName, namespaceName);
        if (namespace == null) {
            throw new NotFoundException(String.format("Could not find namespace for %s %s %s", appId,
                    clusterName, namespaceName));
        }

        Release release = releaseService.mergeBranchChangeSetsAndRelease(namespace, branchName, releaseName,
                releaseComment, isEmergencyPublish, changeSets);

        if (deleteBranch) {
            namespaceBranchService.deleteBranch(appId, clusterName, namespaceName, branchName,
                    NamespaceBranchStatus.MERGED, changeSets.getDataChangeLastModifiedBy());
        }

        messageSender.sendMessage(ReleaseMessageKeyGenerator.generate(appId, clusterName, namespaceName),
                Topics.OMICRON_RELEASE_TOPIC);

        return BeanUtils.transfrom(ReleaseDTO.class, release);

    }

    @RequestMapping(path = "/releases/{releaseId}/rollback", method = RequestMethod.PUT)
    public void rollback(@PathVariable("releaseId") long releaseId,
                         @RequestParam("operator") String operator) {

        Release release = releaseService.rollback(releaseId, operator);

        String appId = release.getAppId();
        String clusterName = release.getClusterName();
        String namespaceName = release.getNamespaceName();
        //send release message
        messageSender.sendMessage(ReleaseMessageKeyGenerator.generate(appId, clusterName, namespaceName),
                Topics.OMICRON_RELEASE_TOPIC);
    }
}
