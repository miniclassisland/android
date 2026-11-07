package org.miniopensource.miniclassisland

import android.content.Context
import android.graphics.Color
import android.view.Gravity
import android.view.ViewGroup
import android.widget.LinearLayout
import org.json.JSONArray
import org.json.JSONObject

// ==================== 4. POLICY EDITOR ====================
internal fun ProfileEditorComponent.buildPolicyVisual(ctx: Context, container: LinearLayout, root: JSONObject) {
    container.addView(UiKit.label(ctx, "客户端集控限制策略", 14f, UiKit.TEXT_PRIMARY, true))
    container.addView(UiKit.label(ctx, "配置下发至终端的集控功能锁定策略与主动退出权限", 12f, UiKit.TEXT_SECONDARY).apply {
        setPadding(0, 0, 0, UiKit.dp(ctx, 8))
    })
    val items = listOf(
        Triple("DisableProfileClassPlanEditing", "禁止终端修改课表", "锁定课表编排，仅允许集控下发"),
        Triple("DisableProfileTimeLayoutEditing", "禁止终端修改时间表", "锁定作息时间点"),
        Triple("DisableProfileSubjectsEditing", "禁止终端修改科目", "锁定科目与任课教师列表"),
        Triple("DisableProfileEditing", "完全禁止编辑档案", "锁定全部档案设置页"),
        Triple("DisableSettingsEditing", "禁止修改应用设置", "锁定客户端设置菜单"),
        Triple("DisableSplashCustomize", "禁止自定义启动画面", "强制使用标准启动屏"),
        Triple("DisableDebugMenu", "禁用调试菜单", "防止通过调试入口绕过管控"),
        Triple("AllowExitManagement", "允许主动退出集控", "开启后终端可在设置中脱离集控"),
        Triple("DisableEasterEggs", "禁用彩蛋特性", "关闭应用内置彩蛋")
    )
    items.forEach { (key, title, sub) ->
        container.addView(
            UiKit.switchRow(ctx, title, sub, root.optBoolean(key, key == "AllowExitManagement")) { v ->
                root.put(key, v)
                commitJsonAndRefresh(root)
            }
        )
    }
}

