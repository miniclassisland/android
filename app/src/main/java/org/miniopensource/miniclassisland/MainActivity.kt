package org.miniopensource.miniclassisland

import android.app.Activity
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.Typeface
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.view.WindowManager
import android.widget.CheckBox
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Switch
import android.widget.TextView
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.Executors

class MainActivity : Activity() {
    private val executor = Executors.newCachedThreadPool()
    private val mainHandler = Handler(Looper.getMainLooper())

    // Global Auth & Shell State (1:1 with Web /login, /, and AppShell)
    private var isLoggedIn = false
    private var authStep = "email" // "email" | "login" | "register" | "totp" | "oobe"
    private var authEmailInput = ""
    private var authUsernameInput = ""
    private var authDisplayNameInput = ""
    private var useAdminLogin = false
    private var showEndpointConfig = false
    private var totpSession = ""
    private var totpUseRecovery = false
    private var statusMessage = ""
    private var statusIsError = false

    // Account Selection & Shell Popover State
    private var currentUser = JSONObject()
    private var accounts = JSONArray()
    private var currentAccountId = 0
    private var currentAccountName = ""
    private var showCreateAccountForm = false
    private var showJoinInviteForm = false
    private var showUserMenu = false
    private var isSuperAdminMode = false
    private var isSuperAdminTakeover = false

    private var currentSection = "dashboard"
    private var showMobileMenu = false

    // Profile Editor Component instance
    private lateinit var profileEditor: ProfileEditorComponent

    // Dashboard metrics
    private var dashOnline = 0
    private var dashTotalClients = 0
    private var dashPendingPairing = 0
    private var dashTotalResources = 0
    private var dashRecentAudits = 0
    private var exportedPresetJson = ""

    // Clients & Inspector state
    private var clientsArray = JSONArray()
    private var clientFilter = "all"
    private var clientSearchKeyword = ""
    private var selectedClientUids = mutableSetOf<String>()
    private var activeClient: JSONObject? = null
    private var inspectorTab = "info" // "info" | "profile" | "snapshots" | "notify"
    private var clientProfileObj = JSONObject()
    private var clientSnapshots = JSONArray()
    private var snapshotDetailText = ""

    // Notifications state
    private var notifyTargetUid = "__broadcast__"
    private var notifyMask = "集控中心通知"
    private var notifyContent = "请各班注意查收最新课表安排"
    private var notifyEmergency = false
    private var notifySpeech = true
    private var notifySound = true
    private var notifyEffect = true
    private var notifyTopmost = true
    private var notifyDuration = 10
    private var notifyRepeat = 1

    // Audit Logs state
    private var auditLogs = JSONArray()
    private var auditEventFilter = "all"
    private var auditPayloadPreview = ""

    // Admission & Governance state
    private var accountInfoObj = JSONObject()
    private var pairingList = JSONArray()
    private var preRegList = JSONArray()
    private var quotasList = JSONArray()
    private var membersList = JSONArray()
    private var invitationsList = JSONArray()
    private var globalUsersList = JSONArray()
    private var pendingUsersList = JSONArray()
    private var globalAccountsList = JSONArray()
    private var globalStatsObj = JSONObject()
    private var sysSettingsObj = JSONObject()
    private var totpEnableSecret = ""
    private var totpEnableUri = ""
    private var totpRecoveryCodesText = ""

    // Edge-to-Edge Safe Area Insets (in pixels)
    private var safeInsetTopPx = 0
    private var safeInsetBottomPx = 0
    private var safeInsetLeftPx = 0
    private var safeInsetRightPx = 0

    private lateinit var rootContainer: LinearLayout

    fun <T> runAsync(work: () -> T, done: (T) -> Unit) {
        executor.execute {
            val res = work()
            mainHandler.post {
                if (!isFinishing && !isDestroyed) {
                    done(res)
                }
            }
        }
    }

    fun apiBase(): String = "${ApiClient.managementUrl}/account/$currentAccountId"

    fun showBanner(msg: String, isErr: Boolean = false) {
        setFeedback(msg, isErr)
    }

