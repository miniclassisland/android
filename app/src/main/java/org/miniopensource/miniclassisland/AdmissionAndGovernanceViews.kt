package org.miniopensource.miniclassisland

import android.graphics.Color
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import org.json.JSONArray
import org.json.JSONObject

// ==================== 7. PAIRING & PRE-REGISTRATION ====================
fun MainActivity.loadPairingData() {
    runAsync(
        work = {
            val info = ApiClient.request(ApiClient.managementUrl, "/account/$currentAccountId/info")
            val list = ApiClient.request(ApiClient.managementUrl, "/account/$currentAccountId/pairing/list")
            Pair(info, list)
        },
        done = { (info, list) ->
            if (info.ok) accountInfoObj = info.json
            pairingList = list.json.optJSONArray("requests") ?: JSONArray()
            rebuildUi()
        }
    )
}

fun MainActivity.buildPairingView(): View {
    val box = UiKit.vBox(this)
    box.addView(UiKit.pageHeader(this, "配对码管理", "审批客户端发起的 6 位数字配对准入请求与配置注册策略") {
        addView(UiKit.button(this@buildPairingView, "刷新", UiKit.BTN_MUTED, UiKit.TEXT_PRIMARY, true) {
            loadPairingData()
        })
    })

    val modeCard = UiKit.card(this, 16)
    modeCard.addView(UiKit.label(this, "客户端接入注册策略", 14f, UiKit.TEXT_PRIMARY, true))
    modeCard.addView(UiKit.spacer(this, 6))
    val curMode = accountInfoObj.optString("registration_mode", "open")
    val modeOpts = listOf(
        "open" to "开放自动注册",
        "pairing" to "6位配对码审批",
        "pre_reg" to "仅限预注册白名单"
    )
    modeCard.addView(UiKit.chipRow(this, modeOpts, curMode) { mKey ->
        val body = JSONObject().apply {
            put("name", accountInfoObj.optString("name"))
            put("slug", accountInfoObj.optString("slug"))
            put("registration_mode", mKey)
        }
        runAsync(
            work = { ApiClient.request(ApiClient.managementUrl, "/account/$currentAccountId/info", "PUT", body) },
            done = { loadPairingData() }
        )
    })
    box.addView(modeCard)

    val c = UiKit.card(this, 16)
    c.addView(UiKit.label(this, "配对请求列表 (${pairingList.length()})", 14f, UiKit.TEXT_PRIMARY, true))
    if (pairingList.length() == 0) {
        c.addView(UiKit.label(this, "当前没有待处理的配对请求", 12f, UiKit.TEXT_TERTIARY).apply {
            setPadding(0, UiKit.dp(this@buildPairingView, 12), 0, UiKit.dp(this@buildPairingView, 12))
        })
    }
    for (i in 0 until pairingList.length()) {
        val req = pairingList.optJSONObject(i) ?: continue
        val item = UiKit.subCard(this, 12)
        val h = UiKit.hBox(this)
        val id = req.optString("id")
        val code = req.optString("pairing_code").ifEmpty { req.optString("code") }
        val st = req.optString("status")
        h.addView(UiKit.label(this, code, 18f, UiKit.ACCENT_BLUE, true).apply {
            setPadding(0, 0, UiKit.dp(this@buildPairingView, 10), 0)
        })
        h.addView(UiKit.label(this, "标识: ${req.optString("client_id")}", 13f, UiKit.TEXT_PRIMARY).apply {
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        })
        h.addView(UiKit.badge(this, st, if (st == "approved") "success" else if (st == "pending") "warning" else "danger"))
        if (st == "pending") {
            h.addView(UiKit.button(this, "批准", UiKit.ACCENT_BLUE, Color.WHITE, true) {
                runAsync(
                    work = { ApiClient.request(ApiClient.managementUrl, "/account/$currentAccountId/pairing/$id/approve", "POST", JSONObject()) },
                    done = { loadPairingData() }
                )
            })
            h.addView(UiKit.button(this, "拒绝", UiKit.BADGE_DANGER_BG, UiKit.BADGE_DANGER_FG, true) {
                runAsync(
                    work = { ApiClient.request(ApiClient.managementUrl, "/account/$currentAccountId/pairing/$id/reject", "POST", JSONObject()) },
                    done = { loadPairingData() }
                )
            })
        }
        item.addView(h)
        c.addView(item)
    }
    box.addView(c)
    return box
}

fun MainActivity.loadPreRegData() {
    runAsync(
        work = { ApiClient.request(ApiClient.managementUrl, "/account/$currentAccountId/pre-registration/list") },
        done = { r ->
            preRegList = r.json.optJSONArray("items") ?: JSONArray()
            rebuildUi()
        }
    )
}

