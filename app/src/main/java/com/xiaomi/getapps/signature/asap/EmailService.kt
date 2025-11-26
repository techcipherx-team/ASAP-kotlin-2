package com.xiaomi.getapps.signature.asap

import android.content.Context
import android.net.Uri
import android.util.Log
import android.accounts.Account
import android.util.Base64
import com.google.android.gms.auth.GoogleAuthUtil
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.common.api.Scope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

object EmailService {

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
        .readTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
        .writeTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
        .build()

    suspend fun sendEmail(
        context: Context,
        to: String,
        subject: String,
        body: String,
        attachments: List<Uri>,
        brandName: String? = null,
        logoUrl: String? = null,
        logoRes: Int = 0
    ): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                val gmailResult = sendViaGmail(context, to, subject, body, attachments, brandName, logoUrl, logoRes)
                if (gmailResult.isSuccess) return@withContext gmailResult
                val apiUrl = try {
                    context.getString(R.string.email_api_url)
                } catch (e: Exception) {
                    null
                }

                if (apiUrl.isNullOrBlank()) {
                    val gmailError = gmailResult.exceptionOrNull()
                    return@withContext Result.failure(gmailError ?: Exception("Gmail not authorized. Please sign in with Google and grant Gmail permission"))
                }

                val builder = MultipartBody.Builder().setType(MultipartBody.FORM)
                builder.addFormDataPart("to", to)
                builder.addFormDataPart("subject", subject)
                builder.addFormDataPart("body", body)

                attachments.forEachIndexed { index, uri ->
                    try {
                        val cr = context.contentResolver
                        val name = queryDisplayName(cr, uri) ?: "attachment_${index}"
                        val input = cr.openInputStream(uri)
                        val bytes = input?.readBytes() ?: ByteArray(0)
                        input?.close()
                        val mime = cr.getType(uri) ?: "application/octet-stream"
                        val reqBody: RequestBody = bytes.toRequestBody(mime.toMediaTypeOrNull())
                        builder.addFormDataPart("files[]", name, reqBody)
                    } catch (e: Exception) {
                        Log.w("EmailService", "Failed to attach file: ${e.message}")
                    }
                }

                val requestBody = builder.build()
                val request = Request.Builder()
                    .url(apiUrl)
                    .post(requestBody)
                    .build()

