package com.jaywong.config;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * @author wangjie
 * @create 2021-05-06 11:17
 */
public class ConfigurationManager {
    //私有配置对象
    private static Properties prop = new Properties();

    /**
     * 静态代码块
     * */
    static {
        try {
            InputStream in = ConfigurationManager.class
                    .getClassLoader().getResourceAsStream("blaze.properties");

            //加载配置对象
            prop.load(in);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    /**
     * 获取指定key对应的value
     */
    public static String getProperty(String key){
        return prop.getProperty(key);
    }

    /**
     * 获取整数类型的配置项
     */
    public static Integer getInteger(String key){
        String value = getProperty(key);
        try {
            return Integer.valueOf(value);
        } catch (NumberFormatException e) {
            e.printStackTrace();
        }
        return 0;
    }
    /**
     * 获取布尔类型的配置项
     */
    public static Boolean getBoolean(String key){
        String value = getProperty(key);
        try {
            return Boolean.valueOf(value);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }
    /**
     * 获取Long类型的配置项
     *
     * @param key
     * @return
     */
    public static Long getLong(String key) {
        String value = getProperty(key);
        try {
            return Long.valueOf(value);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return 0L;
    }
}
