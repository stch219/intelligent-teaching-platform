# -*- coding: utf-8 -*-
"""
============================================================================
【功能】智能教学平台接口回归测试脚本（阶段9）
----------------------------------------------------------------------------
【说明】
  1. 覆盖范围：三角色登录 / 越权反查 / 学生注册审批闭环 / 管理员端查询 /
     教师端(班级·分组·任务·模块·赋分·预警·批改·成绩·仪表盘) /
     学生端(信息·模块·贡献率·成绩·消息·预警·仪表盘) / 帮助中心
  2. 可复跑性：注册用例使用「99+时间戳」动态学号，建班用例用动态班名并在
     结束时删除，不污染固定测试数据，可反复执行
  3. 依赖：Python 3.8+、requests（pip install requests）
  4. 运行前提：后端 8080 已启动；若开启验证码，需能访问 redis-cli 读码
  5. 运行方式：python scripts/api_regression_test.py
     结束后输出每条用例 PASS/FAIL 与总通过率；全部通过退出码 0，否则 1
============================================================================
"""
import json
import subprocess
import sys
import time

import requests

# ------------------------- 基础配置 -------------------------
BASE_URL = "http://localhost:8080"          # 后端服务地址
TIMEOUT = 15                                 # 单请求超时（秒）

# 三个测试角色的账号密码（与 README 账号体系一致）
ADMIN = ("admin", "admin123")                # 超级管理员
TEACHER = ("t001", "123456")                 # 指导教师
STUDENT = ("20230101", "230101")             # A组组长（学号后6位为密码）

# redis-cli 候选路径（用于读取图形验证码；最后兜底走 PATH）
REDIS_CLI_CANDIDATES = [
    r"C:\Users\tjx\Downloads\Redis-7.4.8-Windows-x64-cygwin-with-Service"
    r"\Redis-7.4.8-Windows-x64-cygwin-with-Service\redis-cli.exe",
    "redis-cli",
]

# ------------------------- 结果收集 -------------------------
RESULTS = []  # 每项: (用例名, 是否通过, 详情)


def check(name, ok, detail=""):
    """【工具】记录一条用例结果并即时打印"""
    RESULTS.append((name, ok, detail))
    tag = "PASS" if ok else "FAIL"
    line = f"[{tag}] {name}"
    if not ok and detail:
        line += f"  -> {detail}"
    print(line)


def get_rows(body):
    """【工具】兼容两种返回结构：分页接口取 rows，非分页取 data（仅列表时）"""
    if not isinstance(body, dict):
        return []
    rows = body.get("rows")
    if rows is None and isinstance(body.get("data"), list):
        rows = body.get("data")
    return rows or []


def make_session(token=None):
    """【工具】创建独立会话；trust_env=False 防止系统代理劫持本机请求"""
    s = requests.Session()
    s.trust_env = False  # 问题12教训：本机联调必须绕开系统代理
    s.headers.update({"Content-Type": "application/json"})
    if token:
        s.headers.update({"Authorization": f"Bearer {token}"})
    return s


def req(s, method, path, expect_code=200, **kwargs):
    """
    【工具】统一请求封装
    返回 (是否满足期望code, 响应json或None, 失败详情)
    """
    try:
        r = s.request(method, BASE_URL + path, timeout=TIMEOUT, **kwargs)
    except Exception as e:  # 网络异常直接判失败
        return False, None, f"请求异常: {e}"
    try:
        body = r.json()
    except Exception:
        return False, None, f"非JSON响应 HTTP{r.status_code}: {r.text[:120]}"
    code = body.get("code")
    if expect_code == "non200":
        # 期望"非200"（越权/非法操作被拦截类用例）
        return code != 200, body, f"预期拦截但返回code={code}"
    return code == expect_code, body, f"code={code} msg={body.get('msg')}"


def read_captcha_from_redis(uuid):
    """【工具】用 redis-cli 读取验证码（值形如 "abcd"，需去引号）"""
    for cli in REDIS_CLI_CANDIDATES:
        try:
            out = subprocess.check_output(
                [cli, "GET", f"captcha_codes:{uuid}"], timeout=5
            )
            val = out.decode("utf-8", errors="ignore").strip().strip('"')
            if val:
                return val
        except Exception:
            continue
    return None


def login(username, password):
    """
    【工具】登录并返回 token
    兼容验证码开关：captchaEnabled=false 直接登录；否则走 redis 读码
    """
    s = make_session()
    ok, cap, _ = req(s, "GET", "/captchaImage")
    payload = {"username": username, "password": password}
    if ok and cap and cap.get("captchaEnabled"):
        code = read_captcha_from_redis(cap.get("uuid"))
        if not code:
            return None
        payload.update({"code": code, "uuid": cap.get("uuid")})
    ok, body, _ = req(s, "POST", "/login", json=payload)
    return body.get("token") if ok else None