                val response = client.newCall(request).execute()
                return@withContext if (response.isSuccessful) {
                    try {
                        saveSentEmail(
                            context,
                            SentEmail(
                                id = "local_${System.currentTimeMillis()}",
                                threadId = "",
                                to = to,
                                subject = subject,
                                timestamp = System.currentTimeMillis(),
                                brandName = brandName ?: deriveBrandFromSubject(subject),
                                logoUrl = logoUrl ?: "",
                                logoRes = logoRes
                            )
                        )
                    } catch (_: Exception) {}
                    Result.success(Unit)
                } else {
                    val msg = response.message
                    Result.failure(Exception("Email send failed: ${response.code} - $msg"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    private fun buildMimeMessage(from: String?, to: String, subject: String, body: String, attachments: List<Pair<String, ByteArrayWithMime>>): String {
        return if (attachments.isEmpty()) {
            val headers = listOf(
                if (!from.isNullOrBlank()) "From: $from" else null,
                "To: $to",
                "Subject: $subject",
                "MIME-Version: 1.0",
                "Content-Type: text/plain; charset=UTF-8"
            ).filterNotNull()
            headers.joinToString("\r\n") + "\r\n\r\n" + body
        } else {
            val boundary = "asap_${System.currentTimeMillis()}"
            val lines = mutableListOf<String>()
            if (!from.isNullOrBlank()) lines.add("From: $from")
            lines.add("To: $to")
            lines.add("Subject: $subject")
            lines.add("MIME-Version: 1.0")
            lines.add("Content-Type: multipart/mixed; boundary=$boundary")
            lines.add("")
            lines.add("--$boundary")
            lines.add("Content-Type: text/plain; charset=UTF-8")
            lines.add("")
            lines.add(body)
            attachments.forEach { att ->
                lines.add("--$boundary")
                lines.add("Content-Type: ${att.second.mime}")
                lines.add("Content-Disposition: attachment; filename=\"${att.first}\"")
                lines.add("Content-Transfer-Encoding: base64")
                lines.add("")
                lines.add(Base64.encodeToString(att.second.bytes, Base64.NO_WRAP))
            }
            lines.add("--$boundary--")
            lines.joinToString("\r\n")
        }
    }

    private data class ByteArrayWithMime(val bytes: ByteArray, val mime: String)

    private fun readAttachments(context: Context, uris: List<Uri>): List<Pair<String, ByteArrayWithMime>> {
        val list = mutableListOf<Pair<String, ByteArrayWithMime>>()
        uris.forEachIndexed { index, uri ->
            try {
                val cr = context.contentResolver
                val name = queryDisplayName(cr, uri) ?: "attachment_${index}"
                val input = cr.openInputStream(uri)
                val bytes = input?.readBytes() ?: ByteArray(0)
                input?.close()
                val mime = cr.getType(uri) ?: "application/octet-stream"
                list.add(name to ByteArrayWithMime(bytes, mime))
            } catch (e: Exception) {
                Log.w("EmailService", "Failed to attach file: ${e.message}")
            }
        }
        return list
    }

    private fun base64UrlEncode(input: ByteArray): String {
        val s = Base64.encodeToString(input, Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING)
        return s
    }

    private fun base64UrlDecode(input: String?): String {
        if (input.isNullOrBlank()) return ""
        return try {
            val bytes = Base64.decode(input, Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING)
            String(bytes, Charsets.UTF_8)
        } catch (_: Exception) { "" }
    }

    private fun getGmailAccessToken(context: Context): String? {
        return try {
            val account = GoogleSignIn.getLastSignedInAccount(context)
            if (account == null) return null
            val scope = "oauth2:https://www.googleapis.com/auth/gmail.send"
            val acc = account.account ?: Account(account.email ?: "", "com.google")
            GoogleAuthUtil.getToken(context, acc, scope)
        } catch (e: Exception) {
            Log.e("EmailService", "Token error: ${e.message}")
            null
        }
    }

    private fun getGmailReadToken(context: Context): String? {
        return try {
            val account = GoogleSignIn.getLastSignedInAccount(context)
            if (account == null) return null
            val scope = "oauth2:https://www.googleapis.com/auth/gmail.readonly"
            val acc = account.account ?: Account(account.email ?: "", "com.google")
            GoogleAuthUtil.getToken(context, acc, scope)
        } catch (e: Exception) {
            Log.e("EmailService", "Token error: ${e.message}")
            null
        }
    }

    private fun getGmailModifyToken(context: Context): String? {
        return try {
            val account = GoogleSignIn.getLastSignedInAccount(context)
            if (account == null) return null
            val scope = "oauth2:https://www.googleapis.com/auth/gmail.modify"
            val acc = account.account ?: Account(account.email ?: "", "com.google")
            GoogleAuthUtil.getToken(context, acc, scope)
        } catch (e: Exception) {
            Log.e("EmailService", "Token error: ${e.message}")
            null
        }
    }

    private fun sendViaGmail(
        context: Context,
        to: String,
        subject: String,
        body: String,
        attachments: List<Uri>,
        brandName: String?,
        logoUrl: String?,
        logoRes: Int
    ): Result<Unit> {
        return try {
            val token = getGmailAccessToken(context) ?: return Result.failure(Exception("Gmail not authorized"))
            val account = GoogleSignIn.getLastSignedInAccount(context)
            val from = account?.email
            val atts = readAttachments(context, attachments)
            val mime = buildMimeMessage(from, to, subject, body, atts)
            val raw = base64UrlEncode(mime.toByteArray(Charsets.UTF_8))
            val json = JSONObject()
            json.put("raw", raw)
            val requestBody = json.toString().toRequestBody("application/json".toMediaTypeOrNull())
            val request = Request.Builder()
                .url("https://gmail.googleapis.com/gmail/v1/users/me/messages/send")
                .addHeader("Authorization", "Bearer $token")
                .post(requestBody)
                .build()
            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                val bodyStr = response.body?.string()
                try {
                    val respJson = JSONObject(bodyStr ?: "{}")
                    val messageId = respJson.optString("id")
                    val threadId = respJson.optString("threadId")
                    if (messageId.isNotBlank() && threadId.isNotBlank()) {
                        saveSentEmail(
                            context,
                            SentEmail(
                                id = messageId,
                                threadId = threadId,
                                to = to,
                                subject = subject,
                                timestamp = System.currentTimeMillis(),
                                brandName = brandName ?: deriveBrandFromSubject(subject),
                                logoUrl = logoUrl ?: "",
                                logoRes = logoRes
                            )
                        )
                    }
                } catch (_: Exception) {}
                Result.success(Unit)
            } else {
                val msg = response.message
                Result.failure(Exception("Gmail send failed: ${response.code} - $msg"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    data class SentEmail(
        val id: String,
        val threadId: String,
        val to: String,
        val subject: String,
        val timestamp: Long,
        val brandName: String = "",
        val logoUrl: String = "",
        val logoRes: Int = 0
    )

    private fun prefs(context: Context) = context.getSharedPreferences("asap_emails", Context.MODE_PRIVATE)

    fun getSentEmails(context: Context): List<SentEmail> {
        return try {
            val s = prefs(context).getString("sent", "[]") ?: "[]"
            val arr = org.json.JSONArray(s)
            val list = mutableListOf<SentEmail>()
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                list.add(
                    SentEmail(
                        o.optString("id"),
                        o.optString("threadId"),
                        o.optString("to"),
                        o.optString("subject"),
                        o.optLong("timestamp"),
                        o.optString("brandName"),
                        o.optString("logoUrl"),
                        o.optInt("logoRes", 0)
                    )
                )
            }
            list.sortedByDescending { it.timestamp }
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun saveSentEmail(context: Context, email: SentEmail) {
        try {
            val existing = getSentEmails(context).toMutableList()
            existing.add(email)
            val arr = org.json.JSONArray()
            existing.forEach { se ->
                val o = JSONObject()
                o.put("id", se.id)
                o.put("threadId", se.threadId)
                o.put("to", se.to)
                o.put("subject", se.subject)
                o.put("timestamp", se.timestamp)
                o.put("brandName", se.brandName)
                o.put("logoUrl", se.logoUrl)
                o.put("logoRes", se.logoRes)
                arr.put(o)
            }
            prefs(context).edit().putString("sent", arr.toString()).apply()
        } catch (_: Exception) {}
    }

    private fun deriveBrandFromSubject(subject: String): String {
        return try {
            val prefix = "Inquiry about "
            if (subject.startsWith(prefix)) subject.removePrefix(prefix).trim() else subject
        } catch (_: Exception) { subject }
    }

    data class ThreadSummary(val threadId: String, val lastSnippet: String, val totalMessages: Int)

    fun fetchThreadSummary(context: Context, threadId: String): Result<ThreadSummary> {
        return try {
            val token = getGmailReadToken(context) ?: return Result.failure(Exception("Gmail read permission not granted"))
            val url = "https://gmail.googleapis.com/gmail/v1/users/me/threads/$threadId?format=metadata"
            val request = Request.Builder()
                .url(url)
                .addHeader("Authorization", "Bearer $token")
                .get()
                .build()
            val response = client.newCall(request).execute()
            if (!response.isSuccessful) {
                return Result.failure(Exception("Failed to fetch thread: ${response.code}"))
            }
            val bodyStr = response.body?.string() ?: "{}"
            val json = JSONObject(bodyStr)
            val messages = json.optJSONArray("messages") ?: org.json.JSONArray()
            val total = messages.length()
            var snippet = json.optString("snippet")
            if (snippet.isNullOrBlank() && total > 0) {
                val lastMsg = messages.getJSONObject(total - 1)
                snippet = lastMsg.optString("snippet")
            }
            Result.success(ThreadSummary(threadId, snippet ?: "", total))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    data class MessageDetail(
        val id: String,
        val from: String,
        val to: String,
        val date: String,
        val subject: String,
        val snippet: String,
        val bodyHtml: String,
        val bodyText: String
    )

    data class ThreadDetail(
        val threadId: String,
        val subject: String,
        val messages: List<MessageDetail>
    )

    fun fetchThread(context: Context, threadId: String): Result<ThreadDetail> {
        return try {
            val token = getGmailReadToken(context) ?: return Result.failure(Exception("Gmail read permission not granted"))
            val url = "https://gmail.googleapis.com/gmail/v1/users/me/threads/$threadId?format=full"
            val request = Request.Builder()
                .url(url)
                .addHeader("Authorization", "Bearer $token")
                .get()
                .build()
            val response = client.newCall(request).execute()
            if (!response.isSuccessful) return Result.failure(Exception("Failed to fetch thread: ${response.code}"))
            val bodyStr = response.body?.string() ?: "{}"
            val json = JSONObject(bodyStr)
            val messagesJson = json.optJSONArray("messages") ?: org.json.JSONArray()
            val list = mutableListOf<MessageDetail>()
            var threadSubject = ""
            for (i in 0 until messagesJson.length()) {
                val msg = messagesJson.getJSONObject(i)
                val id = msg.optString("id")
                val payload = msg.optJSONObject("payload")
                var from = ""
                var to = ""
                var date = ""
                var subject = ""
                val headers = payload?.optJSONArray("headers") ?: org.json.JSONArray()
                for (hIdx in 0 until headers.length()) {
                    val h = headers.getJSONObject(hIdx)
                    val name = h.optString("name")
                    val value = h.optString("value")
                    when (name) {
                        "From" -> from = value
                        "To" -> to = value
                        "Date" -> date = value
                        "Subject" -> subject = value
                    }
                }
                if (threadSubject.isBlank() && subject.isNotBlank()) threadSubject = subject
                val snippet = msg.optString("snippet")
                val bodyPair = extractBody(payload)
                list.add(
                    MessageDetail(
                        id = id,
                        from = from,
                        to = to,
                        date = date,
                        subject = subject,
                        snippet = snippet,
                        bodyHtml = bodyPair.first,
                        bodyText = bodyPair.second
                    )
                )
            }
            Result.success(ThreadDetail(threadId, if (threadSubject.isBlank()) json.optString("snippet") else threadSubject, list))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun extractBody(payload: JSONObject?): Pair<String, String> {
        if (payload == null) return "" to ""
        val directData = base64UrlDecode(payload.optJSONObject("body")?.optString("data"))
        val directMime = payload.optString("mimeType")
        if (directData.isNotBlank()) {
            return if (directMime == "text/html") directData to "" else "" to directData
        }
        fun findPart(p: JSONObject?, mime: String): String {
            if (p == null) return ""
            if (p.optString("mimeType") == mime) {
                val d = base64UrlDecode(p.optJSONObject("body")?.optString("data"))
                if (d.isNotBlank()) return d
            }
            val parts = p.optJSONArray("parts") ?: org.json.JSONArray()
            for (i in 0 until parts.length()) {
                val child = parts.optJSONObject(i)
                val d = findPart(child, mime)
                if (d.isNotBlank()) return d
            }
            return ""
        }
        val html = findPart(payload, "text/html")
        val text = if (html.isNotBlank()) "" else findPart(payload, "text/plain")
        return html to text
    }

    fun replyToThread(context: Context, threadId: String, to: String, subject: String, body: String): Result<Unit> {
        return try {
            val token = getGmailAccessToken(context) ?: return Result.failure(Exception("Gmail not authorized"))
            val account = GoogleSignIn.getLastSignedInAccount(context)
            val from = account?.email
            val mime = buildMimeMessage(from, to, if (subject.startsWith("Re:")) subject else "Re: $subject", body, emptyList())
            val raw = base64UrlEncode(mime.toByteArray(Charsets.UTF_8))
            val json = JSONObject()
            json.put("raw", raw)
            json.put("threadId", threadId)
            val requestBody = json.toString().toRequestBody("application/json".toMediaTypeOrNull())
            val request = Request.Builder()
                .url("https://gmail.googleapis.com/gmail/v1/users/me/messages/send")
                .addHeader("Authorization", "Bearer $token")
                .post(requestBody)
                .build()
            val response = client.newCall(request).execute()
            if (response.isSuccessful) Result.success(Unit) else Result.failure(Exception("Reply failed: ${response.code}"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun trashThread(context: Context, threadId: String): Result<Unit> {
        return try {
            val token = getGmailModifyToken(context) ?: return Result.failure(Exception("Gmail modify permission not granted"))
            val request = Request.Builder()
                .url("https://gmail.googleapis.com/gmail/v1/users/me/threads/$threadId/trash")
                .addHeader("Authorization", "Bearer $token")
                .post("".toRequestBody(null))
                .build()
            val response = client.newCall(request).execute()
            if (response.isSuccessful) Result.success(Unit) else Result.failure(Exception("Trash failed: ${response.code}"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun deleteLocalSent(context: Context, id: String): Result<Unit> {
        return try {
            val s = prefs(context).getString("sent", "[]") ?: "[]"
            val arr = org.json.JSONArray(s)
            val newArr = org.json.JSONArray()
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                if (o.optString("id") != id) newArr.put(o)
            }
            prefs(context).edit().putString("sent", newArr.toString()).apply()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun queryDisplayName(cr: android.content.ContentResolver, uri: Uri): String? {
        return try {
            val cursor = cr.query(uri, arrayOf(android.provider.OpenableColumns.DISPLAY_NAME), null, null, null)
            cursor?.use {
                if (it.moveToFirst()) {
                    val idx = it.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                    if (idx >= 0) return it.getString(idx)
                }
            }
            null
        } catch (e: Exception) {
            null
        }
    }
}
