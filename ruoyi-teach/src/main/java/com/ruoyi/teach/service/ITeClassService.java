package com.ruoyi.teach.service;

import java.util.List;
import java.util.Map;
import com.ruoyi.teach.domain.TeClass;
import com.ruoyi.teach.domain.TeStudent;

/**
 * ============================================================================
 * 【功能】班级管理业务接口（教师端核心）
 * ----------------------------------------------------------------------------
 * 【能力】建班（自动生成字母小组）→ Excel 名单导入（自动建号+自动分组）→
 *         分组看板 → 手动调组 / 自动重新分组 → 删除班级（级联清理）
 * ============================================================================
 */
public interface ITeClassService
{
    /** 查询班级列表（教师仅自己的班级，管理员全部） */
    public List<TeClass> selectTeClassList(TeClass teClass);

    /** 查询班级详情 */
    public TeClass selectTeClassById(Long id);

    /** 新增班级（按分组数自动生成 A/B/C…小组） */
    public int insertTeClass(TeClass teClass, String operateBy);

    /** 修改班级（名称/截止时间/分组数等；调整分组数有强校验） */
    public int updateTeClass(TeClass teClass, String operateBy);

    /** 批量删除班级（有学生拒绝；级联清理小组/任务/设置等配置数据） */
    public int deleteTeClassByIds(Long[] ids, String operateBy);

    /**
     * Excel 名单导入（列：学号/姓名）
     * 自动创建登录账号（初始密码=学号后6位）+ 按学号末2位自动分组 + 归入本班
     */
    public String importStudents(Long classId, List<TeStudent> studentList, boolean updateSupport, String operateBy);

    /** 分组看板：班级小组列表（含组内成员与已分配任务） */
    public List<Map<String, Object>> selectGroupBoard(Long classId);

    /** 自动重新分组：对本班全部学生按"学号末2位对组数取模"重新归组 */
    public String autoGrouping(Long classId);

    /** 手动调整单个学生的分组 */
    public int assignStudent(Long userId, Long groupId);
}
