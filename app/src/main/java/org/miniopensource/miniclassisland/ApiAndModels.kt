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

data class BuiltinComponentDef(val id: String, val name: String, val desc: String)

val BUILTIN_COMPONENTS = listOf(
    BuiltinComponentDef("1507b9a2-7884-4937-9499-4a1b54f4c855", "课表", "显示当前课程表与上下课状态"),
    BuiltinComponentDef("908c5531-f80c-4c32-89d2-f935455d65c8", "时钟", "显示当前精确时间（修正后）"),
    BuiltinComponentDef("f4c9a059-55b0-42b0-9b09-f506e068d801", "日期", "显示今日日期与星期"),
    BuiltinComponentDef("6fe32b24-b78c-4c62-b8c4-3f6838d167a8", "天气简报", "显示实时天气温度与预警"),
    BuiltinComponentDef("a79923a8-71d0-43b9-b9b2-611c8bf7186a", "倒计时", "显示距离特定日期剩余天数"),
    BuiltinComponentDef("df3f8295-21F6-482e-ada2-fa0e52b2620f", "自定义文本", "显示固定标语或提示文本"),
    BuiltinComponentDef("c2f3f06e-b62c-46a0-a080-322c0c492d91", "分隔线", "用于组件间的视觉分隔"),
    BuiltinComponentDef("b51c85d7-2069-413b-8347-5a8116eb209b", "轮播组件容器", "定时轮播多个子组件"),
    BuiltinComponentDef("e3148204-5f12-40a2-9c35-2b259c039a2b", "分组容器", "将多个组件组合显示")
)

val AUDIT_EVENT_LABELS = mapOf(
    0 to "未指定",
    1 to "应用启动",
    2 to "应用崩溃",
    3 to "应用退出",
    4 to "插件安装",
    5 to "插件卸载",
    6 to "时间偏移变更",
    7 to "授权认证成功",
    8 to "授权认证失败",
    9 to "设置修改",
    10 to "档案修改",
    11 to "退出集控",
    12 to "集控设置变更",
    13 to "同步失败"
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
    return if (s.length >= 8) s.substring(0, 8) else s
}

data class HttpRes(
    val ok: Boolean,
    val code: Int,
    val json: JSONObject = JSONObject(),
    val rawText: String = "",
    val error: String = "",
    val isOobe: Boolean = false
)

object ApiClient {
    var managementUrl: String = "https://api.miniclassisland.com"
    var adminUrl: String = "https://admin.miniclassisland.com/api"
    var token: String = ""

    fun setEndpoints(mgmt: String, adm: String) {
        val m = mgmt.trim().trimEnd('/')
        val a = adm.trim().trimEnd('/')
        if (m.isNotEmpty()) managementUrl = m
        if (a.isNotEmpty()) adminUrl = a
    }

    fun enc(s: String): String = URLEncoder.encode(s, "UTF-8")

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
                val msg = parsed.optString("message").ifEmpty {
                    parsed.optString("error").ifEmpty { "请求失败 (HTTP $code)" }
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
                put("ClassPlans", JSONObject().apply {
                    put(id, JSONObject().apply {
                        put("Name", "周一课表")
                        put("TimeLayoutId", "")
                        put("IsEnabled", true)
                        put("IsOverlay", false)
                        put("TimeRule", JSONObject().apply {
                            put("WeekDay", 1)
                            put("WeekCountDiv", 0)
                            put("WeekCountDivTotal", 2)
                        })
                        put("Classes", JSONArray().apply {
                            put(JSONObject().put("SubjectId", ""))
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
                put("IsNotificationEnabled", true)
                put("IsNotificationEffectEnabled", true)
                put("IsNotificationSoundEnabled", true)
                put("IsSpeechEnabled", true)
                put("SpeechVolume", 1.0)
                put("Theme", 0)
                put("PrimaryColor", "#3B82F6")
                put("TimeOffsetSeconds", 0.0)
            }.toString(2)
            "Components" -> JSONObject().apply {
                put("Lines", JSONArray().apply {
                    put(JSONObject().apply {
                        put("IsMainLine", true)
                        put("IsEnabled", true)
                        put("IslandSeparationMode", 0)
                        put("Children", JSONArray().apply {
                            put(JSONObject().apply {
                                put("Id", "1507b9a2-7884-4937-9499-4a1b54f4c855")
                                put("NameCache", "课表")
                                put("HorizontalAlignment", 1)
                            })
                        })
                    })
                })
            }.toString(2)
            "Credentials" -> JSONObject().apply {
                put("UserCredential", "")
                put("AdminCredential", "")
                put("EditProfileAuthorizeLevel", 0)
                put("EditPolicyAuthorizeLevel", 2)
                put("ExitManagementAuthorizeLevel", 2)
                put("EditSettingsAuthorizeLevel", 1)
                put("SwitchProfileAuthorizeLevel", 1)
                put("TempOverlayClassPlanAuthorizeLevel", 1)
                put("ChangeTimeOffsetAuthorizeLevel", 1)
                put("ExitApplicationAuthorizeLevel", 2)
                put("RestartApplicationAuthorizeLevel", 1)
            }.toString(2)
            else -> "{}"
        }
    }
}
