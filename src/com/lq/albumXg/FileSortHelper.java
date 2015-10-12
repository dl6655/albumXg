package com.lq.albumXg;


import java.util.Comparator;
import java.util.HashMap;

public class FileSortHelper {

	public enum SortMethod {
		name, size, date, type, date_asc, size_asc
	}

	private SortMethod mSort;

	private HashMap<SortMethod, Comparator<ImageInfoItem>> mComparatorList = new HashMap<SortMethod, Comparator<ImageInfoItem>>();

	public FileSortHelper() {
		mSort = SortMethod.date;//按照时间排序
		mComparatorList.put(SortMethod.name, cmpName);
		mComparatorList.put(SortMethod.size, cmpSize);//从小到大
		mComparatorList.put(SortMethod.date, cmpDate);
		mComparatorList.put(SortMethod.date_asc, cmpDateAsc);
		mComparatorList.put(SortMethod.size_asc, cmpSizeAsc);//从大到小

		// mComparatorList.put(SortMethod.type, cmpType);
	}

	public void setSortMethod(SortMethod s) {
		mSort = s;
	}

	public SortMethod getSortMethod() {
		return mSort;
	}

	public Comparator<ImageInfoItem> getComparator() {
		return mComparatorList.get(mSort);
	}

	private abstract class FileComparator implements Comparator<ImageInfoItem> {

		@Override
		public int compare(ImageInfoItem object1, ImageInfoItem object2) {
			return doCompare(object1, object2);
		}

		protected abstract int doCompare(ImageInfoItem object1, ImageInfoItem object2);
	}

	private Comparator<ImageInfoItem> cmpName = new FileComparator() {
		@Override
		public int doCompare(ImageInfoItem object1, ImageInfoItem object2) {
			return object1.fileName.compareToIgnoreCase(object2.fileName);
		}
	};

	private Comparator<ImageInfoItem> cmpSize = new FileComparator() {
		@Override
		public int doCompare(ImageInfoItem object1, ImageInfoItem object2) {
			return longToCompareInt(object1.fileSize - object2.fileSize);
		}
	};
	private Comparator<ImageInfoItem> cmpSizeAsc = new FileComparator() {
		@Override
		public int doCompare(ImageInfoItem object1, ImageInfoItem object2) {
			return longToCompareInt(object2.fileSize - object1.fileSize);
		}
	};
	private Comparator<ImageInfoItem> cmpDate = new FileComparator() {
		@Override
		public int doCompare(ImageInfoItem object1, ImageInfoItem object2) {
			return longToCompareInt(object2.ModifiedDate - object1.ModifiedDate);
		}
	};
	private Comparator<ImageInfoItem> cmpDateAsc = new FileComparator() {
		@Override
		public int doCompare(ImageInfoItem object1, ImageInfoItem object2) {
			return longToCompareInt(object1.ModifiedDate - object2.ModifiedDate);
		}
	};

	private int longToCompareInt(long result) {
		return result > 0 ? 1 : (result < 0 ? -1 : 0);
	}

	// private Comparator cmpType = new FileComparator() {
	// @Override
	// public int doCompare(MediaFileInfo object1, MediaFileInfo object2) {
	// int result =
	// Utils.getExtFromFilename(object1.fileName).compareToIgnoreCase(Utils.getExtFromFilename(object2.fileName));
	// if (result != 0)
	// return result;
	//
	// return
	// Utils.getNameFromFilename(object1.fileName).compareToIgnoreCase(Utils.getNameFromFilename(object2.fileName));
	// }
	// };
}
