package com.lq.albumXg;

import android.app.Activity;
import android.graphics.Bitmap;
import android.os.Bundle;

import java.io.File;

/**
 * Created by dingli on 2015-9-25.
 */
public class ShowViewActivity extends Activity {
    public static Bitmap bitmap;
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.show_img_activity);
        WebImageView show_img=(WebImageView)findViewById(R.id.show_img_layout);
        show_img.setImageBitmap(bitmap);
//        show_img.setImageUrl(picPath,true);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(bitmap!=null){
            bitmap=null;
            System.gc();
        }
    }
    private void deletFile(String dir) {
        BaseStorageUtils.deleteFile(new File(dir));
    }
}