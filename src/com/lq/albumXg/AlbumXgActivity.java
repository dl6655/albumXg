package com.lq.albumXg;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.provider.MediaStore;
import android.support.v4.util.LruCache;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import com.squareup.picasso.Picasso;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.*;

public class AlbumXgActivity extends Activity {
    private TextView picGridViewTileBar;
    private GridView mPicsGridView;
    private ListView galleryListView;
    private Map<String, ArrayList<ImageInfoItem>> imageInfos;//相册列表
    private List<ImageInfoItem> galleryList;//图片列表
    private List<ImageInfoItem> selectedPic = new ArrayList<ImageInfoItem>();//选中的图片列表
    private AlbumAdapter mAlbumAdapter;//相册列表adapter
    private AlbumAdapterItem picsGridAdapter;// 图片Adapter
    private int inViewWhere = 0;//这个字段判断在该view的什么地方 0 表示刚进入页面展示相机的相册图片 1表示在相簿列表中 2 表示从相簿列表进入相簿的图片展示
    private ImageInfoItem mImageInfoItem;
    private LruCache<String, Bitmap> mMemoryCache;
    private Set<BitmapWorkerTask> taskCollection;
    private int[] sortArray;
    private FileSortHelper mSortHelper = new FileSortHelper();
    private CropImageView mCropImage;
    private AlbumItemViewHolder viewHolderOld;
    private LinearLayout mSelectPicUPLayout, tv_layout1;
    private ImageView mSelectPicImg, left_arrow_1;
    private com.lq.albumXg.StickyNavLayout rel_layout;
    private String picFileName, picFilePath;
    private boolean firstEnter;
    private Bitmap pathBitMap;
    private float mLastY;
    private OverScroller mScroller;
    private PicGridFragment mPicGridFragment;
    /**
     * 第一张可见图片的下标
     */
    private int mFirstVisibleItem;

