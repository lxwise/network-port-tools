package com.lxwise.net.tools;

import javafx.concurrent.Task;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * 端口操作服务接口
 * 提供端口查询、进程管理、端口扫描等功能
 *
 * @author lstar
 * @create 2022-03
 * @update 2025-04 新增异步操作和端口扫描功能
 */
public interface PortOperateService {

    /**
     * 列出所有活动的端口连接
     *
     * @return 端口信息列表
     */
    List<PortInfo> listPort();

    /**
     * 根据端口号获取端口信息
     *
     * @param port 端口号
     * @return 端口信息
     */
    PortInfo getByPort(int port);

    /**
     * 根据进程名获取端口信息列表
     *
     * @param processName 进程名
     * @return 端口信息列表
     */
    List<PortInfo> getByProcessName(String processName);

    /**
     * 终止指定PID的进程
     *
     * @param pid 进程ID
     * @return 操作结果（终止的进程数）
     */
    Integer kill(int pid);

    /**
     * 批量终止多个进程
     *
     * @param pids 进程ID列表
     * @return 成功终止的进程数
     */
    int killBatch(List<Integer> pids);

    /**
     * 异步列出所有端口
     *
     * @return CompletableFuture包装的端口列表
     */
    CompletableFuture<List<PortInfo>> listPortAsync();

    /**
     * 扫描指定端口范围
     *
     * @param startPort 起始端口
     * @param endPort   结束端口
     * @return 开放端口列表
     */
    List<Integer> scanPortRange(int startPort, int endPort);

    /**
     * 异步扫描端口范围
     *
     * @param startPort 起始端口
     * @param endPort   结束端口
     * @return Task对象，可用于进度跟踪
     */
    Task<List<Integer>> scanPortRangeAsync(int startPort, int endPort);

    /**
     * 检查本地端口是否被占用
     *
     * @param port 端口号
     * @return true表示被占用
     */
    boolean isPortOccupied(int port);

    /**
     * 获取指定PID的进程详细信息
     *
     * @param pid 进程ID
     * @return 进程详细信息
     */
    ProcessInfo getProcessInfo(int pid);

    /**
     * 获取系统所有进程列表
     *
     * @return 进程信息列表
     */
    List<ProcessInfo> listAllProcesses();

    /**
     * 搜索端口和进程
     *
     * @param keyword 关键字（支持端口号或进程名模糊匹配）
     * @return 匹配的端口信息列表
     */
    List<PortInfo> search(String keyword);

    /**
     * 进程信息内部类
     */
    class ProcessInfo {
        private int pid;
        private String name;
        private String memoryUsage;
        private String status;
        private String userName;
        private String commandLine;

        public ProcessInfo(int pid, String name) {
            this.pid = pid;
            this.name = name;
        }

        // Getters and Setters
        public int getPid() { return pid; }
        public void setPid(int pid) { this.pid = pid; }

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }

        public String getMemoryUsage() { return memoryUsage; }
        public void setMemoryUsage(String memoryUsage) { this.memoryUsage = memoryUsage; }

        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }

        public String getUserName() { return userName; }
        public void setUserName(String userName) { this.userName = userName; }

        public String getCommandLine() { return commandLine; }
        public void setCommandLine(String commandLine) { this.commandLine = commandLine; }

        @Override
        public String toString() {
            return String.format("ProcessInfo{pid=%d, name='%s', memory='%s', status='%s'}",
                    pid, name, memoryUsage, status);
        }
    }
}
