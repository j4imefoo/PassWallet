package org.ligi.passandroid.ui;

import android.content.Context;
import android.content.res.TypedArray;
import com.google.android.material.appbar.AppBarLayout;

import androidx.annotation.NonNull;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import android.util.AttributeSet;
import android.view.View;
import org.ligi.passandroid.R;

@SuppressWarnings("WeakerAccess")
public class MyShyFABBehavior extends CoordinatorLayout.Behavior<View> {

    public MyShyFABBehavior() {}

    public MyShyFABBehavior(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    public boolean layoutDependsOn(@NonNull CoordinatorLayout parent,
                                   @NonNull View child,
                                   @NonNull View dependency) {
        return isSnackbarLayout(dependency) || dependency instanceof AppBarLayout;
    }

    @Override
    public boolean onDependentViewChanged(@NonNull CoordinatorLayout parent,
                                          @NonNull View child,
                                          @NonNull View dependency) {
        if (isSnackbarLayout(dependency)) {
            updateFabTranslationForSnackbar(child, dependency);
        }
        if (dependency instanceof AppBarLayout) {
            final CoordinatorLayout.LayoutParams lp = (CoordinatorLayout.LayoutParams) child.getLayoutParams();
            final int fabBottomMargin = lp.bottomMargin;
            final int collapsedFabHeight = (int) (56 * child.getContext().getResources().getDisplayMetrics().density);
            final int distanceToScroll = Math.max(child.getHeight(), collapsedFabHeight) + fabBottomMargin;
            final float ratio = dependency.getY() / getToolbarHeight(dependency.getContext());

            child.setTranslationY(-distanceToScroll * ratio);
        }
        return false;
    }

    @Override
    public void onDependentViewRemoved(@NonNull final CoordinatorLayout parent,
                                       @NonNull final View child,
                                       @NonNull final View dependency) {
        super.onDependentViewRemoved(parent, child, dependency);
        onDependentViewChanged(parent,child,dependency);
    }

    private void updateFabTranslationForSnackbar(View child, View dependency) {
        final float translationY = dependency.getTranslationY() - dependency.getHeight();
        child.setTranslationY(Math.min(0, translationY));
    }

    private boolean isSnackbarLayout(View dependency) {
        return "com.google.android.material.snackbar.Snackbar$SnackbarLayout".equals(dependency.getClass().getName());
    }

    private int getToolbarHeight(Context context) {
        final TypedArray styledAttributes = context.getTheme().obtainStyledAttributes(
                new int[]{R.attr.actionBarSize});
        int toolbarHeight = (int) styledAttributes.getDimension(0, 0);
        styledAttributes.recycle();

        return toolbarHeight;
    }
}
