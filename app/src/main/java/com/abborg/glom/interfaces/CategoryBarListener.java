package com.abborg.glom.interfaces;

import android.support.v7.widget.RecyclerView;

import com.abborg.glom.model.Category;

import java.util.List;

public interface CategoryBarListener {

    void onCategoryBarRequireUpdate(List<Category> categories, RecyclerView recyclerView);

    boolean shouldShowCategoryBar();
}