// ==================== 5. DEFAULT SETTINGS EDITOR (15 UPSTREAM TABS) ====================
internal fun ProfileEditorComponent.buildDefaultSettingsVisual(ctx: Context, container: LinearLayout, root: JSONObject) {
    val presetBox = UiKit.subCard(ctx, 10)
    presetBox.addView(UiKit.label(ctx, "快速生成客户端接入预设", 12f, UiKit.ACCENT_BLUE, true))
    val pRow = UiKit.hBox(ctx)
    val idEdit = UiKit.input(ctx, "指定班级标识 (留空生成通用预设)", presetIdentityInput).apply {
        layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply {
            rightMargin = UiKit.dp(ctx, 6)
        }
    }
    pRow.addView(idEdit)
    pRow.addView(UiKit.button(ctx, "查看预设 JSON", UiKit.BTN_MUTED, UiKit.ACCENT_BLUE, true) {
        val cid = idEdit.text.toString().trim()
        presetIdentityInput = cid
        val accId = act.currentAccountId
        val path = if (cid.isEmpty()) {
            "/account/$accId/ManagementPreset.json"
        } else {
            "/account/$accId/ManagementPreset.json?id=${ApiClient.enc(cid)}"
        }
        act.runAsync({ ApiClient.request(ApiClient.managementUrl, path) }) { r ->
            if (r.ok) {
                act.exportedPresetJson = r.rawText
                act.showBanner("已获取 ManagementPreset.json，可在仪表盘或弹窗复制", false)
            } else {
                act.showBanner("获取预设失败: ${r.error}", true)
            }
            act.renderUI()
        }
    })
    presetBox.addView(pRow)
    if (act.exportedPresetJson.isNotEmpty()) {
        presetBox.addView(UiKit.input(ctx, "ManagementPreset.json", act.exportedPresetJson, multiLine = true))
    }
    container.addView(presetBox)

    val settingsTabs = listOf(
        "general" to "基本",
        "components" to "组件",
        "appearance" to "外观",
        "window" to "窗口",
        "notification" to "提醒与语音",
        "weather" to "天气",
        "clock" to "时钟",
        "automation" to "自动化",
        "update" to "更新",
        "storage" to "存储",
        "privacy" to "隐私",
        "plugins" to "插件",
        "refreshing" to "翻新与迎新",
        "advanced" to "高级",
        "debug" to "调试"
    )
    container.addView(UiKit.chipRow(ctx, settingsTabs, activeSettingsTab) { tab ->
        activeSettingsTab = tab
        act.renderUI()
    })
    container.addView(UiKit.divider(ctx, 8))

    val pane = UiKit.subCard(ctx, 12)
    when (activeSettingsTab) {
        "general" -> {
            pane.addView(UiKit.label(ctx, "基本与启动设置", 13f, UiKit.TEXT_PRIMARY, true))
            val profEdit = UiKit.input(ctx, "当前加载档案文件名", root.optString("SelectedProfile", "Default.json"))
            val grpEdit = UiKit.input(ctx, "启用课表群组 GUID", root.optString("CurrentClassPlanGroupId", EMPTY_GUID))
            pane.addView(profEdit)
            pane.addView(grpEdit)
            pane.addView(UiKit.button(ctx, "保存档案与群组参数", UiKit.BTN_MUTED, UiKit.TEXT_PRIMARY, true) {
                root.put("SelectedProfile", profEdit.text.toString().trim())
                root.put("CurrentClassPlanGroupId", grpEdit.text.toString().trim())
                commitJsonAndRefresh(root)
            })
            pane.addView(UiKit.switchRow(ctx, "显示主界面窗口", "控制客户端主界面是否可见", root.optBoolean("IsMainWindowVisible", true)) { v ->
                root.put("IsMainWindowVisible", v)
                commitJsonAndRefresh(root)
            })
            pane.addView(UiKit.switchRow(ctx, "显示启动画面", "启动时展示欢迎启动屏", root.optBoolean("IsSplashEnabled", true)) { v ->
                root.put("IsSplashEnabled", v)
                commitJsonAndRefresh(root)
            })
            pane.addView(UiKit.switchRow(ctx, "显示系统托盘图标", "在任务栏通知区域显示图标", root.optBoolean("IsShowTaskbarIcon", true)) { v ->
                root.put("IsShowTaskbarIcon", v)
                commitJsonAndRefresh(root)
            })
            pane.addView(UiKit.switchRow(ctx, "临时换课在大课间后自动复原", "", root.optBoolean("RecoverTempClassPlanAfterRecess", false)) { v ->
                root.put("RecoverTempClassPlanAfterRecess", v)
                commitJsonAndRefresh(root)
            })
        }
        "components" -> {
            pane.addView(UiKit.label(ctx, "组件配置方案", 13f, UiKit.TEXT_PRIMARY, true))
            val cEdit = UiKit.input(ctx, "当前启用的组件配置文件名", root.optString("CurrentComponentConfig", "Default"))
            pane.addView(cEdit)
            pane.addView(UiKit.button(ctx, "应用组件方案名", UiKit.BTN_MUTED, UiKit.TEXT_PRIMARY, true) {
                root.put("CurrentComponentConfig", cEdit.text.toString().trim().ifEmpty { "Default" })
                commitJsonAndRefresh(root)
            })
            pane.addView(UiKit.switchRow(ctx, "启用教程引导提示", "", root.optBoolean("ShowComponentsMigrateTip", false)) { v ->
                root.put("ShowComponentsMigrateTip", v)
                commitJsonAndRefresh(root)
            })
        }
        "appearance" -> {
            pane.addView(UiKit.label(ctx, "主题与视觉外观", 13f, UiKit.TEXT_PRIMARY, true))
            val themeOpts = listOf("0" to "跟随系统", "1" to "浅色主题", "2" to "深色主题")
            pane.addView(UiKit.chipRow(ctx, themeOpts, root.optInt("Theme", 0).toString()) { v ->
                root.put("Theme", v.toIntOrNull() ?: 0)
                commitJsonAndRefresh(root)
            })
            val colorSrcOpts = listOf("0" to "自定义颜色", "1" to "系统壁纸主题色", "2" to "系统强调色")
            pane.addView(UiKit.label(ctx, "主题色来源:", 12f, UiKit.TEXT_SECONDARY))
            pane.addView(UiKit.chipRow(ctx, colorSrcOpts, root.optInt("ColorSource", 0).toString()) { v ->
                root.put("ColorSource", v.toIntOrNull() ?: 0)
                commitJsonAndRefresh(root)
            })

            val r = UiKit.hBox(ctx)
            val scaleEd = UiKit.input(ctx, "界面缩放比例", root.optDouble("Scale", 1.0).toString()).apply {
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply { rightMargin = UiKit.dp(ctx, 6) }
            }
            val opEd = UiKit.input(ctx, "背景不透明度", root.optDouble("Opacity", 1.0).toString()).apply {
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply { rightMargin = UiKit.dp(ctx, 6) }
            }
            val radEd = UiKit.input(ctx, "圆角半径", root.optDouble("RadiusX", 8.0).toString()).apply {
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
            }
            r.addView(scaleEd)
            r.addView(opEd)
            r.addView(radEd)
            pane.addView(r)

            val cRow = UiKit.hBox(ctx)
            val pColorEd = UiKit.input(ctx, "主色调", root.optString("PrimaryColor", "#0090FF")).apply {
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply { rightMargin = UiKit.dp(ctx, 6) }
            }
            val sColorEd = UiKit.input(ctx, "次色调", root.optString("SecondaryColor", "#0090FF")).apply {
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply { rightMargin = UiKit.dp(ctx, 6) }
            }
            val fontEd = UiKit.input(ctx, "主界面字体名称", root.optString("MainWindowFont", "HarmonyOS Sans SC")).apply {
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
            }
            cRow.addView(pColorEd)
            cRow.addView(sColorEd)
            cRow.addView(fontEd)
            pane.addView(cRow)

            pane.addView(UiKit.button(ctx, "保存外观参数", UiKit.ACCENT_BLUE, Color.WHITE, true) {
                root.put("Scale", scaleEd.text.toString().toDoubleOrNull() ?: 1.0)
                root.put("Opacity", opEd.text.toString().toDoubleOrNull() ?: 1.0)
                val rad = radEd.text.toString().toDoubleOrNull() ?: 8.0
                root.put("RadiusX", rad)
                root.put("RadiusY", rad)
                root.put("PrimaryColor", pColorEd.text.toString().trim().ifEmpty { "#0090FF" })
                root.put("SecondaryColor", sColorEd.text.toString().trim().ifEmpty { "#0090FF" })
                root.put("MainWindowFont", fontEd.text.toString().trim())
                commitJsonAndRefresh(root)
            })
        }
        "window" -> {
            pane.addView(UiKit.label(ctx, "窗口停靠位置 (6 点可视化选择):", 13f, UiKit.TEXT_PRIMARY, true))
            val curDock = root.optInt("WindowDockingLocation", 1)
            val screenBox = UiKit.vBox(ctx, 12).apply {
                background = UiKit.roundedBg(0xFF1A2230.toInt(), 10, ctx, UiKit.ACCENT_BLUE, 1)
            }
            val topRow = UiKit.hBox(ctx)
            listOf(0 to "左上", 1 to "顶部居中", 2 to "右上").forEach { (pos, label) ->
                val active = curDock == pos
                topRow.addView(UiKit.button(
                    ctx,
                    label,
                    if (active) UiKit.ACCENT_BLUE else 0xFF2C3648.toInt(),
                    Color.WHITE,
                    true
                ) {
                    root.put("WindowDockingLocation", pos)
                    commitJsonAndRefresh(root)
                }.apply {
                    layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply {
                        marginEnd = UiKit.dp(ctx, 4)
                    }
                })
            }
            screenBox.addView(topRow)
            screenBox.addView(UiKit.spacer(ctx, 20))
            val botRow = UiKit.hBox(ctx)
            listOf(3 to "左下", 4 to "底部居中", 5 to "右下").forEach { (pos, label) ->
                val active = curDock == pos
                botRow.addView(UiKit.button(
                    ctx,
                    label,
                    if (active) UiKit.ACCENT_BLUE else 0xFF2C3648.toInt(),
                    Color.WHITE,
                    true
                ) {
                    root.put("WindowDockingLocation", pos)
                    commitJsonAndRefresh(root)
                }.apply {
                    layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply {
                        marginEnd = UiKit.dp(ctx, 4)
                    }
                })
            }
            screenBox.addView(botRow)
            pane.addView(screenBox)
            pane.addView(UiKit.spacer(ctx, 8))

            pane.addView(UiKit.label(ctx, "窗口层级:", 12f, UiKit.TEXT_SECONDARY))
            val layerOpts = listOf("0" to "置于底层", "1" to "置于顶层 (置顶)")
            pane.addView(UiKit.chipRow(ctx, layerOpts, root.optInt("WindowLayer", 1).toString()) { v ->
                root.put("WindowLayer", v.toIntOrNull() ?: 1)
                commitJsonAndRefresh(root)
            })

            val offRow = UiKit.hBox(ctx)
            val oxEd = UiKit.input(ctx, "横向偏移", root.optInt("WindowDockingOffsetX", 0).toString()).apply {
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply { rightMargin = UiKit.dp(ctx, 6) }
            }
            val oyEd = UiKit.input(ctx, "纵向偏移", root.optInt("WindowDockingOffsetY", 0).toString()).apply {
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply { rightMargin = UiKit.dp(ctx, 6) }
            }
            val monEd = UiKit.input(ctx, "显示器索引", root.optInt("WindowDockingMonitorIndex", 0).toString()).apply {
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
            }
            offRow.addView(oxEd)
            offRow.addView(oyEd)
            offRow.addView(monEd)
            pane.addView(offRow)
            pane.addView(UiKit.button(ctx, "保存偏移与显示器", UiKit.BTN_MUTED, UiKit.TEXT_PRIMARY, true) {
                root.put("WindowDockingOffsetX", oxEd.text.toString().toIntOrNull() ?: 0)
                root.put("WindowDockingOffsetY", oyEd.text.toString().toIntOrNull() ?: 0)
                root.put("WindowDockingMonitorIndex", monEd.text.toString().toIntOrNull() ?: 0)
                commitJsonAndRefresh(root)
            })

            pane.addView(UiKit.switchRow(ctx, "鼠标移入时避让或穿透", "", root.optBoolean("IsMouseClickingThroughEnabled", false)) { v ->
                root.put("IsMouseClickingThroughEnabled", v)
                commitJsonAndRefresh(root)
            })
            pane.addView(UiKit.switchRow(ctx, "全屏或最大化程序运行时隐藏", "", root.optBoolean("HideOnFullscreen", false)) { v ->
                root.put("HideOnFullscreen", v)
                commitJsonAndRefresh(root)
            })
        }
        "notification" -> {
            pane.addView(UiKit.label(ctx, "提醒与语音合成设置", 13f, UiKit.TEXT_PRIMARY, true))
            val switches = listOf(
                "IsNotificationEnabled" to "启用全局提醒功能",
                "IsNotificationEffectEnabled" to "启用全屏醒目提醒光效",
                "IsNotificationSoundEnabled" to "启用提醒音效",
                "IsNotificationTopmostEnabled" to "提醒时强制窗口置顶",
                "AllowNotificationSpeech" to "启用语音播报"
            )
            switches.forEach { (k, title) ->
                pane.addView(UiKit.switchRow(ctx, title, "", root.optBoolean(k, true)) { v ->
                    root.put(k, v)
                    commitJsonAndRefresh(root)
                })
            }

            pane.addView(UiKit.label(ctx, "语音合成引擎:", 12f, UiKit.TEXT_SECONDARY))
            val ttsSources = listOf("0" to "系统内置 TTS", "1" to "Edge TTS", "2" to "GPT-SoVITS")
            pane.addView(UiKit.chipRow(ctx, ttsSources, root.optInt("SpeechSource", 0).toString()) { s ->
                root.put("SpeechSource", s.toIntOrNull() ?: 0)
                commitJsonAndRefresh(root)
            })

            val vRow = UiKit.hBox(ctx)
            val sndVolEd = UiKit.input(ctx, "音效音量", root.optDouble("NotificationSoundVolume", 1.0).toString()).apply {
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply { rightMargin = UiKit.dp(ctx, 6) }
            }
            val ttsVolEd = UiKit.input(ctx, "语音音量", root.optDouble("SpeechVolume", 1.0).toString()).apply {
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply { rightMargin = UiKit.dp(ctx, 6) }
            }
            val voiceEd = UiKit.input(ctx, "EdgeTTS 发音人", root.optString("EdgeTtsVoiceName", "zh-CN-XiaoxiaoNeural")).apply {
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.5f)
            }
            vRow.addView(sndVolEd)
            vRow.addView(ttsVolEd)
            vRow.addView(voiceEd)
            pane.addView(vRow)
            pane.addView(UiKit.button(ctx, "保存音量与发音人", UiKit.BTN_MUTED, UiKit.TEXT_PRIMARY, true) {
                root.put("NotificationSoundVolume", sndVolEd.text.toString().toDoubleOrNull() ?: 1.0)
                root.put("SpeechVolume", ttsVolEd.text.toString().toDoubleOrNull() ?: 1.0)
                root.put("EdgeTtsVoiceName", voiceEd.text.toString().trim())
                commitJsonAndRefresh(root)
            })
        }
        "weather" -> {
            pane.addView(UiKit.label(ctx, "气象数据源与城市配置", 13f, UiKit.TEXT_PRIMARY, true))
            val cIdEd = UiKit.input(ctx, "城市编码", root.optString("CityId", "weathercn:101010100"))
            val cNameEd = UiKit.input(ctx, "城市显示名称", root.optString("CityName", "北京"))
            val latEd = UiKit.input(ctx, "经度", root.optDouble("WeatherLongitude", 0.0).toString())
            val lonEd = UiKit.input(ctx, "纬度", root.optDouble("WeatherLatitude", 0.0).toString())
            pane.addView(cIdEd)
            pane.addView(cNameEd)
            pane.addView(latEd)
            pane.addView(lonEd)
            pane.addView(UiKit.switchRow(ctx, "自动根据 IP 或系统定位获取位置", "", root.optBoolean("AutoRefreshWeatherLocation", false)) { v ->
                root.put("AutoRefreshWeatherLocation", v)
                commitJsonAndRefresh(root)
            })
            pane.addView(UiKit.button(ctx, "保存天气参数", UiKit.ACCENT_BLUE, Color.WHITE, true) {
                root.put("CityId", cIdEd.text.toString().trim())
                root.put("CityName", cNameEd.text.toString().trim())
                root.put("WeatherLongitude", latEd.text.toString().toDoubleOrNull() ?: 0.0)
                root.put("WeatherLatitude", lonEd.text.toString().toDoubleOrNull() ?: 0.0)
                commitJsonAndRefresh(root)
            })
        }
        "clock" -> {
            pane.addView(UiKit.label(ctx, "时钟偏移与 NTP 自动校准", 13f, UiKit.TEXT_PRIMARY, true))
            val offEd = UiKit.input(ctx, "时间偏移秒数", root.optDouble("TimeOffsetSeconds", 0.0).toString())
            val ntpEd = UiKit.input(ctx, "NTP 校准服务器", root.optString("TimeAutoAdjustServer", "ntp.aliyun.com"))
            pane.addView(offEd)
            pane.addView(ntpEd)
            pane.addView(UiKit.switchRow(ctx, "启用 NTP 自动校准时间", "", root.optBoolean("IsTimeAutoAdjustEnabled", false)) { v ->
                root.put("IsTimeAutoAdjustEnabled", v)
                commitJsonAndRefresh(root)
            })
            pane.addView(UiKit.switchRow(ctx, "启用每日时间自动微小偏移", "", root.optBoolean("IsTimeRuleAdjustEnabled", false)) { v ->
                root.put("IsTimeRuleAdjustEnabled", v)
                commitJsonAndRefresh(root)
            })
            pane.addView(UiKit.button(ctx, "保存时钟设置", UiKit.ACCENT_BLUE, Color.WHITE, true) {
                root.put("TimeOffsetSeconds", offEd.text.toString().toDoubleOrNull() ?: 0.0)
                root.put("TimeAutoAdjustServer", ntpEd.text.toString().trim())
                commitJsonAndRefresh(root)
            })
        }
        "automation" -> {
            pane.addView(UiKit.label(ctx, "自动化工作流配置", 13f, UiKit.TEXT_PRIMARY, true))
            pane.addView(UiKit.switchRow(ctx, "启用自动化工作流引擎", "允许根据触发器自动执行行动集", root.optBoolean("IsAutomationEnabled", false)) { v ->
                root.put("IsAutomationEnabled", v)
                commitJsonAndRefresh(root)
            })
            val cfgEd = UiKit.input(ctx, "当前自动化配置方案名称", root.optString("CurrentAutomationConfig", "Default"))
            pane.addView(cfgEd)
            pane.addView(UiKit.button(ctx, "保存自动化方案", UiKit.BTN_MUTED, UiKit.TEXT_PRIMARY, true) {
                root.put("CurrentAutomationConfig", cfgEd.text.toString().trim().ifEmpty { "Default" })
                commitJsonAndRefresh(root)
            })
        }
        "update" -> {
            pane.addView(UiKit.label(ctx, "应用更新策略", 13f, UiKit.TEXT_PRIMARY, true))
            val modeOpts = listOf("0" to "不自动检查", "1" to "自动检查并通知", "2" to "自动检查并下载", "3" to "自动下载并重启安装")
            pane.addView(UiKit.chipRow(ctx, modeOpts, root.optInt("UpdateMode", 2).toString()) { m ->
                root.put("UpdateMode", m.toIntOrNull() ?: 2)
                commitJsonAndRefresh(root)
            })
            val chEd = UiKit.input(ctx, "更新通道", root.optString("SelectedUpdateChannelV2", "stable"))
            val mirrorEd = UiKit.input(ctx, "更新镜像源标识", root.optString("SelectedUpdateMirrorV2", "github"))
            pane.addView(chEd)
            pane.addView(mirrorEd)
            pane.addView(UiKit.button(ctx, "保存更新设置", UiKit.BTN_MUTED, UiKit.TEXT_PRIMARY, true) {
                root.put("SelectedUpdateChannelV2", chEd.text.toString().trim())
                root.put("SelectedUpdateMirrorV2", mirrorEd.text.toString().trim())
                commitJsonAndRefresh(root)
            })
        }
        "storage" -> {
            pane.addView(UiKit.label(ctx, "自动备份与存储", 13f, UiKit.TEXT_PRIMARY, true))
            pane.addView(UiKit.switchRow(ctx, "启用配置文件定期自动备份", "", root.optBoolean("IsAutoBackupEnabled", true)) { v ->
                root.put("IsAutoBackupEnabled", v)
                commitJsonAndRefresh(root)
            })
            val daysEd = UiKit.input(ctx, "自动备份间隔天数", root.optInt("AutoBackupIntervalDays", 7).toString())
            val maxEd = UiKit.input(ctx, "最大保留备份份数", root.optInt("AutoBackupLimitCount", 16).toString())
            pane.addView(daysEd)
            pane.addView(maxEd)
            pane.addView(UiKit.button(ctx, "保存备份策略", UiKit.BTN_MUTED, UiKit.TEXT_PRIMARY, true) {
                root.put("AutoBackupIntervalDays", daysEd.text.toString().toIntOrNull() ?: 7)
                root.put("AutoBackupLimitCount", maxEd.text.toString().toIntOrNull() ?: 16)
                commitJsonAndRefresh(root)
            })
        }
        "privacy" -> {
            pane.addView(UiKit.label(ctx, "遥测与崩溃报告隐私设置", 13f, UiKit.TEXT_PRIMARY, true))
            pane.addView(UiKit.switchRow(ctx, "允许通过 Sentry 上报崩溃错误", "", root.optBoolean("IsReportingEnabled", true)) { v ->
                root.put("IsReportingEnabled", v)
                commitJsonAndRefresh(root)
            })
            pane.addView(UiKit.switchRow(ctx, "允许发送匿名使用统计", "", root.optBoolean("IsTelemetryEnabled", true)) { v ->
                root.put("IsTelemetryEnabled", v)
                commitJsonAndRefresh(root)
            })
        }
        "plugins" -> {
            pane.addView(UiKit.label(ctx, "插件市场与镜像源配置", 13f, UiKit.TEXT_PRIMARY, true))
            val idxEd = UiKit.input(ctx, "官方插件索引镜像标识", root.optString("PluginIndexSelectedMirror", "github"))
            val custEd = UiKit.input(ctx, "自定义插件源地址 (多行或逗号分隔)", root.optString("CustomPluginIndexUrls", ""))
            pane.addView(idxEd)
            pane.addView(custEd)
            pane.addView(UiKit.button(ctx, "保存插件源配置", UiKit.BTN_MUTED, UiKit.TEXT_PRIMARY, true) {
                root.put("PluginIndexSelectedMirror", idxEd.text.toString().trim())
                root.put("CustomPluginIndexUrls", custEd.text.toString().trim())
                commitJsonAndRefresh(root)
            })
        }
        "refreshing" -> {
            pane.addView(UiKit.label(ctx, "长期未启动翻新与迎新向导", 13f, UiKit.TEXT_PRIMARY, true))
            pane.addView(UiKit.switchRow(ctx, "启用翻新与迎新检测", "跨学期或长期未用时提示清理临时课表", root.optBoolean("IsRefreshingEnabled", true)) { v ->
                root.put("IsRefreshingEnabled", v)
                commitJsonAndRefresh(root)
            })
            val thEd = UiKit.input(ctx, "触发翻新向导的未启动天数阈值", root.optInt("RefreshingThresholdDays", 20).toString())
            pane.addView(thEd)
            pane.addView(UiKit.button(ctx, "保存翻新阈值", UiKit.BTN_MUTED, UiKit.TEXT_PRIMARY, true) {
                root.put("RefreshingThresholdDays", thEd.text.toString().toIntOrNull() ?: 20)
                commitJsonAndRefresh(root)
            })
        }
        "advanced" -> {
            pane.addView(UiKit.label(ctx, "渲染与系统高级参数", 13f, UiKit.TEXT_PRIMARY, true))
            pane.addView(UiKit.switchRow(ctx, "禁用硬件加速 (软件渲染回退)", "", root.optBoolean("IsSoftwareRenderingEnabled", false)) { v ->
                root.put("IsSoftwareRenderingEnabled", v)
                commitJsonAndRefresh(root)
            })
            pane.addView(UiKit.switchRow(ctx, "等待网络就绪后再初始化天气与集控", "", root.optBoolean("WaitForNetworkBeforeInit", false)) { v ->
                root.put("WaitForNetworkBeforeInit", v)
                commitJsonAndRefresh(root)
            })
        }
        "debug" -> {
            pane.addView(UiKit.label(ctx, "开发与调试选项", 13f, UiKit.TEXT_PRIMARY, true))
            pane.addView(UiKit.switchRow(ctx, "启用调试菜单", "在主菜单中显示调试工具入口", root.optBoolean("IsDebugEnabled", false)) { v ->
                root.put("IsDebugEnabled", v)
                commitJsonAndRefresh(root)
            })
            pane.addView(UiKit.switchRow(ctx, "显示帧率与渲染性能浮层", "", root.optBoolean("IsDebugShowFps", false)) { v ->
                root.put("IsDebugShowFps", v)
                commitJsonAndRefresh(root)
            })
        }
    }
    container.addView(pane)
}

