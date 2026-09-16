package de.minicheckliste.app

import android.app.*
import android.content.*
import android.graphics.*
import android.graphics.drawable.GradientDrawable
import android.os.IBinder
import android.view.*
import android.widget.*

class OverlayService:Service(){
 private lateinit var wm:WindowManager; private var root:LinearLayout?=null; private lateinit var checks:android.content.SharedPreferences; private lateinit var ui:android.content.SharedPreferences
 private val items=listOf("Sonnenblumenmais","Weizenbrot","Karotten Zucker","Milch","Spargelsalat","Zuckerrohr","Eier","Tomaten","Dosenmais","Chili-Eintopf","Erdnüsse, Erdbeeren","Bohnen + Dosenbohnen","Abends Sonnenblumen")
 override fun onBind(i:Intent?):IBinder?=null
 override fun onCreate(){super.onCreate();checks=getSharedPreferences("checks",0);ui=getSharedPreferences("ui",0);createChannel();val pi=PendingIntent.getActivity(this,0,Intent(this,MainActivity::class.java),PendingIntent.FLAG_IMMUTABLE);startForeground(7,Notification.Builder(this,"mmg").setContentTitle("Mais Mafia Go aktiv").setContentText("Checkliste wird über dem Spiel angezeigt").setSmallIcon(android.R.drawable.ic_menu_agenda).setContentIntent(pi).build());showOverlay()}
 private fun createChannel(){getSystemService(NotificationManager::class.java).createNotificationChannel(NotificationChannel("mmg","Mais Mafia Go",NotificationManager.IMPORTANCE_LOW))}
 private fun showOverlay(){
  wm=getSystemService(WINDOW_SERVICE) as WindowManager; val d=resources.displayMetrics.density; val width=ui.getInt("width",170); val font=ui.getInt("font",9).toFloat(); val alpha=ui.getInt("alpha",52); val a=(255*alpha/100f).toInt()
  root=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setPadding((3*d).toInt(),(2*d).toInt(),(3*d).toInt(),(2*d).toInt());background=GradientDrawable().apply{setColor(Color.argb(a,15,18,20));cornerRadius=10*d}}
  val head=LinearLayout(this).apply{orientation=LinearLayout.HORIZONTAL;gravity=Gravity.CENTER_VERTICAL}
  val title=TextView(this).apply{text="✓ Mais Mafia Go";setTextColor(Color.WHITE);textSize=font+1;setPadding(4,0,4,0)}
  val reset=Button(this).apply{text="↻";textSize=font;minWidth=0;minimumWidth=0;minHeight=0;minimumHeight=0;setPadding(4,0,4,0);setOnClickListener{checks.edit().clear().apply();rebuild()}}
  val close=Button(this).apply{text="×";textSize=font;minWidth=0;minimumWidth=0;minHeight=0;minimumHeight=0;setPadding(4,0,4,0);setOnClickListener{stopSelf()}}
  head.addView(title,LinearLayout.LayoutParams(0,(23*d).toInt(),1f));head.addView(reset,LinearLayout.LayoutParams((28*d).toInt(),(23*d).toInt()));head.addView(close,LinearLayout.LayoutParams((28*d).toInt(),(23*d).toInt()));root!!.addView(head)
  val rowH=(font*1.9f*d).toInt().coerceAtLeast((18*d).toInt())
  items.forEachIndexed{i,s->root!!.addView(CheckBox(this).apply{text="${i+1}. $s";textSize=font;setTextColor(Color.WHITE);setPadding(0,0,0,0);minHeight=0;minimumHeight=0;isChecked=checks.getBoolean("c$i",false);setOnCheckedChangeListener{_,v->checks.edit().putBoolean("c$i",v).apply()}},LinearLayout.LayoutParams(-1,rowH))}
  val p=WindowManager.LayoutParams((width*d).toInt(),WindowManager.LayoutParams.WRAP_CONTENT,WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,PixelFormat.TRANSLUCENT).apply{gravity=Gravity.START or Gravity.CENTER_VERTICAL;x=0;y=0}
  var sx=0;var sy=0;var tx=0f;var ty=0f
  head.setOnTouchListener{_,e->when(e.action){MotionEvent.ACTION_DOWN->{sx=p.x;sy=p.y;tx=e.rawX;ty=e.rawY;true};MotionEvent.ACTION_MOVE->{p.x=sx+(e.rawX-tx).toInt();p.y=sy+(e.rawY-ty).toInt();wm.updateViewLayout(root,p);true};else->false}}
  wm.addView(root,p)
 }
 private fun rebuild(){root?.let{wm.removeView(it)};root=null;showOverlay()}
 override fun onDestroy(){root?.let{wm.removeView(it)};root=null;super.onDestroy()}
}
