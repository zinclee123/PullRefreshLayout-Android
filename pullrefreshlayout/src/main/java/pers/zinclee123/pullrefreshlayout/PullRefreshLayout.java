package pers.zinclee123.pullrefreshlayout;

import android.animation.Animator;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.support.v4.view.MotionEventCompat;
import android.support.v4.view.ViewCompat;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.widget.AbsListView;

/**
 * Created by liyanjin on 2017/5/23.
 */

public class PullRefreshLayout extends ViewGroup {

    private static final String TAG = PullRefreshLayout.class.getSimpleName();

    private static final int INVALID_POINTER = -1;
    private static final float DECELERATE_INTERPOLATION_FACTOR = 2f;
    private static final int ANIM_DURATION = 275;

    /**
     * 内部的目标可滚动的View,如ListView,RecyclerView
     */
    View mTargetView;

    /**
     * 顶部刷新View
     */
    View mRefreshView;

    /**
     * 底部加载View
     */
    View mLoadMoreView;

    int mActivePointerId = -1;

    float mInitialDownY;
    //每次Move的时候记录下，计算上次Move到这次Move的便宜，以便移动内容，这个是在onTouchEvent中记录的
    float mTouchLastMotionY = -1f;

    int mTouchSlop;

    boolean mInited = false;

    //是否正在恢复正常的位置
    boolean mIsReturningToCorrect = false;

    boolean mIsLoadingMore = false;

    boolean mIsRefreshing = false;

    boolean mIsBeingDragged = false;

    boolean mNotify = true;

    DecelerateInterpolator mDecelerateInterpolator;

    OnRefreshListener mOnRefreshListen;

    OnLoadMoreListener mOnLoadMoreListener;

    OnRefreshViewShowListener mOnRefreshViewShowListener;

    OnLoadMoreViewShowListener mOnLoadMoreViewShowListener;

    Animator.AnimatorListener mAnimResumeListener = new Animator.AnimatorListener() {
        @Override
        public void onAnimationStart(Animator animation) {
            mIsReturningToCorrect = true;
        }

        @Override
        public void onAnimationEnd(Animator animation) {
            mIsReturningToCorrect = false;
        }

        @Override
        public void onAnimationCancel(Animator animation) {

        }

        @Override
        public void onAnimationRepeat(Animator animation) {

        }
    };


    Animator.AnimatorListener mAnimRefreshListener = new Animator.AnimatorListener() {
        @Override
        public void onAnimationStart(Animator animation) {

        }

        @Override
        public void onAnimationEnd(Animator animation) {
            if (mIsRefreshing) {
                // Make sure the progress view is fully visible
                if (mNotify) {
                    if (mOnRefreshListen != null) {
                        mOnRefreshListen.onRefresh();
                    }
                }
            } else {
                reset();
            }
        }

        @Override
        public void onAnimationCancel(Animator animation) {

        }

        @Override
        public void onAnimationRepeat(Animator animation) {

        }
    };

    Animator.AnimatorListener mAnimLoadMoreListener = new Animator.AnimatorListener() {
        @Override
        public void onAnimationStart(Animator animation) {

        }

        @Override
        public void onAnimationEnd(Animator animation) {
            if (mIsLoadingMore) {
                // Make sure the progress view is fully visible
                if (mNotify) {
                    if (mOnLoadMoreListener != null) {
                        mOnLoadMoreListener.onLoadMore();
                    }
                }
            } else {
                reset();
            }
        }

        @Override
        public void onAnimationCancel(Animator animation) {

        }

        @Override
        public void onAnimationRepeat(Animator animation) {

        }
    };

    void reset() {
        //全部移动到对应位置即可
        scrollTo(0, mRefreshView.getMeasuredHeight());
    }

    public PullRefreshLayout(Context context, AttributeSet attrs) {
        super(context, attrs);


        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.PullRefreshLayout);
        final int refreshLayoutId = a.getResourceId(R.styleable.PullRefreshLayout_prl_refreshLayout, R.layout.prl_layout_refresh);
        final int loadMoreLayoutId = a.getResourceId(R.styleable.PullRefreshLayout_prl_loadMoreLayout, R.layout.prl_layout_load_more);
        a.recycle();

        mDecelerateInterpolator = new DecelerateInterpolator(DECELERATE_INTERPOLATION_FACTOR);
        mTouchSlop = ViewConfiguration.get(context).getScaledTouchSlop();


