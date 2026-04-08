package com.lxwise.net.tools;

import com.lxwise.net.utils.CmdUtils;
import com.lxwise.net.utils.ListUtils;
import javafx.concurrent.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

/**
 * 端口操作服务实现类
 * 提供端口查询、进程管理、端口扫描等功能实现
 * 支持 Windows / Linux / macOS 三平台
 *
 * @author lstar
 * @create 2022-03
 * @update 2025-04 新增异步操作、端口扫描功能及跨平台支持
 */
public class PortOperateServiceImpl implements PortOperateService {

    private static final Logger logger = LoggerFactory.getLogger(PortOperateServiceImpl.class);
    private static final int SCAN_TIMEOUT = 200; // 端口扫描超时时间(ms)
    private static final int SCAN_THREADS = 50;  // 扫描线程数

    private final ExecutorService executor = Executors.newFixedThreadPool(SCAN_THREADS);

    @Override
    public List<PortInfo> listPort() {
        logger.debug("当前操作系统: {}", CmdConstants.getOsName());

        List<PortInfo> portInfos;
        if (CmdConstants.IS_WINDOWS) {
            portInfos = parseNetstatOutput(CmdConstants.LIST_PORT);
            Map<Integer, ProcessInfo> processMap = getProcessMap();
            if (!ListUtils.isEmpty(portInfos)) {
                for (PortInfo portInfo : portInfos) {
                    ProcessInfo processInfo = processMap.get(portInfo.getPid());
                    if (processInfo != null) {
                        portInfo.setProcessName(processInfo.getName());
                    }
                }
            }
        } else {
            // Linux/macOS: 使用 lsof 直接获取进程名和端口
            portInfos = parseUnixPortOutput();
        }

        // 去重
        return portInfos.stream()
                .distinct()
                .sorted(Comparator.comparingInt(PortInfo::getPort))
                .collect(Collectors.toList());
    }

    @Override
    public PortInfo getByPort(int port) {
        return listPort().stream()
                .filter(p -> p.getPort() == port)
                .findFirst()
                .orElse(null);
    }

    @Override
    public List<PortInfo> getByProcessName(String processName) {
        if (processName == null || processName.trim().isEmpty()) {
            return Collections.emptyList();
        }
        String lowerName = processName.toLowerCase();
        return listPort().stream()
                .filter(p -> p.getProcessName() != null &&
                        p.getProcessName().toLowerCase().contains(lowerName))
                .collect(Collectors.toList());
    }

    @Override
    public Integer kill(int pid) {
        try {
            logger.info("正在终止进程 PID: {}", pid);
            List<String> result;
            if (CmdConstants.IS_WINDOWS) {
                result = CmdUtils.execute(CmdConstants.KILL_PROCESS + pid);
                // Windows taskkill 成功时输出"成功: 进程..."，失败时才为空
                boolean success = result.stream().anyMatch(s ->
                        s.contains("SUCCESS") || s.contains("成功"));
                if (success || result.isEmpty()) {
                    logger.info("成功终止进程 PID: {}", pid);
                    return 1;
                } else {
                    logger.warn("终止进程 PID: {} 返回: {}", pid, result);
                    return 0;
                }
            } else {
                // Linux/macOS: kill -9 <pid>，成功时无输出，失败时有错误信息
                result = CmdUtils.executeWithBuilder(Arrays.asList("kill", "-9", String.valueOf(pid)));
                boolean failed = result.stream().anyMatch(s ->
                        s.contains("No such process") || s.contains("Operation not permitted"));
                if (failed) {
                    logger.warn("终止进程 PID: {} 返回: {}", pid, result);
                    return 0;
                }
                logger.info("成功终止进程 PID: {}", pid);
                return 1;
            }
        } catch (Exception e) {
            logger.error("终止进程 PID: {} 失败", pid, e);
            return 0;
        }
    }

    @Override
    public int killBatch(List<Integer> pids) {
        if (ListUtils.isEmpty(pids)) {
            return 0;
        }
        int successCount = 0;
        for (Integer pid : pids) {
            if (kill(pid) > 0) {
                successCount++;
            }
        }
        logger.info("批量终止进程完成，成功: {}/总数: {}", successCount, pids.size());
        return successCount;
    }

