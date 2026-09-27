package com.ruoyi.teach.service.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.system.service.ISysUserService;
import com.ruoyi.teach.domain.TeChat;
import com.ruoyi.teach.domain.TeChatMember;
import com.ruoyi.teach.domain.TeClass;
import com.ruoyi.teach.domain.TeMessage;
import com.ruoyi.teach.domain.TeStudent;
import com.ruoyi.teach.mapper.TeChatMapper;
import com.ruoyi.teach.mapper.TeChatMemberMapper;
import com.ruoyi.teach.mapper.TeClassMapper;
import com.ruoyi.teach.mapper.TeMessageMapper;
import com.ruoyi.teach.mapper.TeStudentMapper;
import com.ruoyi.teach.service.ITeMessageService;

/**
 * ============================================================================
 * 【功能】消息系统 Service 实现（阶段6）
 * ----------------------------------------------------------------------------
 * 【说明】核心规则：
 *         - 会话懒创建：单聊=两用户首聊时建（chat_type=1）；
 *           群聊=小组首用消息功能时建（chat_type=2，成员=组员+指导教师）
 *         - 成员校验：历史/已读/发送均要求当前用户是会话成员，
 *           学生只能联系指导教师与本组成员，教师只能联系本班学生
 *         - 已读回执：会话内消息ID > 我的 last_read_msg_id 且非本人发送
 *           计为未读；进入会话（history）即把回执推进到最新消息
 *         - 发送：文本非空且≤2000字；落库后刷新会话最后消息时间
 * ============================================================================
 */
@Service
public class TeMessageServiceImpl implements ITeMessageService
{
    /** 历史消息单次最大返回条数 */
    private static final int HISTORY_LIMIT = 200;

    @Autowired
    private TeChatMapper chatMapper;

    @Autowired
    private TeChatMemberMapper chatMemberMapper;

    @Autowired
    private TeMessageMapper messageMapper;

    @Autowired
    private TeStudentMapper studentMapper;

    @Autowired
    private TeClassMapper classMapper;

    @Autowired
    private ISysUserService userService;

    // ------------------------------------------------------------------
    // ① 会话列表 / 历史消息 / 已读回执
    // ------------------------------------------------------------------

    /**
     * 我的会话列表（展示名：单聊=对方昵称，群聊=组名；含未读数与最后一条预览）
     */
    @Override
    public List<Map<String, Object>> contacts(Long userId)
    {
        return chatMapper.selectMyChatList(userId);
    }

    /**
     * 历史消息：校验成员 → 取最新limit条并反转为时间正序 → 顺带推进已读回执
     */
    @Override
    public Map<String, Object> history(Long userId, Long chatId, int limit)
    {
        checkMember(chatId, userId);
        int realLimit = (limit <= 0 || limit > HISTORY_LIMIT) ? HISTORY_LIMIT : limit;
        List<TeMessage> messages = messageMapper.selectByChatId(chatId, realLimit);
        Collections.reverse(messages); // 倒序取出的最新200条反转为正序展示
        markRead(userId, chatId);

        Map<String, Object> result = new HashMap<>();
        result.put("chatId", chatId);
        result.put("messages", messages);
        return result;
    }

    /**
     * 标记会话已读：回执推进到该会话最新消息ID
     */
    @Override
    public void markRead(Long userId, Long chatId)
    {
        checkMember(chatId, userId);
        chatMemberMapper.updateLastReadToLatest(chatId, userId);
    }

    /**
     * 我的未读总数（导航角标）
     */
    @Override
    public int unreadTotal(Long userId)
    {
        return chatMemberMapper.countMyUnread(userId);
    }

    // ------------------------------------------------------------------
    // ② 联系人 / 发起单聊
    // ------------------------------------------------------------------

