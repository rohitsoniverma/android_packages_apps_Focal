/**
 * Copyright (C) 2013 The CyanogenMod Project
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */

package org.cyanogenmod.nemesis.widgets;

import android.animation.Animator;
import android.animation.Animator.AnimatorListener;
import android.content.Context;
import android.hardware.Camera;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.widget.FrameLayout;
import android.widget.GridLayout;
import android.widget.ImageView;

import org.cyanogenmod.nemesis.CameraManager;
import org.cyanogenmod.nemesis.R;
import org.cyanogenmod.nemesis.ui.SideBar;
import org.cyanogenmod.nemesis.ui.WidgetRenderer;

/**
 * Base class for settings widget. Each setting widget
 * will extend this class.
 * <p/>
 * Remember that we are working in landscape, so the GridLayout
 * column count is the width of the widget when looking landscape,
 * and become the height when looking portrait.
 * <p/>
 * What we call the "MaxWidgetWidth" is the number of ROWS in landscape,
 * that looks like COLUMNS in portrait.
 */
public abstract class WidgetBase {
    public final static String TAG = "WidgetBase";

    protected CameraManager mCamManager;
    private GestureDetector mGestureDetector;
    private WidgetToggleButton mToggleButton;
    private WidgetContainer mWidget;
    private int mWidgetMaxWidth;
    private boolean mIsOpen;

    private final static int WIDGET_FADE_DURATION_MS = 200;
    private final static int WIDGET_ANIM_TRANSLATE = 80;

    public WidgetBase(CameraManager cam, Context context, int iconResId) {
        mCamManager = cam;
        mWidget = new WidgetContainer(context);
        mToggleButton = new WidgetToggleButton(context);
        mGestureDetector = new GestureDetector(context, new WidgetFlingGestureListener());
        mIsOpen = false;

        // Setup the container
        mWidget.setColumnCount(1);
        mWidget.setRowCount(0);
        mWidget.setAlpha(0.0f);

        // Setup the toggle button
        mToggleButton.setImageResource(iconResId);

        mWidgetMaxWidth = context.getResources().getInteger(R.integer.widget_default_max_width);
    }

    /**
     * This method returns whether or not the widget settings are
     * supported by the camera HAL or not.
     * <p/>
     * This method must be overridden by each widget, in order to hide
     * widgets that are not supported by the Camera device.
     *
     * @param params The Camera parameters provided by HAL
     * @return true if the widget is supported by the device
     */
    public abstract boolean isSupported(Camera.Parameters params);

    /**
     * Add a view to the container widget
     *
     * @param v The view to add
     */
    public void addViewToContainer(View v) {
        if (mWidget.getRowCount() * mWidget.getColumnCount() < mWidget.getChildCount()+1) {
            if (mWidget.getRowCount() == mWidgetMaxWidth) {
                // Add a new column instead of a line to fit the max width
                mWidget.setColumnCount(mWidget.getColumnCount() + 1);
            } else if (mWidget.getRowCount() < mWidgetMaxWidth) {
                mWidget.setRowCount(mWidget.getRowCount() + 1);
            }
        }

        mWidget.addView(v, 0);
    }

    public WidgetToggleButton getToggleButton() {
        return mToggleButton;
    }

    public WidgetContainer getWidget() {
        return mWidget;
    }

    public boolean isOpen() {
        return mIsOpen;
    }

    /**
     * Opens the widget, show the fade in animation
     */
    public void open() {
        WidgetRenderer parent = (WidgetRenderer) mWidget.getParent();

        mWidget.setVisibility(View.VISIBLE);
        mWidget.invalidate();

        mIsOpen = true;

        parent.widgetOpened(mWidget);

        mWidget.animate().alpha(1.0f).translationXBy(WIDGET_ANIM_TRANSLATE)
                .setDuration(WIDGET_FADE_DURATION_MS).start();
    }

    public void close() {
        WidgetRenderer parent = (WidgetRenderer) mWidget.getParent();
        parent.widgetClosed(mWidget);

        mWidget.animate().alpha(0.0f).translationXBy(-WIDGET_ANIM_TRANSLATE)
                .setDuration(WIDGET_FADE_DURATION_MS).start();

        mIsOpen = false;
    }


    /**
     * Represents the button that toggles the widget, in the
     * side bar.
     */
    public class WidgetToggleButton extends ImageView {
        public WidgetToggleButton(Context context, AttributeSet attrs,
                                  int defStyle) {
            super(context, attrs, defStyle);
            initialize();
        }

        public WidgetToggleButton(Context context, AttributeSet attrs) {
            super(context, attrs);
            initialize();
        }

