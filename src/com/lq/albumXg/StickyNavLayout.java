package com.lq.albumXg;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import android.widget.GridView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.OverScroller;

public class StickyNavLayout extends LinearLayout {

    private View mTop, tabLayout;
    private View mNav;
    private GridView mViewPager;
    private LinearLayout list_grid;
    private int mTopViewHeight;
    private ListView mInnerScrollView;
    private boolean isTopHidden = false;

    private OverScroller mScroller;
    private VelocityTracker mVelocityTracker;
    private int mTouchSlop;
    private int mMaximumVelocity, mMinimumVelocity;

    private float mLastY;
    private boolean mDragging;

    private boolean isInControl = false;
    private boolean isClickOne = false;
    private int mScrollY = 0;

    public StickyNavLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        setOrientation(LinearLayout.VERTICAL);

        mScroller = new OverScroller(context);
        mTouchSlop = ViewConfiguration.get(context).getScaledTouchSlop();
        mMaximumVelocity = ViewConfiguration.get(context)
                .getScaledMaximumFlingVelocity();
        mMinimumVelocity = ViewConfiguration.get(context)
                .getScaledMinimumFlingVelocity();

    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();

        mTop = findViewById(R.id.cropImageView);
        mNav = findViewById(R.id.tv_layout);
        tabLayout = findViewById(R.id.tab_layout);
        mInnerScrollView = (ListView) findViewById(R.id.layout_listview);
        View view = findViewById(R.id.layout_grid);
        if (!(view instanceof GridView)) {
            throw new RuntimeException(
                    "id_stickynavlayout_viewpager show used by ViewPager !");
        }
        mViewPager = (GridView) view;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);


        mViewPager.getLayoutParams().height = getMeasuredHeight() - (mTop.getMeasuredHeight() +
                tabLayout.getMeasuredHeight() + mNav.getMeasuredHeight() * 2) + getScrollY();
        mInnerScrollView.getLayoutParams().height = getMeasuredHeight() - (mTop.getMeasuredHeight() +
                tabLayout.getMeasuredHeight() + mNav.getMeasuredHeight()) + getScrollY();
        heightMeasureSpec = getMeasuredHeight();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        mTopViewHeight = mTop.getMeasuredHeight();
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        int action = ev.getAction();
        float y = ev.getY();

        switch (action) {
            case MotionEvent.ACTION_DOWN:
                mLastY = y;
                break;
            case MotionEvent.ACTION_MOVE:
                float dy = y - mLastY;
                if (mInnerScrollView.getVisibility() == View.VISIBLE) {
                    View c = mInnerScrollView.getChildAt(mInnerScrollView.getFirstVisiblePosition());
                    if (!isInControl&&c != null && c.getTop() == 0 && isTopHidden && dy > 0) {
                        isInControl = true;
                        ev.setAction(MotionEvent.ACTION_CANCEL);
                        MotionEvent ev2 = MotionEvent.obtain(ev);
                        dispatchTouchEvent(ev);
                        ev2.setAction(MotionEvent.ACTION_DOWN);
                        return dispatchTouchEvent(ev2);
                    }
                }else if(mViewPager.getVisibility()== View.VISIBLE){
                    View c = mViewPager.getChildAt(mViewPager.getFirstVisiblePosition());
                    if (!isInControl&&c != null && c.getTop() == 0 && isTopHidden && dy > 0) {
                        isInControl = true;
                        ev.setAction(MotionEvent.ACTION_CANCEL);
                        MotionEvent ev2 = MotionEvent.obtain(ev);
                        dispatchTouchEvent(ev);
                        ev2.setAction(MotionEvent.ACTION_DOWN);
                        return dispatchTouchEvent(ev2);
                    }
                }
                break;
        }
        return super.dispatchTouchEvent(ev);
    }

    /**
     *
     */
    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        final int action = ev.getAction();
        float y = ev.getY();
        switch (action) {
            case MotionEvent.ACTION_DOWN:
                mLastY = y;
                break;
            case MotionEvent.ACTION_MOVE:
                float dy = y - mLastY;
                if (Math.abs(dy) > mTouchSlop) {
                    mDragging = true;
                  //  topView隐藏 && 下拉，则拦截
                    if (mInnerScrollView.getVisibility() == View.VISIBLE) {
                        View c = mInnerScrollView.getChildAt(mInnerScrollView.getFirstVisiblePosition());
                        if ( c != null && c.getTop() == 0 && isTopHidden && dy > 0) {
                            initVelocityTrackerIfNotExists();
                            mVelocityTracker.addMovement(ev);
                            mLastY = y;
                            return true;
                        }
                    }
                    else if(mViewPager.getVisibility()== View.VISIBLE){
                        View c = mViewPager.getChildAt(mViewPager.getFirstVisiblePosition());
                        if ( c != null && c.getTop() == 0 && isTopHidden && dy > 0) {
                            initVelocityTrackerIfNotExists();
                            mVelocityTracker.addMovement(ev);
                            mLastY = y;
                            return true;
                        }
                    }

                }
                break;
            case MotionEvent.ACTION_CANCEL:
            case MotionEvent.ACTION_UP:
                mDragging = false;
                recycleVelocityTracker();
                break;
        }
        return super.onInterceptTouchEvent(ev);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        initVelocityTrackerIfNotExists();
        mVelocityTracker.addMovement(event);
        int action = event.getAction();
        float y = event.getY();

        switch (action) {
            case MotionEvent.ACTION_DOWN:
                if (!mScroller.isFinished())
                    mScroller.abortAnimation();
                mLastY = y;
//			if(y>mTopViewHeight){
//				scrollBy(0, mTopViewHeight);
//			}else if(y<mTopViewHeight){
//				scrollBy(0, -mTopViewHeight);
//			}
                return true;
            case MotionEvent.ACTION_MOVE:
                float dy = y - mLastY;


                if (!mDragging && Math.abs(dy) > mTouchSlop) {
                    mDragging = true;
                }
                if (mDragging) {
                    scrollBy(0, (int) -dy);

                    // 如果topView隐藏，且上滑动时，则改变当前事件为ACTION_DOWN
//                    Log.i("AAAD","onTouchEvent getScrollY()="+getScrollY()+",mTopViewHeight="+mTopViewHeight+",dy="+dy);
                    if (getScrollY() == mTopViewHeight && dy < 0) {
                        event.setAction(MotionEvent.ACTION_DOWN);
                        dispatchTouchEvent(event);
                        isInControl = false;
                    }
                }

                mLastY = y;
                break;
            case MotionEvent.ACTION_CANCEL:
                mDragging = false;
                recycleVelocityTracker();
                if (!mScroller.isFinished()) {
                    mScroller.abortAnimation();
                }
                break;
            case MotionEvent.ACTION_UP:
                if (!mDragging) {
                    if (getScrollY() == mTopViewHeight || getScrollY() == 0) {
                        if (y > mTopViewHeight) {
                            scrollBy(0, mTopViewHeight);
                        } else if (y < mTopViewHeight) {
                            scrollBy(0, -mTopViewHeight);
                        }
                        break;
                    }
                }

                mDragging = false;
                mVelocityTracker.computeCurrentVelocity(5000, mMaximumVelocity);
                int velocityY = (int) mVelocityTracker.getYVelocity();
//			if (Math.abs(velocityY) > mMinimumVelocity) {
//				fling(-velocityY);
//			}
                recycleVelocityTracker();


                if (y > mTopViewHeight) {
                    scrollBy(0, -mTopViewHeight);
                } else if (y < mTopViewHeight) {
                    scrollBy(0, mTopViewHeight);
                }
//                if (y > mTopViewHeight) {
//                    fling(-velocityY);
//                } else if (y < mTopViewHeight) {
//                    flingN(-velocityY);
//                }

                break;
        }

        return super.onTouchEvent(event);
    }

    public void fling(int velocityY) {
        mScroller.fling(0, getScrollY(), 0, velocityY, 0, 0, 0, mTopViewHeight);
        invalidate();
    }

    public void flingN(int velocityY) {
        mScroller.fling(0, getScrollY(), 0, velocityY, 0, 0, 0, -mTopViewHeight);
        invalidate();
    }

    @Override
    public void scrollTo(int x, int y) {
        if (y < 0) {
            y = 0;
        }
        if (y > mTopViewHeight) {
            y = mTopViewHeight;
        }
        if (y != getScrollY()) {
            super.scrollTo(x, y);
        }
        isTopHidden = getScrollY() == mTopViewHeight;
    }

    @Override
    public void computeScroll() {
        if (mScroller.computeScrollOffset()) {
            scrollTo(0, mScroller.getCurrY());
//			if(mScroller.getCurrY()<mTopViewHeight){
//				scrollTo(0, -mTopViewHeight);
//			}
// else{
//				scrollTo(0, mTopViewHeight);
//			}
            mScrollY = getScrollY();
        }
        invalidate();
        requestLayout();
        if (mScroller.isFinished()) {

        }

    }

    private void initVelocityTrackerIfNotExists() {
        if (mVelocityTracker == null) {
            mVelocityTracker = VelocityTracker.obtain();
        }
    }

    private void recycleVelocityTracker() {
        if (mVelocityTracker != null) {
            mVelocityTracker.recycle();
            mVelocityTracker = null;
        }
    }

}
