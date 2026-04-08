package com.lxwise.net.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 命令行工具类（跨平台版本）
 * 提供执行系统命令的功能，自动适配 Windows/Linux/macOS 的字符编码
 *
 * @author lstar
 * @create 2022-03
 * @update 2025-04 增强错误处理、日志记录和跨平台字符编码支持
 */
public final class CmdUtils {

    private static final Logger logger = LoggerFactory.getLogger(CmdUtils.class);
    private static final int DEFAULT_TIMEOUT_SECONDS = 30;

    /**
     * 根据当前操作系统自动选择字符编码
     * Windows 系统终端输出为 GBK，Linux/macOS 为 UTF-8
     */
    private static final Charset CMD_CHARSET = detectCmdCharset();

    private static Charset detectCmdCharset() {
        String osName = System.getProperty("os.name", "").toLowerCase();
        if (osName.contains("win")) {
            // Windows 命令行输出为系统默认编码（中文系统为 GBK/GB2312）
            try {
                // 尝试从系统属性获取控制台编码
                String encoding = System.getProperty("sun.stdout.encoding");
                if (encoding == null) {
                    encoding = System.getProperty("file.encoding", "GBK");
                }
                return Charset.forName(encoding);
            } catch (Exception e) {
                return Charset.forName("GBK");
            }
        }
        return StandardCharsets.UTF_8;
    }

    private CmdUtils() {
        // 工具类禁止实例化
    }

    /**
     * 执行命令并返回输出结果
     *
     * @param cmd 命令
     * @return 命令输出列表
     */
    public static List<String> execute(String cmd) {
        return execute(cmd, DEFAULT_TIMEOUT_SECONDS);
    }

    /**
     * 执行命令并返回输出结果（带超时）
     *
     * @param cmd     命令
     * @param timeout 超时时间（秒）
     * @return 命令输出列表
     */
    public static List<String> execute(String cmd, int timeout) {
        logger.debug("执行命令: {}", cmd);
        List<String> output = new ArrayList<>();
        Process process = null;

        try {
            process = Runtime.getRuntime().exec(cmd);

            // 读取标准输出（使用平台对应字符编码）
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream(), CMD_CHARSET))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    line = line.trim();
                    if (!line.isEmpty()) {
                        output.add(line);
                    }
                }
            }

            // 读取错误输出
            List<String> errors = new ArrayList<>();
            try (BufferedReader errorReader = new BufferedReader(
                    new InputStreamReader(process.getErrorStream(), CMD_CHARSET))) {
                String line;
                while ((line = errorReader.readLine()) != null) {
                    line = line.trim();
                    if (!line.isEmpty()) {
                        errors.add(line);
                    }
                }
            }

            // 等待进程完成
            boolean finished = process.waitFor(timeout, TimeUnit.SECONDS);
            if (!finished) {
                logger.warn("命令执行超时: {}", cmd);
                process.destroyForcibly();
                return output;
            }

            int exitCode = process.exitValue();
            if (exitCode != 0) {
                logger.warn("命令执行返回非零退出码: {}, exitCode: {}, errors: {}",
                        cmd, exitCode, errors);
            }

            if (!errors.isEmpty()) {
                logger.debug("命令错误输出: {}", errors);
            }

        } catch (Exception e) {
            logger.error("执行命令失败: {}", cmd, e);
        } finally {
            if (process != null && process.isAlive()) {
                process.destroyForcibly();
            }
        }

        return output;
    }

    /**
     * 执行命令并返回合并的输出字符串
     *
     * @param cmd 命令
     * @return 输出字符串
     */
    public static String executeAsString(String cmd) {
        List<String> lines = execute(cmd);
        return String.join("\n", lines);
    }

    /**
     * 执行命令并返回合并的输出字符串（带超时）
     *
     * @param cmd     命令
     * @param timeout 超时时间（秒）
     * @return 输出字符串
     */
    public static String executeAsString(String cmd, int timeout) {
        List<String> lines = execute(cmd, timeout);
        return String.join("\n", lines);
    }

    /**
     * 使用ProcessBuilder执行命令（更灵活）
     *
     * @param commands 命令及参数列表
     * @return 命令输出列表
     */
    public static List<String> executeWithBuilder(List<String> commands) {
        return executeWithBuilder(commands, DEFAULT_TIMEOUT_SECONDS);
    }

    /**
     * 使用ProcessBuilder执行命令（带超时）
     *
     * @param commands 命令及参数列表
     * @param timeout  超时时间（秒）
     * @return 命令输出列表
     */
    public static List<String> executeWithBuilder(List<String> commands, int timeout) {
        logger.debug("执行命令: {}", String.join(" ", commands));
        List<String> output = new ArrayList<>();
        Process process = null;

        try {
            ProcessBuilder pb = new ProcessBuilder(commands);
            pb.redirectErrorStream(true); // 合并错误输出到标准输出
            process = pb.start();

            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream(), CMD_CHARSET))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    line = line.trim();
                    if (!line.isEmpty()) {
                        output.add(line);
                    }
                }
            }

            boolean finished = process.waitFor(timeout, TimeUnit.SECONDS);
            if (!finished) {
                logger.warn("命令执行超时: {}", commands);
                process.destroyForcibly();
            }

        } catch (Exception e) {
            logger.error("执行命令失败: {}", commands, e);
        } finally {
            if (process != null && process.isAlive()) {
                process.destroyForcibly();
            }
        }

        return output;
    }

    /**
     * 检查命令是否可用（跨平台实现）
     * Windows: where <cmd>
     * Linux/macOS: which <cmd>
     *
     * @param cmd 命令
     * @return true 表示可用
     */
    public static boolean isCommandAvailable(String cmd) {
        try {
            String osName = System.getProperty("os.name", "").toLowerCase();
            List<String> checkCmd;
            if (osName.contains("win")) {
                checkCmd = Arrays.asList("where", cmd);
            } else {
                checkCmd = Arrays.asList("which", cmd);
            }
            List<String> result = executeWithBuilder(checkCmd, 5);
            return !result.isEmpty() && !result.get(0).toLowerCase().contains("not found");
        } catch (Exception e) {
            return false;
        }
    }
}