        public WidgetToggleButton(Context context) {
            super(context);
            initialize();
        }

        private void initialize() {
            this.setClickable(true);

            int pad = getResources().getDimensionPixelSize(R.dimen.widget_toggle_button_padding);
            this.setPadding(pad, pad, pad, pad);

            this.setOnClickListener(new ToggleClickListener());
        }

        public void toggleBackground(boolean active) {
            if (active) {
                this.setBackgroundColor(getResources().getColor(R.color.widget_toggle_active));
            } else {
                this.setBackgroundColor(0);
            }
        }

        @Override
        public boolean onTouchEvent(MotionEvent event) {
            // to dispatch click / long click event, we do it first
            boolean defaultResult = super.onTouchEvent(event);

            // Handle the background color on touch
            if (event.getActionMasked() == MotionEvent.ACTION_DOWN) {
                this.setBackgroundColor(getResources().getColor(R.color.widget_toggle_pressed));
            } else if (event.getActionMasked() == MotionEvent.ACTION_UP) {
                // State is not updated yet, but after ACTION_UP is received the button
                // will likely be clicked and its state will change.
                toggleBackground(!mIsOpen);
            }

            return defaultResult;
        }

        private class ToggleClickListener implements OnClickListener {
            @Override
            public void onClick(View v) {
                SideBar sb = (SideBar) WidgetToggleButton.this.getParent().getParent();
                sb.toggleWidgetVisibility(WidgetBase.this, false);
            }
        }
    }

    /**
     * Interface that all widget options must implement. Give info
     * about the layout of the widget.
     */
    public interface WidgetOption {
        /**
         * Returns the number of columns occupied by the widget.
         * A button typically occupies 1 column, but specific widgets
         * like text widget might occupy two columns in some cases.
         *
         * @return Number of columns
         */
        public int getColSpan();
    }

    /**
     * Represents a standard setting button put inside
     * a {@link WidgetContainer}.
     * Note that you're not forced to use exclusively buttons,
     * TextViews can also be added (for Timer for instance)
     */
    public class WidgetOptionButton extends ImageView implements WidgetOption {
        private float mTouchOffset = 0.0f;
        private int mOriginalResource;

        public WidgetOptionButton(int resId, Context context, AttributeSet attrs,
                                  int defStyle) {
            super(context, attrs, defStyle);
            initialize(resId);
        }

        public WidgetOptionButton(int resId, Context context, AttributeSet attrs) {
            super(context, attrs);
            initialize(resId);

        }

        public WidgetOptionButton(int resId, Context context) {
            super(context);
            initialize(resId);
        }

        public void resetImage() {
            setImageResource(mOriginalResource);
        }

        @Override
        public boolean onTouchEvent(MotionEvent event) {
            boolean handle = false;

            mGestureDetector.onTouchEvent(event);

            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                handle = true;
                mTouchOffset = mWidget.getX() - event.getRawX();
                WidgetRenderer parent = (WidgetRenderer) mWidget.getParent();
                parent.widgetPressed(mWidget);
            } else if (event.getAction() == MotionEvent.ACTION_MOVE) {
                WidgetRenderer parent = (WidgetRenderer) mWidget.getParent();
                mWidget.setX(event.getRawX() + mTouchOffset);
                parent.widgetMoved(mWidget);
                mWidget.forceFinalX(getX());
                handle = true;
            } else if (event.getAction() == MotionEvent.ACTION_UP) {
                WidgetRenderer parent = (WidgetRenderer) mWidget.getParent();
                parent.widgetDropped(mWidget);
            }

            return (super.onTouchEvent(event) || handle);
        }

        private void initialize(int resId) {
            this.setImageResource(resId);
            this.setClickable(true);
            mOriginalResource = resId;
        }

