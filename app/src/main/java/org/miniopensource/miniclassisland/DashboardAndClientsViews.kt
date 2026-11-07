package org.miniopensource.miniclassisland

import android.graphics.Color
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.LinearLayout
import org.json.JSONArray
import org.json.JSONObject

// ==================== 3. WEB DASHBOARD VIEW ====================
fun MainActivity.refreshDashboardData() {
    if (currentAccountId.isEmpty()) {
        rebuildUi()
        return
    }
    runAsync(
        work = {
            val cRes = ApiClient.request(ApiClient.managementUrl, "/account/$currentAccountId/client/list")
            val pRes = ApiClient.request(ApiClient.managementUrl, "/account/$currentAccountId/pairing/list?status=pending")
            val aRes = ApiClient.request(ApiClient.managementUrl, "/account/$currentAccountId/audit/list?limit=200")
            var resTotal = 0
            val types = listOf("Subjects", "TimeLayout", "ClassPlan", "Policy", "DefaultSettings", "Components", "Credentials")
            for (t in types) {
                resTotal += ApiClient.listResourceFiles(currentAccountId, t).size
            }
            Triple(cRes, pRes, Pair(aRes, resTotal))
        },
        done = { (cRes, pRes, tail) ->
            val (aRes, resTotal) = tail
            val cArr = cRes.optJsonArray()
            clientsArray = cArr
            dashTotalClients = cArr.length()
            var on = 0
            for (i in 0 until cArr.length()) {
                if (cArr.optJSONObject(i)?.optString("status") == "online") on++
            }
            dashOnline = on
            val pArr = pRes.optJsonArray()
            var pendingCnt = 0
            for (i in 0 until pArr.length()) {
                val pItem = pArr.optJSONObject(i) ?: continue
                val st = pItem.optString("status")
                if (st == "pending" || (st.isEmpty() && !pItem.optBoolean("approved", false))) {
                    pendingCnt++
                }
            }
            dashPendingPairing = pendingCnt
            dashRecentAudits = aRes.optJsonArray().length()
            dashTotalResources = resTotal
            rebuildUi()
        }
    )
}

