package com.myxoz.myxon.notification

/*
MIT License

Copyright (c) 2025 Myxoz

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.


Disclaimer

This software is an independent project and is not affiliated, associated,
authorized, endorsed by, or in any way officially connected with WhatsApp LLC,
Meta Platforms, Inc., or any of their subsidiaries or affiliates.

The name "WhatsApp" as well as related names, marks, emblems, and images are
registered trademarks of their respective owners. Use of these names and trademarks
does not imply any endorsement.

This software is provided "as is," without warranty of any kind. Use at your own risk.
*/

import android.app.Notification
import android.service.notification.StatusBarNotification
import kotlin.math.pow

enum class WhatsAppMessageType {
    /**
     * A voice message from someone
     *
     * Duration in seconds: [WhatsAppMessage.durationInSeconds] (non-null)
     *
     * Sender: [WhatsAppMessage.sender] (non-null)
     *
     * Group: [WhatsAppMessage.group] (nullable, if send in a private chat)
     */
    VOICE,
    /**
     * A WhatsApp poll from someone
     *
     * Caption: [WhatsAppMessage.content] (non-null)
     *
     * Sender: [WhatsAppMessage.sender] (non-null)
     *
     * Group: [WhatsAppMessage.group] (nullable, if send in a private chat)
     */
    POLL,
    /**
     * A photo from someone
     *
     * Caption: [WhatsAppMessage.content] (null if no caption is present)
     *
     * Sender: [WhatsAppMessage.sender] (non-null)
     *
     * Group: [WhatsAppMessage.group] (nullable, if send in a private chat)
     */
    PHOTO,
    /**
     * A video from a someone
     *
     * Caption: [WhatsAppMessage.content] (null if no caption is present)
     *
     * Duration in seconds: [WhatsAppMessage.durationInSeconds] (non-null)
     *
     * Sender: [WhatsAppMessage.sender] (non-null)
     *
     * Group: [WhatsAppMessage.group] (nullable, if send in a private chat)
     */
    VIDEO,
    /**
     * A video note from someone
     *
     * Duration in seconds: [WhatsAppMessage.durationInSeconds] (non-null)
     *
     * Sender: [WhatsAppMessage.sender] (non-null)
     *
     * Group: [WhatsAppMessage.group] (nullable, if send in a private chat)
     */
    VIDEO_NOTE,
    /**
     * A pdf file from a someone
     *
     * Filename: [WhatsAppMessage.content] (if no caption is present the pdf isn't detectable, (not distinguishable), non-null)  Format: "filename.pdf ([0-9] pages?)"
     *
     * Sender: [WhatsAppMessage.sender] (non-null)
     +
     * Group: [WhatsAppMessage.group] (nullable, if send in a private chat)
     */
    PDF,
    /**
     * A file from a someone
     *
     * Caption / Filename: [WhatsAppMessage.content] (if no caption is present, this will be the filename (not distinguishable), non-null)
     *
     * Sender: [WhatsAppMessage.sender] (non-null)
     *
     * Group: [WhatsAppMessage.group] (nullable, if send in a private chat)
     */
    FILE,
    /**
     * A listen-once voice message
     *
     * Sender: [WhatsAppMessage.sender] (non-null)
     *
     * Group: [WhatsAppMessage.group] (nullable, if send in a private chat)
     */
    VOICE_ONCE,
    /**
     * A view-once photo
     *
     * Sender: [WhatsAppMessage.sender] (non-null)
     *
     * Group: [WhatsAppMessage.group] (nullable, if send in a private chat)
     */
    PHOTO_ONCE,
    /**
     * A view-once video
     *
     * Sender: [WhatsAppMessage.sender] (non-null)
     *
     * Group: [WhatsAppMessage.group] (nullable, if send in a private chat)
     */
    VIDEO_ONCE,
    /**
     * A reply to your message, only detectable if this is the only notification for WhatsApp
     *
     * Message replied content: [WhatsAppMessage.content] (non-null)
     *
     * Replier: [WhatsAppMessage.sender] (non-null)
     *
     * Group: [WhatsAppMessage.group] (non-null, only send if in a group)
     */
    GROUP_REPLY,
    /**
     * A notification that someone added you to a group
     *
     * The person who added you: [WhatsAppMessage.sender] (non-null)
     *
     * Group: [WhatsAppMessage.group] (non-null, only send if added to a group, the group you were added to)
     */
    ADDED_TO_GROUP,
    /**
     * Multiple people reacted to you message
     *
     * The message reacted to: [WhatsAppMessage.cited] (non-null)
     *
     * The amount of people who reacted: [WhatsAppMessage.sender] (Format: "{amount} people") (non-null)
     *
     * Group: [WhatsAppMessage.group] (non-null, only send if added to a group)
     */
    GROUP_REACTION,
    /**
     * One persons reaction to one message of your messages
     *
     * Message voted to: [WhatsAppMessage.cited] (non-null)
     *
     * Reaction Emoji: [WhatsAppMessage.content] (non-null)
     *
     * The person who reacted: [WhatsAppMessage.sender] (non-null)
     *
     * Group: [WhatsAppMessage.group] (nullable, if send in a private chat)
     */
    REACTION,
    /**
     * Multiple people voted in your poll
     *
     * The name of the poll: [WhatsAppMessage.cited] (non-null)
     *
     * The amount of people who voted: [WhatsAppMessage.sender] (Format: "{amount} people") (non-null)
     *
     * Group: [WhatsAppMessage.group] (non-null, only send if voted to a group)
     */
    GROUP_VOTE,
    /**
     * One person voting in one of you polls
     *
     * Poll voted to: [WhatsAppMessage.cited] (non-null)
     *
     * The person who voted: [WhatsAppMessage.sender] (non-null)
     *
     * Group: [WhatsAppMessage.group] (nullable, if send in a private chat)
     */
    VOTE,
    /**
     * Contact card(s)
     *
     * Content: [WhatsAppMessage.content] (Format: "{name of single contact} and {amount} other contacts" or the name of the single contact)
     *
     * The person who send them: [WhatsAppMessage.sender] (non-null)
     *
     * Group: [WhatsAppMessage.group] (nullable, if send in a private chat)
     */
    CONTACT,
    /**
     * The Location of something/someone
     *
     * Description: [WhatsAppMessage.content] (nullable, if no description is provided)
     *
     * The person sending the location: [WhatsAppMessage.sender] (non-null)
     *
     * Group: [WhatsAppMessage.group] (nullable, if send in a private chat)
     */
    LOCATION,
    /**
     * The live location of someone (only detectable without caption)
     *
     * The person sharing the location: [WhatsAppMessage.sender] (non-null)
     *
     * Group: [WhatsAppMessage.group] (nullable, if send in a private chat)
     * */
    LIVE_LOCATION,
    /**
     * A WhatsAppEvent
     *
     * Event title: [WhatsAppMessage.content] (non-null)
     *
     * The person inviting to the event: [WhatsAppMessage.sender] (non-null)
     *
     * The events date: [WhatsAppMessage.eventTimestamp] (look into that documentation)
     *
     * Group: [WhatsAppMessage.group] (non-null, not creatable in private chats)
     */
    EVENT,
    /**
     * A text message from someone, applied if nothing else matches
     *
     * Content: [WhatsAppMessage.content] (non-null)
     *
     * Sender: [WhatsAppMessage.sender] (non-null)
     *
     * Group: [WhatsAppMessage.group] (nullable, if send in a private chat)
     */
    TEXT;
    companion object {
        fun getTypeByContent(content: String): WhatsAppMessageType{
            return when {
                content.matches("^(🎤|🎵) Voice message \\(([0-9]+:)?[0-9]{1,2}:[0-9]{2}\\)$".toMultilineMatchingRegex()) -> VOICE // Combining redirected voice messages and normal ones
                content.startsWith("📊 ") -> POLL
                content.startsWith("📷 ") -> PHOTO
                content.matches("^🎥 Video note \\(([0-9]+:)?[0-9]{1,2}:[0-9]{2}\\)\$".toMultilineMatchingRegex()) -> VIDEO_NOTE
                content.matches("^🎥 .* \\(([0-9]+:)?[0-9]{1,2}:[0-9]{2}\\)\$".toMultilineMatchingRegex()) -> VIDEO
                content.matches("📄 .*\\.pdf \\([0-9]+ pages?\\)".toMultilineMatchingRegex()) -> PDF
                content.startsWith("📄 ") -> FILE
                content=="① Voice message" -> VOICE_ONCE
                content=="① Photo" -> PHOTO_ONCE
                content=="① Video" -> VIDEO_ONCE
//                false -> GROUP_REPLY (Handled in Parser)
//                false -> ADDED_TO_GROUP (Handled in getByMessageWithoutSender)
//                false -> GROUP_REACTION (Same)
                content.matches("^.*Reacted .* to \".*\"$".toMultilineMatchingRegex()) -> REACTION
//                false -> GROUP_VOTE (Handled in constructor)
                content.matches("^.*Voted in: \".*\" $".toMultilineMatchingRegex()) -> VOTE // Space at the end cause WhatsApp has a space at the end for no reason
                content.startsWith("👤 ") -> CONTACT
                content=="📌 Live location" -> LIVE_LOCATION
                content.startsWith("📌 ") -> LOCATION
                content.matches("^🗓️ Invited you to an event: .* on [A-Z][a-z]+ [0-9]{1,2} at [0-9]{1,2}:[0-9]{2}(AM|PM)$".toMultilineMatchingRegex()) -> EVENT
                else -> TEXT
            }
        }
    }
}

