package com.lq.albumXg;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.support.v4.util.LruCache;
import android.text.TextUtils;
import android.widget.ImageView;
import com.nostra13.universalimageloader.cache.disc.impl.UnlimitedDiscCache;
import com.nostra13.universalimageloader.cache.disc.naming.Md5FileNameGenerator;
import com.nostra13.universalimageloader.cache.memory.impl.LruMemoryCache;
import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.ImageLoaderConfiguration;
import com.nostra13.universalimageloader.core.assist.ImageScaleType;
import com.nostra13.universalimageloader.core.assist.QueueProcessingType;
import com.nostra13.universalimageloader.core.listener.ImageLoadingListener;
import com.nostra13.universalimageloader.core.listener.ImageLoadingProgressListener;
import com.nostra13.universalimageloader.utils.DiskCacheUtils;

import java.io.File;
import java.util.HashMap;

/**
 * 网络图片工具类，处理对图片的请求，cache管理等逻辑
 * @author changxiang
 *
 */
public class ImageHelper {

	/** 缓存不同配置的图片设置 */
	private static HashMap<String, DisplayImageOptions> displayImageOptionsMap = new HashMap<String, DisplayImageOptions>();
	
	/** 
	 * 缓存默认图的处理 ，开辟比较小的内存空间,1M的空间
	 */
	private static LruCache<String, BitmapDrawable> defaultImageProcessedMap = new LruCache<String, BitmapDrawable>(1024 * 1024 * 1){
		@Override
		protected int sizeOf(String key, BitmapDrawable value) {
			int size = 0;
			if(value != null && value.getBitmap() != null){
				size = value.getBitmap().getRowBytes() * value.getBitmap().getHeight();
			}
			return size;
		}
		
	};

	/**
	 * 使用网络图片需要的相关配置
	 * @param context
	 */
	public static void initConfig(Context context){
		// 动态设定memory cache大小
    	long heapSize = Runtime.getRuntime().maxMemory();
        int div = 8;
        if (heapSize / 1024 > 200000) {
            div = 4;
        }
        if (heapSize / 1024 > 500000) {
            div = 2;
        }
        int imageMemoryCacheSize = (int)(heapSize / div);

        ImageLoaderConfiguration.Builder builder = new ImageLoaderConfiguration.Builder(context);
        builder.threadPriority(Thread.NORM_PRIORITY - 2)
               .diskCache(new UnlimitedDiscCache(BaseStorageUtils.getOwnCacheDirectory(context, "uil"),
                       null, new Md5FileNameGenerator()))
               .memoryCache(new LruMemoryCache(imageMemoryCacheSize))
               .memoryCacheExtraOptions(640, 640)
               .threadPoolSize(3)
               .tasksProcessingOrder(QueueProcessingType.FIFO);
        builder.defaultDisplayImageOptions(getDefaultImageOptions(context));
        switch (div) {
            case 8:
                builder.memoryCacheExtraOptions(320, 320);
                builder.diskCacheExtraOptions(320, 320, null);
                builder.defaultDisplayImageOptions(getImageOptions(context, true,
                        true, 0, ImageScaleType.IN_SAMPLE_POWER_OF_2, true));
                break;
            case 4:
                builder.memoryCacheExtraOptions(480, 480);
                builder.diskCacheExtraOptions(480, 480, null);
                break;
            default:
                builder.memoryCacheExtraOptions(640, 640);
                break;

        }
        ImageLoader.getInstance().init(builder.build());
    }
	
