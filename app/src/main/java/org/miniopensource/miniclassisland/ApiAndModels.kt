package org.miniopensource.miniclassisland

import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

const val EMPTY_GUID = "00000000-0000-0000-0000-000000000000"

fun isEmptyGuid(id: String?): Boolean {
    if (id.isNullOrBlank()) return true
    return id.trim() == EMPTY_GUID
}

fun toUpstreamGuidOrEmpty(id: String?): String {
    if (id.isNullOrBlank()) return EMPTY_GUID
    return id.trim()
}

data class BuiltinComponentDef(
    val id: String,
    val name: String,
    val desc: String,
    val isContainer: Boolean = false,
    val hasSettings: Boolean = false,
    val category: String = "basic",
    val defaultSettingsJson: String = "{}"
)

val BUILTIN_COMPONENTS = listOf(
    BuiltinComponentDef(
        id = "1db2017d-e374-4bc6-9d57-0b4adf03a6b8",
        name = "课程表",
        desc = "显示当前的课程表信息。",
        isContainer = false,
        hasSettings = true,
        category = "basic",
        defaultSettingsJson = """{"ShowExtraInfoOnTimePoint":true,"ExtraInfoType":0,"IsCountdownEnabled":true,"CountdownSeconds":60,"ScheduleSpacing":1,"ShowCurrentLessonOnlyOnClass":false,"IsNonExactCountdownEnabled":false,"ShowTomorrowSchedule":true,"TomorrowScheduleShowMode":0,"HighlightChangedClass":true,"FadeCompletedClasses":true}"""
    ),
    BuiltinComponentDef(
        id = "9e1af71d-8f77-4b21-a342-448787104dd9",
        name = "时钟",
        desc = "显示现在的时间，支持指针表盘或数字时钟。",
        isContainer = false,
        hasSettings = true,
        category = "basic",
        defaultSettingsJson = """{"Style":0,"ShowSeconds":false,"FlashTimeSeparator":true,"ShowRealTime":false,"DialBorderThickness":1.5,"ShowHourScale":true,"ShowMinuteScale":false}"""
    ),
    BuiltinComponentDef(
        id = "df3f8295-21f6-482e-bada-fa0e5f14bb66",
        name = "日期",
        desc = "显示今天的日期和星期。",
        isContainer = false,
        hasSettings = false,
        category = "basic"
    ),
    BuiltinComponentDef(
        id = "ca495086-e297-4beb-9603-c5c1c1a8551e",
        name = "天气简报",
        desc = "显示简略的天气信息与气象预警。",
        isContainer = false,
        hasSettings = true,
        category = "basic",
        defaultSettingsJson = """{"ShowAlerts":true,"ShowRainTime":true,"MainWeatherInfoKind":0,"AlertsTitleShowMode":1}"""
    ),
    BuiltinComponentDef(
        id = "7c645d35-8151-48ba-b4ac-15017460d994",
        name = "倒计时",
        desc = "计算并显示距离某一天的剩余天数。",
        isContainer = false,
        hasSettings = true,
        category = "basic",
        defaultSettingsJson = """{"CountDownName":"高考","OverTime":"2026-06-07T00:00:00","FontColor":"#FF0078D4","FontSize":16,"CustomStringFormat":"距离 %n 还有 %d","IsCompactModeEnabled":false,"ShowProgress":false,"StartTime":"2026-01-01T00:00:00","IsConnectorColored":false}"""
    ),
    BuiltinComponentDef(
        id = "ee8f66bd-c423-4e7c-ab46-aa9976b00e08",
        name = "文本",
        desc = "显示自定义文本内容。",
        isContainer = false,
        hasSettings = true,
        category = "basic",
        defaultSettingsJson = """{"TextContent":"自定义文本","UseCustomFontColor":false,"FontColor":"#FF0078D4","UseCustomFontSize":false,"FontSize":16}"""
    ),
    BuiltinComponentDef(
        id = "ab0f26d5-9df6-4575-b844-73b04d0907c1",
        name = "分割线",
        desc = "显示一条分割线，用于视觉分隔。",
        isContainer = false,
        hasSettings = false,
        category = "basic"
    ),
    BuiltinComponentDef(
        id = "c911d762-107f-40c6-84cc-0146ab3c86b1",
        name = "分组容器",
        desc = "将多个组件合为一组横向排列展示。",
        isContainer = true,
        hasSettings = false,
        category = "container",
        defaultSettingsJson = """{"Children":[]}"""
    ),
    BuiltinComponentDef(
        id = "2d849ece-9f21-4c78-9434-415cfc283294",
        name = "堆叠容器",
        desc = "将组件在垂直方向上堆叠，并自动调整布局。",
        isContainer = true,
        hasSettings = false,
        category = "container",
        defaultSettingsJson = """{"Children":[]}"""
    ),
    BuiltinComponentDef(
        id = "7e19a113-d281-4f33-970a-834a0b78b5ad",
        name = "轮播容器",
        desc = "轮流展示容器中的各个子组件。",
        isContainer = true,
        hasSettings = true,
        category = "container",
        defaultSettingsJson = """{"Children":[],"StayTimeSeconds":15.0,"AnimationMode":0,"SlideDirection":0,"IsPartly":true,"IsLoop":true,"StopOnRule":false}"""
    ),
    BuiltinComponentDef(
        id = "70fcd5ea-3fae-4e06-aca2-4f4df47f9acd",
        name = "滚动容器",
        desc = "以跑马灯形式循环滚动显示内部组件。",
        isContainer = true,
        hasSettings = true,
        category = "container",
        defaultSettingsJson = """{"Children":[],"SpeedPixelPerSecond":40.0,"IsPauseEnabled":false,"PauseSeconds":5.0,"PauseOffsetX":0.0,"PauseRuleMode":0,"Spacing":40.0,"Continuous":true,"StartDelaySeconds":3.0,"StopOnRule":false}"""
    )
)