// ==================== 6. COMPONENTS EDITOR (11 REAL UPSTREAM COMPONENTS + CONTAINER STACK) ====================
internal fun ProfileEditorComponent.buildComponentsVisual(ctx: Context, container: LinearLayout, root: JSONObject) {
    val lines = root.optJSONArray("Lines") ?: JSONArray().also { root.put("Lines", it) }

    // Detect legacy GUIDs
    val migratedCount = countLegacyComponentGuids(lines)
    if (migratedCount > 0) {
        val migCard = UiKit.subCard(ctx, 10)
        val r = UiKit.hBox(ctx)
        r.addView(UiKit.label(ctx, "检测到 $migratedCount 个旧版组件 GUID，可一键升级为上游标准 GUID", 12f, UiKit.BADGE_WARNING_FG, true).apply {
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        })
        r.addView(UiKit.button(ctx, "一键迁移", UiKit.ACCENT_BLUE, Color.WHITE, true) {
            migrateLegacyComponentGuids(lines)
            commitJsonAndRefresh(root)
        })
        migCard.addView(r)
        container.addView(migCard)
    }

    val topRow = UiKit.hBox(ctx)
    topRow.addView(UiKit.label(ctx, "主界面多行预览 (共 ${lines.length()} 行)", 13f, UiKit.TEXT_PRIMARY, true).apply {
        layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
    })
    topRow.addView(UiKit.iconTextButton(ctx, MdsIcon.ADD, "新增组件行", UiKit.ACCENT_BLUE, Color.WHITE, true) {
        lines.put(JSONObject().apply {
            put("IsMainLine", lines.length() == 0)
            put("IsNotificationEnabled", lines.length() == 0)
            put("IsEnabled", true)
            put("IslandSeparationMode", 0)
            put("Children", JSONArray())
        })
        selectedLineIndex = lines.length() - 1
        selectedCompIndex = 0
        containerStack.clear()
        commitJsonAndRefresh(root)
    })
    container.addView(topRow)

    if (selectedLineIndex >= lines.length()) {
        selectedLineIndex = (lines.length() - 1).coerceAtLeast(0)
        containerStack.clear()
    }

    // Container Breadcrumb Bar if inside a nested container
    if (containerStack.isNotEmpty()) {
        val bcBox = UiKit.subCard(ctx, 8)
        val bcRow = UiKit.hBox(ctx)
        val pathStr = "第 ${selectedLineIndex + 1} 行 / " + containerStack.joinToString(" / ") { it.title }
        bcRow.addView(UiKit.label(ctx, "容器层级: $pathStr", 12f, UiKit.ACCENT_BLUE, true).apply {
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        })
        bcRow.addView(UiKit.iconTextButton(ctx, MdsIcon.ARROW_LEFT, "返回上一级", UiKit.BTN_MUTED, UiKit.TEXT_PRIMARY, true) {
            containerStack.removeLastOrNull()
            selectedCompIndex = 0
            act.renderUI()
        })
        bcRow.addView(UiKit.button(ctx, "返回顶层行", UiKit.BTN_MUTED, UiKit.TEXT_SECONDARY, true) {
            containerStack.clear()
            selectedCompIndex = 0
            act.renderUI()
        })
        bcBox.addView(bcRow)
        container.addView(bcBox)
    }

    // Visual Lines / Active Container Canvas
    for (lIdx in 0 until lines.length()) {
        val line = lines.optJSONObject(lIdx) ?: continue
        val isLineSelected = (lIdx == selectedLineIndex)
        val lineCard = UiKit.subCard(ctx, 10).apply {
            background = UiKit.roundedBg(
                if (isLineSelected) UiKit.BG_ACCENT_SUBTLE else UiKit.BG_SUBTLE,
                8,
                ctx,
                if (isLineSelected) UiKit.ACCENT_BLUE else UiKit.BORDER_COLOR,
                1
            )
        }
        val lh = UiKit.hBox(ctx)
        val badges = mutableListOf<String>()
        if (line.optBoolean("IsMainLine")) badges.add("主要行")
        if (line.optBoolean("IsNotificationEnabled")) badges.add("提醒行")
        if (!line.optBoolean("IsEnabled", true)) badges.add("已禁用")
        val badgeText = if (badges.isNotEmpty()) " (${badges.joinToString(" · ")})" else ""

        lh.addView(UiKit.label(ctx, "第 ${lIdx + 1} 行$badgeText", 12f, if (isLineSelected) UiKit.ACCENT_BLUE else UiKit.TEXT_PRIMARY, true).apply {
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
            setOnClickListener {
                selectedLineIndex = lIdx
                selectedCompIndex = 0
                containerStack.clear()
                act.renderUI()
            }
        })
        if (lIdx > 0) {
            lh.addView(UiKit.iconButton(ctx, MdsIcon.ARROW_UP, UiKit.BTN_MUTED, UiKit.TEXT_PRIMARY) {
                val prev = lines.optJSONObject(lIdx - 1)
                lines.put(lIdx - 1, line)
                lines.put(lIdx, prev)
                selectedLineIndex = lIdx - 1
                commitJsonAndRefresh(root)
            })
        }
        if (lIdx < lines.length() - 1) {
            lh.addView(UiKit.iconButton(ctx, MdsIcon.ARROW_DOWN, UiKit.BTN_MUTED, UiKit.TEXT_PRIMARY) {
                val next = lines.optJSONObject(lIdx + 1)
                lines.put(lIdx + 1, line)
                lines.put(lIdx, next)
                selectedLineIndex = lIdx + 1
                commitJsonAndRefresh(root)
            })
        }
        lh.addView(UiKit.iconButton(ctx, MdsIcon.DELETE, UiKit.BADGE_DANGER_BG, UiKit.BADGE_DANGER_FG) {
            lines.remove(lIdx)
            containerStack.clear()
            commitJsonAndRefresh(root)
        })
        lineCard.addView(lh)

        val targetChildren = if (isLineSelected && containerStack.isNotEmpty()) {
            resolveActiveChildrenArray(lines)
        } else {
            line.optJSONArray("Children") ?: JSONArray().also { line.put("Children", it) }
        }

        if (targetChildren.length() == 0) {
            lineCard.addView(UiKit.label(ctx, "暂无组件，请在下方「组件库」点击添加", 11f, UiKit.TEXT_TERTIARY))
        } else {
            for (cIdx in 0 until targetChildren.length()) {
                val comp = targetChildren.optJSONObject(cIdx) ?: continue
                val cid = comp.optString("Id")
                val def = BUILTIN_COMPONENTS.find { it.id.equals(cid, ignoreCase = true) }
                val cName = def?.name ?: comp.optString("NameCache").ifBlank { "插件组件" }
                val isCompSel = isLineSelected && (cIdx == selectedCompIndex)

                val cRow = UiKit.hBox(ctx, 8).apply {
                    background = UiKit.roundedBg(
                        if (isCompSel) Color.WHITE else Color.TRANSPARENT,
                        6,
                        ctx,
                        if (isCompSel) UiKit.ACCENT_BLUE else UiKit.BORDER_COLOR,
                        1
                    )
                    layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                        topMargin = UiKit.dp(ctx, 4)
                    }
                    setOnClickListener {
                        selectedLineIndex = lIdx
                        selectedCompIndex = cIdx
                        act.renderUI()
                    }
                }
                cRow.addView(UiKit.badge(ctx, if (def?.isContainer == true) "容器" else "组件", if (def?.isContainer == true) "warning" else "info"))
                cRow.addView(UiKit.label(ctx, "  $cName", 12f, UiKit.TEXT_PRIMARY, isCompSel).apply {
                    layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
                })
                if (def?.isContainer == true) {
                    cRow.addView(UiKit.iconTextButton(ctx, MdsIcon.FOLDER, "进入容器", UiKit.ACCENT_BLUE, Color.WHITE, true) {
                        selectedLineIndex = lIdx
                        val nextPath = (containerStack.lastOrNull()?.pathIndices ?: emptyList()) + cIdx
                        containerStack.add(ContainerBreadcrumb(cName, lIdx, nextPath))
                        selectedCompIndex = 0
                        act.renderUI()
                    })
                }
                if (cIdx > 0) {
                    cRow.addView(UiKit.iconButton(ctx, MdsIcon.ARROW_LEFT, UiKit.BTN_MUTED, UiKit.TEXT_PRIMARY) {
                        val prev = targetChildren.optJSONObject(cIdx - 1)
                        targetChildren.put(cIdx - 1, comp)
                        targetChildren.put(cIdx, prev)
                        selectedCompIndex = cIdx - 1
                        commitJsonAndRefresh(root)
                    })
                }
                if (cIdx < targetChildren.length() - 1) {
                    cRow.addView(UiKit.iconButton(ctx, MdsIcon.ARROW_RIGHT, UiKit.BTN_MUTED, UiKit.TEXT_PRIMARY) {
                        val next = targetChildren.optJSONObject(cIdx + 1)
                        targetChildren.put(cIdx + 1, comp)
                        targetChildren.put(cIdx, next)
                        selectedCompIndex = cIdx + 1
                        commitJsonAndRefresh(root)
                    })
                }
                cRow.addView(UiKit.iconButton(ctx, MdsIcon.DELETE, UiKit.BADGE_DANGER_BG, UiKit.BADGE_DANGER_FG) {
                    targetChildren.remove(cIdx)
                    commitJsonAndRefresh(root)
                })
                lineCard.addView(cRow)
            }
        }
        container.addView(lineCard)
    }

    // 4 Bottom Tabs matching Upstream & Web (`library`, `settings`, `advanced`, `line`)
    val bottomTabs = listOf(
        "library" to "组件库",
        "settings" to "组件专属设置",
        "advanced" to "高级布局与覆盖",
        "line" to "当前行设置"
    )
    container.addView(UiKit.chipRow(ctx, bottomTabs, activeComponentBottomTab) { tab ->
        activeComponentBottomTab = tab
        act.renderUI()
    })

    val activeChildren = resolveActiveChildrenArray(lines)
    val selectedComp = activeChildren.optJSONObject(selectedCompIndex)
    val bottomPane = UiKit.subCard(ctx, 12)

    when (activeComponentBottomTab) {
        "library" -> {
            bottomPane.addView(UiKit.label(ctx, "内置基础组件 (点击添加到当前行/容器):", 12f, UiKit.TEXT_SECONDARY, true))
            val basicOpts = BUILTIN_COMPONENTS.filter { !it.isContainer }.map { it.id to it.name }
            bottomPane.addView(UiKit.chipRow(ctx, basicOpts, "") { compId ->
                addComponentToActiveChildren(root, lines, compId)
            })

            bottomPane.addView(UiKit.label(ctx, "内置容器组件 (可嵌套子组件):", 12f, UiKit.TEXT_SECONDARY, true))
            val contOpts = BUILTIN_COMPONENTS.filter { it.isContainer }.map { it.id to it.name }
            bottomPane.addView(UiKit.chipRow(ctx, contOpts, "") { compId ->
                addComponentToActiveChildren(root, lines, compId)
            })

            bottomPane.addView(UiKit.divider(ctx, 8))
            bottomPane.addView(UiKit.label(ctx, "插件提供组件:", 12f, UiKit.TEXT_SECONDARY, true))
            val pRow = UiKit.hBox(ctx)
            val guidEd = UiKit.input(ctx, "组件 GUID", customPluginComponentId).apply {
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.5f).apply { rightMargin = UiKit.dp(ctx, 6) }
            }
            val nameEd = UiKit.input(ctx, "备注名称", customPluginComponentName).apply {
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply { rightMargin = UiKit.dp(ctx, 6) }
            }
            pRow.addView(guidEd)
            pRow.addView(nameEd)
            pRow.addView(UiKit.button(ctx, "添加", UiKit.ACCENT_BLUE, Color.WHITE, true) {
                val gid = guidEd.text.toString().trim()
                val gname = nameEd.text.toString().trim().ifEmpty { "插件组件" }
                if (gid.isNotEmpty()) {
                    activeChildren.put(JSONObject().apply {
                        put("Id", gid)
                        put("NameCache", gname)
                        put("HorizontalAlignment", 1)
                        put("Settings", JSONObject())
                    })
                    customPluginComponentId = ""
                    customPluginComponentName = ""
                    commitJsonAndRefresh(root)
                }
            })
            bottomPane.addView(pRow)
        }
        "settings" -> {
            if (selectedComp == null) {
                bottomPane.addView(UiKit.label(ctx, "请先在上方点击选择一个组件", 12f, UiKit.TEXT_SECONDARY))
            } else {
                buildSelectedComponentSpecificSettings(ctx, bottomPane, root, selectedComp)
            }
        }
        "advanced" -> {
            if (selectedComp == null) {
                bottomPane.addView(UiKit.label(ctx, "请先在上方点击选择一个组件", 12f, UiKit.TEXT_SECONDARY))
            } else {
                buildSelectedComponentAdvancedSettings(ctx, bottomPane, root, activeChildren, selectedComp)
            }
        }
        "line" -> {
            val activeLine = lines.optJSONObject(selectedLineIndex)
            if (activeLine == null) {
                bottomPane.addView(UiKit.label(ctx, "请先选择或新建组件行", 12f, UiKit.TEXT_SECONDARY))
            } else {
                bottomPane.addView(UiKit.label(ctx, "第 ${selectedLineIndex + 1} 行属性配置", 13f, UiKit.TEXT_PRIMARY, true))
                bottomPane.addView(UiKit.switchRow(ctx, "启用此组件行", "", activeLine.optBoolean("IsEnabled", true)) { v ->
                    activeLine.put("IsEnabled", v)
                    commitJsonAndRefresh(root)
                })
                bottomPane.addView(UiKit.switchRow(ctx, "设为主要行", "主界面核心展示行", activeLine.optBoolean("IsMainLine", false)) { v ->
                    if (v) {
                        for (j in 0 until lines.length()) lines.optJSONObject(j)?.put("IsMainLine", j == selectedLineIndex)
                    } else {
                        activeLine.put("IsMainLine", false)
                    }
                    commitJsonAndRefresh(root)
                })
                bottomPane.addView(UiKit.switchRow(ctx, "承载课堂提醒横幅", "在此行显示上下课及广播通知", activeLine.optBoolean("IsNotificationEnabled", false)) { v ->
                    activeLine.put("IsNotificationEnabled", v)
                    commitJsonAndRefresh(root)
                })
                bottomPane.addView(UiKit.label(ctx, "岛屿分离模式:", 12f, UiKit.TEXT_SECONDARY))
                val sepOpts = listOf("0" to "跟随全局设置", "1" to "合并为整体条", "2" to "按组件分离为独立岛屿")
                bottomPane.addView(UiKit.chipRow(ctx, sepOpts, activeLine.optInt("IslandSeparationMode", 0).toString()) { s ->
                    activeLine.put("IslandSeparationMode", s.toIntOrNull() ?: 0)
                    commitJsonAndRefresh(root)
                })
            }
        }
    }
    container.addView(bottomPane)
}

