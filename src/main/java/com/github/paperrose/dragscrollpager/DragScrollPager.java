package com.github.paperrose.dragscrollpager;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.widget.RelativeLayout;

import java.util.ArrayList;

/**
 * Created by Emil.Allakhverdiev on 14.02.2017.
 */
public class DragScrollPager extends RelativeLayout implements View.OnTouchListener {
    DragScrollPagerAdapter pagerAdapter;

    ArrayList<OnContentTapListener> tapListeners = new ArrayList<>();
    ArrayList<OnContentScrollListener> scrollListeners = new ArrayList<>();

    public void addOnContentTapListener(OnContentTapListener listener) {
        tapListeners.add(listener);
    }

    public void addOnContentScrollListener(OnContentScrollListener listener) {
        scrollListeners.add(listener);
    }

    public DragScrollPager(Context context) {
        super(context);
        setOnTouchListener(touchListener);
        preInit();
    }

    public DragScrollPager(Context context, AttributeSet attrs) {
        super(context, attrs);
        setOnTouchListener(touchListener);
        preInit();
    }

    public interface OnContentTapListener {
        void onTap();
    }

    public interface OnContentScrollListener {
        void onScroll(float distance);
    }

    public DragScrollPager(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        setOnTouchListener(touchListener);
        preInit();
    }

    public void setCurrentViews(int position) {
        smartViewFront.setDragger(pagerAdapter.instantiateDragger(this, position));
        smartViewFront.setContent(pagerAdapter.instantiateContent(this, position));
        if (position < pagerAdapter.getCount() - 1) {
            smartViewBottom.setDragger(pagerAdapter.instantiateBottomDragger(this, position + 1));
            smartViewBottom.setContent(pagerAdapter.instantiateContent(this, position + 1));
        }
        if (position > 0) {
            smartViewBack.setDragger(pagerAdapter.instantiateDragger(this, position - 1));
            smartViewBack.setContent(pagerAdapter.instantiateContent(this, position - 1));
        }
    }