fun MainActivity.buildPreRegView(): View {
    val box = UiKit.vBox(this)
    box.addView(UiKit.pageHeader(this, "预注册客户端", "预先登记班级终端白名单并生成专属 ManagementPreset.json"))

    val createCard = UiKit.card(this, 16)
    createCard.addView(UiKit.label(this, "添加预注册客户端", 14f, UiKit.TEXT_PRIMARY, true))
    val labelEd = UiKit.input(this, "备注名称")
    val cidEd = UiKit.input(this, "班级标识")
    val macEd = UiKit.input(this, "MAC 地址（可选）")
    createCard.addView(labelEd)
    createCard.addView(cidEd)
    createCard.addView(macEd)
    createCard.addView(UiKit.iconTextButton(this, MdsIcon.ADD, "添加预注册客户端", UiKit.ACCENT_BLUE, Color.WHITE, false) {
        val body = JSONObject().apply {
            put("label", labelEd.text.toString().trim())
            put("class_identity", cidEd.text.toString().trim())
            put("mac_address", macEd.text.toString().trim())
        }
        runAsync(
            work = { ApiClient.request(ApiClient.managementUrl, "/account/$currentAccountId/pre-registration/create", "POST", body) },
            done = { loadPreRegData() }
        )
    })
    box.addView(createCard)

    val listCard = UiKit.card(this, 16)
    listCard.addView(UiKit.label(this, "已预注册列表 (${preRegList.length()})", 14f, UiKit.TEXT_PRIMARY, true))
    for (i in 0 until preRegList.length()) {
        val it = preRegList.optJSONObject(i) ?: continue
        val itemId = it.optString("id")
        val row = UiKit.subCard(this, 10)
        val h = UiKit.hBox(this)
        h.addView(UiKit.label(
            this,
            "${it.optString("label")} (${it.optString("class_identity")}) · MAC: ${it.optString("mac_address").ifEmpty { "-" }}",
            13f,
            UiKit.TEXT_PRIMARY
        ).apply {
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        })
        h.addView(UiKit.button(this, "下载专属配置", UiKit.BTN_MUTED, UiKit.ACCENT_BLUE, true) {
            runAsync(
                work = { ApiClient.request(ApiClient.managementUrl, "/account/$currentAccountId/pre-registration/$itemId/preset.json") },
                done = { r ->
                    exportedPresetJson = r.rawText
                    rebuildUi()
                }
            )
        })
        h.addView(UiKit.button(this, "删除", UiKit.BADGE_DANGER_BG, UiKit.BADGE_DANGER_FG, true) {
            runAsync(
                work = { ApiClient.request(ApiClient.managementUrl, "/account/$currentAccountId/pre-registration/$itemId", "DELETE") },
                done = { loadPreRegData() }
            )
        })
        row.addView(h)
        listCard.addView(row)
    }
    if (exportedPresetJson.isNotEmpty()) {
        listCard.addView(UiKit.input(this, "专属 ManagementPreset.json", exportedPresetJson, multiLine = true))
    }
    box.addView(listCard)
    return box
}

// ==================== 8. ACCOUNT, ACCESS, INVITATIONS, SECURITY ====================
fun MainActivity.loadAccountAndQuotasData() {
    runAsync(
        work = {
            val info = ApiClient.request(ApiClient.managementUrl, "/account/$currentAccountId/info")
            val q = ApiClient.request(ApiClient.managementUrl, "/account/$currentAccountId/quota")
            Pair(info, q)
        },
        done = { (info, q) ->
            if (info.ok) accountInfoObj = info.json
            quotasList = q.json.optJSONArray("quotas") ?: JSONArray()
            rebuildUi()
        }
    )
}