# ============================================================================
# 一、认证与安全
# ============================================================================
def test_auth():
    """【用例组】三角色登录 + 未登录拦截 + 越权反查"""
    # 1. 管理员登录
    tk = login(*ADMIN)
    check("认证-管理员登录", bool(tk), "未取到token")
    globals()["TK_ADMIN"] = tk

    # 2. 教师登录
    tk = login(*TEACHER)
    check("认证-教师登录", bool(tk), "未取到token")
    globals()["TK_TEACHER"] = tk

    # 3. 学生登录
    tk = login(*STUDENT)
    check("认证-学生登录", bool(tk), "未取到token")
    globals()["TK_STUDENT"] = tk

    # 4. 未登录访问业务接口 -> 应 401 拦截
    s = make_session()
    ok, body, detail = req(s, "GET", "/teach/class/list", expect_code=401)
    check("安全-未登录访问业务接口被401拦截", ok, detail)

    # 5. 学生 token 调教师专属接口 -> 应被权限拦截（非200）
    s = make_session(TK_STUDENT)
    ok, body, detail = req(s, "GET", "/teach/class/list", expect_code="non200")
    check("安全-学生越权调教师接口被拦截", ok, detail)


# ============================================================================
# 二、学生注册审批闭环（动态学号，可复跑）
# ============================================================================
def test_registration():
    """【用例组】注册提交 -> 防重复 -> 审批通过 -> 新号登录；幽灵班级申请被拦截后驳回清理"""
    ts = str(int(time.time()))
    sno = "99" + ts[-8:]            # 动态学号，保证可复跑唯一
    pwd = sno[-6:]                  # 审批通过后初始密码=学号后6位
    s_admin = make_session(TK_ADMIN)

    # 6. 提交注册申请（匿名接口）
    s = make_session()
    payload = {
        "realName": f"回归测试{ts[-4:]}", "studentNo": sno,
        "className": "车辆2301测试班", "phone": "13800000000",
    }
    ok, body, detail = req(s, "POST", "/teach/register", json=payload)
    check("注册-提交申请成功", ok, detail)

    # 7. 同学号重复提交 -> 应被拦截
    ok, body, detail = req(s, "POST", "/teach/register", json=payload,
                           expect_code="non200")
    check("注册-同学号重复提交被拦截", ok, detail)

    # 8. 幽灵班级申请可提交，但审批时应被拦截（班级不存在自动拦截）
    ghost = {"realName": f"幽灵{ts[-4:]}", "studentNo": "98" + ts[-8:],
             "className": "不存在的班级XYZ", "phone": ""}
    ok, _, _ = req(s, "POST", "/teach/register", json=ghost)
    check("注册-幽灵班级申请提交（表单层放行）", ok, "提交本身应成功")

    # 9. 管理员查申请列表 -> 能找到本次申请（须用管理员会话，匿名会查不到）
    ok, body, detail = req(s_admin, "GET", "/teach/registration/list")
    rows = get_rows(body) if ok else []
    target = next((x for x in rows if x.get("studentNo") == sno), None)
    ghost_row = next((x for x in rows if x.get("studentNo") == ghost["studentNo"]), None)
    # 诊断信息：查询失败时输出实际响应概要，便于定位（正常时为空串）
    diag = "" if (ok and target) else (
        f"ok={ok} code={body.get('code') if isinstance(body, dict) else '?'} "
        f"total={body.get('total') if isinstance(body, dict) else '?'} rows={len(rows)}")
    check("管理-注册申请列表可查到新申请", bool(target),
          diag or f"rows={len(rows)} 未找到{sno}")

    # 10. 幽灵班级申请审批 -> 应失败（班级不存在）
    if ghost_row:
        ok, body, detail = req(s_admin, "PUT",
                               f"/teach/registration/approve/{ghost_row['id']}",
                               expect_code="non200")
        check("注册-幽灵班级审批被拦截", ok, detail)
        # 驳回清理，保持数据干净
        req(s_admin, "PUT", "/teach/registration/reject",
            json={"id": ghost_row["id"], "rejectReason": "回归测试自动驳回"})
    else:
        check("注册-幽灵班级审批被拦截", False, "未找到幽灵申请")

    # 11. 审批通过（事务：建账号+绑学生角色+回填状态）
    if target:
        ok, body, detail = req(s_admin, "PUT",
                               f"/teach/registration/approve/{target['id']}")
        check("管理-审批通过新建学生账号", ok, detail)

        # 12. 新账号用「学号后6位」登录验证
        tk_new = login(sno, pwd)
        check("注册-新账号按学号后6位可登录", bool(tk_new), "登录失败")
    else:
        check("管理-审批通过新建学生账号", False, "无待审批申请")
        check("注册-新账号按学号后6位可登录", False, "前置失败")