val LEGACY_COMPONENT_GUID_MAP = mapOf(
    "1507b9a2-7884-4937-9499-4a1b54f4c855" to "1db2017d-e374-4bc6-9d57-0b4adf03a6b8",
    "908c5531-f80c-4c32-89d2-f935455d65c8" to "9e1af71d-8f77-4b21-a342-448787104dd9",
    "f4c9a059-55b0-42b0-9b09-f506e068d801" to "df3f8295-21f6-482e-bada-fa0e5f14bb66",
    "6fe32b24-b78c-4c62-b8c4-3f6838d167a8" to "ca495086-e297-4beb-9603-c5c1c1a8551e",
    "a79923a8-71d0-43b9-b9b2-611c8bf7186a" to "7c645d35-8151-48ba-b4ac-15017460d994",
    "df3f8295-21f6-482e-ada2-fa0e52b2620f" to "ee8f66bd-c423-4e7c-ab46-aa9976b00e08",
    "df3f8295-21F6-482e-ada2-fa0e52b2620f" to "ee8f66bd-c423-4e7c-ab46-aa9976b00e08",
    "c2f3f06e-b62c-46a0-a080-322c0c492d91" to "ab0f26d5-9df6-4575-b844-73b04d0907c1",
    "b51c85d7-2069-413b-8347-5a8116eb209b" to "7e19a113-d281-4f33-970a-834a0b78b5ad",
    "e3148204-5f12-40a2-9c35-2b259c039a2b" to "c911d762-107f-40c6-84cc-0146ab3c86b1"
)

data class BuiltinActionDef(
    val id: String,
    val name: String,
    val category: String,
    val desc: String,
    val defaultSettingsJson: String
)