fun MainActivity.buildAccountSettingsView(): View {
    val box = UiKit.vBox(this)
    box.addView(UiKit.pageHeader(this, "账户概览", "管理集控账户基本信息、API 密钥与资源配额使用量"))

    val c = UiKit.card(this, 16)
    c.addView(UiKit.label(this, "基本信息", 14f, UiKit.TEXT_PRIMARY, true))
    c.addView(UiKit.label(this, "账户名称", 12f, UiKit.TEXT_SECONDARY, true))
    val nameEd = UiKit.input(this, "账户名称", accountInfoObj.optString("name", currentAccountName))
    c.addView(nameEd)
    c.addView(UiKit.label(this, "唯一标识", 12f, UiKit.TEXT_SECONDARY, true))
    val slugEd = UiKit.input(this, "唯一标识", accountInfoObj.optString("slug"))
    c.addView(slugEd)

    val r = UiKit.hBox(this)
    r.addView(UiKit.button(this, "保存更改", UiKit.ACCENT_BLUE, Color.WHITE, false) {
        val b = JSONObject().put("name", nameEd.text.toString()).put("slug", slugEd.text.toString())
        runAsync(
            work = { ApiClient.request(ApiClient.managementUrl, "/account/$currentAccountId/info", "POST", b) },
            done = {
                currentAccountName = nameEd.text.toString()
                loadAccountAndQuotasData()
            }
        )
    })
    r.addView(UiKit.button(this, "重新生成密钥", UiKit.BTN_MUTED, UiKit.TEXT_PRIMARY, false) {
        runAsync(
            work = { ApiClient.request(ApiClient.managementUrl, "/account/$currentAccountId/info/regenerate-api-key", "POST", JSONObject()) },
            done = { loadAccountAndQuotasData() }
        )
    })
    c.addView(r)
    c.addView(UiKit.divider(this, 12))
    c.addView(UiKit.label(this, "接口密钥: ${accountInfoObj.optString("api_key", "-")}", 12f, UiKit.TEXT_SECONDARY))
    box.addView(c)

    val qCard = UiKit.card(this, 16)
    qCard.addView(UiKit.label(this, "账户配额使用情况", 14f, UiKit.TEXT_PRIMARY, true))
    for (i in 0 until quotasList.length()) {
        val q = quotasList.optJSONObject(i) ?: continue
        val qr = UiKit.subCard(this, 10)
        qr.addView(UiKit.label(
            this,
            "${q.optString("quota_type").ifEmpty { q.optString("quota_key") }}: ${q.optInt("current_value")} / ${q.optInt("limit_value", q.optInt("max_value"))}",
            13f,
            UiKit.TEXT_PRIMARY,
            true
        ))
        qCard.addView(qr)
    }
    box.addView(qCard)
    return box
}

fun MainActivity.loadMembersData() {
    runAsync(
        work = { ApiClient.request(ApiClient.managementUrl, "/account/$currentAccountId/access/list") },
        done = { r ->
            membersList = r.json.optJSONArray("members") ?: JSONArray()
            rebuildUi()
        }
    )
}

fun MainActivity.buildAccessMembersView(): View {
    val box = UiKit.vBox(this)
    box.addView(UiKit.pageHeader(this, "访问控制", "管理当前集控账户的协作成员与角色权限"))

    val c = UiKit.card(this, 16)
    c.addView(UiKit.label(this, "账户成员列表 (${membersList.length()})", 14f, UiKit.TEXT_PRIMARY, true))
    for (i in 0 until membersList.length()) {
        val m = membersList.optJSONObject(i) ?: continue
        val uid = m.optString("user_id")
        val curRole = m.optString("role_in_account").ifEmpty { m.optString("role") }
        val item = UiKit.subCard(this, 12)
        val top = UiKit.hBox(this)
        top.addView(UiKit.label(
            this,
            "${m.optString("display_name")} (@${m.optString("username")})",
            13f,
            UiKit.TEXT_PRIMARY,
            true
        ).apply {
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        })
        top.addView(UiKit.badge(this, curRole, "info"))
        item.addView(top)
        item.addView(UiKit.spacer(this, 6))

        val h = UiKit.hBox(this)
        for (rn in listOf("admin", "member", "viewer")) {
            h.addView(UiKit.button(
                this,
                rn,
                if (curRole == rn) UiKit.ACCENT_BLUE else UiKit.BTN_MUTED,
                if (curRole == rn) Color.WHITE else UiKit.TEXT_PRIMARY,
                true
            ) {
                runAsync(
                    work = { ApiClient.request(ApiClient.managementUrl, "/account/$currentAccountId/access/$uid", "POST", JSONObject().put("role_in_account", rn)) },
                    done = { loadMembersData() }
                )
            })
        }
        h.addView(UiKit.button(this, "移除成员", UiKit.BADGE_DANGER_BG, UiKit.BADGE_DANGER_FG, true) {
            runAsync(
                work = { ApiClient.request(ApiClient.managementUrl, "/account/$currentAccountId/access/$uid", "DELETE") },
                done = { loadMembersData() }
            )
        })
        item.addView(h)
        c.addView(item)
    }
    box.addView(c)
    return box
}

fun MainActivity.loadInvitationsData() {
    runAsync(
        work = { ApiClient.request(ApiClient.managementUrl, "/account/$currentAccountId/invitation/list") },
        done = { r ->
            invitationsList = r.json.optJSONArray("invitations") ?: JSONArray()
            rebuildUi()
        }
    )
}

