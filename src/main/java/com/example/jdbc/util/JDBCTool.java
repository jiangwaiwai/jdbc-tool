package com.example.jdbc.util;

import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.math.BigDecimal;
import java.sql.*;
import java.util.*;
import java.util.Date;

/**
 * JDBC 通用工具类 (简易 ORM 框架核心实现)
 * <p>
 * 实现原理：
 * 1. 对象-关系映射 (ORM)：通过类名对应表名、属性名对应表字段。
 * 2. Java 反射 (Reflection)：动态分析对象的字段、读取属性值、创建对象实例并注入查询结果。
 * 3. JDBC 统一访问：基于 PreparedStatement 防止 SQL 注入，统一管理参数与资源释放。
 */
public class JDBCTool {

    private static String driver;
    private static String url;
    private static String username;
    private static String password;

    // 静态代码块：加载 jdbc.properties 配置文件与注册驱动
    static {
        try (InputStream in = JDBCTool.class.getClassLoader().getResourceAsStream("jdbc.properties")) {
            Properties props = new Properties();
            if (in != null) {
                props.load(in);
                driver = props.getProperty("jdbc.driver");
                url = props.getProperty("jdbc.url");
                username = props.getProperty("jdbc.username");
                password = props.getProperty("jdbc.password");
                Class.forName(driver);
            } else {
                throw new RuntimeException("未能找到 jdbc.properties 配置文件！");
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("初始化数据库驱动或读取配置文件失败: " + e.getMessage(), e);
        }
    }

    // ==========================================
    // 老师要求的核心方法 1 ~ 5
    // ==========================================

    /**
     * 方法 1：将 ResultSet 结果集自动转换为泛型对象列表
     * <p>
     * 通过 ResultSetMetaData 获取列名，利用反射动态实例化对象，并将列数据注入到对象属性中。
     *
     * @param rs    SQL 查询结果集
     * @param clazz 目标实体类的 Class 对象
     * @param <T>   实体类型
     * @return 实体对象列表
     */
    public static <T> List<T> resultSetToList(ResultSet rs, Class<T> clazz) {
        List<T> list = new ArrayList<>();
        if (rs == null || clazz == null) {
            return list;
        }

        try {
            // 获取结果集元数据（列数、列名等）
            ResultSetMetaData metaData = rs.getMetaData();
            int columnCount = metaData.getColumnCount();

            // 缓存该类所有非静态字段（字段名小写 -> Field 对象），便于快速匹配
            Map<String, Field> fieldMap = getFieldsMap(clazz);

            while (rs.next()) {
                // 利用反射调用无参构造函数创建目标类的对象实例
                T obj = clazz.getDeclaredConstructor().newInstance();

                for (int i = 1; i <= columnCount; i++) {
                    // 获取列别名或列名
                    String columnLabel = metaData.getColumnLabel(i);
                    Object columnValue = rs.getObject(i);

                    // 规范化列名以匹配字段（支持驼峰命名与下划线命名相互映射）
                    Field targetField = findMatchingField(fieldMap, columnLabel);

                    if (targetField != null && columnValue != null) {
                        targetField.setAccessible(true);
                        // 数据类型兼容转换（如 Timestamp -> java.util.Date, BigDecimal -> Double 等）
                        Object convertedValue = convertType(columnValue, targetField.getType());
                        targetField.set(obj, convertedValue);
                    }
                }
                list.add(obj);
            }
        } catch (Exception e) {
            throw new RuntimeException("ResultSet 转换为对象列表失败: " + e.getMessage(), e);
        }
        return list;
    }

    /**
     * 方法 2：通用的数据保存（INSERT）方法
     * <p>
     * 通过反射提取对象所有非空字段与值，动态拼接 INSERT SQL 语句并执行保存。
     *
     * @param obj        待保存的实体对象
     * @param connection 数据库连接
     * @param <T>        实体类型
     * @return 受影响的行数
     */
    public static <T> int save(T obj, Connection connection) {
        if (obj == null || connection == null) {
            return 0;
        }

        Class<?> clazz = obj.getClass();
        String tableName = getTableName(clazz);

        List<String> columnNames = new ArrayList<>();
        List<String> placeholders = new ArrayList<>();
        List<Object> paramValues = new ArrayList<>();

        for (Field field : getDeclaredFields(clazz)) {
            field.setAccessible(true);
            try {
                Object val = field.get(obj);
                if (val != null) {
                    columnNames.add(camelToUnderscore(field.getName()));
                    placeholders.add("?");
                    paramValues.add(formatParamForSql(val));
                }
            } catch (IllegalAccessException e) {
                throw new RuntimeException("读取对象字段值失败: " + field.getName(), e);
            }
        }

        if (columnNames.isEmpty()) {
            return 0;
        }

        String sql = String.format("INSERT INTO %s (%s) VALUES (%s)",
                tableName,
                String.join(", ", columnNames),
                String.join(", ", placeholders));

        return executeUpdate(connection, sql, paramValues);
    }

    /**
     * 方法 3：通用的数据更新（UPDATE）方法
     * <p>
     * 通过反射提取对象的非主键字段作为更新列，以 id 字段值作为 WHERE 条件动态拼接 UPDATE 语句。
     *
     * @param obj        待更新的实体对象
     * @param connection 数据库连接
     * @param <T>        实体类型
     * @return 受影响的行数
     */
    public static <T> int update(T obj, Connection connection) {
        if (obj == null || connection == null) {
            return 0;
        }

        Class<?> clazz = obj.getClass();
        String tableName = getTableName(clazz);

        List<String> setClauses = new ArrayList<>();
        List<Object> paramValues = new ArrayList<>();
        Object idValue = null;

        for (Field field : getDeclaredFields(clazz)) {
            field.setAccessible(true);
            try {
                String fieldName = field.getName();
                Object val = field.get(obj);

                if ("id".equalsIgnoreCase(fieldName)) {
                    idValue = val;
                } else if (val != null) {
                    setClauses.add(camelToUnderscore(fieldName) + " = ?");
                    paramValues.add(formatParamForSql(val));
                }
            } catch (IllegalAccessException e) {
                throw new RuntimeException("读取对象字段值失败: " + field.getName(), e);
            }
        }

        if (idValue == null) {
            throw new IllegalArgumentException("更新失败：对象中主键 id 为空！");
        }
        if (setClauses.isEmpty()) {
            return 0;
        }

        // 把主键值追加到参数列表最后（对应 WHERE id = ?）
        paramValues.add(idValue);

        String sql = String.format("UPDATE %s SET %s WHERE id = ?",
                tableName,
                String.join(", ", setClauses));

        return executeUpdate(connection, sql, paramValues);
    }

    /**
     * 方法 4：通用的数据删除（DELETE）方法
     * <p>
     * 通过反射获取实体对象的 id 属性值，动态拼接 DELETE 语句执行删除。
     *
     * @param obj        待删除的实体对象
     * @param connection 数据库连接
     * @param <T>        实体类型
     * @return 受影响的行数
     */
    public static <T> int delete(T obj, Connection connection) {
        if (obj == null || connection == null) {
            return 0;
        }

        Class<?> clazz = obj.getClass();
        String tableName = getTableName(clazz);

        Object idValue = null;
        for (Field field : getDeclaredFields(clazz)) {
            if ("id".equalsIgnoreCase(field.getName())) {
                field.setAccessible(true);
                try {
                    idValue = field.get(obj);
                    break;
                } catch (IllegalAccessException e) {
                    throw new RuntimeException("读取 id 字段失败", e);
                }
            }
        }

        if (idValue == null) {
            throw new IllegalArgumentException("删除失败：对象主键 id 为空！");
        }

        String sql = String.format("DELETE FROM %s WHERE id = ?", tableName);
        return executeUpdate(connection, sql, Collections.singletonList(idValue));
    }

    /**
     * 方法 5：根据主键 ID 查询单条记录（SELECT ONE）
     * <p>
     * 根据目标 Class 获取表名，拼接 SQL，查询并复用 resultSetToList 方法转换为对象返回。
     *
     * @param id         主键编号
     * @param clazz      目标实体类 Class
     * @param connection 数据库连接
     * @param <T>        实体类型
     * @return 对应的实体对象，不存在则返回 null
     */
    public static <T> T getOneById(String id, Class<T> clazz, Connection connection) {
        if (id == null || clazz == null || connection == null) {
            return null;
        }

        String tableName = getTableName(clazz);
        String sql = String.format("SELECT * FROM %s WHERE id = ?", tableName);

        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            stmt = connection.prepareStatement(sql);
            stmt.setObject(1, id);
            rs = stmt.executeQuery();
            List<T> list = resultSetToList(rs, clazz);
            return list.isEmpty() ? null : list.get(0);
        } catch (SQLException e) {
            throw new RuntimeException("根据 ID 查询失败: " + e.getMessage(), e);
        } finally {
            close(null, stmt, rs);
        }
    }

