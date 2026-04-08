package com.lxwise.net.utils;

import atlantafx.base.controls.Notification;
import atlantafx.base.theme.Styles;
import atlantafx.base.util.Animations;
import com.lxwise.net.ui.PortController;
import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Popup;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.stage.Window;
import javafx.util.Duration;
import org.kordamp.ikonli.javafx.FontIcon;
import org.kordamp.ikonli.material2.Material2OutlinedAL;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

/**
 * 对话框工具类
 * 基于AtlantaFX的Notification和Alert封装
 *
 * @author lstar
 * @create 2022-03
 * @description: 消息通知和对话框工具
 */
public final class DialogUtils {
    private static final Logger logger = LoggerFactory.getLogger(DialogUtils.class);

    private static final double NOTIFICATION_WIDTH = 360;
    private static final Duration DEFAULT_DURATION = Duration.seconds(3);

    /** 全局 logo（只加载一次） */
    private static final Image LOGO;

    static {
        var iconStream = DialogUtils.class.getResourceAsStream("/images/logo.png");
        if (iconStream != null) {
            LOGO = new Image(iconStream, 48, 48, true, true); // 固定大小 + 平滑缩放
        } else {
            LOGO = null;
            logger.warn("应用图标资源未找到: /images/logo.png");
        }
    }

    private DialogUtils() {
        // 工具类禁止实例化
    }

    // ==================== 通知消息 ====================

    /**
     * 显示成功消息
     */
    public static void success(String message) {
        showNotification(message, Styles.SUCCESS, Material2OutlinedAL.CHECK_CIRCLE_OUTLINE, DEFAULT_DURATION);
    }

    /**
     * 显示错误消息
     */
    public static void error(String message) {
        showNotification(message, Styles.DANGER, Material2OutlinedAL.ERROR, DEFAULT_DURATION);
    }

    /**
     * 显示警告消息
     */
    public static void warning(String message) {
        showNotification(message, Styles.WARNING, Material2OutlinedAL.ASSIGNMENT, DEFAULT_DURATION);
    }

    /**
     * 显示信息消息
     */
    public static void info(String message) {
        showNotification(message, Styles.ACCENT, Material2OutlinedAL.INFO, DEFAULT_DURATION);
    }

    /**
     * 显示自定义时长的成功消息
     */
    public static void success(String message, Duration duration) {
        showNotification(message, Styles.SUCCESS, Material2OutlinedAL.CHECK_CIRCLE_OUTLINE, duration);
    }

    /**
     * 显示自定义时长的错误消息
     */
    public static void error(String message, Duration duration) {
        showNotification(message, Styles.DANGER, Material2OutlinedAL.ERROR, duration);
    }

    // ==================== 确认对话框 ====================

    /**
     * 显示确认对话框
     *
     * @param title   标题
     * @param message 消息内容
     * @return 是否确认
     */
    public static boolean confirm(String title, String message) {
        Alert alert = createAlert(Alert.AlertType.CONFIRMATION, title, message);
        ButtonType okButton = new ButtonType("确定");
        ButtonType cancelButton = new ButtonType("取消");
        alert.getButtonTypes().setAll(okButton, cancelButton);

        Optional<ButtonType> result = alert.showAndWait();
        return result.isPresent() && result.get() == okButton;
    }

    /**
     * 显示确认对话框（带自定义按钮文本）
     *
     * @param title       标题
     * @param message     消息内容
     * @param okText      确定按钮文本
     * @param cancelText  取消按钮文本
     * @return 是否确认
     */
    public static boolean confirm(String title, String message, String okText, String cancelText) {
        Alert alert = createAlert(Alert.AlertType.CONFIRMATION, title, message);
        ButtonType okButton = new ButtonType(okText);
        ButtonType cancelButton = new ButtonType(cancelText);
        alert.getButtonTypes().setAll(okButton, cancelButton);

        Optional<ButtonType> result = alert.showAndWait();
        return result.isPresent() && result.get() == okButton;
    }