val BUILTIN_ACTIONS = listOf(
    BuiltinActionDef(
        id = "classisland.os.run",
        name = "运行",
        category = "操作系统",
        desc = "运行应用程序、命令、打开文件或文件夹、打开网页链接",
        defaultSettingsJson = """{"RunType":0,"Value":"","Args":""}"""
    ),
    BuiltinActionDef(
        id = "classisland.showNotification",
        name = "显示提醒",
        category = "提醒",
        desc = "触发自定义课堂桌面横幅提醒与语音播报",
        defaultSettingsJson = """{"Mask":"提醒","MaskDurationSeconds":3,"IsMaskSpeechEnabled":true,"Content":"","ContentDurationSeconds":10,"IsContentSpeechEnabled":true,"IsAdvancedSettingsEnabled":false,"IsEffectEnabled":true,"IsSoundEnabled":true,"IsTopmostEnabled":true,"CustomSoundEffectPath":""}"""
    ),
    BuiltinActionDef(
        id = "classisland.notification.weather",
        name = "发送天气提醒",
        category = "提醒",
        desc = "显示今日天气预报、气象预警或逐小时预报提醒",
        defaultSettingsJson = """{"NotificationKind":0}"""
    ),
    BuiltinActionDef(
        id = "classisland.broadcastSignal",
        name = "广播信号",
        category = "自动化",
        desc = "触发指定名称的信号，用于联动自动化工作流",
        defaultSettingsJson = """{"SignalName":"","IsRevert":false}"""
    ),
    BuiltinActionDef(
        id = "classisland.action.sleep",
        name = "等待时长",
        category = "自动化",
        desc = "暂停行动组执行指定的秒数",
        defaultSettingsJson = """{"Value":5.0}"""
    ),
    BuiltinActionDef(
        id = "classisland.settings",
        name = "修改应用设置",
        category = "应用",
        desc = "修改 ClassIsland 的指定设置属性并在行动恢复时还原",
        defaultSettingsJson = """{"Name":"Scale","Value":"1.0"}"""
    ),
    BuiltinActionDef(
        id = "classisland.app.restart",
        name = "重启应用",
        category = "应用",
        desc = "重新启动 ClassIsland 客户端",
        defaultSettingsJson = """{"Value":false}"""
    ),
    BuiltinActionDef(
        id = "classisland.app.quit",
        name = "退出应用",
        category = "应用",
        desc = "安全关闭并退出 ClassIsland 客户端",
        defaultSettingsJson = """{}"""
    )
)

val AUDIT_EVENT_LABELS = mapOf(
    0 to "未指定",
    1 to "认证成功",
    2 to "认证失败",
    3 to "授权设置变更",
    4 to "应用启动",
    5 to "应用崩溃",
    6 to "应用退出",
    7 to "插件安装",
    8 to "插件卸载",
    9 to "课表变更",
    10 to "时间偏移设置变更",
    11 to "策略变更",
    12 to "预注册请求",
    13 to "重复上线拒绝"
)

fun formatTs(ts: Long): String {
    if (ts <= 0L) return "-"
    val ms = if (ts > 100_000_000_000L) ts else ts * 1000L
    return SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date(ms))
}

fun extractTimePart(raw: String): String {
    if (raw.isBlank()) return "08:00:00"
    val tIdx = raw.indexOf('T')
    val s = if (tIdx >= 0) raw.substring(tIdx + 1) else raw
    val parts = s.split(':').toMutableList()
    while (parts.size < 3) parts.add("00")
    return parts.take(3).joinToString(":") { it.take(2).padStart(2, '0') }
}

fun addMinutesToTimePart(raw: String, deltaMinutes: Int): String {
    val clean = extractTimePart(raw)
    val parts = clean.split(':')
    val h = parts.getOrNull(0)?.toIntOrNull() ?: 8
    val m = parts.getOrNull(1)?.toIntOrNull() ?: 0
    val s = parts.getOrNull(2)?.toIntOrNull() ?: 0
    val totalMin = (h * 60 + m + deltaMinutes).coerceIn(0, 23 * 60 + 59)
    val nh = (totalMin / 60).toString().padStart(2, '0')
    val nm = (totalMin % 60).toString().padStart(2, '0')
    val ns = s.toString().padStart(2, '0')
    return "$nh:$nm:$ns"
}

