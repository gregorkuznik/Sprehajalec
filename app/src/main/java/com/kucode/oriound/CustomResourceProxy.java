package com.kucode.oriound;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;

import org.osmdroid.DefaultResourceProxyImpl;

public class CustomResourceProxy extends DefaultResourceProxyImpl {

    private final Context mContext;
    public CustomResourceProxy(Context pContext) {
        super(pContext);
        mContext = pContext;
    }

    @Override
    public Bitmap getBitmap(final bitmap pResId) {
        switch (pResId){
            case direction_arrow:
                return BitmapFactory.decodeResource(mContext.getResources(),R.drawable.ic_nav_3);
        }
        return super.getBitmap(pResId);
    }

    @Override
    public Drawable getDrawable(final bitmap pResId) {
        switch (pResId){
            case direction_arrow:
                return mContext.getResources().getDrawable(R.drawable.ic_nav_3);
        }
        return super.getDrawable(pResId);
    }
}