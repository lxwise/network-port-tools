package com.lxwise.net.utils;

import javafx.concurrent.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * 命令行工具类（跨平台版本）
 * 提供执行系统命令的功能，自动适配 Windows/Linux/macOS 的字符编码
 *
 * @author lstar
 * @create 2022-03
 * @update 2025-04 增强错误处理、日志记录和跨平台字符编码支持
 * @update 2025-06 新增异步执行、实时输出回调和进度反馈支持
 */
public final class CmdUtils {

    private static final Logger logger = LoggerFactory.getLogger(CmdUtils.class);
    private static final int DEFAULT_TIMEOUT_SECONDS = 30;

    /** 是否为 Windows 系统 */
    private static final boolean IS_WINDOWS = System.getProperty("os.name", "").toLowerCase().contains("win");

    /**
     * Windows 系统 OEM 编码（回退方案，用于部分命令不遵守 chcp 65001 的场景）
     */
    private static final Charset WIN_OEM_CHARSET = detectWindowsOemCharset();

    private static Charset detectWindowsOemCharset() {
        if (!IS_WINDOWS) {
            return StandardCharsets.UTF_8;
        }
        try {
            // 通过 chcp 获取当前活动代码页
            Process p = new ProcessBuilder("cmd", "/c", "chcp")
                    .redirectErrorStream(true).start();
            try (BufferedReader r = new BufferedReader(
                    new InputStreamReader(p.getInputStream(), StandardCharsets.US_ASCII))) {
                String line = r.readLine();
                if (line != null) {
                    // 输出格式: "Active code page: 936" 或 "活动代码页: 936"
                    String num = line.replaceAll("[^0-9]", "");
                    if (!num.isEmpty()) {
                        int cp = Integer.parseInt(num);
                        return Charset.forName("CP" + cp);
                    }
                }
            }
            p.waitFor(5, TimeUnit.SECONDS);
        } catch (Exception e) {
            logger.debug("检测 Windows OEM 代码页失败，回退到 GBK", e);
        }
        return Charset.forName("GBK");
    }

    /**
     * Windows 命令前缀：强制设置控制台代码页为 UTF-8 (65001)
     * 确保子进程输出为 UTF-8 编码
     */
    private static final String WIN_UTF8_PREFIX = "chcp 65001 >nul & ";

    private CmdUtils() {
        // 工具类禁止实例化
    }

    // ==================== 智能编码处理 ====================

    /**
     * 智能解码字节数据：先尝试 UTF-8 严格解码，失败则回退到 OEM 编码（GBK）。
     * 解决 Windows 上部分命令（如 ipconfig、cmd.exe 错误消息）不遵守 chcp 65001 的问题。
     *
     * @param bytes 原始字节数据
     * @return 正确解码后的字符串
     */
    private static String smartDecode(byte[] bytes) {
        if (!IS_WINDOWS) {
            return new String(bytes, StandardCharsets.UTF_8);
        }

        // 尝试 UTF-8 严格解码
        CharsetDecoder utf8Decoder = StandardCharsets.UTF_8.newDecoder()
                .onMalformedInput(CodingErrorAction.REPORT)
                .onUnmappableCharacter(CodingErrorAction.REPORT);
        try {
            CharBuffer decoded = utf8Decoder.decode(ByteBuffer.wrap(bytes));
            return decoded.toString();
        } catch (CharacterCodingException e) {
            // UTF-8 解码失败，说明输出不是 UTF-8，回退到 OEM 编码
            logger.debug("UTF-8 解码失败，回退到 OEM 编码: {}", WIN_OEM_CHARSET.name());
            return new String(bytes, WIN_OEM_CHARSET);
        }
    }