# ============================================================================
# 三、管理员端
# ============================================================================
def test_admin():
    """【用例组】账号体系与平台级查询"""
    s = make_session(TK_ADMIN)

    # 13. 教师列表
    ok, body, detail = req(s, "GET", "/teach/teacher/list")
    rows = body.get("rows") or [] if ok else []
    check("管理-教师列表查询", ok and len(rows) > 0, detail or "列表为空")

    # 14. 学生列表
    ok, body, detail = req(s, "GET", "/teach/student/list")
    check("管理-学生列表查询", ok, detail)

    # 15. 班级-小组树
    ok, body, detail = req(s, "GET", "/teach/student/classGroupTree")
    check("管理-班级小组树查询", ok, detail)

    # 16. 平台仪表盘（六卡+AI覆盖率）
    ok, body, detail = req(s, "GET", "/teach/dashboard/admin")
    check("管理-平台仪表盘数据", ok and body.get("data"), detail or "data为空")

    # 17. 帮助中心列表（admin 口径，可看下架条目）
    ok, body, detail = req(s, "GET", "/teach/help/list")
    check("帮助-管理员查询全部条目", ok, detail)


# ============================================================================
# 四、教师端（班级/分组/任务/模块/赋分/预警/批改/成绩/仪表盘）
# ============================================================================
def test_teacher():
    """【用例组】教师端核心接口，含建班删除闭环与预警扫描写操作"""
    s = make_session(TK_TEACHER)

    # 18. 班级列表（取第一个班级作为后续用例的 classId；非分页返回 data）
    ok, body, detail = req(s, "GET", "/teach/class/list")
    rows = get_rows(body) if ok else []
    class_id = rows[0].get("id") if rows else None
    check("教师-班级列表查询", ok and class_id, detail or "无班级")

    # 19. 建班闭环：创建动态名班级 -> 成功
    ts = str(int(time.time()))
    # 从教师列表取自己的 userId 作为指导教师
    ok_t, body_t, _ = req(make_session(TK_ADMIN), "GET", "/teach/teacher/list")
    trows = get_rows(body_t) if ok_t else []
    tid = next((x.get("userId") for x in trows if x.get("userName") == TEACHER[0]),
               None)
    new_class = {"className": f"回归测试班{ts[-6:]}", "teacherId": tid,
                 "groupCount": 4, "grade": "2026", "remark": "回归测试临时班级"}
    ok, body, detail = req(s, "POST", "/teach/class", json=new_class)
    check("教师-创建班级", ok, detail)

    # 20. 删除刚建的班级（无学生可删，闭环清理）
    ok2, body2, detail2 = req(s, "GET", "/teach/class/list")
    rows2 = get_rows(body2) if ok2 else []
    tmp = next((x for x in rows2 if x.get("className") == new_class["className"]),
               None)
    if tmp:
        ok, body, detail = req(s, "DELETE", f"/teach/class/{tmp['id']}")
        check("教师-删除临时班级（闭环清理）", ok, detail)
    else:
        check("教师-删除临时班级（闭环清理）", False, "未找到临时班级")

    # 21. 分组看板
    ok, body, detail = req(s, "GET", f"/teach/class/{class_id}/groupBoard")
    check("教师-分组看板", ok, detail)
    # 从看板取一个小组 id，供成绩查询用
    gid = None
    if ok and body.get("data"):
        groups = body["data"] if isinstance(body["data"], list) else []
        for g in groups:
            gid = g.get("id") or g.get("groupId")
            if gid:
                break

    # 22. 任务列表
    ok, body, detail = req(s, "GET", f"/teach/task/list?classId={class_id}")
    check("教师-任务列表", ok, detail)

    # 23. 模块设置（10大模块字数区间）
    ok, body, detail = req(s, "GET", f"/teach/moduleset/{class_id}")
    check("教师-模块设置查询", ok, detail)

    # 24. 模块赋分（总和须=100）
    ok, body, detail = req(s, "GET", f"/teach/scoreset/{class_id}")
    check("教师-模块赋分查询", ok, detail)

    # 25. 预警规则列表（每班上限3条）
    ok, body, detail = req(s, "GET", f"/teach/warningrule/list/{class_id}")
    rules = body.get("data") or [] if ok else []
    check("教师-预警规则列表", ok, detail)
    if ok:
        check("教师-预警规则不超过3条", len(rules) <= 3, f"实际{len(rules)}条")

    # 26. 一键扫描预警（写操作，幂等可复跑）
    ok, body, detail = req(s, "POST", f"/teach/warning/scan/{class_id}")
    check("教师-一键扫描预警", ok, detail)

    # 27. 预警记录查询
    ok, body, detail = req(s, "GET", f"/teach/warning/records/{class_id}")
    check("教师-预警记录查询", ok, detail)

    # 28. AI批改看板
    ok, body, detail = req(s, "GET", f"/teach/review/board/{class_id}")
    check("教师-AI批改看板", ok, detail)

    # 29. 小组成绩查询（A组）
    if gid:
        ok, body, detail = req(s, "GET", f"/teach/review/{gid}/scores")
        check("教师-小组成绩查询", ok, detail)
    else:
        check("教师-小组成绩查询", False, "groupBoard未返回小组id")

    # 30. 教学仪表盘（echarts两图数据）
    ok, body, detail = req(s, "GET", f"/teach/dashboard/teacher/{class_id}")
    check("教师-教学仪表盘数据", ok and body.get("data"), detail or "data为空")


