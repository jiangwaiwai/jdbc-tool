-- =================================================================
-- 作业1：JDBC工具的设计与开发 - 数据库初始化脚本
-- 数据库名：DateTest
-- 数据表：student（学生表）、college（学院表）
-- =================================================================

CREATE DATABASE IF NOT EXISTS DateTest CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE DateTest;

-- 1. 创建学生表 (student)
DROP TABLE IF EXISTS student;
CREATE TABLE student (
    id VARCHAR(64) PRIMARY KEY COMMENT '学号(主键)',
    name VARCHAR(50) NOT NULL COMMENT '姓名',
    major VARCHAR(50) NOT NULL COMMENT '专业',
    age INT NOT NULL COMMENT '年龄',
    admission_date DATE NOT NULL COMMENT '入学时间',
    is_graduated TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否毕业(0:未毕业, 1:已毕业)',
    tuition DECIMAL(10, 2) NOT NULL COMMENT '学费'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='学生表';

-- 2. 创建学院表 (college)
DROP TABLE IF EXISTS college;
CREATE TABLE college (
    id VARCHAR(64) PRIMARY KEY COMMENT '学院编号(主键)',
    name VARCHAR(100) NOT NULL COMMENT '学院名称',
    code VARCHAR(50) NOT NULL COMMENT '学院代码'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='学院表';
