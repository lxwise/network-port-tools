package com.lxwise.net.ui;

import atlantafx.base.theme.Styles;
import com.lxwise.net.tools.PortInfo;
import com.lxwise.net.tools.PortOperateService;
import com.lxwise.net.tools.PortOperateServiceImpl;
import com.lxwise.net.utils.DialogUtils;
import javafx.application.Platform;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.CheckBoxTableCell;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.input.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.stage.Window;
import javafx.util.Duration;
import org.kordamp.ikonli.javafx.FontIcon;
import org.kordamp.ikonli.material2.Material2OutlinedAL;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 端口控制器
 * 管理端口列表展示、搜索、终止进程等功能
 *
 * @author lstar
 * @create 2022-03
 * @update 2025-04 全面升级，新增批量操作、端口扫描等功能
 */
public class PortController implements Initializable {

    private static final Logger logger = LoggerFactory.getLogger(PortController.class);

    // 服务层
    private final PortOperateService portService = new PortOperateServiceImpl();

    // 数据
    private ObservableList<PortInfo> allPortData = FXCollections.observableArrayList();
    private ObservableList<PortInfo> filteredPortData = FXCollections.observableArrayList();
    private final Set<PortInfo> selectedPortInfos = new HashSet<>();

    // 筛选状态
    private String currentProtocolFilter = "全部";
    private String currentStateFilter = "全部";
    private Integer currentMinPort = null;
    private Integer currentMaxPort = null;

    // ==================== FXML组件 ====================

    @FXML
    private TextField searchField;
    @FXML
    private Button refreshBtn;
    @FXML
    private Button clearSearchBtn;
    @FXML
    private TextField startPortField;
    @FXML
    private TextField endPortField;
    @FXML
    private Button scanBtn;
    @FXML
    private Button killSelectedBtn;
    @FXML
    private Button copySelectedBtn;
    @FXML
    private Button exportBtn;
    @FXML
    private Button checkPortBtn;
    @FXML
    private Button advancedFilterBtn;
    @FXML
    private ProgressBar scanProgressBar;
    @FXML
    private Label statusLabel;
    @FXML
    private Label countLabel;
    @FXML
    private Label selectedLabel;
    @FXML
    private ComboBox<String> protocolFilterCombo;
    @FXML
    private ComboBox<String> stateFilterCombo;

    @FXML
    private TableView<PortInfo> portTable;
    @FXML
    private TableColumn<PortInfo, Boolean> selectColumn;
    @FXML
    private TableColumn<PortInfo, String> processNameColumn;
    @FXML
    private TableColumn<PortInfo, Integer> pidColumn;
    @FXML
    private TableColumn<PortInfo, Integer> portColumn;
    @FXML
    private TableColumn<PortInfo, String> protocolColumn;
    @FXML
    private TableColumn<PortInfo, String> stateColumn;
    @FXML
    private TableColumn<PortInfo, String> localAddressColumn;
    @FXML
    private TableColumn<PortInfo, Void> actionColumn;