    /**
     * 清除内存缓存
     */
	public static void clearMemoryCache(){
        try {
            ImageLoader.getInstance().clearMemoryCache();
            defaultImageProcessedMap.evictAll();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
	
	/**
	 * 清除文件缓存
	 */
	public static void clearDiscCache(){
		ImageLoader.getInstance().clearDiskCache();
	}
	
	/**
	 * 恢复后台图片请求的处理
	 */
	public static void resume(){
		ImageLoader.getInstance().resume();
	}
	
	/**
	 * 暂停后台图片请求的处理， listview滑动时为了更好的流畅性可以使用
	 */
	public static void pause(){
		ImageLoader.getInstance().pause();
	}

    public static void loadImageToDiscCache(Context context, String uri) {
        // 非五种支持的格式，暂时判定为本地路径
        File inCache = DiskCacheUtils.findInCache(uri, ImageLoader.getInstance().getDiskCache());
        if (inCache != null) {
            return;
        }
        uri = formatUri(uri);
        ImageLoader.getInstance().loadImage(uri, getImageOptions(context, false, true, 0, ImageScaleType.IN_SAMPLE_POWER_OF_2), null);
    }

    /**
	 * 显示图片
     * @param imageView
     * @param uri
     * @param progressListener
     * @param use565
     */
	protected static void displayImage(ImageView imageView, String uri, int imageRes, ImageLoadingListener stateListener,
                                       ImageLoadingProgressListener progressListener, boolean use565) {
        ImageScaleType exactly = null;
        if (use565) {
            exactly = ImageScaleType.EXACTLY;
        } else {
            exactly = ImageScaleType.IN_SAMPLE_POWER_OF_2;
        }
        DisplayImageOptions options = getImageOptions(imageView.getContext(), true, true, imageRes, exactly, use565);

        // 非五种支持的格式，暂时判定为本地路径
        uri = formatUri(uri);
        ImageLoader.getInstance().displayImage(uri, imageView, options, stateListener, progressListener);
	}
	
    private static String formatUri(String uri) {
        if (!TextUtils.isEmpty(uri)) {
            if (!uri.startsWith("http://") && !uri.startsWith("file:///") && !uri.startsWith("content://")
                    && !uri.startsWith("assets://") && !uri.startsWith("drawable://")) {
                uri = "file:///" + uri;
            }
        }
        return uri;
    }

    /**
	 * 获取默认的图片配置
	 * @return
	 */
	protected static DisplayImageOptions getDefaultImageOptions(Context context){
		return getImageOptions(context, true, true, 0, ImageScaleType.IN_SAMPLE_POWER_OF_2);
	}

    /**
     * 获取图片配置，复用已有的，没有则重新创建
     * @param memoryCache 是否缓存到内存里
     * @param diskCache   是否缓存到文件
     * @param imageRes    默认的图片
     * @param scaleType   图片的缩放方式
     * @return
     */
    protected static DisplayImageOptions getImageOptions(Context context, boolean memoryCache, boolean diskCache,
                                                         int imageRes, ImageScaleType scaleType
                                                        ) {
        return getImageOptions(context, memoryCache, diskCache, imageRes, scaleType, false);
    }

    /**
	 * 获取图片配置，复用已有的，没有则重新创建
	 * @param memoryCache 是否缓存到内存里
	 * @param diskCache   是否缓存到文件
	 * @param imageRes    默认的图片
	 * @param scaleType   图片的缩放方式
	 * @param use565      使用565像素解析Bitmap
     * @return
	 */
	protected static DisplayImageOptions getImageOptions(Context context, boolean memoryCache, boolean diskCache, int imageRes, ImageScaleType scaleType,  boolean use565){
		StringBuilder buffer = new StringBuilder();
		buffer.append(memoryCache).append("|");
		buffer.append(diskCache).append("|");
		buffer.append(imageRes).append("|");
		buffer.append((scaleType == null)?"":scaleType).append("|");
		String key = buffer.toString();
    	DisplayImageOptions options = displayImageOptionsMap.get(key);
		if(options == null){			
			// 对图片的处理
//			WebImageView.ImageProcesser processer = ImageProcesserFactory.getInstance().getImageProcesser(context, processType);
			Drawable drawable = getDefaultProcessedDrawable(context, imageRes);

            if (drawable != null) {
                options = new DisplayImageOptions.Builder()
                        .cacheInMemory(memoryCache)
                        .considerExifParams(true)
                        .cacheOnDisk(diskCache)
                        .showImageForEmptyUri(drawable)
                        .showImageOnFail(drawable)
                        .showImageOnLoading(drawable)
                        .imageScaleType(scaleType)
//                        .preProcessor(processer)
                        .decodingOptions(getBitmapOptions(use565))
                        .build();
            } else {
                options = new DisplayImageOptions.Builder()
                        .cacheInMemory(memoryCache)
                        .considerExifParams(true)
                        .cacheOnDisk(diskCache)
                        .imageScaleType(scaleType)
//                        .preProcessor(processer)
                        .decodingOptions(getBitmapOptions(use565))
                        .build();
            }
            displayImageOptionsMap.put(key, options);
		}
		
		return options;
	}
	
	/**
	 * 获取默认的图片，区分处理效果
	 * @param context
	 * @param imageRes      默认资源图
	 * @return
	 */
	private static Drawable getDefaultProcessedDrawable(Context context, int imageRes) {
        if (imageRes <= 0) {
            return new ColorDrawable(context.getResources().getColor(android.R.color.transparent));
        }
        StringBuilder buffer = new StringBuilder();
        buffer.append(imageRes).append("|");
//        buffer.append(processType == null ? "" : processType).append("|");
        String key = buffer.toString();

        BitmapDrawable drawable = defaultImageProcessedMap.get(key);
        if (drawable == null) {
            Bitmap bitmap = BitmapFactory.decodeResource(context.getResources(), imageRes);
//            WebImageView.ImageProcesser processer = ImageProcesserFactory.getInstance().getImageProcesser(context, processType);
//            if (processer != null) {
//                bitmap = processer.process(bitmap);
//        }
            drawable = new BitmapDrawable(context.getResources(), bitmap);
            defaultImageProcessedMap.put(key, drawable);
        }
        return drawable;
    }


    private static BitmapFactory.Options getBitmapOptions(boolean in565) {
        BitmapFactory.Options opts = new BitmapFactory.Options();
        opts.inInputShareable = true;
        opts.inPreferQualityOverSpeed = false;
        if (in565) {
            opts.inPreferredConfig = Bitmap.Config.RGB_565;
        }
        return opts;
    }

    public static Bitmap decodeSampledBitmap565FromFile(String filename,
                                                        int reqWidth, int reqHeight) {

        // First decode with inJustDecodeBounds=true to check dimensions
        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(filename, options);

        // Calculate inSampleSize
        options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight);
        options.inPreferredConfig = Bitmap.Config.RGB_565;

        // Decode bitmap with inSampleSize set
        options.inJustDecodeBounds = false;
        return BitmapFactory.decodeFile(filename, options);
    }

    public static int calculateInSampleSize(BitmapFactory.Options options,
                                            int reqWidth, int reqHeight) {
        // Raw height and width of image
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {
            if (height < width) {
                inSampleSize = Math.round((float) height / (float) reqHeight);
            } else {
                inSampleSize = Math.round((float) width / (float) reqWidth);
            }

            // This offers some additional logic in case the image has a strange
            // aspect ratio. For example, a panorama may have a much larger
            // width than height. In these cases the total pixels might still
            // end up being too large to fit comfortably in memory, so we should
            // be more aggressive with sample down the image (=larger
            // inSampleSize).

            final float totalPixels = width * height;

            // Anything more than 2x the requested pixels we'll sample down
            // further.
            final float totalReqPixelsCap = reqWidth * reqHeight * 2;

            while (totalPixels / (inSampleSize * inSampleSize) > totalReqPixelsCap) {
                inSampleSize++;
            }
        }
        return inSampleSize;
    }
}
