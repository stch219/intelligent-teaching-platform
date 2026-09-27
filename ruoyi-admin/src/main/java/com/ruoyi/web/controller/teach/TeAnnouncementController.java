package com.ruoyi.web.controller.teach;

import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.utils.DateUtils;
import com.ruoyi.common.utils.spring.SpringUtils;
import com.ruoyi.teach.domain.TeAnnouncement;
import com.ruoyi.teach.domain.TeStudent;
import com.ruoyi.teach.mapper.TeStudentMapper;
import com.ruoyi.teach.service.ITeAnnouncementService;
import com.ruoyi.web.core.websocket.WsSessionPool;

/**
 * ============================================================================
 * 【功能】班级公告控制器（阶段6消息系统）
 * ----------------------------------------------------------------------------
 * 【说明】教师面向自己指导的班级发布公告；学生查看本班公告。
 *         发布成功后，除落库外还向该班所有在线学生推送 WebSocket
 *         实时提醒（离线学生下次进入公告页照常可查）。
 * ============================================================================
 */
@RestController
@RequestMapping("/teach/notice")
public class TeAnnouncementController extends BaseController
{
    @Autowired
    private ITeAnnouncementService noticeService;

    /**
     * 公告列表（学生=本班公告；教师=我指导的全部班级公告）
     */
    @PreAuthorize("@ss.hasAnyRoles('student,teacher')")
    @GetMapping("/list")
    public AjaxResult list()
    {
        return success(noticeService.list(getUserId()));
    }

    /**
     * 教师发布公告（班级归属校验在 Service 内；发布后 WebSocket 推送在线学生）
     */
    @PreAuthorize("@ss.hasRole('teacher')")
    @PostMapping
    public AjaxResult publish(@RequestBody Map<String, Object> body)
    {
        Long classId = body.get("classId") == null ? null : Long.valueOf(body.get("classId").toString());
        String title = body.get("title") == null ? "" : body.get("title").toString();
        String content = body.get("content") == null ? "" : body.get("content").toString();
        // 1. 落库（返回的公告对象已含发布人姓名与发布时间）
        TeAnnouncement ann = noticeService.publish(getUserId(), title, content, classId);
        // 2. 组装实时提醒报文
        String push = WsSessionPool.buildJson("notice",
                "title", ann.getTitle(),
                "content", ann.getContent(),
                "publishBy", ann.getPublishBy(),
                "classId", ann.getClassId());
        // 3. 查该班全部学生，向其中在线者推送（离线者靠未读/列表兜底）
        TeStudent query = new TeStudent();
        query.setClassId(classId);
        List<TeStudent> students = SpringUtils.getBean(TeStudentMapper.class).selectTeStudentList(query);
        int hit = 0;
        for (TeStudent st : students)
        {
            if (WsSessionPool.isOnline(st.getUserId()))
            {
                WsSessionPool.pushToUser(st.getUserId(), push);
                hit++;
            }
        }
        // 4. 返回推送结果（在线提醒数仅用于接口自测观察）
        return success("发布成功，实时提醒在线学生 " + hit + " 人");
    }
}
