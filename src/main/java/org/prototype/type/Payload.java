package org.prototype.type;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;


public class Payload {
    String name;
    String repository;
    String namespace;
    @JsonProperty("docker_url")
    String dockerUrl;
    String homepage;
    @JsonProperty("updated_tags")
    List<String> updatedTags;
    String k8sNameSpace;


    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getRepository() {
        return repository;
    }

    public void setRepository(String repository) {
        this.repository = repository;
    }

    public String getNamespace() {
        return namespace;
    }

    public void setNamespace(String namespace) {
        this.namespace = namespace;
    }

    public String getDockerUrl() {
        return dockerUrl;
    }

    public void setDockerUrl(String dockerUrl) {
        this.dockerUrl = dockerUrl;
    }

    public String getHomepage() {
        return homepage;
    }

    public void setHomepage(String homepage) {
        this.homepage = homepage;
    }

    public List<String> getUpdatedTags() {
        return updatedTags;
    }

    public void setUpdatedTags(List<String> updatedTags) {
        this.updatedTags = updatedTags;
    }

    public boolean tagExists(String tag) {
        return updatedTags.contains(tag);
    }

    public String getK8sNameSpace() {
        return k8sNameSpace;
    }

    public void setK8sNameSpace(String k8sNameSpace) {
        this.k8sNameSpace = k8sNameSpace;
    }

    @Override
    public String toString() {
        return "{\"Payload\":{"
                + "\"name\":\"" + name + "\""
                + ",\"repository\":\"" + repository + "\""
                + ",\"namespace\":\"" + namespace + "\""
                + ",\"dockerUrl\":\"" + dockerUrl + "\""
                + ",\"homepage\":\"" + homepage + "\""
                + ",\"updatedTags\":" + updatedTags
                + "}}";
    }
}
