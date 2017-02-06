package lite.widget.pull;

import android.animation.ObjectAnimator;
import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.ViewPropertyAnimator;
import android.widget.TextView;

import java.util.Calendar;

public class PullLayout extends ViewGroup {

    public static final int MAX_OFFSET_ANIMATION_DURATION = 700;

    private static final int INVALID_POINTER = -1;

    public final static int STATE_NORMAL = 0;
    public final static int STATE_PULLING_HEADER = 1;
    public final static int STATE_PULLING_FOOTER = 2;
    public final static int STATE_LOADING_HEADER = 3;
    public final static int STATE_LOADING_FOOTER = 4;

    private final static int PULLING = 0;
    private final static int READY = 1;
    private final static int LOADING = 2;
    private final static int COMPLETE = 3;

    private View mTarget;
    private View mHeaderView, mFooterView;
    private TextView mHeaderHint, mHeaderTime, mFooterHint;
    private View mHeaderProgress, mFooterProgress;
    private View mHeaderArrow;
    private int mHeaderHeight, mFooterHeight;

    private boolean headerPullable = true;
    private boolean footerPullable = true;
    private int mState = STATE_NORMAL;
    private int mHeaderStatus = PULLING, mFooterStatus = PULLING;
    private ViewPropertyAnimator mArrowAnimator;
    private ObjectAnimator mAnimator;

    private int mTouchSlop;
    private int mActivePointerId;
    private float mInitialMotionY;

    private OnRefreshListener mListener;


    public PullLayout(Context context) {
        this(context, null);
    }