    /**
     * 一屏有多少张图片可见
     */
    private int mVisibleItemCount;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        initMemory();
        initView();
    }

    private final View.OnClickListener btnListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            switch (v.getId()) {
                case R.id.buttonDone:
//                    File f=BaseStorageUtils.getOwnCacheDirectory(getApplicationContext(),"tempCrop.jpg");
//                    f=new File(f.getPath(),"tempCrop.jpg");
//                    savePic(mCropImage.getCroppedBitmap(),f.getPath());
                    // Start ResultActivity
                    ShowViewActivity.bitmap=mCropImage.getCroppedBitmap();
                    Intent intent = new Intent(AlbumXgActivity.this, ShowViewActivity.class);
//                    intent.putExtra("cropPic",f.getPath());
                    startActivity(intent);
                    break;
                case R.id.button1_1:
                    mCropImage.setCropMode(CropImageView.CropMode.RATIO_1_1);
                    button1_1.setBackgroundColor(getResources().getColor(R.color.translucent_red));
                    button3_4.setBackgroundColor(getResources().getColor(R.color.text));
                    buttonFree.setBackgroundColor(getResources().getColor(R.color.text));
                    break;
                case R.id.button3_4:
                    mCropImage.setCropMode(CropImageView.CropMode.RATIO_3_4);
                    button1_1.setBackgroundColor(getResources().getColor(R.color.text));
                    button3_4.setBackgroundColor(getResources().getColor(R.color.translucent_red));
                    buttonFree.setBackgroundColor(getResources().getColor(R.color.text));
                    break;
                case R.id.buttonFree:
                    button1_1.setBackgroundColor(getResources().getColor(R.color.text));
                    button3_4.setBackgroundColor(getResources().getColor(R.color.text));
                    buttonFree.setBackgroundColor(getResources().getColor(R.color.translucent_red));
                    mCropImage.setCropMode(CropImageView.CropMode.RATIO_FREE);
                    break;
            }

        }
    };

    private void initMemory() {
        // 获取到可用内存的最大值，使用内存超出这个值会引起OutOfMemory异常。
        // LruCache通过构造函数传入缓存值，以KB为单位。
        int maxMemory = (int) (Runtime.getRuntime().maxMemory() / 1024);
        // 使用最大可用内存值的1/8作为缓存的大小。
        int cacheSize = maxMemory / 4;
        mMemoryCache = new LruCache<String, Bitmap>(cacheSize) {
            @Override
            protected int sizeOf(String key, Bitmap bitmap) {
                // 重写此方法来衡量每张图片的大小，默认返回图片数量。
                return bitmap.getByteCount() / 1024;
            }
        };
        taskCollection = new HashSet<BitmapWorkerTask>();

    }
    private Button button1_1,button3_4,buttonFree;
    private void initView() {
        firstEnter = true;
        mScroller = new OverScroller(this);
        mCropImage = getView(R.id.cropImageView);
        mPicsGridView = getView(R.id.layout_grid);//相薄中的图片gridView
        mPicsGridView.setNumColumns(4);


//        FragmentManager fm = getFragmentManager();
//        mPicGridFragment=(PicGridFragment)fm.findFragmentById(R.id.layout_frag_grid);

        galleryListView = getView(R.id.layout_listview);//相薄listView
        mSelectPicUPLayout = getView(R.id.tv_layout);
        mSelectPicImg = getView(R.id.gallery_select_btn);
        picGridViewTileBar = getView(R.id.gallery_item_title);
        left_arrow_1 = getView(R.id.left_arrow_1);
        tv_layout1 = getView(R.id.tv_layout1);
        rel_layout = getView(R.id.rel_layout);
        button1_1=getView(R.id.button1_1);
        button3_4= getView(R.id.button3_4);
        buttonFree=getView(R.id.buttonFree);
       button1_1.setOnClickListener(btnListener);
        button3_4.setOnClickListener(btnListener);
        buttonFree.setBackgroundColor(getResources().getColor(R.color.translucent_red));
       buttonFree.setOnClickListener(btnListener);
        getView(R.id.buttonDone).setOnClickListener(btnListener);
//        scroll_view=getView(R.id.scroll_view);
//        mSelectPicImg.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                if (mCropImage.getVisibility() == View.GONE) {
//                    mCropImage.setVisibility(View.VISIBLE);
//                    mSelectPicImg.setImageResource(R.drawable.select_pic_up);
//                } else {
//                    mCropImage.setVisibility(View.GONE);
//                    mSelectPicImg.setImageResource(R.drawable.select_pic_down);
//                }
//            }
//        });
//        mSelectPicUPLayout.setOnDragListener(new View.OnDragListener() {
//            @Override
//            public boolean onDrag(View view, DragEvent dragEvent) {
//                return false;
//            }
//        });
//
//        mSelectPicUPLayout.setOnTouchListener(new View.OnTouchListener() {
//            @Override
//            public boolean onTouch(View view, MotionEvent motionEvent) {
//                float y =motionEvent.getY();
//                switch (motionEvent.getAction()){
//                    case MotionEvent.ACTION_DOWN:
////                        if (!mScroller.isFinished())
////                            mScroller.abortAnimation();
////                        mLastY = y;
////                        return true;
//                        if (mCropImage.getVisibility() == View.GONE) {
//                            mCropImage.setVisibility(View.VISIBLE);
//                            mSelectPicImg.setImageResource(R.drawable.select_pic_up);
//                        } else {
//                            mCropImage.setVisibility(View.GONE);
//                            mSelectPicImg.setImageResource(R.drawable.select_pic_down);
//                        }
//                    break;
//                    case MotionEvent.ACTION_MOVE:
////                        float dy = y - mLastY;
////                        rel_layout.computeScroll();
////                        rel_layout.scrollBy(0, (int) -dy);
////                        rel_layout.invalidate();
////                        if (mCropImage.getVisibility() == View.GONE) {
////                            mCropImage.setVisibility(View.VISIBLE);
////                            mSelectPicImg.setImageResource(R.drawable.select_pic_up);
////                        } else {
////                            mCropImage.setVisibility(View.GONE);
////                            mSelectPicImg.setImageResource(R.drawable.select_pic_down);
////                        }
//                        mLastY = y;
//                    break;
//                }
//
//                return false;
//            }
//        });
//        mSelectPicUPLayout.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                if (mCropImage.getVisibility() == View.GONE) {
//                    mCropImage.setVisibility(View.VISIBLE);
//                    mSelectPicImg.setImageResource(R.drawable.select_pic_up);
//                } else {
//                    mCropImage.setVisibility(View.GONE);
//                    mSelectPicImg.setImageResource(R.drawable.select_pic_down);
//                }
//            }
//        });
        left_arrow_1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                tv_layout1.setVisibility(View.GONE);
                setLocalalbum();
            }
        });
        galleryListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                ImageInfoItem galleryInfo = (ImageInfoItem) view.getTag(R.id.tag_info);
                tv_layout1.setVisibility(View.VISIBLE);
                changePicAdapter(galleryInfo.folderPath, galleryInfo.fileName);
            }
        });

        mPicsGridView.setOnScrollListener(new AbsListView.OnScrollListener() {

            @Override
            public void onScrollStateChanged(AbsListView absListView, int scrollState) {
//                if (scrollState == SCROLL_STATE_FLING || scrollState == SCROLL_STATE_TOUCH_SCROLL) {
//                    ImageHelper.pause();
//                } else {
//                    ImageHelper.resume();
//                }


//                // 仅当GridView静止时才去下载图片，GridView滑动时取消所有正在下载的任务
//                if (scrollState == SCROLL_STATE_IDLE) {
////                    loadBitmaps(mFirstVisibleItem, mVisibleItemCount);
//                } else {
//                    cancelAllTasks();
//                }
            }

            @Override
            public void onScroll(AbsListView absListView, int firstVisibleItem, int visibleItemCount,
                                 int totalItemCount) {

            }
        });

        mPicsGridView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
                mCropImage.setVisibility(View.VISIBLE);

                rel_layout.scrollTo(0, (int) -mCropImage.getMeasuredHeight());