fun MainActivity.buildDashboardView(): View {
    val box = UiKit.vBox(this)

    if (isSuperAdminTakeover) {
        val banner = UiKit.card(this, 12).apply {
            background = UiKit.roundedBg(UiKit.BADGE_WARNING_BG, 8, this@buildDashboardView, UiKit.BADGE_WARNING_FG, 1)
        }
        val r = UiKit.hBox(this)
        r.addView(UiKit.label(
            this,
            "超级管理员接管模式 · 当前正在管理账户 $currentAccountName",
            12f,
            UiKit.BADGE_WARNING_FG,
            true
        ).apply {
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        })
        r.addView(UiKit.button(this, "返回超级管理平台", UiKit.ACCENT_BLUE, Color.WHITE, true) {
            isSuperAdminTakeover = false
            isSuperAdminMode = true
            navigateToSection("admin_accounts")
        })
        banner.addView(r)
        box.addView(banner)
    }

    // Web PageHeader (.page-header)
    box.addView(UiKit.pageHeader(this, "仪表盘", "当前集控账户运行概览与快捷操作") {
        addView(UiKit.button(this@buildDashboardView, "下载通用配置", UiKit.ACCENT_BLUE, Color.WHITE, true) {
            runAsync(
                work = { ApiClient.request(ApiClient.managementUrl, "/account/$currentAccountId/ManagementPreset.json") },
                done = { r ->
                    if (r.ok) {
                        exportedPresetJson = r.rawText
                        rebuildUi()
                    } else {
                        setFeedback(r.error, true)
                    }
                }
            )
        })
        addView(UiKit.button(this@buildDashboardView, "刷新", UiKit.BTN_MUTED, UiKit.TEXT_PRIMARY, true) {
            refreshDashboardData()
        })
    })

    // 4 Stat Cards matching web/app/(dashboard)/dashboard/page.tsx
    if (isWideTabletLayout()) {
        val row = UiKit.hBox(this)
        row.addView(UiKit.statCard(this, "已注册客户端", "$dashTotalClients", UiKit.TEXT_PRIMARY).apply {
            setOnClickListener { navigateToSection("clients") }
        })
        row.addView(UiKit.statCard(this, "当前在线", "$dashOnline", UiKit.ACCENT_GREEN).apply {
            setOnClickListener { navigateToSection("clients") }
        })
        row.addView(UiKit.statCard(this, "档案资源数", "$dashTotalResources", UiKit.ACCENT_BLUE).apply {
            setOnClickListener { navigateToSection("res_ClassPlan") }
        })
        row.addView(UiKit.statCard(this, "待审批配对", "$dashPendingPairing", UiKit.TEXT_PRIMARY).apply {
            setOnClickListener { navigateToSection("pairing") }
        })
        box.addView(row)
    } else {
        val row1 = UiKit.hBox(this)
        row1.addView(UiKit.statCard(this, "已注册客户端", "$dashTotalClients", UiKit.TEXT_PRIMARY).apply {
            setOnClickListener { navigateToSection("clients") }
        })
        row1.addView(UiKit.statCard(this, "当前在线", "$dashOnline", UiKit.ACCENT_GREEN).apply {
            setOnClickListener { navigateToSection("clients") }
        })
        box.addView(row1)

        val row2 = UiKit.hBox(this)
        row2.addView(UiKit.statCard(this, "档案资源数", "$dashTotalResources", UiKit.ACCENT_BLUE).apply {
            setOnClickListener { navigateToSection("res_ClassPlan") }
        })
        row2.addView(UiKit.statCard(this, "待审批配对", "$dashPendingPairing", UiKit.TEXT_PRIMARY).apply {
            setOnClickListener { navigateToSection("pairing") }
        })
        box.addView(row2)
    }

    if (exportedPresetJson.isNotEmpty()) {
        val presetCard = UiKit.card(this, 16)
        val ph = UiKit.hBox(this)
        ph.addView(UiKit.label(this, "ManagementPreset.json 配置文件内容", 14f, UiKit.TEXT_PRIMARY, true).apply {
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        })
        ph.addView(UiKit.button(this, "关闭预览", UiKit.BTN_MUTED, UiKit.TEXT_SECONDARY, true) {
            exportedPresetJson = ""
            rebuildUi()
        })
        presetCard.addView(ph)
        presetCard.addView(UiKit.input(this, "ManagementPreset.json", exportedPresetJson, multiLine = true))
        box.addView(presetCard)
    }

    // Web Workflow Guide Card ("MINI ClassIsland 集控工作流指引")
    val guideCard = UiKit.card(this, 18)
    guideCard.addView(UiKit.label(this, "MINI ClassIsland 集控工作流指引", 15f, UiKit.TEXT_PRIMARY, true))
    guideCard.addView(UiKit.label(this, "按照以下 3 个步骤即可完成班级终端接入与课表统一编排下发：", 12f, UiKit.TEXT_SECONDARY).apply {
        setPadding(0, UiKit.dp(this@buildDashboardView, 4), 0, UiKit.dp(this@buildDashboardView, 10))
    })

    val steps = listOf(
        Triple("1. 编排档案资源", "在左侧「档案管理」中配置科目管理、时间表管理、课表管理及策略控制。", "res_Subjects" to "前往科目管理"),
        Triple("2. 终端接入与配对", "下载 ManagementPreset.json 放入 ClassIsland 目录，在「配对码管理」或「预注册客户端」完成准入。", "pairing" to "前往配对码管理"),
        Triple("3. 客户端绑定与远程推送", "在「客户端管理」为班级分配专属档案或下发实时课堂通知横幅。", "clients" to "前往客户端管理")
    )
    for ((stepTitle, stepDesc, actionPair) in steps) {
        val stepBox = UiKit.subCard(this, 12)
        val r = UiKit.hBox(this)
        val col = UiKit.vBox(this).apply {
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
            addView(UiKit.label(this@buildDashboardView, stepTitle, 13f, UiKit.TEXT_PRIMARY, true))
            addView(UiKit.label(this@buildDashboardView, stepDesc, 12f, UiKit.TEXT_SECONDARY).apply {
                setPadding(0, UiKit.dp(this@buildDashboardView, 2), 0, 0)
            })
        }
        r.addView(col)
        r.addView(UiKit.button(this, actionPair.second, UiKit.BTN_MUTED, UiKit.ACCENT_BLUE, true) {
            navigateToSection(actionPair.first)
        })
        stepBox.addView(r)
        guideCard.addView(stepBox)
    }
    box.addView(guideCard)
    return box
}