data class HttpRes(
    val ok: Boolean,
    val code: Int,
    val json: JSONObject = JSONObject(),
    val rawText: String = "",
    val error: String = "",
    val isOobe: Boolean = false
) {
    fun optJsonArray(): JSONArray {
        val t = rawText.trim()
        if (t.startsWith("[")) {
            return try {
                JSONArray(t)
            } catch (_: Exception) {
                JSONArray()
            }
        }
        return json.optJSONArray("items") ?: json.optJSONArray("resources") ?: JSONArray()
    }
}

data class StoredLoginCert(
    val certId: String,
    val familyId: String,
    val userId: String,
    val certSecret: String,
    val generation: Int,
    val maxGeneration: Int,
    val issuedAt: Long,
    val expiresAt: Long,
    val deviceName: String = ""
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("cert_id", certId)
        put("family_id", familyId)
        put("user_id", userId)
        put("cert_secret", certSecret)
        put("generation", generation)
        put("max_generation", maxGeneration)
        put("issued_at", issuedAt)
        put("expires_at", expiresAt)
        put("device_name", deviceName)
    }

    fun remainingSeconds(): Long = expiresAt - (System.currentTimeMillis() / 1000L)
    fun isExpired(): Boolean = remainingSeconds() <= 0L
    fun canRotate(): Boolean = remainingSeconds() in 1..86400L && generation < maxGeneration

    companion object {
        fun fromJson(obj: JSONObject?): StoredLoginCert? {
            if (obj == null) return null
            val cid = obj.optString("cert_id").trim()
            val sec = obj.optString("cert_secret").trim()
            if (cid.isEmpty() || sec.isEmpty()) return null
            return StoredLoginCert(
                certId = cid,
                familyId = obj.optString("family_id", cid),
                userId = obj.optString("user_id", ""),
                certSecret = sec,
                generation = obj.optInt("generation", 0),
                maxGeneration = obj.optInt("max_generation", 12),
                issuedAt = obj.optLong("issued_at", 0L),
                expiresAt = obj.optLong("expires_at", 0L),
                deviceName = obj.optString("device_name", "")
            )
        }
    }
}

object ApiClient {
    var managementUrl: String = "https://api.miniclassisland.com"
    var adminUrl: String = "https://admin.miniclassisland.com/api"
    var token: String = ""
    var tokenExpiresAtEpochSec: Long = 0L
    var storedCertificate: StoredLoginCert? = null

    fun extractExchangeCode(codeOrUri: String): String {
        val cleaned = codeOrUri.trim()
        val prefix = "miniclassisland://exchange-certificate/"
        return if (cleaned.lowercase(Locale.ROOT).startsWith(prefix)) {
            cleaned.substring(prefix.length).trim().trim('/')
        } else {
            cleaned
        }
    }

    fun redeemCertificate(codeOrUri: String): HttpRes {
        val code = extractExchangeCode(codeOrUri)
        val body = JSONObject().put("code", code)
        val res = request(managementUrl, "/token/certificate/redeem", "POST", body)
        if (res.ok) {
            val certObj = res.json.optJSONObject("certificate")
            val parsed = StoredLoginCert.fromJson(certObj)
            if (parsed != null) {
                storedCertificate = parsed
            }
            val tok = res.json.optString("token")
            val expIn = res.json.optLong("expires_in", 3600L)
            if (tok.isNotEmpty()) {
                token = tok
                tokenExpiresAtEpochSec = (System.currentTimeMillis() / 1000L) + expIn
            }
        }
        return res
    }

