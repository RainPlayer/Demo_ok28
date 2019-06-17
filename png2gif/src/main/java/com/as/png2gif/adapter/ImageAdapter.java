package com.as.png2gif.adapter;

import android.support.annotation.Nullable;
import android.view.View;
import android.widget.ImageView;

import com.as.png2gif.R;
import com.bumptech.glide.Glide;
import com.chad.library.adapter.base.BaseQuickAdapter;
import com.chad.library.adapter.base.BaseViewHolder;

import java.util.List;

/**
 * -----------------------------
 * Created by zqf on 2019/6/14.
 * ---------------------------
 */
public class ImageAdapter extends BaseQuickAdapter<String, BaseViewHolder> {

    public ImageAdapter(int layoutResId, @Nullable List<String> data) {
        super(layoutResId, data);
    }

    @Override
    protected void convert(BaseViewHolder helper, String item) {
        ImageView item_image = helper.getView(R.id.item_image);
        Glide.with(mContext)
                .load(item)
                .into(item_image);

    }
}
