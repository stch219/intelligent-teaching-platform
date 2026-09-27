package com.ruoyi.web.controller.teach;

import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.teach.domain.TeWarningRecord;
import com.ruoyi.teach.service.ITeWarningScanService;
import com.ruoyi.web.core.websocket.WsSessionPool;

/**
 * ============================================================================
 * 【功能】三级预警控制器（阶段8）
 * ----------------------------------------------------------------------------
 * 【说明】教师触发「立即扫描」：Service 落库预警记录并返回触发明细，
 *         本控制器遍历明细向组内在线学生推送 WebSocket 实时提醒
 *         （报文 type=warning，与 notice/chat 同栈复用阶段6长连接）；
 *         学生端通过 /my 查询本组预警记录。
 * ============================================================================
 */
@RestController
@RequestMapping("/teach/warning")
public class TeWarningScanController extends BaseController
{
    @Autowired
    private ITeWarningScanService scanService;

    /**
     * 教师触发班级扫描：判定+落库+实时推送在线组员
     */
    @PreAuthorize("@ss.hasRole('teacher')")
    @PostMapping("/scan/{classId}")
    public AjaxResult scan(@PathVariable Long classId)
    {
        // 1. 执行扫描（判定+幂等落库，返回触发明细）
        Map<String, Object> result = scanService.scanClass(classId, getUserId());

        // 2. 遍历触发明细，向每组在线学生推送实时提醒
        int hit = 0;
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> triggered = (List<Map<String, Object>>) result.get("triggered");
        for (Map<String, Object> item : triggered)
        {
            String push = WsSessionPool.buildJson("warning",
                    "level", item.get("level"),
                    "content", item.get("content"),
                    "progress", item.get("progress"));
            @SuppressWarnings("unchecked")
            List<Long> memberIds = (List<Long>) item.get("memberIds");
            for (Long uid : memberIds)
            {
                if (WsSessionPool.isOnline(uid))
                {
                    WsSessionPool.pushToUser(uid, push);
                    hit++;
                }
            }
        }

        // 3. 返回扫描统计与推送结果
        result.put("pushCount", hit);
        return success(result);
    }

    /**
     * 教师查询班级预警记录列表
     */
    @PreAuthorize("@ss.hasRole('teacher')")
    @GetMapping("/records/{classId}")
    public AjaxResult records(@PathVariable Long classId)
    {
        return success(scanService.listRecords(classId, getUserId()));
    }

    /**
     * 教师标记预警记录已处置/忽略
     */
    @PreAuthorize("@ss.hasRole('teacher')")
    @PutMapping("/resolve/{id}")
    public AjaxResult resolve(@PathVariable Long id)
    {
        return toAjax(scanService.resolve(id, getUserId()));
    }

    /**
     * 学生查询本组预警记录
     */
    @PreAuthorize("@ss.hasRole('student')")
    @GetMapping("/my")
    public AjaxResult my()
    {
        return success(scanService.myWarnings(getUserId()));
    }
}