private fun String.toMultilineMatchingRegex(): Regex {
    return toRegex(RegexOption.DOT_MATCHES_ALL)
}

class WhatsAppMessage(
        /**
         * The group where a message got send, if the message was send in a private chat, this value will be null
         *
         * Some MessageTypes need to have a group those are:
         * [WhatsAppMessageType.GROUP_REACTION], [WhatsAppMessageType.GROUP_VOTE], [WhatsAppMessageType.GROUP_REPLY], [WhatsAppMessageType.ADDED_TO_GROUP]
         * */
        val group: String?,
        /** The type of a WhatsApp message, whether it's a poll, a photo, a video or even someone reacting to your messages.
         *
         * The detection of those aren't perfect. I tried to get the best possible regexes to match without any false detections.
         * For the detection functions look into [WhatsAppMessageType.getTypeByContent] or (special cases) [newParser] and [WhatsAppMessage] (init) */
        val messageType: WhatsAppMessageType,
        /**
         * A message which already existed, which just got reacted / voted to
         *
         * Messages where cited isn't null are:
         *
         * [WhatsAppMessageType.REACTION],
         * [WhatsAppMessageType.GROUP_REACTION],
         * [WhatsAppMessageType.VOTE],
         * [WhatsAppMessageType.GROUP_VOTE]
         * */
        val cited: String?,
        /**
         * The person sending the message, has two special cases:
         *
         * [WhatsAppMessageType.ADDED_TO_GROUP] (where the sender is the person who added you)
         *
         * [WhatsAppMessageType.GROUP_REACTION] / [WhatsAppMessageType.GROUP_VOTE] (where the sender is the amount of people who reacted/voted "{amount} people")
         * */
        val sender: String,
        /**
         * The duration of a Video or Voice Message in Seconds, only non-null in [WhatsAppMessageType.VIDEO_NOTE] / [WhatsAppMessageType.VIDEO] / [WhatsAppMessageType.VOICE]
         * */
        val durationInSeconds: Int?,
        /**
         * The timestamp of a WhatsApp event as everything after the " on " in the notification, I'm not going to deal with dates. On my phone it's formated like: "Dec 21 at 2:00PM"
         * */
        val eventTimestamp: String?,
        /**
        * The content of the Message, this might be a caption, this value is only non-null if the content is unique to this message, if it is cited from another message e.x. [WhatsAppMessageType.GROUP_REACTION]
         * the reacted message is stored inside [WhatsAppMessage.cited]
         *
         * Messages where content isn't null are:
         * [WhatsAppMessageType.TEXT],
         * [WhatsAppMessageType.EVENT],
         * [WhatsAppMessageType.LOCATION] (if description provided),
         * [WhatsAppMessageType.CONTACT],
         * [WhatsAppMessageType.REACTION] (Reacted Emoji),
         * [WhatsAppMessageType.GROUP_REPLY],
         * [WhatsAppMessageType.PDF],
         * [WhatsAppMessageType.FILE],
         * [WhatsAppMessageType.VIDEO],
         * [WhatsAppMessageType.PHOTO] (if caption is present)
         * [WhatsAppMessageType.POLL]
        * */
        val content: String?
)
{
    companion object {
        fun isMessageWithoutSender(content: String): Boolean{
            return content.endsWith(" added you") || content.matches("^.*Reacted .* \\+[0-9]+ to \".*\"$".toMultilineMatchingRegex())
        }
        @Throws(WhatsAppNotificationParserException::class)
        fun getByMessageWithoutSender(content: String, EXTRA_TITLE: String, areDifferentChats: Boolean, EXTRA_CONVERSATION_TITLE: String?): WhatsAppMessage?{
            return if(content.endsWith("added you$")) {
                if(areDifferentChats){ // Multiple chats, format changes to "Group: XXX added you"
                    by(
                        "",
                        content.substringAfter(": ").substringBeforeLast(" added you"),
                        content.substringBefore(": "),
                        WhatsAppMessageType.ADDED_TO_GROUP
                    )
                } else {
                    by(
                        "",
                        content.substringBeforeLast(" added you"),
                        EXTRA_TITLE,
                        WhatsAppMessageType.ADDED_TO_GROUP
                    )
                }
            }else if(content.matches("^.*Reacted .* \\+[0-9]+ to \".*\"$".toMultilineMatchingRegex())){
                if(areDifferentChats) { //
                    by(
                        "^.* \\+[0-9]+ other @ .*: Reacted .* \\+[0-9]+ to \"(.*)\"\$".toMultilineMatchingRegex().find(content)?.groups?.get(1)?.value?:content,
                        ("^.* \\+([0-9]+) other @ .*: Reacted .* \\+[0-9]+ to \".*\"\$".toMultilineMatchingRegex().find(content)?.groups?.get(1)?.value?.toIntOrNull()?.plus(1) ?: throw WhatsAppNotificationParserException("Can't find group reaction amount")).toString()+" people",
                        "^.* \\+[0-9]+ other @ (.*): Reacted .* \\+[0-9]+ to \".*\"\$".toMultilineMatchingRegex().find(content)?.groups?.get(1)?.value?:throw WhatsAppNotificationParserException("Can't find group in message of type group reaction"),
                        WhatsAppMessageType.GROUP_REACTION
                    )
                } else {
                    by(
                        "^.* \\+[0-9]+ other: Reacted .* \\+[0-9]+ to \"(.*)\"\$".toMultilineMatchingRegex().find(content)?.groups?.get(1)?.value?:throw WhatsAppNotificationParserException("Can't parse cited message in WhatsAppMessageType.GROUP_REACTION"),
                        ("^.* \\+([0-9]+) other: Reacted .* \\+[0-9]+ to \".*\"\$".toMultilineMatchingRegex().find(content)?.groups?.get(1)?.value?.toIntOrNull()?.plus(1) ?: throw WhatsAppNotificationParserException("Can't find group reaction amount")).toString()+" people",
                        EXTRA_CONVERSATION_TITLE?:EXTRA_TITLE,
                        WhatsAppMessageType.GROUP_REACTION
                    )
                }
            } else {
                null
            }
        }
        @Throws(WhatsAppNotificationParserException::class)
        fun by(initialContent: String, initialSender: String, initialGroup: String?, initialMessageType: WhatsAppMessageType?=null): WhatsAppMessage{
            val group: String?
            val messageType: WhatsAppMessageType
            val cited: String?
            val sender: String
            val durationInSeconds: Int?
            val eventTimestamp: String?
            val content: String?
            if(initialMessageType!=null) {
                messageType = initialMessageType
                sender = initialSender
                group = initialGroup
                content = (if(initialMessageType==WhatsAppMessageType.GROUP_REACTION || initialMessageType==WhatsAppMessageType.ADDED_TO_GROUP) null else initialContent)
                durationInSeconds = null
                eventTimestamp = null
                cited = (if(initialMessageType==WhatsAppMessageType.GROUP_REACTION) initialContent else null)
            } else {
                group=initialGroup
                // GROUP VOTE has the same pattern as VOTE, only the sender has a different structure
                messageType = if(initialSender.matches("^.* \\+[0-9]+ other$".toMultilineMatchingRegex())) WhatsAppMessageType.GROUP_VOTE else WhatsAppMessageType.getTypeByContent(initialContent)
                eventTimestamp = if(messageType==WhatsAppMessageType.EVENT) initialContent.substringAfterLast(" on ") else null
                durationInSeconds = if(messageType==WhatsAppMessageType.VIDEO || messageType==WhatsAppMessageType.VOICE || messageType==WhatsAppMessageType.VIDEO_NOTE){
                    initialContent.dropLast(1).substringAfterLast("(").split(":").run{foldIndexed(0) { index, prev, cur -> (prev + cur.toInt()*60.0.pow(size-index-1)).toInt()}}
                } else {
                    null
                }
                sender = if(messageType==WhatsAppMessageType.GROUP_VOTE) {
                    initialSender
                        .substringAfterLast("+")
                        .substringBefore(" ")
                        .toIntOrNull()
                        ?.plus(1)
                        ?.toString()
                        ?.plus(" people")
                        ?: throw WhatsAppNotificationParserException("Can't parse sender from WhatsAppMessageType.GROUP_VOTE")
                } else {
                    initialSender
                }
                cited = if(messageType==WhatsAppMessageType.REACTION || messageType==WhatsAppMessageType.GROUP_VOTE || messageType==WhatsAppMessageType.VOTE){
                    initialContent.substringAfter("\"").dropLast(if(messageType==WhatsAppMessageType.REACTION) 1 else 2) // '" ' are the last characters
                } else {
                    null
                }
                content=when(messageType){
                    WhatsAppMessageType.VOICE -> null
                    WhatsAppMessageType.POLL -> initialContent.drop("📊 ".length)
                    WhatsAppMessageType.PHOTO -> initialContent.drop("📷 ".length).let { if(it=="Photo") null else it }
                    WhatsAppMessageType.VIDEO_NOTE -> null
                    WhatsAppMessageType.VIDEO -> initialContent.drop("🎥 ".length).let { if(it.substringBeforeLast(" (")=="Video") null else it }
                    WhatsAppMessageType.PDF -> initialContent.drop("📄 ".length)
                    WhatsAppMessageType.FILE -> initialContent.drop("📄 ".length)
                    WhatsAppMessageType.VOICE_ONCE -> null
                    WhatsAppMessageType.PHOTO_ONCE -> null
                    WhatsAppMessageType.VIDEO_ONCE -> null
                    WhatsAppMessageType.GROUP_REPLY, WhatsAppMessageType.ADDED_TO_GROUP, WhatsAppMessageType.GROUP_REACTION -> throw WhatsAppNotificationParserException("Can't be triggered cause a ${messageType.name} will be caught in the first part of the constructor and WhatsAppMessageType.getTypeByContent can't return ${messageType.name}")
                    WhatsAppMessageType.REACTION -> initialContent.substringAfter("Reacted ").substringBefore(" ")
                    WhatsAppMessageType.GROUP_VOTE, WhatsAppMessageType.VOTE -> null
                    WhatsAppMessageType.CONTACT -> initialContent.dropLast(" other contacts".length).substringAfterLast(" ").toIntOrNull()?.plus(1)?.toString()?.plus(" contacts")?:initialContent.drop("👤 ".length)
                    WhatsAppMessageType.LOCATION ->  initialContent.drop("📌 ".length).let { if(it=="Location") null else it }
                    WhatsAppMessageType.LIVE_LOCATION -> null
                    WhatsAppMessageType.EVENT -> initialContent.substringBeforeLast(" on ").substringAfter(": ")
                    WhatsAppMessageType.TEXT -> initialContent
                }
            }
            return WhatsAppMessage(group, messageType, cited, sender, durationInSeconds, eventTimestamp, content)
        }
    }
}

