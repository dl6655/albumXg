package com.lq.albumXg;

import android.app.Fragment;
import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.ArrayAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import com.squareup.picasso.Picasso;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by dingli on 2015-9-28.
 */
public class PicGridFragment extends Fragment {
    private GridView layout_grid_fragment;
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view=inflater.inflate(R.layout.pic_grid_fragment, container, false);
        layout_grid_fragment=(GridView)view.findViewById(R.id.layout_grid_fragment);
        layout_grid_fragment.setNumColumns(4);
        return view;
    }

    public void setGridData(Context context, int textViewResourceId, List<ImageInfoItem> objects){
        layout_grid_fragment.setAdapter(new AlbumAdapterItem(context,textViewResourceId,objects));
    }
    //œ‡≤·÷–Õº∆¨
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
//                picHeight = viewWidth;
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
//                    if (firstEnter) {
//                        firstEnter = false;
//                        picFileName = item.fileName;
//                        picFilePath = item.filePath;
//                        final String stPath = item.filePath;
////                        mCropImage.setImageUrl(item.filePath, true);
//                        loadBitmap(picFilePath, mCropImage);
//                        viewHolderOld = holder;
//                    }
//                    if (item.fileName.equals(picFileName)) {
//                        holder.picImageMask.setVisibility(View.VISIBLE);
//                        holder.picImageMask.setAlpha(0.8f);
//                    } else {
//                        holder.picImageMask.setVisibility(View.GONE);
//                    }
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

}