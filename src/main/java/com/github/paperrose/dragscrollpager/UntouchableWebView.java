package com.github.paperrose.dragscrollpager;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.webkit.WebView;

/**
 * Created by Emil.Allakhverdiev on 14.02.2017.
 */
public class UntouchableWebView extends WebView implements LoadedView {
    public void setPageLoaded(boolean pageLoaded) {
        this.pageLoaded = pageLoaded;
    }

    private boolean pageLoaded = false;

    public UntouchableWebView(Context context) {
        super(context);
    }

    public UntouchableWebView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public UntouchableWebView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent e) {
        return true;
    }

    @Override
    public boolean onTouchEvent(MotionEvent e) {
        return false;
    }

    @Override
    public boolean isLoaded() {
        return pageLoaded;
    }
}