    /**
     * 可联系人：学生=指导教师(type 1)+本组成员(type 2)；教师=本班学生去重(type 3)
     */
    @Override
    public List<Map<String, Object>> buddies(Long userId)
    {
        List<Map<String, Object>> buddies = new ArrayList<>();
        TeStudent student = studentMapper.selectTeStudentByUserId(userId);
        if (student != null)
        {
            // 学生：指导教师
            if (student.getClassId() != null)
            {
                TeClass teClass = classMapper.selectTeClassById(student.getClassId());
                if (teClass != null && teClass.getTeacherId() != null)
                {
                    Map<String, Object> t = new HashMap<>();
                    t.put("userId", teClass.getTeacherId());
                    t.put("nickName", userService.selectUserById(teClass.getTeacherId()).getNickName());
                    t.put("type", 1L); // 1指导教师
                    buddies.add(t);
                }
            }
            // 学生：本组成员（排除自己）
            if (student.getGroupId() != null)
            {
                TeStudent q = new TeStudent();
                q.setGroupId(student.getGroupId());
                for (TeStudent mate : studentMapper.selectTeStudentList(q))
                {
                    if (mate.getUserId().equals(userId))
                    {
                        continue;
                    }
                    Map<String, Object> m = new HashMap<>();
                    m.put("userId", mate.getUserId());
                    m.put("nickName", mate.getNickName());
                    m.put("type", 2L); // 2组内成员
                    m.put("groupName", mate.getGroupName());
                    buddies.add(m);
                }
            }
            return buddies;
        }

        // 教师：本班学生（多班合并去重）
        TeClass cq = new TeClass();
        cq.setTeacherId(userId);
        Set<Long> seen = new HashSet<>();
        for (TeClass teClass : classMapper.selectTeClassList(cq))
        {
            TeStudent sq = new TeStudent();
            sq.setClassId(teClass.getId());
            for (TeStudent s : studentMapper.selectTeStudentList(sq))
            {
                if (!seen.add(s.getUserId()))
                {
                    continue;
                }
                Map<String, Object> m = new HashMap<>();
                m.put("userId", s.getUserId());
                m.put("nickName", s.getNickName());
                m.put("type", 3L); // 3班级学生
                m.put("className", teClass.getClassName());
                m.put("studentNo", s.getStudentNo());
                buddies.add(m);
            }
        }
        return buddies;
    }

    /**
     * 发起/进入单聊：目标必须在可联系人范围内；无会话则懒创建（chat+2成员）
     */
    @Override
    @Transactional
    public Long startSingle(Long userId, Long targetUserId)
    {
        if (targetUserId == null || targetUserId.equals(userId))
        {
            throw new ServiceException("目标联系人不合法");
        }
        // 目标必须在我的联系人列表内（防止越权与任何人建会话）
        boolean allowed = false;
        for (Map<String, Object> b : buddies(userId))
        {
            if (targetUserId.equals(((Number) b.get("userId")).longValue()))
            {
                allowed = true;
                break;
            }
        }
        if (!allowed)
        {
            throw new ServiceException("只能与指导教师、组内成员（或本班学生）发起会话");
        }
        // 已有单聊直接返回
        TeChat exist = chatMapper.selectSingleChat(userId, targetUserId);
        if (exist != null)
        {
            return exist.getId();
        }
        // 懒创建：会话 + 双方成员记录
        Date now = new Date();
        TeChat chat = new TeChat();
        chat.setChatType(1L);
        chat.setChatName("");
        chat.setCreateTime(now);
        chatMapper.insertTeChat(chat);

        TeChatMember me = new TeChatMember();
        me.setChatId(chat.getId());
        me.setUserId(userId);
        me.setJoinTime(now);
        TeChatMember other = new TeChatMember();
        other.setChatId(chat.getId());
        other.setUserId(targetUserId);
        other.setJoinTime(now);
        List<TeChatMember> members = new ArrayList<>();
        members.add(me);
        members.add(other);
        chatMemberMapper.insertMembers(members);
        return chat.getId();
    }

    // ------------------------------------------------------------------
    // ③ 发送消息（WebSocket 服务端调用）
    // ------------------------------------------------------------------

    /**
     * 发送消息：成员校验 → 非空/长度校验 → 落库（回填ID）→ 刷新会话时间
     */
    @Override
    @Transactional
    public TeMessage send(Long userId, Long chatId, String content)
    {
        checkMember(chatId, userId);
        if (StringUtils.isEmpty(content) || content.trim().isEmpty())
        {
            throw new ServiceException("消息内容不能为空");
        }
        if (content.length() > 2000)
        {
            throw new ServiceException("消息内容不能超过2000字");
        }
        TeMessage msg = new TeMessage();
        msg.setChatId(chatId);
        msg.setSenderId(userId);
        msg.setMsgType(1L); // 1文本
        msg.setContent(content.trim());
        msg.setSendTime(new Date());
        messageMapper.insertTeMessage(msg);
        // 回填发送人姓名（推送气泡展示用）
        msg.setSenderName(userService.selectUserById(userId).getNickName());
        chatMapper.updateChatTime(chatId);
        return msg;
    }

    // ------------------------------------------------------------------
    // 内部工具
    // ------------------------------------------------------------------

    /**
     * 会话成员校验：非成员抛出异常（防越权读写他人会话）
     */
    private void checkMember(Long chatId, Long userId)
    {
        TeChatMember member = chatMemberMapper.selectByChatAndUser(chatId, userId);
        if (member == null)
        {
            throw new ServiceException("你不是该会话成员，无权操作");
        }
    }
}
