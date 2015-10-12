package com.lq.albumXg;

import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.os.Environment;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import static android.os.Environment.MEDIA_MOUNTED;

/**
 * Created by Zhp on 2014/7/9.
 */
public class BaseStorageUtils {
    private static final String EXTERNAL_STORAGE_PERMISSION = "android.permission.WRITE_EXTERNAL_STORAGE";
    private static final String TAG = "StorageUtils";

    /**
     * Returns application cache directory. Cache directory will be created on SD card
     * <i>("/Android/data/[app_package_name]/cache")</i> if card is mounted and app has appropriate permission. Else -
     * Android defines cache directory on device's file system.
     *
     * @param context Application context
     * @return Cache {@link File directory}.<br />
     * <b>NOTE:</b> Can be null in some unpredictable cases (if SD card is unmounted and
     * {@link Context#getCacheDir() Context.getCacheDir()} returns null).
     */
    public static File getCacheDirectory(Context context) {
        return getCacheDirectory(context, true);
    }

    /**
     * Returns application cache directory. Cache directory will be created on SD card
     * <i>("/Android/data/[app_package_name]/cache")</i> (if card is mounted and app has appropriate permission) or
     * on device's file system depending incoming parameters.
     *
     * @param context        Application context
     * @param preferExternal Whether prefer external location for cache
     * @return Cache {@link File directory}.<br />
     * <b>NOTE:</b> Can be null in some unpredictable cases (if SD card is unmounted and
     * {@link Context#getCacheDir() Context.getCacheDir()} returns null).
     */
    public static File getCacheDirectory(Context context, boolean preferExternal) {
        File appCacheDir = null;
        String externalStorageState;
        try {
            externalStorageState = Environment.getExternalStorageState();
        } catch (NullPointerException e) { // (sh)it happens (Issue #660)
            externalStorageState = "";
        }
        if (preferExternal && MEDIA_MOUNTED.equals(externalStorageState) && hasExternalStoragePermission(context)) {
            appCacheDir = getExternalCacheDir(context);
        }
        if (appCacheDir == null) {
            appCacheDir = context.getCacheDir();
        }
        if (appCacheDir == null) {
            appCacheDir = context.getCacheDir();
        }
        return appCacheDir;
    }

    /**
     * Returns specified application cache directory. Cache directory will be created on SD card by defined path if card
     * is mounted and app has appropriate permission. Else - Android defines cache directory on device's file system.
     *
     * @param context  Application context
     * @param cacheDir Cache directory path (e.g.: "AppCacheDir", "Android/data/com.yunmall.ym/cache/cachedir")
     * @return Cache {@link File directory}
     */
    public static File getOwnCacheDirectory(Context context, String cacheDir) {
        File appCacheDir = null;
        if (MEDIA_MOUNTED.equals(Environment.getExternalStorageState()) && hasExternalStoragePermission(context)) {
            appCacheDir = new File(getCacheDirectory(context), cacheDir);
        }
        if (appCacheDir == null || (!appCacheDir.exists() && !appCacheDir.mkdirs())) {
            appCacheDir = context.getCacheDir();
        }
        return appCacheDir;
    }

    public static boolean isExternalStorageWritable(Context context) {
        if (hasExternalStoragePermission(context)) {
            String externalStorageState;
            try {
                externalStorageState = Environment.getExternalStorageState();
            } catch (NullPointerException e) { // (sh)it happens (Issue #660)
                externalStorageState = "";
            }
            return MEDIA_MOUNTED.equals(externalStorageState);
        } else {
            return false;
        }
    }

    protected static File getExternalCacheDir(Context context) {
        File appCacheDir = context.getExternalCacheDir();
        if (appCacheDir == null) {
            File dataDir = new File(new File(Environment.getExternalStorageDirectory(), "Android"), "data");
            File cacheDir = new File(new File(dataDir, context.getPackageName()), "cache");
            if (makeNoMedia(cacheDir)) return null;
            return cacheDir;
        } else {
            if (makeNoMedia(appCacheDir)) return null;
            return appCacheDir;
        }
    }

    protected static boolean makeNoMedia(File cacheDir) {
        if (!cacheDir.exists()) {
            if (!cacheDir.mkdirs()) {
                return true;
            }
            try {
                new File(cacheDir, ".nomedia").createNewFile();
            } catch (IOException e) {
            }
        }
        return false;
    }

    protected static boolean hasExternalStoragePermission(Context context) {
        int perm = context.checkCallingOrSelfPermission(EXTERNAL_STORAGE_PERMISSION);
        return perm == PackageManager.PERMISSION_GRANTED;
    }
    public static File storeCapturedImageFile(Bitmap bm, String filePath) {
        File file=null;
        OutputStream outputStream = null;
        try {
            file = new File(filePath);
            File dir = file.getParentFile();
            if (!dir.exists())
                dir.mkdirs();
            outputStream = new FileOutputStream(file);
            bm.compress(Bitmap.CompressFormat.JPEG, 100, outputStream);
            return file;
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (outputStream != null) {
                    outputStream.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return file;
    }

    /**
     * �ݹ�ɾ���ļ�Ŀ¼
     *
     * @param dir
     *            �ļ�Ŀ¼
     */
    public static void deleteFileDir(File dir) {
        try {
            if (dir.exists() && dir.isDirectory()) {// �ж����ļ�����Ŀ¼
                if (dir.listFiles().length == 0) {// ��Ŀ¼��û���ļ���ֱ��ɾ��
                    dir.delete();
                } else {// ��������ļ��Ž����飬���ж��Ƿ����¼�Ŀ¼
                    File delFile[] = dir.listFiles();
                    int len = dir.listFiles().length;
                    for (int j = 0; j < len; j++) {
                        if (delFile[j].isDirectory()) {
                            deleteFileDir(delFile[j]);// �ݹ����deleteFileDir������ȡ����Ŀ¼·��
                        } else {
                            boolean isDeltet = delFile[j].delete();// ɾ���ļ�
                        }
                    }
                    delFile = null;
                }
                deleteFileDir(dir);// �ݹ����
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * ɾ�������ļ�
     *
     * @param file
     *            �ļ�Ŀ¼
     */
    public static void deleteFile(File file) {
        try {
            if (file != null && file.isFile() && file.exists()) {
                file.delete();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
