package com.glasscc

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView

class MainActivity : Activity() {
    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        val col = LinearLayout(this)
        col.orientation = LinearLayout.VERTICAL
        col.setPadding(dp(24), dp(72), dp(24), dp(24))
        col.setBackgroundColor(0xFF121216.toInt())
        fun lbl(t: String, sp: Float) {
            val tv = TextView(this)
            tv.text = t; tv.textSize = sp; tv.setTextColor(Color.WHITE)
            tv.setPadding(0, 0, 0, dp(16))
            col.addView(tv)
        }
        fun btn(t: String, i: Intent) {
            val bt = Button(this)
            bt.text = t
            bt.setOnClickListener { startActivity(i) }
            col.addView(bt)
        }
        val pkg = Uri.parse("package:$packageName")
        lbl("Glas Kontrollzentrum", 26f)
        lbl("1. Bedienungshilfe „Glas Kontrollzentrum“ aktivieren.\n2. Helligkeit-Schreibrecht erlauben.\n3. Am rechten Bildschirmrand den kleinen Strich nach links ziehen.", 15f)
        lbl("Ist der Schalter grau: App-Info → ⋮ → „Eingeschränkte Einstellungen zulassen“. Fensterunschärfe funktioniert nicht im Energiesparmodus.", 13f)
        btn("Bedienungshilfen öffnen", Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        btn("Helligkeit erlauben", Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS, pkg))
        btn("App-Info öffnen", Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, pkg))
        setContentView(col)
    }
}
