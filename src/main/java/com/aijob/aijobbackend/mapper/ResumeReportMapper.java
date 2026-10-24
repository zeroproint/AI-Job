package com.aijob.aijobbackend.mapper;

import com.aijob.aijobbackend.dto.ResumeHistoryItem;
import com.aijob.aijobbackend.entity.ResumeReport;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 简历分析报告表 Mapper
 */
@Mapper
public interface ResumeReportMapper extends BaseMapper<ResumeReport> {

    /**
     * 查询最近 10 份简历的分析历史。
     * <p>
     * 每份简历只取最新一次报告（避免同一简历多次分析在列表里重复，
     * 且点击记录后按 resumeId 打开报告页，展示的也一定是这份最新报告）。
     * 总分从 result（JSON 字符串）中提取，仅在分析成功且 JSON 合法时返回。
     * 自定义 SQL 不会自动追加逻辑删除条件，需手写 deleted = 0。
     */
    @Select("""
            SELECT rp.id                    AS reportId,
                   rp.resume_id             AS resumeId,
                   r.file_name              AS fileName,
                   r.create_time            AS uploadTime,
                   rp.status                AS status,
                   CASE
                       WHEN rp.status = 1 AND JSON_VALID(rp.result)
                           THEN CAST(JSON_EXTRACT(rp.result, '$.total_score') AS SIGNED)
                       ELSE NULL
                       END                  AS totalScore
            FROM resume_report rp
                     INNER JOIN resume r ON r.id = rp.resume_id AND r.deleted = 0
                     INNER JOIN (SELECT resume_id, MAX(id) AS latest_id
                                 FROM resume_report
                                 WHERE deleted = 0
                                 GROUP BY resume_id) latest ON latest.latest_id = rp.id
            WHERE rp.deleted = 0
            ORDER BY rp.id DESC
            LIMIT 10
            """)
    List<ResumeHistoryItem> selectRecentHistory();
}