    private OnTouchListener touchListener = new OnTouchListener() {
        @Override
        public boolean onTouch(View view, MotionEvent event) {
            final float distance;
            long time;
            ValueAnimator animator;
            if ((currentMotion == null) || (currentMotion.motionMode == MotionMode.NONE))
                return true;
            switch (event.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                    currentMotion.startY = event.getY();
                    currentMotion.lastY = currentMotion.startY - currentMotion.align;
                    currentMotion.lastMoveTime = System.currentTimeMillis();
                    break;
                case MotionEvent.ACTION_MOVE:
                    currentMotion.hasMove = true;
                    long lastMoveTime = System.currentTimeMillis();
                    time = lastMoveTime - currentMotion.lastMoveTime;
                    float lastY = event.getY() - currentMotion.align;
                    distance = lastY - currentMotion.lastY;
                    currentMotion.lastDistance = distance;
                    currentMotion.velocity = distance / time;
                    switch (currentMotion.motionMode) {
                        case MOVE:
                            if (currentItem == 0 && currentMotion.smartView.getTranslationY() >= 0 && !currentMotion.smartView.collapsed) {
                                currentMotion.smartView.setTranslationY(0);
                                return true;
                            }
                            currentMotion.smartView
                                    .setTranslationYWithLimit(currentMotion.smartView.getTranslationY() + distance);
                            break;
                        case SCROLL:

                            if (Math.abs(distance) > 2) {
                                if (currentMotion.smartView.hitBottom() && (currentItem != pagerAdapter.getCount() - 1) && currentMotion.smartView.getTranslationY() <= 0) {
                                    smartViewBottom.setTranslationY(currentMotion.smartView.getHeight() + Math.max(-currentMotion.smartView.draggerHeight, currentMotion.smartView.getTranslationY() + distance));
                                    currentMotion.smartView.setTranslationY(Math.max(-currentMotion.smartView.draggerHeight, currentMotion.smartView.getTranslationY() + distance));
                                } else {
                                    currentMotion.smartView.setTranslationY(0);
                                    currentMotion.smartView.setContentTranslationY(currentMotion.smartView.getContentTranslationY() + distance);
                                    for (OnContentScrollListener listener : scrollListeners) {
                                        listener.onScroll(distance);
                                    }
                                }
                            }
                            if (currentMotion.smartView.hitTop() && distance > 0) {
                                currentMotion.motionMode = MotionMode.MOVE;
                                return true;
                            }
                            break;
                    }
                    currentMotion.lastMoveTime = lastMoveTime;
                    currentMotion.lastY = lastY;
                    break;
                case MotionEvent.ACTION_UP:
                    final MotionItem lastMotion = currentMotion;
                    currentMotion = null;
                    switch (lastMotion.motionMode) {
                        case SCROLL:
                            if (!lastMotion.hasMove) {
                                for (OnContentTapListener listener : tapListeners) {
                                    listener.onTap();
                                }
                                break;
                            }

                            animator = ValueAnimator.ofFloat(Math.abs(lastMotion.lastDistance), 0);
                            animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                                @Override
                                public void onAnimationUpdate(ValueAnimator animation) {
                                    if (Math.abs(lastMotion.lastDistance) > 2) {
                                        if (lastMotion.smartView.hitBottom() && (currentItem != pagerAdapter.getCount() - 1) && lastMotion.smartView.getTranslationY() <= 0) {
                                            smartViewBottom.setTranslationY(lastMotion.smartView.getHeight() +
                                                    Math.max(-lastMotion.smartView.draggerHeight, lastMotion.smartView.getTranslationY() +
                                                            Math.signum(lastMotion.lastDistance) * (float) animation.getAnimatedValue()));
                                            lastMotion.smartView.setTranslationY(Math.max(-lastMotion.smartView.draggerHeight,
                                                    lastMotion.smartView.getTranslationY() +
                                                    Math.signum(lastMotion.lastDistance) * (float) animation.getAnimatedValue()));
                                        } else {
                                            lastMotion.smartView.setTranslationY(0);
                                            lastMotion.smartView.setContentTranslationY(lastMotion.smartView.getContentTranslationY() +
                                                    Math.signum(lastMotion.lastDistance) * (float) animation.getAnimatedValue());
                                            for (OnContentScrollListener listener : scrollListeners) {
                                                listener.onScroll(Math.signum(lastMotion.lastDistance) * (float) animation.getAnimatedValue());
                                            }
                                        }
                                    }
                                }
                            });

                            animator.setDuration(1000);
                            animator.start();
                            break;
                        case MOVE:
                            float startY = event.getY() - lastMotion.align;
                            float endY;
                            Animator.AnimatorListener listener = new AnimatorListenerAdapter() {
                                @Override
                                public void onAnimationEnd(Animator animation) {
                                    super.onAnimationEnd(animation);
                                }
                            };
                            ValueAnimator.AnimatorUpdateListener updateListener = null;
                            if (lastMotion.smartView.collapsed) {
                                if (currentItem == pagerAdapter.getCount() - 1) {
                                    return true;
                                }
                                if (!lastMotion.hasMove || lastMotion.velocity < -3.5 || lastMotion.startY - event.getY() > lastMotion.smartView.getHeight() / 2) {
                                    endY = 0f;
                                    listener = new AnimatorListenerAdapter() {
                                        @Override
                                        public void onAnimationEnd(Animator animation) {
                                            super.onAnimationEnd(animation);
                                            smartViewFront.setTranslationY(0);
                                            smartViewFront.setContentTranslationY(0);
                                            lastMotion.smartView.collapsed = false;
                                            setNextItem(lastMotion);
                                        }
                                    };
                                    updateListener = new ValueAnimator.AnimatorUpdateListener() {
                                        @Override
                                        public void onAnimationUpdate(ValueAnimator animation) {
                                            float coeff = (float)(animation.getCurrentPlayTime())/animation.getDuration();
                                            lastMotion.smartView.setContentTranslationY((1 - coeff)*lastMotion.smartView.draggerHeight);
                                        }
                                    };


                                } else {
                                    endY = lastMotion.smartView.getHeight() + smartViewFront.getTranslationY();
                                }
                            } else {
                                if (currentItem == 0 || !lastMotion.hasMove) {
                                    break;
                                }
                                if (lastMotion.velocity > 3.5 || event.getY() - lastMotion.startY > lastMotion.smartView.getHeight() / 2) {
                                    endY = lastMotion.smartView.getHeight();
                                    listener = new AnimatorListenerAdapter() {
                                        @Override
                                        public void onAnimationEnd(Animator animation) {
                                            super.onAnimationEnd(animation);
                                            lastMotion.smartView.collapsed = true;
                                            setPrevItem(lastMotion);
                                        }
                                    };
                                } else {
                                    endY = 0f;
                                }

                            }
                            distance = startY - endY;
                            animator = ValueAnimator.ofFloat(startY, endY);
                            animator.addListener(listener);
                            animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                                @Override
                                public void onAnimationUpdate(ValueAnimator animation) {
                                    lastMotion.smartView.setTranslationY((Float) animation.getAnimatedValue());
                                }
                            });
                            if (updateListener != null) {
                                animator.addUpdateListener(updateListener);
                            }
                            animator.setInterpolator(new DecelerateInterpolator());
                            animator.setDuration((lastMotion.hasMove && lastMotion.velocity != 0 &&
                                    (int) Math.abs(distance / lastMotion.velocity) < 500) ?
                                    (int) Math.abs(distance / lastMotion.velocity) : 500);
                            animator.start();

