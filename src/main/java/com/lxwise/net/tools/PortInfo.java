package com.lxwise.net.tools;

import javafx.beans.property.*;

import java.util.Objects;

/**
 * 端口信息实体类
 * 支持JavaFX属性绑定
 *
 * @author lstar
 * @create 2022-03
 * @update 2025-04 升级为JavaFX属性绑定
 */
public class PortInfo {

    private final IntegerProperty port = new SimpleIntegerProperty();
    private final IntegerProperty pid = new SimpleIntegerProperty();
    private final StringProperty processName = new SimpleStringProperty();
    private final StringProperty protocol = new SimpleStringProperty();
    private final StringProperty state = new SimpleStringProperty();
    private final StringProperty localAddress = new SimpleStringProperty();
    private final StringProperty foreignAddress = new SimpleStringProperty();

    public PortInfo() {
    }

    public PortInfo(int port, int pid, String protocol) {
        this.port.set(port);
        this.pid.set(pid);
        this.protocol.set(protocol);
    }

    public PortInfo(int port, int pid, String protocol, String state, String localAddress, String foreignAddress) {
        this.port.set(port);
        this.pid.set(pid);
        this.protocol.set(protocol);
        this.state.set(state);
        this.localAddress.set(localAddress);
        this.foreignAddress.set(foreignAddress);
    }

    // ==================== JavaFX属性访问器 ====================

    public IntegerProperty portProperty() {
        return port;
    }

    public IntegerProperty pidProperty() {
        return pid;
    }

    public StringProperty processNameProperty() {
        return processName;
    }

    public StringProperty protocolProperty() {
        return protocol;
    }

    public StringProperty stateProperty() {
        return state;
    }

    public StringProperty localAddressProperty() {
        return localAddress;
    }

    public StringProperty foreignAddressProperty() {
        return foreignAddress;
    }

    // ==================== Getter和Setter ====================

    public int getPort() {
        return port.get();
    }

    public void setPort(int port) {
        this.port.set(port);
    }

    public int getPid() {
        return pid.get();
    }

    public void setPid(int pid) {
        this.pid.set(pid);
    }

    public String getProcessName() {
        return processName.get();
    }

    public void setProcessName(String processName) {
        this.processName.set(processName);
    }

    public String getProtocol() {
        return protocol.get();
    }

    public void setProtocol(String protocol) {
        this.protocol.set(protocol);
    }

    public String getState() {
        return state.get();
    }

    public void setState(String state) {
        this.state.set(state);
    }

    public String getLocalAddress() {
        return localAddress.get();
    }

    public void setLocalAddress(String localAddress) {
        this.localAddress.set(localAddress);
    }

    public String getForeignAddress() {
        return foreignAddress.get();
    }

    public void setForeignAddress(String foreignAddress) {
        this.foreignAddress.set(foreignAddress);
    }

    // ==================== 重写方法 ====================

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PortInfo portInfo = (PortInfo) o;
        return getPort() == portInfo.getPort()
                && getPid() == portInfo.getPid()
                && Objects.equals(getProcessName(), portInfo.getProcessName())
                && Objects.equals(getProtocol(), portInfo.getProtocol());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getPort(), getPid(), getProcessName(), getProtocol());
    }

    @Override
    public String toString() {
        return "PortInfo{" +
                "port=" + getPort() +
                ", pid=" + getPid() +
                ", processName='" + getProcessName() + '\'' +
                ", protocol='" + getProtocol() + '\'' +
                ", state='" + getState() + '\'' +
                '}';
    }
}