// ==================== 4. CLIENTS & REMOTE CONTROL ====================
fun MainActivity.loadClientsData() {
    runAsync(
        work = { ApiClient.request(ApiClient.managementUrl, "/account/$currentAccountId/client/list") },
        done = { r ->
            if (r.ok) {
                clientsArray = r.optJsonArray()
                if (activeClient == null && clientsArray.length() > 0) {
                    selectClientForInspector(clientsArray.optJSONObject(0))
                    return@runAsync
                }
            }
            rebuildUi()
        }
    )
}

fun MainActivity.selectClientForInspector(c: JSONObject?) {
    activeClient = c
    if (c == null) {
        rebuildUi()
        return
    }
    val uid = c.optString("uid")
    runAsync(
        work = {
            val p = ApiClient.request(ApiClient.managementUrl, "/account/$currentAccountId/client/${ApiClient.enc(uid)}/config")
            val s = ApiClient.request(ApiClient.managementUrl, "/account/$currentAccountId/client/${ApiClient.enc(uid)}/config-uploads")
            Pair(p, s)
        },
        done = { (p, s) ->
            clientProfileObj = p.json.optJSONObject("profile_config") ?: p.json.optJSONObject("profile") ?: JSONObject()
            clientSnapshots = s.optJsonArray()
            rebuildUi()
        }
    )
}

fun MainActivity.buildClientsView(): View {
    val box = UiKit.vBox(this)

    box.addView(UiKit.pageHeader(this, "客户端管理", "查看并管理所有已注册的 ClassIsland 实例、分配档案与下发远程指令") {
        addView(UiKit.button(this@buildClientsView, "刷新列表", UiKit.BTN_MUTED, UiKit.TEXT_PRIMARY, true) {
            loadClientsData()
        })
    })

    var onlineCount = 0
    for (i in 0 until clientsArray.length()) {
        if (clientsArray.optJSONObject(i)?.optString("status") == "online") onlineCount++
    }
    val totalCount = clientsArray.length()
    val offlineCount = (totalCount - onlineCount).coerceAtLeast(0)

    val statRow = UiKit.hBox(this)
    statRow.addView(UiKit.statCard(this, "客户端总数", "$totalCount", UiKit.TEXT_PRIMARY))
    statRow.addView(UiKit.statCard(this, "当前在线", "$onlineCount", UiKit.ACCENT_GREEN))
    statRow.addView(UiKit.statCard(this, "当前离线", "$offlineCount", UiKit.TEXT_SECONDARY))
    box.addView(statRow)

    if (isWideTabletLayout()) {
        val split = UiKit.hBox(this).apply { gravity = Gravity.TOP }
        val leftPane = UiKit.vBox(this).apply {
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 0.46f).apply {
                rightMargin = UiKit.dp(this@buildClientsView, 12)
            }
        }
        val rightPane = UiKit.vBox(this).apply {
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 0.54f)
        }
        leftPane.addView(buildClientListCard())
        rightPane.addView(buildClientInspectorCard())
        split.addView(leftPane)
        split.addView(rightPane)
        box.addView(split)
    } else {
        box.addView(buildClientListCard())
        box.addView(buildClientInspectorCard())
    }
    return box
}

