package com.github.paperrose.dragscrollpager;

import android.content.Context;
import android.os.Build;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;

/**
 * Created by Emil on 09.02.2017.
 */
public class SmartView extends RelativeLayout {
    private Context mContext;
    public FrameLayout dragger;
    public LoadedView content;
    public FrameLayout contentContainer;
    public int draggerHeight;
    public int draggerStatus;
    public boolean collapsed;

    public SmartView setDraggerTouchListener(OnTouchListener draggerTouchListener, OnTouchListener scrollTouchListener) {
        dragger.setOnTouchListener(draggerTouchListener);
        contentContainer.setOnTouchListener(scrollTouchListener);
        return this;
    }

    public void setTranslationYWithLimit(float y) {
        if ((y <= 0) || (collapsed && y > getHeight() - draggerHeight)) return;
        setTranslationY(y);
    }

    public void setContentTranslationY(float y) {
        if (y > 0) {
            LayoutParams lp = (LayoutParams) dragger.getLayoutParams();
            lp.setMargins(0, (int) Math.min(0, Math.max(-draggerHeight, y-draggerHeight)), 0, 0);
            dragger.setLayoutParams(lp);
            draggerStatus = (int) y;
        } else {
            if (draggerStatus != 0) {
                LayoutParams lp = (LayoutParams) dragger.getLayoutParams();
                lp.setMargins(0, -draggerHeight, 0, 0);
                dragger.setLayoutParams(lp);
                draggerStatus = 0;
            }
            ((View)content).setTranslationY(Math.min(Math.max(y, this.getHeight() - ((View)content).getHeight()), 0));
        }
    }

    public boolean hitTop() {
        return getContentTranslationY() >= draggerHeight;
    }

    public boolean hitBottom() {
        return (getContentTranslationY() <= this.getHeight() - ((View)content).getHeight()) || getTranslationY() < 0;
    }

    public void resetTranslation() {
        LayoutParams lp = (LayoutParams) dragger.getLayoutParams();
        lp.setMargins(0, 0, 0, 0);
        dragger.setLayoutParams(lp);
        draggerStatus = draggerHeight;
//        ((View)content).setTranslationY(0);
    }

    private int getViewSize( View view ) {
        int width = this.getWidth();
        view.measure(MeasureSpec.makeMeasureSpec(width, MeasureSpec.AT_MOST),
                MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED));

        int targetHeight = view.getMeasuredHeight();
        return targetHeight;
    }

    public float getContentTranslationY() {
        return draggerStatus + ((View)content).getTranslationY();
    }

    public boolean allowTranslation() {
        return hitTop();
    }

    public SmartView(Context context) {
        super(context);
        mContext = context;
        preInit();
    }

    public SmartView(Context context, AttributeSet attrs) {
        super(context, attrs);
        mContext = context;
        preInit();
    }

    public SmartView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        mContext = context;
        preInit();
    }

    public void preInit() {
        this.dragger = new FrameLayout(mContext);
        this.dragger.setId(R.id.smart_dragger);
        LayoutParams lp = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
        this.dragger.setLayoutParams(lp);

        contentContainer = new FrameLayout(mContext);
        contentContainer.setId(R.id.smart_content);
        contentContainer.setBackgroundColor(mContext.getResources().getColor(android.R.color.white));
        LayoutParams lpContent = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
        lpContent.addRule(RelativeLayout.BELOW, R.id.smart_dragger);
        contentContainer.setLayoutParams(lpContent);

        setLayoutParams(new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        this.addView(dragger);
        this.addView(contentContainer);
    }

    public SmartView setDragger(View dragView) {
        if (this.dragger.getChildAt(0) != null) {
            this.dragger.removeViewAt(0);
        }
        this.dragger.addView(dragView);
        ViewTreeObserver vto = dragger.getViewTreeObserver();
        vto.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                    dragger.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                } else {
                    dragger.getViewTreeObserver().removeGlobalOnLayoutListener(this);
                }
                FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams)dragger.getChildAt(0).getLayoutParams();
                draggerHeight = lp.height;
                draggerStatus = draggerHeight;
                dragger.getChildAt(0).setLayoutParams(lp);
            }
        });
        return this;
    }

    public void resizeContent() {
        ViewTreeObserver vto = contentContainer.getViewTreeObserver();
        vto.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                int contentSize = getViewSize(((View)content));
                if (contentSize == 0) return;
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                    contentContainer.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                } else {
                    contentContainer.getViewTreeObserver().removeGlobalOnLayoutListener(this);
                }
                FrameLayout.LayoutParams contentLp = (FrameLayout.LayoutParams)((View)content).getLayoutParams();
                contentLp.height = contentSize;
                ((View)content).setLayoutParams(contentLp);
            }
        });
        FrameLayout.LayoutParams contentLp = (FrameLayout.LayoutParams)((View)content).getLayoutParams();
        contentLp.height = FrameLayout.LayoutParams.WRAP_CONTENT;
        ((View)content).setLayoutParams(contentLp);
    }

    public SmartView setContent(LoadedView contentView) {
        if (this.contentContainer.getChildAt(0) != null) {
            this.contentContainer.removeViewAt(0);
        }
        contentContainer.addView((View)contentView);
        ViewTreeObserver vto = contentContainer.getViewTreeObserver();
        vto.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {

                content = (LoadedView)contentContainer.getChildAt(0);
                int contentSize = getViewSize(((View)content));
                if (contentSize == 0) return;
                if (content.isLoaded()) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                        contentContainer.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                    } else {
                        contentContainer.getViewTreeObserver().removeGlobalOnLayoutListener(this);
                    }
                }
                FrameLayout.LayoutParams contentLp = (FrameLayout.LayoutParams)((View)content).getLayoutParams();
                contentLp.height = contentSize;
                ((View)content).setLayoutParams(contentLp);
            }
        });

        return this;
    }
}
