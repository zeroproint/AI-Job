package com.aijob.aijobbackend.mapper;

import com.aijob.aijobbackend.entity.Resume;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 简历表 Mapper
 */
@Mapper
public interface ResumeMapper extends BaseMapper<Resume> {
}
