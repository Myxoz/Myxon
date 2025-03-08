package com.myxoz.myxon

import androidx.compose.ui.graphics.Color

class Colors {
    companion object {
        // Official Snapchat color as provided in https://storage.googleapis.com/snap-inc/brand-guidelines/snapchat-brand-standards.pdf (08.03.2025)
        val SNAPCHAT = Color(0xFFFFFC00)

        // Official Telegram color from the official website logos: https://telegram.org/tour/screenshots (08.03.2025)
        val TELEGRAM = Color(0xFF2AABEE)

        // Official WhatsApp color as used in the header on https://about.meta.com/brand/resources/whatsapp/whatsapp-brand/ (08.03.2025)
        val WHATSAPP = Color(0xFF25D366)

        /*
        These colors are used to represent the apps like lined out in their brand guidelines,
        not to imitate them.

        This project is not affiliated with, endorsed by, or sponsored by Meta (WhatsApp), Telegram or Snap Inc (Snapchat).
        */

        val MAIN = Color(0xFF181818)
        val SECONDARY=Color(0xFF2B2B2B)
        val TERTIARY=Color(0xFF404040)
        val FONT = Color(0xFFFFFFFF)
        val SECONDARY_FONT=Color(0xFF888888)
        val TERTIARY_FONT=Color(0xFF585858)
        val CHARGING=Color(0xFF1A332A)
        //Color(0xFF21331A)
        val DISCHARGING=Color(0xFF332424)
//            Color(0xFF003B42)
//        val CHARGING=Color(0xFF00FF00)
        val BLACK=Color(0xFF000000)
        val APPSELECTED=Color(0xFF2E992E)
    }
}