    /**
     * 从输入流读取全部字节并智能解码为行列表
     *
     * @param inputStream 进程输入流
     * @return 解码后的行列表（已去除空行）
     */
    private static List<String> readAndDecode(InputStream inputStream) throws IOException {
        byte[] rawBytes = inputStream.readAllBytes();
        String decoded = smartDecode(rawBytes);
        List<String> lines = new ArrayList<>();
        for (String line : decoded.split("\\r?\\n")) {
            String trimmed = line.trim();
            if (!trimmed.isEmpty()) {
                lines.add(trimmed);
            }
        }
        return lines;
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
            ProcessBuilder pb;
            if (IS_WINDOWS) {
                // Windows: 通过 chcp 65001 强制 UTF-8 输出
                pb = new ProcessBuilder("cmd", "/c", WIN_UTF8_PREFIX + cmd);
            } else {
                pb = new ProcessBuilder("sh", "-c", cmd);
            }
            pb.redirectErrorStream(false);
            process = pb.start();

            // 读取标准输出（智能编码检测）
            output = readAndDecode(process.getInputStream());

            // 读取错误输出
            List<String> errors = readAndDecode(process.getErrorStream());

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
            ProcessBuilder pb;
            if (IS_WINDOWS) {
                // Windows: 包装为 cmd /c chcp 65001 >nul & <command> 以确保 UTF-8 输出
                String joinedCmd = String.join(" ", commands);
                pb = new ProcessBuilder("cmd", "/c", WIN_UTF8_PREFIX + joinedCmd);
            } else {
                pb = new ProcessBuilder(commands);
            }
            pb.redirectErrorStream(true); // 合并错误输出到标准输出
            process = pb.start();

            // 智能编码检测读取
            output = readAndDecode(process.getInputStream());

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
            List<String> checkCmd;
            if (IS_WINDOWS) {
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

    // ==================== 异步执行支持 ====================

    /**
     * 异步执行命令，返回 JavaFX Task 对象
     * 支持实时输出、进度更新和取消操作
     *
     * @param cmd     命令字符串
     * @param timeout 超时时间（秒）
     * @return 可绑定到 UI 的 Task
     */
    public static Task<String> executeAsync(String cmd, int timeout) {
        return new Task<>() {
            private Process process;

            @Override
            protected String call() throws Exception {
                logger.debug("异步执行命令: {}", cmd);
                updateMessage("正在执行: " + cmd);
                StringBuilder output = new StringBuilder();

                try {
                    ProcessBuilder pb;
                    if (IS_WINDOWS) {
                        pb = new ProcessBuilder("cmd", "/c", WIN_UTF8_PREFIX + cmd);
                    } else {
                        pb = new ProcessBuilder("sh", "-c", cmd);
                    }
                    pb.redirectErrorStream(true);
                    process = pb.start();

                    // 读取原始字节后智能解码，避免部分命令不遵守 chcp 65001 导致乱码
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    InputStream is = process.getInputStream();
                    byte[] buffer = new byte[4096];
                    int bytesRead;
                    while ((bytesRead = is.read(buffer)) != -1) {
                        if (isCancelled()) {
                            process.destroyForcibly();
                            updateMessage("命令已取消");
                            return smartDecode(baos.toByteArray());
                        }
                        baos.write(buffer, 0, bytesRead);
                        updateMessage("正在接收数据...");
                    }

                    // 智能解码全部输出
                    String decoded = smartDecode(baos.toByteArray());
                    for (String line : decoded.split("\\r?\\n")) {
                        String trimmed = line.trim();
                        if (!trimmed.isEmpty()) {
                            output.append(trimmed).append("\n");
                        }
                    }

                    boolean finished = process.waitFor(timeout, TimeUnit.SECONDS);
                    if (!finished) {
                        process.destroyForcibly();
                        updateMessage("命令执行超时（" + timeout + "秒）");
                        output.append("\n[警告] 命令执行超时，已强制终止");
                    } else {
                        int exitCode = process.exitValue();
                        if (exitCode != 0) {
                            output.append("\n[退出码: ").append(exitCode).append("]");
                        }
                        updateMessage("执行完成");
                    }

                } catch (Exception e) {
                    if (!isCancelled()) {
                        logger.error("异步执行命令失败: {}", cmd, e);
                        output.append("\n[错误] ").append(e.getMessage());
                        updateMessage("执行失败: " + e.getMessage());
                    }
                }

                return output.toString();
            }

            @Override
            protected void cancelled() {
                super.cancelled();
                if (process != null && process.isAlive()) {
                    process.destroyForcibly();
                }
                updateMessage("命令已取消");
            }
        };
    }

    /**
     * 异步执行命令（默认超时）
     */
    public static Task<String> executeAsync(String cmd) {
        return executeAsync(cmd, DEFAULT_TIMEOUT_SECONDS);
    }

    /**
     * 执行命令并实时回调每行输出
     * 适用于需要逐行展示结果的场景
     *
     * @param cmd          命令字符串
     * @param lineConsumer 每行输出的回调
     * @param timeout      超时时间（秒）
     * @return 完整的输出列表
     */
    public static List<String> executeWithCallback(String cmd, Consumer<String> lineConsumer, int timeout) {
        logger.debug("执行命令(带回调): {}", cmd);
        List<String> output = new ArrayList<>();
        Process process = null;

        try {
            ProcessBuilder pb;
            if (IS_WINDOWS) {
                pb = new ProcessBuilder("cmd", "/c", WIN_UTF8_PREFIX + cmd);
            } else {
                pb = new ProcessBuilder("sh", "-c", cmd);
            }
            pb.redirectErrorStream(true);
            process = pb.start();

            // 读取原始字节并智能解码
            byte[] rawBytes = process.getInputStream().readAllBytes();
            String decoded = smartDecode(rawBytes);
            for (String line : decoded.split("\\r?\\n")) {
                String trimmed = line.trim();
                if (!trimmed.isEmpty()) {
                    output.add(trimmed);
                    if (lineConsumer != null) {
                        lineConsumer.accept(trimmed);
                    }
                }
            }

            boolean finished = process.waitFor(timeout, TimeUnit.SECONDS);
            if (!finished) {
                logger.warn("命令执行超时(带回调): {}", cmd);
                process.destroyForcibly();
            }

        } catch (Exception e) {
            logger.error("执行命令失败(带回调): {}", cmd, e);
        } finally {
            if (process != null && process.isAlive()) {
                process.destroyForcibly();
            }
        }

        return output;
    }

    /**
     * 获取命令行字符编码（用于外部组件参考）
     * 在 Windows 上返回实际 OEM 编码，其他平台返回 UTF-8
     */
    public static Charset getCmdCharset() {
        return IS_WINDOWS ? WIN_OEM_CHARSET : StandardCharsets.UTF_8;
    }
}