fun MainActivity.buildClientListCard(): View {
    val c = UiKit.card(this, 16)
    val head = UiKit.hBox(this)
    head.addView(UiKit.label(this, "客户端列表", 15f, UiKit.TEXT_PRIMARY, true).apply {
        layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
    })
    val filterOpts = listOf("all" to "全部", "online" to "在线", "offline" to "离线", "disabled" to "已禁用")
    head.addView(UiKit.chipRow(this, filterOpts, clientFilter) { k ->
        clientFilter = k
        rebuildUi()
    })
    c.addView(head)
    c.addView(UiKit.spacer(this, 8))

    if (selectedClientUids.isNotEmpty()) {
        val batchBox = UiKit.subCard(this, 10)
        batchBox.addView(UiKit.label(this, "已选择 ${selectedClientUids.size} 台客户端批量操作:", 12f, UiKit.ACCENT_BLUE, true))
        val bRow = UiKit.hBox(this)
        val batchOps = listOf(
            "force_sync" to "强制同步",
            "restart" to "重启",
            "disconnect" to "断开",
            "enable" to "启用",
            "disable" to "禁用"
        )
        for ((act, title) in batchOps) {
            bRow.addView(UiKit.button(this, title, UiKit.ACCENT_BLUE, Color.WHITE, true) {
                val body = JSONObject().apply {
                    put("action", act)
                    put("client_uids", JSONArray(selectedClientUids.toList()))
                }
                runAsync(
                    work = { ApiClient.request(ApiClient.managementUrl, "/account/$currentAccountId/client/batch", "POST", body) },
                    done = { r ->
                        setFeedback(if (r.ok) "批量指令 [$title] 已发送" else r.error, !r.ok)
                        loadClientsData()
                    }
                )
            })
        }
        bRow.addView(UiKit.button(this, "取消选择", UiKit.BTN_MUTED, UiKit.TEXT_SECONDARY, true) {
            selectedClientUids.clear()
            rebuildUi()
        })
        batchBox.addView(bRow)
        c.addView(batchBox)
        c.addView(UiKit.spacer(this, 8))
    }

    if (clientsArray.length() == 0) {
        c.addView(UiKit.label(this, "暂无已注册客户端", 12f, UiKit.TEXT_TERTIARY).apply {
            setPadding(0, UiKit.dp(this@buildClientListCard, 12), 0, UiKit.dp(this@buildClientListCard, 12))
        })
    }

    for (i in 0 until clientsArray.length()) {
        val cl = clientsArray.optJSONObject(i) ?: continue
        val st = cl.optString("status", "offline")
        if (clientFilter != "all" && st != clientFilter) continue
        val uid = cl.optString("uid")
        val isSelected = activeClient?.optString("uid") == uid

        val item = UiKit.vBox(this, 10).apply {
            background = UiKit.roundedBg(
                if (isSelected) UiKit.BG_ACCENT_SUBTLE else UiKit.BG_SUBTLE,
                6,
                this@buildClientListCard,
                if (isSelected) UiKit.ACCENT_BLUE else UiKit.BORDER_COLOR,
                1
            )
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                bottomMargin = UiKit.dp(this@buildClientListCard, 6)
            }
        }
        val row = UiKit.hBox(this)
        val chk = CheckBox(this).apply {
            isChecked = selectedClientUids.contains(uid)
            setOnCheckedChangeListener { _, checked ->
                if (checked) selectedClientUids.add(uid) else selectedClientUids.remove(uid)
                rebuildUi()
            }
        }
        row.addView(chk)

        val infoCol = UiKit.vBox(this).apply {
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
            setOnClickListener { selectClientForInspector(cl) }
        }
        val nameStr = cl.optString("display_name").ifEmpty { cl.optString("client_id").ifEmpty { uid } }
        val titleRow = UiKit.hBox(this)
        titleRow.addView(UiKit.label(this, nameStr, 13f, UiKit.TEXT_PRIMARY, true).apply {
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        })
        titleRow.addView(UiKit.badge(
            this,
            if (st == "online") "在线" else "离线",
            if (st == "online") "success" else "neutral"
        ))
        infoCol.addView(titleRow)
        infoCol.addView(UiKit.label(
            this,
            "标识符: ${cl.optString("client_id", "-")} · IP: ${cl.optString("last_ip", "-")}",
            11f,
            UiKit.TEXT_SECONDARY
        ).apply {
            setPadding(0, UiKit.dp(this@buildClientListCard, 2), 0, 0)
        })
        row.addView(infoCol)
        item.addView(row)
        c.addView(item)
    }
    return c
}

