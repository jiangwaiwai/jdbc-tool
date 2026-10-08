package com.example.jdbc.util;

import java.io.InputStream;
import java.lang.reflect.Field;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public class JDBCTool {

    private static Properties props = new Properties();

    static {
        try (InputStream in = JDBCTool.class.getClassLoader().getResourceAsStream("jdbc.properties")) {
            props.load(in);
            Class.forName(props.getProperty("jdbc.driver"));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // 获取数据库连接
    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(
                props.getProperty("jdbc.url"),
                props.getProperty("jdbc.username"),
                props.getProperty("jdbc.password")
        );
    }

    // 关闭资源
    public static void close(Connection conn, Statement stmt, ResultSet rs) {
        try {
            if (rs != null) rs.close();
            if (stmt != null) stmt.close();
            if (conn != null) conn.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // 方法1：结果集转对象列表
    public static <T> List<T> resultSetToList(ResultSet rs, Class<T> clazz) {
        List<T> list = new ArrayList<>();
        try {
            ResultSetMetaData metaData = rs.getMetaData();
            int columnCount = metaData.getColumnCount();

            while (rs.next()) {
                T obj = clazz.getDeclaredConstructor().newInstance();
                for (int i = 1; i <= columnCount; i++) {
                    String columnName = metaData.getColumnLabel(i);
                    Object val = rs.getObject(i);
                    if (val == null) continue;

                    Field field = findField(clazz, columnName);
                    if (field != null) {
                        field.setAccessible(true);
                        // 类型兼容适配
                        if (field.getType() == java.util.Date.class && val instanceof java.sql.Date) {
                            val = new java.util.Date(((java.sql.Date) val).getTime());
                        } else if ((field.getType() == Boolean.class || field.getType() == boolean.class) && val instanceof Number) {
                            val = ((Number) val).intValue() != 0;
                        } else if ((field.getType() == Double.class || field.getType() == double.class) && val instanceof Number) {
                            val = ((Number) val).doubleValue();
                        }
                        field.set(obj, val);
                    }
                }
                list.add(obj);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return list;
    }

    // 方法2：保存数据
    public static <T> int save(T obj, Connection connection) {
        try {
            Class<?> clazz = obj.getClass();
            String tableName = clazz.getSimpleName().toLowerCase();
            Field[] fields = clazz.getDeclaredFields();

            StringBuilder cols = new StringBuilder();
            StringBuilder vals = new StringBuilder();
            List<Object> params = new ArrayList<>();

            for (Field field : fields) {
                field.setAccessible(true);
                Object val = field.get(obj);
                if (val != null) {
                    if (cols.length() > 0) {
                        cols.append(", ");
                        vals.append(", ");
                    }
                    cols.append(camelToUnderline(field.getName()));
                    vals.append("?");
                    if (val instanceof java.util.Date && !(val instanceof java.sql.Date)) {
                        val = new java.sql.Date(((java.util.Date) val).getTime());
                    }
                    params.add(val);
                }
            }

            String sql = "INSERT INTO " + tableName + " (" + cols + ") VALUES (" + vals + ")";
            try (PreparedStatement ps = connection.prepareStatement(sql)) {
                for (int i = 0; i < params.size(); i++) {
                    ps.setObject(i + 1, params.get(i));
                }
                return ps.executeUpdate();
            }
        } catch (Exception e) {
            e.printStackTrace();
            return 0;
        }
    }

    // 方法3：更新数据
    public static <T> int update(T obj, Connection connection) {
        try {
            Class<?> clazz = obj.getClass();
            String tableName = clazz.getSimpleName().toLowerCase();
            Field[] fields = clazz.getDeclaredFields();

            StringBuilder setClause = new StringBuilder();
            List<Object> params = new ArrayList<>();
            Object idVal = null;

            for (Field field : fields) {
                field.setAccessible(true);
                String name = field.getName();
                Object val = field.get(obj);

                if ("id".equalsIgnoreCase(name)) {
                    idVal = val;
                } else if (val != null) {
                    if (setClause.length() > 0) setClause.append(", ");
                    setClause.append(camelToUnderline(name)).append(" = ?");
                    if (val instanceof java.util.Date && !(val instanceof java.sql.Date)) {
                        val = new java.sql.Date(((java.util.Date) val).getTime());
                    }
                    params.add(val);
                }
            }

            if (idVal == null) return 0;
            params.add(idVal);

            String sql = "UPDATE " + tableName + " SET " + setClause + " WHERE id = ?";
            try (PreparedStatement ps = connection.prepareStatement(sql)) {
                for (int i = 0; i < params.size(); i++) {
                    ps.setObject(i + 1, params.get(i));
                }
                return ps.executeUpdate();
            }
        } catch (Exception e) {
            e.printStackTrace();
            return 0;
        }
    }

    // 方法4：删除数据
    public static <T> int delete(T obj, Connection connection) {
        try {
            Class<?> clazz = obj.getClass();
            String tableName = clazz.getSimpleName().toLowerCase();
            Field idField = findField(clazz, "id");
            if (idField == null) return 0;
            idField.setAccessible(true);
            Object idVal = idField.get(obj);

            String sql = "DELETE FROM " + tableName + " WHERE id = ?";
            try (PreparedStatement ps = connection.prepareStatement(sql)) {
                ps.setObject(1, idVal);
                return ps.executeUpdate();
            }
        } catch (Exception e) {
            e.printStackTrace();
            return 0;
        }
    }

    // 方法5：根据ID查询一个对象
    public static <T> T getOneById(String id, Class<T> clazz, Connection connection) {
        String tableName = clazz.getSimpleName().toLowerCase();
        String sql = "SELECT * FROM " + tableName + " WHERE id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                List<T> list = resultSetToList(rs, clazz);
                return list.isEmpty() ? null : list.get(0);
            }
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    // 辅助方法：匹配属性
    private static Field findField(Class<?> clazz, String colName) {
        for (Field f : clazz.getDeclaredFields()) {
            if (f.getName().equalsIgnoreCase(colName) ||
                camelToUnderline(f.getName()).equalsIgnoreCase(colName)) {
                return f;
            }
        }
        return null;
    }

    // 辅助方法：驼峰转下划线
    private static String camelToUnderline(String str) {
        StringBuilder sb = new StringBuilder();
        for (char c : str.toCharArray()) {
            if (Character.isUpperCase(c)) {
                sb.append("_").append(Character.toLowerCase(c));
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    // 辅助方法：免显式传 Connection 的重载统一访问方法
    public static <T> int save(T obj) {
        try (Connection conn = getConnection()) {
            return save(obj, conn);
        } catch (SQLException e) {
            e.printStackTrace();
            return 0;
        }
    }

    public static <T> int update(T obj) {
        try (Connection conn = getConnection()) {
            return update(obj, conn);
        } catch (SQLException e) {
            e.printStackTrace();
            return 0;
        }
    }

    public static <T> int delete(T obj) {
        try (Connection conn = getConnection()) {
            return delete(obj, conn);
        } catch (SQLException e) {
            e.printStackTrace();
            return 0;
        }
    }

    public static <T> T getOneById(String id, Class<T> clazz) {
        try (Connection conn = getConnection()) {
            return getOneById(id, clazz, conn);
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
    }
}