private fun ProfileEditorComponent.resolveActiveChildrenArray(lines: JSONArray): JSONArray {
    if (lines.length() == 0) {
        lines.put(JSONObject().apply {
            put("IsMainLine", true)
            put("IsNotificationEnabled", true)
            put("IsEnabled", true)
            put("Children", JSONArray())
        })
    }
    val line = lines.optJSONObject(selectedLineIndex) ?: lines.optJSONObject(0) ?: return JSONArray()
    var curChildren = line.optJSONArray("Children") ?: JSONArray().also { line.put("Children", it) }
    val bc = containerStack.lastOrNull() ?: return curChildren
    for (idx in bc.pathIndices) {
        val comp = curChildren.optJSONObject(idx) ?: break
        val settings = comp.optJSONObject("Settings") ?: JSONObject().also { comp.put("Settings", it) }
        curChildren = settings.optJSONArray("Children") ?: JSONArray().also { settings.put("Children", it) }
    }
    return curChildren
}

private fun ProfileEditorComponent.addComponentToActiveChildren(root: JSONObject, lines: JSONArray, compId: String) {
    val def = BUILTIN_COMPONENTS.find { it.id == compId } ?: return
    val target = resolveActiveChildrenArray(lines)
    target.put(JSONObject().apply {
        put("Id", def.id)
        put("NameCache", def.name)
        put("HorizontalAlignment", 1)
        put("Settings", JSONObject(def.defaultSettingsJson))
    })
    selectedCompIndex = target.length() - 1
    commitJsonAndRefresh(root)
}

