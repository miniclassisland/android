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

// ==================== 2. WEB / ACCOUNT SELECTION VIEW ====================
fun MainActivity.buildAccountSelectionView(): View {
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

    val topBar = UiKit.hBox(this).apply {
        layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        gravity = Gravity.END or Gravity.CENTER_VERTICAL
    }
    topBar.addView(buildThemeModeToggle(true))
    outer.addView(topBar)

    val cardWidth = if (isTabletLayout()) UiKit.dp(this, 460) else ViewGroup.LayoutParams.MATCH_PARENT
    val card = UiKit.vBox(this, 24).apply {
        background = UiKit.roundedBg(UiKit.BG_CARD, 12, this@buildAccountSelectionView, UiKit.BORDER_COLOR, 1)
        layoutParams = LinearLayout.LayoutParams(cardWidth, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
            topMargin = UiKit.dp(this@buildAccountSelectionView, 20)
        }
    }

    // Header matching web/app/page.tsx
    val headCol = UiKit.vBox(this).apply {
        gravity = Gravity.CENTER_HORIZONTAL
        addView(UiKit.label(this@buildAccountSelectionView, "选择 MINI ClassIsland 账户", 19f, UiKit.TEXT_PRIMARY, true))
        addView(UiKit.label(this@buildAccountSelectionView, "选择一个账户以进入集控管理面板", 12f, UiKit.TEXT_SECONDARY).apply {
            setPadding(0, UiKit.dp(this@buildAccountSelectionView, 4), 0, UiKit.dp(this@buildAccountSelectionView, 14))
        })
    }
    card.addView(headCol)

    if (statusMessage.isNotEmpty()) {
        val banner = UiKit.subCard(this, 10).apply {
            background = UiKit.roundedBg(
                if (statusIsError) UiKit.BADGE_DANGER_BG else UiKit.BADGE_SUCCESS_BG,
                6,
                this@buildAccountSelectionView
            )
        }
        banner.addView(UiKit.label(
            this,
            statusMessage,
            12f,
            if (statusIsError) UiKit.BADGE_DANGER_FG else UiKit.BADGE_SUCCESS_FG
        ))
        card.addView(banner)
        card.addView(UiKit.spacer(this, 8))
    }

    // Account Cards List
    if (accounts.length() == 0) {
        card.addView(UiKit.label(this, "您当前还没有加入任何集控账户，请创建一个新账户或使用邀请码加入。", 12f, UiKit.TEXT_SECONDARY).apply {
            setPadding(0, UiKit.dp(this@buildAccountSelectionView, 12), 0, UiKit.dp(this@buildAccountSelectionView, 12))
        })
    } else {
        for (i in 0 until accounts.length()) {
            val acc = accounts.optJSONObject(i) ?: continue
            val accId = acc.optString("id")
            val accName = acc.optString("name", "未命名账户")
            val accSlug = acc.optString("slug", "default")
            val isActive = acc.optBoolean("is_active", true)

            val itemCard = UiKit.hBox(this, 12).apply {
                background = UiKit.roundedBg(UiKit.BG_CARD, 8, this@buildAccountSelectionView, UiKit.BORDER_COLOR, 1)
                layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                    bottomMargin = UiKit.dp(this@buildAccountSelectionView, 8)
                }
                setOnClickListener {
                    currentAccountId = accId
                    currentAccountName = accName
                    isSuperAdminMode = false
                    isSuperAdminTakeover = false
                    saveLocalAuthState()
                    navigateToSection("dashboard")
                }
            }

            val iconBox = TextView(this).apply {
                text = accName.take(1).uppercase()
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
                setTextColor(UiKit.ACCENT_BLUE)
                setTypeface(typeface, Typeface.BOLD)
                gravity = Gravity.CENTER
                background = UiKit.roundedBg(UiKit.BG_ACCENT_SUBTLE, 8, this@buildAccountSelectionView)
                layoutParams = LinearLayout.LayoutParams(UiKit.dp(this@buildAccountSelectionView, 44), UiKit.dp(this@buildAccountSelectionView, 44)).apply {
                    rightMargin = UiKit.dp(this@buildAccountSelectionView, 12)
                }
            }
            itemCard.addView(iconBox)

            val midCol = UiKit.vBox(this).apply {
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
            }
            val titleRow = UiKit.hBox(this)
            titleRow.addView(UiKit.label(this, accName, 14f, UiKit.TEXT_PRIMARY, true).apply {
                setPadding(0, 0, UiKit.dp(this@buildAccountSelectionView, 6), 0)
            })
            titleRow.addView(UiKit.badge(this, accSlug, "neutral").apply {
                layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                    rightMargin = UiKit.dp(this@buildAccountSelectionView, 4)
                }
            })
            titleRow.addView(UiKit.badge(this, if (isActive) "活跃" else "停用", if (isActive) "success" else "danger"))
            midCol.addView(titleRow)
            midCol.addView(UiKit.label(this, accId, 11f, UiKit.TEXT_TERTIARY).apply {
                setPadding(0, UiKit.dp(this@buildAccountSelectionView, 2), 0, 0)
            })
            itemCard.addView(midCol)

            itemCard.addView(OfficialIcons.imageView(this, MdsIcon.ARROW_RIGHT, UiKit.TEXT_TERTIARY, 18))
            card.addView(itemCard)
        }
    }

    // Expandable Create Account or Join Invite Forms
    if (showCreateAccountForm) {
        val form = UiKit.subCard(this, 12)
        form.addView(UiKit.label(this, "创建新集控账户", 13f, UiKit.TEXT_PRIMARY, true))
        val nameEd = UiKit.input(this, "账户名称")
        val slugEd = UiKit.input(this, "唯一标识")
        form.addView(nameEd)
        form.addView(slugEd)
        val r = UiKit.hBox(this)
        r.addView(UiKit.button(this, "创建并进入", UiKit.ACCENT_BLUE, Color.WHITE, true) {
            val n = nameEd.text.toString().trim()
            val s = slugEd.text.toString().trim()
            if (n.isNotEmpty() && s.isNotEmpty()) {
                val b = JSONObject().put("name", n).put("slug", s)
                runAsync(
                    work = { ApiClient.request(ApiClient.managementUrl, "/account/apply", "POST", b) },
                    done = { res ->
                        if (res.ok) {
                            showCreateAccountForm = false
                            val newId = res.json.optString("id", "")
                            if (newId.isNotEmpty()) {
                                currentAccountId = newId
                                currentAccountName = n
                                saveLocalAuthState()
                                navigateToSection("dashboard")
                            } else {
                                loadAccountsListOnly()
                            }
                        } else {
                            setFeedback(res.error, true)
                        }
                    }
                )
            }
        })
        r.addView(UiKit.button(this, "取消", UiKit.BTN_MUTED, UiKit.TEXT_SECONDARY, true) {
            showCreateAccountForm = false
            rebuildUi()
        })
        form.addView(r)
        card.addView(form)
    }

    if (showJoinInviteForm) {
        val form = UiKit.subCard(this, 12)
        form.addView(UiKit.label(this, "使用邀请码加入账户", 13f, UiKit.TEXT_PRIMARY, true))
        val codeEd = UiKit.input(this, "输入邀请码")
        form.addView(codeEd)
        val r = UiKit.hBox(this)
        r.addView(UiKit.button(this, "加入账户", UiKit.ACCENT_BLUE, Color.WHITE, true) {
            val code = codeEd.text.toString().trim()
            if (code.isNotEmpty()) {
                runAsync(
                    work = { ApiClient.request(ApiClient.managementUrl, "/account/invitation/accept", "POST", JSONObject().put("code", code)) },
                    done = { res ->
                        if (res.ok) {
                            showJoinInviteForm = false
                            setFeedback("成功加入集控账户")
                            loadAccountsListOnly()
                        } else {
                            setFeedback(res.error, true)
                        }
                    }
                )
            }
        })
        r.addView(UiKit.button(this, "取消", UiKit.BTN_MUTED, UiKit.TEXT_SECONDARY, true) {
            showJoinInviteForm = false
            rebuildUi()
        })
        form.addView(r)
        card.addView(form)
    }

    card.addView(UiKit.divider(this, 12))

    val btnRow = UiKit.hBox(this)
    btnRow.addView(UiKit.iconTextButton(this, MdsIcon.ADD, "创建新账户", UiKit.ACCENT_BLUE, Color.WHITE, false) {
        showCreateAccountForm = !showCreateAccountForm
        showJoinInviteForm = false
        rebuildUi()
    }.apply {
        layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply {
            rightMargin = UiKit.dp(this@buildAccountSelectionView, 6)
        }
    })
    btnRow.addView(UiKit.button(this, "使用邀请码加入", UiKit.BTN_MUTED, UiKit.TEXT_PRIMARY, false) {
        showJoinInviteForm = !showJoinInviteForm
        showCreateAccountForm = false
        rebuildUi()
    }.apply {
        layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
    })
    card.addView(btnRow)

    val bottomActRow = UiKit.hBox(this).apply {
        layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
            topMargin = UiKit.dp(this@buildAccountSelectionView, 8)
        }
    }
    bottomActRow.addView(UiKit.button(this, "超级管理平台", UiKit.BTN_MUTED, UiKit.ACCENT_BLUE, true) {
        isSuperAdminMode = true
        navigateToSection("admin_dashboard")
    }.apply {
        layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply {
            rightMargin = UiKit.dp(this@buildAccountSelectionView, 6)
        }
    })
    bottomActRow.addView(UiKit.button(this, "退出登录", UiKit.BTN_MUTED, UiKit.ACCENT_RED, true) {
        logoutCurrentUser()
    }.apply {
        layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
    })
    card.addView(bottomActRow)

    outer.addView(card)
    outer.addView(buildWebFooterDisclaimer(true))
    scroll.addView(outer)
    return scroll
}
