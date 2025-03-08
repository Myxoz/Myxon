package com.myxoz.myxon.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.Intent.FLAG_ACTIVITY_NEW_TASK
import android.content.SharedPreferences
import android.widget.Toast
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow.Companion.Ellipsis
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.myxoz.myxon.Colors
import com.myxoz.myxon.R
import com.myxoz.myxon.SharedPrefsKeys
import com.myxoz.myxon.async
import com.myxoz.myxon.launchIntentSafely
import com.myxoz.myxon.notification.WhatsAppMessageType.ADDED_TO_GROUP
import com.myxoz.myxon.notification.WhatsAppMessageType.CONTACT
import com.myxoz.myxon.notification.WhatsAppMessageType.EVENT
import com.myxoz.myxon.notification.WhatsAppMessageType.FILE
import com.myxoz.myxon.notification.WhatsAppMessageType.GROUP_REACTION
import com.myxoz.myxon.notification.WhatsAppMessageType.GROUP_REPLY
import com.myxoz.myxon.notification.WhatsAppMessageType.GROUP_VOTE
import com.myxoz.myxon.notification.WhatsAppMessageType.LIVE_LOCATION
import com.myxoz.myxon.notification.WhatsAppMessageType.LOCATION
import com.myxoz.myxon.notification.WhatsAppMessageType.PDF
import com.myxoz.myxon.notification.WhatsAppMessageType.PHOTO
import com.myxoz.myxon.notification.WhatsAppMessageType.PHOTO_ONCE
import com.myxoz.myxon.notification.WhatsAppMessageType.POLL
import com.myxoz.myxon.notification.WhatsAppMessageType.REACTION
import com.myxoz.myxon.notification.WhatsAppMessageType.TEXT
import com.myxoz.myxon.notification.WhatsAppMessageType.VIDEO
import com.myxoz.myxon.notification.WhatsAppMessageType.VIDEO_NOTE
import com.myxoz.myxon.notification.WhatsAppMessageType.VIDEO_ONCE
import com.myxoz.myxon.notification.WhatsAppMessageType.VOICE
import com.myxoz.myxon.notification.WhatsAppMessageType.VOICE_ONCE
import com.myxoz.myxon.notification.WhatsAppMessageType.VOTE
import kotlinx.coroutines.delay