fun MainActivity.buildInvitationsView(): View {
    val box = UiKit.vBox(this)
    box.addView(UiKit.pageHeader(this, "邀请管理", "创建成员邀请码或输入邀请码加入其他集控账户") {
        addView(UiKit.iconTextButton(this@buildInvitationsView, MdsIcon.ADD, "创建邀请码", UiKit.ACCENT_BLUE, Color.WHITE, true) {
            val b = JSONObject().put("role_in_account", "member").put("max_uses", 10)
            runAsync(
                work = { ApiClient.request(ApiClient.managementUrl, "/account/$currentAccountId/invitation/create", "POST", b) },
                done = { loadInvitationsData() }
            )
        })
    })

    val joinCard = UiKit.card(this, 16)
    joinCard.addView(UiKit.label(this, "使用邀请码加入其他账户", 14f, UiKit.TEXT_PRIMARY, true))
    val joinRow = UiKit.hBox(this)
    val joinEd = UiKit.input(this, "输入邀请码").apply {
        layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply {
            rightMargin = UiKit.dp(this@buildInvitationsView, 8)
        }
    }
    joinRow.addView(joinEd)
    joinRow.addView(UiKit.button(this, "加入账户", UiKit.ACCENT_BLUE, Color.WHITE, true) {
        val code = joinEd.text.toString().trim()
        if (code.isNotEmpty()) {
            runAsync(
                work = { ApiClient.request(ApiClient.managementUrl, "/account/invitation/accept", "POST", JSONObject().put("code", code)) },
                done = { r ->
                    setFeedback(if (r.ok) "已成功加入集控账户" else r.error, !r.ok)
                    loadAccountsListOnly()
                }
            )
        }
    })
    joinCard.addView(joinRow)
    box.addView(joinCard)

    val c = UiKit.card(this, 16)
    c.addView(UiKit.label(this, "当前账户有效邀请码 (${invitationsList.length()})", 14f, UiKit.TEXT_PRIMARY, true))
    for (i in 0 until invitationsList.length()) {
        val inv = invitationsList.optJSONObject(i) ?: continue
        val invId = inv.optString("id")
        val row = UiKit.subCard(this, 10)
        val h = UiKit.hBox(this)
        h.addView(UiKit.label(
            this,
            "${inv.optString("code")} · 角色: ${inv.optString("role_in_account", inv.optString("role"))} · 已用: ${inv.optInt("used_count")}/${inv.optInt("max_uses")}",
            13f,
            UiKit.TEXT_PRIMARY,
            true
        ).apply {
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        })
        h.addView(UiKit.button(this, "撤销", UiKit.BADGE_DANGER_BG, UiKit.BADGE_DANGER_FG, true) {
            runAsync(
                work = { ApiClient.request(ApiClient.managementUrl, "/account/$currentAccountId/invitation/$invId", "DELETE") },
                done = { loadInvitationsData() }
            )
        })
        row.addView(h)
        c.addView(row)
    }
    box.addView(c)
    return box
}

fun MainActivity.loadProfileData() {
    runAsync(
        work = { ApiClient.request(ApiClient.managementUrl, "/user/info") },
        done = { r ->
            if (r.ok) currentUser = r.json
            rebuildUi()
        }
    )
}