//                rel_layout.computeScroll();

                mSelectPicImg.setImageResource(R.drawable.select_pic_up);
                AlbumItemViewHolder viewHolder = (AlbumItemViewHolder) view.getTag(R.id.tag_view);
                ImageInfoItem picFileInfo = (ImageInfoItem) view.getTag(R.id.tag_info);

                if (viewHolderOld != null && !(viewHolder.equals(viewHolderOld))) {
                    viewHolderOld.picImageMask.setVisibility(View.GONE);
                    viewHolder.picImageMask.setVisibility(View.VISIBLE);
                    viewHolder.picImageMask.setAlpha(0.8f);
                    viewHolderOld = viewHolder;
                } else {
                    viewHolderOld = viewHolder;
                    viewHolder.picImageMask.setVisibility(View.VISIBLE);
                    viewHolder.picImageMask.setAlpha(0.8f);
                }
                if (viewHolder.picImageMask.getVisibility() == View.VISIBLE) {
                    final String filePath = picFileInfo.filePath;
                    picFileInfo.iSelected = true;
                    picFileName = picFileInfo.fileName;
                    picFilePath = picFileInfo.filePath;
//                    mCropImage.setImageUrl(filePath, true);
                    loadBitmap(picFilePath, mCropImage);
                    scrollPosition(position);
                }

//                if (!picFileInfo.iSelected) {
//                    picFileInfo.iSelected = true;
//                    viewHolder.picImageMask.setVisibility(View.VISIBLE);
//                    viewHolder.picImageMask.setAlpha(0.8f);
//                    BitmapFactory.Options opts = new BitmapFactory.Options();
//                    opts.inJustDecodeBounds = true;
//                    BitmapFactory.decodeFile(picFileInfo.filePath, opts);
//                    int width = opts.outWidth;
//                    int height = opts.outHeight;
//                    if (isFromTopic) {
//                        if (width < 320 || height < 320) {
//                           showToast(AlbumXgActivity.this, "该图片像素较低，请选择其他图片（不能小于320*320）");
//                            return;
//                        }
//                    } else {
//                        if (width < 460 || height < 460) {
//                            showToast(AlbumXgActivity.this, "该图片像素较低，请选择其他图片（不能小于460*460）");
//                            return;
//                        }
//                    }
//
//                    viewHolder.checkBox.setChecked(picFileInfo.iSelected);
////                    remainPicCount--;
//                    picFileInfo.picWidth = width;
//                    picFileInfo.picHeight = height;
//                    selectedPic.add(picFileInfo);