class WhatsAppNotificationParserException(message: String): Exception(message)

@Throws(WhatsAppNotificationParserException::class)
fun StatusBarNotification.parseWhatsAppStatusBarNotification(): List<WhatsAppMessage>?{
    val extras=this.notification.extras
    val allMessages = mutableListOf<WhatsAppMessage>()
    val EXTRA_SUMMARY_TEXT = extras.getCharSequence(Notification.EXTRA_SUMMARY_TEXT)?.toString()
    val EXTRA_TITLE = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString() ?: return null // Aren't required everywhere, but are always defined, if not, something is wrong
    val EXTRA_TEXT = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString() ?: return null // Same as above
    val EXTRA_TEXT_LINES = extras.getCharSequenceArray(Notification.EXTRA_TEXT_LINES)?.toList()
    val EXTRA_CONVERSATION_TITLE = extras.getCharSequence(Notification.EXTRA_CONVERSATION_TITLE)?.toString()

    if(EXTRA_TEXT=="Checking for new messages" && EXTRA_TEXT_LINES==null && EXTRA_CONVERSATION_TITLE==null && EXTRA_TITLE=="WhatsApp" && EXTRA_SUMMARY_TEXT==null) return null // Checking for new messages

    if(EXTRA_SUMMARY_TEXT==null){
        // One Message
        allMessages.add(
            if(EXTRA_CONVERSATION_TITLE==null)
            // In a chat
                WhatsAppMessage.by( // Perfectly Accurate
                    EXTRA_TEXT,
                    EXTRA_TITLE,
                    null
                )
            else
            // In a group
                if(WhatsAppMessage.isMessageWithoutSender(EXTRA_TEXT))
                    WhatsAppMessage.getByMessageWithoutSender(EXTRA_TEXT, EXTRA_TITLE, false, EXTRA_TITLE.substring(EXTRA_CONVERSATION_TITLE.length+2))!!
                else if(EXTRA_TITLE.substring(EXTRA_CONVERSATION_TITLE.length+2)=="Replied to you") // Group Reply
                    WhatsAppMessage.by( // NOT Perfectly Accurate
                        EXTRA_TEXT.substringAfter(": "),
                        EXTRA_TEXT.substringBefore(": "),
                        EXTRA_CONVERSATION_TITLE,
                        WhatsAppMessageType.GROUP_REPLY
                    )
                else
                    WhatsAppMessage.by( // Perfectly Accurate
                        EXTRA_TEXT,
                        EXTRA_TITLE.substring(EXTRA_CONVERSATION_TITLE.length+2),
                        EXTRA_CONVERSATION_TITLE
                    )
        )
    } else {
        val messagesWithoutSender = EXTRA_TEXT_LINES?.map { line ->
            if(WhatsAppMessage.isMessageWithoutSender(line.toString()))
                WhatsAppMessage.getByMessageWithoutSender(line.toString(), EXTRA_TITLE, EXTRA_SUMMARY_TEXT.contains("chats"), EXTRA_CONVERSATION_TITLE)
            else
                null
        }
        // Multiple Messages
        if(EXTRA_SUMMARY_TEXT.contains("chats")){
            // Multiple Chats
            EXTRA_TEXT_LINES?.forEachIndexed { index, line ->
                allMessages.add(
                    messagesWithoutSender?.getOrNull(index)
                        ?: WhatsAppMessage.by( // NOT Perfectly Accurate
                            line.split(": ", limit = 2).getOrNull(1) ?: "null null null false true 1",
                            line.split(": ")[0].split(" @ ").getOrNull(0) ?: "null null false true 2",
                            line.split(": ")[0].split(" @ ").getOrNull(1)
                        )
                )
            }
        } else {
            // Same Chat / Group
            if(EXTRA_TEXT_LINES?.find { !it.contains(": ") }==null){ // ESTIMATION
                // Assume it's a group
                EXTRA_TEXT_LINES?.forEachIndexed { index, line ->
                    allMessages.add(
                        messagesWithoutSender?.getOrNull(index)
                            ?: WhatsAppMessage.by( // NOT Perfectly Accurate
                                line.split(": ", limit = 2).getOrNull(1)?:"null null null false false 1",
                                line.split(": ").getOrNull(0)?:"null null null false false 2",
                                EXTRA_TITLE
                            )
                    )
                }
            } else {
                // Assume it's a chat
                EXTRA_TEXT_LINES.forEachIndexed { index, line ->
                    allMessages.add(
                        messagesWithoutSender?.getOrNull(index)
                            ?: WhatsAppMessage.by( // If estimations are right: Perfectly Accurate
                                line.toString(),
                                EXTRA_TITLE,
                                null
                            )
                    )
                }
            }
        }
    }
    return allMessages
}