//var whatsAppMessage: WhatsAppPipeable, var telegramSender: String?, var telegramMessage: String?, var snapchatDrawables: MutableList<SnapPerson>?
class NotificationHub(val prefs: SharedPreferences) {
    var context: Context?=null
    var listener: ((context: Context, intent: Intent) -> Unit)? = null
    val notificationReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            println("Fire!")
            listener?.invoke(context, intent)
        }
    }
    init {
        println("NotificationHub instance")
        if(context!=null){
            Toast.makeText(context, "new instance, insanly bad!", Toast.LENGTH_LONG).show()
        }
    }
    @OptIn(ExperimentalFoundationApi::class)
    @Composable
    fun NotificationHubComposable(){
        // TODO reformat
        var whatsAppMessages: WhatsAppMessages? by remember {
            mutableStateOf(prefs.getString(SharedPrefsKeys.WHATSAPP,/*"""{"content":[{"group":"Gruppe","type":"TEXT","sender":"Person","content":"Nachricht tralla"}],"chatAmount":1,"messageAmount":1}"""*/ null)?.let { WhatsAppMessages.fromJson(it) })
        }
        var telegramMessage: TelegramMessage? by remember {
            mutableStateOf(prefs.getString(SharedPrefsKeys.TELEGRAM,null)?.let { TelegramMessage.fromJson(it) })
        }
        val snapChatDrawable = remember("Snapchat") {
            prefs.getStringSet(SharedPrefsKeys.SNAPCHAT,null)?.map { SnapPerson.getPersonByBase64(it) }?.toMutableStateList()?: mutableStateListOf()
        }
        this.listener = { _, i ->
            val whatsAppParcelable = i.extras?.getParcelable("package", ParcelableWhatsAppMessages::class.java)
            val snapchatParcelable = i.extras?.getString("package")
            val telegramParcelable = i.extras?.getParcelable("package", ParcelableTelegramMessage::class.java)
            Toast.makeText(context, "Received something", Toast.LENGTH_LONG).show()
            if(whatsAppParcelable!=null) {
                logToServer("Received parcel: \n${whatsAppParcelable.asWhatsAppMessages().json()}").start()
                whatsAppMessages = whatsAppParcelable.asWhatsAppMessages()
                val json=whatsAppMessages?.json()
                if(json != null){
                    prefs.edit().putString(SharedPrefsKeys.WHATSAPP, json).apply()
                }
            }
            if(snapchatParcelable!=null && null==snapChatDrawable.find{it.hashCode == snapchatParcelable.hashCode()}){
                snapChatDrawable.add(SnapPerson.getPersonByBase64(snapchatParcelable))
                prefs.edit().putStringSet(SharedPrefsKeys.SNAPCHAT, snapChatDrawable.map { it.asBase64() }.toSet()).apply()
            }
            if(telegramParcelable!=null){
                telegramMessage=telegramParcelable.asMessage()
                val json=telegramMessage?.json()
                if(json!=null){
                    prefs
                        .edit()
                        .putString(SharedPrefsKeys.TELEGRAM, json)
                        .apply()
                }
            }
        } // i.extras?.getString("package")
        Surface(
            Modifier
                .fillMaxWidth(),
            color = Color.Transparent
        ){
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
//                Row {
//                    Text(debugText,color=Colors.FONT)
//                }
                Row(
                    Modifier
                        .height(60.dp)
                        .background(if(whatsAppMessages?.messageAmount greater 0) Colors.WHATSAPP.copy(alpha = .25f) else Color.Transparent, RoundedCornerShape(20.dp))
                        .combinedClickable(
                            indication = ripple(),
                            interactionSource = remember { MutableInteractionSource() },
                            onLongClick = {
                                async {
                                    delay(1000)
                                    whatsAppMessages=null
                                    prefs.edit().putString(SharedPrefsKeys.WHATSAPP, null).apply()
                                }
                            }
                        ) {
                            async {
                                delay(1000)
                                whatsAppMessages=null
                                prefs.edit().putString(SharedPrefsKeys.WHATSAPP, null).apply()
                            }
                            if (context != null) {
                                startApp(NotificationActivity.WhatsApp, context!!)
                            }
                        }
                        .padding(horizontal = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(20.dp),
                    verticalAlignment = Alignment.CenterVertically
                )
                {
                    Icon(ImageVector.vectorResource(R.drawable.whatsapp), "WhatsApp", tint = if(whatsAppMessages?.messageAmount greater 0) Colors.WHATSAPP else Colors.SECONDARY_FONT, modifier = Modifier
                        .size(30.dp)
                    )
                    Column(
                        Modifier
                            .fillMaxHeight()
                            .weight(5f), verticalArrangement = Arrangement.Center)
                    {
                        logToServer("\n"+whatsAppMessages?.messages?.reversed()?.sortedByDescending { getScoreByWhatsAppMessage(it) }?.joinToString { it.content?:"" }+"\n")
                        val message = whatsAppMessages?.messages?.reversed()?.sortedByDescending { getScoreByWhatsAppMessage(it) }?.getOrNull(0)
                        if(message!=null){
                            Text(message.group?:message.sender, color = Colors.FONT, fontSize = 17.sp, maxLines = 1)
                            Spacer(modifier = Modifier.height(2.dp))
                        }
                        if(message!=null) {
                            Row {
                                with(message) {
                                    val text = when(message.messageType){
                                        VOICE -> "Sent a ${message.durationInSeconds!!.let { "${(it / 3600).run { if (this == 0) "" else "${this}h " }}${(it % 3600 / 60).run { if (this == 0) "" else "${this}m " }}${it % 60}s" }} voice message"
                                        POLL -> "Shared a poll: \"$content\""
                                        PHOTO -> "Sent a photo${message.content?.let { ": \"$it\"" } ?: ""}"
                                        VIDEO_NOTE -> "Sent you a ${message.durationInSeconds!!.let { "${(it / 3600).run { if (this == 0) "" else "${this}h " }}${(it % 3600 / 60).run { if (this == 0) "" else "${this}m " }}${it % 60}s" }} video note"
                                        VIDEO -> "Shared a video (${message.durationInSeconds!!.let { "${(it / 3600).run { if (this == 0) "" else "${this}h " }}${(it % 3600 / 60).run { if (this == 0) "" else "${this}m " }}${it % 60}s" }})${message.content?.let { " with caption \"$it\"" } ?: ""}"
                                        PDF -> "Shared a ${message.content?.substringBeforeLast(" page")?.substringAfterLast("(")?.toIntOrNull()?:-1} page PDF named ${message.content?.substringBeforeLast("(")}"
                                        FILE -> "Uploaded a file${message.content?.let { ": \"$it\"" } ?: ""}"
                                        VOICE_ONCE -> "Shared a one-time voice message"
                                        PHOTO_ONCE -> "Shared a view-once photo"
                                        VIDEO_ONCE -> "Shared a view-once video"
                                        GROUP_REPLY -> "Replied with: \"$content\""
                                        ADDED_TO_GROUP -> "Added you"
                                        GROUP_REACTION -> "Reacted to \"$cited\""
                                        REACTION -> "Reacted with $content to \"$cited\""
                                        GROUP_VOTE -> "Voted in \"$cited\""
                                        VOTE -> "Participated in \"$cited\""
                                        CONTACT -> "Shared $content"
                                        LOCATION -> "Shared a location${content?.let { ": \"$it\"" } ?: ""}"
                                        LIVE_LOCATION -> "Shared their live location"
                                        EVENT -> "Invited you to $content on $eventTimestamp"
                                        TEXT -> if(message.group!=null) ": $content" else "$content"
                                    }.let { if(message.group!=null) "$sender ${it[0].lowercase()}${it.drop(1)}" else it }
                                    Text(text, color = Colors.SECONDARY_FONT, maxLines = 2, overflow = Ellipsis)
                                }
                            }
                        } else {
                            Text(
                                "No new messages",
                                color = Colors.SECONDARY_FONT, maxLines=2, overflow = Ellipsis
                            )
                        }
                    }

                    whatsAppMessages?.let {
                        Box(
                            Modifier
                                .size(25.dp)
                                .background(Colors.WHATSAPP, CircleShape)){
                            Text("${it.messageAmount}",color = Colors.BLACK, modifier = Modifier.align(Alignment.Center), textAlign = TextAlign.Center, fontWeight = FontWeight.Bold)
                        }
                    }
                }
                Row(
                    Modifier
                        .height(60.dp)
                        .background(if(telegramMessage?.messageAmount greater 0) Colors.TELEGRAM.copy(.20f) else Color.Transparent, RoundedCornerShape(20.dp))
                        .combinedClickable(
                            indication = ripple(),
                            interactionSource = remember { MutableInteractionSource() },
                            onLongClick = {
                                async {
                                    delay(1000)
                                    telegramMessage = null
                                    prefs
                                        .edit()
                                        .putString(SharedPrefsKeys.TELEGRAM, null)
                                        .apply()
                                }
                            }
                        ) {
                            async {
                                delay(1000)
                                telegramMessage = null
                                prefs
                                    .edit()
                                    .putString(SharedPrefsKeys.TELEGRAM, null)
                                    .apply()
                            }
                            if (context != null) {
                                startApp(NotificationActivity.Telegram, context!!)
                            }
                        }
                        .padding(horizontal = 10.dp)
                    , horizontalArrangement = Arrangement.spacedBy(20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ){
                    Icon(ImageVector.vectorResource(R.drawable.telegram), "Telegram", tint = if(telegramMessage?.messageAmount greater 0) Colors.TELEGRAM else Colors.SECONDARY_FONT, modifier = Modifier
                        .size(30.dp)
                    )
                    val nonNull=telegramMessage
                    Column(
                        Modifier
                            .fillMaxHeight()
                            .weight(5f), verticalArrangement = Arrangement.Center) {
                        if(nonNull!=null){
                            Text(nonNull.sender.trim(), color = Colors.FONT, fontSize = 17.sp)
                            Spacer(modifier = Modifier.height(2.dp))
                        }
                        Text(telegramMessage?.content ?: "No new messages", color = Colors.SECONDARY_FONT)
                    }
                    telegramMessage?.let {
                        Box(
                            Modifier
                                .size(25.dp)
                                .background(Colors.TELEGRAM, CircleShape)){
                            Text("${it.messageAmount}",color = Colors.BLACK, modifier = Modifier.align(Alignment.Center), textAlign = TextAlign.Center, fontWeight = FontWeight.Bold)
                        }
                    }
                }
                Row(
                    Modifier
                        .height(60.dp)
                        .background(Colors.SNAPCHAT.copy(snapChatDrawable.size*.05f), RoundedCornerShape(20.dp))
                        .clickable(
                            indication = ripple(),
                            interactionSource = remember { MutableInteractionSource() }) {
                            async {
                                delay(1000)
                                snapChatDrawable.clear()
                                prefs
                                    .edit()
                                    .putStringSet(SharedPrefsKeys.SNAPCHAT, setOf())
                                    .apply()
                            }
                            if (context != null) {
                                startApp(NotificationActivity.Snapchat, context!!)
                            }
                        }
                        .padding(horizontal = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ){
                    Icon(ImageVector.vectorResource(R.drawable.snapchat), "Snapchat", tint = Colors.SNAPCHAT.copy(snapChatDrawable.size*.15f).compositeOver(Colors.SECONDARY_FONT), modifier = Modifier
                        .size(45.dp)
                    )
//                    Box(modifier = Modifier.weight(5f).fillMaxWidth().background(Colors.SECONDARY, CircleShape).padding(10.dp)){
                        if(snapChatDrawable.isNotEmpty()) {
                            Row(
                                Modifier
                                    .weight(5f)
                                    .fillMaxHeight()
                                    .horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                                snapChatDrawable.forEach {
                                    Image(bitmap = it.imageBitmap, contentDescription = "Snap Person", modifier = Modifier
                                        .background(Colors.SNAPCHAT.copy(snapChatDrawable.size*.15f).compositeOver(Colors.SECONDARY_FONT).copy(.25f), CircleShape)
                                        .padding(5.dp)
                                        .size(30.dp))
                                }
        //                        Text(debugText)
                            }
                        } else {
                            Column(
                                Modifier
                                    .fillMaxHeight()
                                    .weight(5f), verticalArrangement = Arrangement.Center) {
                                Text("No new snaps", color = Colors.SECONDARY_FONT)
                            }
                        }

//                    }
                }
//                Spacer(modifier = Modifier
//                    .height(1.dp)
//                    .fillMaxWidth()
//                    .background(Colors.SECONDARY_FONT, CircleShape))
//                Box(){
//                    Text("Penis")
//                }
            }
        }
