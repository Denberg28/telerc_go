package io.github.denberg28.telerc
import android.app.Activity
import android.app.Instrumentation
import android.content.Intent
import android.os.Bundle
import android.os.SystemClock
import android.view.View
import android.view.ViewGroup
import io.github.denberg28.telerc.go.BuildConfig
import java.net.DatagramSocket
import java.net.DatagramPacket
import java.net.InetAddress
import java.io.File
class GoInstrumentation:Instrumentation(){
 private var testOnly=false
 private var wallpaperOnly=false
 override fun onCreate(args:Bundle?){super.onCreate(args);wallpaperOnly=args?.getString("wallpaperOnly")=="true";testOnly=args?.getString("testOnly")=="true";start()}
 private fun <T> main(action:()->T):T{var value:Any?=null;var failure:Throwable?=null;runOnMainSync{try{value=action()}catch(t:Throwable){failure=t}};failure?.let{throw it};@Suppress("UNCHECKED_CAST")return value as T}
 private fun waitFor(test:()->Boolean){val until=SystemClock.elapsedRealtime()+10000;while(SystemClock.elapsedRealtime()<until){if(test())return;Thread.sleep(30)};error("Timed out waiting for Go UI")}
 private fun find(v:View,text:String):View?{if(v is android.widget.TextView&&v.text.toString()==text)return v;if(v is ViewGroup)for(i in 0 until v.childCount)find(v.getChildAt(i),text)?.let{return it};return null}
 private fun click(a:Activity,text:String){waitFor{main{find(a.window.decorView,text)?.isEnabled==true}};main{check(find(a.window.decorView,text)?.performClick()==true){"Missing $text"}}}
 private fun shot(name:String){
  val until=SystemClock.elapsedRealtime()+10000
  while(true){
   waitForIdleSync();Thread.sleep(500)
   val b=uiAutomation.takeScreenshot()
   // Starting/rotation windows can obscure a rendered View hierarchy. Require
   // the navy app surface in every quadrant before recording visual evidence.
   val ready=(0..1).all{qx->(0..1).all{qy->
    var blue=0;var total=0
    for(x in qx*b.width/2 until (qx+1)*b.width/2 step 24)for(y in qy*b.height/2 until (qy+1)*b.height/2 step 24){
     val c=b.getPixel(x,y);if(android.graphics.Color.blue(c)-android.graphics.Color.red(c)>=12)blue++;total++
    }
    blue>total/10
   }}
   if(ready){File(targetContext.filesDir,name).outputStream().use{b.compress(android.graphics.Bitmap.CompressFormat.PNG,100,it)};b.recycle();return}
   b.recycle();check(SystemClock.elapsedRealtime()<until){"Launch surface still obscures $name"}
  }
 }
 private fun visible(a:Activity,label:String){val v=find(a.window.decorView,label)?:error("Missing $label");val r=android.graphics.Rect();check(v.getGlobalVisibleRect(r)&&r.height()>=v.height-1){"Clipped $label"}}
 private fun control(value:String){DatagramSocket().use{s->val b=value.toByteArray();s.send(DatagramPacket(b,b.size,InetAddress.getByName("10.0.2.2"),14551))}}
 private fun currentRoot(a:GoActivity):View {
  val field=GoActivity::class.java.getDeclaredField("showing");field.isAccessible=true
  val d=field.get(a) as? android.app.Dialog
  return d?.window?.decorView?:a.window.decorView
 }
 private fun tap(a:GoActivity,label:String){waitFor{main{find(currentRoot(a),label)?.isEnabled==true}};main{check(find(currentRoot(a),label)!!.performClick())}}
 private fun all(v:View):List<View> = listOf(v)+if(v is ViewGroup)(0 until v.childCount).flatMap{all(v.getChildAt(it))}else emptyList()
 private fun fit(a:GoActivity){waitFor{main{(all(currentRoot(a)).firstOrNull{it.tag=="go-sheet"}?.height?:0)>0}};waitForIdleSync();main{fitted(a)}}
 private fun fitted(a:GoActivity){
  val root=currentRoot(a)
  check(all(root).none{it is android.widget.ScrollView||it is android.widget.HorizontalScrollView}){"Popup contains a scrolling container"}
  val sheet=all(root).first{it.tag=="go-sheet"}
  val available=android.graphics.Rect().also{a.window.decorView.getWindowVisibleDisplayFrame(it)}
  val seen=android.graphics.Rect();check(sheet.getGlobalVisibleRect(seen))
  val xy=IntArray(2);sheet.getLocationOnScreen(xy);val bounds=android.graphics.Rect(xy[0],xy[1],xy[0]+sheet.width,xy[1]+sheet.height)
  check(seen.height()>=sheet.height-1&&seen.width()>=sheet.width-1&&available.contains(bounds)){"Sheet clipped: $bounds / $available"}
  val widgets=all(sheet).filter{it is android.widget.Button||it is android.widget.EditText||it is android.widget.SeekBar}
  val rectangles=widgets.map{v->android.graphics.Rect().also{r->check(v.getGlobalVisibleRect(r)&&r.height()>=v.height-1&&r.width()>=v.width-1){"Clipped ${v.contentDescription} / ${(v as? android.widget.TextView)?.text}"}}}
  for(i in rectangles.indices)for(j in i+1 until rectangles.size)check(!android.graphics.Rect.intersects(rectangles[i],rectangles[j])){"Overlapping form actions"}
 }
 private fun controlsPosition(a:GoActivity){
  val arm=all(a.window.decorView).first{it.tag=="go-arm"};val status=all(a.window.decorView).first{it.tag=="go-status"}
  fun bounds(v:View)=android.graphics.Rect().also{check(v.getGlobalVisibleRect(it))}
  val ar=bounds(arm);val sr=bounds(status);val cr=bounds(arm.parent.parent as View)
  check(ar.top>cr.centerY()&&ar.bottom<=cr.bottom&&sr.bottom<ar.top){"ARM/status not in lower center"}
  check(kotlin.math.abs(ar.centerX()-cr.centerX())<=2&&kotlin.math.abs(sr.centerX()-ar.centerX())<=2){"ARM/status not centered"}
  val version=all(a.window.decorView).filterIsInstance<android.widget.TextView>().first{it.text.toString()=="TeleRC Go v${BuildConfig.VERSION_NAME}"}
  val vr=bounds(version);check(version.isClickable&&vr.left<cr.centerX()&&vr.top<cr.top){"Version not clickable at upper left"}
 }
 private fun verifyUpdater(a:GoActivity){
  val candidate=File(targetContext.filesDir,"update-candidate.apk");check(candidate.isFile)
  val digest=GoApkVerifier.hash(candidate)
  check(GoApkVerifier.verify(a,candidate,digest,"1.0.7")){"Newer same-package/signer APK rejected"}
  check(!GoApkVerifier.verify(a,candidate,"00".repeat(32),"1.0.7")){"Wrong hash accepted"}
  check(!GoApkVerifier.verify(a,candidate,digest,"9.9.9")){"Wrong version accepted"}
  val current=File(a.applicationInfo.sourceDir);check(!GoApkVerifier.verify(a,current,GoApkVerifier.hash(current))){"Same-code APK accepted"}
  val other=File(targetContext.filesDir,"update-wrong-package.apk");check(!GoApkVerifier.verify(a,other,GoApkVerifier.hash(other))){"Full TeleRC APK accepted"}
  val calls=java.util.Collections.synchronizedList(mutableListOf<String>())
  val offered=java.util.concurrent.atomic.AtomicReference<GoUpdateCandidate?>()
  val messages=java.util.Collections.synchronizedList(mutableListOf<String>())
  val fake=GoUpdater(a,{messages.add(it)},{value,_->offered.set(value)},{url->
   calls.add(url)
   if(url.endsWith("matching-refs/tags/go-v"))"""[{"ref":"refs/tags/v9.9.9"},{"ref":"refs/tags/go-v1.0.7"},{"ref":"refs/tags/go-v1.0.8"}]"""
   else {
    val v=if(url.endsWith("1.0.8"))"1.0.8" else "1.0.7"
    """{"tag_name":"go-v$v","draft":false,"prerelease":${v=="1.0.8"},"assets":[{"name":"TeleRC-Go-v$v.apk","browser_download_url":"https://github.com/Denberg28/TeleRC/releases/download/go-v$v/TeleRC-Go-v$v.apk","digest":"sha256:$digest","size":${candidate.length()}}]}"""
   }
  })
  main{fake.check()}
  try{waitFor{offered.get()!=null||messages.any{it!="Checking for updates…"}}}catch(t:Throwable){error("Update callback timed out: calls=$calls messages=$messages finishing=${a.isFinishing} destroyed=${a.isDestroyed} fixtureBytes=${candidate.length()}")}
  check(offered.get()?.version=="1.0.7"){"Stable Go selection failed: calls=$calls messages=$messages fixtureBytes=${candidate.length()}"}
  check(calls.none{it.endsWith("releases/latest")||it.endsWith("v9.9.9")});main{fake.close()}
 }
 private fun await(a:GoActivity,phase:String,test:()->Boolean){try{waitFor(test)}catch(t:Throwable){error("$phase: ${a.link.snapshot()}")}}
 private fun imageReady(a:GoActivity){
  val field=GoActivity::class.java.getDeclaredField("imageWork").apply{isAccessible=true}
  waitFor{main{!field.getBoolean(a)}}
 }
 private fun wallpaper(a:GoActivity)=all(a.window.decorView).filterIsInstance<android.widget.ImageView>().first{it.tag=="go-wallpaper"}
 private fun selectImage(a:GoActivity,result:Int,path:String?=null){
  val filter=android.content.IntentFilter(Intent.ACTION_OPEN_DOCUMENT).apply{addCategory(Intent.CATEGORY_OPENABLE);addDataType("image/*")}
  val data=path?.let{Intent().setData(android.net.Uri.parse("content://io.github.denberg28.telerc.go.test.images/$it"))}
  val monitor=addMonitor(filter,ActivityResult(result,data),true)
  try{
   tap(a,"Settings");tap(a,"Background");fit(a);tap(a,"Choose image")
   waitFor{monitor.hits==1};waitForIdleSync();imageReady(a)
   main{check(!a.link.snapshot().enabled&&!a.link.snapshot().pending)}
   // Let Android's result/error toast expire before capturing another sheet.
   if(result==Activity.RESULT_OK)Thread.sleep(3800)
  }finally{removeMonitor(monitor)}
 }
 private fun verifyWallpaper(){
  ThemeImageStore.remove(targetContext)
  fun launch()=(startActivitySync(Intent(targetContext,GoActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) as GoActivity).also{imageReady(it)}
  var a=launch()
  selectImage(a,Activity.RESULT_CANCELED)
  check(!ThemeImageStore.exists(targetContext)){"Picker cancellation created a wallpaper"}
  selectImage(a,Activity.RESULT_OK,"photo")
  check(ThemeImageStore.exists(targetContext)){"Photo was not saved"}
  val saved=File(targetContext.filesDir,"theme-background.jpg").readBytes()
  main{
   val image=(wallpaper(a).drawable as android.graphics.drawable.BitmapDrawable).bitmap
   check(image.width==320&&image.height==160){"Imported photo not rendered"}
   check(kotlin.math.abs(android.graphics.Color.blue(image.getPixel(0,0))-150)<8)
  }
  shot("go-wallpaper-custom.png")
  selectImage(a,Activity.RESULT_CANCELED)
  selectImage(a,Activity.RESULT_OK,"invalid")
  check(saved.contentEquals(File(targetContext.filesDir,"theme-background.jpg").readBytes())){"Failed import replaced the wallpaper"}
  selectImage(a,Activity.RESULT_OK,"oversize")
  check(saved.contentEquals(File(targetContext.filesDir,"theme-background.jpg").readBytes())){"Oversize import replaced the wallpaper"}
  // A normal UI rebuild and Activity replacement both retain the imported photo.
  tap(a,"Settings");tap(a,"Sensitivity");tap(a,"Done")
  main{check((wallpaper(a).drawable as android.graphics.drawable.BitmapDrawable).bitmap.width==320);a.finish()}
  waitForIdleSync();a=launch()
  main{check((wallpaper(a).drawable as android.graphics.drawable.BitmapDrawable).bitmap.width==320)}
  for(scale in listOf("1.0","1.3")){
   main{a.finish()};waitForIdleSync()
   if(scale=="1.3"){
    android.os.ParcelFileDescriptor.AutoCloseInputStream(uiAutomation.executeShellCommand("wm size 1280x720")).use{it.readBytes()}
    android.os.ParcelFileDescriptor.AutoCloseInputStream(uiAutomation.executeShellCommand("wm density 320")).use{it.readBytes()}
   }
   android.os.ParcelFileDescriptor.AutoCloseInputStream(uiAutomation.executeShellCommand("settings put system font_scale $scale")).use{it.readBytes()}
   waitFor{if(scale=="1.3")targetContext.resources.configuration.fontScale>=1.25f else targetContext.resources.configuration.fontScale<1.1f}
   a=launch()
   tap(a,"Settings");fit(a);shot("go-$scale-settings.png")
   tap(a,"Background");fit(a);shot("go-$scale-background.png");tap(a,"Done")
  }
  tap(a,"Settings");tap(a,"Background");tap(a,"Restore default");imageReady(a)
  check(!ThemeImageStore.exists(targetContext)){"Restore default kept the custom photo"}
  main{check((wallpaper(a).drawable as android.graphics.drawable.BitmapDrawable).bitmap.width!=320);a.finish()}
  waitForIdleSync();a=launch()
  check(!ThemeImageStore.exists(targetContext))
  main{check((wallpaper(a).drawable as android.graphics.drawable.BitmapDrawable).bitmap.width!=320);a.finish()}
 }
 private fun verifyTestGame(){
  for(scale in listOf("1.0","1.3")){
   for(command in listOf("wm size 1280x720","wm density 320","settings put system font_scale $scale"))
    android.os.ParcelFileDescriptor.AutoCloseInputStream(uiAutomation.executeShellCommand(command)).use{it.readBytes()}
   Thread.sleep(1500)
   val a=startActivitySync(Intent(targetContext,GoActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) as GoActivity
   imageReady(a);waitForIdleSync()
   fun rect(v:View)=android.graphics.Rect().also{check(v.getGlobalVisibleRect(it)&&it.width()>=v.width-1&&it.height()>=v.height-1)}
   fun symmetry(){
    val all=all(a.window.decorView)
    val labels=listOf("go-steer-label","go-drive-label").map{tag->all.first{it.tag==tag}}
    val sticks=all.filterIsInstance<JoystickView>()
    check(sticks.size==2)
    val l=rect(labels[0]);val r=rect(labels[1]);val ls=rect(sticks[0]);val rs=rect(sticks[1])
    check(l.top==r.top&&l.height()==r.height()&&ls.top==rs.top&&ls.height()==rs.height()){"Asymmetric captions/sticks"}
    check(kotlin.math.abs(l.centerX()-ls.centerX())<=1&&kotlin.math.abs(r.centerX()-rs.centerX())<=1)
    val pageButton=all.first{it.tag=="go-test"} as android.widget.TextView
    check(pageButton.lineCount==1){"Test/Controls action must stay on one line"}
    val test=rect(pageButton);val phone=rect(find(a.window.decorView,if(sticks[0].isEnabled)"Offline" else "Phone connect")!!)
    check(test.right<phone.left){"Test must precede Phone connect"}
   }
   main{symmetry();check(!a.link.snapshot().enabled)}
   shot("go-test-$scale-controls.png")
   // Start a real socket attempt, then enter Test and verify it cannot reconnect.
   main{a.link.connect(GoEndpoint(GoProfile.WIFI,"10.0.2.2",14550,0,1))}
   tap(a,"Test");Thread.sleep(300)
   main{
    symmetry();check(!a.link.snapshot().connected&&!a.link.snapshot().enabled&&!a.link.snapshot().pending)
    check(find(a.window.decorView,"Offline")?.isEnabled==false)
    val sticks=all(a.window.decorView).filterIsInstance<JoystickView>();check(sticks.all{it.isEnabled})
    val game=all(a.window.decorView).filterIsInstance<TestDriveView>().single();check(game.mode==TestDriveView.Mode.GAME)
    game.setSteering(1800);game.setDrive(1800)
   }
   Thread.sleep(300);main{check(!a.link.snapshot().connected&&!a.link.snapshot().pending)}
   shot("go-test-$scale-game.png")
   tap(a,"Test drive");main{check(all(a.window.decorView).filterIsInstance<TestDriveView>().single().mode==TestDriveView.Mode.TEST)}
   shot("go-test-$scale-drive.png");tap(a,"Reset");tap(a,"Game");tap(a,"Sound");tap(a,"Settings");tap(a,"Done")
   main{check(find(a.window.decorView,"Sound")!=null);callActivityOnPause(a);callActivityOnResume(a);check(!a.link.snapshot().connected)}
   tap(a,"Controls");main{symmetry();check(find(a.window.decorView,"Phone connect")!=null);check(all(a.window.decorView).filterIsInstance<JoystickView>().none{it.isEnabled});a.finish()}
   waitForIdleSync()
  }
 }
 override fun onStart(){try{
  if(testOnly){
   try{verifyTestGame()}finally{
    for(command in listOf("settings put system font_scale 1.0","wm size reset","wm density reset"))android.os.ParcelFileDescriptor.AutoCloseInputStream(uiAutomation.executeShellCommand(command)).use{it.readBytes()}
   }
   finish(Activity.RESULT_OK,Bundle().apply{putString("stream","PASS: offline Test game, disconnected transport, Game/Test/Reset/sound lifecycle and symmetric labels at normal/large text")});return
  }
  if(wallpaperOnly){
   try{verifyWallpaper()}finally{
    for(command in listOf("settings put system font_scale 1.0","wm size reset","wm density reset"))android.os.ParcelFileDescriptor.AutoCloseInputStream(uiAutomation.executeShellCommand(command)).use{it.readBytes()}
   }
   finish(Activity.RESULT_OK,Bundle().apply{putString("stream","PASS: gallery image picker, cancel, photo render, corrupt/oversize rollback, rebuild/restart persistence, restore default and fitted normal/large-text wallpaper settings")});return
  }
  val prefs=targetContext.getSharedPreferences("go",0);prefs.edit().putString("profile","BACKPACK").putString("WIFI.host","10.0.2.2").commit()
  val icon=targetContext.packageManager.getApplicationIcon(targetContext.packageName)
  val bitmap=android.graphics.Bitmap.createBitmap(216,216,android.graphics.Bitmap.Config.ARGB_8888)
  icon.setBounds(0,0,216,216);icon.draw(android.graphics.Canvas(bitmap))
  File(targetContext.filesDir,"go-icon.png").outputStream().use{bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG,100,it)};bitmap.recycle()
  for(scale in listOf("1.0","1.3")){
   // The large-text pass also exercises a shorter, 640 x 360 dp landscape phone.
   if(scale=="1.3"){
    android.os.ParcelFileDescriptor.AutoCloseInputStream(uiAutomation.executeShellCommand("wm size 1280x720")).use{it.readBytes()}
    android.os.ParcelFileDescriptor.AutoCloseInputStream(uiAutomation.executeShellCommand("wm density 320")).use{it.readBytes()}
    Thread.sleep(1500)
   }
   android.os.ParcelFileDescriptor.AutoCloseInputStream(uiAutomation.executeShellCommand("settings put system font_scale $scale")).use{it.readBytes()}
   waitFor{if(scale=="1.3")targetContext.resources.configuration.fontScale>=1.25f else targetContext.resources.configuration.fontScale<1.1f}
   val a=startActivitySync(Intent(targetContext,GoActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) as GoActivity
   waitFor{main{find(a.window.decorView,"Phone connect")!=null}}
   main{
    for(label in listOf("Setup","Controls","Waypoint","Balance","LoRa","Phone control","Use ELRS","STOP / Disarm"))check(find(a.window.decorView,label)==null)
    check(all(a.window.decorView).count{it is android.widget.Button}==4)
    for(label in listOf("Phone connect","Settings","ARM"))visible(a,label)
    check(!find(a.window.decorView,"ARM")!!.isEnabled);controlsPosition(a)
   }
   shot("go-$scale-controls.png")
   tap(a,"Settings");main{
    val strings=all(currentRoot(a)).filterIsInstance<android.widget.TextView>().map{it.text.toString()}
    check(strings.none{it.contains("CH5")||it.contains("Connect →")||it=="1.0.1"||it=="1.0.2"})
   };fit(a);shot("go-$scale-settings.png")
   tap(a,"Connection details");main{check(all(currentRoot(a)).none{it is android.widget.Spinner});check(all(currentRoot(a)).filterIsInstance<android.widget.EditText>().size==4)}
   shot("go-$scale-connection.png")
   main{all(currentRoot(a)).filterIsInstance<android.widget.EditText>().first().setText("192.168.4.19")};tap(a,"Cancel")
   check(prefs.getString("WIFI.host",null)=="10.0.2.2")
   tap(a,"Settings");tap(a,"Connection details");fit(a);tap(a,"Save")
   main{check(all(currentRoot(a)).none{it.tag=="go-sheet"})}
   tap(a,"Settings");tap(a,"Sensitivity");fit(a);shot("go-$scale-sensitivity.png");tap(a,"Done")
   tap(a,"TeleRC Go v${BuildConfig.VERSION_NAME}");fit(a);main{check(!a.link.snapshot().enabled)};shot("go-$scale-updates.png");tap(a,"Done")
   if(scale=="1.0")verifyUpdater(a)
   main{a.finish()};waitForIdleSync()
  }
  android.os.ParcelFileDescriptor.AutoCloseInputStream(uiAutomation.executeShellCommand("settings put system font_scale 1.0")).use{it.readBytes()}
  android.os.ParcelFileDescriptor.AutoCloseInputStream(uiAutomation.executeShellCommand("wm size reset")).use{it.readBytes()}
  android.os.ParcelFileDescriptor.AutoCloseInputStream(uiAutomation.executeShellCommand("wm density reset")).use{it.readBytes()}
  Thread.sleep(1500)
  val occupied=DatagramSocket(14550)
  val a=startActivitySync(Intent(targetContext,GoActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) as GoActivity
  tap(a,"Phone connect");await(a,"Port conflict recovery"){a.link.snapshot().fresh&&a.link.snapshot().verified};check(occupied.isBound)
  tap(a,"ARM");tap(a,"Cancel arming");await(a,"Cancel ARM"){!a.link.snapshot().pending&&!a.link.snapshot().enabled};Thread.sleep(500);check(!a.link.snapshot().armed)
  tap(a,"ARM");await(a,"Merged ARM"){a.link.snapshot().armed}
  main{a.link.axes(1700,1800)};Thread.sleep(200);control("SILENCE");await(a,"Telemetry loss"){!a.link.snapshot().enabled}
  control("RESTORE");await(a,"Telemetry recovery"){a.link.snapshot().verified&&a.link.snapshot().fresh};check(!a.link.snapshot().enabled)
  await(a,"Watchdog disarm"){!a.link.snapshot().armed}
  tap(a,"ARM");await(a,"Re-arm"){a.link.snapshot().armed};tap(a,"DISARM");await(a,"Merged DISARM"){!a.link.snapshot().armed&&!a.link.snapshot().enabled}
  tap(a,"ARM");await(a,"Reboot setup"){a.link.snapshot().armed};control("REBOOT");await(a,"Reboot revokes control"){!a.link.snapshot().enabled};check(!a.link.snapshot().pending)
  tap(a,"ARM");tap(a,"Settings");check(!a.link.snapshot().enabled&&!a.link.snapshot().pending);tap(a,"Done")
  for(i in 1..5){tap(a,"Settings");tap(a,"Done");check(a.link.snapshot().connected);check(!a.link.snapshot().enabled)}
  main{a.link.disconnect();a.finish()};occupied.close();Thread.sleep(400)
  control("ELRS_START")
  val elrs=startActivitySync(Intent(targetContext,GoActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) as GoActivity
  tap(elrs,"Phone connect");await(elrs,"RF Wi-Fi connection"){elrs.link.snapshot().verified}
  tap(elrs,"ARM");await(elrs,"RF phone ARM"){elrs.link.snapshot().armed}
  control("SA_RADIO");await(elrs,"SA takeover"){elrs.link.snapshot().handset&&!elrs.link.snapshot().enabled};check(!elrs.link.snapshot().pending)
  control("SA_DRIVE");await(elrs,"Handset armed"){elrs.link.snapshot().armed};tap(elrs,"Settings");tap(elrs,"Done");Thread.sleep(600);check(elrs.link.snapshot().armed)
  control("SA_PHONE");await(elrs,"SA return"){!elrs.link.snapshot().handset&&!elrs.link.snapshot().armed};check(!elrs.link.snapshot().enabled)
  tap(elrs,"ARM");await(elrs,"Deliberate phone return"){elrs.link.snapshot().armed};tap(elrs,"DISARM");await(elrs,"Disarm before close"){!elrs.link.snapshot().armed}
  main{elrs.link.disconnect();elrs.finish()};Thread.sleep(400)
  control("FC_START")
  val fc=startActivitySync(Intent(targetContext,GoActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) as GoActivity
  tap(fc,"Phone connect");await(fc,"Auto ArduRover + RC telemetry"){fc.link.snapshot().verified&&fc.link.snapshot().fresh}
  tap(fc,"ARM");await(fc,"FC ARM"){fc.link.snapshot().armed};control("SA_RADIO");await(fc,"FC SA takeover"){fc.link.snapshot().handset&&!fc.link.snapshot().enabled}
  control("SA_DRIVE");await(fc,"FC RF observer"){fc.link.snapshot().armed};tap(fc,"Settings");tap(fc,"Done");Thread.sleep(600);check(fc.link.snapshot().armed)
  control("SA_PHONE");await(fc,"FC phone return"){!fc.link.snapshot().handset&&!fc.link.snapshot().armed};check(!fc.link.snapshot().enabled)
  tap(fc,"ARM");await(fc,"FC deliberate ARM"){fc.link.snapshot().armed};tap(fc,"DISARM");await(fc,"FC HOLD"){fc.link.snapshot().stopped&&!fc.link.snapshot().enabled&&!fc.link.snapshot().armed}
  main{fc.link.disconnect();fc.finish()};Thread.sleep(400);control("EXIT")
  finish(Activity.RESULT_OK,Bundle().apply{putString("stream","PASS: Go Wi-Fi only; fitted non-scrolling sheets, lower center ARM/status, clickable upper-left version, verified Go-only updater archives/metadata; normal/large text and short landscape; port collision, cancelled ARM, recovery, reboot, popup transitions, SA takeover/return, passive RF observer, auto ArduRover + HOLD")})
 }catch(t:Throwable){runCatching{shot("go-failed.png")};finish(Activity.RESULT_CANCELED,Bundle().apply{putString("stream","FAIL: "+t.stackTraceToString())})}}
}
