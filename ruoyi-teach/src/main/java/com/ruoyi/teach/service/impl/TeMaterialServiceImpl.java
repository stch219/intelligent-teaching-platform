package com.ruoyi.teach.service.impl;

import java.util.Date;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.teach.domain.TeMaterial;
import com.ruoyi.teach.mapper.TeMaterialMapper;
import com.ruoyi.teach.service.ITeMaterialService;

/**
 * ============================================================================
 * 【功能】学习资料业务实现
 * ----------------------------------------------------------------------------
 * 【规则】
 *   1. 文件本体由若依通用上传接口 /common/upload 落盘，本模块仅记录元数据
 *   2. 资料四类：1任务指导书 2说明书模板 3参考答案 4辅助资料
 *   3. 参考答案（类型3）严格保密：visible 强制为 0，任何操作都无法对学生开放
 * ============================================================================
 */
@Service
public class TeMaterialServiceImpl implements ITeMaterialService
{
    /** 参考答案资料类型值 */
    private static final long TYPE_ANSWER = 3;

    @Autowired
    private TeMaterialMapper materialMapper;

    /**
     * 查询资料列表
     */
    @Override
    public List<TeMaterial> selectTeMaterialList(TeMaterial teMaterial)
    {
        return materialMapper.selectTeMaterialList(teMaterial);
    }

    /**
     * 新增资料记录
     */
    @Override
    public int insertTeMaterial(TeMaterial teMaterial, String operateBy)
    {
        // 必填校验（文件由前端先经 /common/upload 上传后回传路径）
        if (teMaterial.getClassId() == null || StringUtils.isEmpty(teMaterial.getFileName())
                || StringUtils.isEmpty(teMaterial.getFilePath()))
        {
            throw new ServiceException("班级、资料名称与文件为必填项");
        }
        // 参考答案严格保密：强制学生不可见
        if (teMaterial.getMaterialType() != null && teMaterial.getMaterialType() == TYPE_ANSWER)
        {
            teMaterial.setVisible(0L);
        }
        else if (teMaterial.getVisible() == null)
        {
            teMaterial.setVisible(1L);
        }
        teMaterial.setUploadBy(operateBy);
        teMaterial.setUploadTime(new Date());
        return materialMapper.insertTeMaterial(teMaterial);
    }

    /**
     * 修改资料（名称/可见性）
     */
    @Override
    public int updateTeMaterial(TeMaterial teMaterial)
    {
        TeMaterial exist = materialMapper.selectTeMaterialById(teMaterial.getId());
        if (exist == null)
        {
            throw new ServiceException("资料不存在");
        }
        // 参考答案类型无论怎么改都保持不可见
        if (exist.getMaterialType() != null && exist.getMaterialType() == TYPE_ANSWER)
        {
            teMaterial.setVisible(0L);
        }
        return materialMapper.updateTeMaterial(teMaterial);
    }

    /**
     * 批量删除资料记录
     */
    @Override
    public int deleteTeMaterialByIds(Long[] ids)
    {
        return materialMapper.deleteTeMaterialByIds(ids);
    }
}