//        Log.d(TAG, "Stack: ${Log.getStackTraceString(Throwable())}")
    }


    companion object {
        const val INTENTNAME = "com.myxoz.myxon.NOTIFICATION_RECEIVED"
    }
}
enum class NotificationActivity{
    WhatsApp,Telegram,Snapchat
}
fun startApp(launch: NotificationActivity, context: Context){
    when(launch){
        NotificationActivity.WhatsApp -> launchIntentSafely(context, "android.intent.action.MAIN", "com.whatsapp", FLAG_ACTIVITY_NEW_TASK, "com.whatsapp","com.whatsapp.Main")
        NotificationActivity.Telegram -> launchIntentSafely(context, "android.intent.action.MAIN", "org.telegram.messenger", FLAG_ACTIVITY_NEW_TASK, "org.telegram.messenger","org.telegram.messenger.DefaultIcon")
        NotificationActivity.Snapchat -> launchIntentSafely(context, "android.intent.action.VIEW", "com.snapchat.android", FLAG_ACTIVITY_NEW_TASK, "com.snapchat.android","com.snap.mushroom.MainActivity", "snapchat://chat_shortcut")
    }
}
fun getScoreByWhatsAppMessage(message: WhatsAppMessage): Int {
    return when(message.messageType){
        VOICE -> 15 //
        PDF -> 11
        VIDEO_NOTE -> 37
        POLL -> 35 //
        PHOTO -> 20 //
        VIDEO -> 20 //
        FILE -> 10 //
        VOICE_ONCE -> 60
        PHOTO_ONCE -> 60
        VIDEO_ONCE -> 60
        GROUP_REPLY -> 19
        ADDED_TO_GROUP -> 40
        GROUP_REACTION -> 4
        REACTION -> 3 //
        GROUP_VOTE -> 6
        VOTE -> 5 //
        CONTACT -> 10 //
        LOCATION -> 30
        LIVE_LOCATION -> 50
        EVENT -> 20
        TEXT -> 10 // Group
    }
}
infix fun Int?.greater(compare: Int): Boolean = if(this==null) false else this>compare