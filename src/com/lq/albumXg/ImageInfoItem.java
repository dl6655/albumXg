package com.lq.albumXg;

import android.os.Parcel;
import android.os.Parcelable;

import java.io.Serializable;

/**
 * Created by dingli on 2015-9-10.
 */
public class ImageInfoItem implements Serializable,Parcelable {
    public String folderPath = "";

    public String filePath = "";

    public String fileName = "";

    public long fileSize;

    public int count;

    public long ModifiedDate;

    public boolean iSelected;

    public long duration;


    public boolean isHidden;

    public int orientation;


    public long dbId;

    /**
     * ͼƬ�ĳ��Ϳ� ���������ж��Ƿ���Ҫѹ��ͼƬ��
     */
    public int picWidth;
    public int picHeight;

    /**
     * ������¼ѹ����ĵ�ַ��
     */
    public String compressPath;

    public int rotateDegree = 0;

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Parcelable.Creator<ImageInfoItem> CREATOR = new Parcelable.Creator<ImageInfoItem>() {
        //��дCreator

        @Override
        public ImageInfoItem createFromParcel(Parcel source) {
            ImageInfoItem info = new ImageInfoItem();
            info.filePath = source.readString();
            info.fileName = source.readString();
            info.fileSize = source.readLong();
            info.count = source.readInt();
            info.ModifiedDate = source.readLong();
            info.dbId = source.readLong();
            return info;

        }

        @Override
        public ImageInfoItem[] newArray(int size) {
            return null;
        }
    };

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(filePath);
        dest.writeString(fileName);
        dest.writeLong(fileSize);
        dest.writeInt(count);
        dest.writeLong(ModifiedDate);
        dest.writeLong(dbId);
    }
}
