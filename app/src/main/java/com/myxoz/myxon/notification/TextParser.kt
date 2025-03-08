package com.myxoz.myxon.notification

import android.os.Build
import android.os.Bundle
import android.app.Notification as n
import android.service.notification.StatusBarNotification
import androidx.annotation.RequiresApi

fun testParser(sbn: StatusBarNotification): String {
    val e = sbn.notification.extras
//    n.EXTRA_TEXT
//    n.EXTRA_TEXT_LINES
//    n.EXTRA_SUB_TEXT
//    n.EXTRA_BIG_TEXT
//    n.EXTRA_INFO_TEXT
//    n.EXTRA_SUMMARY_TEXT
    n.EXTRA_HISTORIC_MESSAGES
    n.EXTRA_MESSAGES
    n.EXTRA_TITLE_BIG
    n.EXTRA_VERIFICATION_TEXT
//    n.InboxStyle
    val messages=n.MessagingStyle.Message.getMessagesFromBundleArray(e.getParcelableArray(n.EXTRA_MESSAGES, Bundle.EMPTY.javaClass))
    return messages.joinToString { "${it}${it.senderPerson?.name}: ${it.text}" }
//    return "BUNDLE:" +
//            "\n${n.MessagingStyle.Message.getMessagesFromBundleArray(e.getParcelableArray(n.EXTRA_MESSAGES, Bundle.EMPTY.javaClass)).size}\n"+
//            "TYPE:"+
//            "\n${e.getString(n.EXTRA_TEMPLATE)}\n"+
//            "EXTRA_TEXT: "+
//            "\n${e.getCharSequence(n.EXTRA_TEXT)}\n" + //v
//            "EXTRA_TEXT_LINES: \n" +
//            "${e.getCharSequenceArray(n.EXTRA_TEXT_LINES)?.joinToString("\n") {it.toString()}}\n" + //v
//            "EXTRA_BIG_TEXT: \n" +
//            "${e.getCharSequence(n.EXTRA_BIG_TEXT)}\n"+ //hv
//            "EXTRA_SUB_TEXT: \n" +
//            "${e.getCharSequence(n.EXTRA_SUB_TEXT)}\n"+ //hv
//            "EXTRA_INFO_TEXT: \n" +
//            "${e.getCharSequence(n.EXTRA_INFO_TEXT)}\n"+ //hv
//            "EXTRA_SUMMARY_TEXT: \n" +
//            "${e.getParcelableArray(n.EXTRA_HISTORIC_MESSAGES, Bundle.EMPTY.javaClass)}"+ //hv (Only MessageStyle)
//            "EXTRA_SUB_TEXT: \n" +
//            "${e.getCharSequence(n.EXTRA_SUB_TEXT)}\n"+ //hv
//            "EXTRA_SUB_TEXT: \n" +
//            "${e.getCharSequence(n.EXTRA_SUB_TEXT)}\n"+ //hv
//            "EXTRA_TITLE: \n" +
//            "${e.getString(n.EXTRA_TITLE)}\n" //hv
}