    public PullLayout(Context context, AttributeSet attrs) {
        super(context, attrs);

        mTouchSlop = ViewConfiguration.get(context).getScaledTouchSlop();

        LayoutInflater inflater = LayoutInflater.from(context);
        mHeaderView = inflater.inflate(R.layout.xlistview_header, this, false);
        mFooterView = inflater.inflate(R.layout.xlistview_footer, this, false);

        addView(mHeaderView);
        addView(mFooterView);
        mAnimator = ObjectAnimator.ofInt(this, "scrollY", 0);
        mAnimator.setDuration(MAX_OFFSET_ANIMATION_DURATION);
        mHeaderHint = (TextView) mHeaderView.findViewById(R.id.xlistview_header_hint_textview);
        mHeaderTime = (TextView) mHeaderView.findViewById(R.id.xlistview_header_time);
        mHeaderProgress = mHeaderView.findViewById(R.id.xlistview_header_progressbar);
        mHeaderArrow = mHeaderView.findViewById(R.id.xlistview_header_arrow);
        mFooterHint = (TextView) mFooterView.findViewById(R.id.xlistview_footer_hint_textview);
        mFooterProgress = mFooterView.findViewById(R.id.xlistview_footer_progressbar);
        mArrowAnimator = mHeaderArrow.animate();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

        ensureTarget();
        if (mTarget == null)
            return;

        widthMeasureSpec = MeasureSpec.makeMeasureSpec(getMeasuredWidth() - getPaddingRight() - getPaddingLeft(), MeasureSpec.EXACTLY);
        heightMeasureSpec = MeasureSpec.makeMeasureSpec(getMeasuredHeight() - getPaddingTop() - getPaddingBottom(), MeasureSpec.EXACTLY);
        mTarget.measure(widthMeasureSpec, heightMeasureSpec);
        heightMeasureSpec = MeasureSpec.makeMeasureSpec(getMeasuredHeight(), MeasureSpec.AT_MOST);
        mHeaderView.measure(widthMeasureSpec, heightMeasureSpec);
        mFooterView.measure(widthMeasureSpec, heightMeasureSpec);
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {

        ensureTarget();
        if (mTarget == null)
            return;

        int height = getMeasuredHeight();
        int width = getMeasuredWidth();
        int left = getPaddingLeft();
        int top = getPaddingTop();
        int right = getPaddingRight();
        int bottom = getPaddingBottom();
        mHeaderHeight = mHeaderView.getMeasuredHeight();
        mFooterHeight = mFooterView.getMeasuredHeight();

        mTarget.layout(left, top, left + width - right, top + height - bottom);
        mHeaderView.layout(0, -mHeaderHeight, width, 0);
        mFooterView.layout(0, height, width, height + mFooterHeight);
    }

    private void ensureTarget() {
        if (mTarget != null)
            return;
        if (getChildCount() > 0) {
            for (int i = 0; i < getChildCount(); i++) {
                View child = getChildAt(i);
                if (child != mHeaderView) {
                    mTarget = child;
                }
            }
        }
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {

        if (!isEnabled()) {
            return false;
        }

        final int action = ev.getActionMasked();

        switch (action) {
            case MotionEvent.ACTION_DOWN:
                mActivePointerId = ev.getPointerId(0);
                final float initialMotionY = getMotionEventY(ev, mActivePointerId);
                if (initialMotionY == -1) {
                    return false;
                }
                mInitialMotionY = initialMotionY;
                setupState();
                break;
            case MotionEvent.ACTION_MOVE:
                if (mActivePointerId == INVALID_POINTER) {
                    return false;
                }
                final float y = getMotionEventY(ev, mActivePointerId);
                if (y == -1) {
                    return false;
                }
                final float yDiff = y - mInitialMotionY;
                if (yDiff > mTouchSlop && headerPullable && !mTarget.canScrollVertically(-1)) {
                    if(mState != STATE_PULLING_HEADER) {
                        mState = STATE_PULLING_HEADER;
                        setupHeader(PULLING);
                    }
                    mInitialMotionY = y;
                }
                else if(yDiff < -mTouchSlop && footerPullable && !mTarget.canScrollVertically(1)) {
                    if(mState != STATE_PULLING_FOOTER) {
                        mState = STATE_PULLING_FOOTER;
                        setupFooter(PULLING);
                    }
                    mInitialMotionY = y;
                }
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                mActivePointerId = INVALID_POINTER;
                break;
            case MotionEvent.ACTION_POINTER_UP:
                onSecondaryPointerUp(ev);
                break;
        }

        return mState == STATE_PULLING_HEADER || mState == STATE_PULLING_FOOTER;
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {

        if (mState == STATE_LOADING_HEADER || mState == STATE_LOADING_FOOTER) {
            return true;
        }

        final int action = ev.getActionMasked();

        switch (action) {
            case MotionEvent.ACTION_MOVE: {
                final int pointerIndex = ev.findPointerIndex(mActivePointerId);
                if (pointerIndex < 0) {
                    return false;
                }

                final float y = ev.getY(pointerIndex);
                final float yDiff = y - mInitialMotionY;
                if(mState == STATE_PULLING_HEADER) {
                    if(yDiff < 0) {
                        setScrollY(0);
                        return false;
                    }
                    if(yDiff > mHeaderHeight)
                        setupHeader(READY);
                    else
                        setupHeader(PULLING);
                    setScrollY(-(int)yDiff);
                }
                else if(mState == STATE_PULLING_FOOTER) {
                    if(yDiff > 0) {
                        setScrollY(0);
                        return false;
                    }
                    if(-yDiff > mFooterHeight)
                        setupFooter(READY);
                    else
                        setupFooter(PULLING);
                    setScrollY(-(int)yDiff);
                }
                break;
            }
            case MotionEvent.ACTION_POINTER_DOWN:
                final int index = ev.getActionIndex();
                mActivePointerId = ev.getPointerId(index);
                break;
            case MotionEvent.ACTION_POINTER_UP:
                onSecondaryPointerUp(ev);
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL: {
                if (mActivePointerId == INVALID_POINTER) {
                    return false;
                }
                final int pointerIndex = ev.findPointerIndex(mActivePointerId);
                final float y = ev.getY(pointerIndex);
                final float yDiff = y - mInitialMotionY;
                if(mState == STATE_PULLING_HEADER) {
                    if(yDiff > mHeaderHeight) {
                        notifyPullEvent(true);
                        mState = STATE_LOADING_HEADER;
                        setupHeader(LOADING);
                    }
                    else {
                        mState = STATE_NORMAL;
                    }
                }
                else if(mState == STATE_PULLING_FOOTER) {
                    if(-yDiff > mFooterHeight) {
                        notifyPullEvent(false);
                        mState = STATE_LOADING_FOOTER;
                        setupFooter(LOADING);
                    }
                    else {
                        mState = STATE_NORMAL;
                    }
                }

                animateScrollY();
                mActivePointerId = INVALID_POINTER;
                return false;
            }
        }

        return true;
    }

    private void notifyPullEvent(boolean headerEvent) {
        if(mListener != null)
            mListener.onPull(headerEvent);
    }

    private void animateScrollY() {
        switch (mState) {
            case STATE_LOADING_HEADER:
                if(mAnimator.isStarted())
                    mAnimator.cancel();
                mAnimator.setIntValues(-mHeaderHeight);
                mAnimator.start();
                break;
            case STATE_LOADING_FOOTER:
                if(mAnimator.isStarted())
                    mAnimator.cancel();
                mAnimator.setIntValues(mFooterHeight);
                mAnimator.start();
                break;
            case STATE_NORMAL:
                if(mAnimator.isStarted())
                    mAnimator.cancel();
                mAnimator.setIntValues(0);
                mAnimator.start();
                break;
        }
    }

    public int getState() {
        return mState;
    }


    public void stopLoading() {
        switch (mState) {
            case STATE_LOADING_HEADER:
                setupHeader(COMPLETE);
                String time = String.format("%1$tm/%1$td %1$tH:%1$tM:%1$tS",
                        Calendar.getInstance());
                mHeaderTime.setText(time);
                mState = STATE_NORMAL;
                animateScrollY();
                break;
            case STATE_LOADING_FOOTER:
                setupFooter(COMPLETE);
                mState = STATE_NORMAL;
                animateScrollY();
                break;
        }
    }

    private void onSecondaryPointerUp(MotionEvent ev) {
        final int pointerIndex = ev.getActionIndex();
        final int pointerId = ev.getPointerId(pointerIndex);
        if (pointerId == mActivePointerId) {
            final int newPointerIndex = pointerIndex == 0 ? 1 : 0;
            mActivePointerId = ev.getPointerId(newPointerIndex);
        }
    }

    private float getMotionEventY(MotionEvent ev, int activePointerId) {
        final int index = ev.findPointerIndex(activePointerId);
        if (index < 0) {
            return -1;
        }
        return ev.getY(index);
    }

    private void setupState() {
        if(mState == STATE_LOADING_FOOTER || mState == STATE_LOADING_HEADER)
            return;
        int sy = getScrollY();
        if(mAnimator.isStarted()) {
            mAnimator.cancel();
        }
        if(sy < 0 && headerPullable) {
            mState = STATE_PULLING_HEADER;
            if(-sy < mHeaderHeight)
                setupHeader(PULLING);
            else
                setupHeader(READY);
            mInitialMotionY = sy + mInitialMotionY;
        }
        else if (sy > 0 && footerPullable) {
            mState = STATE_PULLING_FOOTER;
            if(sy < mFooterHeight)
                setupFooter(PULLING);
            else
                setupFooter(READY);
            mInitialMotionY = sy + mInitialMotionY;
        }
    }

    private void setupHeader(int status) {
        if(status == mHeaderStatus)
            return;
        switch (status) {
            case PULLING:
                mHeaderHint.setText(R.string.xlistview_header_hint_normal);
                mHeaderProgress.setVisibility(INVISIBLE);
                mHeaderArrow.setVisibility(VISIBLE);
                mArrowAnimator.rotation(0).start();
                break;
            case READY:
                mHeaderHint.setText(R.string.xlistview_header_hint_ready);
                mHeaderProgress.setVisibility(INVISIBLE);
                mHeaderArrow.setVisibility(VISIBLE);
                mArrowAnimator.rotation(180).start();
                break;
            case LOADING:
                mHeaderHint.setText(R.string.xlistview_header_hint_loading);
                mHeaderProgress.setVisibility(VISIBLE);
                mHeaderArrow.setVisibility(INVISIBLE);
                mArrowAnimator.cancel();
                mHeaderArrow.setRotation(180);
                break;
            case COMPLETE:
                mHeaderHint.setText(R.string.xlistview_header_hint_complete);
                mHeaderProgress.setVisibility(INVISIBLE);
                mHeaderArrow.setVisibility(VISIBLE);
                mArrowAnimator.cancel();
                mHeaderArrow.setRotation(0);
                break;
        }
        mHeaderStatus = status;
    }

    private void setupFooter(int status) {
        if(status == mFooterStatus)
            return;
        switch (status) {
            case PULLING:
                mFooterHint.setText(R.string.xlistview_footer_hint_normal);
                mFooterProgress.setVisibility(INVISIBLE);
                break;
            case READY:
                mFooterHint.setText(R.string.xlistview_footer_hint_ready);
                mFooterProgress.setVisibility(INVISIBLE);
                break;
            case LOADING:
                mFooterHint.setText(R.string.xlistview_footer_loading);
                mFooterProgress.setVisibility(VISIBLE);
                break;
            case COMPLETE:
                mFooterHint.setText(R.string.xlistview_footer_complete);
                mFooterProgress.setVisibility(INVISIBLE);
                break;
        }
        mFooterStatus = status;
    }

    public boolean isFooterPullable() {
        return footerPullable;
    }

    public void setFooterPullable(boolean footerPullable) {
        this.footerPullable = footerPullable;
    }

    public boolean isHeaderPullable() {
        return headerPullable;
    }

    public void setHeaderPullable(boolean headerPullable) {
        this.headerPullable = headerPullable;
    }

    public void setOnRefreshListener(OnRefreshListener listener) {
        mListener = listener;
    }

    public interface OnRefreshListener {
        void onPull(boolean header);
    }
}