fun MainActivity.buildSecurityView(): View {
    val box = UiKit.vBox(this)
    box.addView(UiKit.pageHeader(this, "安全设置", "管理您的个人资料、登录安全证书、登录密码与两步验证"))

    // 1. 本机登录安全证书状态与轮换卡片
    val certCard = UiKit.card(this, 16)
    val certHead = UiKit.hBox(this)
    certHead.addView(UiKit.label(this, "本机移动端登录安全证书", 14f, UiKit.TEXT_PRIMARY, true).apply {
        layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
    })
    val cert = ApiClient.storedCertificate
    certHead.addView(
        UiKit.badge(
            this,
            if (cert != null && !cert.isExpired()) "第 ${cert.generation}/${cert.maxGeneration} 代 · 生效中" else "未绑定",
            if (cert != null && !cert.isExpired()) "success" else "warning"
        )
    )
    certCard.addView(certHead)
    certCard.addView(UiKit.spacer(this, 6))

    if (cert != null && !cert.isExpired()) {
        val remSec = cert.remainingSeconds()
        val remHours = (remSec / 3600L).coerceAtLeast(0L)
        val remMins = ((remSec % 3600L) / 60L).coerceAtLeast(0L)
        certCard.addView(UiKit.label(
            this,
            "证书标识: ${cert.certId}\n签发时间: ${formatTs(cert.issuedAt)} · 到期时间: ${formatTs(cert.expiresAt)}\n剩余有效期: ${remHours} 小时 ${remMins} 分钟（低于 24 小时可自动或手动轮换下一代，最多 12 代）",
            12f,
            UiKit.TEXT_SECONDARY
        ))
        val cBtnRow = UiKit.hBox(this).apply {
            setPadding(0, UiKit.dp(this@buildSecurityView, 8), 0, 0)
        }
        cBtnRow.addView(UiKit.button(this, "申请 1 小时新会话令牌", UiKit.ACCENT_BLUE, Color.WHITE, true) {
            loginWithStoredCertificate()
        })
        if (cert.canRotate()) {
            cBtnRow.addView(UiKit.button(this, "轮换下一代安全证书", UiKit.ACCENT_GREEN, Color.WHITE, true) {
                rotateStoredCertificateNow()
            })
        }
        cBtnRow.addView(UiKit.button(this, "解除本机证书绑定", UiKit.BADGE_DANGER_BG, UiKit.BADGE_DANGER_FG, true) {
            ApiClient.storedCertificate = null
            saveLocalAuthState()
            setFeedback("已解除本机登录安全证书绑定")
        })
        certCard.addView(cBtnRow)
    } else {
        certCard.addView(UiKit.label(
            this,
            "当前设备尚未绑定登录安全证书。请在网页端「个人资料与安全设置 -> 移动端登录安全证书」验证密码（及两步验证）后，通过扫码唤醒本应用或在此粘贴 5 分钟兑换链接绑定。",
            12f,
            UiKit.TEXT_SECONDARY
        ))
        val bindRow = UiKit.hBox(this).apply {
            setPadding(0, UiKit.dp(this@buildSecurityView, 6), 0, 0)
        }
        val bindEd = UiKit.input(this, "miniclassisland://exchange-certificate/{Code} 或兑换码", certExchangeInput).apply {
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply {
                rightMargin = UiKit.dp(this@buildSecurityView, 8)
            }
        }
        bindRow.addView(bindEd)
        bindRow.addView(UiKit.button(this, "兑换并绑定", UiKit.ACCENT_BLUE, Color.WHITE, true) {
            val raw = bindEd.text.toString().trim()
            if (raw.isNotEmpty()) {
                certExchangeInput = raw
                redeemCertificateCodeAndLogin(raw)
            } else {
                setFeedback("请输入唤醒链接或单次兑换码", true)
            }
        })
        certCard.addView(bindRow)
    }
    box.addView(certCard)

    // 1b. 反向扫码授权登录网页端卡片
    val qrWebCard = UiKit.card(this, 16)
    val qrHead = UiKit.hBox(this)
    qrHead.addView(OfficialIcons.imageView(this, MdsIcon.QR_CODE, UiKit.ACCENT_BLUE, 20).apply {
        (layoutParams as LinearLayout.LayoutParams).rightMargin = UiKit.dp(this@buildSecurityView, 8)
    })
    qrHead.addView(UiKit.label(this, "反向扫码登录网页端", 14f, UiKit.TEXT_PRIMARY, true).apply {
        layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
    })
    qrWebCard.addView(qrHead)
    qrWebCard.addView(UiKit.spacer(this, 6))
    qrWebCard.addView(UiKit.label(
        this,
        "在电脑浏览器打开登录页并点击「移动端扫码登录」，使用手机系统相机扫描网页上的二维码即可唤醒本应用确认登录。",
        12f,
        UiKit.TEXT_SECONDARY
    ))
    val qrRow = UiKit.hBox(this).apply {
        setPadding(0, UiKit.dp(this@buildSecurityView, 6), 0, 0)
    }
    val qrInput = UiKit.input(this, "miniclassisland://qr-login/{Code} 或网页登录码", qrWebLoginInput).apply {
        layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply {
            rightMargin = UiKit.dp(this@buildSecurityView, 8)
        }
    }
    qrRow.addView(qrInput)
    qrRow.addView(UiKit.iconTextButton(this, MdsIcon.QR_CODE, "确认授权网页登录", UiKit.ACCENT_BLUE, Color.WHITE, true) {
        val raw = qrInput.text.toString().trim()
        if (raw.isNotEmpty()) {
            qrWebLoginInput = raw
            authorizeWebQrLoginFromApp(raw)
        } else {
            setFeedback("请输入或扫描网页端登录二维码链接", true)
        }
    })
    qrWebCard.addView(qrRow)
    box.addView(qrWebCard)

    // 2. 个人资料卡片
    val profileCard = UiKit.card(this, 16)
    profileCard.addView(UiKit.label(this, "个人资料", 14f, UiKit.TEXT_PRIMARY, true))
    profileCard.addView(UiKit.label(this, "显示昵称", 12f, UiKit.TEXT_SECONDARY, true))
    val dnEd = UiKit.input(this, "显示昵称", currentUser.optString("display_name"))
    profileCard.addView(dnEd)
    profileCard.addView(UiKit.label(this, "用户名", 12f, UiKit.TEXT_SECONDARY, true))
    val unEd = UiKit.input(this, "用户名", currentUser.optString("username"))
    profileCard.addView(unEd)
    profileCard.addView(UiKit.button(this, "保存个人资料", UiKit.ACCENT_BLUE, Color.WHITE, false) {
        val newDn = dnEd.text.toString().trim()
        val newUn = unEd.text.toString().trim()
        runAsync(
            work = {
                var lastRes = HttpRes(true, 200)
                if (newDn.isNotEmpty() && newDn != currentUser.optString("display_name")) {
                    lastRes = ApiClient.request(ApiClient.managementUrl, "/user/info/display-name", "POST", JSONObject().put("display_name", newDn))
                }
                if (lastRes.ok && newUn.isNotEmpty() && newUn != currentUser.optString("username")) {
                    lastRes = ApiClient.request(ApiClient.managementUrl, "/user/info/username", "POST", JSONObject().put("username", newUn))
                }
                lastRes
            },
            done = { r ->
                setFeedback(if (r.ok) "个人资料已更新" else r.error, !r.ok)
                if (r.ok) loadProfileData()
            }
        )
    })
    box.addView(profileCard)

    // 3. 修改密码卡片
    val pwdCard = UiKit.card(this, 16)
    pwdCard.addView(UiKit.label(this, "修改登录密码", 14f, UiKit.TEXT_PRIMARY, true))
    val oldPwd = UiKit.input(this, "当前密码", isPassword = true)
    val newPwd = UiKit.input(this, "新密码（至少 12 个字符）", isPassword = true)
    pwdCard.addView(oldPwd)
    pwdCard.addView(newPwd)
    pwdCard.addView(UiKit.button(this, "更新密码", UiKit.ACCENT_BLUE, Color.WHITE, false) {
        val b = JSONObject().put("old_password", oldPwd.text.toString()).put("new_password", newPwd.text.toString())
        runAsync(
            work = { ApiClient.request(ApiClient.managementUrl, "/user/info/password/change", "POST", b) },
            done = { r -> setFeedback(if (r.ok) "密码已修改" else r.error, !r.ok) }
        )
    })
    box.addView(pwdCard)

    // 4. 两步验证卡片
    val totpCard = UiKit.card(this, 16)
    val totpOn = currentUser.optBoolean("totp_enabled", false)
    val tRow = UiKit.hBox(this)
    tRow.addView(UiKit.label(this, "两步验证", 14f, UiKit.TEXT_PRIMARY, true).apply {
        layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
    })
    tRow.addView(UiKit.badge(this, if (totpOn) "已开启" else "未开启", if (totpOn) "success" else "neutral"))
    totpCard.addView(tRow)
    totpCard.addView(UiKit.spacer(this, 8))

    if (!totpOn) {
        totpCard.addView(UiKit.button(this, "开启两步验证", UiKit.ACCENT_BLUE, Color.WHITE, false) {
            runAsync(
                work = { ApiClient.request(ApiClient.managementUrl, "/user/2fa/totp/enable", "POST", JSONObject()) },
                done = { r ->
                    if (r.ok) {
                        totpEnableSecret = r.json.optString("secret")
                        totpEnableUri = r.json.optString("qr_uri").ifEmpty { r.json.optString("uri") }
                        totpRecoveryCodesText = (r.json.optJSONArray("recovery_codes") ?: JSONArray()).toString()
                        rebuildUi()
                    }
                }
            )
        })
        if (totpEnableSecret.isNotEmpty()) {
            totpCard.addView(UiKit.label(this, "密钥: $totpEnableSecret\n验证链接: $totpEnableUri", 12f, UiKit.TEXT_SECONDARY))
            if (totpRecoveryCodesText.isNotEmpty()) {
                totpCard.addView(UiKit.label(this, "恢复码: $totpRecoveryCodesText", 12f, UiKit.ACCENT_GREEN, true))
            }
            val codeEd = UiKit.input(this, "输入验证器 6 位动态码确认绑定")
            totpCard.addView(codeEd)
            totpCard.addView(UiKit.button(this, "确认开启两步验证", UiKit.ACCENT_BLUE, Color.WHITE, true) {
                runAsync(
                    work = { ApiClient.request(ApiClient.managementUrl, "/user/2fa/totp/confirm", "POST", JSONObject().put("code", codeEd.text.toString().trim())) },
                    done = { r ->
                        if (r.ok) {
                            loadProfileData()
                        } else {
                            setFeedback(r.error, true)
                        }
                    }
                )
            })
        }
    }
    box.addView(totpCard)
    return box
}