    @Override
    public CompletableFuture<List<PortInfo>> listPortAsync() {
        return CompletableFuture.supplyAsync(this::listPort, executor);
    }

    @Override
    public List<Integer> scanPortRange(int startPort, int endPort) {
        if (startPort < 1 || endPort > 65535 || startPort > endPort) {
            throw new IllegalArgumentException("端口范围无效: " + startPort + "-" + endPort);
        }

        List<Integer> openPorts = new ArrayList<>();
        logger.info("开始扫描端口范围: {}-{}", startPort, endPort);

        for (int port = startPort; port <= endPort; port++) {
            if (isPortOpen("localhost", port)) {
                openPorts.add(port);
                logger.debug("端口 {} 开放", port);
            }
        }

        logger.info("端口扫描完成，发现 {} 个开放端口", openPorts.size());
        return openPorts;
    }

    @Override
    public Task<List<Integer>> scanPortRangeAsync(int startPort, int endPort) {
        return new Task<>() {
            @Override
            protected List<Integer> call() throws Exception {
                if (startPort < 1 || endPort > 65535 || startPort > endPort) {
                    throw new IllegalArgumentException("端口范围无效: " + startPort + "-" + endPort);
                }

                List<Integer> openPorts = new ArrayList<>();
                int totalPorts = endPort - startPort + 1;
                int scanned = 0;

                updateMessage("准备扫描端口...");

                for (int port = startPort; port <= endPort; port++) {
                    if (isCancelled()) {
                        break;
                    }

                    if (isPortOpen("localhost", port)) {
                        openPorts.add(port);
                    }

                    scanned++;
                    updateProgress(scanned, totalPorts);
                    updateMessage(String.format("正在扫描端口 %d/%d...", scanned, totalPorts));
                }

                updateMessage(String.format("扫描完成，发现 %d 个开放端口", openPorts.size()));
                return openPorts;
            }
        };
    }

    @Override
    public boolean isPortOccupied(int port) {
        return getByPort(port) != null;
    }

    @Override
    public ProcessInfo getProcessInfo(int pid) {
        if (CmdConstants.IS_WINDOWS) {
            return getProcessMap().get(pid);
        } else {
            // Linux/macOS: ps -p <pid> -o pid=,comm=,rss=
            List<String> result = CmdUtils.executeWithBuilder(
                    Arrays.asList("ps", "-p", String.valueOf(pid), "-o", "pid=,comm=,rss="));
            if (result.isEmpty()) return null;
            String[] parts = result.get(0).trim().split("\\s+");
            if (parts.length < 2) return null;
            ProcessInfo info = new ProcessInfo(pid, parts[1]);
            if (parts.length >= 3) {
                // rss 单位是 KB
                try {
                    long rssKb = Long.parseLong(parts[2]);
                    info.setMemoryUsage(rssKb + " K");
                } catch (NumberFormatException ignored) {}
            }
            return info;
        }
    }

    @Override
    public List<ProcessInfo> listAllProcesses() {
        return new ArrayList<>(getProcessMap().values());
    }

    @Override
    public List<PortInfo> search(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return listPort();
        }

        String lowerKeyword = keyword.toLowerCase().trim();

