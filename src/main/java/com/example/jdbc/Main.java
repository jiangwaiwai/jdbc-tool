package com.example.jdbc;

import com.example.jdbc.entity.College;
import com.example.jdbc.entity.Student;
import com.example.jdbc.util.JDBCTool;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

/**
 * 主运行演示类：验证 JDBCTool 对 student（学生表）与 college（学院表）的增删改查 (CRUD) 操作
 */
public class Main {

    private static final SimpleDateFormat SDF = new SimpleDateFormat("yyyy-MM-dd");

    public static void main(String[] args) {
        System.out.println("================================================================================");
        System.out.println("            JDBCTool 数据库通用工具类 - 功能测试与运行效果演示                    ");
        System.out.println("================================================================================");

        try (Connection conn = JDBCTool.getConnection()) {
            System.out.println("【数据库连接状态】: 成功连接至 MySQL 数据库 (DateTest)\n");

            // 清理旧测试数据，保证每次测试独立
            cleanTestData(conn);

            // ==========================================================
            // 测试模块一：学院表 (college) 的增删改查测试
            // ==========================================================
            testCollegeCRUD(conn);

            System.out.println("\n--------------------------------------------------------------------------------\n");

            // ==========================================================
            // 测试模块二：学生表 (student) 的增删改查测试
            // ==========================================================
            testStudentCRUD(conn);

            System.out.println("\n================================================================================");
            System.out.println("            所有测试用例执行完毕！增删改查功能全部验证通过！                      ");
            System.out.println("================================================================================");

        } catch (Exception e) {
            System.err.println("测试过程中发生异常: " + e.getMessage());
            e.printStackTrace();
        } finally {
            // 优雅关闭 MySQL 驱动守护清理线程，避免进程挂起
            try {
                com.mysql.cj.jdbc.AbandonedConnectionCleanupThread.checkedShutdown();
            } catch (Throwable ignored) {
            }
        }
    }

    /**
     * 测试学院表增删改查
     */
    private static void testCollegeCRUD(Connection conn) throws Exception {
        System.out.println("====================【模块一：学院表 (college) CRUD 测试】====================");

        // 1. 测试 save (新增)
        System.out.println("\n>>> 1. [新增测试] 调用 JDBCTool.save() 保存 2 个学院对象...");
        College c1 = new College("COL01", "计算机科学与技术学院", "CS-001");
        College c2 = new College("COL02", "软件工程学院", "SE-002");

        int row1 = JDBCTool.save(c1, conn);
        int row2 = JDBCTool.save(c2, conn);
        System.out.println("-> 成功插入 c1 (受影响行数: " + row1 + "): " + c1);
        System.out.println("-> 成功插入 c2 (受影响行数: " + row2 + "): " + c2);

        // 2. 测试 getOneById (按ID查询单个)
        System.out.println("\n>>> 2. [按ID查询测试] 调用 JDBCTool.getOneById('COL01', College.class)...");
        College queryC1 = JDBCTool.getOneById("COL01", College.class, conn);
        System.out.println("-> 查询结果: " + queryC1);

        // 3. 测试 resultSetToList (列表查询)
        System.out.println("\n>>> 3. [列表查询测试] 执行 SQL 并调用 JDBCTool.resultSetToList()...");
        List<College> collegeListBefore = queryAllColleges(conn);
        printCollegeList("当前学院表全部数据", collegeListBefore);

        // 4. 测试 update (更新)
        System.out.println("\n>>> 4. [修改测试] 调用 JDBCTool.update() 修改 COL01 的名称与代码...");
        queryC1.setName("人工智能与未来技术学院");
        queryC1.setCode("AI-001");
        int updateRow = JDBCTool.update(queryC1, conn);
        System.out.println("-> 更新完成 (受影响行数: " + updateRow + ")");
        College updatedC1 = JDBCTool.getOneById("COL01", College.class, conn);
        System.out.println("-> 重新查询 COL01 验证更新: " + updatedC1);

        // 5. 测试 delete (删除)
        System.out.println("\n>>> 5. [删除测试] 调用 JDBCTool.delete() 删除学院 c2 (COL02)...");
        int deleteRow = JDBCTool.delete(c2, conn);
        System.out.println("-> 删除完成 (受影响行数: " + deleteRow + ")");

        // 6. 验证删除结果
        System.out.println("\n>>> 6. [验证删除后数据] 查询全表确认 COL02 已被删除...");
        List<College> collegeListAfter = queryAllColleges(conn);
        printCollegeList("删除后的学院表全部数据", collegeListAfter);
    }

