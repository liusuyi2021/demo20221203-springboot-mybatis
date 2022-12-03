package com.example.mapper;

import com.example.model.Student;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface StudentMapper {
    int deleteByPrimaryKey(Integer id);

    int insert(Student row);

    int insertSelective(Student row);

    Student selectByPrimaryKey(Integer id);

    int updateByPrimaryKeySelective(Student row);

    int updateByPrimaryKey(Student row);
}