        return listPort().stream()
                .filter(p -> {
                    // 按端口号搜索
                    if (String.valueOf(p.getPort()).contains(lowerKeyword)) {
                        return true;
                    }
                    // 按进程名搜索
                    if (p.getProcessName() != null &&
                            p.getProcessName().toLowerCase().contains(lowerKeyword)) {
                        return true;
                    }
                    // 按PID搜索
                    if (String.valueOf(p.getPid()).contains(lowerKeyword)) {
                        return true;
                    }
                    // 按协议搜索
                    if (p.getProtocol() != null &&
                            p.getProtocol().toLowerCase().contains(lowerKeyword)) {
                        return true;
                    }
                    return false;
                })
                .collect(Collectors.toList());
    }

    // ==================== 私有方法 ====================

    /**
     * Windows: 解析 netstat -ano 输出
     */
    private List<PortInfo> parseNetstatOutput(String cmd) {
        List<PortInfo> list = new ArrayList<>();
        List<String> lines = CmdUtils.execute(cmd);

        for (String line : lines) {
            String upper = line.trim().toUpperCase();
            if (upper.startsWith("TCP") || upper.startsWith("UDP")) {
                try {
                    PortInfo portInfo = parseWindowsNetstatLine(line);
                    if (portInfo != null) {
                        list.add(portInfo);
                    }
                } catch (Exception e) {
                    logger.warn("解析netstat行失败: {}", line, e);
                }
            }
        }
        return list;
    }

    /**
     * Windows netstat -ano 行解析
     * 格式: TCP   0.0.0.0:80   0.0.0.0:0   LISTENING   1234
     */
    private PortInfo parseWindowsNetstatLine(String line) {
        String[] parts = line.trim().split("\\s+");
        if (parts.length < 5) {
            return null;
        }

        String protocol = parts[0];
        String localAddress = parts[1];
        String foreignAddress = parts[2];
        String state = parts.length > 4 ? parts[3] : "";
        // UDP 行没有 state 字段，parts.length == 4
        int pidIndex = (protocol.toUpperCase().startsWith("UDP") && parts.length == 4) ? 3 : parts.length - 1;
        int pid;
        try {
            pid = Integer.parseInt(parts[pidIndex]);
        } catch (NumberFormatException e) {
            return null;
        }

        int port = extractPort(localAddress);
        if (port < 0) return null;

        PortInfo portInfo = new PortInfo();
        portInfo.setPort(port);
        portInfo.setPid(pid);
        portInfo.setProtocol(protocol);
        portInfo.setState(state);
        portInfo.setLocalAddress(localAddress);
        portInfo.setForeignAddress(foreignAddress);
        return portInfo;
    }

    /**
     * Linux/macOS: 使用 lsof -i -P -n 解析端口和进程信息
     * 格式: COMMAND  PID USER   FD   TYPE DEVICE SIZE/OFF NODE NAME
     *       nginx   1234 root   6u  IPv4  12345      0t0  TCP *:80 (LISTEN)
     */
    private List<PortInfo> parseUnixPortOutput() {
        List<PortInfo> list = new ArrayList<>();

        // 首先尝试 lsof（macOS 和大多数 Linux 都有）
        List<String> lines = CmdUtils.executeWithBuilder(Arrays.asList("lsof", "-i", "-P", "-n"));
        if (lines.isEmpty()) {
            // 回退到 netstat
            lines = CmdUtils.executeWithBuilder(Arrays.asList("netstat", "-anv"));
            return parseUnixNetstatLines(lines);
        }

        for (String line : lines) {
            // 跳过标题行
            if (line.startsWith("COMMAND") || line.trim().isEmpty()) continue;
            try {
                PortInfo portInfo = parseLsofLine(line);
                if (portInfo != null) {
                    list.add(portInfo);
                }
            } catch (Exception e) {
                logger.debug("解析lsof行跳过: {}", line);
            }
        }
        return list;
    }

    /**
     * 解析 lsof 输出行
     * COMMAND  PID USER FD TYPE DEVICE SIZE/OFF NODE NAME
     */
    private PortInfo parseLsofLine(String line) {
        String[] parts = line.trim().split("\\s+");
        if (parts.length < 9) return null;

        String command = parts[0];
        int pid;
        try {
            pid = Integer.parseInt(parts[1]);
        } catch (NumberFormatException e) {
            return null;
        }

        String type = parts[7]; // TCP 或 UDP
        if (!type.equalsIgnoreCase("TCP") && !type.equalsIgnoreCase("UDP")) return null;

        String name = parts[8]; // 例如 *:80 或 127.0.0.1:8080->192.168.1.1:12345
        // 解析本地地址和远程地址
        String localAddr, foreignAddr = "*:*";
        String state = "";
        if (name.contains("->")) {
            String[] addrs = name.split("->");
            localAddr = addrs[0];
            foreignAddr = addrs[1];
        } else {
            localAddr = name;
        }
        // 括号内是状态，如 (LISTEN)
        if (parts.length > 9) {
            state = parts[parts.length - 1].replace("(", "").replace(")", "");
        }

        int port = extractPort(localAddr);
        if (port < 0) return null;

        PortInfo portInfo = new PortInfo();
        portInfo.setPort(port);
        portInfo.setPid(pid);
        portInfo.setProcessName(command);
        portInfo.setProtocol(type.toUpperCase());
        portInfo.setState(state);
        portInfo.setLocalAddress(localAddr);
        portInfo.setForeignAddress(foreignAddr);
        return portInfo;
    }

    /**
     * 解析 Unix netstat 输出（备用方案）
     * 格式: tcp  0  0  0.0.0.0:80  0.0.0.0:*  LISTEN  -
     */
    private List<PortInfo> parseUnixNetstatLines(List<String> lines) {
        List<PortInfo> list = new ArrayList<>();
        for (String line : lines) {
            String upper = line.trim().toLowerCase();
            if (!upper.startsWith("tcp") && !upper.startsWith("udp")) continue;
            try {
                String[] parts = line.trim().split("\\s+");
                if (parts.length < 5) continue;
                String protocol = parts[0].toUpperCase().replaceAll("[0-9]", ""); // tcp6 -> TCP
                String localAddress = parts[3];
                String foreignAddress = parts[4];
                String state = parts.length > 5 ? parts[5] : "";

                int port = extractPort(localAddress);
                if (port < 0) continue;

                PortInfo portInfo = new PortInfo();
                portInfo.setPort(port);
                portInfo.setPid(0); // Unix netstat 不直接提供 PID（需 sudo）
                portInfo.setProtocol(protocol);
                portInfo.setState(state);
                portInfo.setLocalAddress(localAddress);
                portInfo.setForeignAddress(foreignAddress);
                list.add(portInfo);
            } catch (Exception e) {
                logger.debug("解析netstat行跳过: {}", line);
            }
        }
        return list;
    }

    private int extractPort(String address) {
        if (address == null || address.isEmpty() || address.equals("*")) return -1;
        try {
            // 处理 IPv6 格式 [::]:8080
            if (address.contains("]")) {
                int lastColon = address.lastIndexOf(':');
                return Integer.parseInt(address.substring(lastColon + 1));
            }
            // 处理 *:80 格式
            if (address.startsWith("*:")) {
                return Integer.parseInt(address.substring(2));
            }
            // 处理 IPv4 格式 0.0.0.0:8080
            String[] parts = address.split(":");
            return Integer.parseInt(parts[parts.length - 1]);
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    /**
     * Windows: 解析 tasklist /fo csv /nh 获取进程名映射
     * Linux/macOS: 从 lsof 输出中已包含进程名，此方法仅 Windows 使用
     */
    private Map<Integer, ProcessInfo> getProcessMap() {
        Map<Integer, ProcessInfo> map = new HashMap<>();
        if (!CmdConstants.IS_WINDOWS) {
            // Linux/macOS 通过 lsof 直接获取，无需单独查询
            return map;
        }

        List<String> tasklist = CmdUtils.execute("tasklist /fo csv /nh");
        for (String line : tasklist) {
            try {
                ProcessInfo info = parseTasklistLine(line);
                if (info != null) {
                    map.put(info.getPid(), info);
                }
            } catch (Exception e) {
                logger.warn("解析tasklist行失败: {}", line, e);
            }
        }
        return map;
    }

    private ProcessInfo parseTasklistLine(String line) {
        // 解析CSV格式: "进程名","PID","会话名","会话#","内存使用"
        String[] parts = line.split("\",\"");
        if (parts.length < 5) {
            return null;
        }

        String name = parts[0].replace("\"", "").trim();
        int pid = Integer.parseInt(parts[1].replace("\"", "").trim());
        String memory = parts[4].replace("\"", "").trim();

        ProcessInfo info = new ProcessInfo(pid, name);
        info.setMemoryUsage(memory);
        return info;
    }

    private boolean isPortOpen(String host, int port) {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(host, port), SCAN_TIMEOUT);
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    /**
     * 关闭执行器服务
     */
    public void shutdown() {
        executor.shutdown();
    }
}