//                }
//                sendResult(picFileInfo);

            }
        });
        startLoadPicTask(task);

    }

    private void showToast(Context context, String msg) {
        Toast.makeText(context, msg, Toast.LENGTH_LONG).show();

    }

    private <T extends View> T getView(int rId) {
        View view = findViewById(rId);
        return (T) view;
    }

    //相册图片
    private void setCameralbum() {
        if (isSysCameraPathHasPic && imageInfos != null && imageInfos.size() > 0 && galleryList != null && galleryList.size() > 0) {
            picsGridAdapter = new AlbumAdapterItem(AlbumXgActivity.this, android.R.id.text1, imageInfos.get(galleryList.get(0).folderPath));
            mPicsGridView.setVisibility(View.VISIBLE);
            inViewWhere = 0;
            galleryListView.setVisibility(View.GONE);
//                picGridViewTileBar.setTitle(galleryList.get(0).fileName);
        }
        mPicsGridView.setAdapter(picsGridAdapter);
    }

    //本地相册
    private void setLocalalbum() {
        inViewWhere = 1;
        mPicsGridView.setVisibility(View.GONE);
        galleryListView.setVisibility(View.VISIBLE);
        mAlbumAdapter = new AlbumAdapter(AlbumXgActivity.this, galleryList);
        galleryListView.setAdapter(mAlbumAdapter);
    }

    /**
     * 切换到图片页面
     */
    public void changePicAdapter(String gallery, String title) {
        List<ImageInfoItem> galleryItemList = imageInfos.get(gallery);
        if (galleryItemList != null) {
            galleryListView.setVisibility(View.GONE);
            mPicsGridView.setVisibility(View.VISIBLE);

//            mPicGridFragment.setGridData(AlbumXgActivity.this, android.R.id.text1, galleryItemList);

            picsGridAdapter = new AlbumAdapterItem(AlbumXgActivity.this, android.R.id.text1, galleryItemList);
            mPicsGridView.setAdapter(picsGridAdapter);
            picGridViewTileBar.setText(title);
            inViewWhere = 2;
        }
    }

    //相册列表
    private class AlbumAdapter extends BaseAdapter {
        private List<ImageInfoItem> gallaryArray = new ArrayList<>();
        private LayoutInflater layoutInflater;
        private Context context;

        public AlbumAdapter(Context context, List<ImageInfoItem> itemList) {
            gallaryArray = itemList;
            layoutInflater = LayoutInflater.from(context);
            this.context = context;
        }

        @Override
        public int getCount() {
            if (gallaryArray != null) {
                return gallaryArray.size();
            } else {
                return 0;
            }

        }

        @Override
        public Object getItem(int position) {
            if (gallaryArray != null) {
                return gallaryArray.get(position);
            }
            return null;
        }

        @Override
        public long getItemId(int position) {
            if (gallaryArray != null) {
                return gallaryArray.size();
            }
            return 0;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View view;
            if (convertView != null) {
                view = convertView;
            } else {
                view = layoutInflater.inflate(R.layout.gallery_item, null);
                GalleryItemViewHolder viewHolder = new GalleryItemViewHolder();
                viewHolder.galleryTitle = (TextView) view.findViewById(R.id.gallery_item_title);
                viewHolder.galleryItemView = (WebImageView) view.findViewById(R.id.gallery_item_image_id);
                view.setTag(R.id.tag_view, viewHolder);
            }

            ImageInfoItem picInfo = galleryList.get(position);
            if (picInfo != null) {
                view.setTag(R.id.tag_info, picInfo);
                GalleryItemViewHolder holder = (GalleryItemViewHolder) view.getTag(R.id.tag_view);

                //加载图片
                holder.galleryItemView.setImageUrl(picInfo.filePath, R.drawable.head_default_150, true, true);
                holder.galleryTitle.setText(picInfo.fileName + "(" + picInfo.fileSize + ")");
            }

            return view;
        }


    }

    public static class GalleryItemViewHolder {
        public WebImageView galleryItemView;
        public TextView galleryTitle;

    }

    private int picHeight;

    //相册中图片
    private class AlbumAdapterItem extends ArrayAdapter<ImageInfoItem> {
        private List<ImageInfoItem> picfileList = new ArrayList<ImageInfoItem>();
        private LayoutInflater inflater;
        private int viewWidth = 0;
        private int viewHeight = 0;
        public int numColumns = 4;
        private Context context;

        public AlbumAdapterItem(Context context, int textViewResourceId, List<ImageInfoItem> objects) {
            super(context, textViewResourceId, objects);
            inflater = LayoutInflater.from(context);
            this.picfileList = objects;
            this.context = context;
        }

        @Override
        public int getCount() {
            if (picfileList != null && picfileList.size() > 0) {
                return picfileList.size() + numColumns;
            } else {
                return 0;
            }

        }


        @Override
        public ImageInfoItem getItem(int position) {
            return position < numColumns ? null : picfileList.get(position - numColumns);
        }

        @Override
        public long getItemId(int position) {
            return position < numColumns ? 0 : position - numColumns;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (position < numColumns) {
                if (convertView == null) {
                    convertView = new View(context);
                }
                convertView.setLayoutParams(new AbsListView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0));
                return convertView;
            }

            View view;
            if (convertView != null) {
                view = convertView;
            } else {
                int width = parent.getWidth();
                viewWidth = (int) (width / 4);
                viewHeight = viewWidth;
                picHeight = viewWidth;
                view = inflater.inflate(R.layout.album_pic_item, null);
                AlbumItemViewHolder holder = new AlbumItemViewHolder();
                holder.checkBox = (CheckableLinearLayout) view.findViewById(R.id.pic_item_chackbox_id);
                holder.picImage = (WebImageView) view.findViewById(R.id.pic_item_id);
                holder.picImageMask = (ImageView) view.findViewById(R.id.pic_item_mask);
                if (width > 0) {
                    AbsListView.LayoutParams layoutParams = new AbsListView.LayoutParams(viewWidth, viewHeight);
                    view.setLayoutParams(layoutParams);
                }

                view.setTag(R.id.tag_view, holder);
            }
            final ImageInfoItem item = getItem(position);

            if (item != null) {
                view.setTag(R.id.tag_info, item);
                AlbumItemViewHolder holder = (AlbumItemViewHolder) view.getTag(R.id.tag_view);

                if (!TextUtils.isEmpty(item.compressPath)) {
                    holder.picImage.setImageUrl(item.compressPath, true);
                } else {
                    Picasso.with(context).load(new File(item.filePath)).resize(200, 200).centerCrop().into(holder.picImage);
                    if (firstEnter) {
                        firstEnter = false;
                        picFileName = item.fileName;
                        picFilePath = item.filePath;
                        final String stPath = item.filePath;
//                        mCropImage.setImageUrl(item.filePath, true);
                        loadBitmap(picFilePath, mCropImage);
                        viewHolderOld = holder;
                    }
                    if (item.fileName.equals(picFileName)) {
                        holder.picImageMask.setVisibility(View.VISIBLE);
                        holder.picImageMask.setAlpha(0.8f);
                    } else {
                        holder.picImageMask.setVisibility(View.GONE);
                    }
//                    holder.picImage.setImageUrl(item.filePath, true);
//                    holder.picImage.setTag(item.filePath);
//                    loadBitmap(item.filePath,holder.picImage);
                }


                holder.checkBox.setChecked(item.iSelected);
            }
            return view;
        }


        @Override
        public boolean hasStableIds() {
            return true;
        }


        @Override
        public int getItemViewType(int position) {
            return (position < numColumns) ? 1 : 0;
        }


        @Override
        public int getViewTypeCount() {
            return 2;
        }

    }

    public static class AlbumItemViewHolder {
        public CheckableLinearLayout checkBox;
        public WebImageView picImage;
        public ImageView picImageMask;
    }

    private void startLoadPicTask(AsyncTask<Void, Void, Void> task) {
        task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    /**
     * loadpic task
     */
    protected AsyncTask<Void, Void, Void> task = new AsyncTask<Void, Void, Void>() {

        @Override
        protected void onPostExecute(Void result) {
            changePicAdapter(galleryList.get(0).folderPath, galleryList.get(0).fileName);
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
//			showLoadingProgress();
        }

        @Override
        protected Void doInBackground(Void... params) {
            loadPic();
            return null;
        }
    };

    private void loadPic() {
        imageInfos = new HashMap<String, ArrayList<ImageInfoItem>>();
        galleryList = new ArrayList<ImageInfoItem>();
        File file = null;
        Cursor query = picQuery();
        if (query != null) {
            ImageInfoItem info = null;
            ImageInfoItem galleryInfo = null;
            ArrayList<ImageInfoItem> list = null;

            while (query.moveToNext()) {
                info = new ImageInfoItem();
                int idIndex = query.getColumnIndex("_id");
                int sizeIndex = query.getColumnIndex("_size");
                int dataIndex = query.getColumnIndex("_data");
                int displayNameIndex = query.getColumnIndex("_display_name");
                int dateIndex = query.getColumnIndex("date_added");
                int folderIndex = query.getColumnIndex("bucket_display_name");
                int orientationIndex = query.getColumnIndex("orientation");
                if (dataIndex != -1) {
                    info.filePath = query.getString(dataIndex);
                }

                if (sizeIndex != -1) {
                    info.fileSize = query.getInt(sizeIndex);
                }
                if (displayNameIndex != -1) {
                    info.fileName = query.getString(displayNameIndex);
                }
                if (idIndex != -1) {
                    info.dbId = query.getLong(idIndex);
                }
                if (orientationIndex != -1) {
                    info.orientation = query.getInt(orientationIndex);
                }
                String galleryName = null;
                if (folderIndex != -1) {
                    galleryName = query.getString(folderIndex);
                }
                file = new File(info.filePath);
                if (file.getParent() != null) {
                    info.folderPath = file.getParent();
                } else {
                    info.folderPath = file.getName();
                }
                if (file.exists() && file.length() > 0) {
                    info.ModifiedDate = file.lastModified();
                    if (imageInfos.containsKey(info.folderPath)) {
                        list = imageInfos.get(info.folderPath);
                    } else {
                        // 创建galleryInfo
                        galleryInfo = new ImageInfoItem();
                        if (galleryName != null) {
                            galleryInfo.fileName = galleryName;
                        } else {
                            galleryInfo.fileName = file.getName();
                        }
                        File parentFile = file.getParentFile();
                        if (parentFile != null) {
                            galleryInfo.ModifiedDate = parentFile.lastModified();
                        } else {
                            galleryInfo.ModifiedDate = file.lastModified();
                        }

                        galleryInfo.folderPath = info.folderPath;
                        galleryInfo.filePath = info.filePath;
                        galleryInfo.dbId = info.dbId;
                        galleryList.add(galleryInfo);
                        list = new ArrayList<ImageInfoItem>();
                        imageInfos.put(info.folderPath, list);
                    }

                    //查看哪些是已经选中的
//                    if(transferPic != null && transferPic.size() > 0){
//                        for(int i =0;i<transferPic.size();i++){
//                            PublicProductPicItem item = transferPic.get(i);
//                            if(item.fromWhere == PublicProductPicItem.FROM_ALBUM){
//                                if(albumIndexInPicList == null){
//                                    albumIndexInPicList = new ArrayList<Integer>();
//                                }
//                                albumIndexInPicList.add(i);
//                            }
//                            if(item.fromWhere == PublicProductPicItem.FROM_ALBUM && !item.hasCompared && TextUtils.equals(item.path, info.filePath)){
//                                info.iSelected = true;
//                                item.hasCompared = true;
//                                selectedPic.add(info);
//                                break;
//                            }
//                        }
//                    }

                    list.add(info);
                }
            }
            query.close();

            int gallerySize = imageInfos.size();
            sortArray = new int[gallerySize];
            for (int i = 0; i < gallerySize; i++) {
                sortArray[i] = -1;
            }
            // 排序
            Collection<ArrayList<ImageInfoItem>> values = imageInfos.values();
            mSortHelper.setSortMethod(FileSortHelper.SortMethod.date);
            for (ArrayList<ImageInfoItem> currInfos : values) {
                Collections.sort(currInfos, mSortHelper.getComparator());
            }

            int cameraIndex = -1;
            int size = galleryList.size();
            ImageInfoItem gInfo = null;
            // 相册初始化
            for (int i = 0; i < size; i++) {
                gInfo = galleryList.get(i);
                ArrayList<ImageInfoItem> glist = imageInfos.get(gInfo.folderPath);
                ImageInfoItem mediaFileInfo = glist.get(0);
                gInfo.fileSize = glist.size();
                gInfo.filePath = mediaFileInfo.filePath;
                gInfo.folderPath = mediaFileInfo.folderPath;
                gInfo.dbId = mediaFileInfo.dbId;

            }
            //查找系统默认相册index
            for (int i = 0; i < size; i++) {
                gInfo = galleryList.get(i);
                if (gInfo.filePath != null) {
                    if (gInfo.folderPath.toLowerCase().contains(this.cameraPath)) {
                        isSysCameraPathHasPic = true;
                        cameraIndex = i;
                    }
                }
            }
            //交换系统相册位置为第一位
            if (cameraIndex != -1 && cameraIndex != 0) {
                Collections.swap(galleryList, cameraIndex, 0);
            }

        }
    }

    /**
     * 系统相册地址
     */
    private final String cameraPath = "/dcim/camera";
    private boolean isSysCameraPathHasPic = false;

    private Cursor picQuery() {
        Uri uri = MediaStore.Images.Media.getContentUri("external");

        if (uri == null) {
            return null;
        }
        Cursor query = null;
        try {
            query = this.getContentResolver().query(uri, null, null, null, null);
        } catch (Exception ex) {
            //可能会出现IllegalStateException问题，原因不明
        }
        return query;
    }

    public void sendResult(ImageInfoItem picFileInfo) {
        Intent intent = new Intent();
        String filePath = picFileInfo.filePath;
        String fileName = picFileInfo.fileName;
        String folderPath = picFileInfo.folderPath;
        if (picFileInfo != null) {
            intent.putExtra("filePath", picFileInfo.filePath);
            intent.putExtra("fileName", picFileInfo.fileName);
            intent.putExtra("folderPath", picFileInfo.folderPath);
            setResult(RESULT_OK, intent);
        }


        finish();
    }

    public void addBitmapToMemoryCache(String key, Bitmap bitmap) {
        if (getBitmapFromMemCache(key) == null) {
            mMemoryCache.put(key, bitmap);
        }
    }

    public Bitmap getBitmapFromMemCache(String key) {
        return mMemoryCache.get(key);
    }

    public void loadBitmap(String mStrPath, WebImageView imageView) {
        Bitmap bitmap = getBitmapFromMemCache(mStrPath);
        if (bitmap != null) {
            imageView.setImageBitmap(bitmap);
        } else {
//            imageView.setImageResource(R.drawable.ic_launcher);
//            BitmapWorkerTask bitmapWorkerTask = new BitmapWorkerTask();
//            taskCollection.add(bitmapWorkerTask);
//            bitmapWorkerTask.execute(mStrPath);

        }
    }

    public void loadBitmap(String mStrPath, CropImageView imageView) {
        Bitmap bitmap = getBitmapFromMemCache(mStrPath);
        if (bitmap != null) {
            imageView.setCropBitmapView(bitmap);
//            imageView.setImageBitmap(bitmap);
        } else {
//            imageView.setImageResource(R.drawable.ic_launcher);
            BitmapWorkerTask bitmapWorkerTask = new BitmapWorkerTask(imageView);
            taskCollection.add(bitmapWorkerTask);
            bitmapWorkerTask.execute(mStrPath);

        }
    }

    /**
     * 取消所有正在下载或等待下载的任务。
     */
    public void cancelAllTasks() {
        if (taskCollection != null) {
            for (BitmapWorkerTask task : taskCollection) {
                task.cancel(false);
            }
        }
    }

    class BitmapWorkerTask extends AsyncTask<String, Void, Bitmap> {
        public BitmapWorkerTask(CropImageView iv) {
            imageView1 = iv;
        }

        String strFilePath;
        WebImageView imageView;
        CropImageView imageView1;

        @Override
        protected void onPostExecute(Bitmap bitmap) {
            super.onPostExecute(bitmap);
            imageView = (WebImageView) mPicsGridView.findViewWithTag(strFilePath);
            if (bitmap != null && mCropImage != null) {
                imageView1.setCropBitmapView(bitmap);
//                imageView1.setImageBitmap(bitmap);

            }
//            if (bitmap != null && imageView != null) {
//                imageView.setImageBitmap(bitmap);
//            }
            taskCollection.remove(this);
        }

        // 在后台加载图片。
        @Override
        protected Bitmap doInBackground(String... params) {
            strFilePath = params[0];
//            final Bitmap bitmap = getimage(strFilePath);
            final Bitmap bitmap = decodeSampledBitmapFromResource(strFilePath, 1500, 1500);
            addBitmapToMemoryCache(strFilePath, bitmap);
            return bitmap;
        }
    }

    public static Bitmap decodeSampledBitmapFromResource(String strPath,
                                                         int reqWidth, int reqHeight) {
        // 第一次解析将inJustDecodeBounds设置为true，来获取图片大小
        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
//        BitmapFactory.decodeFile(strPath, options);
        BitmapFactory.decodeFile(strPath, options); //获取尺寸信息
//        BitmapFactory.decodeResource(res, resId, options);
        // 调用上面定义的方法计算inSampleSize值
        options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight);
        // 使用获取到的inSampleSize值再次解析图片
        options.inJustDecodeBounds = false;
        Bitmap bitmap = BitmapFactory.decodeFile(strPath, options);
        bitmap = reviewPicRotate(bitmap, strPath);
        return bitmap;
    }

    public static int calculateInSampleSize(BitmapFactory.Options options,
                                            int reqWidth, int reqHeight) {
        // 源图片的高度和宽度
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;
        if (height > reqHeight || width > reqWidth) {
            // 计算出实际宽高和目标宽高的比率
            final int heightRatio = Math.round((float) height / (float) reqHeight);
            final int widthRatio = Math.round((float) width / (float) reqWidth);
            // 选择宽和高中最小的比率作为inSampleSize的值，这样可以保证最终图片的宽和高
            // 一定都会大于等于目标的宽和高。
            inSampleSize = heightRatio < widthRatio ? heightRatio : widthRatio;
        }
        return inSampleSize;
    }

    public static final float DISPLAY_WIDTH = 200;
    public static final float DISPLAY_HEIGHT = 200;

    /**
     * 从path中获取图片信息
     *
     * @param path
     * @return
     */
    private Bitmap decodeBitmap(String path) {
        BitmapFactory.Options op = new BitmapFactory.Options();
        //inJustDecodeBounds
        //If set to true, the decoder will return null (no bitmap), but the out…
        op.inJustDecodeBounds = true;
        Bitmap bmp = BitmapFactory.decodeFile(path, op); //获取尺寸信息
        //获取比例大小
        int wRatio = (int) Math.ceil(op.outWidth / DISPLAY_WIDTH);
        int hRatio = (int) Math.ceil(op.outHeight / DISPLAY_HEIGHT);
        //如果超出指定大小，则缩小相应的比例
        if (wRatio > 1 && hRatio > 1) {
            if (wRatio > hRatio) {
                op.inSampleSize = wRatio;
            } else {
                op.inSampleSize = hRatio;
            }
        }
        op.inJustDecodeBounds = false;
        bmp = BitmapFactory.decodeFile(path, op);
        return bmp;
    }

    private Bitmap getimage(String srcPath) {
        BitmapFactory.Options newOpts = new BitmapFactory.Options();
        //开始读入图片，此时把options.inJustDecodeBounds 设回true了
        newOpts.inJustDecodeBounds = true;
        Bitmap bitmap = BitmapFactory.decodeFile(srcPath, newOpts);//此时返回bm为空

        newOpts.inJustDecodeBounds = false;
        int w = newOpts.outWidth;
        int h = newOpts.outHeight;
        //现在主流手机比较多是800*480分辨率，所以高和宽我们设置为
        float hh = 1200f;//这里设置高度为800f
        float ww = 1000f;//这里设置宽度为480f
        //缩放比。由于是固定比例缩放，只用高或者宽其中一个数据进行计算即可
        int be = 1;//be=1表示不缩放
        if (w > h && w > ww) {//如果宽度大的话根据宽度固定大小缩放
            be = (int) (newOpts.outWidth / ww);
        } else if (w < h && h > hh) {//如果高度高的话根据宽度固定大小缩放
            be = (int) (newOpts.outHeight / hh);
        }
        if (be <= 0)
            be = 1;
        newOpts.inSampleSize = be;//设置缩放比例
        //重新读入图片，注意此时已经把options.inJustDecodeBounds 设回false了
        bitmap = BitmapFactory.decodeFile(srcPath, newOpts);
        bitmap = reviewPicRotate(bitmap, srcPath);
        return compressImage(bitmap);//压缩好比例大小后再进行质量压缩
    }

    private Bitmap compressImage(Bitmap image) {

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        image.compress(Bitmap.CompressFormat.JPEG, 100, baos);//质量压缩方法，这里100表示不压缩，把压缩后的数据存放到baos中
        int options = 100;
        while (baos.toByteArray().length / 1024 > 100) {    //循环判断如果压缩后图片是否大于100kb,大于继续压缩
            baos.reset();//重置baos即清空baos
            image.compress(Bitmap.CompressFormat.JPEG, options, baos);//这里压缩options%，把压缩后的数据存放到baos中
            options -= 10;//每次都减少10
        }
        ByteArrayInputStream isBm = new ByteArrayInputStream(baos.toByteArray());//把压缩后的数据baos存放到ByteArrayInputStream中
        Bitmap bitmap = BitmapFactory.decodeStream(isBm, null, null);//把ByteArrayInputStream数据生成图片
        return bitmap;
    }

    /**
     * 获取图片文件的信息，是否旋转了90度，如果是则反转
     *
     * @param bitmap 需要旋转的图片
     * @param path   图片的路径
     */
    public static Bitmap reviewPicRotate(Bitmap bitmap, String path) {
        int degree = getPicRotate(path);
        if (degree != 0) {
            Matrix m = new Matrix();
            int width = bitmap.getWidth();
            int height = bitmap.getHeight();
            m.setRotate(degree); // 旋转angle度
            bitmap = Bitmap.createBitmap(bitmap, 0, 0, width, height, m, true);// 从新生成图片
        }
        return bitmap;
    }

    /**
     * 读取图片文件旋转的角度
     *
     * @param path 图片绝对路径
     * @return 图片旋转的角度
     */
    public static int getPicRotate(String path) {
        int degree = 0;
        try {
            ExifInterface exifInterface = new ExifInterface(path);
            int orientation = exifInterface.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
            switch (orientation) {
                case ExifInterface.ORIENTATION_ROTATE_90:
                    degree = 90;
                    break;
                case ExifInterface.ORIENTATION_ROTATE_180:
                    degree = 180;
                    break;
                case ExifInterface.ORIENTATION_ROTATE_270:
                    degree = 270;
                    break;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return degree;
    }

    @Override
    public void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (mMemoryCache != null) {
            mMemoryCache.evictAll();
        }

    }

    private void scrollPosition(int po) {
        (new Handler()).postDelayed(new Runnable() {
            @Override
            public void run() {
//            mPicsGridView.setSelection(po);
                mPicsGridView.smoothScrollToPosition(po);
            }
        }, 300);
    }

    private void savePic(Bitmap bitmap,String stPath){
        BaseStorageUtils.storeCapturedImageFile(bitmap, stPath);
    }


}
