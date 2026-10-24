package com.aijob.aijobbackend.mapper;

import com.aijob.aijobbackend.entity.JobPosition;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 职位表 Mapper
 */
@Mapper
public interface JobPositionMapper extends BaseMapper<JobPosition> {

    /**
     * 按条件动态查询职位（SQL 见 resources/mapper/JobPositionMapper.xml）。
     * 参数为空表示不限制。
     */
    List<JobPosition> selectByCondition(@Param("keyword") String keyword,
                                        @Param("location") String location,
                                        @Param("education") String education,
                                        @Param("experience") String experience,
                                        @Param("size") String size,
                                        @Param("financing") String financing,
                                        @Param("salaryMin") Integer salaryMin,
                                        @Param("salaryMax") Integer salaryMax);
}
