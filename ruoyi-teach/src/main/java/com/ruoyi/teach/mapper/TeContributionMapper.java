package com.ruoyi.teach.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Param;
import com.ruoyi.teach.domain.TeContribution;

/**
 * ============================================================================
 * 【功能】模块贡献率 Mapper 接口（操作表 te_contribution）
 * ----------------------------------------------------------------------------
 * 【说明】贡献率分配与组员确认：
 *         - selectByGroup：本组模块5-10全部分配行（联查姓名学号）
 *         - deleteByGroupModule + batchInsert：组长整体重分配（重置确认）
 *         - confirmByGroupUser：组员一键确认本人全部分配行
 * ============================================================================
 */
public interface TeContributionMapper
{
    /**
     * 查询本组全部贡献率记录（联查组员姓名/学号，按模块号、用户ID排序）
     *
     * @param groupId 小组ID
     * @return 贡献率集合
     */
    public List<TeContribution> selectByGroup(Long groupId);

    /**
     * 删除本组某模块的全部分配行（组长重新分配前先清空）
     *
     * @param groupId 小组ID
     * @param moduleCode 模块编号
     * @return 影响行数
     */
    public int deleteByGroupModule(@Param("groupId") Long groupId, @Param("moduleCode") Long moduleCode);

    /**
     * 批量插入贡献率分配行
     *
     * @param list 贡献率集合
     * @return 影响行数
     */
    public int batchInsert(List<TeContribution> list);

    /**
     * 组员确认本人分配（本组所有行 confirmed=1 并记录确认时间）
     *
     * @param groupId 小组ID
     * @param userId 组员用户ID
     * @return 影响行数
     */
    public int confirmByGroupUser(@Param("groupId") Long groupId, @Param("userId") Long userId);
}
