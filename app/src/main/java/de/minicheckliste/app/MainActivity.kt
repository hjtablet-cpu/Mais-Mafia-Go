package de.minicheckliste.app

import android.app.*
import android.os.Bundle
import android.content.*
import android.graphics.Color
import android.net.Uri
import android.provider.Settings
import android.view.Gravity
import android.widget.*

class MainActivity : Activity() {
 private lateinit var prefs: android.content.SharedPreferences
 override fun onCreate(b: Bundle?) { super.onCreate(b); prefs=getSharedPreferences("ui",0)
  val box=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL; setPadding(40,45,40,30); gravity=Gravity.CENTER_HORIZONTAL}
  box.addView(TextView(this).apply{text="Mais Mafia Go"; textSize=26f; setTextColor(Color.BLACK)})
  box.addView(TextView(this).apply{text="Schwebende Checkliste für den Querformat-Modus. Größe und Transparenz kannst du hier jederzeit ändern."; textSize=16f; setPadding(0,18,0,18)})
  addSlider(box,"Breite", "width", 145,240,170)
  addSlider(box,"Schriftgröße", "font", 8,15,9)
  addSlider(box,"Transparenz", "alpha", 30,90,52)
  box.addView(Button(this).apply{text="CHECKLISTE STARTEN / AKTUALISIEREN"; setOnClickListener{startOverlay()}})
  box.addView(Button(this).apply{text="CHECKLISTE STOPPEN"; setOnClickListener{stopService(Intent(this@MainActivity,OverlayService::class.java))}})
  setContentView(box)
 }
 private fun addSlider(box:LinearLayout,label:String,key:String,min:Int,max:Int,def:Int){
  val t=TextView(this).apply{text="$label: ${prefs.getInt(key,def)}"; textSize=16f; setPadding(0,12,0,0)}; box.addView(t)
  val s=SeekBar(this).apply{this.max=max-min; progress=prefs.getInt(key,def)-min; setOnSeekBarChangeListener(object:SeekBar.OnSeekBarChangeListener{override fun onProgressChanged(v:SeekBar,p:Int,f:Boolean){val n=p+min;t.text="$label: $n";prefs.edit().putInt(key,n).apply()} override fun onStartTrackingTouch(v:SeekBar){} override fun onStopTrackingTouch(v:SeekBar){}})}; box.addView(s,LinearLayout.LayoutParams(-1,-2))
 }
 private fun startOverlay(){
  if(!Settings.canDrawOverlays(this)){startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,Uri.parse("package:$packageName")));Toast.makeText(this,"Bitte 'Über anderen Apps anzeigen' erlauben und danach erneut starten.",Toast.LENGTH_LONG).show();return}
  stopService(Intent(this,OverlayService::class.java)); startForegroundService(Intent(this,OverlayService::class.java))
 }
}
