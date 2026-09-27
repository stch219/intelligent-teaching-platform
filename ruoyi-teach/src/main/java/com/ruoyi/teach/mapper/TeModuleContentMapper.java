package com.ruoyi.teach.mapper;

import java.util.List;
import java.util.Map;
import org.apache.ibatis.annotations.Param;
import com.ruoyi.teach.domain.TeModuleContent;

/**
 * ============================================================================
 * 【功能】模块内容 Mapper 接口（操作表 te_module_content）
 * ----------------------------------------------------------------------------
 * 【说明】服务学生端协同编辑：
 *         - selectModuleBoard：模块设置 × 本组内容 × 模块赋分 三表联查看板
 *         - selectByGroupModule：定位本组某模块内容（乐观锁编辑入口）
 *         - updateWithVersion：带版本号的条件更新（组员同时编辑防覆盖）
 * ============================================================================
 */
public interface TeModuleContentMapper
{
    /**
     * 模块看板联查（模块设置 + 本组内容状态 + 模块赋分，共10行）
     *
     * @param classId 班级ID
     * @param groupId 小组ID
     * @return 看板行集合（moduleCode/moduleName/countType/minCount/maxCount/
     *         editable/score/status/progress/wordCount/itemCount/updateTime）
     */
    public List<Map<String, Object>> selectModuleBoard(@Param("classId") Long classId, @Param("groupId") Long groupId);

    /**
     * 查询本组某模块内容记录（唯一键 group_id + module_code）
     *
     * @param groupId 小组ID
     * @param moduleCode 模块编号
     * @return 模块内容（不存在返回 null）
     */
    public TeModuleContent selectByGroupModule(@Param("groupId") Long groupId, @Param("moduleCode") Long moduleCode);

    /**
     * 新增模块内容记录（首次编辑时初始化）
     *
     * @param content 模块内容
     * @return 影响行数
     */
    public int insertTeModuleContent(TeModuleContent content);

    /**
     * 带乐观锁的内容更新（version 不匹配返回0行 → 前端提示刷新）
     *
     * @param content 模块内容（含 id/version/status/progress/wordCount/itemCount）
     * @return 影响行数（0表示版本冲突）
     */
    public int updateWithVersion(TeModuleContent content);
}
