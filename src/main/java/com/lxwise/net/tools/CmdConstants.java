package com.lxwise.net.tools;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 命令常量类（跨平台版本）
 * 根据当前操作系统提供对应的系统命令
 *
 * @author lstar
 * @create 2022-03
 * @update 2025-04 扩展跨平台命令支持（Windows / Linux / macOS）
 * @update 2025-06 新增快捷网络命令集合，支持分类管理
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

    // ==================== DNS 查询命令 ====================

    /**
     * DNS 查询（nslookup）
     * 所有平台均有 nslookup
     */
    public static final String NSLOOKUP = "nslookup ";

    /**
     * DNS 详细查询（dig）
     * Windows 无自带 dig，使用 PowerShell Resolve-DnsName 替代
     * Linux/macOS 自带 dig
     */
    public static final String DIG = IS_WINDOWS
            ? "powershell -Command \"Resolve-DnsName -Name {param} | Format-List\""
            : "dig ";

    /**
     * 主机名解析（host）
     * Linux/macOS 自带；Windows 无此命令
     */
    public static final String HOST = IS_WINDOWS ? "nslookup " : "host ";

    /**
     * 查看 DNS 配置
     * Windows: ipconfig /displaydns
     * macOS: scutil --dns
     * Linux: resolvectl status 或 cat /etc/resolv.conf
     */
    public static final String DNS_CONFIG = IS_WINDOWS ? "ipconfig /displaydns"
            : (IS_MAC ? "scutil --dns" : "resolvectl status");

    // ==================== 网络接口状态命令 ====================

    /**
     * 查看网络接口简要信息
     * Windows: netsh interface show interface
     * Linux: ip link show
     * macOS: ifconfig -l
     */
    public static final String INTERFACE_LIST = IS_WINDOWS ? "netsh interface show interface"
            : (IS_MAC ? "networksetup -listallhardwareports" : "ip link show");

    /**
     * 查看 ARP 缓存
     * Windows: arp -a
     * Linux/macOS: arp -a
     */
    public static final String ARP_TABLE = "arp -a";

    /**
     * 查看网络接口统计
     * Windows: netsh interface ip show config
     * Linux: ip -s link
     * macOS: netstat -ib
     */
    public static final String INTERFACE_STATS = IS_WINDOWS ? "netsh interface ip show config"
            : (IS_MAC ? "netstat -ib" : "ip -s link");

    // ==================== 防火墙状态命令 ====================

    /**
     * 查看防火墙状态
     * Windows: netsh advfirewall show allprofiles state
     * macOS: pfctl -s info（需要 sudo）
     * Linux: iptables -L -n（需要 sudo）或 ufw status
     */
    public static final String FIREWALL_STATUS = IS_WINDOWS ? "netsh advfirewall show allprofiles state"
            : (IS_MAC ? "pfctl -s info" : "ufw status");

    /**
     * 查看防火墙规则
     * Windows: netsh advfirewall firewall show rule name=all
     * macOS: pfctl -s rules
     * Linux: iptables -L -n --line-numbers
     */
    public static final String FIREWALL_RULES = IS_WINDOWS ? "netsh advfirewall firewall show rule name=all"
            : (IS_MAC ? "pfctl -s rules" : "iptables -L -n --line-numbers");

    // ==================== 网络统计和诊断命令 ====================

    /**
     * 查看 TCP 连接统计
     * Windows: netstat -s -p TCP
     * Linux/macOS: netstat -st
     */
    public static final String NETSTAT_TCP = IS_WINDOWS ? "netstat -s -p TCP" : "netstat -st";

    /**
     * 查看 UDP 连接统计
     * Windows: netstat -s -p UDP
     * Linux/macOS: netstat -su
     */
    public static final String NETSTAT_UDP = IS_WINDOWS ? "netstat -s -p UDP" : "netstat -su";

    /**
     * 查看所有监听端口
     * Windows: netstat -an | findstr LISTENING
     * Linux: ss -tlnp
     * macOS: netstat -an -p tcp
     */
    public static final String LISTENING_PORTS = IS_WINDOWS ? "netstat -ano -p TCP"
            : (IS_MAC ? "netstat -an -p tcp" : "ss -tlnp");

    /**
     * 查看路由表详细信息
     * Windows: route print
     * Linux: ip route show
     * macOS: netstat -rn
     */
    public static final String ROUTE_TABLE = IS_WINDOWS ? "route print"
            : (IS_MAC ? "netstat -rn" : "ip route show");

    /**
     * 查看网络连接的实时状态（快照）
     * Windows: netstat -b（需管理员）
     * Linux: ss -tp
     * macOS: lsof -i
     */
    public static final String NET_CONNECTIONS = IS_WINDOWS ? "netstat -b"
            : (IS_MAC ? "lsof -i" : "ss -tp");

    // ==================== 高级网络工具命令 ====================

    /**
     * curl 测试 URL 连通性
     */
    public static final String CURL_TEST = "curl -I -o /dev/null -s -w \"%{http_code}\" ";

    /**
     * 查看本机公网 IP
     */
    public static final String PUBLIC_IP = IS_WINDOWS
            ? "curl -s https://ifconfig.me"
            : "curl -s https://ifconfig.me";

    /**
     * 端口连通性测试（telnet 替代）
     * Windows: powershell Test-NetConnection -Port
     * Linux/macOS: nc -zv
     */
    public static final String PORT_TEST = IS_WINDOWS
            ? "powershell -Command \"Test-NetConnection -ComputerName {host} -Port {port}\""
            : "nc -zv ";

    /**
     * MTR 路由跟踪（比 traceroute 更详细）
     * Windows: pathping
     * Linux: mtr --report
     * macOS: mtr --report
     */
    public static final String MTR = IS_WINDOWS ? "pathping " : "mtr --report ";

    /**
     * 查看 Wi-Fi 信息
     * Windows: netsh wlan show interfaces
     * macOS: airport -I (需要完整路径)
     * Linux: iwconfig 或 nmcli dev wifi
     */
    public static final String WIFI_INFO = IS_WINDOWS ? "netsh wlan show interfaces"
            : (IS_MAC ? "/System/Library/PrivateFrameworks/Apple80211.framework/Versions/Current/Resources/airport -I"
            : "nmcli dev wifi");

    /**
     * 查看 TCP 连接状态分布
     * Windows: netstat -ano
     * Linux: ss -s
     * macOS: netstat -s -p tcp
     */
    public static final String TCP_STATE_SUMMARY = IS_WINDOWS ? "netstat -ano"
            : (IS_MAC ? "netstat -s -p tcp" : "ss -s");

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

    // ==================== 快捷命令分类常量 ====================

    public static final String CAT_CONNECTIVITY = "网络连通性测试";
    public static final String CAT_DNS = "DNS 查询";
    public static final String CAT_INTERFACE = "网络接口状态";
    public static final String CAT_FIREWALL = "防火墙";
    public static final String CAT_STATISTICS = "网络统计与诊断";
    public static final String CAT_ADVANCED = "高级网络工具";
    public static final String CAT_SYSTEM = "系统信息";

    /**
     * 获取所有命令分类列表（保持顺序）
     */
    public static List<String> getCategories() {
        List<String> categories = new ArrayList<>();
        categories.add(CAT_CONNECTIVITY);
        categories.add(CAT_DNS);
        categories.add(CAT_INTERFACE);
        categories.add(CAT_FIREWALL);
        categories.add(CAT_STATISTICS);
        categories.add(CAT_ADVANCED);
        categories.add(CAT_SYSTEM);
        return categories;
    }

    /**
     * 获取所有预定义的快捷网络命令
     * 按分类组织，便于 UI 展示
     *
     * @return 分类名 -> 命令列表 的有序映射
     */
    public static Map<String, List<NetworkCommand>> getQuickCommands() {
        Map<String, List<NetworkCommand>> commandMap = new LinkedHashMap<>();

        // ==================== 网络连通性测试 ====================
        List<NetworkCommand> connectivity = new ArrayList<>();
        connectivity.add(new NetworkCommand(CAT_CONNECTIVITY, "Ping 测试（4次）",
                "向目标主机发送4个ICMP回显请求，检测网络连通性", PING + "{param}",
                "输入目标主机（如: google.com 或 192.168.1.1）", true, 30));
        connectivity.add(new NetworkCommand(CAT_CONNECTIVITY, "Ping 快速测试（1次）",
                "向目标主机发送1个ICMP请求快速检测", PING_QUICK + "{param}",
                "输入目标主机", true, 10));
        connectivity.add(new NetworkCommand(CAT_CONNECTIVITY, "路由跟踪 (traceroute)",
                "跟踪到目标主机的网络路由路径", TRACERT + "{param}",
                "输入目标主机", true, 60));
        connectivity.add(new NetworkCommand(CAT_CONNECTIVITY, "路径质量检测 (MTR/pathping)",
                "比traceroute更详细的路径质量诊断", MTR + "{param}",
                "输入目标主机", true, 120));
        connectivity.add(new NetworkCommand(CAT_CONNECTIVITY, "端口连通性测试",
                "测试目标主机指定端口是否可达",
                IS_WINDOWS ? "powershell -Command \"Test-NetConnection -ComputerName {param}\"" : "nc -zv {param}",
                "输入 主机 端口（如: google.com 443）", true, 15));
        commandMap.put(CAT_CONNECTIVITY, connectivity);

        // ==================== DNS 查询 ====================
        List<NetworkCommand> dns = new ArrayList<>();
        dns.add(new NetworkCommand(CAT_DNS, "DNS 查询 (nslookup)",
                "查询域名的DNS记录", NSLOOKUP + "{param}",
                "输入域名（如: google.com）", true, 15));
        dns.add(new NetworkCommand(CAT_DNS, "DNS 详细查询 (dig/Resolve-DnsName)",
                "使用dig(Linux/macOS)或Resolve-DnsName(Windows)进行详细DNS查询",
                IS_WINDOWS ? "powershell -Command \"Resolve-DnsName -Name {param} | Format-List\"" : "dig {param}",
                "输入域名（如: baidu.com）", true, 15));
        dns.add(new NetworkCommand(CAT_DNS, "主机名解析 (host/nslookup)",
                "解析域名到IP地址", HOST + "{param}",
                "输入域名", true, 10));
        dns.add(new NetworkCommand(CAT_DNS, "查看 DNS 配置",
                "显示当前系统DNS配置信息", DNS_CONFIG,
                "", false, 10));
        dns.add(new NetworkCommand(CAT_DNS, "刷新 DNS 缓存",
                "清除本地DNS缓存", FLUSH_DNS,
                "", false, 10));
        commandMap.put(CAT_DNS, dns);

        // ==================== 网络接口状态 ====================
        List<NetworkCommand> netInterface = new ArrayList<>();
        netInterface.add(new NetworkCommand(CAT_INTERFACE, "查看网络配置 (ipconfig/ifconfig)",
                "显示所有网络接口的详细配置信息", IPCONFIG,
                "", false, 10));
        netInterface.add(new NetworkCommand(CAT_INTERFACE, "网络接口列表",
                "列出所有网络接口及其状态", INTERFACE_LIST,
                "", false, 10));
        netInterface.add(new NetworkCommand(CAT_INTERFACE, "网络接口统计",
                "显示各网络接口的流量统计数据", INTERFACE_STATS,
                "", false, 10));
        netInterface.add(new NetworkCommand(CAT_INTERFACE, "ARP 缓存表",
                "显示ARP地址解析缓存", ARP_TABLE,
                "", false, 10));
        netInterface.add(new NetworkCommand(CAT_INTERFACE, "路由表",
                "显示系统路由表", ROUTE_TABLE,
                "", false, 10));
        netInterface.add(new NetworkCommand(CAT_INTERFACE, "Wi-Fi 信息",
                "显示当前Wi-Fi连接信息", WIFI_INFO,
                "", false, 10));
        commandMap.put(CAT_INTERFACE, netInterface);

        // ==================== 防火墙 ====================
        List<NetworkCommand> firewall = new ArrayList<>();
        firewall.add(new NetworkCommand(CAT_FIREWALL, "防火墙状态",
                "查看防火墙当前启用状态", FIREWALL_STATUS,
                "", false, 10));
        firewall.add(new NetworkCommand(CAT_FIREWALL, "防火墙规则列表",
                "查看所有防火墙规则（可能需要管理员权限）", FIREWALL_RULES,
                "", false, 30));
        commandMap.put(CAT_FIREWALL, firewall);

        // ==================== 网络统计与诊断 ====================
        List<NetworkCommand> statistics = new ArrayList<>();
        statistics.add(new NetworkCommand(CAT_STATISTICS, "网络统计概览",
                "显示所有协议的网络统计信息", NETSTAT_STATS,
                "", false, 15));
        statistics.add(new NetworkCommand(CAT_STATISTICS, "TCP 协议统计",
                "显示TCP协议的统计数据", NETSTAT_TCP,
                "", false, 15));
        statistics.add(new NetworkCommand(CAT_STATISTICS, "UDP 协议统计",
                "显示UDP协议的统计数据", NETSTAT_UDP,
                "", false, 15));
        statistics.add(new NetworkCommand(CAT_STATISTICS, "所有监听端口",
                "显示当前所有处于监听状态的端口", LISTENING_PORTS,
                "", false, 15));
        statistics.add(new NetworkCommand(CAT_STATISTICS, "活动网络连接",
                "显示当前所有活动的网络连接", NET_CONNECTIONS,
                "", false, 15));
        statistics.add(new NetworkCommand(CAT_STATISTICS, "TCP 状态分布",
                "显示当前TCP连接各状态的汇总", TCP_STATE_SUMMARY,
                "", false, 15));
        commandMap.put(CAT_STATISTICS, statistics);

        // ==================== 高级网络工具 ====================
        List<NetworkCommand> advanced = new ArrayList<>();
        advanced.add(new NetworkCommand(CAT_ADVANCED, "HTTP 状态检测 (curl)",
                "使用curl检测URL的HTTP响应状态码", CURL_TEST + "{param}",
                "输入完整URL（如: https://www.google.com）", true, 15));
        advanced.add(new NetworkCommand(CAT_ADVANCED, "查看公网 IP",
                "通过在线服务获取本机公网IP地址", PUBLIC_IP,
                "", false, 15));
        advanced.add(new NetworkCommand(CAT_ADVANCED, "自定义命令",
                "执行自定义网络命令", "{param}",
                "输入完整命令（如: ping -n 2 google.com）", true, 60));
        commandMap.put(CAT_ADVANCED, advanced);

        // ==================== 系统信息 ====================
        List<NetworkCommand> system = new ArrayList<>();
        system.add(new NetworkCommand(CAT_SYSTEM, "系统信息",
                "显示操作系统详细信息", SYSTEM_INFO,
                "", false, 30));
        system.add(new NetworkCommand(CAT_SYSTEM, "当前用户",
                "显示当前登录用户名", WHOAMI,
                "", false, 5));
        system.add(new NetworkCommand(CAT_SYSTEM, "主机名",
                "显示计算机主机名", HOSTNAME,
                "", false, 5));
        commandMap.put(CAT_SYSTEM, system);

        return commandMap;
    }

    /**
     * 获取所有快捷命令的扁平列表
     */
    public static List<NetworkCommand> getAllQuickCommands() {
        List<NetworkCommand> all = new ArrayList<>();
        getQuickCommands().values().forEach(all::addAll);
        return all;
    }
}