fun MainActivity.buildClientInspectorCard(): View {
    val c = UiKit.card(this, 16)
    val cur = activeClient
    if (cur == null) {
        c.addView(UiKit.label(this, "请从列表中选择一个客户端以管理其档案分配或远程指令", 13f, UiKit.TEXT_SECONDARY))
        return c
    }
    val uid = cur.optString("uid")
    val titleRow = UiKit.hBox(this)
    val col = UiKit.vBox(this).apply {
        layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        addView(UiKit.label(this@buildClientInspectorCard, cur.optString("display_name").ifEmpty { cur.optString("client_id") }, 15f, UiKit.TEXT_PRIMARY, true))
        addView(UiKit.label(this@buildClientInspectorCard, "UID: $uid", 11f, UiKit.TEXT_SECONDARY))
    }
    titleRow.addView(col)
    val st = cur.optString("status", "offline")
    titleRow.addView(UiKit.badge(this, if (st == "online") "在线" else "离线", if (st == "online") "success" else "neutral"))
    c.addView(titleRow)
    c.addView(UiKit.spacer(this, 10))

    val tabs = listOf("info" to "基本控制", "profile" to "分配档案", "snapshots" to "配置抓取", "notify" to "发送通知")
    c.addView(UiKit.chipRow(this, tabs, inspectorTab) { k ->
        inspectorTab = k
        rebuildUi()
    })
    c.addView(UiKit.divider(this, 10))

    when (inspectorTab) {
        "info" -> {
            c.addView(UiKit.label(this, "显示名称", 12f, UiKit.TEXT_SECONDARY, true))
            val dnEd = UiKit.input(this, "备注名称", cur.optString("display_name"))
            c.addView(dnEd)
            c.addView(UiKit.label(this, "客户端标识符", 12f, UiKit.TEXT_SECONDARY, true))
            val cidEd = UiKit.input(this, "班级标识", cur.optString("client_id"))
            c.addView(cidEd)
            c.addView(UiKit.button(this, "保存基本设置", UiKit.ACCENT_BLUE, Color.WHITE, true) {
                val b = JSONObject().put("display_name", dnEd.text.toString()).put("client_id", cidEd.text.toString())
                runAsync(
                    work = { ApiClient.request(ApiClient.managementUrl, "/account/$currentAccountId/client/${ApiClient.enc(uid)}/rename", "POST", b) },
                    done = { r ->
                        setFeedback(if (r.ok) "已保存客户端信息" else r.error, !r.ok)
                        loadClientsData()
                    }
                )
            })
            c.addView(UiKit.divider(this, 12))
            c.addView(UiKit.label(this, "远程操作指令", 12f, UiKit.TEXT_SECONDARY, true))
            val cmdRow = UiKit.hBox(this)
            cmdRow.addView(UiKit.button(this, "强制同步配置", UiKit.ACCENT_BLUE, Color.WHITE, true) {
                runAsync(
                    work = { ApiClient.request(ApiClient.managementUrl, "/account/$currentAccountId/client/${ApiClient.enc(uid)}/command/update-data", "POST", JSONObject()) },
                    done = { setFeedback("已下发强制同步指令") }
                )
            })
            cmdRow.addView(UiKit.button(this, "重启客户端", UiKit.BTN_MUTED, UiKit.TEXT_PRIMARY, true) {
                runAsync(
                    work = { ApiClient.request(ApiClient.managementUrl, "/account/$currentAccountId/client/${ApiClient.enc(uid)}/command/restart", "POST", JSONObject()) },
                    done = { setFeedback("已下发重启指令") }
                )
            })
            cmdRow.addView(UiKit.button(this, "断开连接", UiKit.BTN_MUTED, UiKit.TEXT_PRIMARY, true) {
                runAsync(
                    work = { ApiClient.request(ApiClient.managementUrl, "/account/$currentAccountId/client/${ApiClient.enc(uid)}/disconnect", "POST", JSONObject()) },
                    done = { loadClientsData() }
                )
            })
            val isDisabled = cur.optBoolean("is_disabled", false)
            cmdRow.addView(UiKit.button(
                this,
                if (isDisabled) "启用客户端" else "禁用客户端",
                UiKit.BTN_MUTED,
                UiKit.TEXT_PRIMARY,
                true
            ) {
                val actionPath = if (isDisabled) "enable" else "disable"
                runAsync(
                    work = { ApiClient.request(ApiClient.managementUrl, "/account/$currentAccountId/client/${ApiClient.enc(uid)}/$actionPath", "POST", JSONObject()) },
                    done = { loadClientsData() }
                )
            })
            cmdRow.addView(UiKit.button(this, "删除客户端", UiKit.BADGE_DANGER_BG, UiKit.BADGE_DANGER_FG, true) {
                runAsync(
                    work = { ApiClient.request(ApiClient.managementUrl, "/account/$currentAccountId/client/${ApiClient.enc(uid)}", "DELETE") },
                    done = {
                        activeClient = null
                        loadClientsData()
                    }
                )
            })
            c.addView(cmdRow)
        }
        "profile" -> {
            val fields = listOf(
                "ClassPlan" to "课表配置",
                "TimeLayout" to "时间表配置",
                "Subjects" to "科目配置",
                "Policy" to "策略配置",
                "DefaultSettings" to "默认设置",
                "Components" to "组件配置",
                "Credentials" to "凭据配置"
            )
            val editors = mutableMapOf<String, android.widget.EditText>()
            for ((fieldKey, fieldTitle) in fields) {
                c.addView(UiKit.label(this, fieldTitle, 12f, UiKit.TEXT_SECONDARY, true))
                val curVal = clientProfileObj.optString(fieldKey)
                val ed = UiKit.input(this, "资源名称，留空使用默认", curVal)
                editors[fieldKey] = ed
                c.addView(ed)
            }
            c.addView(UiKit.spacer(this, 6))
            c.addView(UiKit.button(this, "保存档案分配", UiKit.ACCENT_BLUE, Color.WHITE, false) {
                val body = JSONObject()
                for ((k, ed) in editors) body.put(k, ed.text.toString().trim())
                runAsync(
                    work = { ApiClient.request(ApiClient.managementUrl, "/account/$currentAccountId/client/${ApiClient.enc(uid)}/config", "POST", body) },
                    done = { r -> setFeedback(if (r.ok) "已保存客户端档案分配" else r.error, !r.ok) }
                )
            })
        }
        "snapshots" -> {
            c.addView(UiKit.label(this, "向客户端请求上传当前配置快照:", 12f, UiKit.TEXT_SECONDARY, true))
            val typeLabels = CONFIG_TYPE_LABELS.entries.map { it.key to it.value }
            val sRow = UiKit.hBox(this)
            for ((tCode, tName) in typeLabels) {
                sRow.addView(UiKit.button(this, "抓取$tName", UiKit.BTN_MUTED, UiKit.TEXT_PRIMARY, true) {
                    runAsync(
                        work = { ApiClient.request(ApiClient.managementUrl, "/account/$currentAccountId/client/${ApiClient.enc(uid)}/command/get-config?config_type=$tCode") },
                        done = { setFeedback("已下发抓取指令") }
                    )
                })
            }
            c.addView(sRow)
            for (i in 0 until clientSnapshots.length()) {
                val snap = clientSnapshots.optJSONObject(i) ?: continue
                val reqGuid = snap.optString("request_guid")
                val cType = snap.optInt("config_type", 1)
                val typeName = snap.optString("config_type_name").ifEmpty { CONFIG_TYPE_LABELS[cType] ?: "配置快照" }
                val uploadedAt = snap.optString("uploaded_at", "").take(19).replace('T', ' ')
                val sr = UiKit.subCard(this, 10)
                val h = UiKit.hBox(this)
                h.addView(UiKit.label(this, "$typeName · ${uploadedAt.ifEmpty { snap.optString("status", "等待中") }}", 12f, UiKit.TEXT_PRIMARY).apply {
                    layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
                })
                h.addView(UiKit.button(this, "查看内容", UiKit.BTN_MUTED, UiKit.ACCENT_BLUE, true) {
                    val directPayload = snap.optString("payload")
                    if (directPayload.isNotEmpty()) {
                        snapshotDetailText = directPayload
                        rebuildUi()
                    } else if (reqGuid.isNotEmpty()) {
                        runAsync(
                            work = { ApiClient.request(ApiClient.managementUrl, "/account/$currentAccountId/config-upload/${ApiClient.enc(reqGuid)}") },
                            done = { dRes ->
                                snapshotDetailText = dRes.json.optString("payload").ifEmpty { dRes.rawText }
                                rebuildUi()
                            }
                        )
                    }
                })
                sr.addView(h)
                c.addView(sr)
            }
            if (snapshotDetailText.isNotEmpty()) {
                c.addView(UiKit.input(this, "配置快照内容", snapshotDetailText, multiLine = true))
            }
        }
        "notify" -> {
            c.addView(buildNotificationsView(uid))
        }
    }
    return c
}