private fun ProfileEditorComponent.buildSelectedComponentSpecificSettings(
    ctx: Context,
    pane: LinearLayout,
    root: JSONObject,
    comp: JSONObject
) {
    val cid = comp.optString("Id")
    val def = BUILTIN_COMPONENTS.find { it.id.equals(cid, ignoreCase = true) }
    val settings = comp.optJSONObject("Settings") ?: JSONObject(def?.defaultSettingsJson ?: "{}").also { comp.put("Settings", it) }

    pane.addView(UiKit.label(ctx, "${def?.name ?: "组件"} · 专属设置", 13f, UiKit.ACCENT_BLUE, true))

    when (cid.lowercase()) {
        "1db2017d-e374-4bc6-9d57-0b4adf03a6b8" -> { // 课程表
            pane.addView(UiKit.switchRow(ctx, "课间及放学后显示次日课表", "", settings.optBoolean("ShowTomorrowSchedule", true)) { v ->
                settings.put("ShowTomorrowSchedule", v)
                commitJsonAndRefresh(root)
            })
            pane.addView(UiKit.switchRow(ctx, "上课时仅显示当前科目", "", settings.optBoolean("ShowCurrentLessonOnlyOnClass", false)) { v ->
                settings.put("ShowCurrentLessonOnlyOnClass", v)
                commitJsonAndRefresh(root)
            })
            pane.addView(UiKit.switchRow(ctx, "启用上下课倒计时显示", "", settings.optBoolean("IsCountdownEnabled", true)) { v ->
                settings.put("IsCountdownEnabled", v)
                commitJsonAndRefresh(root)
            })
            pane.addView(UiKit.switchRow(ctx, "高亮标记换课科目", "", settings.optBoolean("HighlightChangedClass", true)) { v ->
                settings.put("HighlightChangedClass", v)
                commitJsonAndRefresh(root)
            })
            pane.addView(UiKit.switchRow(ctx, "半透明淡化已上完课程", "", settings.optBoolean("FadeCompletedClasses", true)) { v ->
                settings.put("FadeCompletedClasses", v)
                commitJsonAndRefresh(root)
            })
            val cdEd = UiKit.input(ctx, "倒计时触发秒数", settings.optInt("CountdownSeconds", 60).toString())
            pane.addView(cdEd)
            pane.addView(UiKit.button(ctx, "保存课表参数", UiKit.BTN_MUTED, UiKit.TEXT_PRIMARY, true) {
                settings.put("CountdownSeconds", cdEd.text.toString().toIntOrNull() ?: 60)
                commitJsonAndRefresh(root)
            })
        }
        "9e1af71d-8f77-4b21-a342-448787104dd9" -> { // 时钟
            val styleOpts = listOf("0" to "数字时钟", "1" to "指针表盘")
            pane.addView(UiKit.chipRow(ctx, styleOpts, settings.optInt("Style", 0).toString()) { s ->
                settings.put("Style", s.toIntOrNull() ?: 0)
                commitJsonAndRefresh(root)
            })
            pane.addView(UiKit.switchRow(ctx, "显示秒数", "", settings.optBoolean("ShowSeconds", false)) { v ->
                settings.put("ShowSeconds", v)
                commitJsonAndRefresh(root)
            })
            pane.addView(UiKit.switchRow(ctx, "闪烁时间冒号分隔符", "", settings.optBoolean("FlashTimeSeparator", true)) { v ->
                settings.put("FlashTimeSeparator", v)
                commitJsonAndRefresh(root)
            })
            pane.addView(UiKit.switchRow(ctx, "忽略时间偏移显示系统真实时间", "", settings.optBoolean("ShowRealTime", false)) { v ->
                settings.put("ShowRealTime", v)
                commitJsonAndRefresh(root)
            })
        }
        "ca495086-e297-4beb-9603-c5c1c1a8551e" -> { // 天气简报
            pane.addView(UiKit.switchRow(ctx, "显示气象灾害预警标签", "", settings.optBoolean("ShowAlerts", true)) { v ->
                settings.put("ShowAlerts", v)
                commitJsonAndRefresh(root)
            })
            pane.addView(UiKit.switchRow(ctx, "显示降水临近提醒", "", settings.optBoolean("ShowRainTime", true)) { v ->
                settings.put("ShowRainTime", v)
                commitJsonAndRefresh(root)
            })
            val kindOpts = listOf("0" to "天气与气温", "1" to "湿度", "2" to "风力", "3" to "空气质量 AQI", "4" to "气压", "5" to "体感温度")
            pane.addView(UiKit.chipRow(ctx, kindOpts, settings.optInt("MainWeatherInfoKind", 0).toString()) { k ->
                settings.put("MainWeatherInfoKind", k.toIntOrNull() ?: 0)
                commitJsonAndRefresh(root)
            })
        }
        "7c645d35-8151-48ba-b4ac-15017460d994" -> { // 倒计时
            val nameEd = UiKit.input(ctx, "倒计时事件名称", settings.optString("CountDownName", "高考"))
            val dateEd = UiKit.input(ctx, "目标日期", settings.optString("OverTime", "2026-06-07").take(10))
            val fmtEd = UiKit.input(ctx, "自定义格式 (%n=名称, %d=天数)", settings.optString("CustomStringFormat", "距离 %n 还有 %d"))
            val colorEd = UiKit.input(ctx, "字体颜色", settings.optString("FontColor", "#FF0078D4"))
            pane.addView(nameEd)
            pane.addView(dateEd)
            pane.addView(fmtEd)
            pane.addView(colorEd)
            pane.addView(UiKit.switchRow(ctx, "启用紧凑模式", "", settings.optBoolean("IsCompactModeEnabled", false)) { v ->
                settings.put("IsCompactModeEnabled", v)
                commitJsonAndRefresh(root)
            })
            pane.addView(UiKit.button(ctx, "保存倒计时设置", UiKit.ACCENT_BLUE, Color.WHITE, true) {
                settings.put("CountDownName", nameEd.text.toString().trim())
                settings.put("OverTime", "${dateEd.text.toString().trim()}T00:00:00")
                settings.put("CustomStringFormat", fmtEd.text.toString())
                settings.put("FontColor", colorEd.text.toString().trim())
                commitJsonAndRefresh(root)
            })
        }
        "ee8f66bd-c423-4e7c-ab46-aa9976b00e08" -> { // 文本
            val txtEd = UiKit.input(ctx, "显示的自定义文本内容", settings.optString("TextContent", "自定义文本"))
            val colEd = UiKit.input(ctx, "自定义字体颜色", settings.optString("FontColor", "#FF0078D4"))
            val sizeEd = UiKit.input(ctx, "自定义字号", settings.optInt("FontSize", 16).toString())
            pane.addView(txtEd)
            pane.addView(colEd)
            pane.addView(sizeEd)
            pane.addView(UiKit.switchRow(ctx, "使用自定义字体颜色", "", settings.optBoolean("UseCustomFontColor", false)) { v ->
                settings.put("UseCustomFontColor", v)
                commitJsonAndRefresh(root)
            })
            pane.addView(UiKit.switchRow(ctx, "使用自定义字号", "", settings.optBoolean("UseCustomFontSize", false)) { v ->
                settings.put("UseCustomFontSize", v)
                commitJsonAndRefresh(root)
            })
            pane.addView(UiKit.button(ctx, "保存文本设置", UiKit.ACCENT_BLUE, Color.WHITE, true) {
                settings.put("TextContent", txtEd.text.toString())
                settings.put("FontColor", colEd.text.toString().trim())
                settings.put("FontSize", sizeEd.text.toString().toIntOrNull() ?: 16)
                commitJsonAndRefresh(root)
            })
        }
        "7e19a113-d281-4f33-970a-834a0b78b5ad" -> { // 轮播容器
            val stayEd = UiKit.input(ctx, "单个组件停留秒数", settings.optDouble("StayTimeSeconds", 15.0).toString())
            pane.addView(stayEd)
            val modeOpts = listOf("0" to "循环轮播", "1" to "往返轮播", "2" to "随机轮播")
            pane.addView(UiKit.chipRow(ctx, modeOpts, settings.optInt("AnimationMode", 0).toString()) { m ->
                settings.put("AnimationMode", m.toIntOrNull() ?: 0)
                commitJsonAndRefresh(root)
            })
            pane.addView(UiKit.button(ctx, "保存轮播设置", UiKit.BTN_MUTED, UiKit.TEXT_PRIMARY, true) {
                settings.put("StayTimeSeconds", stayEd.text.toString().toDoubleOrNull() ?: 15.0)
                commitJsonAndRefresh(root)
            })
        }
        "70fcd5ea-3fae-4e06-aca2-4f4df47f9acd" -> { // 滚动容器
            val spdEd = UiKit.input(ctx, "滚动速度", settings.optDouble("SpeedPixelPerSecond", 40.0).toString())
            val spcEd = UiKit.input(ctx, "首尾间距", settings.optDouble("Spacing", 40.0).toString())
            pane.addView(spdEd)
            pane.addView(spcEd)
            pane.addView(UiKit.switchRow(ctx, "启用首尾无缝连续滚动", "", settings.optBoolean("Continuous", true)) { v ->
                settings.put("Continuous", v)
                commitJsonAndRefresh(root)
            })
            pane.addView(UiKit.button(ctx, "保存滚动设置", UiKit.BTN_MUTED, UiKit.TEXT_PRIMARY, true) {
                settings.put("SpeedPixelPerSecond", spdEd.text.toString().toDoubleOrNull() ?: 40.0)
                settings.put("Spacing", spcEd.text.toString().toDoubleOrNull() ?: 40.0)
                commitJsonAndRefresh(root)
            })
        }
        else -> {
            pane.addView(UiKit.label(ctx, "该组件无额外专属配置项，或可在 JSON 源码模式编辑 Settings。", 12f, UiKit.TEXT_SECONDARY))
        }
    }
}

