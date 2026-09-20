package com.example.testapp.feature.dashboard;

import com.example.testapp.core.SocialPlatform;
import com.example.testapp.core.UserQrItem;

import android.app.Activity;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.Button;
import android.widget.EditText;
import java.util.ArrayList;
import java.util.List;

public class DashboardFilterController {

    public interface OnFilterChangeListener {
        void onFilterChanged();
    }

    private final Activity activity;
    private final OnFilterChangeListener listener;

    private EditText etSearch;
    private Button chipAll;
    private Button chipSocial;
    private Button chipBiz;
    private Button chipFav;

    private String searchQuery = "";
    private SocialPlatform.Category activeCategory = SocialPlatform.Category.ALL;

    public DashboardFilterController(Activity activity, OnFilterChangeListener listener) {
        this.activity = activity;
        this.listener = listener;

        bindViews();
        setupListeners();
        updateCategoryUi();
    }

    private void bindViews() {
        etSearch = activity.findViewById(R.id.etSearchQr);
        chipAll = activity.findViewById(R.id.chipFilterAll);
        chipSocial = activity.findViewById(R.id.chipFilterSocial);
        chipBiz = activity.findViewById(R.id.chipFilterBiz);
        chipFav = activity.findViewById(R.id.chipFilterFav);
    }

    private void setupListeners() {
        if (etSearch != null) {
            etSearch.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    searchQuery = s.toString().trim().toLowerCase();
                    if (listener != null) listener.onFilterChanged();
                }

                @Override
                public void afterTextChanged(Editable s) {}
            });
        }

        if (chipAll != null) chipAll.setOnClickListener(v -> setCategory(SocialPlatform.Category.ALL));
        if (chipSocial != null) chipSocial.setOnClickListener(v -> setCategory(SocialPlatform.Category.SOCIAL));
        if (chipBiz != null) chipBiz.setOnClickListener(v -> setCategory(SocialPlatform.Category.BIZ));
        if (chipFav != null) chipFav.setOnClickListener(v -> setCategory(SocialPlatform.Category.FAV));
    }

    public void setCategory(SocialPlatform.Category category) {
        this.activeCategory = category;
        updateCategoryUi();
        if (listener != null) {
            listener.onFilterChanged();
        }
    }

    private void updateCategoryUi() {
        styleChip(chipAll, activeCategory == SocialPlatform.Category.ALL);
        styleChip(chipSocial, activeCategory == SocialPlatform.Category.SOCIAL);
        styleChip(chipBiz, activeCategory == SocialPlatform.Category.BIZ);
        styleChip(chipFav, activeCategory == SocialPlatform.Category.FAV);
    }

    private void styleChip(Button chip, boolean selected) {
        if (chip == null) return;
        chip.setBackgroundResource(selected ? R.drawable.btn_pill_dark : R.drawable.btn_pill_light);
        chip.setTextColor(activity.getColor(selected ? R.color.white : R.color.text_secondary));
    }

    public List<UserQrItem> filter(List<UserQrItem> allItems) {
        List<UserQrItem> result = new ArrayList<>();
        if (allItems == null) return result;

        for (UserQrItem item : allItems) {
            boolean matchesSearch = searchQuery.isEmpty()
                    || item.getName().toLowerCase().contains(searchQuery)
                    || item.getUrl().toLowerCase().contains(searchQuery);

            boolean matchesCategory = true;
            if (activeCategory == SocialPlatform.Category.FAV) {
                matchesCategory = item.isFavorite();
            } else if (activeCategory == SocialPlatform.Category.SOCIAL) {
                SocialPlatform platform = SocialPlatform.fromKey(item.getIcon());
                matchesCategory = (platform.getCategory() == SocialPlatform.Category.SOCIAL);
            } else if (activeCategory == SocialPlatform.Category.BIZ) {
                SocialPlatform platform = SocialPlatform.fromKey(item.getIcon());
                matchesCategory = (platform.getCategory() == SocialPlatform.Category.BIZ);
            }

            if (matchesSearch && matchesCategory) {
                result.add(item);
            }
        }
        return result;
    }
}
