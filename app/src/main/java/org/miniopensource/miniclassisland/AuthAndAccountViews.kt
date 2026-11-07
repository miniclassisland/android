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
        val titleText = when (authStep) {
            "login" -> "欢迎回来，请登录"
            "register" -> "创建一个新账户"
            "totp" -> "两步验证"
            "cert" -> "使用登录安全证书登录"
            "oobe" -> "系统首次启动 OOBE 初始化"
            else -> "欢迎来到 MINI ClassIsland"
        }
        val subText = when (authStep) {
            "login" -> if (useAdminLogin) "正在登录超级管理平台" else "输入您的账号密码以继续"
            "register" -> "填写以下信息完成集控账号注册"
            "totp" -> "请输入验证器应用生成的 6 位动态验证码"
            "cert" -> "扫描二维码、点击唤醒链接或粘贴 5 分钟单次兑换码下载 7 天登录安全证书"
            "oobe" -> "配置数据库连接与初始超级管理员账号"
            else -> "输入您的邮箱或使用登录安全证书继续"
        }
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
        val mgmtEd = UiKit.input(this, "Management API 地址", ApiClient.managementUrl)
        val admEd = UiKit.input(this, "Admin API 地址", ApiClient.adminUrl)
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
        epRow.addView(UiKit.button(this, "OOBE 初始化向导", UiKit.BTN_MUTED, UiKit.TEXT_PRIMARY, true) {
            showEndpointConfig = false
            authStep = "oobe"
            rebuildUi()
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

        val qrWebRow = UiKit.hBox(this).apply {
            setPadding(0, UiKit.dp(this@buildAuthView, 6), 0, 0)
        }
        val qrWebEd = UiKit.input(this, "miniclassisland://qr-login/{Code} 授权网页登录", qrWebLoginInput).apply {
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply {
                rightMargin = UiKit.dp(this@buildAuthView, 6)
            }
        }
        qrWebRow.addView(qrWebEd)
        qrWebRow.addView(UiKit.iconTextButton(this, MdsIcon.QR_CODE, "授权网页登录", UiKit.BTN_MUTED, UiKit.ACCENT_BLUE, true) {
            val raw = qrWebEd.text.toString().trim()
            if (raw.isNotEmpty()) {
                qrWebLoginInput = raw
                authorizeWebQrLoginFromApp(raw)
            } else {
                setFeedback("请扫描网页二维码或粘贴网页登录码", true)
            }
        })
        certBanner.addView(qrWebRow)
        authCard.addView(certBanner)
        authCard.addView(UiKit.spacer(this, 10))
    }

    when (authStep) {
        "email" -> {
            authCard.addView(UiKit.label(this, "邮箱地址", 12f, UiKit.TEXT_SECONDARY, true))
            val emailEd = UiKit.input(this, "name@example.com", authEmailInput)
            authCard.addView(emailEd)
            authCard.addView(UiKit.spacer(this, 8))

            authCard.addView(UiKit.button(this, "继续", UiKit.ACCENT_BLUE, Color.WHITE, false) {
                val em = emailEd.text.toString().trim()
                if (em.isEmpty()) {
                    setFeedback("请输入邮箱地址", true)
                    return@button
                }
                if (em.lowercase().startsWith("miniclassisland://exchange-certificate/")) {
                    redeemCertificateCodeAndLogin(em)
                    return@button
                }
                authEmailInput = em
                authUsernameInput = em
                authStep = "login"
                rebuildUi()
            }.apply {
                layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            })

            authCard.addView(UiKit.spacer(this, 6))
            authCard.addView(UiKit.button(this, "使用登录安全证书兑换码 / 扫码登录", UiKit.BTN_MUTED, UiKit.ACCENT_BLUE, false) {
                authStep = "cert"
                statusMessage = ""
                rebuildUi()
            }.apply {
                layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            })

            authCard.addView(UiKit.divider(this, 14))
            val bottomLinks = UiKit.hBox(this)
            bottomLinks.addView(UiKit.button(this, "账号密码登录", UiKit.BTN_MUTED, UiKit.TEXT_PRIMARY, true) {
                authStep = "login"
                useAdminLogin = false
                rebuildUi()
            }.apply {
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply {
                    rightMargin = UiKit.dp(this@buildAuthView, 6)
                }
            })
            bottomLinks.addView(UiKit.button(this, "超级管理员登录", UiKit.BTN_MUTED, UiKit.ACCENT_BLUE, true) {
                authStep = "login"
                useAdminLogin = true
                rebuildUi()
            }.apply {
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
            })
            authCard.addView(bottomLinks)
        }
        "cert" -> {
            authCard.addView(UiKit.label(this, "安全证书兑换链接或兑换码", 12f, UiKit.TEXT_SECONDARY, true))
            val codeEd = UiKit.input(
                this,
                "miniclassisland://exchange-certificate/{Code} 或兑换码",
                certExchangeInput
            )
            authCard.addView(codeEd)
            authCard.addView(UiKit.label(
                this,
                "提示：请先在网页端「个人资料与安全设置 -> 移动端登录安全证书」验证密码（及两步验证）后，使用系统相机扫码直接唤醒本应用，或将唤醒链接粘贴至上方输入框。",
                11f,
                UiKit.TEXT_TERTIARY
            ).apply {
                setPadding(0, UiKit.dp(this@buildAuthView, 4), 0, UiKit.dp(this@buildAuthView, 8))
            })

            authCard.addView(UiKit.button(this, "兑换下载安全证书并登录", UiKit.ACCENT_BLUE, Color.WHITE, false) {
                val raw = codeEd.text.toString().trim()
                if (raw.isEmpty()) {
                    setFeedback("请输入安全证书唤醒链接或 5 分钟兑换码", true)
                    return@button
                }
                certExchangeInput = raw
                redeemCertificateCodeAndLogin(raw)
            }.apply {
                layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            })

            authCard.addView(UiKit.iconTextButton(this, MdsIcon.ARROW_LEFT, "返回账号密码登录", UiKit.BTN_MUTED, UiKit.TEXT_SECONDARY, true) {
                authStep = "email"
                statusMessage = ""
                rebuildUi()
            }.apply {
                layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                    topMargin = UiKit.dp(this@buildAuthView, 8)
                }
            })
        }
        "login" -> {
            val modeBar = UiKit.hBox(this).apply {
                layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                    bottomMargin = UiKit.dp(this@buildAuthView, 10)
                }
            }
            modeBar.addView(UiKit.button(
                this,
                "集控面板登录",
                if (!useAdminLogin) UiKit.ACCENT_BLUE else UiKit.BTN_MUTED,
                if (!useAdminLogin) Color.WHITE else UiKit.TEXT_PRIMARY,
                true
            ) {
                useAdminLogin = false
                rebuildUi()
            }.apply {
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply {
                    rightMargin = UiKit.dp(this@buildAuthView, 6)
                }
            })
            modeBar.addView(UiKit.button(
                this,
                "超级管理平台",
                if (useAdminLogin) UiKit.ACCENT_BLUE else UiKit.BTN_MUTED,
                if (useAdminLogin) Color.WHITE else UiKit.TEXT_PRIMARY,
                true
            ) {
                useAdminLogin = true
                rebuildUi()
            }.apply {
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
            })
            authCard.addView(modeBar)

            authCard.addView(UiKit.label(this, "登录邮箱", 12f, UiKit.TEXT_SECONDARY, true))
            val userEd = UiKit.input(this, "请输入绑定邮箱", authUsernameInput.ifEmpty { authEmailInput })
            authCard.addView(userEd)

            authCard.addView(UiKit.label(this, "密码", 12f, UiKit.TEXT_SECONDARY, true))
            val pwdEd = UiKit.input(this, "请输入密码", isPassword = true)
            authCard.addView(pwdEd)
            authCard.addView(UiKit.spacer(this, 8))

            authCard.addView(UiKit.button(this, "登录", UiKit.ACCENT_BLUE, Color.WHITE, false) {
                val u = userEd.text.toString().trim()
                val p = pwdEd.text.toString()
                if (u.isEmpty() || p.isEmpty()) {
                    setFeedback("请输入邮箱和密码", true)
                    return@button
                }
                authUsernameInput = u
                authEmailInput = u
                val base = if (useAdminLogin) ApiClient.adminUrl else ApiClient.managementUrl
                val body = JSONObject().put("email", u).put("username", u).put("password", p)
                runAsync(
                    work = { ApiClient.request(base, "/user/auth", "POST", body) },
                    done = { res ->
                        if (res.isOobe) {
                            authStep = "oobe"
                            rebuildUi()
                        } else if (!res.ok) {
                            setFeedback(res.error, true)
                        } else if (res.json.optBoolean("requires_2fa", false) || res.json.optBoolean("totp_required", false)) {
                            totpSession = res.json.optString("temp_token").ifEmpty { res.json.optString("totp_session") }
                            authStep = "totp"
                            rebuildUi()
                        } else {
                            onLoginSuccess(res.json)
                        }
                    }
                )
            }.apply {
                layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            })

            val navRow = UiKit.hBox(this).apply {
                layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                    topMargin = UiKit.dp(this@buildAuthView, 10)
                }
            }
            navRow.addView(UiKit.iconTextButton(this, MdsIcon.ARROW_LEFT, "返回上一步", UiKit.BTN_MUTED, UiKit.TEXT_SECONDARY, true) {
                authStep = "email"
                statusMessage = ""
                rebuildUi()
            })
            navRow.addView(UiKit.button(this, "安全证书登录", UiKit.BTN_MUTED, UiKit.ACCENT_BLUE, true) {
                authStep = "cert"
                statusMessage = ""
                rebuildUi()
            })
            navRow.addView(UiKit.button(this, "注册新账号", UiKit.BTN_MUTED, UiKit.ACCENT_BLUE, true) {
                authStep = "register"
                statusMessage = ""
                rebuildUi()
            })
            authCard.addView(navRow)
        }
        "register" -> {
            authCard.addView(UiKit.label(this, "电子邮箱", 12f, UiKit.TEXT_SECONDARY, true))
            val eEd = UiKit.input(this, "name@example.com", authEmailInput)
            authCard.addView(eEd)

            authCard.addView(UiKit.label(this, "用户名", 12f, UiKit.TEXT_SECONDARY, true))
            val uEd = UiKit.input(this, "登录用户名", authUsernameInput)
            authCard.addView(uEd)

            authCard.addView(UiKit.label(this, "显示昵称", 12f, UiKit.TEXT_SECONDARY, true))
            val dEd = UiKit.input(this, "您的称呼", authDisplayNameInput)
            authCard.addView(dEd)

            authCard.addView(UiKit.label(this, "设置密码", 12f, UiKit.TEXT_SECONDARY, true))
            val pEd = UiKit.input(this, "至少 12 位密码（含大小写字母与数字）", isPassword = true)
            authCard.addView(pEd)
            authCard.addView(UiKit.spacer(this, 8))

            authCard.addView(UiKit.button(this, "注册", UiKit.ACCENT_BLUE, Color.WHITE, false) {
                val body = JSONObject().apply {
                    put("username", uEd.text.toString().trim())
                    put("display_name", dEd.text.toString().trim())
                    put("email", eEd.text.toString().trim())
                    put("password", pEd.text.toString())
                }
                runAsync(
                    work = { ApiClient.request(ApiClient.managementUrl, "/user/apply", "POST", body) },
                    done = { res ->
                        if (res.ok) {
                            authUsernameInput = eEd.text.toString().trim()
                            authEmailInput = eEd.text.toString().trim()
                            authStep = "login"
                            setFeedback("注册已提交，请登录或等待管理员审核")
                        } else {
                            setFeedback(res.error, true)
                        }
                    }
                )
            }.apply {
                layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            })

            authCard.addView(UiKit.iconTextButton(this, MdsIcon.ARROW_LEFT, "已有账户？返回登录", UiKit.BTN_MUTED, UiKit.TEXT_SECONDARY, true) {
                authStep = "login"
                rebuildUi()
            }.apply {
                layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                    topMargin = UiKit.dp(this@buildAuthView, 8)
                }
            })
        }
        "totp" -> {
            authCard.addView(UiKit.label(this, if (totpUseRecovery) "应急恢复码" else "6 位动态验证码", 12f, UiKit.TEXT_SECONDARY, true))
            val codeEd = UiKit.input(this, if (totpUseRecovery) "输入恢复码" else "000000")
            authCard.addView(codeEd)
            authCard.addView(UiKit.spacer(this, 8))

            authCard.addView(UiKit.button(this, "验证并登录", UiKit.ACCENT_BLUE, Color.WHITE, false) {
                val base = if (useAdminLogin) ApiClient.adminUrl else ApiClient.managementUrl
                val path = if (totpUseRecovery) "/user/2fa/totp/recover" else "/user/2fa/totp/verify"
                val key = if (totpUseRecovery) "recovery_code" else "code"
                val body = JSONObject()
                    .put("temp_token", totpSession)
                    .put("totp_session", totpSession)
                    .put(key, codeEd.text.toString().trim())
                runAsync(
                    work = { ApiClient.request(base, path, "POST", body) },
                    done = { res ->
                        if (res.ok) {
                            onLoginSuccess(res.json)
                        } else {
                            setFeedback(res.error, true)
                        }
                    }
                )
            }.apply {
                layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            })

            val r = UiKit.hBox(this).apply {
                layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                    topMargin = UiKit.dp(this@buildAuthView, 8)
                }
            }
            r.addView(UiKit.button(this, if (totpUseRecovery) "使用动态验证码" else "使用应急恢复码", UiKit.BTN_MUTED, UiKit.TEXT_PRIMARY, true) {
                totpUseRecovery = !totpUseRecovery
                rebuildUi()
            })
            r.addView(UiKit.button(this, "返回登录", UiKit.BTN_MUTED, UiKit.TEXT_SECONDARY, true) {
                authStep = "login"
                rebuildUi()
            })
            authCard.addView(r)
        }
        "oobe" -> {
            val passEd = UiKit.input(this, "OOBE Passcode 口令")
            val pgEd = UiKit.input(this, "PostgreSQL URL", "postgres://postgres:postgres@localhost:5432/miniclassisland?sslmode=disable")
            val redisEd = UiKit.input(this, "Redis URL", "redis://localhost:6379/0")
            val domainEd = UiKit.input(this, "系统域名", "api.miniclassisland.com")
            val userEd = UiKit.input(this, "超管用户名", "admin")
            val emailEd = UiKit.input(this, "超管邮箱", "admin@example.com")
            val pwdEd = UiKit.input(this, "超管密码", "", isPassword = true)
            authCard.addView(passEd)
            authCard.addView(pgEd)
            authCard.addView(redisEd)
            authCard.addView(domainEd)
            authCard.addView(userEd)
            authCard.addView(emailEd)
            authCard.addView(pwdEd)
            val actRow = UiKit.hBox(this)
            actRow.addView(UiKit.button(this, "返回登录", UiKit.BTN_MUTED, UiKit.TEXT_PRIMARY, true) {
                authStep = "login"
                rebuildUi()
            })
            actRow.addView(UiKit.button(this, "完成 OOBE 初始化", UiKit.ACCENT_BLUE, Color.WHITE, true) {
                val body = JSONObject().apply {
                    put("passcode", passEd.text.toString())
                    put("postgres_url", pgEd.text.toString())
                    put("redis_url", redisEd.text.toString())
                    put("domain", domainEd.text.toString())
                    put("admin_username", userEd.text.toString())
                    put("admin_email", emailEd.text.toString())
                    put("admin_password", pwdEd.text.toString())
                }
                runAsync(
                    work = { ApiClient.request(ApiClient.adminUrl, "/oobe/configure", "POST", body) },
                    done = { r ->
                        if (r.ok) {
                            authStep = "login"
                            setFeedback("OOBE 初始化完成，请登录")
                        } else {
                            setFeedback(r.error, true)
                        }
                    }
                )
            })
            authCard.addView(actRow)
        }
    }

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
        val nameEd = UiKit.input(this, "账户名称 (如 某某中学高一年级)")
        val slugEd = UiKit.input(this, "唯一标识 (如 school-g1)")
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