    fun renderUI() {
        rebuildUi()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        UiKit.syncSystemNightMode(this)
        setupEdgeToEdgeWindow()
        profileEditor = ProfileEditorComponent(this)
        rootContainer = UiKit.vBox(this).apply {
            setBackgroundColor(UiKit.BG_ROOT)
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            setOnApplyWindowInsetsListener { _, insets ->
                var top = 0
                var bottom = 0
                var left = 0
                var right = 0
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    val bars = insets.getInsets(
                        WindowInsets.Type.systemBars() or WindowInsets.Type.displayCutout()
                    )
                    top = bars.top
                    bottom = bars.bottom
                    left = bars.left
                    right = bars.right
                } else {
                    @Suppress("DEPRECATION")
                    top = insets.systemWindowInsetTop
                    @Suppress("DEPRECATION")
                    bottom = insets.systemWindowInsetBottom
                    @Suppress("DEPRECATION")
                    left = insets.systemWindowInsetLeft
                    @Suppress("DEPRECATION")
                    right = insets.systemWindowInsetRight
                }
                if (top != safeInsetTopPx || bottom != safeInsetBottomPx || left != safeInsetLeftPx || right != safeInsetRightPx) {
                    safeInsetTopPx = top
                    safeInsetBottomPx = bottom
                    safeInsetLeftPx = left
                    safeInsetRightPx = right
                    rebuildUi()
                }
                insets
            }
        }
        setContentView(rootContainer)
        rebuildUi()
    }

    private fun setupEdgeToEdgeWindow() {
        val w = window ?: return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            w.setDecorFitsSystemWindows(false)
        } else {
            @Suppress("DEPRECATION")
            w.decorView.systemUiVisibility = (
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                )
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            w.attributes = w.attributes.apply {
                layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            }
        }
        @Suppress("DEPRECATION")
        w.statusBarColor = Color.TRANSPARENT
        @Suppress("DEPRECATION")
        w.navigationBarColor = Color.TRANSPARENT
    }

    private fun updateSystemBarsAppearance(onLoginBg: Boolean) {
        val w = window ?: return
        val lightBars = !onLoginBg && !UiKit.isDark
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val ctrl = w.insetsController ?: return
            val mask = WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS or WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS
            ctrl.setSystemBarsAppearance(if (lightBars) mask else 0, mask)
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            @Suppress("DEPRECATION")
            var vis = w.decorView.systemUiVisibility
            @Suppress("DEPRECATION")
            vis = if (lightBars) {
                vis or View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
            } else {
                vis and View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR.inv()
            }
            @Suppress("DEPRECATION")
            w.decorView.systemUiVisibility = vis
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        UiKit.syncSystemNightMode(this)
        rebuildUi()
    }

    private fun isTabletLayout(): Boolean = resources.configuration.screenWidthDp >= 600
    private fun isWideTabletLayout(): Boolean = resources.configuration.screenWidthDp >= 840

    private fun setFeedback(msg: String, isErr: Boolean = false) {
        statusMessage = msg
        statusIsError = isErr
        rebuildUi()
    }

    // ==================== THEME MODE TOGGLE (自动 | 浅色 | 深色) ====================
    private fun buildThemeModeToggle(onLoginBg: Boolean = false): View {
        val bar = UiKit.hBox(this, 2).apply {
            background = UiKit.roundedBg(
                if (onLoginBg) 0x26FFFFFF else UiKit.BG_SUBTLE,
                6,
                this@MainActivity,
                if (onLoginBg) 0x40FFFFFF else UiKit.BORDER_COLOR,
                1
            )
        }
        val modes = listOf("system" to "自动", "light" to "浅色", "dark" to "深色")
        for ((mKey, mLabel) in modes) {
            val active = UiKit.themeMode == mKey
            val item = TextView(this).apply {
                text = mLabel
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f)
                setTextColor(
                    when {
                        active && onLoginBg -> Color.WHITE
                        active -> UiKit.ACCENT_BLUE
                        onLoginBg -> 0xCCFFFFFF.toInt()
                        else -> UiKit.TEXT_SECONDARY
                    }
                )
                if (active) setTypeface(typeface, Typeface.BOLD)
                background = UiKit.roundedBg(
                    when {
                        active && onLoginBg -> 0x33FFFFFF
                        active -> UiKit.BG_CARD
                        else -> Color.TRANSPARENT
                    },
                    4,
                    this@MainActivity
                )
                setPadding(UiKit.dp(this@MainActivity, 8), UiKit.dp(this@MainActivity, 4), UiKit.dp(this@MainActivity, 8), UiKit.dp(this@MainActivity, 4))
                setOnClickListener {
                    UiKit.themeMode = mKey
                    rebuildUi()
                }
            }
            bar.addView(item)
        }
        return bar
    }

    // ==================== WEB FOOTER DISCLAIMER ====================
    private fun buildWebFooterDisclaimer(onLoginBg: Boolean = true): View {
        val fg = if (onLoginBg) 0xCCFFFFFF.toInt() else UiKit.TEXT_TERTIARY
        return UiKit.vBox(this).apply {
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(UiKit.dp(this@MainActivity, 12), UiKit.dp(this@MainActivity, 20), UiKit.dp(this@MainActivity, 12), UiKit.dp(this@MainActivity, 16))
            addView(UiKit.label(this@MainActivity, "本项目与 ClassIsland 官方无关，ClassIsland 是 HelloWRC 开发的自由软件。", 11f, fg).apply {
                gravity = Gravity.CENTER
            })
            addView(UiKit.label(this@MainActivity, "本项目由 MINIOpenSource 开发和维护，并开源在 GitHub", 11f, fg).apply {
                gravity = Gravity.CENTER
                setPadding(0, UiKit.dp(this@MainActivity, 2), 0, 0)
            })
        }
    }

    // ==================== ROOT ROUTER ====================
    fun rebuildUi() {
        UiKit.syncSystemNightMode(this)
        val isOnLoginSurface = !isLoggedIn || (!isSuperAdminMode && currentAccountId <= 0)
        updateSystemBarsAppearance(isOnLoginSurface)
        rootContainer.removeAllViews()
        rootContainer.setBackgroundColor(UiKit.BG_ROOT)

        // 1. Not logged in -> Web /login Page
        if (!isLoggedIn) {
            rootContainer.addView(buildAuthView())
            return
        }

        // 2. Logged in & no account selected -> Web / Account Selection Page
        if (!isSuperAdminMode && currentAccountId <= 0) {
            rootContainer.addView(buildAccountSelectionView())
            return
        }

        // 3. Main AppShell (TopBar + Sidebar + Main Content)
        rootContainer.addView(buildTopBar())
        rootContainer.addView(View(this).apply {
            setBackgroundColor(UiKit.BORDER_COLOR)
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, UiKit.dp(this@MainActivity, 1))
        })

        // UserMenu Popover Card (when toggled from top-right avatar pill)
        if (showUserMenu) {
            rootContainer.addView(buildUserMenuPopover())
        }

        if (isTabletLayout()) {
            val bodyRow = UiKit.hBox(this).apply {
                gravity = Gravity.TOP
                layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f)
            }
            val sidebarWrap = ScrollView(this).apply {
                setBackgroundColor(UiKit.BG_SIDEBAR)
                layoutParams = LinearLayout.LayoutParams(
                    UiKit.dp(this@MainActivity, 240) + safeInsetLeftPx,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                addView(buildSidebarContent())
            }
            val sidebarBorder = View(this).apply {
                setBackgroundColor(UiKit.BORDER_COLOR)
                layoutParams = LinearLayout.LayoutParams(UiKit.dp(this@MainActivity, 1), ViewGroup.LayoutParams.MATCH_PARENT)
            }
            val contentScroll = ScrollView(this).apply {
                setBackgroundColor(UiKit.BG_ROOT)
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f)
                addView(buildActiveSectionContainer())
            }
            bodyRow.addView(sidebarWrap)
            bodyRow.addView(sidebarBorder)
            bodyRow.addView(contentScroll)
            rootContainer.addView(bodyRow)
        } else {
            if (showMobileMenu) {
                val menuScroll = ScrollView(this).apply {
                    setBackgroundColor(UiKit.BG_SIDEBAR)
                    layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f)
                    addView(buildSidebarContent())
                }
                rootContainer.addView(menuScroll)
            } else {
                val mainScroll = ScrollView(this).apply {
                    setBackgroundColor(UiKit.BG_ROOT)
                    layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f)
                    addView(buildActiveSectionContainer())
                }
                rootContainer.addView(mainScroll)
            }
        }
    }

    // ==================== WEB TOPBAR ====================
    private fun buildTopBar(): View {
        val bar = UiKit.hBox(this).apply {
            setBackgroundColor(UiKit.BG_HEADER)
            setPadding(
                UiKit.dp(this@MainActivity, 14) + safeInsetLeftPx,
                UiKit.dp(this@MainActivity, 8) + safeInsetTopPx,
                UiKit.dp(this@MainActivity, 14) + safeInsetRightPx,
                UiKit.dp(this@MainActivity, 8)
            )
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        }

        if (!isTabletLayout()) {
            bar.addView(UiKit.button(this, if (showMobileMenu) "✕" else "☰", UiKit.BTN_MUTED, UiKit.TEXT_PRIMARY, true) {
                showMobileMenu = !showMobileMenu
                rebuildUi()
            })
        }

        // Brand Logo Box + Title matching Web AppShell
        val logoBox = TextView(this).apply {
            text = if (isSuperAdminMode) "A" else "M"
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
            setTextColor(Color.WHITE)
            setTypeface(typeface, Typeface.BOLD)
            gravity = Gravity.CENTER
            background = UiKit.roundedBg(UiKit.ACCENT_BLUE, 6, this@MainActivity)
            layoutParams = LinearLayout.LayoutParams(UiKit.dp(this@MainActivity, 26), UiKit.dp(this@MainActivity, 26)).apply {
                rightMargin = UiKit.dp(this@MainActivity, 8)
            }
        }
        bar.addView(logoBox)

        val shellTitle = if (isSuperAdminMode) "MINI ClassIsland 超级管理平台" else "MINI ClassIsland 管理面板"
        bar.addView(UiKit.label(this, shellTitle, 14f, UiKit.TEXT_PRIMARY, true).apply {
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        })

        // ThemeModeToggle
        bar.addView(buildThemeModeToggle(false).apply {
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                rightMargin = UiKit.dp(this@MainActivity, 8)
            }
        })

        // UserMenu Pill (Avatar + Account/User Name + ▾)
        val userName = currentUser.optString("display_name").ifEmpty { currentUser.optString("username", "管理员") }
        val pillLabel = if (isSuperAdminMode) userName else currentAccountName.ifEmpty { userName }
        val userPill = UiKit.hBox(this, 4).apply {
            background = UiKit.roundedBg(UiKit.BG_SUBTLE, 6, this@MainActivity, UiKit.BORDER_COLOR, 1)
            setPadding(UiKit.dp(this@MainActivity, 8), UiKit.dp(this@MainActivity, 4), UiKit.dp(this@MainActivity, 8), UiKit.dp(this@MainActivity, 4))
            setOnClickListener {
                showUserMenu = !showUserMenu
                rebuildUi()
            }
        }
        val avatar = TextView(this).apply {
            text = pillLabel.take(1).uppercase()
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f)
            setTextColor(Color.WHITE)
            setTypeface(typeface, Typeface.BOLD)
            gravity = Gravity.CENTER
            background = UiKit.roundedBg(UiKit.ACCENT_BLUE, 99, this@MainActivity)
            layoutParams = LinearLayout.LayoutParams(UiKit.dp(this@MainActivity, 22), UiKit.dp(this@MainActivity, 22)).apply {
                rightMargin = UiKit.dp(this@MainActivity, 6)
            }
        }
        userPill.addView(avatar)
        userPill.addView(UiKit.label(this, "$pillLabel ▾", 12f, UiKit.TEXT_PRIMARY, true))
        bar.addView(userPill)

        return bar
    }

    private fun buildUserMenuPopover(): View {
        val wrap = UiKit.vBox(this).apply {
            setBackgroundColor(UiKit.BG_SUBTLE)
            setPadding(UiKit.dp(this@MainActivity, 16), UiKit.dp(this@MainActivity, 10), UiKit.dp(this@MainActivity, 16), UiKit.dp(this@MainActivity, 10))
        }
        val card = UiKit.card(this, 12)
        val uName = currentUser.optString("display_name").ifEmpty { currentUser.optString("username", "当前用户") }
        val uMail = currentUser.optString("email", "")
        val topRow = UiKit.hBox(this)
        val infoCol = UiKit.vBox(this).apply {
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
            addView(UiKit.label(this@MainActivity, "当前登录: $uName ${if (uMail.isNotEmpty()) "($uMail)" else ""}", 13f, UiKit.TEXT_PRIMARY, true))
            if (!isSuperAdminMode && currentAccountId > 0) {
                addView(UiKit.label(this@MainActivity, "当前账户: $currentAccountName (ID: $currentAccountId)", 12f, UiKit.TEXT_SECONDARY))
            }
        }
        topRow.addView(infoCol)
        topRow.addView(UiKit.button(this, "收起", UiKit.BTN_MUTED, UiKit.TEXT_SECONDARY, true) {
            showUserMenu = false
            rebuildUi()
        })
        card.addView(topRow)
        card.addView(UiKit.divider(this, 8))

        val actionsRow = UiKit.hBox(this)
        actionsRow.addView(UiKit.button(this, "切换账户", UiKit.BTN_MUTED, UiKit.TEXT_PRIMARY, true) {
            showUserMenu = false
            isSuperAdminMode = false
            isSuperAdminTakeover = false
            currentAccountId = 0
            currentAccountName = ""
            loadAccountsListOnly()
        })
        actionsRow.addView(UiKit.button(this, "安全设置", UiKit.BTN_MUTED, UiKit.TEXT_PRIMARY, true) {
            showUserMenu = false
            isSuperAdminMode = false
            if (currentAccountId <= 0 && accounts.length() > 0) {
                val first = accounts.optJSONObject(0)
                if (first != null) {
                    currentAccountId = first.optInt("id")
                    currentAccountName = first.optString("name")
                }
            }
            navigateToSection("security")
        })
        actionsRow.addView(UiKit.button(
            this,
            if (isSuperAdminMode) "返回集控管理面板" else "超级管理平台",
            UiKit.BTN_MUTED,
            UiKit.ACCENT_BLUE,
            true
        ) {
            showUserMenu = false
            if (isSuperAdminMode) {
                isSuperAdminMode = false
                if (currentAccountId > 0) {
                    navigateToSection("dashboard")
                } else {
                    rebuildUi()
                }
            } else {
                isSuperAdminMode = true
                navigateToSection("admin_dashboard")
            }
        })
        actionsRow.addView(UiKit.button(this, "退出登录", UiKit.BADGE_DANGER_BG, UiKit.BADGE_DANGER_FG, true) {
            showUserMenu = false
            logoutCurrentUser()
        })
        card.addView(actionsRow)
        wrap.addView(card)
        return wrap
    }

    private fun logoutCurrentUser() {
        runAsync(
            work = { ApiClient.request(ApiClient.managementUrl, "/token/deactivate", "DELETE") },
            done = {
                ApiClient.token = ""
                isLoggedIn = false
                isSuperAdminMode = false
                isSuperAdminTakeover = false
                currentAccountId = 0
                currentAccountName = ""
                authStep = "email"
                rebuildUi()
            }
        )
    }

    // ==================== WEB SIDEBAR (6 EXACT SECTIONS + SUPERADMIN) ====================
    private fun buildSidebarContent(): View {
        val p = UiKit.dp(this, 10)
        val box = UiKit.vBox(this).apply {
            setPadding(p + safeInsetLeftPx, p, p, p + safeInsetBottomPx)
        }

        if (isSuperAdminMode) {
            box.addView(UiKit.label(this, "超级管理平台", 11f, UiKit.TEXT_TERTIARY, true).apply {
                setPadding(UiKit.dp(this@MainActivity, 10), UiKit.dp(this@MainActivity, 6), UiKit.dp(this@MainActivity, 10), UiKit.dp(this@MainActivity, 4))
            })
            val adminItems = listOf(
                "admin_dashboard" to "仪表盘",
                "admin_users" to "用户管理",
                "admin_pending" to "注册审核",
                "admin_accounts" to "账户与配额",
                "admin_settings" to "系统设置"
            )
            for ((key, title) in adminItems) {
                box.addView(buildSidebarNavItem(key, title))
            }
            box.addView(UiKit.divider(this, 10))
            box.addView(UiKit.button(this, "← 返回集控管理面板", UiKit.BTN_MUTED, UiKit.TEXT_PRIMARY, false) {
                isSuperAdminMode = false
                if (currentAccountId > 0) {
                    navigateToSection("dashboard")
                } else {
                    rebuildUi()
                }
            }.apply {
                layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            })
            return box
        }

        // Exact 6 Web Navigation Groups from web/app/components/Sidebar.tsx
        val navGroups = listOf(
            "概览" to listOf(
                "dashboard" to "仪表盘"
            ),
            "档案管理" to listOf(
                "res_Subjects" to "科目管理",
                "res_TimeLayout" to "时间表管理",
                "res_ClassPlan" to "课表管理",
                "res_Policy" to "策略管理",
                "res_DefaultSettings" to "默认设置",
                "res_Components" to "组件设置",
                "res_Credentials" to "凭据管理"
            ),
            "客户端" to listOf(
                "clients" to "客户端管理",
                "notifications" to "通知推送",
                "audit" to "审计日志"
            ),
            "接入管理" to listOf(
                "pairing" to "配对码管理",
                "prereg" to "预注册客户端"
            ),
            "账户" to listOf(
                "account" to "账户概览",
                "access" to "访问控制",
                "invitations" to "邀请管理"
            ),
            "安全" to listOf(
                "security" to "安全设置"
            )
        )

        for ((groupTitle, items) in navGroups) {
            box.addView(UiKit.label(this, groupTitle, 11f, UiKit.TEXT_TERTIARY, true).apply {
                setPadding(UiKit.dp(this@MainActivity, 10), UiKit.dp(this@MainActivity, 10), UiKit.dp(this@MainActivity, 10), UiKit.dp(this@MainActivity, 4))
            })
            for ((key, title) in items) {
                box.addView(buildSidebarNavItem(key, title))
            }
        }

        return box
    }

    private fun buildSidebarNavItem(key: String, title: String): View {
        val active = currentSection == key
        return UiKit.hBox(this).apply {
            background = UiKit.roundedBg(
                if (active) UiKit.BG_ACCENT_SUBTLE else Color.TRANSPARENT,
                6,
                this@MainActivity
            )
            setPadding(UiKit.dp(this@MainActivity, 12), UiKit.dp(this@MainActivity, 9), UiKit.dp(this@MainActivity, 12), UiKit.dp(this@MainActivity, 9))
            val lp = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            lp.bottomMargin = UiKit.dp(this@MainActivity, 2)
            layoutParams = lp
            if (active) {
                addView(View(this@MainActivity).apply {
                    background = UiKit.roundedBg(UiKit.ACCENT_BLUE, 2, this@MainActivity)
                    layoutParams = LinearLayout.LayoutParams(UiKit.dp(this@MainActivity, 3), UiKit.dp(this@MainActivity, 16)).apply {
                        rightMargin = UiKit.dp(this@MainActivity, 8)
                    }
                })
            }
            addView(UiKit.label(
                this@MainActivity,
                title,
                13f,
                if (active) UiKit.ACCENT_BLUE else UiKit.TEXT_PRIMARY,
                active
            ))
            setOnClickListener { navigateToSection(key) }
        }
    }

    private fun navigateToSection(sec: String) {
        currentSection = sec
        showMobileMenu = false
        showUserMenu = false
        statusMessage = ""
        if (sec.startsWith("res_")) {
            val rType = sec.removePrefix("res_")
            profileEditor.selectResourceTypeFromSidebar(rType)
            return
        }
        when (sec) {
            "dashboard" -> refreshDashboardData()
            "clients", "notifications" -> loadClientsData()
            "audit" -> loadAuditLogsData()
            "pairing" -> loadPairingData()
            "prereg" -> loadPreRegData()
            "account" -> loadAccountAndQuotasData()
            "access" -> loadMembersData()
            "invitations" -> loadInvitationsData()
            "security" -> loadProfileData()
            "admin_dashboard", "admin_accounts" -> loadGlobalAccountsData()
            "admin_users" -> loadGlobalUsersData()
            "admin_pending" -> loadPendingUsersData()
            "admin_settings" -> loadSystemSettingsData()
            else -> rebuildUi()
        }
    }

    private fun buildActiveSectionContainer(): View {
        val pad = UiKit.dp(this, if (isTabletLayout()) 24 else 14)
        val wrap = UiKit.vBox(this).apply {
            setPadding(
                pad + (if (isTabletLayout()) 0 else safeInsetLeftPx),
                pad,
                pad + safeInsetRightPx,
                pad + safeInsetBottomPx
            )
        }

        if (statusMessage.isNotEmpty()) {
            val banner = UiKit.hBox(this, 12).apply {
                background = UiKit.roundedBg(
                    if (statusIsError) UiKit.BADGE_DANGER_BG else UiKit.BADGE_SUCCESS_BG,
                    6,
                    this@MainActivity,
                    if (statusIsError) UiKit.BADGE_DANGER_FG else UiKit.BADGE_SUCCESS_FG,
                    1
                )
                layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                    bottomMargin = UiKit.dp(this@MainActivity, 12)
                }
            }
            banner.addView(UiKit.label(
                this,
                statusMessage,
                12f,
                if (statusIsError) UiKit.BADGE_DANGER_FG else UiKit.BADGE_SUCCESS_FG,
                true
            ).apply {
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
            })
            banner.addView(UiKit.button(this, "✕", Color.TRANSPARENT, if (statusIsError) UiKit.BADGE_DANGER_FG else UiKit.BADGE_SUCCESS_FG, true) {
                statusMessage = ""
                rebuildUi()
            })
            wrap.addView(banner)
        }

        when {
            currentSection == "dashboard" -> wrap.addView(buildDashboardView())
            currentSection.startsWith("res_") -> wrap.addView(profileEditor.buildView(this, isWideTabletLayout()))
            currentSection == "clients" -> wrap.addView(buildClientsView())
            currentSection == "notifications" -> wrap.addView(buildNotificationsView(null))
            currentSection == "audit" -> wrap.addView(buildAuditView())
            currentSection == "pairing" -> wrap.addView(buildPairingView())
            currentSection == "prereg" -> wrap.addView(buildPreRegView())
            currentSection == "account" -> wrap.addView(buildAccountSettingsView())
            currentSection == "access" -> wrap.addView(buildAccessMembersView())
            currentSection == "invitations" -> wrap.addView(buildInvitationsView())
            currentSection == "security" -> wrap.addView(buildSecurityView())
            currentSection == "admin_dashboard" -> wrap.addView(buildSuperAdminDashboardView())
            currentSection == "admin_accounts" -> wrap.addView(buildGlobalAccountsView())
            currentSection == "admin_users" -> wrap.addView(buildGlobalUsersView())
            currentSection == "admin_pending" -> wrap.addView(buildPendingUsersView())
            currentSection == "admin_settings" -> wrap.addView(buildSystemSettingsView())
        }
        return wrap
    }

    // ==================== 1. WEB /LOGIN VIEW ====================
    private fun buildAuthView(): View {
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
        val srvBtn = TextView(this).apply {
            text = "⚙ 服务器地址"
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f)
            setTextColor(Color.WHITE)
            background = UiKit.roundedBg(0x26FFFFFF, 6, this@MainActivity, 0x40FFFFFF, 1)
            setPadding(UiKit.dp(this@MainActivity, 10), UiKit.dp(this@MainActivity, 5), UiKit.dp(this@MainActivity, 10), UiKit.dp(this@MainActivity, 5))
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                rightMargin = UiKit.dp(this@MainActivity, 8)
            }
            setOnClickListener {
                showEndpointConfig = !showEndpointConfig
                rebuildUi()
            }
        }
        topBar.addView(srvBtn)
        topBar.addView(buildThemeModeToggle(true))
        outer.addView(topBar)

        val cardWidth = if (isTabletLayout()) UiKit.dp(this, 420) else ViewGroup.LayoutParams.MATCH_PARENT
        val authCard = UiKit.vBox(this, 24).apply {
            background = UiKit.roundedBg(UiKit.BG_CARD, 12, this@MainActivity, UiKit.BORDER_COLOR, 1)
            layoutParams = LinearLayout.LayoutParams(cardWidth, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                topMargin = UiKit.dp(this@MainActivity, 28)
            }
        }

        // Centered Logo & Title matching Web /login
        val brandCol = UiKit.vBox(this).apply {
            gravity = Gravity.CENTER_HORIZONTAL
            val logo = TextView(this@MainActivity).apply {
                text = "M"
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 22f)
                setTextColor(Color.WHITE)
                setTypeface(typeface, Typeface.BOLD)
                gravity = Gravity.CENTER
                background = UiKit.roundedBg(UiKit.ACCENT_BLUE, 12, this@MainActivity)
                layoutParams = LinearLayout.LayoutParams(UiKit.dp(this@MainActivity, 48), UiKit.dp(this@MainActivity, 48)).apply {
                    bottomMargin = UiKit.dp(this@MainActivity, 12)
                }
            }
            addView(logo)
            val titleText = when (authStep) {
                "login" -> "欢迎回来，请登录"
                "register" -> "创建一个新账户"
                "totp" -> "两步验证 (2FA)"
                "oobe" -> "系统首次启动 OOBE 初始化"
                else -> "欢迎来到 MINI ClassIsland"
            }
            val subText = when (authStep) {
                "login" -> if (useAdminLogin) "正在登录超级管理平台 (Admin API)" else "输入您的账号密码以继续"
                "register" -> "填写以下信息完成集控账号注册"
                "totp" -> "请输入验证器应用生成的 6 位动态验证码"
                "oobe" -> "配置数据库连接与初始超级管理员账号"
                else -> "输入您的邮箱或用户名以继续"
            }
            addView(UiKit.label(this@MainActivity, titleText, 19f, UiKit.TEXT_PRIMARY, true).apply {
                gravity = Gravity.CENTER
            })
            addView(UiKit.label(this@MainActivity, subText, 12f, UiKit.TEXT_SECONDARY).apply {
                gravity = Gravity.CENTER
                setPadding(0, UiKit.dp(this@MainActivity, 4), 0, UiKit.dp(this@MainActivity, 12))
            })
        }
        authCard.addView(brandCol)

        // Optional Server Endpoint Drawer
        if (showEndpointConfig) {
            val epBox = UiKit.subCard(this, 12)
            epBox.addView(UiKit.label(this, "服务器接口地址配置", 12f, UiKit.TEXT_PRIMARY, true))
            val mgmtEd = UiKit.input(this, "Management API 地址", ApiClient.managementUrl)
            val admEd = UiKit.input(this, "Admin API 地址", ApiClient.adminUrl)
            epBox.addView(UiKit.label(this, "集控后端地址 (Management API):", 11f, UiKit.TEXT_SECONDARY))
            epBox.addView(mgmtEd)
            epBox.addView(UiKit.label(this, "超管后端地址 (Admin API):", 11f, UiKit.TEXT_SECONDARY))
            epBox.addView(admEd)
            val epRow = UiKit.hBox(this)
            epRow.addView(UiKit.button(this, "保存地址", UiKit.ACCENT_BLUE, Color.WHITE, true) {
                ApiClient.setEndpoints(mgmtEd.text.toString(), admEd.text.toString())
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
                    this@MainActivity,
                    if (statusIsError) UiKit.BADGE_DANGER_FG else UiKit.BADGE_SUCCESS_FG,
                    1
                )
                layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                    bottomMargin = UiKit.dp(this@MainActivity, 12)
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

        when (authStep) {
            "email" -> {
                authCard.addView(UiKit.label(this, "邮箱地址", 12f, UiKit.TEXT_SECONDARY, true))
                val emailEd = UiKit.input(this, "name@example.com", authEmailInput)
                authCard.addView(emailEd)
                authCard.addView(UiKit.spacer(this, 8))

                authCard.addView(UiKit.button(this, "继续", UiKit.ACCENT_BLUE, Color.WHITE, false) {
                    val em = emailEd.text.toString().trim()
                    if (em.isEmpty()) {
                        setFeedback("请输入邮箱地址或用户名", true)
                        return@button
                    }
                    authEmailInput = em
                    if (!em.contains("@")) {
                        authUsernameInput = em
                        authStep = "login"
                        rebuildUi()
                        return@button
                    }
                    runAsync(
                        work = { ApiClient.request(ApiClient.managementUrl, "/user/mail/availability?email=${ApiClient.enc(em)}") },
                        done = { res ->
                            if (res.isOobe) {
                                authStep = "oobe"
                            } else if (res.ok && res.json.optBoolean("available", false)) {
                                authStep = "register"
                            } else {
                                authUsernameInput = em
                                authStep = "login"
                            }
                            rebuildUi()
                        }
                    )
                }.apply {
                    layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
                })

                authCard.addView(UiKit.divider(this, 14))
                val bottomLinks = UiKit.hBox(this)
                bottomLinks.addView(UiKit.button(this, "用户名密码直接登录", UiKit.BTN_MUTED, UiKit.TEXT_PRIMARY, true) {
                    authStep = "login"
                    useAdminLogin = false
                    rebuildUi()
                }.apply {
                    layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply {
                        rightMargin = UiKit.dp(this@MainActivity, 6)
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
            "login" -> {
                val modeBar = UiKit.hBox(this).apply {
                    layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                        bottomMargin = UiKit.dp(this@MainActivity, 10)
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
                        rightMargin = UiKit.dp(this@MainActivity, 6)
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

                authCard.addView(UiKit.label(this, "用户名或邮箱", 12f, UiKit.TEXT_SECONDARY, true))
                val userEd = UiKit.input(this, "请输入用户名或邮箱", authUsernameInput.ifEmpty { authEmailInput })
                authCard.addView(userEd)

                authCard.addView(UiKit.label(this, "密码", 12f, UiKit.TEXT_SECONDARY, true))
                val pwdEd = UiKit.input(this, "请输入密码", isPassword = true)
                authCard.addView(pwdEd)
                authCard.addView(UiKit.spacer(this, 8))

                authCard.addView(UiKit.button(this, "登录", UiKit.ACCENT_BLUE, Color.WHITE, false) {
                    val u = userEd.text.toString().trim()
                    val p = pwdEd.text.toString()
                    if (u.isEmpty() || p.isEmpty()) {
                        setFeedback("请输入用户名和密码", true)
                        return@button
                    }
                    authUsernameInput = u
                    val base = if (useAdminLogin) ApiClient.adminUrl else ApiClient.managementUrl
                    val body = JSONObject().put("username", u).put("password", p)
                    runAsync(
                        work = { ApiClient.request(base, "/user/auth", "POST", body) },
                        done = { res ->
                            if (res.isOobe) {
                                authStep = "oobe"
                                rebuildUi()
                            } else if (!res.ok) {
                                setFeedback(res.error, true)
                            } else if (res.json.optBoolean("totp_required", false)) {
                                totpSession = res.json.optString("totp_session")
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
                        topMargin = UiKit.dp(this@MainActivity, 10)
                    }
                }
                navRow.addView(UiKit.button(this, "← 返回上一步", UiKit.BTN_MUTED, UiKit.TEXT_SECONDARY, true) {
                    authStep = "email"
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
                val pEd = UiKit.input(this, "至少 6 位密码", isPassword = true)
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
                                authUsernameInput = uEd.text.toString().trim()
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

                authCard.addView(UiKit.button(this, "← 已有账户？返回登录", UiKit.BTN_MUTED, UiKit.TEXT_SECONDARY, true) {
                    authStep = "login"
                    rebuildUi()
                }.apply {
                    layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                        topMargin = UiKit.dp(this@MainActivity, 8)
                    }
                })
            }
            "totp" -> {
                authCard.addView(UiKit.label(this, if (totpUseRecovery) "8 位应急恢复码" else "6 位动态验证码", 12f, UiKit.TEXT_SECONDARY, true))
                val codeEd = UiKit.input(this, if (totpUseRecovery) "输入恢复码" else "000000")
                authCard.addView(codeEd)
                authCard.addView(UiKit.spacer(this, 8))

                authCard.addView(UiKit.button(this, "验证并登录", UiKit.ACCENT_BLUE, Color.WHITE, false) {
                    val base = if (useAdminLogin) ApiClient.adminUrl else ApiClient.managementUrl
                    val path = if (totpUseRecovery) "/user/2fa/totp/recover" else "/user/2fa/totp/verify"
                    val key = if (totpUseRecovery) "recovery_code" else "code"
                    val body = JSONObject().put("totp_session", totpSession).put(key, codeEd.text.toString().trim())
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
                        topMargin = UiKit.dp(this@MainActivity, 8)
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

    private fun onLoginSuccess(authJson: JSONObject) {
        ApiClient.token = authJson.optString("token")
        currentUser = authJson.optJSONObject("user") ?: JSONObject()
        isLoggedIn = true
        statusMessage = ""
        if (useAdminLogin) {
            isSuperAdminMode = true
            navigateToSection("admin_dashboard")
        } else {
            isSuperAdminMode = false
            currentAccountId = 0
            currentAccountName = ""
            loadAccountsListOnly()
        }
    }

    private fun loadAccountsListOnly() {
        runAsync(
            work = { ApiClient.request(ApiClient.managementUrl, "/account/list") },
            done = { res ->
                if (res.ok) {
                    accounts = res.json.optJSONArray("accounts") ?: JSONArray()
                }
                rebuildUi()
            }
        )
    }

    // ==================== 2. WEB / ACCOUNT SELECTION VIEW ====================
    private fun buildAccountSelectionView(): View {
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
            background = UiKit.roundedBg(UiKit.BG_CARD, 12, this@MainActivity, UiKit.BORDER_COLOR, 1)
            layoutParams = LinearLayout.LayoutParams(cardWidth, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                topMargin = UiKit.dp(this@MainActivity, 20)
            }
        }

        // Header matching web/app/page.tsx
        val headCol = UiKit.vBox(this).apply {
            gravity = Gravity.CENTER_HORIZONTAL
            addView(UiKit.label(this@MainActivity, "选择 MINI ClassIsland 账户", 19f, UiKit.TEXT_PRIMARY, true))
            addView(UiKit.label(this@MainActivity, "选择一个账户以进入集控管理面板", 12f, UiKit.TEXT_SECONDARY).apply {
                setPadding(0, UiKit.dp(this@MainActivity, 4), 0, UiKit.dp(this@MainActivity, 14))
            })
        }
        card.addView(headCol)

        if (statusMessage.isNotEmpty()) {
            val banner = UiKit.subCard(this, 10).apply {
                background = UiKit.roundedBg(
                    if (statusIsError) UiKit.BADGE_DANGER_BG else UiKit.BADGE_SUCCESS_BG,
                    6,
                    this@MainActivity
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

        // Account Cards List (44x44 blue icon box, Name, Slug pill, Active badge, ID, arrow)
        if (accounts.length() == 0) {
            card.addView(UiKit.label(this, "您当前还没有加入任何集控账户，请创建一个新账户或使用邀请码加入。", 12f, UiKit.TEXT_SECONDARY).apply {
                setPadding(0, UiKit.dp(this@MainActivity, 12), 0, UiKit.dp(this@MainActivity, 12))
            })
        } else {
            for (i in 0 until accounts.length()) {
                val acc = accounts.optJSONObject(i) ?: continue
                val accId = acc.optInt("id")
                val accName = acc.optString("name", "未命名账户")
                val accSlug = acc.optString("slug", "default")
                val isActive = acc.optBoolean("is_active", true)

                val itemCard = UiKit.hBox(this, 12).apply {
                    background = UiKit.roundedBg(UiKit.BG_CARD, 8, this@MainActivity, UiKit.BORDER_COLOR, 1)
                    layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                        bottomMargin = UiKit.dp(this@MainActivity, 8)
                    }
                    setOnClickListener {
                        currentAccountId = accId
                        currentAccountName = accName
                        isSuperAdminMode = false
                        isSuperAdminTakeover = false
                        navigateToSection("dashboard")
                    }
                }

                val iconBox = TextView(this).apply {
                    text = accName.take(1).uppercase()
                    setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
                    setTextColor(UiKit.ACCENT_BLUE)
                    setTypeface(typeface, Typeface.BOLD)
                    gravity = Gravity.CENTER
                    background = UiKit.roundedBg(UiKit.BG_ACCENT_SUBTLE, 8, this@MainActivity)
                    layoutParams = LinearLayout.LayoutParams(UiKit.dp(this@MainActivity, 44), UiKit.dp(this@MainActivity, 44)).apply {
                        rightMargin = UiKit.dp(this@MainActivity, 12)
                    }
                }
                itemCard.addView(iconBox)

                val midCol = UiKit.vBox(this).apply {
                    layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
                }
                val titleRow = UiKit.hBox(this)
                titleRow.addView(UiKit.label(this, accName, 14f, UiKit.TEXT_PRIMARY, true).apply {
                    setPadding(0, 0, UiKit.dp(this@MainActivity, 6), 0)
                })
                titleRow.addView(UiKit.badge(this, accSlug, "neutral").apply {
                    layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                        rightMargin = UiKit.dp(this@MainActivity, 4)
                    }
                })
                titleRow.addView(UiKit.badge(this, if (isActive) "活跃" else "停用", if (isActive) "success" else "danger"))
                midCol.addView(titleRow)
                midCol.addView(UiKit.label(this, "ID: $accId", 11f, UiKit.TEXT_TERTIARY).apply {
                    setPadding(0, UiKit.dp(this@MainActivity, 2), 0, 0)
                })
                itemCard.addView(midCol)

                itemCard.addView(UiKit.label(this, "→", 16f, UiKit.TEXT_TERTIARY, true))
                card.addView(itemCard)
            }
        }

        // Expandable Create Account or Join Invite Forms
        if (showCreateAccountForm) {
            val form = UiKit.subCard(this, 12)
            form.addView(UiKit.label(this, "创建新集控账户", 13f, UiKit.TEXT_PRIMARY, true))
            val nameEd = UiKit.input(this, "账户名称 (如 某某中学高一年级)")
            val slugEd = UiKit.input(this, "唯一标识 Slug (如 school-g1)")
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
                                val newId = res.json.optInt("id", 0)
                                if (newId > 0) {
                                    currentAccountId = newId
                                    currentAccountName = n
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
        btnRow.addView(UiKit.button(this, "+ 创建新账户", UiKit.ACCENT_BLUE, Color.WHITE, false) {
            showCreateAccountForm = !showCreateAccountForm
            showJoinInviteForm = false
            rebuildUi()
        }.apply {
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply {
                rightMargin = UiKit.dp(this@MainActivity, 6)
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
                topMargin = UiKit.dp(this@MainActivity, 8)
            }
        }
        bottomActRow.addView(UiKit.button(this, "超级管理平台", UiKit.BTN_MUTED, UiKit.ACCENT_BLUE, true) {
            isSuperAdminMode = true
            navigateToSection("admin_dashboard")
        }.apply {
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply {
                rightMargin = UiKit.dp(this@MainActivity, 6)
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

    // ==================== 3. WEB DASHBOARD VIEW ====================
    private fun refreshDashboardData() {
        if (currentAccountId <= 0) {
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
                    val r = ApiClient.request(ApiClient.managementUrl, "/account/$currentAccountId/api/v1/profiles/resources?type=$t")
                    resTotal += (r.json.optJSONArray("resources")?.length() ?: 0)
                }
                Triple(cRes, pRes, Pair(aRes, resTotal))
            },
            done = { (cRes, pRes, tail) ->
                val (aRes, resTotal) = tail
                val cArr = cRes.json.optJSONArray("clients") ?: JSONArray()
                clientsArray = cArr
                dashTotalClients = cArr.length()
                var on = 0
                for (i in 0 until cArr.length()) {
                    if (cArr.optJSONObject(i)?.optString("status") == "online") on++
                }
                dashOnline = on
                dashPendingPairing = pRes.json.optJSONArray("requests")?.length() ?: 0
                dashRecentAudits = aRes.json.optJSONArray("logs")?.length() ?: 0
                dashTotalResources = resTotal
                rebuildUi()
            }
        )
    }

    private fun buildDashboardView(): View {
        val box = UiKit.vBox(this)

        if (isSuperAdminTakeover) {
            val banner = UiKit.card(this, 12).apply {
                background = UiKit.roundedBg(UiKit.BADGE_WARNING_BG, 8, this@MainActivity, UiKit.BADGE_WARNING_FG, 1)
            }
            val r = UiKit.hBox(this)
            r.addView(UiKit.label(
                this,
                "超级管理员接管模式 · 当前正在管理账户 #$currentAccountId ($currentAccountName)",
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
            addView(UiKit.button(this@MainActivity, "下载通用配置 (仅接口地址)", UiKit.ACCENT_BLUE, Color.WHITE, true) {
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
            addView(UiKit.button(this@MainActivity, "刷新", UiKit.BTN_MUTED, UiKit.TEXT_PRIMARY, true) {
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
            setPadding(0, UiKit.dp(this@MainActivity, 4), 0, UiKit.dp(this@MainActivity, 10))
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
                addView(UiKit.label(this@MainActivity, stepTitle, 13f, UiKit.TEXT_PRIMARY, true))
                addView(UiKit.label(this@MainActivity, stepDesc, 12f, UiKit.TEXT_SECONDARY).apply {
                    setPadding(0, UiKit.dp(this@MainActivity, 2), 0, 0)
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
    private fun loadClientsData() {
        runAsync(
            work = { ApiClient.request(ApiClient.managementUrl, "/account/$currentAccountId/client/list") },
            done = { r ->
                if (r.ok) {
                    clientsArray = r.json.optJSONArray("clients") ?: JSONArray()
                    if (activeClient == null && clientsArray.length() > 0) {
                        selectClientForInspector(clientsArray.optJSONObject(0))
                        return@runAsync
                    }
                }
                rebuildUi()
            }
        )
    }

    private fun selectClientForInspector(c: JSONObject?) {
        activeClient = c
        if (c == null) {
            rebuildUi()
            return
        }
        val uid = c.optString("uid")
        runAsync(
            work = {
                val p = ApiClient.request(ApiClient.managementUrl, "/account/$currentAccountId/client/${ApiClient.enc(uid)}/profile")
                val s = ApiClient.request(ApiClient.managementUrl, "/account/$currentAccountId/client/${ApiClient.enc(uid)}/config-uploads")
                Pair(p, s)
            },
            done = { (p, s) ->
                clientProfileObj = p.json.optJSONObject("profile") ?: JSONObject()
                clientSnapshots = s.json.optJSONArray("uploads") ?: JSONArray()
                rebuildUi()
            }
        )
    }

    private fun buildClientsView(): View {
        val box = UiKit.vBox(this)

        box.addView(UiKit.pageHeader(this, "客户端管理", "查看并管理所有已注册的 ClassIsland 实例、分配档案与下发远程指令") {
            addView(UiKit.button(this@MainActivity, "刷新列表", UiKit.BTN_MUTED, UiKit.TEXT_PRIMARY, true) {
                loadClientsData()
            })
        })

        // 3 Stat Cards matching web/app/(dashboard)/clients/page.tsx
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
                    rightMargin = UiKit.dp(this@MainActivity, 12)
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

    private fun buildClientListCard(): View {
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

        // Batch Actions Bar
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
                setPadding(0, UiKit.dp(this@MainActivity, 12), 0, UiKit.dp(this@MainActivity, 12))
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
                    this@MainActivity,
                    if (isSelected) UiKit.ACCENT_BLUE else UiKit.BORDER_COLOR,
                    1
                )
                layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                    bottomMargin = UiKit.dp(this@MainActivity, 6)
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
                setPadding(0, UiKit.dp(this@MainActivity, 2), 0, 0)
            })
            row.addView(infoCol)
            item.addView(row)
            c.addView(item)
        }
        return c
    }

    private fun buildClientInspectorCard(): View {
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
            addView(UiKit.label(this@MainActivity, cur.optString("display_name").ifEmpty { cur.optString("client_id") }, 15f, UiKit.TEXT_PRIMARY, true))
            addView(UiKit.label(this@MainActivity, "UID: $uid", 11f, UiKit.TEXT_SECONDARY))
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
                c.addView(UiKit.label(this, "客户端标识符 (ClientId)", 12f, UiKit.TEXT_SECONDARY, true))
                val cidEd = UiKit.input(this, "班级标识", cur.optString("client_id"))
                c.addView(cidEd)
                c.addView(UiKit.button(this, "保存基本设置", UiKit.ACCENT_BLUE, Color.WHITE, true) {
                    val b = JSONObject().put("display_name", dnEd.text.toString()).put("client_id", cidEd.text.toString())
                    runAsync(
                        work = { ApiClient.request(ApiClient.managementUrl, "/account/$currentAccountId/client/${ApiClient.enc(uid)}", "PUT", b) },
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
                        work = { ApiClient.request(ApiClient.managementUrl, "/account/$currentAccountId/client/${ApiClient.enc(uid)}/command/force-sync") },
                        done = { setFeedback("已下发强制同步指令") }
                    )
                })
                cmdRow.addView(UiKit.button(this, "重启客户端", UiKit.BTN_MUTED, UiKit.TEXT_PRIMARY, true) {
                    runAsync(
                        work = { ApiClient.request(ApiClient.managementUrl, "/account/$currentAccountId/client/${ApiClient.enc(uid)}/command/restart") },
                        done = { setFeedback("已下发重启指令") }
                    )
                })
                cmdRow.addView(UiKit.button(this, "断开连接", UiKit.BTN_MUTED, UiKit.TEXT_PRIMARY, true) {
                    runAsync(
                        work = { ApiClient.request(ApiClient.managementUrl, "/account/$currentAccountId/client/${ApiClient.enc(uid)}/disconnect", "POST", JSONObject()) },
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
                    "cp_resource_name" to "课表配置 (ClassPlan)",
                    "tl_resource_name" to "时间表配置 (TimeLayout)",
                    "sub_resource_name" to "科目配置 (Subjects)",
                    "pol_resource_name" to "策略配置 (Policy)",
                    "ds_resource_name" to "默认设置 (DefaultSettings)",
                    "com_resource_name" to "组件配置 (Components)",
                    "cred_resource_name" to "凭据配置 (Credentials)"
                )
                val editors = mutableMapOf<String, android.widget.EditText>()
                for ((fieldKey, fieldTitle) in fields) {
                    c.addView(UiKit.label(this, fieldTitle, 12f, UiKit.TEXT_SECONDARY, true))
                    val ed = UiKit.input(this, "资源名称 (留空使用默认)", clientProfileObj.optString(fieldKey))
                    editors[fieldKey] = ed
                    c.addView(ed)
                }
                c.addView(UiKit.spacer(this, 6))
                c.addView(UiKit.button(this, "保存档案分配", UiKit.ACCENT_BLUE, Color.WHITE, false) {
                    val body = JSONObject()
                    for ((k, ed) in editors) body.put(k, ed.text.toString().trim())
                    runAsync(
                        work = { ApiClient.request(ApiClient.managementUrl, "/account/$currentAccountId/client/${ApiClient.enc(uid)}/profile", "PUT", body) },
                        done = { r -> setFeedback(if (r.ok) "已保存客户端档案分配" else r.error, !r.ok) }
                    )
                })
            }
            "snapshots" -> {
                c.addView(UiKit.label(this, "向客户端请求上传当前配置快照:", 12f, UiKit.TEXT_SECONDARY, true))
                val typeLabels = listOf(1 to "课表", 2 to "时间表", 3 to "科目", 4 to "策略", 5 to "设置", 6 to "组件")
                val sRow = UiKit.hBox(this)
                for ((tCode, tName) in typeLabels) {
                    sRow.addView(UiKit.button(this, "抓取$tName", UiKit.BTN_MUTED, UiKit.TEXT_PRIMARY, true) {
                        runAsync(
                            work = { ApiClient.request(ApiClient.managementUrl, "/account/$currentAccountId/client/${ApiClient.enc(uid)}/command/get-config?config_type=$tCode") },
                            done = { setFeedback("已下发抓取 [$tName] 指令") }
                        )
                    })
                }
                c.addView(sRow)
                for (i in 0 until clientSnapshots.length()) {
                    val snap = clientSnapshots.optJSONObject(i) ?: continue
                    val sr = UiKit.subCard(this, 10)
                    val h = UiKit.hBox(this)
                    h.addView(UiKit.label(this, "类型 #${snap.optInt("config_type")} · ${formatTs(snap.optLong("created_at"))}", 12f, UiKit.TEXT_PRIMARY).apply {
                        layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
                    })
                    h.addView(UiKit.button(this, "查看内容", UiKit.BTN_MUTED, UiKit.ACCENT_BLUE, true) {
                        snapshotDetailText = snap.optString("payload")
                        rebuildUi()
                    })
                    sr.addView(h)
                    c.addView(sr)
                }
                if (snapshotDetailText.isNotEmpty()) {
                    c.addView(UiKit.input(this, "快照 JSON", snapshotDetailText, multiLine = true))
                }
            }
            "notify" -> {
                c.addView(buildNotificationsView(uid))
            }
        }
        return c
    }

    // ==================== 5. NOTIFICATIONS PUSH (WEB BANNER PREVIEW) ====================
    private fun buildNotificationsView(fixedClientUid: String?): View {
        val box = UiKit.vBox(this)
        if (fixedClientUid == null) {
            box.addView(UiKit.pageHeader(this, "通知推送", "向指定或全体在线 ClassIsland 终端发送实时桌面横幅与语音提醒"))
        }

        // Web ClassIsland Live Notification Banner Preview (#1F1F1F normal / #FDE7E9 emergency)
        val previewCard = UiKit.card(this, 16)
        previewCard.addView(UiKit.label(this, "ClassIsland 提醒横幅预览", 12f, UiKit.TEXT_SECONDARY, true))
        val bannerBox = UiKit.vBox(this, 16).apply {
            background = UiKit.roundedBg(
                if (notifyEmergency) 0xFFFDE7E9.toInt() else 0xFF1F1F1F.toInt(),
                8,
                this@MainActivity,
                if (notifyEmergency) UiKit.ACCENT_RED else 0xFF333333.toInt(),
                1
            )
            gravity = Gravity.CENTER_HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                topMargin = UiKit.dp(this@MainActivity, 8)
            }
        }
        val fgColor = if (notifyEmergency) 0xFFD13438.toInt() else Color.WHITE
        bannerBox.addView(UiKit.label(this, notifyMask.ifEmpty { "提醒标题" }, 16f, fgColor, true).apply {
            gravity = Gravity.CENTER
        })
        bannerBox.addView(UiKit.label(this, notifyContent.ifEmpty { "提醒正文内容" }, 13f, fgColor).apply {
            gravity = Gravity.CENTER
            setPadding(0, UiKit.dp(this@MainActivity, 4), 0, 0)
        })
        previewCard.addView(bannerBox)
        box.addView(previewCard)

        val c = UiKit.card(this, 16)
        c.addView(UiKit.label(this, "通知内容与特效参数", 14f, UiKit.TEXT_PRIMARY, true))
        c.addView(UiKit.spacer(this, 6))

        c.addView(UiKit.label(this, "遮罩标题 (MessageMask)", 12f, UiKit.TEXT_SECONDARY, true))
        val maskEd = UiKit.input(this, "遮罩标题", notifyMask)
        c.addView(maskEd)

        c.addView(UiKit.label(this, "通知正文 (MessageContent)", 12f, UiKit.TEXT_SECONDARY, true))
        val contentEd = UiKit.input(this, "通知正文内容", notifyContent)
        c.addView(contentEd)

        val numRow = UiKit.hBox(this)
        val durEd = UiKit.input(this, "持续秒数", "$notifyDuration").apply {
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply {
                rightMargin = UiKit.dp(this@MainActivity, 8)
            }
        }
        val repEd = UiKit.input(this, "重复次数", "$notifyRepeat").apply {
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        }
        numRow.addView(durEd)
        numRow.addView(repEd)
        c.addView(numRow)

        c.addView(UiKit.switchRow(this, "紧急通知样式 (IsEmergency)", "使用红色高亮紧急警示横幅", notifyEmergency) {
            notifyEmergency = it
            notifyMask = maskEd.text.toString()
            notifyContent = contentEd.text.toString()
            rebuildUi()
        })
        c.addView(UiKit.switchRow(this, "启用语音播报 (IsSpeechEnabled)", "使用系统或 Edge TTS 朗读通知正文", notifySpeech) {
            notifySpeech = it
        })
        c.addView(UiKit.switchRow(this, "启用提醒音效 (IsSoundEnabled)", "播放通知提示音", notifySound) {
            notifySound = it
        })
        c.addView(UiKit.switchRow(this, "启用全屏醒目特效 (IsEffectEnabled)", "显示波纹光效提醒", notifyEffect) {
            notifyEffect = it
        })
        c.addView(UiKit.switchRow(this, "窗口强制置顶 (IsTopmost)", "提醒期间置顶于所有窗口上方", notifyTopmost) {
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
    private fun loadAuditLogsData() {
        val q = if (auditEventFilter == "all") "?limit=200" else "?limit=200&event=$auditEventFilter"
        runAsync(
            work = { ApiClient.request(ApiClient.managementUrl, "/account/$currentAccountId/audit/list$q") },
            done = { r ->
                auditLogs = r.json.optJSONArray("logs") ?: JSONArray()
                rebuildUi()
            }
        )
    }

    private fun buildAuditView(): View {
        val box = UiKit.vBox(this)
        box.addView(UiKit.pageHeader(this, "审计日志", "查看客户端上报的敏感操作、崩溃与集控同步事件") {
            addView(UiKit.button(this@MainActivity, "刷新", UiKit.BTN_MUTED, UiKit.TEXT_PRIMARY, true) {
                loadAuditLogsData()
            })
            addView(UiKit.button(this@MainActivity, "清空日志", UiKit.BADGE_DANGER_BG, UiKit.BADGE_DANGER_FG, true) {
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
                setPadding(0, UiKit.dp(this@MainActivity, 12), 0, UiKit.dp(this@MainActivity, 12))
            })
        }

        for (i in 0 until auditLogs.length()) {
            val log = auditLogs.optJSONObject(i) ?: continue
            val row = UiKit.subCard(this, 10)
            val h = UiKit.hBox(this)
            val ev = log.optInt("event", 0)
            h.addView(UiKit.badge(this, AUDIT_EVENT_LABELS[ev] ?: "事件#$ev", "info"))
            h.addView(UiKit.label(
                this,
                "  ${log.optString("client_id")} · ${formatTs(log.optLong("timestamp_utc"))}",
                12f,
                UiKit.TEXT_PRIMARY,
                true
            ).apply {
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
            })
            h.addView(UiKit.button(this, "查看载荷", UiKit.BTN_MUTED, UiKit.TEXT_SECONDARY, true) {
                auditPayloadPreview = log.optString("payload", "{}")
                rebuildUi()
            })
            row.addView(h)
            c.addView(row)
        }
        box.addView(c)
        return box
    }

    // ==================== 7. PAIRING & PRE-REGISTRATION ====================
    private fun loadPairingData() {
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

    private fun buildPairingView(): View {
        val box = UiKit.vBox(this)
        box.addView(UiKit.pageHeader(this, "配对码管理", "审批客户端发起的 6 位数字配对准入请求与配置注册策略") {
            addView(UiKit.button(this@MainActivity, "刷新", UiKit.BTN_MUTED, UiKit.TEXT_PRIMARY, true) {
                loadPairingData()
            })
        })

        val modeCard = UiKit.card(this, 16)
        modeCard.addView(UiKit.label(this, "客户端接入注册策略", 14f, UiKit.TEXT_PRIMARY, true))
        modeCard.addView(UiKit.spacer(this, 6))
        val curMode = accountInfoObj.optString("registration_mode", "open")
        val modeOpts = listOf(
            "open" to "开放自动注册 (open)",
            "pairing" to "6位配对码审批 (pairing)",
            "pre_reg" to "仅限预注册白名单 (pre_reg)"
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
                setPadding(0, UiKit.dp(this@MainActivity, 12), 0, UiKit.dp(this@MainActivity, 12))
            })
        }
        for (i in 0 until pairingList.length()) {
            val req = pairingList.optJSONObject(i) ?: continue
            val item = UiKit.subCard(this, 12)
            val h = UiKit.hBox(this)
            val id = req.optInt("id")
            val code = req.optString("pairing_code")
            val st = req.optString("status")
            h.addView(UiKit.label(this, code, 18f, UiKit.ACCENT_BLUE, true).apply {
                setPadding(0, 0, UiKit.dp(this@MainActivity, 10), 0)
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

    private fun loadPreRegData() {
        runAsync(
            work = { ApiClient.request(ApiClient.managementUrl, "/account/$currentAccountId/pre-registration/list") },
            done = { r ->
                preRegList = r.json.optJSONArray("items") ?: JSONArray()
                rebuildUi()
            }
        )
    }

    private fun buildPreRegView(): View {
        val box = UiKit.vBox(this)
        box.addView(UiKit.pageHeader(this, "预注册客户端", "预先登记班级终端白名单并生成专属 ManagementPreset.json"))

        val createCard = UiKit.card(this, 16)
        createCard.addView(UiKit.label(this, "添加预注册客户端", 14f, UiKit.TEXT_PRIMARY, true))
        val labelEd = UiKit.input(this, "备注名称 (如 高一1班一体机)")
        val cidEd = UiKit.input(this, "班级标识 (ClassIdentity，如 G1-C1)")
        val macEd = UiKit.input(this, "MAC 地址 (可选，如 00:11:22:33:44:55)")
        createCard.addView(labelEd)
        createCard.addView(cidEd)
        createCard.addView(macEd)
        createCard.addView(UiKit.button(this, "+ 添加预注册客户端", UiKit.ACCENT_BLUE, Color.WHITE, false) {
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
            val itemId = it.optInt("id")
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
    private fun loadAccountAndQuotasData() {
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

    private fun buildAccountSettingsView(): View {
        val box = UiKit.vBox(this)
        box.addView(UiKit.pageHeader(this, "账户概览", "管理集控账户基本信息、API 密钥与资源配额使用量"))

        val c = UiKit.card(this, 16)
        c.addView(UiKit.label(this, "基本信息", 14f, UiKit.TEXT_PRIMARY, true))
        c.addView(UiKit.label(this, "账户名称", 12f, UiKit.TEXT_SECONDARY, true))
        val nameEd = UiKit.input(this, "账户名称", accountInfoObj.optString("name", currentAccountName))
        c.addView(nameEd)
        c.addView(UiKit.label(this, "唯一标识 (Slug)", 12f, UiKit.TEXT_SECONDARY, true))
        val slugEd = UiKit.input(this, "唯一标识 (slug)", accountInfoObj.optString("slug"))
        c.addView(slugEd)

        val r = UiKit.hBox(this)
        r.addView(UiKit.button(this, "保存更改", UiKit.ACCENT_BLUE, Color.WHITE, false) {
            val b = JSONObject().put("name", nameEd.text.toString()).put("slug", slugEd.text.toString())
            runAsync(
                work = { ApiClient.request(ApiClient.managementUrl, "/account/$currentAccountId/info", "PUT", b) },
                done = {
                    currentAccountName = nameEd.text.toString()
                    loadAccountAndQuotasData()
                }
            )
        })
        r.addView(UiKit.button(this, "重新生成 API Key", UiKit.BTN_MUTED, UiKit.TEXT_PRIMARY, false) {
            runAsync(
                work = { ApiClient.request(ApiClient.managementUrl, "/account/$currentAccountId/apikey/regenerate", "POST", JSONObject()) },
                done = { loadAccountAndQuotasData() }
            )
        })
        c.addView(r)
        c.addView(UiKit.divider(this, 12))
        c.addView(UiKit.label(this, "API Key: ${accountInfoObj.optString("api_key", "-")}", 12f, UiKit.TEXT_SECONDARY))
        box.addView(c)

        val qCard = UiKit.card(this, 16)
        qCard.addView(UiKit.label(this, "账户配额使用情况", 14f, UiKit.TEXT_PRIMARY, true))
        for (i in 0 until quotasList.length()) {
            val q = quotasList.optJSONObject(i) ?: continue
            val qr = UiKit.subCard(this, 10)
            qr.addView(UiKit.label(
                this,
                "${q.optString("quota_type")}: ${q.optInt("current_value")} / ${q.optInt("limit_value")}",
                13f,
                UiKit.TEXT_PRIMARY,
                true
            ))
            qCard.addView(qr)
        }
        box.addView(qCard)
        return box
    }

    private fun loadMembersData() {
        runAsync(
            work = { ApiClient.request(ApiClient.managementUrl, "/account/$currentAccountId/access") },
            done = { r ->
                membersList = r.json.optJSONArray("members") ?: JSONArray()
                rebuildUi()
            }
        )
    }

    private fun buildAccessMembersView(): View {
        val box = UiKit.vBox(this)
        box.addView(UiKit.pageHeader(this, "访问控制", "管理当前集控账户的协作成员与角色权限"))

        val c = UiKit.card(this, 16)
        c.addView(UiKit.label(this, "账户成员列表 (${membersList.length()})", 14f, UiKit.TEXT_PRIMARY, true))
        for (i in 0 until membersList.length()) {
            val m = membersList.optJSONObject(i) ?: continue
            val uid = m.optInt("user_id")
            val curRole = m.optString("role")
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
                        work = { ApiClient.request(ApiClient.managementUrl, "/account/$currentAccountId/access/$uid", "PUT", JSONObject().put("role", rn)) },
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

    private fun loadInvitationsData() {
        runAsync(
            work = { ApiClient.request(ApiClient.managementUrl, "/account/$currentAccountId/invitations") },
            done = { r ->
                invitationsList = r.json.optJSONArray("invitations") ?: JSONArray()
                rebuildUi()
            }
        )
    }

    private fun buildInvitationsView(): View {
        val box = UiKit.vBox(this)
        box.addView(UiKit.pageHeader(this, "邀请管理", "创建成员邀请码或输入邀请码加入其他集控账户") {
            addView(UiKit.button(this@MainActivity, "+ 创建邀请码", UiKit.ACCENT_BLUE, Color.WHITE, true) {
                val b = JSONObject().put("role", "member").put("max_uses", 10)
                runAsync(
                    work = { ApiClient.request(ApiClient.managementUrl, "/account/$currentAccountId/invitations", "POST", b) },
                    done = { loadInvitationsData() }
                )
            })
        })

        val joinCard = UiKit.card(this, 16)
        joinCard.addView(UiKit.label(this, "使用邀请码加入其他账户", 14f, UiKit.TEXT_PRIMARY, true))
        val joinRow = UiKit.hBox(this)
        val joinEd = UiKit.input(this, "输入邀请码").apply {
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply {
                rightMargin = UiKit.dp(this@MainActivity, 8)
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
            val invId = inv.optInt("id")
            val row = UiKit.subCard(this, 10)
            val h = UiKit.hBox(this)
            h.addView(UiKit.label(
                this,
                "${inv.optString("code")} · 角色: ${inv.optString("role")} · 已用: ${inv.optInt("used_count")}/${inv.optInt("max_uses")}",
                13f,
                UiKit.TEXT_PRIMARY,
                true
            ).apply {
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
            })
            h.addView(UiKit.button(this, "撤销", UiKit.BADGE_DANGER_BG, UiKit.BADGE_DANGER_FG, true) {
                runAsync(
                    work = { ApiClient.request(ApiClient.managementUrl, "/account/$currentAccountId/invitations/$invId", "DELETE") },
                    done = { loadInvitationsData() }
                )
            })
            row.addView(h)
            c.addView(row)
        }
        box.addView(c)
        return box
    }

    private fun loadProfileData() {
        runAsync(
            work = { ApiClient.request(ApiClient.managementUrl, "/user/profile") },
            done = { r ->
                if (r.ok) currentUser = r.json
                rebuildUi()
            }
        )
    }

    private fun buildSecurityView(): View {
        val box = UiKit.vBox(this)
        box.addView(UiKit.pageHeader(this, "安全设置", "管理您的个人资料、登录密码与两步验证 (2FA TOTP)"))

        val profileCard = UiKit.card(this, 16)
        profileCard.addView(UiKit.label(this, "个人资料", 14f, UiKit.TEXT_PRIMARY, true))
        profileCard.addView(UiKit.label(this, "显示昵称", 12f, UiKit.TEXT_SECONDARY, true))
        val dnEd = UiKit.input(this, "显示昵称", currentUser.optString("display_name"))
        profileCard.addView(dnEd)
        profileCard.addView(UiKit.label(this, "电子邮箱", 12f, UiKit.TEXT_SECONDARY, true))
        val emEd = UiKit.input(this, "邮箱", currentUser.optString("email"))
        profileCard.addView(emEd)
        profileCard.addView(UiKit.button(this, "保存个人资料", UiKit.ACCENT_BLUE, Color.WHITE, false) {
            val b = JSONObject().apply {
                put("display_name", dnEd.text.toString())
                put("email", emEd.text.toString())
                put("username", currentUser.optString("username"))
            }
            runAsync(
                work = { ApiClient.request(ApiClient.managementUrl, "/user/profile", "PUT", b) },
                done = { r -> setFeedback(if (r.ok) "个人资料已更新" else r.error, !r.ok) }
            )
        })
        box.addView(profileCard)

        val pwdCard = UiKit.card(this, 16)
        pwdCard.addView(UiKit.label(this, "修改登录密码", 14f, UiKit.TEXT_PRIMARY, true))
        val oldPwd = UiKit.input(this, "当前密码", isPassword = true)
        val newPwd = UiKit.input(this, "新密码", isPassword = true)
        pwdCard.addView(oldPwd)
        pwdCard.addView(newPwd)
        pwdCard.addView(UiKit.button(this, "更新密码", UiKit.ACCENT_BLUE, Color.WHITE, false) {
            val b = JSONObject().put("old_password", oldPwd.text.toString()).put("new_password", newPwd.text.toString())
            runAsync(
                work = { ApiClient.request(ApiClient.managementUrl, "/user/security/password", "POST", b) },
                done = { r -> setFeedback(if (r.ok) "密码已修改" else r.error, !r.ok) }
            )
        })
        box.addView(pwdCard)

        val totpCard = UiKit.card(this, 16)
        val totpOn = currentUser.optBoolean("totp_enabled", false)
        val tRow = UiKit.hBox(this)
        tRow.addView(UiKit.label(this, "两步验证 (2FA TOTP)", 14f, UiKit.TEXT_PRIMARY, true).apply {
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
                            rebuildUi()
                        }
                    }
                )
            })
            if (totpEnableSecret.isNotEmpty()) {
                totpCard.addView(UiKit.label(this, "密钥: $totpEnableSecret\nURI: $totpEnableUri", 12f, UiKit.TEXT_SECONDARY))
                val codeEd = UiKit.input(this, "输入验证器 6 位动态码确认绑定")
                totpCard.addView(codeEd)
                totpCard.addView(UiKit.button(this, "确认开启 2FA", UiKit.ACCENT_BLUE, Color.WHITE, true) {
                    runAsync(
                        work = { ApiClient.request(ApiClient.managementUrl, "/user/2fa/totp/confirm", "POST", JSONObject().put("code", codeEd.text.toString().trim())) },
                        done = { r ->
                            if (r.ok) {
                                totpRecoveryCodesText = (r.json.optJSONArray("recovery_codes") ?: JSONArray()).toString()
                                loadProfileData()
                            } else {
                                setFeedback(r.error, true)
                            }
                        }
                    )
                })
            }
            if (totpRecoveryCodesText.isNotEmpty()) {
                totpCard.addView(UiKit.label(this, "应急恢复码: $totpRecoveryCodesText", 12f, UiKit.ACCENT_GREEN, true))
            }
        }
        box.addView(totpCard)
        return box
    }

    // ==================== 9. SUPERADMIN SHELL VIEWS ====================
    private fun loadGlobalAccountsData() {
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

    private fun buildSuperAdminDashboardView(): View {
        val box = UiKit.vBox(this)
        box.addView(UiKit.pageHeader(this, "全局仪表盘", "MINI ClassIsland 超级管理平台全局运行指标") {
            addView(UiKit.button(this@MainActivity, "刷新", UiKit.BTN_MUTED, UiKit.TEXT_PRIMARY, true) {
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

    private fun buildGlobalAccountsView(): View {
        val box = UiKit.vBox(this)
        box.addView(UiKit.pageHeader(this, "账户与配额", "管理全平台集控账户，支持一键进入指定集控账户进行接管编辑") {
            addView(UiKit.button(this@MainActivity, "刷新", UiKit.BTN_MUTED, UiKit.TEXT_PRIMARY, true) {
                loadGlobalAccountsData()
            })
        })
        box.addView(buildGlobalAccountsCardContent())
        return box
    }

    private fun buildGlobalAccountsCardContent(): View {
        val c = UiKit.card(this, 16)
        c.addView(UiKit.label(this, "全平台集控账户列表 (${globalAccountsList.length()})", 14f, UiKit.TEXT_PRIMARY, true))
        for (i in 0 until globalAccountsList.length()) {
            val acc = globalAccountsList.optJSONObject(i) ?: continue
            val accId = acc.optInt("id")
            val accName = acc.optString("name")
            val row = UiKit.subCard(this, 12)
            val h = UiKit.hBox(this)
            h.addView(UiKit.label(
                this,
                "#$accId  $accName (${acc.optString("slug")})",
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

    private fun loadGlobalUsersData() {
        runAsync(
            work = { ApiClient.request(ApiClient.adminUrl, "/user?limit=100") },
            done = { r ->
                globalUsersList = r.json.optJSONArray("users") ?: JSONArray()
                rebuildUi()
            }
        )
    }

    private fun buildGlobalUsersView(): View {
        val box = UiKit.vBox(this)
        box.addView(UiKit.pageHeader(this, "用户管理", "管理全平台注册用户状态与重置两步验证"))

        val c = UiKit.card(this, 16)
        c.addView(UiKit.label(this, "用户列表 (${globalUsersList.length()})", 14f, UiKit.TEXT_PRIMARY, true))
        for (i in 0 until globalUsersList.length()) {
            val u = globalUsersList.optJSONObject(i) ?: continue
            val uid = u.optInt("id")
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
            h.addView(UiKit.badge(this, u.optString("role", "user"), "info"))
            h.addView(UiKit.button(
                this,
                if (active) "禁用" else "启用",
                if (active) UiKit.BADGE_DANGER_BG else UiKit.BADGE_SUCCESS_BG,
                if (active) UiKit.BADGE_DANGER_FG else UiKit.BADGE_SUCCESS_FG,
                true
            ) {
                runAsync(
                    work = { ApiClient.request(ApiClient.adminUrl, "/user/$uid", "PUT", JSONObject().put("is_active", !active)) },
                    done = { loadGlobalUsersData() }
                )
            })
            if (u.optBoolean("totp_enabled", false)) {
                h.addView(UiKit.button(this, "重置 2FA", UiKit.BTN_MUTED, UiKit.TEXT_PRIMARY, true) {
                    runAsync(
                        work = { ApiClient.request(ApiClient.adminUrl, "/user/$uid/disable-totp", "POST", JSONObject()) },
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

    private fun loadPendingUsersData() {
        runAsync(
            work = { ApiClient.request(ApiClient.adminUrl, "/user/pending?limit=100") },
            done = { r ->
                pendingUsersList = r.json.optJSONArray("users") ?: JSONArray()
                rebuildUi()
            }
        )
    }

    private fun buildPendingUsersView(): View {
        val box = UiKit.vBox(this)
        box.addView(UiKit.pageHeader(this, "注册审核", "审批待处理的平台新用户注册申请"))

        val c = UiKit.card(this, 16)
        c.addView(UiKit.label(this, "待审批列表 (${pendingUsersList.length()})", 14f, UiKit.TEXT_PRIMARY, true))
        if (pendingUsersList.length() == 0) {
            c.addView(UiKit.label(this, "当前没有待审核的注册申请", 12f, UiKit.TEXT_TERTIARY).apply {
                setPadding(0, UiKit.dp(this@MainActivity, 12), 0, UiKit.dp(this@MainActivity, 12))
            })
        }
        for (i in 0 until pendingUsersList.length()) {
            val u = pendingUsersList.optJSONObject(i) ?: continue
            val uid = u.optInt("id")
            val item = UiKit.subCard(this, 12)
            val h = UiKit.hBox(this)
            h.addView(UiKit.label(this, "${u.optString("username")} (${u.optString("email")})", 13f, UiKit.TEXT_PRIMARY, true).apply {
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
            })
            h.addView(UiKit.button(this, "批准", UiKit.ACCENT_BLUE, Color.WHITE, true) {
                runAsync(
                    work = { ApiClient.request(ApiClient.adminUrl, "/user/$uid/approve", "POST", JSONObject()) },
                    done = { loadPendingUsersData() }
                )
            })
            h.addView(UiKit.button(this, "拒绝", UiKit.BADGE_DANGER_BG, UiKit.BADGE_DANGER_FG, true) {
                runAsync(
                    work = { ApiClient.request(ApiClient.adminUrl, "/user/$uid/reject", "POST", JSONObject()) },
                    done = { loadPendingUsersData() }
                )
            })
            item.addView(h)
            c.addView(item)
        }
        box.addView(c)
        return box
    }

    private fun loadSystemSettingsData() {
        runAsync(
            work = { ApiClient.request(ApiClient.adminUrl, "/settings") },
            done = { r ->
                sysSettingsObj = r.json.optJSONObject("settings") ?: JSONObject()
                rebuildUi()
            }
        )
    }

    private fun buildSystemSettingsView(): View {
        val box = UiKit.vBox(this)
        box.addView(UiKit.pageHeader(this, "系统设置", "配置全局开放注册策略、账户数量限制与系统公告"))

        val c = UiKit.card(this, 16)
        var regOpen = sysSettingsObj.optString("registration_open", "true") != "false"
        var reqApproval = sysSettingsObj.optString("require_approval", "false") == "true"

        c.addView(UiKit.switchRow(this, "开放新用户注册 (registration_open)", "允许新用户自行注册账号", regOpen) {
            regOpen = it
        })
        c.addView(UiKit.switchRow(this, "注册需超级管理员审核 (require_approval)", "新注册账号需审核通过后方可登录", reqApproval) {
            reqApproval = it
        })
        c.addView(UiKit.spacer(this, 8))
        c.addView(UiKit.label(this, "单用户最大可创建集控账户数", 12f, UiKit.TEXT_SECONDARY, true))
        val maxAccEd = UiKit.input(this, "默认 5", sysSettingsObj.optString("max_accounts_per_user", "5"))
        c.addView(maxAccEd)
        c.addView(UiKit.label(this, "系统全局公告 (MOTD)", 12f, UiKit.TEXT_SECONDARY, true))
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
                work = { ApiClient.request(ApiClient.adminUrl, "/settings", "PUT", JSONObject().put("settings", s)) },
                done = { r -> setFeedback(if (r.ok) "系统设置已保存" else r.error, !r.ok) }
            )
        })
        box.addView(c)
        return box
    }
}