    fun requestSessionFromCertificate(ttlSeconds: Int = 3600): HttpRes {
        val cert = storedCertificate ?: return HttpRes(false, 401, error = "本地未绑定登录安全证书")
        val clampedTtl = ttlSeconds.coerceIn(60, 3600)
        val body = JSONObject()
            .put("cert_id", cert.certId)
            .put("cert_secret", cert.certSecret)
            .put("ttl", clampedTtl)
        val res = request(managementUrl, "/token/certificate/session", "POST", body)
        if (res.ok) {
            val tok = res.json.optString("token")
            val expIn = res.json.optLong("expires_in", clampedTtl.toLong())
            if (tok.isNotEmpty()) {
                token = tok
                tokenExpiresAtEpochSec = (System.currentTimeMillis() / 1000L) + expIn
            }
        }
        return res
    }

    fun rotateCertificateIfEligible(): HttpRes {
        val cert = storedCertificate ?: return HttpRes(false, 401, error = "本地未绑定登录安全证书")
        val body = JSONObject()
            .put("cert_id", cert.certId)
            .put("cert_secret", cert.certSecret)
        val res = request(managementUrl, "/token/certificate/rotate", "POST", body)
        if (res.ok) {
            val newCert = StoredLoginCert.fromJson(res.json.optJSONObject("certificate"))
            if (newCert != null) {
                storedCertificate = newCert
            }
            val tok = res.json.optString("token")
            val expIn = res.json.optLong("expires_in", 3600L)
            if (tok.isNotEmpty()) {
                token = tok
                tokenExpiresAtEpochSec = (System.currentTimeMillis() / 1000L) + expIn
            }
        }
        return res
    }

    fun authorizeWebQrLogin(codeOrUri: String): HttpRes {
        val cleaned = codeOrUri.trim()
        val prefix = "miniclassisland://qr-login/"
        val code = if (cleaned.lowercase(Locale.ROOT).startsWith(prefix)) {
            cleaned.substring(prefix.length).trim().trim('/')
        } else {
            cleaned
        }
        val body = JSONObject().put("code", code)
        val cert = storedCertificate
        if (cert != null && !cert.isExpired()) {
            body.put("cert_id", cert.certId)
            body.put("cert_secret", cert.certSecret)
        }
        return request(managementUrl, "/token/qr-login/authorize", "POST", body)
    }

    fun setEndpoints(mgmt: String, adm: String) {
        val m = mgmt.trim().trimEnd('/')
        val a = adm.trim().trimEnd('/')
        if (m.isNotEmpty()) managementUrl = m
        if (a.isNotEmpty()) adminUrl = a
    }

    fun enc(s: String): String = URLEncoder.encode(s, "UTF-8")

    // ==================== RESOURCE CRUD & VERSION HISTORY ====================
    fun listResourceFiles(accountId: String, type: String): List<String> {
        if (accountId.isBlank()) return emptyList()
        val res = request(managementUrl, "/account/$accountId/$type/list")
        if (!res.ok) return emptyList()
        val arr = res.optJsonArray()
        val list = mutableListOf<String>()
        for (i in 0 until arr.length()) {
            val item = arr.opt(i)
            when (item) {
                is String -> if (item.isNotBlank()) list.add(item)
                is JSONObject -> {
                    val n = item.optString("name")
                    if (n.isNotBlank()) list.add(n)
                }
            }
        }
        return list
    }

    fun readResourceFile(accountId: String, type: String, name: String): HttpRes {
        return request(managementUrl, "/account/$accountId/$type/${enc(name)}")
    }

    fun createResourceFile(accountId: String, type: String, name: String): HttpRes {
        return request(managementUrl, "/account/$accountId/$type/create?name=${enc(name)}")
    }

    fun writeResourceFile(accountId: String, type: String, name: String, payload: JSONObject): HttpRes {
        return request(managementUrl, "/account/$accountId/$type/write?name=${enc(name)}", "POST", payload)
    }

    fun deleteResourceFile(accountId: String, type: String, name: String): HttpRes {
        return request(managementUrl, "/account/$accountId/$type/delete?name=${enc(name)}", "DELETE")
    }

