package com.ruoyi.teach.service;

import java.util.List;
import java.util.Map;
import com.ruoyi.teach.domain.TeContribution;
import com.ruoyi.teach.domain.TeModuleContent;

/**
 * ============================================================================
 * 【功能】学生端门户 Service 接口（阶段4）
 * ----------------------------------------------------------------------------
 * 【说明】面向学生的核心业务：
 *         ①我的信息 + 组内角色选定与分工填报
 *         ②公示板（任务/组员/资料/截止时间/指导教师）
 *         ③10大模块协同编辑（暂存/提交、字数条目校验、乐观锁防覆盖、
 *           只读模块拦截、任务要求模块自动合成）
 *         ④贡献率分配（组长分配，每模块合计100%）与组员确认
 *         ⑤组长分工确认
 * ============================================================================
 */
public interface ITeStudentPortalService
{
    /**
     * 我的信息（学籍 + 班级 + 小组 + 角色 + 分工 + 指导教师）
     *
     * @param userId 当前学生用户ID
     * @return 信息聚合 Map
     */
    public Map<String, Object> myInfo(Long userId);

    /**
     * 选定组内角色并填报分工（角色变更校验组长唯一；分工变化重置确认状态）
     *
     * @param userId 当前学生用户ID
     * @param roleType 组内角色（1组长 2汇报人 3组长兼汇报人 4成员）
     * @param dutyAssignment 个人分工说明
     */
    public void updateMyRole(Long userId, Long roleType, String dutyAssignment);

    /**
     * 公示板聚合（我的任务 + 组员分工一览 + 可见资料 + 截止时间）
     *
     * @param userId 当前学生用户ID
     * @return 公示板聚合 Map
     */
    public Map<String, Object> board(Long userId);

    /**
     * 模块看板（10模块设置 + 本组内容状态/进度 + 模块赋分）
     *
     * @param userId 当前学生用户ID
     * @return 看板行集合
     */
    public List<Map<String, Object>> moduleBoard(Long userId);

    /**
     * 模块内容详情（含设置/内容/版本号；"任务要求"模块无内容时自动合成）
     *
     * @param userId 当前学生用户ID
     * @param moduleCode 模块编号（1-10）
     * @return 详情 Map
     */
    public Map<String, Object> moduleDetail(Long userId, Long moduleCode);

    /**
     * 暂存模块内容（协同编辑：乐观锁防覆盖 + 上限校验 + 进度折算）
     *
     * @param userId 当前学生用户ID
     * @param form 表单（moduleCode/content/version）
     */
    public void saveContent(Long userId, TeModuleContent form);

    /**
     * 提交模块内容（校验字数/条目达到下限；提交后锁定不可再编辑）
     *
     * @param userId 当前学生用户ID
     * @param form 表单（moduleCode/content/version）
     */
    public void submitContent(Long userId, TeModuleContent form);

    /**
     * 贡献率面板数据（组员列表 + 本组分配行 + 赋分 + 我是否组长）
     *
     * @param userId 当前学生用户ID
     * @return 面板聚合 Map
     */
    public Map<String, Object> contributionPanel(Long userId);

    /**
     * 组长保存贡献率分配（模块5-10每模块组内合计必须=100%）
     *
     * @param userId 当前学生用户ID
     * @param list 分配行集合（moduleCode/userId/ratio）
     */
    public void saveContribution(Long userId, List<TeContribution> list);

    /**
     * 组员确认贡献率（一键确认本组本人全部分配行）
     *
     * @param userId 当前学生用户ID
     */
    public void confirmContribution(Long userId);

    /**
     * 组长确认组员分工（duty_status 置1）
     *
     * @param leaderUserId 组长用户ID
     * @param memberUserId 组员用户ID
     */
    public void confirmDuty(Long leaderUserId, Long memberUserId);

    /**
     * 总稿合规预检（阶段5）：组长身份 / 模块全部提交 / 全员分工确认 /
     * 贡献率分配与全员确认，共4项检查
     *
     * @param userId 当前学生用户ID（须为组长）
     * @return 检查结果集合（每项含 code/item/passed/detail）
     */
    public List<Map<String, Object>> submitPrecheck(Long userId);

    /**
     * 提交总稿（阶段5）：预检全过后，系统生成封面写入模块1并锁定，
     * 小组置为"已提交总稿"状态并记录提交时间，组长分工自动确认
     *
     * @param userId 当前学生用户ID（须为组长）
     */
    public void submitFinal(Long userId);

    /**
     * 生成总稿PDF（阶段5）：封面页 + 目录页 + 10模块正文，中文字体渲染，
     * 仅本组已提交总稿后允许导出
     *
     * @param userId 当前学生用户ID
     * @return PDF 文件字节流（响应头与写出由控制器处理）
     */
    public byte[] exportFinalPdf(Long userId);
}
