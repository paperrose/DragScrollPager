package com.github.paperrose.dragscrollpager;

import android.support.v4.view.PagerAdapter;
import android.view.View;
import android.view.ViewGroup;

/**
 * Created by Emil.Allakhverdiev on 14.02.2017.
 */
public abstract class DragScrollPagerAdapter extends PagerAdapter {
   public abstract View instantiateDragger(ViewGroup container, int position);
   public abstract View instantiateBottomDragger(ViewGroup container, int position);
   public abstract LoadedView instantiateContent(ViewGroup container, int position);
}