    // ==================== 初始化 ====================

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        logger.info("初始化端口控制器...");
        initializeTable();
        setupListeners();
        loadPortData();
    }

    private void initializeTable() {
        // 启用多行选择模式
        portTable.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);

        // 选择列 - 复选框（修复多行选中功能）
        selectColumn.setCellFactory(CheckBoxTableCell.forTableColumn(selectColumn));
        selectColumn.setCellValueFactory(cellData -> {
            PortInfo portInfo = cellData.getValue();
            SimpleBooleanProperty selected = new SimpleBooleanProperty(selectedPortInfos.contains(portInfo));
            selected.addListener((obs, oldVal, newVal) -> {
                if (newVal) {
                    selectedPortInfos.add(portInfo);
                } else {
                    selectedPortInfos.remove(portInfo);
                }
                updateSelectedLabel();
            });
            return selected;
        });

        // 进程名列 - 支持双击查看详情和复制
        processNameColumn.setCellValueFactory(new PropertyValueFactory<>("processName"));
        processNameColumn.setCellFactory(tc -> {
            TableCell<PortInfo, String> cell = new TableCell<>() {
                @Override
                protected void updateItem(String item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || item == null) {
                        setText(null);
                        setGraphic(null);
                    } else {
                        setText(item);
                        setTooltip(new Tooltip("双击查看进程详情，Ctrl+双击复制"));
                    }
                }
            };
            cell.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && !cell.isEmpty()) {
                    PortInfo portInfo = cell.getTableRow().getItem();
                    if (portInfo != null) {
                        if (event.isControlDown()) {
                            copyToClipboard(cell.getItem());
                        } else {
                            showProcessDetails(portInfo);
                        }
                    }
                }
            });
            return cell;
        });

        // PID列
        pidColumn.setCellValueFactory(new PropertyValueFactory<>("pid"));

        // 端口列
        portColumn.setCellValueFactory(new PropertyValueFactory<>("port"));

        // 协议列
        protocolColumn.setCellValueFactory(new PropertyValueFactory<>("protocol"));

        // 状态列
        stateColumn.setCellValueFactory(new PropertyValueFactory<>("state"));

        // 本地地址列
        localAddressColumn.setCellValueFactory(new PropertyValueFactory<>("localAddress"));

        // 操作列 - 终止按钮和详情按钮
        actionColumn.setCellFactory(tc -> new TableCell<>() {
            private final Button killBtn = new Button("终止");
            private final Button detailBtn = new Button("详情");
            private final HBox container = new HBox(5, detailBtn, killBtn);

            {
                // 终止按钮：使用 kill-button 样式类，在选中行中保持可见
                killBtn.getStyleClass().addAll("kill-button", Styles.SMALL);
                killBtn.setGraphic(new FontIcon(Material2OutlinedAL.DELETE_OUTLINE));
                killBtn.setOnAction(event -> {
                    PortInfo portInfo = getTableView().getItems().get(getIndex());
                    handleKillProcess(portInfo);
                });

                // 详情按钮：使用 detail-button 自定义样式，选中行时文字清晰可见
                detailBtn.getStyleClass().addAll("detail-button", Styles.SMALL);
                detailBtn.setGraphic(new FontIcon(Material2OutlinedAL.INFO));
                detailBtn.setOnAction(event -> {
                    PortInfo portInfo = getTableView().getItems().get(getIndex());
                    showProcessDetails(portInfo);
                });

                container.setAlignment(javafx.geometry.Pos.CENTER);
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : container);
            }
        });

        // 绑定数据
        portTable.setItems(filteredPortData);

        // 初始化筛选下拉框
        initializeFilterControls();
    }

    private void initializeFilterControls() {
        // 协议筛选
        if (protocolFilterCombo != null) {
            protocolFilterCombo.setItems(FXCollections.observableArrayList("全部", "TCP", "UDP"));
            protocolFilterCombo.getSelectionModel().select("全部");
            protocolFilterCombo.valueProperty().addListener((obs, oldVal, newVal) -> {
                currentProtocolFilter = newVal;
                applyFilters();
            });
        }

        // 状态筛选
        if (stateFilterCombo != null) {
            stateFilterCombo.setItems(FXCollections.observableArrayList("全部", "LISTENING", "ESTABLISHED", "TIME_WAIT", "CLOSE_WAIT", "SYN_SENT"));
            stateFilterCombo.getSelectionModel().select("全部");
            stateFilterCombo.valueProperty().addListener((obs, oldVal, newVal) -> {
                currentStateFilter = newVal;
                applyFilters();
            });
        }
    }

    private void setupListeners() {
        // 搜索框实时搜索
        searchField.textProperty().addListener((obs, oldVal, newVal) -> {
            performSearch(newVal);
        });

        // 表格选择监听 - 同步复选框状态
        portTable.getSelectionModel().getSelectedItems().addListener((javafx.collections.ListChangeListener<PortInfo>) change -> {
            while (change.next()) {
                if (change.wasAdded()) {
                    selectedPortInfos.addAll(change.getAddedSubList());
                }
                if (change.wasRemoved()) {
                    selectedPortInfos.removeAll(change.getRemoved());
                }
            }
            updateSelectedLabel();
            portTable.refresh();
        });

        // 设置全局快捷键
        setupKeyboardShortcuts();
    }

    private void setupKeyboardShortcuts() {
        portTable.setOnKeyPressed(event -> {
            switch (event.getCode()) {
                case DELETE:
                    // Delete键终止选中进程
                    if (!selectedPortInfos.isEmpty()) {
                        handleKillSelected();
                    }
                    event.consume();
                    break;
                case R:
                    if (event.isControlDown()) {
                        // Ctrl+R 刷新
                        handleRefresh();
                        event.consume();
                    }
                    break;
                case A:
                    if (event.isControlDown()) {
                        // Ctrl+A 全选
                        portTable.getSelectionModel().selectAll();
                        selectedPortInfos.addAll(filteredPortData);
                        updateSelectedLabel();
                        portTable.refresh();
                        event.consume();
                    }
                    break;
                case C:
                    if (event.isControlDown()) {
                        // Ctrl+C 复制选中项
                        handleCopySelected();
                        event.consume();
                    }
                    break;
                case F:
                    if (event.isControlDown()) {
                        // Ctrl+F 聚焦搜索框
                        searchField.requestFocus();
                        event.consume();
                    }
                    break;
                default:
                    break;
            }
        });
    }

    // ==================== 事件处理方法 ====================

    @FXML
    private void handleRefresh() {
        logger.info("用户点击刷新按钮");
        loadPortData();
    }

    @FXML
    private void handleClearSearch() {
        searchField.clear();
        performSearch("");
    }

    @FXML
    private void handleSearchKeyPress(KeyEvent event) {
        if (event.getCode() == KeyCode.ENTER) {
            performSearch(searchField.getText());
        }
    }

    @FXML
    private void handleScanPort() {
        String startText = startPortField.getText().trim();
        String endText = endPortField.getText().trim();

        if (startText.isEmpty() || endText.isEmpty()) {
            DialogUtils.warning("请输入起始端口和结束端口");
            return;
        }

        int startPort, endPort;
        try {
            startPort = Integer.parseInt(startText);
            endPort = Integer.parseInt(endText);
        } catch (NumberFormatException e) {
            DialogUtils.error("端口号必须是数字");
            return;
        }

        if (startPort < 1 || endPort > 65535 || startPort > endPort) {
            DialogUtils.error("端口范围无效 (1-65535)");
            return;
        }

        if (endPort - startPort > 1000) {
            if (!DialogUtils.confirm("确认", "扫描范围较大 (" + (endPort - startPort + 1) + " 个端口)，可能需要较长时间，是否继续？")) {
                return;
            }
        }

        performPortScan(startPort, endPort);
    }

    @FXML
    private void handleKillSelected() {
        if (selectedPortInfos.isEmpty()) {
            DialogUtils.warning("请先选择要终止的进程");
            return;
        }

        // 获取唯一的PID列表（避免重复终止同一进程）
        Set<Integer> uniquePids = selectedPortInfos.stream()
                .map(PortInfo::getPid)
                .collect(Collectors.toSet());

        if (!DialogUtils.confirm("确认终止",
                "确定要终止选中的 " + uniquePids.size() + " 个进程吗？",
                "确定终止", "取消")) {
            return;
        }

        List<Integer> pids = new ArrayList<>(uniquePids);

        // 使用异步处理避免UI卡顿
        Task<KillResult> killTask = new Task<>() {
            @Override
            protected KillResult call() {
                int successCount = 0;
                int failCount = 0;
                List<String> failedProcesses = new ArrayList<>();

                for (Integer pid : pids) {
                    try {
                        Integer result = portService.kill(pid);
                        if (result != null && result > 0) {
                            successCount++;
                        } else {
                            failCount++;
                            failedProcesses.add("PID: " + pid);
                        }
                    } catch (Exception e) {
                        failCount++;
                        failedProcesses.add("PID: " + pid + " (" + e.getMessage() + ")");
                    }
                }
                return new KillResult(successCount, failCount, failedProcesses);
            }
        };

        killTask.setOnSucceeded(event -> {
            KillResult result = killTask.getValue();
            if (result.successCount > 0) {
                if (result.failCount == 0) {
                    DialogUtils.success("成功终止 " + result.successCount + " 个进程");
                } else {
                    DialogUtils.warning("终止完成：成功 " + result.successCount + " 个，失败 " + result.failCount + " 个\n" +
                            "失败进程：" + String.join(", ", result.failedProcesses.subList(0, Math.min(5, result.failedProcesses.size()))));
                }
                selectedPortInfos.clear();
                portTable.getSelectionModel().clearSelection();
                loadPortData();
            } else {
                DialogUtils.error("终止进程失败，请检查权限或尝试以管理员身份运行程序");
            }
        });

        killTask.setOnFailed(event -> {
            DialogUtils.error("批量终止进程时发生错误: " + killTask.getException().getMessage());
        });

        new Thread(killTask).start();
    }

    // 批量终止结果内部类
    private static class KillResult {
        final int successCount;
        final int failCount;
        final List<String> failedProcesses;

        KillResult(int successCount, int failCount, List<String> failedProcesses) {
            this.successCount = successCount;
            this.failCount = failCount;
            this.failedProcesses = failedProcesses;
        }
    }

    // ==================== 业务逻辑方法 ====================

    private void loadPortData() {
        statusLabel.setText("正在加载...");
        refreshBtn.setDisable(true);

        portService.listPortAsync().thenAccept(portList -> {
            Platform.runLater(() -> {
                allPortData.setAll(portList);
                performSearch(searchField.getText());
                statusLabel.setText("就绪");
                countLabel.setText("共 " + portList.size() + " 个端口");
                refreshBtn.setDisable(false);
                logger.info("加载了 {} 个端口", portList.size());
            });
        }).exceptionally(ex -> {
            Platform.runLater(() -> {
                statusLabel.setText("加载失败");
                refreshBtn.setDisable(false);
                DialogUtils.error("加载端口数据失败: " + ex.getMessage());
                logger.error("加载端口数据失败", ex);
            });
            return null;
        });
    }

    private void performSearch(String keyword) {
        applyFilters();
    }

    private void applyFilters() {
        String keyword = searchField.getText();

        List<PortInfo> filtered = allPortData.stream()
                .filter(p -> {
                    // 关键词搜索
                    if (keyword != null && !keyword.trim().isEmpty()) {
                        if (!matchesKeyword(p, keyword.toLowerCase().trim())) {
                            return false;
                        }
                    }
                    // 协议筛选
                    if (!"全部".equals(currentProtocolFilter)) {
                        if (!currentProtocolFilter.equalsIgnoreCase(p.getProtocol())) {
                            return false;
                        }
                    }
                    // 状态筛选
                    if (!"全部".equals(currentStateFilter)) {
                        if (!currentStateFilter.equalsIgnoreCase(p.getState())) {
                            return false;
                        }
                    }
                    // 端口范围筛选
                    if (currentMinPort != null && p.getPort() < currentMinPort) {
                        return false;
                    }
                    if (currentMaxPort != null && p.getPort() > currentMaxPort) {
                        return false;
                    }
                    return true;
                })
                .collect(Collectors.toList());

        filteredPortData.setAll(filtered);
        countLabel.setText("共 " + filteredPortData.size() + " 个端口");
    }

    private boolean matchesKeyword(PortInfo portInfo, String keyword) {
        return String.valueOf(portInfo.getPort()).contains(keyword)
                || (portInfo.getProcessName() != null && portInfo.getProcessName().toLowerCase().contains(keyword))
                || String.valueOf(portInfo.getPid()).contains(keyword)
                || (portInfo.getProtocol() != null && portInfo.getProtocol().toLowerCase().contains(keyword))
                || (portInfo.getLocalAddress() != null && portInfo.getLocalAddress().toLowerCase().contains(keyword));
    }

    private void performPortScan(int startPort, int endPort) {
        scanBtn.setDisable(true);
        scanProgressBar.setVisible(true);
        scanProgressBar.setManaged(true);
        statusLabel.setText("正在扫描端口...");

        Task<List<Integer>> scanTask = portService.scanPortRangeAsync(startPort, endPort);

        scanTask.setOnSucceeded(event -> {
            List<Integer> openPorts = scanTask.getValue();
            Platform.runLater(() -> {
                scanBtn.setDisable(false);
                scanProgressBar.setVisible(false);
                scanProgressBar.setManaged(false);
                statusLabel.setText("扫描完成");

                if (openPorts.isEmpty()) {
                    DialogUtils.info("端口扫描完成,未发现开放端口");
                } else {
                    String portsStr = openPorts.stream()
                            .map(String::valueOf)
                            .collect(Collectors.joining(", "));
                    DialogUtils.success("发现 " + openPorts.size() + " 个开放端口:\n" + portsStr);
                }
            });
        });

        scanTask.setOnFailed(event -> {
            Platform.runLater(() -> {
                scanBtn.setDisable(false);
                scanProgressBar.setVisible(false);
                scanProgressBar.setManaged(false);
                statusLabel.setText("扫描失败");
                DialogUtils.error("端口扫描失败: " + scanTask.getException().getMessage());
                logger.error("端口扫描失败", scanTask.getException());
            });
        });

        scanProgressBar.progressProperty().bind(scanTask.progressProperty());

        new Thread(scanTask).start();
    }

    private void handleKillProcess(PortInfo portInfo) {
        if (portInfo == null) return;

        String processName = portInfo.getProcessName() != null ? portInfo.getProcessName() : "未知进程";

        if (!DialogUtils.confirm("确认终止",
                "确定要终止进程 [" + processName + "] (PID: " + portInfo.getPid() + ") 吗？",
                "确定终止", "取消")) {
            return;
        }

        // 使用异步处理
        Task<Integer> killTask = new Task<>() {
            @Override
            protected Integer call() {
                return portService.kill(portInfo.getPid());
            }
        };

        killTask.setOnSucceeded(event -> {
            Integer result = killTask.getValue();
            if (result != null && result > 0) {
                DialogUtils.success("成功终止进程 [" + processName + "]");
                loadPortData();
            } else {
                // 检查进程是否还存在
                PortOperateService.ProcessInfo processInfo = portService.getProcessInfo(portInfo.getPid());
                if (processInfo == null) {
                    DialogUtils.info("进程 [" + processName + "] 已经结束或不存在");
                    loadPortData();
                } else {
                    DialogUtils.error("终止进程失败，请检查权限或尝试以管理员身份运行程序\n" +
                            "提示：某些系统进程或受保护的进程无法终止");
                }
            }
        });

        killTask.setOnFailed(event -> {
            DialogUtils.error("终止进程时发生错误: " + killTask.getException().getMessage());
        });

        new Thread(killTask).start();
    }

    private void updateSelectedLabel() {
        int count = selectedPortInfos.size();
        selectedLabel.setText("选中 " + count + " 个");
        if (killSelectedBtn != null) {
            killSelectedBtn.setDisable(count == 0);
        }
        if (copySelectedBtn != null) {
            copySelectedBtn.setDisable(count == 0);
        }
    }

    // ==================== 新增功能方法 ====================

    /**
     * 显示进程详情
     */
    private void showProcessDetails(PortInfo portInfo) {
        if (portInfo == null) return;

        PortOperateService.ProcessInfo processInfo = portService.getProcessInfo(portInfo.getPid());

        StringBuilder details = new StringBuilder();
        details.append("=== 端口信息 ===\n");
        details.append("端口: ").append(portInfo.getPort()).append("\n");
        details.append("协议: ").append(portInfo.getProtocol()).append("\n");
        details.append("状态: ").append(portInfo.getState()).append("\n");
        details.append("本地地址: ").append(portInfo.getLocalAddress()).append("\n");
        details.append("外部地址: ").append(portInfo.getForeignAddress()).append("\n\n");

        details.append("=== 进程信息 ===\n");
        details.append("进程名称: ").append(portInfo.getProcessName() != null ? portInfo.getProcessName() : "未知").append("\n");
        details.append("PID: ").append(portInfo.getPid()).append("\n");

        if (processInfo != null) {
            details.append("内存使用: ").append(processInfo.getMemoryUsage() != null ? processInfo.getMemoryUsage() : "未知").append("\n");
            details.append("会话名: ").append(processInfo.getUserName() != null ? processInfo.getUserName() : "未知").append("\n");
        }

        DialogUtils.showInfo("进程详情", details.toString());
    }

    /**
     * 批量复制选中项
     */
    @FXML
    private void handleCopySelected() {
        if (selectedPortInfos.isEmpty()) {
            DialogUtils.warning("请先选择要复制的端口");
            return;
        }

        StringBuilder sb = new StringBuilder();
        sb.append("进程名称\tPID\t端口\t协议\t状态\t本地地址\n");

        for (PortInfo portInfo : selectedPortInfos) {
            sb.append(portInfo.getProcessName()).append("\t");
            sb.append(portInfo.getPid()).append("\t");
            sb.append(portInfo.getPort()).append("\t");
            sb.append(portInfo.getProtocol()).append("\t");
            sb.append(portInfo.getState()).append("\t");
            sb.append(portInfo.getLocalAddress()).append("\n");
        }

        copyToClipboard(sb.toString());
        DialogUtils.success("已复制 " + selectedPortInfos.size() + " 条记录到剪贴板");
    }

    /**
     * 导出CSV
     */
    @FXML
    private void handleExport() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("导出端口列表");
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("CSV文件", "*.csv"));

        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_HHmmss");
        String defaultFileName = "port_list_" + sdf.format(new Date()) + ".csv";
        fileChooser.setInitialFileName(defaultFileName);

        File file = fileChooser.showSaveDialog(portTable.getScene().getWindow());

        if (file != null) {
            try {
                exportToCsv(file);
                DialogUtils.success("导出成功: " + file.getAbsolutePath());
            } catch (Exception e) {
                DialogUtils.error("导出失败: " + e.getMessage());
                logger.error("导出CSV失败", e);
            }
        }
    }

    private void exportToCsv(File file) throws IOException {
        try (BufferedWriter writer = Files.newBufferedWriter(file.toPath(), StandardCharsets.UTF_8)) {
            // 写入BOM以支持Excel中文显示
            writer.write('\ufeff');

            // 写入表头
            writer.write("进程名称,PID,端口,协议,状态,本地地址,外部地址");
            writer.newLine();

            // 写入数据
            for (PortInfo portInfo : filteredPortData) {
                writer.write(String.format("\"%s\",%d,%d,\"%s\",\"%s\",\"%s\",\"%s\"",
                        escapeCsv(portInfo.getProcessName()),
                        portInfo.getPid(),
                        portInfo.getPort(),
                        portInfo.getProtocol(),
                        portInfo.getState(),
                        portInfo.getLocalAddress(),
                        portInfo.getForeignAddress()));
                writer.newLine();
            }
        }
    }

    private String escapeCsv(String value) {
        if (value == null) return "";
        return value.replace("\"", "\"\"");
    }

    /**
     * 检查指定端口是否被占用
     */
    @FXML
    private void handleCheckPort() {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("端口占用检测");
        dialog.setHeaderText("输入要检测的端口号");
        dialog.setContentText("端口号:");
        try {
            // Alert 的 DialogPane 所在 Scene 的 Window 就是它的 Stage
            Window window = dialog.getDialogPane().getScene().getWindow();
            var iconStream = PortController.class.getResourceAsStream("/images/logo.png");
            Image logo = null;
            if (iconStream != null) {
                logo = new Image(iconStream, 48, 48, true, true); // 固定大小 + 平滑缩放
            } else {
                logger.warn("应用图标资源未找到: /images/logo.png");
            }
            if (window instanceof Stage stage) {
                // 清空原有图标（避免多个图标叠加），然后添加我们的 logo
                if (logo != null) {
                    stage.getIcons().clear();
                    stage.getIcons().add(logo);
                }
            }
        } catch (Exception e) {
            logger.warn("设置 Alert 窗口图标失败", e);
        }
        Optional<String> result = dialog.showAndWait();
        result.ifPresent(portStr -> {
            try {
                int port = Integer.parseInt(portStr.trim());
                if (port < 1 || port > 65535) {
                    DialogUtils.error("端口号必须在 1-65535 之间");
                    return;
                }

                PortInfo portInfo = portService.getByPort(port);
                if (portInfo != null) {
                    String message = String.format("端口 %d 已被占用\n\n进程: %s\nPID: %d\n协议: %s\n状态: %s",
                            port,
                            portInfo.getProcessName() != null ? portInfo.getProcessName() : "未知",
                            portInfo.getPid(),
                            portInfo.getProtocol(),
                            portInfo.getState());
                    DialogUtils.showInfo("端口占用检测", message);
                } else {
                    DialogUtils.success(String.format("端口 %d 未被占用", port));
                }
            } catch (NumberFormatException e) {
                DialogUtils.error("请输入有效的端口号");
            }
        });
    }

    /**
     * 显示高级筛选对话框
     */
    @FXML
    private void handleAdvancedFilter() {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("高级筛选");
        dialog.setHeaderText("设置筛选条件");

        // 创建表单
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new javafx.geometry.Insets(20, 20, 10, 10));

        // 协议筛选
        ComboBox<String> protocolCombo = new ComboBox<>(FXCollections.observableArrayList("全部", "TCP", "UDP"));
        protocolCombo.setValue(currentProtocolFilter);
        grid.add(new Label("协议:"), 0, 0);
        grid.add(protocolCombo, 1, 0);

        // 状态筛选
        ComboBox<String> stateCombo = new ComboBox<>(FXCollections.observableArrayList("全部", "LISTENING", "ESTABLISHED", "TIME_WAIT", "CLOSE_WAIT", "SYN_SENT"));
        stateCombo.setValue(currentStateFilter);
        grid.add(new Label("状态:"), 0, 1);
        grid.add(stateCombo, 1, 1);

        // 端口范围
        TextField minPortField = new TextField(currentMinPort != null ? String.valueOf(currentMinPort) : "");
        minPortField.setPromptText("最小端口");
        TextField maxPortField = new TextField(currentMaxPort != null ? String.valueOf(currentMaxPort) : "");
        maxPortField.setPromptText("最大端口");

        HBox portRangeBox = new HBox(10, minPortField, new Label("-"), maxPortField);
        grid.add(new Label("端口范围:"), 0, 2);
        grid.add(portRangeBox, 1, 2);

        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        Optional<ButtonType> result = dialog.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            currentProtocolFilter = protocolCombo.getValue();
            currentStateFilter = stateCombo.getValue();

            // 更新UI筛选控件
            if (protocolFilterCombo != null) {
                protocolFilterCombo.setValue(currentProtocolFilter);
            }
            if (stateFilterCombo != null) {
                stateFilterCombo.setValue(currentStateFilter);
            }

            // 解析端口范围
            try {
                String minText = minPortField.getText().trim();
                currentMinPort = minText.isEmpty() ? null : Integer.parseInt(minText);
            } catch (NumberFormatException e) {
                currentMinPort = null;
            }

            try {
                String maxText = maxPortField.getText().trim();
                currentMaxPort = maxText.isEmpty() ? null : Integer.parseInt(maxText);
            } catch (NumberFormatException e) {
                currentMaxPort = null;
            }

            applyFilters();
        }
    }

    private void copyToClipboard(String text) {
        if (text == null || text.isEmpty()) return;

        Clipboard clipboard = Clipboard.getSystemClipboard();
        ClipboardContent content = new ClipboardContent();
        content.putString(text);
        clipboard.setContent(content);

        DialogUtils.success("已复制: " + text, Duration.seconds(1));
    }

    /**
     * 获取当前选中项（用于右键菜单等）
     */
    public Set<PortInfo> getSelectedPortInfos() {
        return new HashSet<>(selectedPortInfos);
    }

    /**
     * 清除所有选择
     */
    public void clearSelection() {
        selectedPortInfos.clear();
        portTable.getSelectionModel().clearSelection();
        updateSelectedLabel();
        portTable.refresh();
    }
}
