package io.github.denberg28.telerc

import android.app.Dialog
import android.content.Context
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.widget.*

/** Same private save schema and reward boundaries as the released TeleRC Test tab. */
internal class GoMazeSession(context:Context) {
    private val prefs=context.getSharedPreferences("maze_practice",Context.MODE_PRIVATE)
    var progress=MazeProgress.restore(prefs.getInt("level",1),prefs.getInt("completed_level",0)); private set
    var wallet=MazeWallet.decode(prefs.getString("wallet","").orEmpty()); private set
    var practice=newPractice(progress.current); private set
    init { saveWallet(wallet);saveProgress() }
    private fun replayOffer(level:Int):Boolean {
        val key="replay_coin_$level"
        val saved=if(prefs.contains(key))prefs.getBoolean(key,false)else null
        val offered=MazeReplayCoins.offer(level<=progress.completed&&wallet.replayCoins<MazeWallet.MAX_REPLAY_COINS,saved)
        if(level<=progress.completed&&saved==null)prefs.edit().putBoolean(key,offered).apply()
        return offered
    }
    private fun newPractice(level:Int)=MazePractice(MazeLevels.layout(level),MazeMarket.available(level,wallet),replayCoin=replayOffer(level))
    private fun saveProgress(){prefs.edit().putInt("level",progress.current).putInt("completed_level",progress.completed).apply()}
    fun saveWallet(value:MazeWallet){wallet=value;prefs.edit().putString("wallet",wallet.encode()).apply()}
    fun claim(slot:Int){wallet.claim(practice.maze.level,slot)?.let(::saveWallet)}
    fun records()=MazeRecords.decode(prefs.getString("records","").orEmpty())
    fun clear(millis:Long){
        val next=MazeRecords.add(records(),MazeClear(practice.maze.seed,millis,System.currentTimeMillis(),MazeLayout.COURSE_VERSION,practice.maze.level))
        if(practice.takeReplayReward())wallet.creditReplay()?.let{wallet=it}
        progress=progress.clear()
        // Completion, payout and the replay-offer boundary commit together.
        prefs.edit().putString("records",MazeRecords.encode(next)).putString("wallet",wallet.encode())
            .putInt("level",progress.current).putInt("completed_level",progress.completed)
            .remove("replay_coin_${practice.maze.level}").apply()
    }
    fun select(level:Int){practice.pause();progress=progress.select(level);saveProgress();practice=newPractice(progress.current)}
    fun restart(){practice.restart(replayOffer(practice.maze.level))}
}