// ==================== 9. SUPERADMIN SHELL VIEWS ====================
fun MainActivity.loadGlobalAccountsData() {
    runAsync(
        work = {
            val st = ApiClient.request(ApiClient.adminUrl, "/stats")
            val accs = ApiClient.request(ApiClient.adminUrl, "/account?limit=100")
            Pair(st, accs)
        },
        done = { (st, accs) ->
            if (st.ok) globalStatsObj = st.json
            globalAccountsList = accs.json.optJSONArray("accounts") ?: JSONArray()
            rebuildUi()
        }
    )
}

fun MainActivity.buildSuperAdminDashboardView(): View {
    val box = UiKit.vBox(this)
    box.addView(UiKit.pageHeader(this, "全局仪表盘", "MINI ClassIsland 超级管理平台全局运行指标") {
        addView(UiKit.button(this@buildSuperAdminDashboardView, "刷新", UiKit.BTN_MUTED, UiKit.TEXT_PRIMARY, true) {
            loadGlobalAccountsData()
        })
    })

    val row1 = UiKit.hBox(this)
    row1.addView(UiKit.statCard(this, "集控账户总数", "${globalStatsObj.optInt("total_accounts")}", UiKit.TEXT_PRIMARY))
    row1.addView(UiKit.statCard(this, "注册用户总数", "${globalStatsObj.optInt("total_users")}", UiKit.ACCENT_BLUE))
    box.addView(row1)

    val row2 = UiKit.hBox(this)
    row2.addView(UiKit.statCard(this, "全局终端总数", "${globalStatsObj.optInt("total_clients")}", UiKit.TEXT_PRIMARY))
    row2.addView(UiKit.statCard(this, "当前在线终端", "${globalStatsObj.optInt("online_clients")}", UiKit.ACCENT_GREEN))
    box.addView(row2)

    box.addView(buildGlobalAccountsCardContent())
    return box
}