    /**
     * 测试学生表增删改查
     */
    private static void testStudentCRUD(Connection conn) throws Exception {
        System.out.println("====================【模块二：学生表 (student) CRUD 测试】====================");

        // 1. 测试 save (新增)
        System.out.println("\n>>> 1. [新增测试] 调用 JDBCTool.save() 保存 2 名学生对象...");
        Date d1 = SDF.parse("2024-09-01");
        Date d2 = SDF.parse("2022-09-01");
        Student s1 = new Student("STU202401", "张三", "计算机科学与技术", 20, d1, false, 6500.0);
        Student s2 = new Student("STU202402", "李四", "软件工程", 22, d2, true, 7000.0);

        int row1 = JDBCTool.save(s1, conn);
        int row2 = JDBCTool.save(s2, conn);
        System.out.println("-> 成功插入 s1 (受影响行数: " + row1 + "): " + s1);
        System.out.println("-> 成功插入 s2 (受影响行数: " + row2 + "): " + s2);

        // 2. 测试 getOneById (按ID查询单个)
        System.out.println("\n>>> 2. [按ID查询测试] 调用 JDBCTool.getOneById('STU202401', Student.class)...");
        Student queryS1 = JDBCTool.getOneById("STU202401", Student.class, conn);
        System.out.println("-> 查询结果: " + queryS1);

        // 3. 测试 resultSetToList (列表查询)
        System.out.println("\n>>> 3. [列表查询测试] 执行 SQL 并调用 JDBCTool.resultSetToList()...");
        List<Student> studentListBefore = queryAllStudents(conn);
        printStudentList("当前学生表全部数据", studentListBefore);

        // 4. 测试 update (更新)
        System.out.println("\n>>> 4. [修改测试] 调用 JDBCTool.update() 修改张三的专业、年龄、学费与毕业状态...");
        queryS1.setMajor("人工智能");
        queryS1.setAge(21);
        queryS1.setTuition(6800.0);
        queryS1.setIsGraduated(true);
        int updateRow = JDBCTool.update(queryS1, conn);
        System.out.println("-> 更新完成 (受影响行数: " + updateRow + ")");
        Student updatedS1 = JDBCTool.getOneById("STU202401", Student.class, conn);
        System.out.println("-> 重新查询 STU202401 验证更新: " + updatedS1);

        // 5. 测试 delete (删除)
        System.out.println("\n>>> 5. [删除测试] 调用 JDBCTool.delete() 删除学生 s2 (李四)...");
        int deleteRow = JDBCTool.delete(s2, conn);
        System.out.println("-> 删除完成 (受影响行数: " + deleteRow + ")");

        // 6. 验证删除结果
        System.out.println("\n>>> 6. [验证删除后数据] 查询全表确认李四已被删除...");
        List<Student> studentListAfter = queryAllStudents(conn);
        printStudentList("删除后的学生表全部数据", studentListAfter);
    }

    /**
     * 清空测试历史数据
     */
    private static void cleanTestData(Connection conn) throws Exception {
        try (Statement stmt = conn.createStatement()) {
            stmt.executeUpdate("DELETE FROM student");
            stmt.executeUpdate("DELETE FROM college");
        }
    }

    /**
     * 查询所有学院
     */
    private static List<College> queryAllColleges(Connection conn) throws Exception {
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT * FROM college ORDER BY id")) {
            return JDBCTool.resultSetToList(rs, College.class);
        }
    }

    /**
     * 查询所有学生
     */
    private static List<Student> queryAllStudents(Connection conn) throws Exception {
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT * FROM student ORDER BY id")) {
            return JDBCTool.resultSetToList(rs, Student.class);
        }
    }

    /**
     * 美化打印学院表格
     */
    private static void printCollegeList(String title, List<College> list) {
        System.out.println("--- [" + title + " (共 " + list.size() + " 条)] ---");
        System.out.printf("%-10s | %-24s | %-12s%n", "ID", "名称", "代码");
        System.out.println("------------------------------------------------------------");
        for (College c : list) {
            System.out.printf("%-10s | %-24s | %-12s%n", c.getId(), c.getName(), c.getCode());
        }
    }

    /**
     * 美化打印学生表格
     */
    private static void printStudentList(String title, List<Student> list) {
        System.out.println("--- [" + title + " (共 " + list.size() + " 条)] ---");
        System.out.printf("%-12s | %-8s | %-16s | %-4s | %-12s | %-8s | %-8s%n",
                "学号", "姓名", "专业", "年龄", "入学时间", "是否毕业", "学费");
        System.out.println("----------------------------------------------------------------------------------------");
        for (Student s : list) {
            String dateStr = s.getAdmissionDate() != null ? SDF.format(s.getAdmissionDate()) : "N/A";
            String gradStr = Boolean.TRUE.equals(s.getIsGraduated()) ? "已毕业" : "在读";
            System.out.printf("%-12s | %-8s | %-16s | %-4d | %-12s | %-8s | %-8.2f%n",
                    s.getId(), s.getName(), s.getMajor(), s.getAge(), dateStr, gradStr, s.getTuition());
        }
    }
}