/** Local-only maze screen. This class has no live link or network reference. */
internal class GoMazePanel(private val activity:GoActivity,private val session:GoMazeSession,
                          private val controls:()->Unit,private val settings:()->Unit):LinearLayout(activity) {
    private fun dp(n:Int)=(n*resources.displayMetrics.density+.5f).toInt()
    private val bar get()=dp(48)
    private var scene:MazePracticeView?=null
    private var pageMenu:PopupMenu?=null
    private var steering:JoystickView?=null
    private var drive:JoystickView?=null
    internal var dialog:Dialog?=null;private set
    private fun label(value:String,size:Float=13f)=TextView(activity).apply{
        text=value;textSize=size;setTextColor(AppColors.ink(activity));includeFontPadding=false;gravity=Gravity.CENTER
    }
    private fun button(value:String,filled:Boolean=false,action:()->Unit)=AppUi.button(activity,value,filled,action).apply{
        setAutoSizeTextTypeUniformWithConfiguration(10,14,1,android.util.TypedValue.COMPLEX_UNIT_SP)
    }
    private fun surface()=GradientDrawable().apply{setColor(AppColors.surface(activity));cornerRadius=dp(12).toFloat()}
    fun neutral(){steering?.reset();drive?.reset()}
    fun pause(){scene?.pause();neutral()}
    fun close(){pageMenu?.dismiss();pageMenu=null;pause();AppDialog.dismissAll(activity);dialog=null}
    fun resume(){if(session.practice.state==MazePractice.State.FAILED&&dialog==null)restartPrompt()}
    private fun track(d:Dialog){dialog=d;d.setOnDismissListener{if(dialog===d)dialog=null}}
    private fun restartPrompt(){
        pause()
        track(MazePracticeDialogs(activity).restart(session.practice.maze.level,session.practice.elapsedMillis,session.practice.bumps){
            neutral();session.restart();scene?.changed()
        })
    }
    init { orientation=HORIZONTAL;isBaselineAligned=false;render() }
    private fun render(){
        close();removeAllViews()
        val practice=session.practice
        val view=MazePracticeView(activity,practice).apply{tag="go-maze"};scene=view
        val timer=label("",14f).apply{
            setTypeface(typeface,Typeface.BOLD);maxLines=1
            setAutoSizeTextTypeUniformWithConfiguration(10,14,1,android.util.TypedValue.COMPLEX_UNIT_SP)
        }
        val start=button("Start practice",true){}
        val levels=button("Levels"){}
        val market=button("Market · ${session.wallet.balance}",true){}
        fun cosmetics(){
            view.vehicleId=session.wallet.vehicle;view.accessories=session.wallet.accessories;view.invalidate()
            market.text="Market · ${session.wallet.balance}"
            market.contentDescription="Market, ${session.wallet.balance} coins, offline practice"
        }
        fun update(){
            timer.text="Lv ${practice.maze.level} · ${MazeEnvironment.forLevel(practice.maze.level).label} · ${MazeRecords.time(practice.elapsedMillis)}"
            levels.text=if(practice.state==MazePractice.State.CLEARED&&practice.maze.level<MazeLevels.COUNT)"Next level"else"Levels"
            start.text=when(practice.state){
                MazePractice.State.READY->"Start practice";MazePractice.State.RUNNING->"Pause"
                MazePractice.State.PAUSED->"Resume";MazePractice.State.CLEARED->"Cleared ✓";MazePractice.State.FAILED->"3 bumps · Restart"
            }
            start.isEnabled=practice.state!=MazePractice.State.CLEARED&&practice.state!=MazePractice.State.FAILED
            val running=practice.state==MazePractice.State.RUNNING
            if(steering?.isEnabled!=running)steering?.isEnabled=running
            if(drive?.isEnabled!=running)drive?.isEnabled=running
        }
        view.onUpdate=::update
        view.onCoin={slot->session.claim(slot);cosmetics()}
        view.onClear={millis->session.clear(millis);neutral();cosmetics();update()}
        view.onFailure={if(scene===view&&!activity.isFinishing&&!activity.isDestroyed)restartPrompt()}
        fun openLevel(level:Int){pause();session.select(level);render()}
        fun chooseLevel(){pause();track(MazePracticeDialogs(activity).levels(session.progress,session.records(),::openLevel))}
        levels.setOnClickListener{if(practice.state==MazePractice.State.CLEARED&&practice.maze.level<MazeLevels.COUNT)openLevel(practice.maze.level+1)else chooseLevel()}
        levels.setOnLongClickListener{chooseLevel();true}
        levels.contentDescription="Choose an unlocked level; after clearing advance to the next level"
        fun stick(vertical:Boolean)=LinearLayout(activity).apply{
            orientation=VERTICAL;setPadding(dp(4),dp(8),dp(4),dp(8))
            val title=label(if(vertical)"DRIVE"else"STEER",13f).apply{
                tag=if(vertical)"go-drive-label"else"go-steer-label";setTypeface(typeface,Typeface.BOLD)
            }
            val hint=if(vertical)"CH2 · forward / reverse"else"CH1 · left / right"
            val feedback=label("$hint · 1500",11f).apply{
                setTextColor(AppColors.muted(activity));minLines=2;maxLines=2
                setAutoSizeTextTypeUniformWithConfiguration(9,11,1,android.util.TypedValue.COMPLEX_UNIT_SP)
            }
            val caption=LinearLayout(activity).apply{
                orientation=VERTICAL;background=surface();setPadding(dp(4),dp(4),dp(4),dp(4))
                addView(title,LayoutParams(-1,dp(22)))
                addView(feedback,LayoutParams(-1,dp((32*maxOf(1f,resources.configuration.fontScale)).toInt())))
            }
            addView(caption,LayoutParams(-1,-2))
            val joystick=JoystickView(activity,vertical){value->
                if(vertical)practice.drive(value)else practice.steering(value)
                feedback.text="$hint · $value"
            }
            if(vertical)drive=joystick else steering=joystick
            addView(joystick,LayoutParams(-1,0,1f).apply{topMargin=dp(8)})
        }
        val left=stick(false);val right=stick(true)
        val page=button("Test ▾",true){}
        page.contentDescription="Test navigation menu; return to Controls or open Settings"
        page.setOnClickListener{
            pause()
            pageMenu=PopupMenu(activity,page).apply{
                setOnDismissListener{pageMenu=null}
                menu.add("Controls").setOnMenuItemClickListener{controls();true}
                menu.add("Settings").setOnMenuItemClickListener{settings();true}
                show()
            }
        }
        start.setOnClickListener{neutral();if(practice.state==MazePractice.State.RUNNING)view.pause()else practice.start();view.changed()}
        market.setOnClickListener{pause();track(MazePracticeDialogs(activity).market({session.wallet}){session.saveWallet(it);cosmetics()})}
        val clearTimes=button("Clear times"){
            pause()
            val records=session.records()
            val message=if(records.isEmpty())"No clears yet. Drive from START to END to save your time."
                else records.take(20).joinToString("\n\n"){
                    val date=java.text.DateFormat.getDateTimeInstance(java.text.DateFormat.SHORT,java.text.DateFormat.SHORT).format(java.util.Date(it.at))
                    val course=if(it.level>0)"Lv ${it.level}"else"Legacy maze ${it.seed.toUInt().toString(16)}${if(it.courseVersion==1)" · Classic"else""}"
                    "${MazeRecords.time(it.millis)} · $course\n$date"
                }
            track(AppDialog.Builder(activity).setTitle("Clear times · latest ${records.size}")
                .setMessage("$message\n\nTimes count active practice only. Up to 50 clears are saved.").setPositiveButton("Done",null).show())
        }
        fun column(top:View,body:View,bottom:View)=LinearLayout(activity).apply{
            orientation=VERTICAL;addView(top,LayoutParams(-1,bar).apply{bottomMargin=dp(8)})
            addView(body,LayoutParams(-1,0,1f));addView(bottom,LayoutParams(-1,bar).apply{topMargin=dp(8)})
        }
        val center=LinearLayout(activity).apply{
            orientation=VERTICAL;background=surface();setPadding(dp(8),dp(4),dp(8),dp(8))
            addView(timer,LayoutParams(-1,dp(28)))
            addView(view,LayoutParams(-1,0,1f).apply{topMargin=dp(4);bottomMargin=dp(4)})
            val actions=LinearLayout(activity).apply{isBaselineAligned=false}
            actions.addView(levels,LayoutParams(0,bar,1f))
            actions.addView(button("Restart level"){neutral();session.restart();view.changed()},LayoutParams(0,bar,1f).apply{leftMargin=dp(4)})
            addView(actions,LayoutParams(-1,bar))
        }
        addView(column(page,left,start),LayoutParams(0,-1,1.6f).apply{rightMargin=dp(8)})
        addView(center,LayoutParams(0,-1,3.8f).apply{rightMargin=dp(8)})
        addView(column(market,right,clearTimes),LayoutParams(0,-1,1.6f))
        cosmetics();update()
    }
}
