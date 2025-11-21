package com.fastgpt.docparser.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * GitHub 图床配置属性
 */
@Component
@ConfigurationProperties(prefix = "github")
public class GitHubProperties {

    /**
     * GitHub Personal Access Token
     */
    private String token;

    /**
     * 仓库路径（格式：username/repo）
     */
    private String repo;

    /**
     * 分支名
     */
    private String branch = "main";

    /**
     * 图片存储路径前缀
     */
    private String pathPrefix = "images/";

    /**
     * CDN 域名
     */
    private String cdn = "cdn.jsdelivr.net";

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getRepo() {
        return repo;
    }

    public void setRepo(String repo) {
        this.repo = repo;
    }

    public String getBranch() {
        return branch;
    }

    public void setBranch(String branch) {
        this.branch = branch;
    }

    public String getPathPrefix() {
        return pathPrefix;
    }

    public void setPathPrefix(String pathPrefix) {
        this.pathPrefix = pathPrefix;
    }

    public String getCdn() {
        return cdn;
    }

    public void setCdn(String cdn) {
        this.cdn = cdn;
    }

    /**
     * 获取 CDN 基础 URL
     */
    public String getCdnBaseUrl() {
        return String.format("https://%s/gh/%s@%s/%s",
                cdn, repo, branch, pathPrefix);
    }

    /**
     * 获取 GitHub API 基础 URL
     */
    public String getApiBaseUrl() {
        return "https://api.github.com";
    }

    /**
     * 获取仓库用户名
     */
    public String getOwner() {
        if (repo != null && repo.contains("/")) {
            return repo.split("/")[0];
        }
        return null;
    }

    /**
     * 获取仓库名
     */
    public String getRepoName() {
        if (repo != null && repo.contains("/")) {
            return repo.split("/")[1];
        }
        return null;
    }
}