private fun ProfileEditorComponent.buildSelectedComponentAdvancedSettings(
    ctx: Context,
    pane: LinearLayout,
    root: JSONObject,
    activeChildren: JSONArray,
    comp: JSONObject
) {
    pane.addView(UiKit.label(ctx, "水平对齐方式:", 12f, UiKit.TEXT_SECONDARY, true))
    val alignOpts = listOf("0" to "拉伸", "1" to "左对齐", "2" to "居中", "3" to "右对齐")
    pane.addView(UiKit.chipRow(ctx, alignOpts, comp.optInt("HorizontalAlignment", 1).toString()) { a ->
        comp.put("HorizontalAlignment", a.toIntOrNull() ?: 1)
        commitJsonAndRefresh(root)
    })

    pane.addView(UiKit.switchRow(ctx, "按条件规则自动隐藏组件", "", comp.optBoolean("HideOnRule", false)) { v ->
        comp.put("HideOnRule", v)
        commitJsonAndRefresh(root)
    })
    pane.addView(UiKit.switchRow(ctx, "固定组件宽度", "", comp.optBoolean("IsFixedWidthEnabled", false)) { v ->
        comp.put("IsFixedWidthEnabled", v)
        commitJsonAndRefresh(root)
    })

    val wRow = UiKit.hBox(ctx)
    val fwEd = UiKit.input(ctx, "固定宽度", comp.optDouble("FixedWidth", 120.0).toString()).apply {
        layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply { rightMargin = UiKit.dp(ctx, 6) }
    }
    val opEd = UiKit.input(ctx, "组件不透明度", comp.optDouble("Opacity", 1.0).toString()).apply {
        layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
    }
    wRow.addView(fwEd)
    wRow.addView(opEd)
    pane.addView(wRow)

    pane.addView(UiKit.button(ctx, "应用宽度与透明度", UiKit.BTN_MUTED, UiKit.TEXT_PRIMARY, true) {
        comp.put("FixedWidth", fwEd.text.toString().toDoubleOrNull() ?: 120.0)
        comp.put("Opacity", opEd.text.toString().toDoubleOrNull() ?: 1.0)
        commitJsonAndRefresh(root)
    })

    val actRow = UiKit.hBox(ctx)
    actRow.addView(UiKit.button(ctx, "复制此组件", UiKit.BTN_MUTED, UiKit.ACCENT_BLUE, true) {
        activeChildren.put(JSONObject(comp.toString()))
        commitJsonAndRefresh(root)
    })
    actRow.addView(UiKit.button(ctx, "包装进分组容器", UiKit.BTN_MUTED, UiKit.TEXT_PRIMARY, true) {
        val wrapped = JSONObject().apply {
            put("Id", "c911d762-107f-40c6-84cc-0146ab3c86b1")
            put("NameCache", "分组容器")
            put("HorizontalAlignment", comp.optInt("HorizontalAlignment", 1))
            put("Settings", JSONObject().put("Children", JSONArray().put(JSONObject(comp.toString()))))
        }
        activeChildren.put(selectedCompIndex, wrapped)
        commitJsonAndRefresh(root)
    })
    pane.addView(actRow)
}

