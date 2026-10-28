package io.github.denberg28.telerc;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;

/** Test APK only. Plain Java also works in the provider's separate process,
 * where the target APK's Kotlin runtime is deliberately not on the classpath. */
public class GoImageFixtureProvider extends ContentProvider {
    @Override public boolean onCreate() { return true; }
    @Override public String getType(Uri uri) { return "image/jpeg"; }
    @Override public Cursor query(Uri uri, String[] projection, String selection,
            String[] selectionArgs, String sortOrder) { return null; }
    @Override public Uri insert(Uri uri, ContentValues values) { throw new UnsupportedOperationException(); }
    @Override public int delete(Uri uri, String selection, String[] args) { throw new UnsupportedOperationException(); }
    @Override public int update(Uri uri, ContentValues values, String selection, String[] args) { throw new UnsupportedOperationException(); }
    @Override public ParcelFileDescriptor openFile(Uri uri, String mode) throws FileNotFoundException {
        String name = uri.getLastPathSegment();
        if (!"r".equals(mode) || !("photo".equals(name) || "invalid".equals(name) || "oversize".equals(name)))
            throw new FileNotFoundException("Unsupported fixture");
        File file = new File(getContext().getCacheDir(), "picker-" + name + ".jpg");
        try {
            if ("photo".equals(name)) {
                Bitmap image = Bitmap.createBitmap(320, 160, Bitmap.Config.ARGB_8888);
                image.eraseColor(Color.rgb(24, 70, 150));
                try (FileOutputStream stream = new FileOutputStream(file)) {
                    if (!image.compress(Bitmap.CompressFormat.JPEG, 95, stream)) throw new IOException("Compress failed");
                } finally { image.recycle(); }
            } else if ("invalid".equals(name)) {
                try (FileOutputStream stream = new FileOutputStream(file)) { stream.write(new byte[]{1, 2, 3}); }
            } else {
                try (RandomAccessFile stream = new RandomAccessFile(file, "rw")) { stream.setLength(20 * 1024 * 1024 + 1L); }
            }
        } catch (IOException error) {
            throw new FileNotFoundException(error.toString());
        }
        return ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY);
    }
}
