package com.github.charleslzq.samplejava;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Environment;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.UUID;

public class BitmapFileHelper {
    public static final String directory = "/tmp_pic_taken/";

    public static String save(Bitmap bitmap) {
        String fileName = UUID.randomUUID().toString();
        File target = getFile(fileName);
        target.getParentFile().mkdirs();

        try (FileOutputStream fileOutputStream = new FileOutputStream(target)) {
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, fileOutputStream);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return fileName;
    }

    public static Bitmap load(String fileName) {
        return BitmapFactory.decodeFile(getFile(fileName).getAbsolutePath());
    }

    public static void delete(String fileName) {
        getFile(fileName).delete();
    }

    private static File getFile(String fileName) {
        return new File(Environment.getExternalStorageDirectory().getAbsolutePath() + directory + fileName);
    }
}
