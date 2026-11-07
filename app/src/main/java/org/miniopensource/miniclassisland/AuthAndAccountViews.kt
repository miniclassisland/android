package org.miniopensource.miniclassisland

import android.graphics.Color
import android.graphics.Typeface
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import org.json.JSONArray
import org.json.JSONObject

// ==================== 1. WEB /LOGIN & CERTIFICATE EXCHANGE VIEW ====================
fun MainActivity.buildAuthView(): View {
    val scroll = ScrollView(this).apply {
        setBackgroundColor(UiKit.LOGIN_BG)
        layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
    }
    val basePad = UiKit.dp(this, 16)
    val outer = UiKit.vBox(this).apply {
        gravity = Gravity.CENTER_HORIZONTAL
        setPadding(
            basePad + safeInsetLeftPx,
            basePad + safeInsetTopPx,
            basePad + safeInsetRightPx,
            basePad + safeInsetBottomPx
        )
    }

    // Top Bar on /login (Server endpoint button + ThemeModeToggle)
    val topBar = UiKit.hBox(this).apply {
        layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        gravity = Gravity.END or Gravity.CENTER_VERTICAL
    }
    val srvBtn = UiKit.iconTextButton(this, MdsIcon.SETTINGS, "服务器地址", 0x26FFFFFF, Color.WHITE, true) {
        showEndpointConfig = !showEndpointConfig
        rebuildUi()
    }
    topBar.addView(srvBtn)
    topBar.addView(buildThemeModeToggle(true))
    outer.addView(topBar)

    val cardWidth = if (isTabletLayout()) UiKit.dp(this, 420) else ViewGroup.LayoutParams.MATCH_PARENT
    val authCard = UiKit.vBox(this, 24).apply {
        background = UiKit.roundedBg(UiKit.BG_CARD, 12, this@buildAuthView, UiKit.BORDER_COLOR, 1)
        layoutParams = LinearLayout.LayoutParams(cardWidth, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
            topMargin = UiKit.dp(this@buildAuthView, 28)
        }
    }

    // Centered Logo & Title matching Web /login
    val brandCol = UiKit.vBox(this).apply {
        gravity = Gravity.CENTER_HORIZONTAL
        val logo = TextView(this@buildAuthView).apply {
            text = "M"
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 22f)
            setTextColor(Color.WHITE)
            setTypeface(typeface, Typeface.BOLD)
            gravity = Gravity.CENTER
            background = UiKit.roundedBg(UiKit.ACCENT_BLUE, 12, this@buildAuthView)
            layoutParams = LinearLayout.LayoutParams(UiKit.dp(this@buildAuthView, 48), UiKit.dp(this@buildAuthView, 48)).apply {
                bottomMargin = UiKit.dp(this@buildAuthView, 12)
            }
        }
        addView(logo)
        val titleText = "欢迎来到 MINI ClassIsland"
        val subText = "请使用扫码器扫描登录安全证书二维码完成移动端登录"
        addView(UiKit.label(this@buildAuthView, titleText, 19f, UiKit.TEXT_PRIMARY, true).apply {
            gravity = Gravity.CENTER
        })
        addView(UiKit.label(this@buildAuthView, subText, 12f, UiKit.TEXT_SECONDARY).apply {
            gravity = Gravity.CENTER
            setPadding(0, UiKit.dp(this@buildAuthView, 4), 0, UiKit.dp(this@buildAuthView, 12))
        })
    }
    authCard.addView(brandCol)

    // Optional Server Endpoint Drawer
    if (showEndpointConfig) {
        val epBox = UiKit.subCard(this, 12)
        epBox.addView(UiKit.label(this, "服务器接口地址配置", 12f, UiKit.TEXT_PRIMARY, true))
        val mgmtEd = UiKit.input(this, "集控接口地址", ApiClient.managementUrl)
        val admEd = UiKit.input(this, "超管接口地址", ApiClient.adminUrl)
        epBox.addView(UiKit.label(this, "集控后端地址:", 11f, UiKit.TEXT_SECONDARY))
        epBox.addView(mgmtEd)
        epBox.addView(UiKit.label(this, "超管后端地址:", 11f, UiKit.TEXT_SECONDARY))
        epBox.addView(admEd)
        val epRow = UiKit.hBox(this)
        epRow.addView(UiKit.button(this, "保存地址", UiKit.ACCENT_BLUE, Color.WHITE, true) {
            ApiClient.setEndpoints(mgmtEd.text.toString(), admEd.text.toString())
            saveLocalAuthState()
            showEndpointConfig = false
            setFeedback("已更新服务器地址")
        })
        epBox.addView(epRow)
        authCard.addView(epBox)
        authCard.addView(UiKit.spacer(this, 10))
    }

    // Status Feedback Banner inside Card
    if (statusMessage.isNotEmpty()) {
        val errBox = UiKit.vBox(this, 10).apply {
            background = UiKit.roundedBg(
                if (statusIsError) UiKit.BADGE_DANGER_BG else UiKit.BADGE_SUCCESS_BG,
                6,
                this@buildAuthView,
                if (statusIsError) UiKit.BADGE_DANGER_FG else UiKit.BADGE_SUCCESS_FG,
                1
            )
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                bottomMargin = UiKit.dp(this@buildAuthView, 12)
            }
        }
        errBox.addView(UiKit.label(
            this,
            statusMessage,
            12f,
            if (statusIsError) UiKit.BADGE_DANGER_FG else UiKit.BADGE_SUCCESS_FG
        ))
        authCard.addView(errBox)
    }

    // Stored Security Certificate Quick Login Banner (if a valid certificate is already saved on device)
    val savedCert = ApiClient.storedCertificate
    if (savedCert != null && !savedCert.isExpired()) {
        val certBanner = UiKit.subCard(this, 12)
        val remHours = (savedCert.remainingSeconds() / 3600L).coerceAtLeast(0L)
        certBanner.addView(UiKit.label(
            this,
            "已绑定本机登录安全证书 · 第 ${savedCert.generation}/${savedCert.maxGeneration} 代 · 剩余约 ${remHours} 小时",
            12f,
            UiKit.ACCENT_BLUE,
            true
        ))
        val cbRow = UiKit.hBox(this).apply {
            setPadding(0, UiKit.dp(this@buildAuthView, 6), 0, 0)
        }
        cbRow.addView(UiKit.button(this, "使用本机安全证书一键登录", UiKit.ACCENT_BLUE, Color.WHITE, true) {
            loginWithStoredCertificate()
        })
        if (savedCert.canRotate()) {
            cbRow.addView(UiKit.button(this, "轮换下一代证书", UiKit.ACCENT_GREEN, Color.WHITE, true) {
                rotateStoredCertificateNow()
            })
        }
        certBanner.addView(cbRow)
        authCard.addView(certBanner)
        authCard.addView(UiKit.spacer(this, 10))
    }

    authCard.addView(
        UiKit.iconTextButton(
            this,
            MdsIcon.QR_CODE,
            "扫码登录",
            UiKit.ACCENT_BLUE,
            Color.WHITE,
            false
        ) {
            QrScanner.startScan(this) { scanned ->
                handleDeepLinkOrScannedText(scanned)
            }
        }.apply {
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }
    )

    authCard.addView(
        UiKit.label(
            this,
            "请先在网页端「个人资料与安全设置」签发移动端登录安全证书，然后点击上方「扫码登录」扫描二维码即可直接完成登录；已绑定安全证书时也可直接扫描网页端登录二维码确认授权。",
            12f,
            UiKit.TEXT_SECONDARY
        ).apply {
            gravity = Gravity.CENTER
            setPadding(0, UiKit.dp(this@buildAuthView, 12), 0, 0)
        }
    )

    outer.addView(authCard)
    outer.addView(buildWebFooterDisclaimer(true))
    scroll.addView(outer)
    return scroll
}