private fun countLegacyComponentGuids(lines: JSONArray): Int {
    var count = 0
    for (i in 0 until lines.length()) {
        val children = lines.optJSONObject(i)?.optJSONArray("Children") ?: continue
        count += countLegacyInChildren(children)
    }
    return count
}

private fun countLegacyInChildren(children: JSONArray): Int {
    var count = 0
    for (i in 0 until children.length()) {
        val c = children.optJSONObject(i) ?: continue
        val id = c.optString("Id")
        if (LEGACY_COMPONENT_GUID_MAP.containsKey(id)) count++
        val nested = c.optJSONObject("Settings")?.optJSONArray("Children")
        if (nested != null) count += countLegacyInChildren(nested)
    }
    return count
}

private fun migrateLegacyComponentGuids(lines: JSONArray) {
    for (i in 0 until lines.length()) {
        val children = lines.optJSONObject(i)?.optJSONArray("Children") ?: continue
        migrateLegacyInChildren(children)
    }
}

private fun migrateLegacyInChildren(children: JSONArray) {
    for (i in 0 until children.length()) {
        val c = children.optJSONObject(i) ?: continue
        val id = c.optString("Id")
        val newId = LEGACY_COMPONENT_GUID_MAP[id]
        if (newId != null) {
            c.put("Id", newId)
            val def = BUILTIN_COMPONENTS.find { it.id == newId }
            if (def != null) c.put("NameCache", def.name)
        }
        val nested = c.optJSONObject("Settings")?.optJSONArray("Children")
        if (nested != null) migrateLegacyInChildren(nested)
    }
}

