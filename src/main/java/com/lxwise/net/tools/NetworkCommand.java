package com.lxwise.net.tools;

/**
 * 网络快捷命令模型类
 * 封装预定义的网络命令，支持跨平台和参数化
 *
 * @author lstar
 * @create 2025-06
 */
public class NetworkCommand {

    /** 命令分类 */
    private final String category;

    /** 命令显示名称 */
    private final String displayName;

    /** 命令描述 */
    private final String description;

    /** 实际执行的命令模板（{param} 为参数占位符） */
    private final String commandTemplate;

    /** 参数提示（如果命令需要参数输入） */
    private final String paramHint;

    /** 是否需要用户输入参数 */
    private final boolean requiresParam;

    /** 预估执行超时时间（秒） */
    private final int timeoutSeconds;

    public NetworkCommand(String category, String displayName, String description,
                          String commandTemplate, String paramHint, boolean requiresParam, int timeoutSeconds) {
        this.category = category;
        this.displayName = displayName;
        this.description = description;
        this.commandTemplate = commandTemplate;
        this.paramHint = paramHint;
        this.requiresParam = requiresParam;
        this.timeoutSeconds = timeoutSeconds;
    }

    /**
     * 构建最终执行的命令字符串
     *
     * @param param 用户输入的参数（可为空）
     * @return 可执行的命令
     */
    public String buildCommand(String param) {
        if (requiresParam && (param == null || param.trim().isEmpty())) {
            throw new IllegalArgumentException("此命令需要参数: " + paramHint);
        }
        if (param != null && !param.trim().isEmpty()) {
            return commandTemplate.replace("{param}", param.trim());
        }
        return commandTemplate;
    }

    // ==================== Getters ====================

    public String getCategory() {
        return category;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }

    public String getCommandTemplate() {
        return commandTemplate;
    }

    public String getParamHint() {
        return paramHint;
    }

    public boolean isRequiresParam() {
        return requiresParam;
    }

    public int getTimeoutSeconds() {
        return timeoutSeconds;
    }

    @Override
    public String toString() {
        return displayName;
    }
}
