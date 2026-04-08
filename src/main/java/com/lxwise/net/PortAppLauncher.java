package com.lxwise.net;

import com.lxwise.net.ui.PortApp;
import javafx.application.Application;

/**
 * 应用启动器
 * 用于启动JavaFX应用程序
 *
 * @author lstar
 * @create 2022-03
 * @description: 端口工具启动入口
 */
public class PortAppLauncher {

    public static void main(String[] args) {
        Application.launch(PortApp.class, args);
    }
}
