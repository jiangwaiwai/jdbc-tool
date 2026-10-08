package com.example.jdbc.entity;

import java.util.Date;

/**
 * 学生实体类 (对应数据库表: student)
 * 包含字段: id, name, major, age, 入学时间(admissionDate), 是否毕业(isGraduated), 学费(tuition)
 */
public class Student {
    private String id;
    private String name;
    private String major;
    private Integer age;
    private Date admissionDate;
    private Boolean isGraduated;
    private Double tuition;

    // 必须提供无参构造函数（反射实例化所需）
    public Student() {
    }

    public Student(String id, String name, String major, Integer age, Date admissionDate, Boolean isGraduated, Double tuition) {
        this.id = id;
        this.name = name;
        this.major = major;
        this.age = age;
        this.admissionDate = admissionDate;
        this.isGraduated = isGraduated;
        this.tuition = tuition;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getMajor() {
        return major;
    }

    public void setMajor(String major) {
        this.major = major;
    }

    public Integer getAge() {
        return age;
    }

    public void setAge(Integer age) {
        this.age = age;
    }

    public Date getAdmissionDate() {
        return admissionDate;
    }

    public void setAdmissionDate(Date admissionDate) {
        this.admissionDate = admissionDate;
    }

    public Boolean getIsGraduated() {
        return isGraduated;
    }

    public void setIsGraduated(Boolean isGraduated) {
        this.isGraduated = isGraduated;
    }

    public Double getTuition() {
        return tuition;
    }

    public void setTuition(Double tuition) {
        this.tuition = tuition;
    }

    @Override
    public String toString() {
        return "Student{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", major='" + major + '\'' +
                ", age=" + age +
                ", admissionDate=" + admissionDate +
                ", isGraduated=" + isGraduated +
                ", tuition=" + tuition +
                '}';
    }
}