fun MainActivity.buildGlobalAccountsView(): View {
    val box = UiKit.vBox(this)
    box.addView(UiKit.pageHeader(this, "账户与配额", "管理全平台集控账户，支持一键进入指定集控账户进行接管编辑") {
        addView(UiKit.button(this@buildGlobalAccountsView, "刷新", UiKit.BTN_MUTED, UiKit.TEXT_PRIMARY, true) {
            loadGlobalAccountsData()
        })
    })
    box.addView(buildGlobalAccountsCardContent())
    return box
}

fun MainActivity.buildGlobalAccountsCardContent(): View {
    val c = UiKit.card(this, 16)
    c.addView(UiKit.label(this, "全平台集控账户列表 (${globalAccountsList.length()})", 14f, UiKit.TEXT_PRIMARY, true))
    for (i in 0 until globalAccountsList.length()) {
        val acc = globalAccountsList.optJSONObject(i) ?: continue
        val accId = acc.optString("id")
        val accName = acc.optString("name")
        val row = UiKit.subCard(this, 12)
        val h = UiKit.hBox(this)
        h.addView(UiKit.label(
            this,
            "$accName (${acc.optString("slug")})",
            13f,
            UiKit.TEXT_PRIMARY,
            true
        ).apply {
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        })
        h.addView(UiKit.button(this, "进入集控编辑", UiKit.ACCENT_BLUE, Color.WHITE, true) {
            currentAccountId = accId
            currentAccountName = accName
            isSuperAdminMode = false
            isSuperAdminTakeover = true
            navigateToSection("dashboard")
        })
        h.addView(UiKit.button(this, "删除", UiKit.BADGE_DANGER_BG, UiKit.BADGE_DANGER_FG, true) {
            runAsync(
                work = { ApiClient.request(ApiClient.adminUrl, "/account/$accId", "DELETE") },
                done = { loadGlobalAccountsData() }
            )
        })
        row.addView(h)
        c.addView(row)
    }
    return c
}

fun MainActivity.loadGlobalUsersData() {
    runAsync(
        work = { ApiClient.request(ApiClient.adminUrl, "/user/list?limit=100") },
        done = { r ->
            globalUsersList = r.json.optJSONArray("users") ?: JSONArray()
            rebuildUi()
        }
    )
}

fun MainActivity.buildGlobalUsersView(): View {
    val box = UiKit.vBox(this)
    box.addView(UiKit.pageHeader(this, "用户管理", "管理全平台注册用户状态与重置两步验证"))

    val c = UiKit.card(this, 16)
    c.addView(UiKit.label(this, "用户列表 (${globalUsersList.length()})", 14f, UiKit.TEXT_PRIMARY, true))
    for (i in 0 until globalUsersList.length()) {
        val u = globalUsersList.optJSONObject(i) ?: continue
        val uid = u.optString("id")
        val active = u.optBoolean("is_active", true)
        val item = UiKit.subCard(this, 12)
        val h = UiKit.hBox(this)
        h.addView(UiKit.label(
            this,
            "${u.optString("username")} (${u.optString("email")})",
            13f,
            UiKit.TEXT_PRIMARY,
            true
        ).apply {
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        })
        h.addView(UiKit.badge(this, u.optString("role_code", u.optString("role", "normal")), "info"))
        h.addView(UiKit.button(
            this,
            if (active) "禁用" else "启用",
            if (active) UiKit.BADGE_DANGER_BG else UiKit.BADGE_SUCCESS_BG,
            if (active) UiKit.BADGE_DANGER_FG else UiKit.BADGE_SUCCESS_FG,
            true
        ) {
            runAsync(
                work = { ApiClient.request(ApiClient.adminUrl, "/user/$uid", "POST", JSONObject().put("is_active", !active)) },
                done = { loadGlobalUsersData() }
            )
        })
        if (u.optBoolean("totp_enabled", false)) {
            h.addView(UiKit.button(this, "重置两步验证", UiKit.BTN_MUTED, UiKit.TEXT_PRIMARY, true) {
                runAsync(
                    work = { ApiClient.request(ApiClient.adminUrl, "/user/$uid", "POST", JSONObject().put("totp_enabled", false)) },
                    done = { loadGlobalUsersData() }
                )
            })
        }
        item.addView(h)
        c.addView(item)
    }
    box.addView(c)
    return box
}

