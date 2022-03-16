package com.rundeck.plugins.azure.azure

import com.rundeck.plugins.azure.util.AzurePluginUtil

import java.util.*;
import java.util.regex.Matcher
import java.util.regex.Pattern

/**
 * Created by luistoledo on 11/9/17.
 */
class AzureNodeMapper {

    final String defaultMapping =   "nodename.selector=name\n" +
                                    "hostname.selector=hostname\n" +
                                    "description.default=Azure Instance \n" +
                                    "description.selector=short_description\n" +
                                    "osName.selector=osName\n" +
                                    "osVersion.selector=osVersion\n" +
                                    "osFamily.selector=osFamily\n" +
                                    "username.selector=username\n" +
                                    "region.selector=azure_region\n" +
                                    "resourceGroup.selector=azure_resourceGroup\n" +
                                    "status.selector=azure_status\n" +
                                    "id.selector=azure_id\n" +
                                    "node-executor.selector=node_executor\n" +
                                    "file-copier.selector=file_copier\n" +
                                    "vmId.selector=azure_vmId\n" +
                                    "size\\:name.selector=azure_size_name\n" +
                                    "size\\:numberOfCores.selector=azure_size_numberOfCores\n" +
                                    "size\\:memoryInMB.selector=azure_size_memoryInMB\n" +
                                    "size\\:maxDataDiskCount.selector=azure_size_maxDataDiskCount\n" +
                                    "size\\:azure_size_resourceDiskSizeInMB.selector=azure_size_azure_size_resourceDiskSizeInMB\n" +
                                    "image\\:type.selector=azure_image_type\n" +
                                    "image\\:offer.selector=azure_image_offer\n" +
                                    "image\\:sku.selector=azure_image_sku\n" +
                                    "image\\:version.selector=azure_image_version\n" +
                                    "osDisk\\:osType.selector=azure_osDisk_osType\n" +
                                    "osDisk\\:name.selector=azure_osDisk_name\n" +
                                    "osDisk\\:createOption.selector=azure_osDisk_createOption\n" +
                                    "osDisk\\:diskSizeGB.selector=azure_osDisk_diskSizeGB\n" +
                                    "netInterface\\:privateIp.selector=azure_netInterface_privateIp\n" +
                                    "provisioningState\\:code.selector=azure_provisioningState_code\n" +
                                    "provisioningState\\:displayStatus.selector=azure_provisioningState_displayStatus\n" +
                                    "provisioningState\\:message.selector=azure_provisioningState_message\n" +
                                    "provisioningState\\:time.selector=azure_provisioningState_time\n" +
                                    "plan\\:name.selector=azure_plan_name\n" +
                                    "plan\\:product.selector=azure_plan_product\n" +
                                    "plan\\:publisher.selector=azure_plan_publisher\n" +
                                    "tags.selector=tags\n" +
                                    "tags.default=azure\n"



    AzureNode node
    Properties mapping = new Properties();


    AzureNodeMapper(AzureNode node, String extraMapping) {
        this.node = node

        final StringReader stringReader = new StringReader(defaultMapping);
        try {
            mapping.load(stringReader);
        } finally {
            stringReader.close();
        }

        if (null != extraMapping) {
            extraMapping.split(";").findAll{it.contains("=")}.each { parameter ->
                final String[] split = parameter.split("=", 2);
                if (2 == split.length) {
                    mapping.put(split[0], split[1]);
                }
            }
        }
    }

    HashMap getAttributes(){

        HashMap nodeMap = new HashMap()
        //apply default values which do not have corresponding selector
        final Pattern attribDefPat = Pattern.compile("^([^.]+?)\\.default\$");

        //evaluate selectors
        mapping.keySet().findAll{!it.toString().contains("tags")}.each {key ->
            final String value = mapping.getProperty(key);
            final Matcher m = attribDefPat.matcher(key);
            if (m.matches() && (!mapping.containsKey(key + ".selector")
                    || "".equals(mapping.getProperty(key + ".selector")))) {
                final String attrName = m.group(1);
                if (null != value) {
                    nodeMap.put(attrName, value);
                }
            }
        }

        final Pattern attribPat = Pattern.compile("^([^.]+?)\\.selector\$");
        //evaluate selectors
        mapping.keySet().findAll{!it.toString().contains("tags")}.each {key ->
            final String selector = mapping.getProperty(key);

            final Matcher m = attribPat.matcher(key);
            if (m.matches()) {
                final String attrName = m.group(1);
                String value = applySelector(node,selector,mapping.getProperty(attrName+".default"));

                if (null != value) {
                    nodeMap.put(attrName, value);
                }
            }
        }

        return nodeMap
    }

    HashSet getTags(){

        HashSet tagSet = new HashSet()

        //add extra mapping tags
        if (null != mapping.getProperty("tags.selector")) {
            final String selector = mapping.getProperty("tags.selector")
            final String value = applyMultiSelector(node, selector, mapping.getProperty("tags.default"));

            if (null != value) {
                value.split(",").each { s->
                    tagSet.add(s.trim());
                }
            }
        }
        //add default tags
        if(node.getTags()!=null && !node.getTags().isEmpty()){
            node.getTags().split(",").each { tag->
                tagSet.add(tag.trim());
            }
        }

        if(node.getAzureTags()){
            node.getAzureTags().forEach{key, value ->
                tagSet.add(key +":"+value)
            }
        }

        tagSet.add("AzureVM")

        return tagSet
    }

    private String applySelector(AzureNode node, final String selector, final String defValue){
        if(AzurePluginUtil.isSet(selector)){
            for (final String select: selector.split(",")){
                String value = node.getField(select);
                if(AzurePluginUtil.isSet(value)){
                    return value;
                }
            }
        }

        return defValue;
    }

    private String applyMultiSelector(AzureNode node, final String selector, final String defValue){
        StringBuilder sb = new StringBuilder();
        if(AzurePluginUtil.isSet(selector)){
            selector.split(",").each { select ->
                String value = node.getField(select);

                if(AzurePluginUtil.isSet(value)){
                    if(sb.length()==0){
                        sb = new StringBuilder(value);
                    }else{
                        sb.append(',');
                        sb.append(value);
                    }
                }
            }
        }
        if(sb.length()==0){
            return defValue;
        }
        return sb.toString();
    }



}
