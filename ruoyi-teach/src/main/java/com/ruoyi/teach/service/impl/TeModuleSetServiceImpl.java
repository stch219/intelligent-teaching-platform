package com.ruoyi.teach.service.impl;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.teach.domain.TeModuleSet;
import com.ruoyi.teach.mapper.TeModuleSetMapper;
import com.ruoyi.teach.service.ITeModuleSetService;

/**
 * ============================================================================
 * 【功能】模块设置业务实现
 * ----------------------------------------------------------------------------
 * 【规则】
 *   1. 模块顺序全平台统一（1-10）：封面 任务要求 角色与分工 参数配置
 *      知识背景 计算步骤 代码实现 计算结果与分析 心得体会 参考资料
 *   2. 默认配置：封面/任务要求/参数配置为只读（editable=0）；
 *      参考资料按条目数校验（默认3~20条），其余按字数校验（默认不限）
 *   3. 保存为整体覆盖式 upsert（唯一键 class_id+module_code）；
 *      模块名称由后端按编号统一回填，防止前端篡改
 * ============================================================================
 */
@Service
public class TeModuleSetServiceImpl implements ITeModuleSetService
{
    /** 模块编号 → 模块名称（全平台统一，顺序固定） */
    private static final String[] MODULE_NAMES = {
            "封面", "任务要求", "角色与分工", "参数配置", "知识背景",
            "计算步骤", "代码实现", "计算结果与分析", "心得体会", "参考资料"
    };

    /** 默认只读模块编号（封面自动生成；任务要求/参数配置仅展示） */
    private static final long[] READONLY_MODULES = { 1, 2, 4 };

    /** 参考资料模块编号（按条目数校验） */
    private static final long REFERENCE_MODULE = 10;

    @Autowired
    private TeModuleSetMapper moduleSetMapper;

    /**
     * 查询班级模块设置；无记录时返回平台默认配置（不落库，保存时才写入）
     */
    @Override
    public List<TeModuleSet> selectByClassId(Long classId)
    {
        List<TeModuleSet> list = moduleSetMapper.selectByClassId(classId);
        if (list == null || list.isEmpty())
        {
            return buildDefaults(classId);
        }
        return list;
    }

    /**
     * 批量保存模块设置（事务，整体校验）
     */
    @Transactional
    @Override
    public int saveBatch(Long classId, List<TeModuleSet> list, String operateBy)
    {
        if (list == null || list.size() != MODULE_NAMES.length)
        {
            throw new ServiceException("必须完整提交 10 个模块的设置");
        }
        int rows = 0;
        Date now = new Date();
        for (TeModuleSet item : list)
        {
            long code = item.getModuleCode() == null ? 0 : item.getModuleCode();
            if (code < 1 || code > MODULE_NAMES.length)
            {
                throw new ServiceException("模块编号必须在 1-10 之间");
            }
            // 区间校验：min <= max（max=0 表示不限）
            long min = item.getMinCount() == null ? 0 : item.getMinCount();
            long max = item.getMaxCount() == null ? 0 : item.getMaxCount();
            if (max > 0 && min > max)
            {
                throw new ServiceException("模块「" + MODULE_NAMES[(int) code - 1] + "」最低要求不能大于最高要求");
            }
            // 名称按编号统一回填，防止前端篡改；设置默认值兜底
            item.setClassId(classId);
            item.setModuleName(MODULE_NAMES[(int) code - 1]);
            if (item.getCountType() == null) item.setCountType(1L);
            if (item.getNeedFormula() == null) item.setNeedFormula(0L);
            if (item.getNeedImage() == null) item.setNeedImage(0L);
            if (item.getNeedCode() == null) item.setNeedCode(0L);
            if (item.getEditable() == null) item.setEditable(1L);
            item.setCreateTime(now);
            item.setUpdateTime(now);
            rows += moduleSetMapper.upsert(item);
        }
        return rows;
    }

    /**
     * 构建平台默认模块配置（10条）
     */
    private List<TeModuleSet> buildDefaults(Long classId)
    {
        List<TeModuleSet> list = new ArrayList<>();
        for (int i = 1; i <= MODULE_NAMES.length; i++)
        {
            TeModuleSet m = new TeModuleSet();
            m.setClassId(classId);
            m.setModuleCode((long) i);
            m.setModuleName(MODULE_NAMES[i - 1]);
            // 参考资料：按条目数校验，默认 3~20 条（规范要求参考文献3-20条）
            if (i == REFERENCE_MODULE)
            {
                m.setCountType(2L);
                m.setMinCount(3L);
                m.setMaxCount(20L);
            }
            else
            {
                m.setCountType(1L);
                m.setMinCount(0L);
                m.setMaxCount(0L);
            }
            // 只读模块：封面/任务要求/参数配置
            boolean readonly = false;
            for (long r : READONLY_MODULES)
            {
                if (r == i)
                {
                    readonly = true;
                    break;
                }
            }
            m.setEditable(readonly ? 0L : 1L);
            m.setNeedFormula(0L);
            m.setNeedImage(0L);
            m.setNeedCode(0L);
            list.add(m);
        }
        return list;
    }
}