                            break;
                    }
                    return true;
            }
            return true;
        }
    };

    public void preInit() {
        smartViewFront = new SmartView(getContext());
        smartViewBack = new SmartView(getContext());
        smartViewBottom = new SmartView(getContext());
        LayoutParams params = new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        smartViewFront.setLayoutParams(params);
        smartViewBack.setLayoutParams(params);
        smartViewBottom.setLayoutParams(params);
        addView(smartViewBack);
        addView(smartViewBottom);
        addView(smartViewFront);
        smartViewBottom.collapsed = true;
        smartViewFront.collapsed = false;
        smartViewBack.collapsed = false;
        smartViewFront.setDraggerTouchListener(this, this);
        smartViewBack.setDraggerTouchListener(this, this);
        smartViewBottom.setDraggerTouchListener(this, this);
    }

    public void setPagerAdapter(DragScrollPagerAdapter pagerAdapter) {
        this.pagerAdapter = pagerAdapter;
        currentItem = 0;
        setCurrentViews(currentItem);
    }

    public void setCurrentItem(int position) {
        if (position < 0 || position > pagerAdapter.getCount() - 1 || position == currentItem)
            return;
        if (currentItem - position == 1) {
            setPrevItem(null);
        } else if (position - currentItem == 1) {
            setNextItem(null);
        } else {
            setCurrentViews(position);
        }
        currentItem = position;
    }

    public int getCurrentItem() {
        return currentItem;
    }

    private void setNextItem(MotionItem lastMotion) {
        if (currentItem == pagerAdapter.getCount() - 1) return;
        SmartView tempView = smartViewBottom;
        smartViewBottom = smartViewBack;
        smartViewBack = smartViewFront;
        smartViewFront = lastMotion != null ? lastMotion.smartView : tempView;
        smartViewFront.bringToFront();
        smartViewBottom.setTranslationY(smartViewBottom.getHeight());
        smartViewBottom.bringToFront();
        smartViewBottom.collapsed = true;
        smartViewFront.collapsed = false;
        smartViewBack.collapsed = false;
        smartViewBack.resetTranslation();
        if (lastMotion != null)
            smartViewBottom.resetTranslation();
        currentItem++;
        smartViewFront.setDragger(pagerAdapter.instantiateDragger(this, currentItem));
        if (currentItem != pagerAdapter.getCount() - 1) {
            smartViewBottom.setContent(pagerAdapter.instantiateContent(this, currentItem + 1));
            smartViewBottom.setDragger(pagerAdapter.instantiateBottomDragger(this, currentItem + 1));
        }

    }

    private void setPrevItem(MotionItem lastMotion) {
        if (currentItem == 0) return;
        SmartView tempView = smartViewFront;
        smartViewFront = smartViewBack;
        smartViewBack = smartViewBottom;
        smartViewBottom = lastMotion != null ? lastMotion.smartView : tempView;
        smartViewFront.bringToFront();
        smartViewBottom.bringToFront();
        smartViewBack.setTranslationY(0);
        smartViewBottom.collapsed = true;
        smartViewFront.collapsed = false;
        smartViewBack.collapsed = false;
        if (lastMotion != null)
            smartViewBack.resetTranslation();
        smartViewBottom.resetTranslation();
        currentItem--;
        if (currentItem != 0) {
            smartViewBack.setContent(pagerAdapter.instantiateContent(this, currentItem - 1));
            smartViewBack.setDragger(pagerAdapter.instantiateDragger(this, currentItem - 1));
        }
        smartViewBottom.setDragger(pagerAdapter.instantiateBottomDragger(this, currentItem + 1));
    }

    private int currentItem;
    public SmartView smartViewFront;
    public SmartView smartViewBack;
    public SmartView smartViewBottom;
    private MotionItem currentMotion;

    class MotionItem {
        MotionMode motionMode;
        SmartView smartView;
        float align;
        float startY;
        float lastY;
        float lastDistance;
        long lastMoveTime;
        float velocity;
        boolean hasMove;
    }


    private enum MotionMode {
        SCROLL,
        MOVE,
        NONE
    }


    @Override
    public boolean onTouch(View view, MotionEvent event) {
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                if (currentMotion != null) {
                    if (currentMotion.motionMode == MotionMode.NONE)
                        currentMotion = null;
                    return false;
                }
                currentMotion = new MotionItem();
                currentMotion.smartView = (SmartView) view.getParent();
                if (currentMotion.smartView == smartViewFront) {
                    smartViewBottom.setTranslationY(smartViewFront.getHeight() + smartViewFront.getTranslationY());
                    smartViewBottom.bringToFront();
                }
                if (currentMotion.smartView.content == null) return false;
                if (view.getId() == R.id.smart_dragger)
                    currentMotion.motionMode = MotionMode.MOVE;
                else if (view.getId() == R.id.smart_content) {
                    if (!currentMotion.smartView.collapsed)
                        currentMotion.motionMode = MotionMode.SCROLL;
                    else
                        currentMotion.motionMode = MotionMode.NONE;
                }
                currentMotion.align = event.getY();

                break;
        }
        return false;
    }
}
