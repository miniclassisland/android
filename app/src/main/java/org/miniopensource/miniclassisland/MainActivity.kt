package org.miniopensource.miniclassisland

import android.app.Activity
import android.content.Intent
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
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.Executors

class MainActivity : Activity() {
    internal val executor = Executors.newCachedThreadPool()
    internal val mainHandler = Handler(Looper.getMainLooper())

    // Global Auth & Shell State (1:1 with Web /login, /, and AppShell)
    internal var isLoggedIn = false
    internal var authStep = "email" // "email" | "login" | "register" | "totp" | "oobe"
    internal var authEmailInput = ""
    internal var authUsernameInput = ""
    internal var authDisplayNameInput = ""
    internal var useAdminLogin = false
    internal var showEndpointConfig = false
    internal var totpSession = ""
    internal var totpUseRecovery = false
    internal var statusMessage = ""
    internal var statusIsError = false
    internal var certExchangeInput = ""
    internal var qrWebLoginInput = ""

    // Account Selection & Shell Popover State
    internal var currentUser = JSONObject()
    internal var accounts = JSONArray()
    internal var currentAccountId: String = ""
    internal var currentAccountName = ""
    internal var showCreateAccountForm = false
    internal var showJoinInviteForm = false
    internal var showUserMenu = false
    internal var isSuperAdminMode = false
    internal var isSuperAdminTakeover = false

    internal var currentSection = "dashboard"
    internal var showMobileMenu = false

    // Profile Editor Component instance
    internal lateinit var profileEditor: ProfileEditorComponent

    // Dashboard metrics
    internal var dashOnline = 0
    internal var dashTotalClients = 0
    internal var dashPendingPairing = 0
    internal var dashTotalResources = 0
    internal var dashRecentAudits = 0
    internal var exportedPresetJson = ""

    // Clients & Inspector state
    internal var clientsArray = JSONArray()
    internal var clientFilter = "all"
    internal var clientSearchKeyword = ""
    internal var selectedClientUids = mutableSetOf<String>()
    internal var activeClient: JSONObject? = null
    internal var inspectorTab = "info" // "info" | "profile" | "snapshots" | "notify"
    internal var clientProfileObj = JSONObject()
    internal var clientSnapshots = JSONArray()
    internal var snapshotDetailText = ""

    // Notifications state
    internal var notifyTargetUid = "__broadcast__"
    internal var notifyMask = "集控中心通知"
    internal var notifyContent = "请各班注意查收最新课表安排"
    internal var notifyEmergency = false
    internal var notifySpeech = true
    internal var notifySound = true
    internal var notifyEffect = true
    internal var notifyTopmost = true
    internal var notifyDuration = 10
    internal var notifyRepeat = 1

    // Audit Logs state
    internal var auditLogs = JSONArray()
    internal var auditEventFilter = "all"
    internal var auditPayloadPreview = ""

    // Admission & Governance state
    internal var accountInfoObj = JSONObject()
    internal var pairingList = JSONArray()
    internal var preRegList = JSONArray()
    internal var quotasList = JSONArray()
    internal var membersList = JSONArray()
    internal var invitationsList = JSONArray()
    internal var globalUsersList = JSONArray()
    internal var pendingUsersList = JSONArray()
    internal var globalAccountsList = JSONArray()
    internal var globalStatsObj = JSONObject()
    internal var sysSettingsObj = JSONObject()
    internal var totpEnableSecret = ""
    internal var totpEnableUri = ""
    internal var totpRecoveryCodesText = ""

    // Edge-to-Edge Safe Area Insets (in pixels)
    internal var safeInsetTopPx = 0
    internal var safeInsetBottomPx = 0
    internal var safeInsetLeftPx = 0
    internal var safeInsetRightPx = 0

    internal lateinit var rootContainer: LinearLayout

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

    internal fun setFeedback(msg: String, isErr: Boolean = false) {
        statusMessage = msg
        statusIsError = isErr
        rebuildUi()
    }