        mRefreshView = LayoutInflater.from(context).inflate(refreshLayoutId, this, false);
        addView(mRefreshView);

        mLoadMoreView = LayoutInflater.from(context).inflate(loadMoreLayoutId, this, false);
        addView(mLoadMoreView);

        setChildrenDrawingOrderEnabled(true);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {

        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

        ensureTarget();
        if (mTargetView == null)
            return;

        mTargetView.measure(widthMeasureSpec, heightMeasureSpec);

        mLoadMoreView.measure(widthMeasureSpec, MeasureSpec.makeMeasureSpec(mLoadMoreView.getLayoutParams().height, MeasureSpec.EXACTLY));
        mRefreshView.measure(widthMeasureSpec, MeasureSpec.makeMeasureSpec(mRefreshView.getLayoutParams().height, MeasureSpec.EXACTLY));

        if (!mInited) {
            mInited = true;
            setScrollY(mRefreshView.getMeasuredHeight());
        }
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {

        ensureTarget();
        if (mTargetView == null) {
            return;
        }

        mRefreshView.layout(
                l,
                0,
                r,
                mRefreshView.getMeasuredHeight());

        mTargetView.layout(
                l,
                mRefreshView.getMeasuredHeight(),
                r,
                mRefreshView.getMeasuredHeight() + mTargetView.getMeasuredHeight());


        mLoadMoreView.layout(
                l,
                mRefreshView.getMeasuredHeight() + mTargetView.getMeasuredHeight(),
                r,
                mRefreshView.getMeasuredHeight() + mTargetView.getMeasuredHeight() + mLoadMoreView.getMeasuredHeight());

    }


    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        ensureTarget();

        final int action = MotionEventCompat.getActionMasked(ev);
        int pointerIndex;

        if (mIsReturningToCorrect && action == MotionEvent.ACTION_DOWN) {
            mIsReturningToCorrect = false;
        }

        if (!isEnabled()) {
            return false;
        }

        //这几种状态直接吃掉触摸事件，视图不响应
        if (mIsReturningToCorrect
                || mIsLoadingMore || mIsRefreshing) {
            // Fail fast if we're not in a state where a swipe is possible
            return true;
        }

        switch (action) {
            case MotionEvent.ACTION_DOWN:
                //手指按下时，强制恢复到初始位置
                mActivePointerId = ev.getPointerId(0);
                mIsBeingDragged = false;

                pointerIndex = ev.findPointerIndex(mActivePointerId);
                if (pointerIndex < 0) {
                    return false;
                }
                mInitialDownY = ev.getY(pointerIndex);
                mTouchLastMotionY = mInitialDownY;
                break;

            case MotionEvent.ACTION_MOVE:

                //这里面主要做拖拽检测，如果是拖拽，拦截下来到该视图的OnTouchEvent中去消费
                if (mActivePointerId == INVALID_POINTER) {
                    Log.e(TAG, "Got ACTION_MOVE event but don't have an active pointer id.");
                    return false;
                }
                pointerIndex = ev.findPointerIndex(mActivePointerId);
                if (pointerIndex < 0) {
                    return false;
                }
                final float y = ev.getY(pointerIndex);
                final float yDiff = y - mInitialDownY;

                if (yDiff > 0 && (canTargetScrollUp() || mOnRefreshListen == null)) { //
                    return false;
                }

                if (yDiff < 0 && (canTargetScrollDown() || mOnLoadMoreListener == null)) { //
                    return false;
                }


                if (Math.abs(yDiff) >= mTouchSlop && !mIsBeingDragged) {
                    mIsBeingDragged = true;
                }
                break;
            case MotionEventCompat.ACTION_POINTER_UP:
                onSecondaryPointerUp(ev);
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                mIsBeingDragged = false;
                mActivePointerId = INVALID_POINTER;
                break;
        }

        return mIsBeingDragged;
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {

        final int action = MotionEventCompat.getActionMasked(ev);
        int pointerIndex = -1;

        if (mIsReturningToCorrect && action == MotionEvent.ACTION_DOWN) {
            mIsReturningToCorrect = false;
        }

        if (!isEnabled()
                || mIsReturningToCorrect
                || mIsLoadingMore || mIsRefreshing) {
            // Fail fast if we're not in a state where a swipe is possible
            return false;
        }

        switch (action) {
            case MotionEvent.ACTION_DOWN:
                mActivePointerId = ev.getPointerId(0);
                mIsBeingDragged = false;
                break;

            case MotionEvent.ACTION_MOVE: {

                pointerIndex = ev.findPointerIndex(mActivePointerId);
                if (pointerIndex < 0) {
                    Log.e(TAG, "Got ACTION_MOVE event but have an invalid active pointer id.");
                    return false;
                }

                final float y = ev.getY(pointerIndex);
                final float yDiff = y - mInitialDownY;


                //todo yDiff+canTargetScrollUp+canTargetScrollDown的判断只是粗判断，
                // 表明这次滑动的整体趋势是上拉还是下拉但是会出现这种情况，用户在屏幕底部小幅度的下拉，
                // 把顶部的刷新视图拽出来之后又大幅上拉，这种情况下面的判断会失效，
                // 所以在下面调用contentMove之前做了一次弥补判断，如果弥补的判断调用targetView的scroll方法后，
                // 就不会在整体滚动视图，而此时move的这个操作已经被当前view拦截，
                // 目前没找到合适的方法传递到targetView中所以在下面的各个判断中都有插入让targetView滚动的代码，后面优化逻辑，现在有点混乱……

                if (yDiff > 0 && canTargetScrollUp()) {


                    int diff = (int) (y - mTouchLastMotionY);
                    mTouchLastMotionY = y;
                    mTargetView.scrollBy(0, -diff);
                    return false;
                }

                if (yDiff < 0 && canTargetScrollDown()) {
                    int diff = (int) (y - mTouchLastMotionY);
                    mTouchLastMotionY = y;
                    mTargetView.scrollBy(0, -diff);
                    return false;
                }

                //下面是拖拽检测
                if (Math.abs(yDiff) >= mTouchSlop && !mIsBeingDragged) {
                    mIsBeingDragged = true;
                }

                if (mIsBeingDragged) {
                    //计算这一次move和上一次move直接的距离
                    int diff = (int) (y - mTouchLastMotionY);

                    //如果diff > 0则表示此时的操作是下拉操作
                    //canTargetScrollUp()返回真则表示targetView还可以向下拉
                    //getScrollY() <= mRefreshView.getMeasuredHeight() 表示顶部刷新视图即将要展示出来
                    //合起来表示处理 下拉操作，下拉马上要展示下拉刷新视图的时候发现视图还可以继续下拉，那就继续下拉
                    if (diff > 0 && canTargetScrollUp() && getScrollY() <= mRefreshView.getMeasuredHeight()) {
                        mTouchLastMotionY = y;
                        mTargetView.scrollBy(0, -diff);
                        scrollTo(0, mRefreshView.getMeasuredHeight());
                        return true;
                    }

                    //如果diff < 0则表示此时的操作是上拉操作
                    //canTargetScrollDown()返回真则表示targetView还可以向上拉
                    //getScrollY() >= mRefreshView.getMeasuredHeight() 表示底部刷新视图即将要展示出来
                    //合起来表示处理 上拉操作，上拉马上要展示上拉加载视图的时候发现视图还可以继续上拉，那就继续下拉
                    if (diff < 0 && canTargetScrollDown() && getScrollY() >= mRefreshView.getMeasuredHeight()) {
                        mTouchLastMotionY = y;
                        mTargetView.scrollBy(0, -diff);
                        scrollTo(0, mRefreshView.getMeasuredHeight());
                        return true;
                    }

                    contentMove(diff);
                }
                mTouchLastMotionY = y;
                break;
            }
            case MotionEventCompat.ACTION_POINTER_DOWN: {
                pointerIndex = MotionEventCompat.getActionIndex(ev);
                if (pointerIndex < 0) {
                    Log.e(TAG,
                            "Got ACTION_POINTER_DOWN event but have an invalid action index.");
                    return false;
                }
                mActivePointerId = ev.getPointerId(pointerIndex);
                break;
            }

            case MotionEventCompat.ACTION_POINTER_UP:
                onSecondaryPointerUp(ev);
                break;

            case MotionEvent.ACTION_UP: {
                pointerIndex = ev.findPointerIndex(mActivePointerId);
                if (pointerIndex < 0) {
                    Log.e(TAG, "Got ACTION_UP event but don't have an active pointer id.");
                    return false;
                }

                if (mIsBeingDragged) {
                    if (getScrollY() <= 0) {
                        //进入刷新状态
                        setRefreshing(true);
                    } else if (getScrollY() >= mRefreshView.getMeasuredHeight() + mLoadMoreView.getMeasuredHeight()) {
                        setLoadingMore(true);
                    }
                }

                if (mRefreshView.getMeasuredHeight() != getScrollY() && !mIsRefreshing & !mIsLoadingMore) {
                    animateToCorrectPosition();
                }
                mActivePointerId = INVALID_POINTER;
                return false;
            }
            case MotionEvent.ACTION_CANCEL:
                return false;
        }

        return true;
    }

    public boolean isLoadingMore() {
        return mIsLoadingMore;
    }

    public void setLoadingMore(boolean loadingMore) {
        ensureTarget();
        if (mTargetView == null)
            return;

        mIsLoadingMore = loadingMore;
        if (mIsLoadingMore) {
            if (mIsRefreshing) {
                mIsRefreshing = false;
            }
            animateToLoadingPosition();
        } else {
            animateToCorrectPosition();
        }
    }

    public boolean isRefreshing() {
        return mIsRefreshing;
    }

    public void setRefreshing(boolean refreshing) {
        ensureTarget();
        if (mTargetView == null)
            return;

        mIsRefreshing = refreshing;
        if (mIsRefreshing) {
            if (mIsLoadingMore) {
                mIsLoadingMore = false;
            }
            animateToRefreshingPosition();
        } else {
            animateToCorrectPosition();
        }
    }

    private void animateToCorrectPosition() {
        ObjectAnimator animator = ObjectAnimator.ofInt(this, "scrollY", getScrollY(), mRefreshView.getMeasuredHeight());
        animator.setInterpolator(mDecelerateInterpolator);
        animator.setDuration(ANIM_DURATION);
        animator.addListener(mAnimResumeListener);
        addUpdateListener(animator).start();
    }

    private void animateToLoadingPosition() {
        ObjectAnimator animator = ObjectAnimator.ofInt(this, "scrollY", getScrollY(), mRefreshView.getMeasuredHeight() + mLoadMoreView.getMeasuredHeight());
        animator.setInterpolator(mDecelerateInterpolator);
        animator.setDuration(ANIM_DURATION);
        animator.addListener(mAnimLoadMoreListener);
        addUpdateListener(animator).start();
    }

    private void animateToRefreshingPosition() {
        ObjectAnimator animator = ObjectAnimator.ofInt(this, "scrollY", getScrollY(), 0);
        animator.setInterpolator(mDecelerateInterpolator);
        animator.setDuration(ANIM_DURATION);
        animator.addListener(mAnimRefreshListener);
        addUpdateListener(animator).start();
    }

    public ObjectAnimator addUpdateListener(ObjectAnimator animator) {
        animator.removeAllUpdateListeners();
        animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                int targetY = (int) animation.getAnimatedValue();
                processTargetViewMoved(targetY);
            }
        });
        return animator;
    }

    public void setOnRefreshListener(OnRefreshListener mOnRefreshListen) {
        this.mOnRefreshListen = mOnRefreshListen;
        if (this.mOnRefreshListen != null) {
            this.mRefreshView.setVisibility(VISIBLE);
        } else {
            this.mRefreshView.setVisibility(INVISIBLE);
        }
    }

    public void setOnLoadMoreListener(OnLoadMoreListener mOnLoadMoreListener) {
        this.mOnLoadMoreListener = mOnLoadMoreListener;
        if (this.mOnLoadMoreListener != null) {
            this.mLoadMoreView.setVisibility(VISIBLE);
        } else {
            this.mLoadMoreView.setVisibility(INVISIBLE);
        }
    }

    public void setOnRefreshViewShowListener(OnRefreshViewShowListener onRefreshViewShowListener) {
        this.mOnRefreshViewShowListener = onRefreshViewShowListener;
    }

    public void setOnLoadMoreViewShowListener(OnLoadMoreViewShowListener onLoadMoreViewShowListener) {
        this.mOnLoadMoreViewShowListener = onLoadMoreViewShowListener;
    }


    public View getRefreshView() {
        return mRefreshView;
    }

    public View getLoadMoreView() {
        return mLoadMoreView;
    }

    public void setRefreshView(View mRefreshView) {
        if (this.mRefreshView != null) {
            removeView(this.mRefreshView);
        }
        this.mRefreshView = mRefreshView;
        this.mInited = false;
        addView(this.mRefreshView);
    }

    public void setLoadMoreView(View mLoadMoreView) {
        if (this.mLoadMoreView != null) {
            removeView(this.mLoadMoreView);
        }
        this.mLoadMoreView = mLoadMoreView;
        this.mInited = false;
        addView(this.mLoadMoreView);
    }

    private boolean canTargetScrollUp() {
        //以下这段代码是直接粘贴的SwipeRefreshLayout里的
        if (android.os.Build.VERSION.SDK_INT < 14) {
            if (mTargetView instanceof AbsListView) {
                final AbsListView absListView = (AbsListView) mTargetView;
                return absListView.getChildCount() > 0
                        && (absListView.getFirstVisiblePosition() > 0 || absListView.getChildAt(0)
                        .getTop() < absListView.getPaddingTop());
            } else {
                return ViewCompat.canScrollVertically(mTargetView, -1) || mTargetView.getScrollY() > 0;
            }
        } else {
            return ViewCompat.canScrollVertically(mTargetView, -1);
        }
    }

    private boolean canTargetScrollDown() {
        if (android.os.Build.VERSION.SDK_INT < 14) {
            if (mTargetView instanceof AbsListView) {
                final AbsListView absListView = (AbsListView) mTargetView;
                int count = absListView.getChildCount();
                return count > 0
                        && (absListView.getLastVisiblePosition() == (count - 1) || absListView.getChildAt(count - 1)
                        .getBottom() == absListView.getPaddingBottom());
            } else {
                return ViewCompat.canScrollVertically(mTargetView, 1) || mTargetView.getScrollY() + mTargetView.getHeight() >= mTargetView.getMeasuredHeight();
            }
        } else {
            return ViewCompat.canScrollVertically(mTargetView, 1);
        }
    }

    private void contentMove(int offset) {
        ensureTarget();
        if (mTargetView == null) {
            return;
        }

        offset = (int) (offset * 0.75);

        //移出范围后，滑动的时候减速
        if (offset > 0) {
            int destY = getScrollY() - offset;
            if (destY <= 0) {
                offset = (int) (offset * (mRefreshView.getMeasuredHeight() / (float) (mRefreshView.getMeasuredHeight() - destY)));
            }
        } else {
            int destY = getScrollY() - offset;
            if (destY > mRefreshView.getMeasuredHeight() + mLoadMoreView.getMeasuredHeight()) {
                offset = (int) (offset * (mLoadMoreView.getMeasuredHeight() / (float) (destY - mRefreshView.getMeasuredHeight())));
            }
        }


        setScrollY(getScrollY() - offset);
        processTargetViewMoved(getScrollY());
    }

    private void processTargetViewMoved(int scrollY) {
        if (scrollY < mRefreshView.getMeasuredHeight()) {
            if (mOnRefreshViewShowListener != null) {
                float percent = (mRefreshView.getMeasuredHeight() - scrollY) / (float) mRefreshView.getMeasuredHeight();
                mOnRefreshViewShowListener.onRefreshViewShow(percent);
            }
        } else {
            if (mOnLoadMoreViewShowListener != null) {
                float percent = (scrollY - mRefreshView.getMeasuredHeight()) / (float) mLoadMoreView.getMeasuredHeight();
                mOnLoadMoreViewShowListener.onLoadMoreViewShow(percent);
            }
        }
    }

    private void ensureTarget() {
        if (mTargetView != null)
            return;
        if (getChildCount() > 0) {
            for (int i = 0; i < getChildCount(); i++) {
                View child = getChildAt(i);
                if (child != mRefreshView && child != mLoadMoreView) {
                    mTargetView = child;
                }
            }
        }
    }

    ////todo 这块是拷贝的SwipeRefreshLayout,有时间看下具体作用，目测应该是处理多个触控点的
    private void onSecondaryPointerUp(MotionEvent ev) {
        final int pointerIndex = MotionEventCompat.getActionIndex(ev);
        final int pointerId = ev.getPointerId(pointerIndex);
        if (pointerId == mActivePointerId) {
            final int newPointerIndex = pointerIndex == 0 ? 1 : 0;
            mActivePointerId = ev.getPointerId(newPointerIndex);
        }
    }

    public interface OnRefreshViewShowListener {

        public void onRefreshViewShow(float percent);
    }

    public interface OnLoadMoreViewShowListener {

        public void onLoadMoreViewShow(float percent);
    }

    public interface OnRefreshListener {

        public void onRefresh();

    }

    public interface OnLoadMoreListener {

        public void onLoadMore();

    }
}
