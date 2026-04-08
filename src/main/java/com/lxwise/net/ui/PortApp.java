package com.lxwise.net.ui;

import atlantafx.base.theme.PrimerLight;
import com.lxwise.net.utils.Resources;
import com.lxwise.net.tools.CmdConstants;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 端口工具主应用类（跨平台版本）
 * 支持 Windows / Linux / macOS
 *
 * @author lstar
 * @create 2025-04
 */
public class PortApp extends Application {

    private static final Logger logger = LoggerFactory.getLogger(PortApp.class);

    @Override
    public void init() throws Exception {
        logger.info("初始化端口管理工具...");
        logger.info("当前操作系统: {}, OS: {}",
                CmdConstants.getOsName(),
                System.getProperty("os.name"));
        super.init();
    }

    @Override
    public void start(Stage primaryStage) {
        try {
            // 设置 AtlantaFX 主题 - PrimerLight
            Application.setUserAgentStylesheet(new PrimerLight().getUserAgentStylesheet());

            // 加载 FXML
            FXMLLoader loader = Resources.getLoader("/com/lxwise/net/fxml/port.fxml");
            Parent root = loader.getRoot();

            // 创建场景（尺寸与 FXML prefWidth/prefHeight 对齐）
            Scene scene = new Scene(root, 1216, 766);

            // 配置舞台（StageStyle.UNIFIED 仅 Windows/macOS 支持，Linux 回退到 DECORATED）
            if (isUnifiedStyleSupported()) {
                primaryStage.initStyle(StageStyle.UNIFIED);
            } else {
                primaryStage.initStyle(StageStyle.DECORATED);
            }

            primaryStage.setTitle("网络端口管理工具 v2.0");
            primaryStage.setScene(scene);
            primaryStage.setMinWidth(900);
            primaryStage.setMinHeight(620);

            // 设置应用图标（使用 classpath 资源流，跨平台兼容）
            loadAppIcon(primaryStage);

            // 窗口关闭处理
            primaryStage.setOnCloseRequest(event -> {
                logger.info("应用程序关闭中...");
                Platform.exit();
                System.exit(0);
            });

            // 显示窗口
            primaryStage.show();
            primaryStage.toFront();
            primaryStage.requestFocus();

            logger.info("端口管理工具启动成功");

        } catch (Exception e) {
            logger.error("启动应用程序失败", e);
            throw new RuntimeException("无法启动应用程序", e);
        }
    }

    /**
     * 加载应用图标（跨平台安全实现）
     */
    private void loadAppIcon(Stage stage) {
        try {
            // 使用 getResourceAsStream 避免跨平台路径问题
            var iconStream = getClass().getResourceAsStream("/images/logo.png");
            if (iconStream != null) {
                stage.getIcons().add(new Image(iconStream));
                logger.debug("应用图标加载成功");
            } else {
                logger.warn("应用图标资源未找到: /images/logo.png");
            }
        } catch (Exception e) {
            logger.warn("加载应用图标失败: {}", e.getMessage());
        }
    }

    /**
     * 检测当前平台是否支持 StageStyle.UNIFIED
     * UNIFIED 在 Windows 和 macOS 上支持，Linux GTK/X11 不支持
     */
    private boolean isUnifiedStyleSupported() {
        String osName = System.getProperty("os.name", "").toLowerCase();
        return osName.contains("win") || osName.contains("mac");
    }

    @Override
    public void stop() throws Exception {
        logger.info("应用程序正在停止...");
        super.stop();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