// ==================== 2. WEB USER CENTER HOME VIEW (web/app/page.tsx) ====================
fun MainActivity.buildUserCenterHomeView(): View {
    val box = UiKit.vBox(this)

    val displayName = currentUser.optString("display_name")
        .ifEmpty { currentUser.optString("username", "管理员") }
    val roleCode = currentUser.optString("role_code")
        .ifEmpty { currentUser.optString("role", "normal") }
    val isPendingUser = roleCode == "pending"
    val roleLabel = ROLE_LABELS[roleCode] ?: roleCode
    val totpEnabled = currentUser.optBoolean("totp_enabled", false)

    box.addView(
        UiKit.pageHeader(
            this,
            "欢迎回来，$displayName",
            "用户中心主页 · 从左侧导航栏或下方列表选择任一账户，即可进入对应的集控账户中心"
        ) {
            if (!isPendingUser) {
                addView(
                    UiKit.iconTextButton(
                        this@buildUserCenterHomeView,
                        MdsIcon.ADD,
                        "创建新账户",
                        UiKit.ACCENT_BLUE,
                        Color.WHITE,
                        true
                    ) {
                        showCreateAccountForm = !showCreateAccountForm
                        rebuildUi()
                    }
                )
            }
        }
    )

    if (isPendingUser) {
        val warnCard = UiKit.card(this, 12).apply {
            background = UiKit.roundedBg(UiKit.BADGE_WARNING_BG, 8, this@buildUserCenterHomeView, UiKit.BADGE_WARNING_FG, 1)
        }
        warnCard.addView(
            UiKit.label(
                this,
                "您的账号当前处于待审核状态：暂不会创建默认租户，仅支持通过邀请码加入组织。",
                12f,
                UiKit.BADGE_WARNING_FG
            )
        )
        box.addView(warnCard)
    }

    // Create New Account Popover/Form Card
    if (showCreateAccountForm && !isPendingUser) {
        val createCard = UiKit.card(this, 16)
        createCard.addView(UiKit.label(this, "创建新集控账户", 15f, UiKit.TEXT_PRIMARY, true))
        createCard.addView(UiKit.label(this, "创建一个新的学校或组织集控账户中心", 12f, UiKit.TEXT_SECONDARY).apply {
            setPadding(0, UiKit.dp(this@buildUserCenterHomeView, 2), 0, UiKit.dp(this@buildUserCenterHomeView, 8))
        })
        createCard.addView(UiKit.label(this, "账户 / 组织名称 *", 12f, UiKit.TEXT_SECONDARY, true))
        val nameEd = UiKit.input(this, "例如：第一中学电教中心")
        createCard.addView(nameEd)
        createCard.addView(UiKit.label(this, "子域名标识（可选）", 12f, UiKit.TEXT_SECONDARY, true))
        val slugEd = UiKit.input(this, "留空自动生成")
        createCard.addView(slugEd)
        createCard.addView(UiKit.label(this, "3-64 位小写字母、数字、连字符或下划线", 11f, UiKit.TEXT_TERTIARY))
        val actRow = UiKit.hBox(this).apply {
            setPadding(0, UiKit.dp(this@buildUserCenterHomeView, 8), 0, 0)
        }
        actRow.addView(UiKit.button(this, "立即创建", UiKit.ACCENT_BLUE, Color.WHITE, true) {
            val n = nameEd.text.toString().trim()
            val s = slugEd.text.toString().trim()
            if (n.isEmpty()) {
                setFeedback("请输入账户或组织名称", true)
                return@button
            }
            val body = JSONObject().put("name", n)
            if (s.isNotEmpty()) body.put("slug", s)
            runAsync(
                work = { ApiClient.request(ApiClient.managementUrl, "/account/apply", "POST", body) },
                done = { res ->
                    if (res.ok) {
                        showCreateAccountForm = false
                        val newId = res.json.optString("id", "")
                        val newSlug = res.json.optString("slug", s)
                        runAsync(
                            work = { ApiClient.request(ApiClient.managementUrl, "/account/list") },
                            done = { aRes ->
                                if (aRes.ok) accounts = aRes.optJsonArray()
                                if (newId.isNotEmpty()) {
                                    enterAccountCenter(newId, n, newSlug, "dashboard")
                                } else {
                                    saveLocalAuthState()
                                    rebuildUi()
                                }
                            }
                        )
                    } else {
                        setFeedback(res.error, true)
                    }
                }
            )
        })
        actRow.addView(UiKit.button(this, "取消", UiKit.BTN_MUTED, UiKit.TEXT_SECONDARY, true) {
            showCreateAccountForm = false
            rebuildUi()
        })
        createCard.addView(actRow)
        box.addView(createCard)
    }

    // 3 Stat Cards matching web/app/page.tsx (.stats-grid)
    val statCard1 = UiKit.vBox(this, 14).apply {
        background = UiKit.roundedBg(UiKit.BG_CARD, 8, this@buildUserCenterHomeView, UiKit.BORDER_COLOR, 1)
        val lp = LinearLayout.LayoutParams(
            if (isTabletLayout()) 0 else ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            if (isTabletLayout()) 1f else 0f
        )
        if (isTabletLayout()) lp.rightMargin = UiKit.dp(this@buildUserCenterHomeView, 8)
        lp.bottomMargin = UiKit.dp(this@buildUserCenterHomeView, 8)
        layoutParams = lp
        addView(UiKit.label(this@buildUserCenterHomeView, "可访问账户中心", 12f, UiKit.TEXT_SECONDARY))
        addView(UiKit.label(this@buildUserCenterHomeView, "${accounts.length()}", 24f, UiKit.TEXT_PRIMARY, true).apply {
            setPadding(0, UiKit.dp(this@buildUserCenterHomeView, 4), 0, UiKit.dp(this@buildUserCenterHomeView, 4))
        })
        addView(UiKit.label(this@buildUserCenterHomeView, "可在左侧菜单直接点击切换进入", 11f, UiKit.TEXT_TERTIARY))
    }

    val loginAccountText = currentUser.optString("email")
        .ifEmpty { currentUser.optString("username").ifEmpty { displayName } }
    val statCard2 = UiKit.vBox(this, 14).apply {
        background = UiKit.roundedBg(UiKit.BG_CARD, 8, this@buildUserCenterHomeView, UiKit.BORDER_COLOR, 1)
        val lp = LinearLayout.LayoutParams(
            if (isTabletLayout()) 0 else ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            if (isTabletLayout()) 1f else 0f
        )
        if (isTabletLayout()) lp.rightMargin = UiKit.dp(this@buildUserCenterHomeView, 8)
        lp.bottomMargin = UiKit.dp(this@buildUserCenterHomeView, 8)
        layoutParams = lp
        addView(UiKit.label(this@buildUserCenterHomeView, "当前登录账号", 12f, UiKit.TEXT_SECONDARY))
        addView(UiKit.label(this@buildUserCenterHomeView, loginAccountText, 16f, UiKit.TEXT_PRIMARY, true).apply {
            setPadding(0, UiKit.dp(this@buildUserCenterHomeView, 6), 0, UiKit.dp(this@buildUserCenterHomeView, 6))
        })
        addView(UiKit.label(this@buildUserCenterHomeView, "系统角色：$roleLabel", 11f, UiKit.TEXT_TERTIARY))
    }

    val statCard3 = UiKit.vBox(this, 14).apply {
        background = UiKit.roundedBg(UiKit.BG_CARD, 8, this@buildUserCenterHomeView, UiKit.BORDER_COLOR, 1)
        val lp = LinearLayout.LayoutParams(
            if (isTabletLayout()) 0 else ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            if (isTabletLayout()) 1f else 0f
        )
        lp.bottomMargin = UiKit.dp(this@buildUserCenterHomeView, 8)
        layoutParams = lp
        addView(UiKit.label(this@buildUserCenterHomeView, "两步验证", 12f, UiKit.TEXT_SECONDARY))
        val bWrap = UiKit.hBox(this@buildUserCenterHomeView).apply {
            setPadding(0, UiKit.dp(this@buildUserCenterHomeView, 6), 0, UiKit.dp(this@buildUserCenterHomeView, 6))
        }
        bWrap.addView(
            UiKit.badge(
                this@buildUserCenterHomeView,
                if (totpEnabled) "已开启两步验证" else "未开启两步验证",
                if (totpEnabled) "success" else "warning"
            )
        )
        addView(bWrap)
        addView(UiKit.label(this@buildUserCenterHomeView, "前往个人资料与安全设置 →", 11f, UiKit.ACCENT_BLUE, true).apply {
            setOnClickListener { navigateToSection("security") }
        })
    }

    if (isTabletLayout()) {
        val statsRow = UiKit.hBox(this).apply { gravity = Gravity.TOP }
        statsRow.addView(statCard1)
        statsRow.addView(statCard2)
        statsRow.addView(statCard3)
        box.addView(statsRow)
    } else {
        box.addView(statCard1)
        box.addView(statCard2)
        box.addView(statCard3)
    }

    // Filtered accounts list
    val q = userCenterAccountSearch.trim().lowercase()
    val filteredAccounts = mutableListOf<JSONObject>()
    for (i in 0 until accounts.length()) {
        val acc = accounts.optJSONObject(i) ?: continue
        if (q.isEmpty() ||
            acc.optString("name").lowercase().contains(q) ||
            acc.optString("slug").lowercase().contains(q) ||
            acc.optString("id").lowercase().contains(q)
        ) {
            filteredAccounts.add(acc)
        }
    }

    val listCard = UiKit.card(this, 16)
    val cardHead = UiKit.hBox(this)
    cardHead.addView(UiKit.label(this, "您的集控账户中心 (${filteredAccounts.size})", 15f, UiKit.TEXT_PRIMARY, true).apply {
        layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
    })
    cardHead.addView(UiKit.button(this, "刷新", UiKit.BTN_MUTED, UiKit.TEXT_PRIMARY, true) {
        completeLoginAndLoadAccounts()
    })
    listCard.addView(cardHead)

    val searchRow = UiKit.hBox(this).apply {
        setPadding(0, UiKit.dp(this@buildUserCenterHomeView, 6), 0, UiKit.dp(this@buildUserCenterHomeView, 8))
    }
    val searchEd = UiKit.input(this, "搜索账户名称、子域名标识或账户标识...", userCenterAccountSearch).apply {
        layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply {
            rightMargin = UiKit.dp(this@buildUserCenterHomeView, 8)
        }
    }
    searchRow.addView(searchEd)
    searchRow.addView(UiKit.button(this, "搜索", UiKit.BTN_MUTED, UiKit.TEXT_PRIMARY, true) {
        userCenterAccountSearch = searchEd.text.toString()
        rebuildUi()
    })
    if (userCenterAccountSearch.isNotEmpty()) {
        searchRow.addView(UiKit.button(this, "清除", UiKit.BTN_MUTED, UiKit.TEXT_SECONDARY, true) {
            userCenterAccountSearch = ""
            rebuildUi()
        })
    }
    listCard.addView(searchRow)

    if (accounts.length() == 0) {
        val emptyBox = UiKit.vBox(this, 16).apply {
            gravity = Gravity.CENTER_HORIZONTAL
        }
        emptyBox.addView(UiKit.label(this, "暂无可用账户", 15f, UiKit.TEXT_PRIMARY, true))
        emptyBox.addView(
            UiKit.label(
                this,
                if (isPendingUser)
                    "您的账号正在等待管理员批准，目前可使用邀请码加入现有组织。"
                else
                    "您还没有加入任何集控账户，可以创建新账户或使用邀请码加入现有账户。",
                12f,
                UiKit.TEXT_SECONDARY
            ).apply {
                gravity = Gravity.CENTER
                setPadding(0, UiKit.dp(this@buildUserCenterHomeView, 6), 0, UiKit.dp(this@buildUserCenterHomeView, 12))
            }
        )
        val emptyBtns = UiKit.hBox(this)
        if (!isPendingUser) {
            emptyBtns.addView(UiKit.iconTextButton(this, MdsIcon.ADD, "创建新账户", UiKit.ACCENT_BLUE, Color.WHITE, true) {
                showCreateAccountForm = true
                rebuildUi()
            })
        }
        emptyBtns.addView(UiKit.button(this, "前往安全设置使用邀请码", UiKit.BTN_MUTED, UiKit.TEXT_PRIMARY, true) {
            navigateToSection("security")
        })
        emptyBox.addView(emptyBtns)
        listCard.addView(emptyBox)
    } else if (filteredAccounts.isEmpty()) {
        listCard.addView(UiKit.label(this, "未找到匹配的账户", 13f, UiKit.TEXT_TERTIARY).apply {
            setPadding(0, UiKit.dp(this@buildUserCenterHomeView, 12), 0, UiKit.dp(this@buildUserCenterHomeView, 12))
        })
    } else {
        for (acc in filteredAccounts) {
            val accId = acc.optString("id")
            val accName = acc.optString("name", "未命名账户")
            val accSlug = acc.optString("slug", "default")
            val regMode = acc.optString("registration_mode", "pairing")
            val regLabel = REGISTRATION_MODE_LABELS[regMode] ?: regMode
            val isActive = acc.optBoolean("is_active", true)
            val createdAt = acc.optString("created_at", "").take(10)

            val rowCard = UiKit.subCard(this, 12).apply {
                setOnClickListener {
                    enterAccountCenter(accId, accName, accSlug, "dashboard")
                }
            }
            val topLine = UiKit.hBox(this)
            val iconBox = TextView(this).apply {
                text = accName.take(1).uppercase()
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f)
                setTextColor(UiKit.ACCENT_BLUE)
                setTypeface(typeface, Typeface.BOLD)
                gravity = Gravity.CENTER
                background = UiKit.roundedBg(UiKit.BG_ACCENT_SUBTLE, 8, this@buildUserCenterHomeView)
                layoutParams = LinearLayout.LayoutParams(UiKit.dp(this@buildUserCenterHomeView, 36), UiKit.dp(this@buildUserCenterHomeView, 36)).apply {
                    rightMargin = UiKit.dp(this@buildUserCenterHomeView, 10)
                }
            }
            topLine.addView(iconBox)

            val titleCol = UiKit.vBox(this).apply {
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
            }
            titleCol.addView(UiKit.label(this, accName, 14f, UiKit.TEXT_PRIMARY, true))
            val metaText = if (createdAt.isNotEmpty()) "创建于 $createdAt · 标识: $accSlug" else "标识: $accSlug"
            titleCol.addView(UiKit.label(this, metaText, 11f, UiKit.TEXT_SECONDARY))
            topLine.addView(titleCol)

            topLine.addView(UiKit.badge(this, regLabel, "info").apply {
                layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                    rightMargin = UiKit.dp(this@buildUserCenterHomeView, 6)
                }
            })
            topLine.addView(UiKit.badge(this, if (isActive) "活跃" else "停用", if (isActive) "success" else "danger"))
            rowCard.addView(topLine)

            val botLine = UiKit.hBox(this).apply {
                setPadding(0, UiKit.dp(this@buildUserCenterHomeView, 8), 0, 0)
            }
            botLine.addView(UiKit.label(this, accId, 11f, UiKit.TEXT_TERTIARY).apply {
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
            })
            if (!isPendingUser) {
                botLine.addView(UiKit.iconTextButton(this, MdsIcon.SETTINGS, "基础信息", UiKit.BTN_MUTED, UiKit.TEXT_PRIMARY, true) {
                    enterAccountCenter(accId, accName, accSlug, "account")
                })
            }
            botLine.addView(UiKit.iconTextButton(this, MdsIcon.ARROW_RIGHT, "进入账户中心", UiKit.ACCENT_BLUE, Color.WHITE, true) {
                enterAccountCenter(accId, accName, accSlug, "dashboard")
            })
            rowCard.addView(botLine)
            listCard.addView(rowCard)
        }
    }

    box.addView(listCard)
    box.addView(buildWebFooterDisclaimer(false))
    return box
}

