package com.lxwise.net.tools;

/**
 * 命令常量类（跨平台版本）
 * 根据当前操作系统提供对应的系统命令
 *
 * @author lstar
 * @create 2022-03
 * @update 2025-04 扩展跨平台命令支持（Windows / Linux / macOS）
 */
public final class CmdConstants {

    private CmdConstants() {
        // 工具类禁止实例化
    }

    // ==================== 平台检测 ====================

    /** 当前系统名称（小写） */
    private static final String OS_NAME = System.getProperty("os.name", "").toLowerCase();

    /** 是否为 Windows 系统 */
    public static final boolean IS_WINDOWS = OS_NAME.contains("win");

    /** 是否为 macOS 系统 */
    public static final boolean IS_MAC = OS_NAME.contains("mac");

    /** 是否为 Linux 系统 */
    public static final boolean IS_LINUX = OS_NAME.contains("nix") || OS_NAME.contains("nux") || OS_NAME.contains("aix");

    // ==================== 端口相关命令 ====================

    /**
     * 列出所有 TCP/UDP 端口连接（含 PID）
     * Windows: netstat -ano
     * Linux/macOS: netstat -anp (需要 sudo) 或 ss -anp (Linux)
     */
    public static final String LIST_PORT = IS_WINDOWS ? "netstat -ano" : "netstat -anv";

    /**
     * Linux 下优先使用 ss 命令列出端口（更现代）
     */
    public static final String LIST_PORT_SS = "ss -antp";

    /**
     * macOS/Linux 使用 lsof 列出端口与进程的映射
     */
    public static final String LIST_PORT_LSOF = "lsof -i -P -n";

    // ==================== 进程相关命令 ====================

    /**
     * 终止指定 PID 的进程
     * Windows: taskkill /f /pid <pid>
     * Linux/macOS: kill -9 <pid>
     */
    public static final String KILL_PROCESS = IS_WINDOWS ? "taskkill /f /pid " : "kill -9 ";

    /**
     * 强制终止（与 KILL_PROCESS 相同，保持兼容）
     */
    public static final String KILL_PROCESS_FORCE = KILL_PROCESS;

    /**
     * 终止指定进程名的所有进程
     * Windows: taskkill /f /im <name>
     * Linux/macOS: pkill -9 -f <name>
     */
    public static final String KILL_BY_NAME = IS_WINDOWS ? "taskkill /f /im " : "pkill -9 -f ";

    /**
     * 列出所有进程（CSV 格式，仅 Windows）
     * Linux/macOS: ps aux
     */
    public static final String LIST_PROCESS = IS_WINDOWS ? "tasklist /fo csv /nh" : "ps aux";

    /**
     * 列出所有进程（表格格式）
     */
    public static final String LIST_PROCESS_TABLE = IS_WINDOWS ? "tasklist" : "ps aux";

    /**
     * 获取指定 PID 进程的详细信息
     * Windows: wmic process where processid=<pid> get Name,ProcessId,WorkingSetSize,CommandLine /format:csv
     * Linux/macOS: ps -p <pid> -o pid,comm,rss,args
     */
    public static final String PROCESS_DETAIL_WIN = "wmic process where processid=";

    // ==================== 网络诊断命令 ====================

    /**
     * Ping 命令（4次）
     * Windows: ping -n 4 <host>
     * Linux/macOS: ping -c 4 <host>
     */
    public static final String PING = IS_WINDOWS ? "ping -n 4 " : "ping -c 4 ";

    /**
     * 快速 Ping（1次）
     * Windows: ping -n 1 <host>
     * Linux/macOS: ping -c 1 <host>
     */
    public static final String PING_QUICK = IS_WINDOWS ? "ping -n 1 " : "ping -c 1 ";

    /**
     * 路由跟踪
     * Windows: tracert <host>
     * Linux/macOS: traceroute <host>
     */
    public static final String TRACERT = IS_WINDOWS ? "tracert " : "traceroute ";

    /**
     * 查看路由表
     * Windows: route print
     * Linux/macOS: netstat -rn
     */
    public static final String ROUTE_PRINT = IS_WINDOWS ? "route print" : "netstat -rn";

    /**
     * 查看网络配置
     * Windows: ipconfig /all
     * Linux/macOS: ifconfig -a 或 ip addr
     */
    public static final String IPCONFIG = IS_WINDOWS ? "ipconfig /all" : (IS_MAC ? "ifconfig -a" : "ip addr show");

    /**
     * 刷新 DNS 缓存
     * Windows: ipconfig /flushdns
     * macOS: dscacheutil -flushcache
     * Linux: resolvectl flush-caches 或 systemd-resolve --flush-caches
     */
    public static final String FLUSH_DNS = IS_WINDOWS ? "ipconfig /flushdns"
            : (IS_MAC ? "dscacheutil -flushcache" : "resolvectl flush-caches");

    /**
     * 查看网络统计信息
     */
    public static final String NETSTAT_STATS = "netstat -s";

    // ==================== 系统信息命令 ====================

    /**
     * 查看系统信息
     * Windows: systeminfo
     * Linux/macOS: uname -a
     */
    public static final String SYSTEM_INFO = IS_WINDOWS ? "systeminfo" : "uname -a";

    /**
     * 查看当前用户
     */
    public static final String WHOAMI = "whoami";

    /**
     * 查看主机名
     */
    public static final String HOSTNAME = "hostname";

    // ==================== 工具方法 ====================

    /**
     * 获取当前操作系统名称（用于日志显示）
     */
    public static String getOsName() {
        if (IS_WINDOWS) return "Windows";
        if (IS_MAC) return "macOS";
        if (IS_LINUX) return "Linux";
        return "Unknown(" + OS_NAME + ")";
    }
}