    fun listResourceHistory(accountId: String, type: String, name: String): JSONArray {
        val res = request(managementUrl, "/account/$accountId/$type/history?name=${enc(name)}")
        return if (res.ok) res.optJsonArray() else JSONArray()
    }

    fun readResourceHistoryVersion(accountId: String, type: String, name: String, version: Int): HttpRes {
        return request(managementUrl, "/account/$accountId/$type/history/$version?name=${enc(name)}")
    }

    fun encodeCredential(accountId: String, password: String): HttpRes {
        val body = JSONObject().put("password", password)
        return request(managementUrl, "/account/$accountId/credentials/encode", "POST", body)
    }

    fun request(baseUrl: String, path: String, method: String = "GET", body: JSONObject? = null): HttpRes {
        var conn: HttpURLConnection? = null
        return try {
            val url = URL("$baseUrl$path")
            conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = method
                connectTimeout = 10000
                readTimeout = 15000
                setRequestProperty("Accept", "application/json")
                setRequestProperty("Content-Type", "application/json; charset=UTF-8")
                if (token.isNotEmpty()) {
                    setRequestProperty("Authorization", "Bearer $token")
                }
                if (body != null && (method == "POST" || method == "PUT")) {
                    doOutput = true
                    OutputStreamWriter(outputStream, Charsets.UTF_8).use { writer ->
                        writer.write(body.toString())
                        writer.flush()
                    }
                }
            }

            val code = conn.responseCode
            val stream = if (code in 200..299) conn.inputStream else conn.errorStream
            val text = if (stream != null) {
                BufferedReader(InputStreamReader(stream, Charsets.UTF_8)).use { it.readText() }
            } else {
                ""
            }
            val parsed = try {
                if (text.trim().startsWith("{")) JSONObject(text) else JSONObject()
            } catch (_: Exception) {
                JSONObject()
            }

            if (code == 503 && parsed.optString("code") == "ERR_SYSTEM_OOBE") {
                return HttpRes(false, code, parsed, text, "系统尚未完成 OOBE 初始化", true)
            }
            if (code in 200..299) {
                HttpRes(true, code, parsed, text)
            } else {
                val msg = parsed.optString("detail").ifEmpty {
                    parsed.optString("msg").ifEmpty {
                        parsed.optString("message").ifEmpty {
                            parsed.optString("error").ifEmpty { "请求失败" }
                        }
                    }
                }
                HttpRes(false, code, parsed, text, msg)
            }
        } catch (e: Exception) {
            HttpRes(false, 0, JSONObject(), "", "网络错误: ${e.message ?: "连接异常"}")
        } finally {
            conn?.disconnect()
        }
    }

    fun getDefaultResourceJson(type: String): String {
        val id = UUID.randomUUID().toString()
        return when (type) {
            "Subjects" -> JSONObject().apply {
                put("Name", "默认科目表")
                put("Subjects", JSONObject().apply {
                    put(id, JSONObject().apply {
                        put("Name", "语文")
                        put("Initial", "语")
                        put("TeacherName", "")
                        put("IsOutDoor", false)
                    })
                })
            }.toString(2)
            "TimeLayout" -> JSONObject().apply {
                put("Name", "标准时间表")
                put("TimeLayouts", JSONObject().apply {
                    put(id, JSONObject().apply {
                        put("Name", "常规作息")
                        put("Layouts", JSONArray().apply {
                            put(JSONObject().apply {
                                put("StartSecond", "08:00:00")
                                put("EndSecond", "08:45:00")
                                put("TimeType", 0)
                                put("DefaultClassId", "")
                                put("IsHideDefault", false)
                                put("BreakName", "")
                            })
                        })
                    })
                })
            }.toString(2)
            "ClassPlan" -> JSONObject().apply {
                put("Name", "标准课表")
                put("ClassPlanGroups", JSONObject().apply {
                    put(EMPTY_GUID, JSONObject().apply {
                        put("Name", "默认")
                        put("IsGlobal", false)
                    })
                })
                put("ClassPlans", JSONObject().apply {
                    put(id, JSONObject().apply {
                        put("Name", "周一课表")
                        put("TimeLayoutId", "")
                        put("AssociatedGroup", EMPTY_GUID)
                        put("IsEnabled", true)
                        put("IsOverlay", false)
                        put("TimeRule", JSONObject().apply {
                            put("WeekDay", 1)
                            put("WeekCountDiv", 0)
                            put("WeekCountDivTotal", 2)
                        })
                        put("Classes", JSONArray().apply {
                            put(JSONObject().put("SubjectId", "").put("IsEnabled", true))
                        })
                    })
                })
            }.toString(2)
            "Policy" -> JSONObject().apply {
                put("DisableProfileClassPlanEditing", false)
                put("DisableProfileTimeLayoutEditing", false)
                put("DisableProfileSubjectsEditing", false)
                put("DisableProfileEditing", false)
                put("DisableSettingsEditing", false)
                put("DisableSplashCustomize", false)
                put("DisableDebugMenu", false)
                put("AllowExitManagement", true)
                put("DisableEasterEggs", false)
            }.toString(2)
            "DefaultSettings" -> JSONObject().apply {
                put("WindowDockingLocation", 1)
                put("WindowLayer", 1)
                put("WindowDockingOffsetX", 0)
                put("WindowDockingOffsetY", 0)
                put("Scale", 1.0)
                put("Opacity", 1.0)
                put("IsMainWindowVisible", true)
                put("IsNotificationEnabled", true)
                put("IsNotificationEffectEnabled", true)
                put("IsNotificationSoundEnabled", true)
                put("IsNotificationTopmostEnabled", true)
                put("NotificationSoundVolume", 1.0)
                put("AllowNotificationSpeech", true)
                put("SpeechSource", 0)
                put("SpeechVolume", 1.0)
                put("EdgeTtsVoiceName", "zh-CN-XiaoxiaoNeural")
                put("Theme", 0)
                put("PrimaryColor", "#0090FF")
                put("SecondaryColor", "#0090FF")
                put("TimeOffsetSeconds", 0.0)
                put("IsTimeAutoAdjustEnabled", false)
                put("TimeAutoAdjustServer", "ntp.aliyun.com")
                put("IsAutomationEnabled", false)
                put("SelectedUpdateChannelV2", "stable")
                put("UpdateMode", 2)
            }.toString(2)
            "Components" -> JSONObject().apply {
                put("Lines", JSONArray().apply {
                    put(JSONObject().apply {
                        put("IsMainLine", true)
                        put("IsNotificationEnabled", true)
                        put("IsEnabled", true)
                        put("IslandSeparationMode", 0)
                        put("Children", JSONArray().apply {
                            put(JSONObject().apply {
                                put("Id", "1db2017d-e374-4bc6-9d57-0b4adf03a6b8")
                                put("NameCache", "课程表")
                                put("HorizontalAlignment", 1)
                                put("Settings", JSONObject(BUILTIN_COMPONENTS[0].defaultSettingsJson))
                            })
                        })
                    })
                })
            }.toString(2)
            "Credentials" -> JSONObject().apply {
                put("UserCredential", "")
                put("AdminCredential", "")
                put("EditAuthorizeSettingsAuthorizeLevel", 2)
                put("EditPolicyAuthorizeLevel", 2)
                put("ExitManagementAuthorizeLevel", 2)
                put("EditProfileAuthorizeLevel", 0)
                put("EditSettingsAuthorizeLevel", 0)
                put("ExitApplicationAuthorizeLevel", 0)
                put("EditComponentsAuthorizeLevel", 0)
                put("PluginManagementAuthorizeLevel", 0)
                put("ChangeLessonsAuthorizeLevel", 0)
            }.toString(2)
            else -> "{}"
        }
    }
}