// ==================== 5. NOTIFICATIONS PUSH (WEB BANNER PREVIEW) ====================
fun MainActivity.buildNotificationsView(fixedClientUid: String?): View {
    val box = UiKit.vBox(this)
    if (fixedClientUid == null) {
        box.addView(UiKit.pageHeader(this, "通知推送", "向指定或全体在线 ClassIsland 终端发送实时桌面横幅与语音提醒"))
    }

    val previewCard = UiKit.card(this, 16)
    previewCard.addView(UiKit.label(this, "ClassIsland 提醒横幅预览", 12f, UiKit.TEXT_SECONDARY, true))
    val bannerBox = UiKit.vBox(this, 16).apply {
        background = UiKit.roundedBg(
            if (notifyEmergency) 0xFFFDE7E9.toInt() else 0xFF1F1F1F.toInt(),
            8,
            this@buildNotificationsView,
            if (notifyEmergency) UiKit.ACCENT_RED else 0xFF333333.toInt(),
            1
        )
        gravity = Gravity.CENTER_HORIZONTAL
        layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
            topMargin = UiKit.dp(this@buildNotificationsView, 8)
        }
    }
    val fgColor = if (notifyEmergency) 0xFFD13438.toInt() else Color.WHITE
    bannerBox.addView(UiKit.label(this, notifyMask.ifEmpty { "提醒标题" }, 16f, fgColor, true).apply {
        gravity = Gravity.CENTER
    })
    bannerBox.addView(UiKit.label(this, notifyContent.ifEmpty { "提醒正文内容" }, 13f, fgColor).apply {
        gravity = Gravity.CENTER
        setPadding(0, UiKit.dp(this@buildNotificationsView, 4), 0, 0)
    })
    previewCard.addView(bannerBox)
    box.addView(previewCard)

    val c = UiKit.card(this, 16)
    c.addView(UiKit.label(this, "通知内容与特效参数", 14f, UiKit.TEXT_PRIMARY, true))
    c.addView(UiKit.spacer(this, 6))

    c.addView(UiKit.label(this, "遮罩标题", 12f, UiKit.TEXT_SECONDARY, true))
    val maskEd = UiKit.input(this, "遮罩标题", notifyMask)
    c.addView(maskEd)

    c.addView(UiKit.label(this, "通知正文", 12f, UiKit.TEXT_SECONDARY, true))
    val contentEd = UiKit.input(this, "通知正文内容", notifyContent)
    c.addView(contentEd)

    val numRow = UiKit.hBox(this)
    val durEd = UiKit.input(this, "持续秒数", "$notifyDuration").apply {
        layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply {
            rightMargin = UiKit.dp(this@buildNotificationsView, 8)
        }
    }
    val repEd = UiKit.input(this, "重复次数", "$notifyRepeat").apply {
        layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
    }
    numRow.addView(durEd)
    numRow.addView(repEd)
    c.addView(numRow)

    c.addView(UiKit.switchRow(this, "紧急通知样式", "使用红色高亮紧急警示横幅", notifyEmergency) {
        notifyEmergency = it
        notifyMask = maskEd.text.toString()
        notifyContent = contentEd.text.toString()
        rebuildUi()
    })
    c.addView(UiKit.switchRow(this, "启用语音播报", "使用系统或 Edge TTS 朗读通知正文", notifySpeech) {
        notifySpeech = it
    })
    c.addView(UiKit.switchRow(this, "启用提醒音效", "播放通知提示音", notifySound) {
        notifySound = it
    })
    c.addView(UiKit.switchRow(this, "启用全屏醒目特效", "显示波纹光效提醒", notifyEffect) {
        notifyEffect = it
    })
    c.addView(UiKit.switchRow(this, "窗口强制置顶", "提醒期间置顶于所有窗口上方", notifyTopmost) {
        notifyTopmost = it
    })

    c.addView(UiKit.spacer(this, 10))
    c.addView(UiKit.button(
        this,
        if (fixedClientUid == null) "向全体在线客户端广播通知" else "发送通知到此客户端",
        if (notifyEmergency) UiKit.ACCENT_RED else UiKit.ACCENT_BLUE,
        Color.WHITE,
        false
    ) {
        notifyMask = maskEd.text.toString()
        notifyContent = contentEd.text.toString()
        notifyDuration = durEd.text.toString().toIntOrNull() ?: 10
        notifyRepeat = repEd.text.toString().toIntOrNull() ?: 1

        val payload = JSONObject().apply {
            put("MessageMask", notifyMask)
            put("MessageContent", notifyContent)
            put("OverlayIconLeft", 0)
            put("OverlayIconRight", 0)
            put("IsEmergency", notifyEmergency)
            put("IsSpeechEnabled", notifySpeech)
            put("IsSoundEnabled", notifySound)
            put("IsEffectEnabled", notifyEffect)
            put("IsTopmost", notifyTopmost)
            put("DurationSeconds", notifyDuration)
            put("RepeatCounts", notifyRepeat)
        }
        val path = if (fixedClientUid == null) {
            "/account/$currentAccountId/client/broadcast/send-notification"
        } else {
            "/account/$currentAccountId/client/${ApiClient.enc(fixedClientUid)}/command/send-notification"
        }
        runAsync(
            work = { ApiClient.request(ApiClient.managementUrl, path, "POST", payload) },
            done = { r -> setFeedback(if (r.ok) "通知已成功发送" else r.error, !r.ok) }
        )
    }.apply {
        layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
    })
    box.addView(c)
    return box
}