    internal fun isTabletLayout(): Boolean = resources.configuration.screenWidthDp >= 600
    internal fun isWideTabletLayout(): Boolean = resources.configuration.screenWidthDp >= 840

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        UiKit.syncSystemNightMode(this)
        setupEdgeToEdgeWindow()
        loadLocalAuthState()
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

        handleIncomingDeepLink(intent)
        if (!isLoggedIn && ApiClient.storedCertificate != null) {
            loginWithStoredCertificate()
        }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIncomingDeepLink(intent)
    }

    internal fun handleIncomingDeepLink(intent: Intent?) {
        val data = intent?.data ?: return
        val uriStr = data.toString().trim()
        if (uriStr.startsWith("miniclassisland://exchange-certificate/", ignoreCase = true)) {
            certExchangeInput = uriStr
            redeemCertificateCodeAndLogin(uriStr)
        } else if (uriStr.startsWith("miniclassisland://qr-login/", ignoreCase = true)) {
            qrWebLoginInput = uriStr
            authorizeWebQrLoginFromApp(uriStr)
        }
    }

    internal fun loadLocalAuthState() {
        val prefs = getSharedPreferences("mci_auth_prefs", MODE_PRIVATE)
        val savedMgmt = prefs.getString("management_url", null)
        val savedAdmin = prefs.getString("admin_url", null)
        if (!savedMgmt.isNullOrBlank()) ApiClient.managementUrl = savedMgmt
        if (!savedAdmin.isNullOrBlank()) ApiClient.adminUrl = savedAdmin
        ApiClient.token = prefs.getString("token", "") ?: ""
        ApiClient.tokenExpiresAtEpochSec = prefs.getLong("token_expires_at", 0L)
        currentAccountId = prefs.getString("current_account_id", "") ?: ""
        currentAccountName = prefs.getString("current_account_name", "") ?: ""
        val certStr = prefs.getString("stored_certificate", null)
        if (!certStr.isNullOrBlank()) {
            try {
                ApiClient.storedCertificate = StoredLoginCert.fromJson(JSONObject(certStr))
            } catch (_: Exception) {
                ApiClient.storedCertificate = null
            }
        }
    }

    internal fun saveLocalAuthState() {
        val prefs = getSharedPreferences("mci_auth_prefs", MODE_PRIVATE)
        prefs.edit()
            .putString("management_url", ApiClient.managementUrl)
            .putString("admin_url", ApiClient.adminUrl)
            .putString("token", ApiClient.token)
            .putLong("token_expires_at", ApiClient.tokenExpiresAtEpochSec)
            .putString("current_account_id", currentAccountId)
            .putString("current_account_name", currentAccountName)
            .putString("stored_certificate", ApiClient.storedCertificate?.toJson()?.toString() ?: "")
            .apply()
    }

    internal fun completeLoginAndLoadAccounts() {
        isLoggedIn = true
        statusMessage = ""
        runAsync(
            work = {
                val uRes = ApiClient.request(ApiClient.managementUrl, "/user/info")
                val aRes = ApiClient.request(ApiClient.managementUrl, "/account/list")
                Pair(uRes, aRes)
            },
            done = { (uRes, aRes) ->
                if (uRes.ok) {
                    currentUser = uRes.json.optJSONObject("user") ?: uRes.json
                }
                if (aRes.ok) {
                    accounts = aRes.json.optJSONArray("accounts") ?: JSONArray()
                }
                rebuildUi()
            }
        )
    }

    internal fun onLoginSuccess(authJson: JSONObject) {
        ApiClient.token = authJson.optString("token")
        val expIn = authJson.optLong("expires_in", 3600L)
        ApiClient.tokenExpiresAtEpochSec = (System.currentTimeMillis() / 1000L) + expIn
        currentUser = authJson.optJSONObject("user") ?: JSONObject()
        isLoggedIn = true
        statusMessage = ""
        saveLocalAuthState()
        if (useAdminLogin) {
            isSuperAdminMode = true
            navigateToSection("admin_dashboard")
        } else {
            isSuperAdminMode = false
            currentAccountId = ""
            currentAccountName = ""
            loadAccountsListOnly()
        }
    }

    internal fun loadAccountsListOnly() {
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

    internal fun logoutCurrentUser() {
        runAsync(
            work = { ApiClient.request(ApiClient.managementUrl, "/token/deactivate", "POST", JSONObject()) },
            done = {
                ApiClient.token = ""
                ApiClient.tokenExpiresAtEpochSec = 0L
                isLoggedIn = false
                isSuperAdminMode = false
                isSuperAdminTakeover = false
                currentAccountId = ""
                currentAccountName = ""
                authStep = "email"
                saveLocalAuthState()
                rebuildUi()
            }
        )
    }

    internal fun redeemCertificateCodeAndLogin(codeOrUri: String) {
        val trimmed = codeOrUri.trim()
        if (trimmed.isEmpty()) {
            setFeedback("请输入安全证书兑换码或完整链接", true)
            return
        }
        runAsync(
            work = {
                val r = ApiClient.redeemCertificate(trimmed)
                if (!r.ok) return@runAsync r
                if (ApiClient.token.isNotEmpty()) r else ApiClient.requestSessionFromCertificate(3600)
            },
            done = { res ->
                saveLocalAuthState()
                if (res.ok) {
                    setFeedback("已兑换登录安全证书并获取 1 小时管理会话令牌", false)
                    completeLoginAndLoadAccounts()
                } else {
                    setFeedback("安全证书兑换失败: ${res.error}", true)
                }
            }
        )
    }

    internal fun loginWithStoredCertificate() {
        val cert = ApiClient.storedCertificate
        if (cert == null) {
            setFeedback("本地尚未保存登录安全证书", true)
            return
        }
        runAsync(
            work = {
                ApiClient.rotateCertificateIfEligible()
                ApiClient.requestSessionFromCertificate(3600)
            },
            done = { res ->
                saveLocalAuthState()
                if (res.ok) {
                    setFeedback("已通过本地安全证书签发 1 小时会话 Token", false)
                    completeLoginAndLoadAccounts()
                } else {
                    setFeedback("安全证书会话申请失败: ${res.error}", true)
                }
            }
        )
    }

    internal fun rotateStoredCertificateNow() {
        runAsync(
            work = { ApiClient.rotateCertificateIfEligible() },
            done = { res ->
                saveLocalAuthState()
                if (res.ok) {
                    setFeedback("安全证书已轮换至第 ${ApiClient.storedCertificate?.generation ?: 0} 代", false)
                } else {
                    setFeedback("安全证书轮换失败: ${res.error}", true)
                }
            }
        )
    }

    internal fun authorizeWebQrLoginFromApp(codeOrUri: String) {
        val trimmed = codeOrUri.trim()
        if (trimmed.isEmpty()) {
            setFeedback("请输入或扫描网页端二维码链接 miniclassisland://qr-login/{Code}", true)
            return
        }
        if (ApiClient.token.isEmpty() && ApiClient.storedCertificate == null) {
            setFeedback("请先在移动端登录账户或导入登录安全证书后再扫码授权网页登录", true)
            return
        }
        runAsync(
            work = { ApiClient.authorizeWebQrLogin(trimmed) },
            done = { res ->
                if (res.ok) {
                    qrWebLoginInput = ""
                    setFeedback("已成功授权网页端扫码登录", false)
                } else {
                    setFeedback("网页端扫码登录授权失败: ${res.error}", true)
                }
            }
        )
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

    internal fun buildThemeModeToggle(onLoginBg: Boolean = false): View {
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

    internal fun buildWebFooterDisclaimer(onLoginBg: Boolean = true): View {
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

    fun rebuildUi() {
        UiKit.syncSystemNightMode(this)
        val isOnLoginSurface = !isLoggedIn || (!isSuperAdminMode && currentAccountId.isEmpty())
        updateSystemBarsAppearance(isOnLoginSurface)
        rootContainer.removeAllViews()
        rootContainer.setBackgroundColor(UiKit.BG_ROOT)

        if (!isLoggedIn) {
            rootContainer.addView(buildAuthView())
            return
        }

        if (!isSuperAdminMode && currentAccountId.isEmpty()) {
            rootContainer.addView(buildAccountSelectionView())
            return
        }

        rootContainer.addView(buildTopBar())
        rootContainer.addView(View(this).apply {
            setBackgroundColor(UiKit.BORDER_COLOR)
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, UiKit.dp(this@MainActivity, 1))
        })

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
            bar.addView(UiKit.iconButton(this, if (showMobileMenu) MdsIcon.CLOSE else MdsIcon.MENU, UiKit.BTN_MUTED, UiKit.TEXT_PRIMARY, 28) {
                showMobileMenu = !showMobileMenu
                rebuildUi()
            })
        }

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

        bar.addView(buildThemeModeToggle(false).apply {
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                rightMargin = UiKit.dp(this@MainActivity, 8)
            }
        })

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
        userPill.addView(UiKit.label(this, pillLabel, 12f, UiKit.TEXT_PRIMARY, true))
        userPill.addView(OfficialIcons.imageView(this, MdsIcon.CHEVRON_DOWN, UiKit.TEXT_SECONDARY, 14).apply {
            layoutParams = LinearLayout.LayoutParams(UiKit.dp(this@MainActivity, 14), UiKit.dp(this@MainActivity, 14)).apply {
                leftMargin = UiKit.dp(this@MainActivity, 4)
            }
        })
        bar.addView(userPill)

        return bar
    }

    internal fun buildUserMenuPopover(): View {
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
            if (!isSuperAdminMode && currentAccountId.isNotEmpty()) {
                addView(UiKit.label(this@MainActivity, "当前账户: $currentAccountName", 12f, UiKit.TEXT_SECONDARY))
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
            currentAccountId = ""
            currentAccountName = ""
            saveLocalAuthState()
            loadAccountsListOnly()
        })
        actionsRow.addView(UiKit.button(this, "安全设置", UiKit.BTN_MUTED, UiKit.TEXT_PRIMARY, true) {
            showUserMenu = false
            isSuperAdminMode = false
            if (currentAccountId.isEmpty() && accounts.length() > 0) {
                val first = accounts.optJSONObject(0)
                if (first != null) {
                    currentAccountId = first.optString("id")
                    currentAccountName = first.optString("name")
                    saveLocalAuthState()
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
                if (currentAccountId.isNotEmpty()) {
                    navigateToSection("dashboard")
                } else {
                    rebuildUi()
                }
            } else {
                isSuperAdminMode = true
                navigateToSection("admin_dashboard")
            }
        })
        actionsRow.addView(UiKit.button(this, "退出登录", UiKit.BTN_MUTED, UiKit.ACCENT_RED, true) {
            showUserMenu = false
            logoutCurrentUser()
        })
        card.addView(actionsRow)
        wrap.addView(card)
        return wrap
    }

    internal fun buildSidebarContent(): View {
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
            box.addView(UiKit.iconTextButton(this, MdsIcon.ARROW_LEFT, "返回集控管理面板", UiKit.BTN_MUTED, UiKit.TEXT_PRIMARY, false) {
                isSuperAdminMode = false
                if (currentAccountId.isNotEmpty()) {
                    navigateToSection("dashboard")
                } else {
                    rebuildUi()
                }
            }.apply {
                layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            })
            return box
        }

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

    internal fun buildSidebarNavItem(key: String, title: String): View {
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
            setOnClickListener {
                navigateToSection(key)
            }
        }
    }

    internal fun navigateToSection(sec: String) {
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
            banner.addView(UiKit.iconButton(this, MdsIcon.CLOSE, Color.TRANSPARENT, if (statusIsError) UiKit.BADGE_DANGER_FG else UiKit.BADGE_SUCCESS_FG, 24) {
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
}