// ==================== 7. CREDENTIALS EDITOR (SALTED SHA-256 ENCODE + 9 AUTHORIZE LEVELS) ====================
internal fun ProfileEditorComponent.buildCredentialsVisual(ctx: Context, container: LinearLayout, root: JSONObject) {
    container.addView(UiKit.label(ctx, "客户端本地授权凭据配置", 14f, UiKit.TEXT_PRIMARY, true))
    container.addView(UiKit.label(ctx, "输入明文密码后点击「生成加密凭据串」，系统将自动调用后端生成兼容上游的加盐哈希串", 12f, UiKit.TEXT_SECONDARY))

    val userBox = UiKit.subCard(ctx, 10)
    userBox.addView(UiKit.label(ctx, "普通用户凭据:", 12f, UiKit.TEXT_PRIMARY, true))
    userBox.addView(UiKit.label(
        ctx,
        root.optString("UserCredential").ifBlank { "当前未设置普通用户密码" },
        11f,
        UiKit.TEXT_SECONDARY
    ))
    val uRow = UiKit.hBox(ctx)
    val uPassEd = UiKit.input(ctx, "输入新普通用户明文密码", plainUserPasswordInput, isPassword = true).apply {
        layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply { rightMargin = UiKit.dp(ctx, 6) }
    }
    uRow.addView(uPassEd)
    uRow.addView(UiKit.button(ctx, "生成凭据串", UiKit.ACCENT_BLUE, Color.WHITE, true) {
        val pwd = uPassEd.text.toString()
        if (pwd.isEmpty()) {
            act.showBanner("请输入普通用户密码", true)
            return@button
        }
        act.runAsync({ ApiClient.encodeCredential(act.currentAccountId, pwd) }) { res ->
            if (res.ok) {
                val cred = res.json.optString("credential")
                root.put("UserCredential", cred)
                plainUserPasswordInput = ""
                commitJsonAndRefresh(root)
            } else {
                act.showBanner("生成凭据失败: ${res.error}", true)
            }
        }
    })
    uRow.addView(UiKit.button(ctx, "清空", UiKit.BTN_MUTED, UiKit.TEXT_SECONDARY, true) {
        root.put("UserCredential", "")
        commitJsonAndRefresh(root)
    })
    userBox.addView(uRow)
    container.addView(userBox)

    val adminBox = UiKit.subCard(ctx, 10)
    adminBox.addView(UiKit.label(ctx, "管理员凭据:", 12f, UiKit.TEXT_PRIMARY, true))
    adminBox.addView(UiKit.label(
        ctx,
        root.optString("AdminCredential").ifBlank { "当前未设置管理员密码" },
        11f,
        UiKit.TEXT_SECONDARY
    ))
    val aRow = UiKit.hBox(ctx)
    val aPassEd = UiKit.input(ctx, "输入新管理员明文密码", plainAdminPasswordInput, isPassword = true).apply {
        layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply { rightMargin = UiKit.dp(ctx, 6) }
    }
    aRow.addView(aPassEd)
    aRow.addView(UiKit.button(ctx, "生成凭据串", UiKit.ACCENT_BLUE, Color.WHITE, true) {
        val pwd = aPassEd.text.toString()
        if (pwd.isEmpty()) {
            act.showBanner("请输入管理员密码", true)
            return@button
        }
        act.runAsync({ ApiClient.encodeCredential(act.currentAccountId, pwd) }) { res ->
            if (res.ok) {
                val cred = res.json.optString("credential")
                root.put("AdminCredential", cred)
                plainAdminPasswordInput = ""
                commitJsonAndRefresh(root)
            } else {
                act.showBanner("生成凭据失败: ${res.error}", true)
            }
        }
    })
    aRow.addView(UiKit.button(ctx, "清空", UiKit.BTN_MUTED, UiKit.TEXT_SECONDARY, true) {
        root.put("AdminCredential", "")
        commitJsonAndRefresh(root)
    })
    adminBox.addView(aRow)
    container.addView(adminBox)

    val authLevels = listOf("0" to "无需认证", "1" to "普通用户认证", "2" to "管理员认证")
    val actions = listOf(
        Triple("EditAuthorizeSettingsAuthorizeLevel", "编辑授权设置", 2),
        Triple("EditPolicyAuthorizeLevel", "编辑集控策略", 2),
        Triple("ExitManagementAuthorizeLevel", "退出集控管理", 2),
        Triple("EditProfileAuthorizeLevel", "编辑本地档案", 0),
        Triple("EditSettingsAuthorizeLevel", "修改应用设置", 0),
        Triple("ExitApplicationAuthorizeLevel", "关闭或退出应用", 0),
        Triple("EditComponentsAuthorizeLevel", "编辑组件布局", 0),
        Triple("PluginManagementAuthorizeLevel", "管理插件市场", 0),
        Triple("ChangeLessonsAuthorizeLevel", "临时换课操作", 0)
    )
    actions.forEach { (key, title, defLv) ->
        val box = UiKit.subCard(ctx, 10)
        box.addView(UiKit.label(ctx, title, 12f, UiKit.TEXT_PRIMARY, true))
        box.addView(UiKit.chipRow(ctx, authLevels, root.optInt(key, defLv).toString()) { lv ->
            root.put(key, lv.toIntOrNull() ?: defLv)
            commitJsonAndRefresh(root)
        })
        container.addView(box)
    }
}