// ==================== 2B. WEB USER CENTER TICKETS VIEW (web/app/tickets/page.tsx) ====================
fun MainActivity.loadTicketsData() {
    val q = if (ticketStatusFilter.isNotEmpty()) "?limit=50&status=${ApiClient.enc(ticketStatusFilter)}" else "?limit=50"
    runAsync(
        work = { ApiClient.request(ApiClient.managementUrl, "/ticket/list$q") },
        done = { res ->
            if (res.ok) {
                ticketsList = res.optJsonArray()
                ticketsTotal = res.json.optInt("total", ticketsList.length())
            }
            rebuildUi()
        }
    )
}

fun MainActivity.openTicketDetail(ticketId: String) {
    runAsync(
        work = { ApiClient.request(ApiClient.managementUrl, "/ticket/${ApiClient.enc(ticketId)}") },
        done = { res ->
            if (res.ok) {
                activeTicketDetail = res.json
            } else {
                setFeedback(res.error, true)
            }
            rebuildUi()
        }
    )
}

fun MainActivity.buildTicketsView(): View {
    val box = UiKit.vBox(this)

    box.addView(
        UiKit.pageHeader(
            this,
            "我的工单",
            "提交问题、跟踪处理进度，并与超级管理员沟通"
        ) {
            addView(
                UiKit.iconTextButton(
                    this@buildTicketsView,
                    MdsIcon.ADD,
                    "提交工单",
                    UiKit.ACCENT_BLUE,
                    Color.WHITE,
                    true
                ) {
                    showCreateTicketForm = !showCreateTicketForm
                    rebuildUi()
                }
            )
            addView(
                UiKit.button(this@buildTicketsView, "刷新", UiKit.BTN_MUTED, UiKit.TEXT_PRIMARY, true) {
                    loadTicketsData()
                }
            )
        }
    )

    if (showCreateTicketForm) {
        val createCard = UiKit.card(this, 16)
        createCard.addView(UiKit.label(this, "提交工单", 15f, UiKit.TEXT_PRIMARY, true))
        createCard.addView(UiKit.label(this, "标题", 12f, UiKit.TEXT_SECONDARY, true))
        val subjEd = UiKit.input(this, "简要描述遇到的问题")
        createCard.addView(subjEd)

        var selectedCategory = "other"
        var selectedPriority = "normal"

        createCard.addView(UiKit.label(this, "分类", 12f, UiKit.TEXT_SECONDARY, true))
        val catOpts = TICKET_CATEGORY_LABELS.entries.map { it.key to it.value }
        createCard.addView(UiKit.chipRow(this, catOpts, selectedCategory) { k ->
            selectedCategory = k
        })

        createCard.addView(UiKit.label(this, "优先级", 12f, UiKit.TEXT_SECONDARY, true))
        val priOpts = TICKET_PRIORITY_LABELS.entries.map { it.key to it.value }
        createCard.addView(UiKit.chipRow(this, priOpts, selectedPriority) { k ->
            selectedPriority = k
        })

        createCard.addView(UiKit.label(this, "问题描述", 12f, UiKit.TEXT_SECONDARY, true))
        val contentEd = UiKit.input(this, "请描述问题出现的场景、操作步骤和期望结果", multiLine = true)
        createCard.addView(contentEd)

        val btnRow = UiKit.hBox(this).apply {
            setPadding(0, UiKit.dp(this@buildTicketsView, 8), 0, 0)
        }
        btnRow.addView(UiKit.button(this, "提交工单", UiKit.ACCENT_BLUE, Color.WHITE, true) {
            val s = subjEd.text.toString().trim()
            val c = contentEd.text.toString().trim()
            if (s.isEmpty() || c.isEmpty()) {
                setFeedback("请填写工单标题与问题描述", true)
                return@button
            }
            val body = JSONObject()
                .put("subject", s)
                .put("content", c)
                .put("category", selectedCategory)
                .put("priority", selectedPriority)
            runAsync(
                work = { ApiClient.request(ApiClient.managementUrl, "/ticket", "POST", body) },
                done = { res ->
                    if (res.ok) {
                        showCreateTicketForm = false
                        activeTicketDetail = res.json
                        setFeedback("工单已提交，管理员会尽快处理。")
                        loadTicketsData()
                    } else {
                        setFeedback(res.error, true)
                    }
                }
            )
        })
        btnRow.addView(UiKit.button(this, "取消", UiKit.BTN_MUTED, UiKit.TEXT_SECONDARY, true) {
            showCreateTicketForm = false
            rebuildUi()
        })
        createCard.addView(btnRow)
        box.addView(createCard)
    }

    // Active Ticket Detail Card
    val detail = activeTicketDetail
    if (detail != null && detail.optString("id").isNotEmpty()) {
        val detailId = detail.optString("id")
        val st = detail.optString("status", "open")
        val cat = detail.optString("category", "other")
        val pri = detail.optString("priority", "normal")
        val dCard = UiKit.card(this, 16)
        val dHead = UiKit.hBox(this)
        dHead.addView(UiKit.label(this, detail.optString("subject", "工单详情"), 15f, UiKit.TEXT_PRIMARY, true).apply {
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        })
        dHead.addView(UiKit.button(this, "返回列表", UiKit.BTN_MUTED, UiKit.TEXT_SECONDARY, true) {
            activeTicketDetail = null
            rebuildUi()
        })
        dCard.addView(dHead)

        val badgeRow = UiKit.hBox(this).apply {
            setPadding(0, UiKit.dp(this@buildTicketsView, 6), 0, UiKit.dp(this@buildTicketsView, 6))
        }
        badgeRow.addView(UiKit.badge(this, TICKET_STATUS_LABELS[st] ?: st, if (st == "resolved") "success" else if (st == "open") "warning" else "info").apply {
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                rightMargin = UiKit.dp(this@buildTicketsView, 6)
            }
        })
        badgeRow.addView(UiKit.badge(this, TICKET_CATEGORY_LABELS[cat] ?: cat, "neutral").apply {
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                rightMargin = UiKit.dp(this@buildTicketsView, 6)
            }
        })
        badgeRow.addView(UiKit.badge(this, "${TICKET_PRIORITY_LABELS[pri] ?: pri}优先级", if (pri == "urgent" || pri == "high") "danger" else "info"))
        dCard.addView(badgeRow)

        val msgs = detail.optJSONArray("messages") ?: JSONArray()
        if (msgs.length() == 0 && detail.optString("content").isNotEmpty()) {
            val initBox = UiKit.subCard(this, 10)
            initBox.addView(UiKit.label(this, "我 · ${detail.optString("created_at").take(19).replace('T', ' ')}", 11f, UiKit.TEXT_TERTIARY, true))
            initBox.addView(UiKit.label(this, detail.optString("content"), 13f, UiKit.TEXT_PRIMARY).apply {
                setPadding(0, UiKit.dp(this@buildTicketsView, 4), 0, 0)
            })
            dCard.addView(initBox)
        }
        for (i in 0 until msgs.length()) {
            val m = msgs.optJSONObject(i) ?: continue
            val role = m.optString("author_role", "user")
            val mBox = UiKit.subCard(this, 10)
            mBox.addView(
                UiKit.label(
                    this,
                    "${if (role == "admin") "超级管理员" else "我"} · ${m.optString("created_at").take(19).replace('T', ' ')}",
                    11f,
                    if (role == "admin") UiKit.ACCENT_BLUE else UiKit.TEXT_TERTIARY,
                    true
                )
            )
            mBox.addView(UiKit.label(this, m.optString("content"), 13f, UiKit.TEXT_PRIMARY).apply {
                setPadding(0, UiKit.dp(this@buildTicketsView, 4), 0, 0)
            })
            dCard.addView(mBox)
        }

        if (st != "closed") {
            dCard.addView(UiKit.divider(this, 10))
            val replyEd = UiKit.input(this, "输入回复内容...", multiLine = true)
            dCard.addView(replyEd)
            val rRow = UiKit.hBox(this)
            rRow.addView(UiKit.button(this, "发送回复", UiKit.ACCENT_BLUE, Color.WHITE, true) {
                val txt = replyEd.text.toString().trim()
                if (txt.isNotEmpty()) {
                    runAsync(
                        work = { ApiClient.request(ApiClient.managementUrl, "/ticket/${ApiClient.enc(detailId)}/reply", "POST", JSONObject().put("content", txt)) },
                        done = { res ->
                            if (res.ok) {
                                openTicketDetail(detailId)
                                loadTicketsData()
                            } else {
                                setFeedback(res.error, true)
                            }
                        }
                    )
                }
            })
            rRow.addView(UiKit.button(this, "关闭工单", UiKit.BADGE_DANGER_BG, UiKit.BADGE_DANGER_FG, true) {
                runAsync(
                    work = { ApiClient.request(ApiClient.managementUrl, "/ticket/${ApiClient.enc(detailId)}/close", "POST", JSONObject()) },
                    done = { res ->
                        if (res.ok) {
                            openTicketDetail(detailId)
                            loadTicketsData()
                        } else {
                            setFeedback(res.error, true)
                        }
                    }
                )
            })
            dCard.addView(rRow)
        } else {
            dCard.addView(UiKit.label(this, "该工单已关闭，无法继续回复。", 12f, UiKit.TEXT_TERTIARY).apply {
                setPadding(0, UiKit.dp(this@buildTicketsView, 8), 0, 0)
            })
        }
        box.addView(dCard)
    }

    // Filter & List Card
    val listCard = UiKit.card(this, 16)
    val filterRow = UiKit.hBox(this)
    val filterOpts = listOf(
        "" to "全部状态",
        "open" to "待处理",
        "in_progress" to "处理中",
        "resolved" to "已解决",
        "closed" to "已关闭"
    )
    filterRow.addView(UiKit.chipRow(this, filterOpts, ticketStatusFilter) { k ->
        ticketStatusFilter = k
        loadTicketsData()
    }.apply {
        layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
    })
    filterRow.addView(UiKit.label(this, "共 $ticketsTotal 个工单", 12f, UiKit.TEXT_SECONDARY))
    listCard.addView(filterRow)
    listCard.addView(UiKit.spacer(this, 8))

    if (ticketsList.length() == 0) {
        listCard.addView(UiKit.label(this, "暂无工单。遇到问题时可以提交工单联系管理员。", 13f, UiKit.TEXT_TERTIARY).apply {
            setPadding(0, UiKit.dp(this@buildTicketsView, 16), 0, UiKit.dp(this@buildTicketsView, 16))
        })
    } else {
        for (i in 0 until ticketsList.length()) {
            val item = ticketsList.optJSONObject(i) ?: continue
            val tid = item.optString("id")
            val st = item.optString("status", "open")
            val cat = item.optString("category", "other")
            val pri = item.optString("priority", "normal")
            val unread = item.optBoolean("user_unread", false)

            val row = UiKit.subCard(this, 12).apply {
                setOnClickListener { openTicketDetail(tid) }
            }
            val top = UiKit.hBox(this)
            top.addView(UiKit.label(this, item.optString("subject", "未命名工单"), 14f, UiKit.TEXT_PRIMARY, true).apply {
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
            })
            if (unread) {
                top.addView(UiKit.badge(this, "新回复", "info"))
            }
            row.addView(top)

            row.addView(
                UiKit.label(
                    this,
                    "更新于 ${item.optString("updated_at").take(19).replace('T', ' ')} · ${item.optInt("message_count", 0)} 条回复",
                    11f,
                    UiKit.TEXT_SECONDARY
                ).apply {
                    setPadding(0, UiKit.dp(this@buildTicketsView, 4), 0, UiKit.dp(this@buildTicketsView, 6))
                }
            )

            val bRow = UiKit.hBox(this)
            bRow.addView(UiKit.badge(this, TICKET_STATUS_LABELS[st] ?: st, if (st == "resolved") "success" else if (st == "open") "warning" else "info").apply {
                layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                    rightMargin = UiKit.dp(this@buildTicketsView, 6)
                }
            })
            bRow.addView(UiKit.badge(this, TICKET_CATEGORY_LABELS[cat] ?: cat, "neutral").apply {
                layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                    rightMargin = UiKit.dp(this@buildTicketsView, 6)
                }
            })
            bRow.addView(UiKit.badge(this, "${TICKET_PRIORITY_LABELS[pri] ?: pri}优先级", if (pri == "urgent" || pri == "high") "danger" else "info"))
            row.addView(bRow)
            listCard.addView(row)
        }
    }

    box.addView(listCard)
    return box
}