# ============================================================================
# 五、学生端
# ============================================================================
def test_student():
    """【用例组】学生端门户全量只读接口 + 消息 + 帮助中心"""
    s = make_session(TK_STUDENT)

    # 31. 我的信息（含小组/角色）
    ok, body, detail = req(s, "GET", "/teach/student/portal/myInfo")
    check("学生-我的信息", ok and body.get("data"), detail or "data为空")

    # 32. 公示板（组员分工公示）
    ok, body, detail = req(s, "GET", "/teach/student/portal/board")
    check("学生-公示板", ok, detail)

    # 33. 十大模块列表
    ok, body, detail = req(s, "GET", "/teach/student/portal/modules")
    check("学生-模块列表", ok, detail)
    mods = body.get("data") or [] if ok else []
    mcode = None
    for m in mods:
        mcode = m.get("moduleCode") or m.get("code")
        if mcode:
            break

    # 34. 单模块详情
    if mcode:
        ok, body, detail = req(s, "GET", f"/teach/student/portal/module/{mcode}")
        check("学生-单模块详情", ok, detail)
    else:
        check("学生-单模块详情", False, "模块列表未返回code")

    # 35. 贡献率查询
    ok, body, detail = req(s, "GET", "/teach/student/portal/contribution")
    check("学生-贡献率查询", ok, detail)

    # 36. 我的成绩（教师已发布）
    ok, body, detail = req(s, "GET", "/teach/student/portal/myReview")
    check("学生-我的成绩", ok, detail)

    # 37. 我的预警记录
    ok, body, detail = req(s, "GET", "/teach/warning/my")
    check("学生-我的预警记录", ok, detail)

    # 38. 学生仪表盘（五卡数据）
    ok, body, detail = req(s, "GET", "/teach/dashboard/student")
    check("学生-学习仪表盘数据", ok and body.get("data"), detail or "data为空")

    # 39. 消息-可联系人列表
    ok, body, detail = req(s, "GET", "/teach/msg/contacts")
    check("学生-消息可联系人", ok, detail)

    # 40. 消息-未读总数
    ok, body, detail = req(s, "GET", "/teach/msg/unreadTotal")
    check("学生-未读消息数", ok, detail)

    # 41. 消息-好友列表
    ok, body, detail = req(s, "GET", "/teach/msg/buddies")
    check("学生-好友列表", ok, detail)

    # 42. 帮助中心（学生口径仅已发布）
    ok, body, detail = req(s, "GET", "/teach/help/list")
    check("帮助-学生查询已发布条目", ok, detail)


# ============================================================================
# 主流程
# ============================================================================
def main():
    print("=" * 62)
    print("智能教学平台 接口回归测试（阶段9）  目标:", BASE_URL)
    print("=" * 62)

    # 先探活，后端没起直接退出，避免刷一屏连接失败
    try:
        requests.get(BASE_URL + "/captchaImage", timeout=5,
                     proxies={"http": None, "https": None})
    except Exception as e:
        print(f"[中止] 后端 {BASE_URL} 不可达：{e}")
        print("请先启动后端：java -jar ruoyi-admin/target/ruoyi-admin.jar")
        sys.exit(2)

    test_auth()          # 一、认证与安全
    test_registration()  # 二、注册审批闭环
    test_admin()         # 三、管理员端
    test_teacher()       # 四、教师端
    test_student()       # 五、学生端

    # 汇总
    total = len(RESULTS)
    passed = sum(1 for _, ok, _ in RESULTS if ok)
    failed = total - passed
    rate = passed * 100.0 / total if total else 0.0
    print("=" * 62)
    print(f"总计 {total} 条  通过 {passed}  失败 {failed}  通过率 {rate:.1f}%")
    if failed:
        print("失败用例：")
        for name, ok, detail in RESULTS:
            if not ok:
                print(f"  - {name}: {detail}")
    print("=" * 62)
    sys.exit(0 if failed == 0 else 1)


if __name__ == "__main__":
    main()
