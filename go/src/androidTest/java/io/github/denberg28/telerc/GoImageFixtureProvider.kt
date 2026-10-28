package io.github.denberg28.telerc

import android.content.ContentProvider
import android.content.ContentValues
import android.database.Cursor
import android.graphics.Bitmap
import android.graphics.Color
import android.net.Uri
import android.os.ParcelFileDescriptor
import java.io.File

/** Test APK only: deterministic picker results without access to personal photos. */
class GoImageFixtureProvider:ContentProvider(){
 override fun onCreate()=true
 override fun getType(uri:Uri)="image/jpeg"
 override fun query(uri:Uri,projection:Array<out String>?,selection:String?,selectionArgs:Array<out String>?,sortOrder:String?):Cursor?=null
 override fun insert(uri:Uri,values:ContentValues?):Uri?=throw UnsupportedOperationException()
 override fun delete(uri:Uri,selection:String?,selectionArgs:Array<out String>?)=throw UnsupportedOperationException()
 override fun update(uri:Uri,values:ContentValues?,selection:String?,selectionArgs:Array<out String>?)=throw UnsupportedOperationException()
 override fun openFile(uri:Uri,mode:String):ParcelFileDescriptor{
  require(mode=="r")
  val name=uri.lastPathSegment
  require(name in listOf("photo","invalid","oversize"))
  val file=File(context!!.cacheDir,"picker-$name.jpg")
  when(name){
   "photo"->{
    val image=Bitmap.createBitmap(320,160,Bitmap.Config.ARGB_8888)
    image.eraseColor(Color.rgb(24,70,150))
    file.outputStream().use{check(image.compress(Bitmap.CompressFormat.JPEG,95,it))};image.recycle()
   }
   "invalid"->file.writeText("not an image")
   "oversize"->java.io.RandomAccessFile(file,"rw").use{it.setLength(20*1024*1024+1L)}
  }
  return ParcelFileDescriptor.open(file,ParcelFileDescriptor.MODE_READ_ONLY)
 }
}