        @Override
        public int getColSpan() {
            return 1;
        }
    }

    /**
     * Represents the floating window of a widget.
     */
    public class WidgetContainer extends GridLayout {
        private float mTouchOffset = 0.0f;
        private float mTargetX = 0.0f;

        public WidgetContainer(Context context, AttributeSet attrs,
                               int defStyle) {
            super(context, attrs, defStyle);
            initialize();
        }

        public WidgetContainer(Context context, AttributeSet attrs) {
            super(context, attrs);
            initialize();
        }

        public WidgetContainer(Context context) {
            super(context);
            initialize();
        }

        /**
         * Animate the movement on X
         *
         * @param x
         */
        public void setXSmooth(float x) {
            if (mTargetX != x) {
                this.animate().cancel();
                this.animate().x(x).setDuration(100).start();
                mTargetX = x;
            }
        }

        /**
         * Returns the final X position, after the animation is done
         *
         * @return final X position
         */
        public float getFinalX() {
            return mTargetX;
        }

        public void forceFinalX(float x) {
            mTargetX = x;
        }

        public WidgetBase getWidgetBase() {
            return WidgetBase.this;
        }

        @Override
        public boolean onTouchEvent(MotionEvent event) {
            boolean handle = false;
            if (getAlpha() < 1) return false;

            mGestureDetector.onTouchEvent(event);

            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                handle = true;
                mTouchOffset = getX() - event.getRawX();
                WidgetRenderer parent = (WidgetRenderer) mWidget.getParent();
                parent.widgetPressed(mWidget);
            } else if (event.getAction() == MotionEvent.ACTION_MOVE) {
                setX(event.getRawX() + mTouchOffset);
                WidgetRenderer parent = (WidgetRenderer) mWidget.getParent();
                parent.widgetMoved(mWidget);
                forceFinalX(getX());
                handle = true;
            } else if (event.getAction() == MotionEvent.ACTION_UP) {
                WidgetRenderer parent = (WidgetRenderer) mWidget.getParent();
                parent.widgetDropped(mWidget);
            }

            return (super.onTouchEvent(event) || handle);
        }

        @Override
        protected void onAttachedToWindow() {
            super.onAttachedToWindow();

            FrameLayout.LayoutParams layoutParam = (FrameLayout.LayoutParams) this.getLayoutParams();
            layoutParam.width = FrameLayout.LayoutParams.WRAP_CONTENT;
        }

        private void initialize() {
            this.setBackgroundColor(getResources().getColor(R.color.widget_background));
            this.setColumnOrderPreserved(true);

            // We default the window invisible, so we must respect the status
            // obtained after the "close" animation
            setAlpha(0.0f);
            setTranslationX(-WIDGET_ANIM_TRANSLATE);
            setVisibility(View.VISIBLE);

            // Set padding
            int padding_in_dp = 8;
            final float scale = getResources().getDisplayMetrics().density;
            int pad = (int) (padding_in_dp * scale + 0.5f);
            this.setPadding(pad, pad, pad, pad);

            this.animate().setListener(new AnimatorListener() {
                @Override
                public void onAnimationCancel(Animator arg0) {
                }

                @Override
                public void onAnimationEnd(Animator arg0) {
                    if (!mIsOpen)
                        WidgetContainer.this.setVisibility(View.GONE);
                }

                @Override
                public void onAnimationRepeat(Animator arg0) {
                }

                @Override
                public void onAnimationStart(Animator arg0) {
                    WidgetContainer.this.setVisibility(View.VISIBLE);
                }
            });


        }

        @Override
        protected void onMeasure(int widthSpec, int heightSpec) {
            super.onMeasure(widthSpec, heightSpec);

            if (!isOpen())
                setVisibility(View.GONE);
            else
                setVisibility(View.VISIBLE);
        }


        public void notifyOrientationChanged(int orientation, boolean fastforward) {
            int buttonsCount = getChildCount();

            for (int i = 0; i < buttonsCount; i++) {
                View child = getChildAt(i);

                if (fastforward) {
                    child.setRotation(orientation);
                } else {
                    child.animate().rotation(orientation)
                            .setDuration(200).setInterpolator(new DecelerateInterpolator()).start();
                }
            }
        }
    }

    /**
     * Handles the swipe of widgets out of the screen (to the top/left)
     */
    public class WidgetFlingGestureListener extends GestureDetector.SimpleOnGestureListener {
        //private final static String TAG = "GestureListener";

        private static final int SWIPE_MIN_DISTANCE = 10;
        private static final int SWIPE_MAX_OFF_PATH = 250;
        private static final int SWIPE_THRESHOLD_VELOCITY = 200;

        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
            try {
                if (Math.abs(e1.getX() - e2.getX()) > SWIPE_MAX_OFF_PATH)
                    return false;

                // swipe top to close the widget

                // Somehow, GestureDetector takes the screen orientation into account, even if our
                // activity is locked in landscape.
                float distance = e2.getY() - e1.getY();
                if (distance <= 0.1f)
                    distance = e2.getX() - e1.getX();

                if (distance > SWIPE_MIN_DISTANCE && Math.abs(velocityY) > SWIPE_THRESHOLD_VELOCITY) {
                    final SideBar sb = (SideBar) mToggleButton.getParent().getParent();
                    final WidgetRenderer wr = (WidgetRenderer) mWidget.getParent();
                    sb.toggleWidgetVisibility(WidgetBase.this, true);
                    wr.widgetClosed(mWidget);
                    mToggleButton.toggleBackground(false);
                }
            } catch (Exception e) {
                // nothing
            }
            return false;
        }
    }
}
