package com.example.jdbc;

import com.example.jdbc.entity.College;
import com.example.jdbc.entity.Student;
import com.example.jdbc.util.JDBCTool;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.util.List;

public class Main {

    private static SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");

    public static void main(String[] args) throws Exception {
        try (Connection conn = JDBCTool.getConnection()) {
            System.out.println("数据库连接成功\n");

            // 清理旧测试数据
            try (Statement st = conn.createStatement()) {
                st.executeUpdate("DELETE FROM student");
                st.executeUpdate("DELETE FROM college");
            }

            // 1. 测试学院表 (college) 增删改查
            System.out.println("=== 1. 学院表 CRUD 测试 ===");

            // 增
            College c1 = new College("C01", "信息科学与工程学院", "ISE");
            College c2 = new College("C02", "交通运输学院", "TRANS");
            JDBCTool.save(c1, conn);
            JDBCTool.save(c2, conn);
            System.out.println("保存学院成功: ");
            System.out.println(c1);
            System.out.println(c2);

            // 查 (按ID)
            College findC1 = JDBCTool.getOneById("C01", College.class, conn);
            System.out.println("\n按ID查询 C01 结果: " + findC1);

            // 查 (列表)
            System.out.println("\n全表查询学院列表:");
            try (Statement st = conn.createStatement();
                 ResultSet rs = st.executeQuery("SELECT * FROM college")) {
                List<College> list = JDBCTool.resultSetToList(rs, College.class);
                for (College c : list) {
                    System.out.println(c);
                }
            }

            // 改
            findC1.setCode("ISE-NEW");
            JDBCTool.update(findC1, conn);
            College updatedC1 = JDBCTool.getOneById("C01", College.class, conn);
            System.out.println("\n修改 C01 代码后查询: " + updatedC1);

            // 删
            JDBCTool.delete(c2, conn);
            System.out.println("\n删除 C02 (交通运输学院) 成功");

            // 验证删除后的列表
            System.out.println("\n删除后学院列表:");
            try (Statement st = conn.createStatement();
                 ResultSet rs = st.executeQuery("SELECT * FROM college")) {
                List<College> list = JDBCTool.resultSetToList(rs, College.class);
                for (College c : list) {
                    System.out.println(c);
                }
            }

            // 2. 测试学生表 (student) 增删改查
            System.out.println("\n=== 2. 学生表 CRUD 测试 ===");

            // 增
            Student s1 = new Student("S001", "张三", "计算机科学与技术", 20, sdf.parse("2024-09-01"), false, 6500.0);
            Student s2 = new Student("S002", "李四", "交通工程", 21, sdf.parse("2023-09-01"), true, 6000.0);
            JDBCTool.save(s1, conn);
            JDBCTool.save(s2, conn);
            System.out.println("保存学生成功: ");
            System.out.println(s1);
            System.out.println(s2);

            // 查 (按ID)
            Student findS1 = JDBCTool.getOneById("S001", Student.class, conn);
            System.out.println("\n按ID查询 S001 结果: " + findS1);

            // 查 (列表)
            System.out.println("\n全表查询学生列表:");
            try (Statement st = conn.createStatement();
                 ResultSet rs = st.executeQuery("SELECT * FROM student")) {
                List<Student> list = JDBCTool.resultSetToList(rs, Student.class);
                for (Student s : list) {
                    System.out.println(s);
                }
            }

            // 改
            findS1.setMajor("软件工程");
            findS1.setTuition(7000.0);
            findS1.setIsGraduated(true);
            JDBCTool.update(findS1, conn);
            Student updatedS1 = JDBCTool.getOneById("S001", Student.class, conn);
            System.out.println("\n修改 S001 信息后查询: " + updatedS1);

            // 删
            JDBCTool.delete(s2, conn);
            System.out.println("\n删除 S002 (李四) 成功");

            // 验证删除后的列表
            System.out.println("\n删除后学生列表:");
            try (Statement st = conn.createStatement();
                 ResultSet rs = st.executeQuery("SELECT * FROM student")) {
                List<Student> list = JDBCTool.resultSetToList(rs, Student.class);
                for (Student s : list) {
                    System.out.println(s);
                }
            }

            System.out.println("\n所有测试执行完毕。");
        } finally {
            try {
                com.mysql.cj.jdbc.AbandonedConnectionCleanupThread.checkedShutdown();
            } catch (Throwable ignored) {
            }
        }
    }
}
