# 作业1：JDBC工具的设计与开发

本工程基于 Java 反射机制与原生 JDBC 实现了一个轻量级通用 ORM 数据持久层工具类 `JDBCTool`，支持对象关系映射与通用的增、删、改、查（CRUD）操作，并对 `DateTest` 数据库中的 `student`（学生表）与 `college`（学院表）进行了完整的业务操作与验证。

---

## 一、 项目技术原理

1. **ORM（对象关系映射）核心原理**
   - **类与表映射**：通过 JavaBean 实体类（如 `Student`, `College`）对应数据库中的同名表（`student`, `college`）。
   - **属性与列映射**：Java 属性采用驼峰命名（`admissionDate`），数据库表字段采用下划线命名（`admission_date`），工具类实现自动双向映射转换。
2. **Java 反射机制应用**
   - **对象创建**：利用 `clazz.getDeclaredConstructor().newInstance()` 动态创建泛型实例。
   - **属性读取与设值**：利用 `Field.setAccessible(true)`、`field.get(obj)`、`field.set(obj, value)`，打破私有访问限制，动态读取对象字段值并构建 SQL 参数，或从 ResultSet 中解析值注入属性。
3. **JDBC 统一访问规范**
   - 基于 `PreparedStatement` 预编译执行 SQL，防范 SQL 注入安全隐患。
   - 统一数据库连接获取与释放流程，保证资源可靠关闭。

---

## 二、 核心工具类 JDBCTool 方法实现说明

按作业要求严格实现了五大核心方法与统一访问辅助方法：

| 序号 | 方法签名 | 功能说明 |
| :--- | :--- | :--- |
| **方法 1** | `public static <T> List<T> resultSetToList(ResultSet rs, Class<T> clazz)` | 接收 ResultSet 与目标 Class，利用元数据与反射创建泛型列表并完成属性映射 |
| **方法 2** | `public static <T> int save(T obj, Connection connection)` | 反射解析对象字段与值，动态拼接并执行 `INSERT` SQL 持久化对象 |
| **方法 3** | `public static <T> int update(T obj, Connection connection)` | 反射解析对象，以 `id` 属性为条件，动态拼接并执行 `UPDATE` SQL |
| **方法 4** | `public static <T> int delete(T obj, Connection connection)` | 反射提取对象的 `id` 属性，动态拼接并执行 `DELETE` SQL |
| **方法 5** | `public static <T> T getOneById(String id, Class<T> clazz, Connection connection)` | 根据主键 ID 与目标 Class，动态查询并返回单一泛型实体对象 |
| **辅助方法** | `getConnection()`, `close()`, `camelToUnderscore()`, `queryList()` 等 | 提供数据库连接读取、安全关闭、命名转换与通用查询支持 |

---

## 三、 数据库与数据表结构 (DateTest)

### 1. 学生表 (`student`)
- `id` (VARCHAR(64), PK): 学号
- `name` (VARCHAR(50)): 姓名
- `major` (VARCHAR(50)): 专业
- `age` (INT): 年龄
- `admission_date` (DATE): 入学时间
- `is_graduated` (TINYINT(1)): 是否毕业
- `tuition` (DECIMAL(10,2)): 学费

### 2. 学院表 (`college`)
- `id` (VARCHAR(64), PK): 学院编号
- `name` (VARCHAR(100)): 学院名称
- `code` (VARCHAR(50)): 学院代码

---

## 四、 项目运行指南

### 1. 配置文件
数据库配置位于 `src/main/resources/jdbc.properties`：
```properties
jdbc.driver=com.mysql.cj.jdbc.Driver
jdbc.url=jdbc:mysql://localhost:3306/DateTest?useSSL=false&serverTimezone=Asia/Shanghai&characterEncoding=utf-8&allowPublicKeyRetrieval=true
jdbc.username=root
jdbc.password=48983626shV
```

### 2. 运行演示主程序
在 IntelliJ IDEA 中打开本项目，直接运行主类 `com.example.jdbc.Main`，即可查看学院表与学生表完整的 CRUD 自动化测试输出。
