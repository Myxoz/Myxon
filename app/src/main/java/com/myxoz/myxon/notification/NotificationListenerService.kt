package com.myxoz.myxon.notification

import android.app.Notification
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.os.Parcelable
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.drawable.toBitmap
import com.myxoz.myxon.Promise
import com.myxoz.myxon.toast
import kotlinx.coroutines.delay
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

class NotificationListenerService : NotificationListenerService() {
    private val notificationIntentName: String = "com.myxoz.myxon.NOTIFICATION_RECEIVED"
    private var lastSBNTimestamp=0L
    private val votableSBNs = mutableListOf<StatusBarNotification>()
    override fun onNotificationPosted(sbn: StatusBarNotification)
    {
        val intent = Intent(notificationIntentName)
        if(sbn.packageName=="com.burockgames.timeclocker" || sbn.packageName=="com.android.systemui"  || sbn.packageName=="com.samsung.android.app.smartcapture") return
        println("NOTIFICATION: ${sbn.packageName}")
        when (sbn.packageName) {
            "org.telegram.messenger" -> {
                if(sbn.packageName!="org.telegram.messenger") return
                val extras=sbn.notification.extras
                val messages= Notification.MessagingStyle.Message.getMessagesFromBundleArray(extras.getParcelableArray(
                    Notification.EXTRA_MESSAGES, Bundle.EMPTY.javaClass)).map { TelegramMessage(it.text.toString(),  it.senderPerson?.name.toString(), 0) }
                val chosenMessage=messages
                    .reversed()
                    .sortedWith { a, b ->
                        getScoreByTelegramMessage(b) -getScoreByTelegramMessage(a)
                    }[0]
                intent.putExtra("package", chosenMessage.getParcelable(messages.size))
                sendBroadcast(intent)
            }
            "com.whatsapp" -> {
                votableSBNs.add(sbn)
                if(System.currentTimeMillis()-lastSBNTimestamp>100){
                    lastSBNTimestamp=System.currentTimeMillis()
                    Promise{
                        delay(300)
                        votableSBNs
                            .sortedByDescending { it.notification.extras.getCharSequence(Notification.EXTRA_CONVERSATION_TITLE)!=null }
                            .sortedByDescending { it.notification.extras.getCharSequence(Notification.EXTRA_SUMMARY_TEXT)!=null }
                            .get(0)
                            .apply {
                                val extras = notification.extras
                                try {
                                    val parsed= this.parseWhatsAppStatusBarNotification() ?: return@apply Unit.apply { logToServer("Failed at newParser response null\n\n"+"""
                                        ---------------------------------------------
                                        RAW (still):  
                                        TIMESTAMP: ${System.currentTimeMillis()}
                                        EXTRA_TEXT: ${extras.getCharSequence(Notification.EXTRA_TEXT)}
                                        EXTRA_TEXT_LINES: ${
                                        extras.getCharSequenceArray(Notification.EXTRA_TEXT_LINES)
                                            ?.joinToString("") { "\n -> $it" }
                                    }
                                        EXTRA_SUMMARY_TEXT: ${extras.getCharSequence(Notification.EXTRA_SUMMARY_TEXT)}
                                        EXTRA_TITLE: ${extras.getCharSequence(Notification.EXTRA_TITLE)}
                                        EXTRA_CONVERSATION_TITLE: ${
                                        extras.getCharSequence(
                                            Notification.EXTRA_CONVERSATION_TITLE
                                        )
                                    }
                                    """.trimIndent()).start() }
                                    val whatsAppMessagesParcelable = ParcelableWhatsAppMessages(
                                        parsed.map { ParcelableWhatsAppMessage.byWhatsAppMessage(it) },
                                        extras.getCharSequence(Notification.EXTRA_SUMMARY_TEXT)?.toString()?.dropLast(" chats".length)?.substringAfterLast(" ")?.toIntOrNull()?:1,
                                        extras.getCharSequence(Notification.EXTRA_SUMMARY_TEXT)?.toString()?.substringBefore(" messages")?.substringBefore(" new")?.toIntOrNull()?:1
                                    )
                                    intent.putExtra(
                                        "package",
                                        whatsAppMessagesParcelable
                                    )
                                    sendBroadcast(intent)
                                    logToServer("""
---------------------------------------------
RAW: 
TIMESTAMP: ${System.currentTimeMillis()}
EXTRA_TEXT: ${extras.getCharSequence(Notification.EXTRA_TEXT)}
EXTRA_TEXT_LINES: ${extras.getCharSequenceArray(Notification.EXTRA_TEXT_LINES)?.joinToString("") { "\n -> $it" }}
EXTRA_SUMMARY_TEXT: ${extras.getCharSequence(Notification.EXTRA_SUMMARY_TEXT)}
EXTRA_TITLE: ${extras.getCharSequence(Notification.EXTRA_TITLE)}
EXTRA_CONVERSATION_TITLE: ${extras.getCharSequence(Notification.EXTRA_CONVERSATION_TITLE)}
AMOUNT: ${whatsAppMessagesParcelable.chatAmount} - Message amount: ${whatsAppMessagesParcelable.messageAmount}


PARSED:
${parsed.joinToString("\n") {
    "Group: ${it.group} - Sender: ${it.sender} - Content: ${it.content} - Cited: ${it.cited} - Type: ${it.messageType.name} - Duration: ${it.durationInSeconds} - EventTimestamp: ${it.eventTimestamp}"
}}
""").get { votableSBNs.clear() }
                                } catch (e: Exception) {
                                    logToServer(e.stackTraceToString()).start()
                                }
                            }
                    }.start()
                }
            }

            "com.snapchat.android" -> {
                val icon=sbn.notification.getLargeIcon()
                val drawable=icon.loadDrawable(applicationContext)?:return toast(applicationContext, "No drawable")
                val width=if(drawable.intrinsicWidth>0) drawable.intrinsicWidth else 128
                val height=if(drawable.intrinsicHeight>0) drawable.intrinsicHeight else 128
                val bitmap=drawable.toBitmap(width, height)
//                val title=sbn.notification.extras.getString(Notification.EXTRA_TITLE)?:"NO_TITLE"
                intent.putExtra("package", SnapPerson.bitmapToBase64(bitmap))
                sendBroadcast(intent)
            }
        }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification) {
        // Handle notification removal if necessary
    }
}
@Parcelize
data class ParcelableWhatsAppMessage(
    val content: String?,
    val group: String?,
    val messageType: WhatsAppMessageType,
    val cited: String?,
    val sender: String,
    val durationInSeconds: Int?,
    val eventTimestamp: String?
): Parcelable{
    fun asWhatsAppMessage(): WhatsAppMessage {
        return WhatsAppMessage(group, messageType, cited, sender, durationInSeconds, eventTimestamp, content)
    }
    companion object {
        fun byWhatsAppMessage(message: WhatsAppMessage): ParcelableWhatsAppMessage{
            return ParcelableWhatsAppMessage(
                message.content,
                message.group,
                message.messageType,
                message.cited,
                message.sender,
                message.durationInSeconds,
                message.eventTimestamp
            )
        }
    }
}