// ==================== 6. AUDIT LOGS ====================
fun MainActivity.loadAuditLogsData() {
    val q = if (auditEventFilter == "all") "?limit=200" else "?limit=200&event_type=$auditEventFilter"
    runAsync(
        work = { ApiClient.request(ApiClient.managementUrl, "/account/$currentAccountId/audit/list$q") },
        done = { r ->
            auditLogs = r.optJsonArray()
            rebuildUi()
        }
    )
}

fun MainActivity.buildAuditView(): View {
    val box = UiKit.vBox(this)
    box.addView(UiKit.pageHeader(this, "审计日志", "查看客户端上报的敏感操作、崩溃与集控同步事件") {
        addView(UiKit.button(this@buildAuditView, "刷新", UiKit.BTN_MUTED, UiKit.TEXT_PRIMARY, true) {
            loadAuditLogsData()
        })
        addView(UiKit.button(this@buildAuditView, "清空日志", UiKit.BADGE_DANGER_BG, UiKit.BADGE_DANGER_FG, true) {
            runAsync(
                work = { ApiClient.request(ApiClient.managementUrl, "/account/$currentAccountId/audit/clear", "DELETE") },
                done = {
                    auditLogs = JSONArray()
                    setFeedback("审计日志已清空")
                }
            )
        })
    })

    val c = UiKit.card(this, 16)
    val filterOpts = mutableListOf("all" to "全部事件")
    for ((evCode, evLabel) in AUDIT_EVENT_LABELS) {
        filterOpts.add("$evCode" to evLabel)
    }
    c.addView(UiKit.chipRow(this, filterOpts, auditEventFilter) { k ->
        auditEventFilter = k
        loadAuditLogsData()
    })
    c.addView(UiKit.spacer(this, 10))

    if (auditPayloadPreview.isNotEmpty()) {
        c.addView(UiKit.input(this, "审计载荷详情", auditPayloadPreview, multiLine = true))
    }

    if (auditLogs.length() == 0) {
        c.addView(UiKit.label(this, "暂无审计日志记录", 12f, UiKit.TEXT_TERTIARY).apply {
            setPadding(0, UiKit.dp(this@buildAuditView, 12), 0, UiKit.dp(this@buildAuditView, 12))
        })
    }

    for (i in 0 until auditLogs.length()) {
        val log = auditLogs.optJSONObject(i) ?: continue
        val row = UiKit.subCard(this, 10)
        val h = UiKit.hBox(this)
        val ev = log.optInt("event_type", log.optInt("event", 0))
        val evLabel = log.optString("event_name").ifEmpty { AUDIT_EVENT_LABELS[ev] ?: "事件#$ev" }
        val clientUid = log.optString("client_uid").ifEmpty { log.optString("client_id", "-") }
        h.addView(UiKit.badge(this, evLabel, "info"))
        h.addView(UiKit.label(
            this,
            "  $clientUid · ${formatTs(log.optLong("timestamp_utc"))}",
            12f,
            UiKit.TEXT_PRIMARY,
            true
        ).apply {
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        })
        h.addView(UiKit.button(this, "查看载荷", UiKit.BTN_MUTED, UiKit.TEXT_SECONDARY, true) {
            val decoded = log.optJSONObject("decoded")
            auditPayloadPreview = decoded?.toString(2) ?: log.optString("payload", "{}")
            rebuildUi()
        })
        row.addView(h)
        c.addView(row)
    }
    box.addView(c)
    return box
}
