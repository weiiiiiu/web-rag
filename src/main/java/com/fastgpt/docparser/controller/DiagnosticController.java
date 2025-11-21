package com.fastgpt.docparser.controller;

import com.fastgpt.docparser.config.AliyunBailianProperties;
import com.fastgpt.docparser.dto.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * 诊断控制器 - 用于检查配置
 *
 * @author ZHONG WEI
 */
@RestController
@RequestMapping("/api/diagnostic")
public class DiagnosticController {

    private final AliyunBailianProperties properties;

    public DiagnosticController(AliyunBailianProperties properties) {
        this.properties = properties;
    }

    /**
     * 获取当前配置信息（脱敏）
     */
    @GetMapping("/config")
    public ApiResponse<Map<String, Object>> getConfig() {
        Map<String, Object> config = new HashMap<>();

        String accessKeyId = properties.getAccessKeyId();
        String accessKeySecret = properties.getAccessKeySecret();
        String workspaceId = properties.getWorkspaceId();

        // 脱敏显示
        config.put("endpoint", properties.getEndpoint());
        config.put("workspaceId", workspaceId);
        config.put("categoryId", properties.getCategoryId());
        config.put("appId", properties.getAppId());

        // AccessKey 脱敏
        if (accessKeyId != null && accessKeyId.length() > 8) {
            config.put("accessKeyId", accessKeyId.substring(0, 4) + "****" + accessKeyId.substring(accessKeyId.length() - 4));
        } else {
            config.put("accessKeyId", "未配置或格式错误");
        }

        config.put("accessKeySecret", accessKeySecret != null && accessKeySecret.length() > 4 ? "已配置（长度: " + accessKeySecret.length() + "）" : "未配置");

        // 配置检查
        Map<String, Boolean> checks = new HashMap<>();
        checks.put("accessKeyId已配置", accessKeyId != null && !accessKeyId.isEmpty() && !accessKeyId.contains("your-"));
        checks.put("accessKeySecret已配置", accessKeySecret != null && !accessKeySecret.isEmpty() && !accessKeySecret.contains("your-"));
        checks.put("workspaceId已配置", workspaceId != null && !workspaceId.isEmpty() && !workspaceId.contains("your-"));
        checks.put("workspaceId格式正确", workspaceId != null && workspaceId.startsWith("llm-"));

        config.put("checks", checks);

        // 提示信息
        StringBuilder tips = new StringBuilder();
        if (Boolean.FALSE.equals(checks.get("accessKeyId已配置"))) {
            tips.append("❌ AccessKey ID 未正确配置。请在 application.yml 中设置正确的值。\n");
        }
        if (Boolean.FALSE.equals(checks.get("accessKeySecret已配置"))) {
            tips.append("❌ AccessKey Secret 未正确配置。请在 application.yml 中设置正确的值。\n");
        }
        if (Boolean.FALSE.equals(checks.get("workspaceId已配置"))) {
            tips.append("❌ Workspace ID 未正确配置。请在 application.yml 中设置正确的值。\n");
        }
        if (Boolean.FALSE.equals(checks.get("workspaceId格式正确"))) {
            tips.append("⚠️ Workspace ID 格式可能不正确，应该以 'llm-' 开头。\n");
        }

        if (tips.length() == 0) {
            tips.append("✅ 所有配置项格式正确！如果仍有 403 错误，请检查：\n");
            tips.append("1. AccessKey 是否有效（未过期、未删除）\n");
            tips.append("2. Workspace 是否存在且可访问\n");
            tips.append("3. 在百炼控制台检查业务空间状态\n");
        }

        config.put("tips", tips.toString());

        return ApiResponse.success(config);
    }

    /**
     * 快速配置检查
     */
    @GetMapping("/check")
    public ApiResponse<String> quickCheck() {
        String accessKeyId = properties.getAccessKeyId();
        String accessKeySecret = properties.getAccessKeySecret();
        String workspaceId = properties.getWorkspaceId();

        // 检查配置
        if (accessKeyId == null || accessKeyId.isEmpty() || accessKeyId.contains("your-")) {
            return ApiResponse.error("AccessKey ID 未配置或使用了默认值，请修改 application.yml");
        }

        if (accessKeySecret == null || accessKeySecret.isEmpty() || accessKeySecret.contains("your-")) {
            return ApiResponse.error("AccessKey Secret 未配置或使用了默认值，请修改 application.yml");
        }

        if (workspaceId == null || workspaceId.isEmpty() || workspaceId.contains("your-")) {
            return ApiResponse.error("Workspace ID 未配置或使用了默认值，请修改 application.yml");
        }

        if (!workspaceId.startsWith("llm-")) {
            return ApiResponse.error("Workspace ID 格式可能不正确，应该以 'llm-' 开头，当前值: " + workspaceId);
        }

        return ApiResponse.success("配置检查通过！如果仍有 403 错误，请检查 AccessKey 权限和业务空间状态。", null);
    }
}
