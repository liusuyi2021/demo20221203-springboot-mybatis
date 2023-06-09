package com.example.controller;

import com.alibaba.fastjson.JSON;
import com.example.mapper.StudentMapper;
import com.example.model.Student;
import io.github.yedaxia.apidocs.ApiDoc;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;

/**
 * 学生接口
 */
@RequestMapping("/api/user/")
@RestController
public class StudentController {

    @Resource
    private StudentMapper studentMapper;

    /**
     * 增加学生
     */
    @GetMapping("/add")
    public String add() {
        Student student = new Student();
        student.setName("张曼玉");
        int rows = studentMapper.insert(student);
        return String.valueOf(rows);
    }
    /**
     * 增加学生1
     */
    @RequestMapping("/add1")
    public String add1() {
        Student student = new Student();
        student.setName("张曼玉1");
        int rows = studentMapper.insertSelective(student);
        return String.valueOf(rows);
    }

    /**
     * 获取学生
     * @param id 编号
     */
    @RequestMapping("/getStudent/{id}")
    public String getStudent(@PathVariable Integer id) {
        Student student = studentMapper.selectByPrimaryKey(id);
        String stringJSON = JSON.toJSONString(student);
        return stringJSON;
    }
    /**
     * 删除
     * @description 删除学生
     * @param id 编号
     * @return java.lang.String
     * @author 刘苏义
     * @time 2023/6/9 11:23
     */
    @RequestMapping("/delete/{id}")
    @ApiDoc(result = String.class, url = "/delete/id", method = "put")
    public String deleteStudent( @PathVariable Integer id) {

        int student = studentMapper.deleteByPrimaryKey(id);
        return String.valueOf(student);
    }

    @RequestMapping("/update/{id}")
    public String updateStudent(@PathVariable Integer id) {
        Student student = new Student();
        student.setId(id);
        student.setName("张曼玉1");
        student.setAge("60");
        student.setPhone("1111111111111");
        int i = studentMapper.updateByPrimaryKey(student);
        return String.valueOf(i);
    }
}