fun MainActivity.loadPendingUsersData() {
    runAsync(
        work = { ApiClient.request(ApiClient.adminUrl, "/user/pending/list?limit=100") },
        done = { r ->
            pendingUsersList = r.json.optJSONArray("users") ?: JSONArray()
            rebuildUi()
        }
    )
}

fun MainActivity.buildPendingUsersView(): View {
    val box = UiKit.vBox(this)
    box.addView(UiKit.pageHeader(this, "注册审核", "审批待处理的平台新用户注册申请"))

    val c = UiKit.card(this, 16)
    c.addView(UiKit.label(this, "待审批列表 (${pendingUsersList.length()})", 14f, UiKit.TEXT_PRIMARY, true))
    if (pendingUsersList.length() == 0) {
        c.addView(UiKit.label(this, "当前没有待审核的注册申请", 12f, UiKit.TEXT_TERTIARY).apply {
            setPadding(0, UiKit.dp(this@buildPendingUsersView, 12), 0, UiKit.dp(this@buildPendingUsersView, 12))
        })
    }
    for (i in 0 until pendingUsersList.length()) {
        val u = pendingUsersList.optJSONObject(i) ?: continue
        val uid = u.optString("id")
        val item = UiKit.subCard(this, 12)
        val h = UiKit.hBox(this)
        h.addView(UiKit.label(this, "${u.optString("username")} (${u.optString("email")})", 13f, UiKit.TEXT_PRIMARY, true).apply {
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        })
        h.addView(UiKit.button(this, "批准", UiKit.ACCENT_BLUE, Color.WHITE, true) {
            runAsync(
                work = { ApiClient.request(ApiClient.adminUrl, "/user/pending/approve/$uid", "POST", JSONObject()) },
                done = { loadPendingUsersData() }
            )
        })
        h.addView(UiKit.button(this, "拒绝", UiKit.BADGE_DANGER_BG, UiKit.BADGE_DANGER_FG, true) {
            runAsync(
                work = { ApiClient.request(ApiClient.adminUrl, "/user/pending/reject/$uid", "POST", JSONObject()) },
                done = { loadPendingUsersData() }
            )
        })
        item.addView(h)
        c.addView(item)
    }
    box.addView(c)
    return box
}

fun MainActivity.loadSystemSettingsData() {
    runAsync(
        work = { ApiClient.request(ApiClient.adminUrl, "/settings") },
        done = { r ->
            sysSettingsObj = r.json.optJSONObject("settings") ?: r.json
            rebuildUi()
        }
    )
}

fun MainActivity.buildSystemSettingsView(): View {
    val box = UiKit.vBox(this)
    box.addView(UiKit.pageHeader(this, "系统设置", "配置全局开放注册策略、账户数量限制与系统公告"))

    val c = UiKit.card(this, 16)
    var regOpen = sysSettingsObj.optString("registration_open", "true") != "false"
    var reqApproval = sysSettingsObj.optString("require_approval", "false") == "true"

    c.addView(UiKit.switchRow(this, "开放新用户注册", "允许新用户自行注册账号", regOpen) {
        regOpen = it
    })
    c.addView(UiKit.switchRow(this, "注册需超级管理员审核", "新注册账号需审核通过后方可登录", reqApproval) {
        reqApproval = it
    })
    c.addView(UiKit.spacer(this, 8))
    c.addView(UiKit.label(this, "单用户最大可创建集控账户数", 12f, UiKit.TEXT_SECONDARY, true))
    val maxAccEd = UiKit.input(this, "默认 5", sysSettingsObj.optString("max_accounts_per_user", "5"))
    c.addView(maxAccEd)
    c.addView(UiKit.label(this, "系统全局公告", 12f, UiKit.TEXT_SECONDARY, true))
    val motdEd = UiKit.input(this, "输入系统公告", sysSettingsObj.optString("motd", ""))
    c.addView(motdEd)
    c.addView(UiKit.spacer(this, 8))
    c.addView(UiKit.button(this, "保存系统设置", UiKit.ACCENT_BLUE, Color.WHITE, false) {
        val s = JSONObject().apply {
            put("registration_open", if (regOpen) "true" else "false")
            put("require_approval", if (reqApproval) "true" else "false")
            put("max_accounts_per_user", maxAccEd.text.toString())
            put("motd", motdEd.text.toString())
        }
        runAsync(
            work = { ApiClient.request(ApiClient.adminUrl, "/settings", "POST", JSONObject().put("items", s)) },
            done = { r -> setFeedback(if (r.ok) "系统设置已保存" else r.error, !r.ok) }
        )
    })
    box.addView(c)
    return box
}
