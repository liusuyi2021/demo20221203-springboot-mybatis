package com.example.controller;

import com.alibaba.fastjson.JSON;
import com.example.mapper.StudentMapper;
import com.example.model.Student;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

/**
 * @ClassName StudentController
 * @Description:
 * @Author 刘苏义
 * @Date 2022/12/3 10:06
 * @Version 1.0
 */
@RestController
public class StudentController {

    @Resource
    StudentMapper studentMapper;
    @RequestMapping("/add")
    private String add()
    {
        Student student=new Student();
        student.setName("张曼玉");
        int rows = studentMapper.insert(student);
        return String.valueOf(rows);
    }
    @RequestMapping("/add1")
    private String add1()
    {Student student=new Student();
        student.setName("张曼玉1");
        int rows = studentMapper.insertSelective(student);
        return String.valueOf(rows);
    }
    @RequestMapping("/getStudent/{id}")
    private String getStudent(@PathVariable Integer id)
    {
        Student student = studentMapper.selectByPrimaryKey(id);
        String stringJSON = JSON.toJSONString(student);
        return stringJSON;
    }
    @RequestMapping("/delete/{id}")
    private String deleteStudent(@PathVariable Integer id)
    {
        int student = studentMapper.deleteByPrimaryKey(id);
        return String.valueOf(student);
    }
    @RequestMapping("/update/{id}")
    private String updateStudent(@PathVariable Integer id)
    {
        Student student=new Student();
        student.setId(id);
        student.setName("张曼玉1");
        student.setAge("60");
        student.setPhone("1111111111111");
        int i = studentMapper.updateByPrimaryKey(student);
        return String.valueOf(i);
    }
}
