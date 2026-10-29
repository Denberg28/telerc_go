package io.github.denberg28.telerc

import android.app.Activity
import android.app.Dialog
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.Bitmap
import android.graphics.Rect
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.GradientDrawable
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.view.Window
import android.view.WindowManager
import android.widget.*
import io.github.denberg28.telerc.go.BuildConfig
import io.github.denberg28.telerc.go.R

/** Controls and compact, fully measured sheets; opening any sheet revokes phone drive. */
class GoActivity:Activity(){
 companion object {
  private const val IMAGE_REQUEST=45
  // Serialize private-file access even when Android replaces the Activity during import.
  private val imageExecutor=java.util.concurrent.Executors.newSingleThreadExecutor()
 }
 internal lateinit var link:GoLink
 internal lateinit var updater:GoUpdater
 private val ui=Handler(Looper.getMainLooper())
 private val prefs by lazy{getSharedPreferences("go",MODE_PRIVATE)}
 private var status:TextView?=null
 private var arm:Button?=null
 private var connect:Button?=null
 private var left:JoystickView?=null
 private var right:JoystickView?=null
 private var steerLabel:TextView?=null
 private var driveLabel:TextView?=null
 private var steer=1500
 private var drive=1500
 private var showing:Dialog?=null
 private var responseDirty=false
 private var updateMessage:TextView?=null
 private var updateDialog:Dialog?=null
 private var wallpaper:ImageView?=null
 private var themeBitmap:Bitmap?=null
 private var testing=false
 private var course:TestDriveView?=null
 private var music:Button?=null
 private var imageWork=false
 private var pendingImageUri:android.net.Uri?=null
 private fun dp(value:Int)=(value*resources.displayMetrics.density+.5f).toInt()
 private fun row()=dp(48)
 private fun text(value:String,size:Float=14f)=TextView(this).apply{
  text=value;textSize=size;setTextColor(Color.rgb(236,242,250));includeFontPadding=false
  setLineSpacing(dp(2).toFloat(),1f);setShadowLayer(dp(2).toFloat(),0f,dp(1).toFloat(),Color.BLACK)
 }
 private fun button(value:String,filled:Boolean=false,click:()->Unit)=AppUi.button(this,value,filled,click).apply{
  setAutoSizeTextTypeUniformWithConfiguration(12,14,1,android.util.TypedValue.COMPLEX_UNIT_SP)
 }
 private fun shape(color:Int)=GradientDrawable().apply{setColor(color);cornerRadius=dp(14).toFloat()}
 override fun onCreate(savedInstanceState:Bundle?){
  getSharedPreferences("appearance",0).edit().putString("background","DARK").putString("accent","Blue").apply()
  setTheme(android.R.style.Theme_Material_NoActionBar)
  super.onCreate(savedInstanceState)
  prefs.edit().putString("profile","WIFI").apply()
  link=GoLink(this){ui.removeCallbacks(refreshTask);ui.post(refreshTask)}
  updater=GoUpdater(this,::updateReport,::offerUpdate)
  render()
  imageTask(null){ThemeImageStore.load(applicationContext)}
 }
 private val refreshTask=Runnable{if(!isFinishing&&!isDestroyed)refresh()}
 private fun endpoint()=GoEndpoint(GoProfile.WIFI,prefs.getString("WIFI.host","192.168.4.1")?:"192.168.4.1",
  prefs.getInt("WIFI.port",14550),prefs.getInt("WIFI.local",14550),prefs.getInt("WIFI.system",1))
 private fun phoneConnect(){
  if(testing)return
  if(link.snapshot().connected){link.disconnect();resetSticks();refresh();return}
  val value=endpoint()
  if(value.valid()){link.connect(value);refresh()}else connectionDetails()
 }
 private fun resetSticks(){left?.reset();right?.reset();steer=1500;drive=1500;refreshLabels()}
 private fun dismissSheet(){showing?.setOnDismissListener(null);showing?.dismiss();showing=null;updateDialog=null;updateMessage=null}
 private fun render(){
  dismissSheet();course?.stop();course=null;music=null;resetSticks();left=null;right=null
  val frame=FrameLayout(this)
  wallpaper=backgroundImage().apply{tag="go-wallpaper"}
  frame.addView(wallpaper,FrameLayout.LayoutParams(-1,-1))
  frame.addView(View(this).apply{setBackgroundColor(0x28071120)},FrameLayout.LayoutParams(-1,-1))
  val root=LinearLayout(this).apply{
   keepScreenOn=true;orientation=LinearLayout.VERTICAL;setPadding(dp(12),dp(8),dp(12),dp(8))
   setOnApplyWindowInsetsListener{v,insets->
    if(android.os.Build.VERSION.SDK_INT>=30){
     val safe=insets.getInsets(android.view.WindowInsets.Type.systemBars() or android.view.WindowInsets.Type.displayCutout())
     v.setPadding(dp(12)+safe.left,dp(8)+safe.top,dp(12)+safe.right,dp(8)+safe.bottom)
    }else{
     @Suppress("DEPRECATION")
     v.setPadding(dp(12)+insets.systemWindowInsetLeft,dp(8)+insets.systemWindowInsetTop,dp(12)+insets.systemWindowInsetRight,dp(8)+insets.systemWindowInsetBottom)
    };insets}
  }
  val header=LinearLayout(this).apply{gravity=Gravity.CENTER_VERTICAL;isBaselineAligned=false}
  header.addView(text("TeleRC Go v${BuildConfig.VERSION_NAME}",17f).apply{
   setTypeface(typeface,android.graphics.Typeface.BOLD);gravity=Gravity.CENTER_VERTICAL
   isClickable=true;isFocusable=true;contentDescription="TeleRC Go v${BuildConfig.VERSION_NAME}; check for updates"
   setOnClickListener{checkUpdates()}
   setAutoSizeTextTypeUniformWithConfiguration(12,17,1,android.util.TypedValue.COMPLEX_UNIT_SP)
   maxLines=1;setPadding(0,0,dp(8),0)
   val a=obtainStyledAttributes(intArrayOf(android.R.attr.selectableItemBackground));foreground=a.getDrawable(0);a.recycle()
  },LinearLayout.LayoutParams(0,row(),1f))
  connect=button("Phone connect",true){phoneConnect()}.apply{contentDescription="Phone connection; Wi-Fi by default"}
  header.addView(button(if(testing)"Controls" else "Test"){
   link.disconnect();resetSticks();testing=!testing;render()
  }.apply{tag="go-test";contentDescription=if(testing)"Return to rover controls; connection stays disconnected" else "Open offline Test game; disconnect rover"},LinearLayout.LayoutParams(dp(76),row()).apply{rightMargin=dp(8)})
  header.addView(connect,LinearLayout.LayoutParams(dp(142),row()))
  header.addView(button("Settings"){settings()},LinearLayout.LayoutParams(dp(92),row()).apply{leftMargin=dp(8)})
  root.addView(header,LinearLayout.LayoutParams(-1,-2))
  val controls=LinearLayout(this).apply{isBaselineAligned=false}
  fun stick(vertical:Boolean)=LinearLayout(this).apply{
   orientation=LinearLayout.VERTICAL;setPadding(dp(4),0,dp(4),dp(12))
   val label=text(if(vertical)"DRIVE · CH2" else "STEER · CH1",13f).apply{
    gravity=Gravity.CENTER;maxLines=1;tag=if(vertical)"go-drive-label" else "go-steer-label"
    setAutoSizeTextTypeUniformWithConfiguration(11,13,1,android.util.TypedValue.COMPLEX_UNIT_SP)
   }
   if(vertical)driveLabel=label else steerLabel=label
   addView(label,LinearLayout.LayoutParams(-1,dp(28)))
   val joystick=JoystickView(this@GoActivity,vertical){value->
    if(vertical)drive=value else steer=value
    if(testing){if(vertical)course?.setDrive(value)else course?.setSteering(value)}else link.axes(steer,drive)
    refreshLabels()
   }
   if(vertical)this@GoActivity.right=joystick else this@GoActivity.left=joystick
   addView(joystick,LinearLayout.LayoutParams(-1,0,1f).apply{topMargin=dp(8)})
  }
  controls.addView(stick(false),LinearLayout.LayoutParams(0,-1,1.25f))
  val center=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;gravity=Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL;setPadding(dp(6),dp(8),dp(6),dp(12))}
  status=text("Join rover Wi-Fi, then Phone connect",13f).apply{
   gravity=Gravity.CENTER;maxLines=3;ellipsize=android.text.TextUtils.TruncateAt.END
   background=shape(0xc0112033.toInt());setPadding(dp(10),dp(8),dp(10),dp(8));tag="go-status"
  }
  center.addView(status,LinearLayout.LayoutParams(-1,-2))
  arm=button("ARM",true){resetSticks();link.toggleArm();refresh()}.apply{tag="go-arm"}
  center.addView(arm,LinearLayout.LayoutParams(-1,row()).apply{topMargin=dp(8)})
  controls.addView(if(testing)gamePanel()else center,LinearLayout.LayoutParams(0,-1,if(testing)2.6f else 1f).apply{leftMargin=dp(8);rightMargin=dp(8)})
  controls.addView(stick(true),LinearLayout.LayoutParams(0,-1,1.25f))
  root.addView(controls,LinearLayout.LayoutParams(-1,0,1f).apply{topMargin=dp(12)})
  frame.addView(root,FrameLayout.LayoutParams(-1,-1));setContentView(frame);refresh()
 }
 private fun gamePanel()=LinearLayout(this).apply{
  orientation=LinearLayout.VERTICAL
  val game=TestDriveView(this@GoActivity).apply{tag="go-game"};course=game
  addView(game,LinearLayout.LayoutParams(-1,0,1f))
  val actions=LinearLayout(this@GoActivity).apply{isBaselineAligned=false}
  val mode=button("Test drive"){}
  music=button("Sound"){}.apply{
   contentDescription="Turn game music on or off"
   setOnClickListener{isSelected=game.setMusic(!isSelected);text=if(isSelected)"Sound on" else "Sound"}
  }
  mode.setOnClickListener{
   resetSticks()
   val free=game.mode==TestDriveView.Mode.GAME
   game.setMode(if(free)TestDriveView.Mode.TEST else TestDriveView.Mode.GAME)
   mode.text=if(free)"Game" else "Test drive"
   music?.apply{isEnabled=!free;isSelected=false;text="Sound"}
  }
  actions.addView(mode,LinearLayout.LayoutParams(0,row(),1f))
  actions.addView(button("Reset"){resetSticks();game.resetCourse()},LinearLayout.LayoutParams(0,row(),1f).apply{leftMargin=dp(8)})
  actions.addView(music,LinearLayout.LayoutParams(0,row(),1f).apply{leftMargin=dp(8)})
  addView(actions,LinearLayout.LayoutParams(-1,row()).apply{topMargin=dp(8);bottomMargin=dp(12)})
 }
 private fun refreshLabels(){steerLabel?.text="STEER · $steer";driveLabel?.text="DRIVE · $drive"}
 private fun refresh(){
  if(!::link.isInitialized)return
  val s=link.snapshot()
  if(testing){
   status?.text="Offline Test · rover disconnected";arm?.isEnabled=false
   connect?.apply{text="Offline";isEnabled=false}
   left?.isEnabled=true;right?.isEnabled=true
   return
  }
  connect?.isEnabled=true
  status?.text=s.text
  arm?.text=when{s.handset->"Radio control";s.pending->"Cancel arming";s.resume->"Resume control";s.armed->"DISARM";else->"ARM"}
  arm?.isEnabled=!imageWork&&!s.handset&&(s.pending||s.fresh&&s.verified&&!s.stopped)
  val canDrive=!imageWork&&s.fresh&&s.enabled&&s.armed&&s.driveReady&&!s.handset
  if(left?.isEnabled!=canDrive)left?.isEnabled=canDrive
  if(right?.isEnabled!=canDrive)right?.isEnabled=canDrive
  connect?.text=if(s.connected)"Disconnect" else "Phone connect"
 }
 /** All children have intrinsic heights; no weighted body, clipping or scrollbars. */
 private fun dialog(title:String,content:View,positive:Pair<String,()->Unit>?=null):Dialog{
  link.stop(true);resetSticks();dismissSheet();course?.stop();music?.apply{isSelected=false;text="Sound"}
  val d=Dialog(this);d.requestWindowFeature(Window.FEATURE_NO_TITLE)
  val panel=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setPadding(dp(16),dp(12),dp(16),dp(12));background=shape(AppColors.surface(this@GoActivity));tag="go-sheet"}
  panel.addView(text(title,18f).apply{gravity=Gravity.CENTER;maxLines=1;setAutoSizeTextTypeUniformWithConfiguration(14,18,1,android.util.TypedValue.COMPLEX_UNIT_SP)},LinearLayout.LayoutParams(-1,dp(28)))
  panel.addView(content,LinearLayout.LayoutParams(-1,-2).apply{topMargin=dp(12)})
  val footer=LinearLayout(this).apply{isBaselineAligned=false;gravity=Gravity.CENTER_VERTICAL}
  footer.addView(button(if(positive==null)"Done" else "Cancel"){d.dismiss()},LinearLayout.LayoutParams(0,row(),1f))
  positive?.let{(label,action)->footer.addView(button(label,true){action()},LinearLayout.LayoutParams(0,row(),1f).apply{leftMargin=dp(8)})}
  panel.addView(footer,LinearLayout.LayoutParams(-1,-2).apply{topMargin=dp(12)})
  d.setContentView(panel)
  val visible=Rect().also{window.decorView.getWindowVisibleDisplayFrame(it)}
  val availableWidth=if(visible.width()>0)visible.width() else resources.displayMetrics.widthPixels
  val width=minOf(dp(420),availableWidth-dp(32)).coerceAtLeast(1)
  panel.measure(View.MeasureSpec.makeMeasureSpec(width,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(0,View.MeasureSpec.UNSPECIFIED))
  d.window?.apply{
   setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT));addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);setDimAmount(.48f)
   setGravity(Gravity.CENTER);attributes=attributes.apply{x=0;y=0;windowAnimations=0}
   // A pan keeps the focused field visible when the keyboard is open without resizing the form into its footer.
   setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN or WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN)
   setLayout(width,panel.measuredHeight)
  }
  showing=d;d.setOnDismissListener{if(showing===d){showing=null;updateDialog=null;updateMessage=null;course?.resume()}}
  d.show();refresh();return d
 }
 private fun settings(){
  val body=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL}
  val actions=listOf("Wi-Fi settings" to {startActivity(Intent(Settings.ACTION_WIFI_SETTINGS))},"Connection details" to {connectionDetails()},"Sensitivity" to {sensitivity()},"Background" to {backgroundSettings()})
  for((index,items) in actions.chunked(2).withIndex()){
   val line=LinearLayout(this).apply{isBaselineAligned=false}
   for((column,item) in items.withIndex())line.addView(button(item.first){item.second()},LinearLayout.LayoutParams(0,row(),1f).apply{if(column>0)leftMargin=dp(8)})
   body.addView(line,LinearLayout.LayoutParams(-1,-2).apply{if(index>0)topMargin=dp(8)})
  }
  dialog("Controls settings",body)
 }
 private fun backgroundImage()=ImageView(this).apply{
  themeBitmap?.let{setImageBitmap(it)}?:setImageResource(R.drawable.go_background)
  scaleType=ImageView.ScaleType.CENTER_CROP
  importantForAccessibility=View.IMPORTANT_FOR_ACCESSIBILITY_NO
 }
 private fun backgroundSettings(){
  if(imageWork){Toast.makeText(this,"Loading background…",Toast.LENGTH_SHORT).show();return}
  val body=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL}
  body.addView(backgroundImage().apply{tag="go-background-preview"},LinearLayout.LayoutParams(-1,dp(80)))
  val actions=LinearLayout(this).apply{isBaselineAligned=false}
  actions.addView(button("Choose image"){chooseBackground()},LinearLayout.LayoutParams(0,row(),1f))
  actions.addView(button("Restore default"){
   dismissSheet()
   imageTask("Default background restored"){ThemeImageStore.remove(applicationContext);null}
  }.apply{isEnabled=themeBitmap!=null||ThemeImageStore.exists(this@GoActivity)},LinearLayout.LayoutParams(0,row(),1f).apply{leftMargin=dp(8)})
  body.addView(actions,LinearLayout.LayoutParams(-1,-2).apply{topMargin=dp(8)})
  dialog("Background",body)
 }
 @Suppress("DEPRECATION")
 private fun chooseBackground(){
  if(imageWork)return
  link.stop(true);resetSticks();dismissSheet();course?.stop();music?.apply{isSelected=false;text="Sound"};refresh()
  try{
   startActivityForResult(Intent(Intent.ACTION_OPEN_DOCUMENT).apply{
    addCategory(Intent.CATEGORY_OPENABLE);type="image/*";addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
   },IMAGE_REQUEST)
  }catch(_:android.content.ActivityNotFoundException){Toast.makeText(this,"No image picker available",Toast.LENGTH_LONG).show()}
 }
 /** Decode, bound and atomically save with the full TeleRC photo importer. */
 private fun imageTask(success:String?,work:()->Bitmap?){
  if(imageWork)return
  if(success!=null){link.stop(true);resetSticks()}
  imageWork=true;refresh()
  imageExecutor.execute{
   var failure:String?=null
   val image=try{work()}catch(error:Exception){failure=error.message?:"Image could not be imported";null}
    catch(_:OutOfMemoryError){failure="Image is too large; choose a smaller photo";null}
   ui.post{
    imageWork=false
    if(!isFinishing&&!isDestroyed){
     if(failure==null){
      themeBitmap=image
      if(image==null)wallpaper?.setImageResource(R.drawable.go_background)else wallpaper?.setImageBitmap(image)
     }
     refresh()
     (failure?:success)?.let{Toast.makeText(this,it,Toast.LENGTH_LONG).show()}
     pendingImageUri?.let{uri->
      pendingImageUri=null
      imageTask("Background image saved"){ThemeImageStore.importImage(applicationContext,uri)}
     }
    }
   }
  }
 }
 @Deprecated("Activity result used for the Android document picker")
 override fun onActivityResult(requestCode:Int,resultCode:Int,data:Intent?){
  super.onActivityResult(requestCode,resultCode,data)
  if(requestCode!=IMAGE_REQUEST||resultCode!=RESULT_OK)return
  val uri=data?.data?:return
  // onCreate's queued load must finish first after process recreation. The same
  // executor orders the subsequent import; the returned URI grant is temporary.
  if(imageWork){pendingImageUri=uri;return}
  imageTask("Background image saved"){ThemeImageStore.importImage(applicationContext,uri)}
 }
 private fun sensitivity(){
  val body=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL}
  var value=ControlResponseStore.read(this)
  fun slider(label:String,initial:Int,save:(Int)->Unit){
   val caption=text("$label · $initial%",13f);body.addView(caption,LinearLayout.LayoutParams(-1,dp(22)))
   body.addView(SeekBar(this).apply{max=75;progress=initial-25;contentDescription="$label sensitivity";setPadding(dp(8),0,dp(8),0);setOnSeekBarChangeListener(object:SeekBar.OnSeekBarChangeListener{
    override fun onStartTrackingTouch(s:SeekBar?){};override fun onStopTrackingTouch(s:SeekBar?){}
    override fun onProgressChanged(s:SeekBar?,p:Int,user:Boolean){caption.text="$label · ${p+25}%";if(user){responseDirty=true;save(p+25)}}
   })},LinearLayout.LayoutParams(-1,row()))
  }
  slider("Steering",value.steeringPercent){value=value.copy(steeringPercent=it);ControlResponseStore.save(this,value)}
  body.addView(View(this),LinearLayout.LayoutParams(-1,dp(8)))
  slider("Drive",value.drivePercent){value=value.copy(drivePercent=it);ControlResponseStore.save(this,value)}
  dialog("Sensitivity",body).setOnDismissListener{showing=null;if(!isFinishing&&!isDestroyed){responseDirty=false;render()}}
 }
 private fun connectionDetails(){
  val value=endpoint();val body=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL}
  fun field(parent:LinearLayout,label:String,content:String,numeric:Boolean=false)=LinearLayout(this).run{
   orientation=LinearLayout.VERTICAL
   addView(text(label,12f),LinearLayout.LayoutParams(-1,dp(22)))
   val edit=EditText(this@GoActivity).apply{
    contentDescription=label;setText(content);setSingleLine();textSize=16f;setTextColor(Color.rgb(236,242,250));setPadding(dp(8),0,dp(8),0);minWidth=0;minimumWidth=0
    inputType=if(numeric)android.text.InputType.TYPE_CLASS_NUMBER else android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS
    imeOptions=android.view.inputmethod.EditorInfo.IME_ACTION_NEXT or android.view.inputmethod.EditorInfo.IME_FLAG_NO_EXTRACT_UI
   }
   addView(edit,LinearLayout.LayoutParams(-1,row()))
   parent.addView(this,LinearLayout.LayoutParams(0,-2,1f).apply{if(parent.childCount>0)leftMargin=dp(12)})
   edit
  }
  val first=LinearLayout(this).apply{isBaselineAligned=false};body.addView(first)
  val host=field(first,"IPv4",value.host);val remote=field(first,"Rover port",value.port.toString(),true)
  val second=LinearLayout(this).apply{isBaselineAligned=false};body.addView(second,LinearLayout.LayoutParams(-1,-2).apply{topMargin=dp(8)})
  val local=field(second,"Phone port",value.localPort.toString(),true);val id=field(second,"System ID",value.system.toString(),true)
  dialog("Connection details",body,"Save" to {
   val v=GoEndpoint(GoProfile.WIFI,host.text.toString().trim(),remote.text.toString().toIntOrNull()?:0,local.text.toString().toIntOrNull()?:0,id.text.toString().toIntOrNull()?:0)
   if(v.valid()){link.disconnect();prefs.edit().putString("profile","WIFI").putString("WIFI.host",v.host).putInt("WIFI.port",v.port).putInt("WIFI.local",v.localPort).putInt("WIFI.system",v.system).apply();render()}else host.error="Check address, ports and ID"
  })
 }
 private fun checkUpdates(){
  val message=text("Checking for updates…",14f).apply{gravity=Gravity.CENTER;minHeight=dp(76);setPadding(dp(4),dp(8),dp(4),dp(8));tag="go-update-message"}
  val d=dialog("TeleRC Go v${BuildConfig.VERSION_NAME}",message)
  updateDialog=d;updateMessage=message;updater.check()
 }
 private fun updateReport(value:String){
  if(isFinishing||isDestroyed)return
  if(showing===updateDialog&&updateDialog!=null)updateMessage?.text=value else if(hasWindowFocus())Toast.makeText(this,value,Toast.LENGTH_LONG).show()
 }
 private fun offerUpdate(candidate:GoUpdateCandidate,download:()->Unit){
  // A dismissed update sheet is not permission to reopen a prompt or download.
  if(showing!==updateDialog||updateDialog==null||!hasWindowFocus())return
  val message=text("Version ${candidate.version} is available. Download and open Android’s installer?",14f).apply{gravity=Gravity.CENTER;minHeight=dp(76);setPadding(dp(4),dp(8),dp(4),dp(8));tag="go-update-message"}
  val d=dialog("TeleRC Go update",message,"Download" to {link.disconnect();resetSticks();download();refresh()})
  updateDialog=d;updateMessage=message
 }
 @Deprecated("Platform back callback")
 override fun onBackPressed(){if(testing){link.disconnect();testing=false;render()}else super.onBackPressed()}
 override fun onConfigurationChanged(config:Configuration){super.onConfigurationChanged(config);link.stop(true);render()}
 override fun onResume(){super.onResume();course?.resume();if(responseDirty){responseDirty=false;render()};if(::link.isInitialized)link.foreground(true);if(::updater.isInitialized)updater.resumePendingInstall()}
 override fun onWindowFocusChanged(hasFocus:Boolean){super.onWindowFocusChanged(hasFocus);if(hasFocus&&::updater.isInitialized)updater.resumePendingInstall()}
 override fun onPause(){course?.stop();music?.apply{isSelected=false;text="Sound"};if(::link.isInitialized)link.foreground(false);dismissSheet();resetSticks();super.onPause()}
 override fun onDestroy(){course?.stop();if(::link.isInitialized)link.close();if(::updater.isInitialized)updater.close();ui.removeCallbacksAndMessages(null);dismissSheet();super.onDestroy()}
}