data class WhatsAppMessages(
    val messages: List<WhatsAppMessage>,
    val chatAmount: Int,
    val messageAmount: Int
) {
    fun json():String{
        /*"""{"content":[{"group":"Gruppe","type":"TEXT","sender":"Person","content":"Nachricht tralla"}],"chatAmount":1,"messageAmount":1}"""*/
        return JSONObject().apply {
            put("content",
                JSONArray().apply {
                    messages.forEach {
                        put(JSONObject().apply {
                            put("group",it.group)
                            put("type",it.messageType.name)
                            put("cited",it.cited)
                            put("sender",it.sender)
                            put("durationInSeconds",it.durationInSeconds)
                            put("eventTimestamp",it.eventTimestamp)
                            put("content",it.content)
                        })
                    }
                }
            )
            put("chatAmount",chatAmount)
            put("messageAmount",messageAmount)
        }.toString()
    }
    companion object {
        fun fromJson(json: String):WhatsAppMessages?{
            val parsed=Json.parseToJsonElement(json).jsonObject
            return WhatsAppMessages(
                parsed["content"]?.jsonArray?.map {
                    val asObject=it.jsonObject
                    WhatsAppMessage(
                        group = asObject["group"]?.jsonPrimitive?.contentOrNull,
                        messageType = WhatsAppMessageType.valueOf(asObject["type"]?.jsonPrimitive?.contentOrNull?:return null),
                        cited = asObject["group"]?.jsonPrimitive?.contentOrNull,
                        sender = asObject["sender"]?.jsonPrimitive?.contentOrNull?:return null,
                        durationInSeconds = asObject["durationInSeconds"]?.jsonPrimitive?.intOrNull,
                        eventTimestamp = asObject["eventTimestamp"]?.jsonPrimitive?.contentOrNull,
                        content = asObject["content"]?.jsonPrimitive?.contentOrNull
                    )
                }?:return null,
                parsed["chatAmount"]?.jsonPrimitive?.intOrNull?:return null,
                parsed["messageAmount"]?.jsonPrimitive?.intOrNull?:return null
            )
        }
    }
}
@Parcelize
data class ParcelableWhatsAppMessages(
    val messages: List<ParcelableWhatsAppMessage>,
    val chatAmount: Int,
    val messageAmount: Int
) :Parcelable {
    fun asWhatsAppMessages(): WhatsAppMessages{
        return WhatsAppMessages(messages = messages.map { it.asWhatsAppMessage() }, chatAmount, messageAmount)
    }
}
@Parcelize
data class ParcelableTelegramMessage(
    val content: String,
    val sender: String,
    val messageAmount: Int
): Parcelable {
    fun asMessage(): TelegramMessage{
        return TelegramMessage(content, sender, messageAmount)
    }
}
data class TelegramMessage(
    val content: String,
    val sender: String,
    val messageAmount: Int
)  {
    fun getParcelable(messageAmount: Int): ParcelableTelegramMessage{
        return ParcelableTelegramMessage(content, sender, messageAmount)
    }
    fun json():String{
        return JSONObject().apply {
            put("content",content)
            put("sender",sender)
            put("messageAmount",messageAmount)
        }.toString()
    }
    companion object {
        fun fromJson(json: String):TelegramMessage?{
            val parsed=Json.parseToJsonElement(json).jsonObject
            return TelegramMessage(
                parsed["content"]?.jsonPrimitive?.contentOrNull?:return null,
                parsed["sender"]?.jsonPrimitive?.contentOrNull?:return null,
                parsed["messageAmount"]?.jsonPrimitive?.intOrNull?:return null
            )
        }
    }
}
class SnapPerson(val imageBitmap: ImageBitmap, val hashCode: Int){
    fun asBase64():String{
        return bitmapToBase64(imageBitmap.asAndroidBitmap())
    }
    companion object{
        fun getPersonByBase64(base64Str: String): SnapPerson{
            return SnapPerson(base64toBitmap(base64Str).asImageBitmap(), base64Str.hashCode())
        }
        fun bitmapToBase64(bitmap: Bitmap): String{
            val byteArrayOutputStream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, byteArrayOutputStream)
            val byteArray = byteArrayOutputStream.toByteArray()
            return android.util.Base64.encodeToString(byteArray, android.util.Base64.DEFAULT)
        }
        private fun base64toBitmap(base64Str: String): Bitmap {
            val decodedBytes = android.util.Base64.decode(
                base64Str.substring(base64Str.indexOf(",") + 1),
                android.util.Base64.DEFAULT
            )
            return BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
        }
    }
}
fun getScoreByTelegramMessage(message: TelegramMessage): Int{
    return if(message.sender.trim()=="Mom") 1000+message.content.length else 0
}
fun logToServer(data: String?): Promise<String?> {
    return Promise {
        val url = URL("https://myxoz.de/etc/log/_log.php?name=myxon")
        val connection = url.openConnection() as HttpURLConnection
        connection.requestMethod = "POST"
        connection.setRequestProperty("Accept", "*/*")
        connection.setRequestProperty("Accept-Encoding", "identity")
        connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
        connection.doOutput = true // Enable output for writing post fields

        // Add POST data
        val postData = "data=${URLEncoder.encode(data ?: "", "UTF-8")}"
        connection.outputStream.use { output ->
            output.write(postData.toByteArray(Charsets.UTF_8))
        }

        val responseCode = connection.responseCode
        val response = if (responseCode == HttpURLConnection.HTTP_OK) {
            println("Fetch success")
            connection.inputStream.bufferedReader().use { it.readText() }
        } else {
            println("Error: $responseCode")
            null
        }
        connection.disconnect()
        response
    }
}