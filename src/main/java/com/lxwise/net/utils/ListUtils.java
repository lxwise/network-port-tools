package com.lxwise.net.utils;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 集合工具类
 * 提供常用的集合判断方法
 *
 * @author lstar
 * @create 2022-03
 * @update 2025-04 扩展集合类型支持
 */
public final class ListUtils {

    private ListUtils() {
        // 工具类禁止实例化
    }

    /**
     * 判断List是否为空
     *
     * @param list 列表
     * @return true表示为空
     */
    public static boolean isEmpty(List<?> list) {
        return list == null || list.isEmpty();
    }

    /**
     * 判断List是否不为空
     *
     * @param list 列表
     * @return true表示不为空
     */
    public static boolean isNotEmpty(List<?> list) {
        return !isEmpty(list);
    }

    /**
     * 判断Collection是否为空
     *
     * @param collection 集合
     * @return true表示为空
     */
    public static boolean isEmpty(Collection<?> collection) {
        return collection == null || collection.isEmpty();
    }

    /**
     * 判断Collection是否不为空
     *
     * @param collection 集合
     * @return true表示不为空
     */
    public static boolean isNotEmpty(Collection<?> collection) {
        return !isEmpty(collection);
    }

    /**
     * 判断Map是否为空
     *
     * @param map 映射
     * @return true表示为空
     */
    public static boolean isEmpty(Map<?, ?> map) {
        return map == null || map.isEmpty();
    }

    /**
     * 判断Map是否不为空
     *
     * @param map 映射
     * @return true表示不为空
     */
    public static boolean isNotEmpty(Map<?, ?> map) {
        return !isEmpty(map);
    }

    /**
     * 判断Set是否为空
     *
     * @param set 集合
     * @return true表示为空
     */
    public static boolean isEmpty(Set<?> set) {
        return set == null || set.isEmpty();
    }

    /**
     * 判断Set是否不为空
     *
     * @param set 集合
     * @return true表示不为空
     */
    public static boolean isNotEmpty(Set<?> set) {
        return !isEmpty(set);
    }

    /**
     * 获取集合大小（安全）
     *
     * @param collection 集合
     * @return 集合大小，null返回0
     */
    public static int size(Collection<?> collection) {
        return collection == null ? 0 : collection.size();
    }

    /**
     * 获取Map大小（安全）
     *
     * @param map 映射
     * @return 映射大小，null返回0
     */
    public static int size(Map<?, ?> map) {
        return map == null ? 0 : map.size();
    }
}
