package com.ruoyi.teach.domain;

import java.util.Date;
import java.io.Serializable;
import com.fasterxml.jackson.annotation.JsonFormat;

/**
 * ============================================================================
 * 【功能】学习资料实体（对应表 te_material）
 * ----------------------------------------------------------------------------
 * 【说明】教师按班级发布学习资料，共四类：
 *         1任务指导书 2说明书模板 3参考答案 4辅助资料。
 *         参考答案类资料严格保密（visible 强制为 0，学生端永不可见），
 *         其余类型教师可控制学生是否可见。
 *         className 为联查展示字段（不落库）。
 * ============================================================================
 */
public class TeMaterial implements Serializable
{
    private static final long serialVersionUID = 1L;

    /** 资料ID（主键） */
    private Long id;

    /** 所属班级ID（关联 te_class.id） */
    private Long classId;

    /** 资料类型（1任务指导书 2说明书模板 3参考答案 4辅助资料） */
    private Long materialType;

    /** 资料显示名称 */
    private String fileName;

    /** 文件存储路径（若依 /common/upload 返回的相对路径） */
    private String filePath;

    /** 文件大小（字节） */
    private Long fileSize;

    /** 学生是否可见（参考答案强制0严格保密） */
    private Long visible;

    /** 上传教师 */
    private String uploadBy;

    /** 上传时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date uploadTime;

    /** 班级名称（联查 te_class.class_name，不落库） */
    private String className;

    public Long getId()
    {
        return id;
    }

    public void setId(Long id)
    {
        this.id = id;
    }

    public Long getClassId()
    {
        return classId;
    }

    public void setClassId(Long classId)
    {
        this.classId = classId;
    }

    public Long getMaterialType()
    {
        return materialType;
    }

    public void setMaterialType(Long materialType)
    {
        this.materialType = materialType;
    }

    public String getFileName()
    {
        return fileName;
    }

    public void setFileName(String fileName)
    {
        this.fileName = fileName;
    }

    public String getFilePath()
    {
        return filePath;
    }

    public void setFilePath(String filePath)
    {
        this.filePath = filePath;
    }

    public Long getFileSize()
    {
        return fileSize;
    }

    public void setFileSize(Long fileSize)
    {
        this.fileSize = fileSize;
    }

    public Long getVisible()
    {
        return visible;
    }

    public void setVisible(Long visible)
    {
        this.visible = visible;
    }

    public String getUploadBy()
    {
        return uploadBy;
    }

    public void setUploadBy(String uploadBy)
    {
        this.uploadBy = uploadBy;
    }

    public Date getUploadTime()
    {
        return uploadTime;
    }

    public void setUploadTime(Date uploadTime)
    {
        this.uploadTime = uploadTime;
    }

    public String getClassName()
    {
        return className;
    }

    public void setClassName(String className)
    {
        this.className = className;
    }
}