    /**
     * 显示信息对话框
     */
    public static void showInfo(String title, String message) {
        Platform.runLater(() -> {
            Alert alert = createAlert(Alert.AlertType.INFORMATION, title, message);
            alert.showAndWait();
        });
    }

    /**
     * 显示错误对话框
     */
    public static void showError(String title, String message) {
        Platform.runLater(() -> {
            Alert alert = createAlert(Alert.AlertType.ERROR, title, message);
            alert.showAndWait();
        });
    }

    /**
     * 显示警告对话框
     */
    public static void showWarning(String title, String message) {
        Platform.runLater(() -> {
            Alert alert = createAlert(Alert.AlertType.WARNING, title, message);
            alert.showAndWait();
        });
    }

    /**
     * 创建带 logo 的 Alert（核心优化点）
     */
    private static Alert createAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);

        // 设置标题栏左上角的窗口图标
        setWindowIcon(alert);

        return alert;
    }

    /**
     * 为 Alert 设置标题栏窗口图标
     */
    private static void setWindowIcon(Alert alert) {
        if (LOGO == null) {
            return;
        }
        try {
            // Alert 的 DialogPane 所在 Scene 的 Window 就是它的 Stage
            Window window = alert.getDialogPane().getScene().getWindow();
            if (window instanceof Stage stage) {
                // 清空原有图标（避免多个图标叠加），然后添加我们的 logo
                stage.getIcons().clear();
                stage.getIcons().add(LOGO);
            }
        } catch (Exception e) {
            logger.warn("设置 Alert 窗口图标失败", e);
        }
    }

    // ==================== 私有方法 ====================

    private static void showNotification(String message, String style, org.kordamp.ikonli.Ikon icon, Duration duration) {
        Platform.runLater(() -> {
            try {
                Notification notification = new Notification(message, new FontIcon(icon));
                notification.setPrefWidth(NOTIFICATION_WIDTH);
                notification.getStyleClass().addAll(style, Styles.ELEVATED_1);

                Popup popup = new Popup();
                popup.setAutoFix(true);
                popup.setAutoHide(false);

                VBox container = new VBox(10);
                container.setAlignment(Pos.CENTER);
                container.getChildren().add(notification);

                StackPane root = new StackPane(container);
                root.setStyle("-fx-padding: 10;");

                popup.getContent().add(root);

                // 计算位置 - 窗口顶部居中
                Stage stage = getCurrentStage();
                if (stage != null) {
                    popup.setX(stage.getX() + (stage.getWidth() - NOTIFICATION_WIDTH) / 2);
                    popup.setY(stage.getY() + 20);
                } else {
                    // 屏幕顶部居中
                    double screenWidth = Screen.getPrimary().getBounds().getWidth();
                    popup.setX((screenWidth - NOTIFICATION_WIDTH) / 2);
                    popup.setY(20);
                }

                popup.show(stage != null ? stage : new Stage());

                // 入场动画
                Animations.fadeIn(notification, Duration.millis(200)).play();

                // 自动关闭
                PauseTransition delay = new PauseTransition(duration);
                delay.setOnFinished(e -> closeWithAnimation(popup, notification));
                delay.play();

                // 手动关闭
                notification.setOnClose(e -> closeWithAnimation(popup, notification));

            } catch (Exception e) {
                System.err.println("显示通知失败: " + e.getMessage());
            }
        });
    }

    private static void closeWithAnimation(Popup popup, Notification notification) {
        var out = Animations.fadeOut(notification, Duration.millis(250));
        out.setOnFinished(e -> popup.hide());
        out.play();
    }

    private static Stage getCurrentStage() {
        try {
            return Stage.getWindows().stream()
                    .filter(window -> window instanceof Stage && window.isShowing())
                    .map(window -> (Stage) window)
                    .findFirst()
                    .orElse(null);
        } catch (Exception e) {
            return null;
        }
    }
}