    // ==========================================
    // 辅助方法：连接管理、资源释放、反射与数据转换
    // ==========================================

    /**
     * 辅助方法：获取新的数据库连接
     */
    public static Connection getConnection() {
        try {
            return DriverManager.getConnection(url, username, password);
        } catch (SQLException e) {
            throw new RuntimeException("获取数据库连接失败: " + e.getMessage(), e);
        }
    }

    /**
     * 辅助方法：统一安全释放资源
     */
    public static void close(Connection conn, Statement stmt, ResultSet rs) {
        if (rs != null) {
            try {
                rs.close();
            } catch (SQLException ignored) {
            }
        }
        if (stmt != null) {
            try {
                stmt.close();
            } catch (SQLException ignored) {
            }
        }
        if (conn != null) {
            try {
                conn.close();
            } catch (SQLException ignored) {
            }
        }
    }

    /**
     * 辅助方法：执行通用的全表或条件查询列表
     */
    public static <T> List<T> queryList(String sql, Class<T> clazz, Connection connection, Object... params) {
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            stmt = connection.prepareStatement(sql);
            if (params != null) {
                for (int i = 0; i < params.length; i++) {
                    stmt.setObject(i + 1, params[i]);
                }
            }
            rs = stmt.executeQuery();
            return resultSetToList(rs, clazz);
        } catch (SQLException e) {
            throw new RuntimeException("查询数据失败: " + e.getMessage(), e);
        } finally {
            close(null, stmt, rs);
        }
    }

    /**
     * 辅助方法：统一执行 DML 更新操作 (INSERT/UPDATE/DELETE)
     */
    private static int executeUpdate(Connection connection, String sql, List<Object> params) {
        PreparedStatement stmt = null;
        try {
            stmt = connection.prepareStatement(sql);
            for (int i = 0; i < params.size(); i++) {
                stmt.setObject(i + 1, params.get(i));
            }
            return stmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("执行 SQL 失败 [" + sql + "]: " + e.getMessage(), e);
        } finally {
            close(null, stmt, null);
        }
    }

    /**
     * 辅助方法：根据类名确定数据库表名（默认为小写类名）
     */
    public static String getTableName(Class<?> clazz) {
        return clazz.getSimpleName().toLowerCase();
    }

    /**
     * 辅助方法：获取类及其父类中所有非静态字段
     */
    private static List<Field> getDeclaredFields(Class<?> clazz) {
        List<Field> fields = new ArrayList<>();
        Class<?> current = clazz;
        while (current != null && current != Object.class) {
            for (Field field : current.getDeclaredFields()) {
                if (!Modifier.isStatic(field.getModifiers())) {
                    fields.add(field);
                }
            }
            current = current.getSuperclass();
        }
        return fields;
    }

    /**
     * 辅助方法：获取类的字段映射表（转换为小写字段名索引）
     */
    private static Map<String, Field> getFieldsMap(Class<?> clazz) {
        Map<String, Field> map = new HashMap<>();
        for (Field f : getDeclaredFields(clazz)) {
            map.put(f.getName().toLowerCase(), f);
            map.put(camelToUnderscore(f.getName()).toLowerCase(), f);
        }
        return map;
    }

    /**
     * 辅助方法：匹配数据库列名与实体类 Field
     */
    private static Field findMatchingField(Map<String, Field> fieldMap, String columnLabel) {
        String cleanLabel = columnLabel.toLowerCase().replace("_", "");
        for (Map.Entry<String, Field> entry : fieldMap.entrySet()) {
            String cleanKey = entry.getKey().toLowerCase().replace("_", "");
            if (cleanKey.equals(cleanLabel)) {
                return entry.getValue();
            }
        }
        return fieldMap.get(columnLabel.toLowerCase());
    }

    /**
     * 辅助方法：驼峰命名转下划线命名 (e.g. admissionDate -> admission_date)
     */
    public static String camelToUnderscore(String str) {
        if (str == null || str.isEmpty()) {
            return str;
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < str.length(); i++) {
            char c = str.charAt(i);
            if (Character.isUpperCase(c)) {
                sb.append('_').append(Character.toLowerCase(c));
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    /**
     * 辅助方法：将参数值转换为适合 JDBC 设置的类型（如 java.util.Date -> java.sql.Date）
     */
    private static Object formatParamForSql(Object value) {
        if (value instanceof Date && !(value instanceof java.sql.Date || value instanceof java.sql.Timestamp)) {
            return new java.sql.Date(((Date) value).getTime());
        }
        return value;
    }

    /**
     * 辅助方法：处理数据类型转换，确保 ResultSet 取出的值安全注入到目标类型的 Field
     */
    private static Object convertType(Object value, Class<?> targetType) {
        if (value == null) {
            return null;
        }
        if (targetType.isAssignableFrom(value.getClass())) {
            return value;
        }

        // 处理 日期 类型转换
        if (targetType == Date.class) {
            if (value instanceof java.sql.Date) {
                return new Date(((java.sql.Date) value).getTime());
            }
            if (value instanceof java.sql.Timestamp) {
                return new Date(((java.sql.Timestamp) value).getTime());
            }
        }

        // 处理 布尔 类型转换 (MySQL TINYINT(1) 可能是 Number)
        if (targetType == Boolean.class || targetType == boolean.class) {
            if (value instanceof Number) {
                return ((Number) value).intValue() != 0;
            }
            if (value instanceof String) {
                return "1".equals(value) || "true".equalsIgnoreCase((String) value);
            }
        }

        // 处理 浮点数 (Double / Float / BigDecimal)
        if (targetType == Double.class || targetType == double.class) {
            if (value instanceof Number) {
                return ((Number) value).doubleValue();
            }
            return Double.parseDouble(value.toString());
        }

        // 处理 整数
        if (targetType == Integer.class || targetType == int.class) {
            if (value instanceof Number) {
                return ((Number) value).intValue();
            }
            return Integer.parseInt(value.toString());
        }

        // 处理 长整数
        if (targetType == Long.class || targetType == long.class) {
            if (value instanceof Number) {
                return ((Number) value).longValue();
            }
            return Long.parseLong(value.toString());
        }

        // 处理 字符串
        if (targetType == String.class) {
            return value.toString();
        }

        return value;
    }
}
