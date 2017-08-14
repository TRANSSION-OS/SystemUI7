/*
 * Copyright (C) 2008 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.systemui.statusbar.policy;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.TypedArray;
import android.graphics.Typeface;
import android.icu.text.DateFormat;
import android.util.AttributeSet;
import android.widget.TextView;

import com.android.systemui.R;
import com.android.systemui.statusbar.phoneleather.SystemUIFontFactory;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class DateView extends TextView {
    private static final String TAG = "DateView";

    private final Date mCurrentTime = new Date();

    private DateFormat mDateFormat;
    private String mLastText;
    private String mDatePattern;

    private BroadcastReceiver mIntentReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (Intent.ACTION_TIME_TICK.equals(action)
                    || Intent.ACTION_TIME_CHANGED.equals(action)
                    || Intent.ACTION_TIMEZONE_CHANGED.equals(action)
                    || Intent.ACTION_LOCALE_CHANGED.equals(action)) {
                if (Intent.ACTION_LOCALE_CHANGED.equals(action)
                        || Intent.ACTION_TIMEZONE_CHANGED.equals(action)) {
                    // need to get a fresh date format
                    mDateFormat = null;
                }
                updateClock();
            }
        }
    };

    public DateView(Context context, AttributeSet attrs) {
        super(context, attrs);
        TypedArray a = context.getTheme().obtainStyledAttributes(
                attrs,
                R.styleable.DateView,
                0, 0);

        try {
            mDatePattern = a.getString(R.styleable.DateView_datePattern);
        } finally {
            a.recycle();
        }
        if (mDatePattern == null) {
            mDatePattern = getContext().getString(R.string.system_ui_date_pattern);
        }
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();

        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_TIME_TICK);
        filter.addAction(Intent.ACTION_TIME_CHANGED);
        filter.addAction(Intent.ACTION_TIMEZONE_CHANGED);
        filter.addAction(Intent.ACTION_LOCALE_CHANGED);
        getContext().registerReceiver(mIntentReceiver, filter, null, null);

        //added by chenzhengjun start
//        Typeface typeface = SystemUIFontFactory.getInstance(mContext).getTypefaceByName("ItelNumber.ttf");
//        this.setTypeface(typeface);
        //added by chenzhengjun end

        updateClock();
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();

        mDateFormat = null; // reload the locale next time
        getContext().unregisterReceiver(mIntentReceiver);
    }

    protected void updateClock() {
        String text = mLastText;
        final Locale l = Locale.getDefault();
        mCurrentTime.setTime(System.currentTimeMillis());

        //talpa zhw modify SimpleDateFormat df = new SimpleDateFormat("EEEE MMMM d", Locale.getDefault());
        //talpa zhw add
        SimpleDateFormat df = new SimpleDateFormat("EEE.MMM d", Locale.getDefault());
        //talpa zhw add end
        text = df.format(mCurrentTime);

        if (getResources().getConfiguration().locale.getCountry().equals("CN")
                || getResources().getConfiguration().locale.getCountry().equals("TW")) {
            df = new SimpleDateFormat("EEEE MMMd", Locale.getDefault());
            text = df.format(mCurrentTime);
            text += getContext().getResources().getString(R.string.keyguard_bottom_date_day);
        }

        if (getResources().getConfiguration().locale.getCountry().equals("FR")) {
            df = new SimpleDateFormat("EEEE d MMMM", Locale.getDefault());
            text = df.format(mCurrentTime);
        }
        setText(text);
        mLastText = text;


        /*if (mDateFormat == null) {
            final Locale l = Locale.getDefault();
            DateFormat format = DateFormat.getInstanceForSkeleton(mDatePattern, l);
            format.setContext(DisplayContext.CAPITALIZATION_FOR_STANDALONE);
            mDateFormat = format;
        }

        mCurrentTime.setTime(System.currentTimeMillis());

        final String text = mDateFormat.format(mCurrentTime);
        if (!text.equals(mLastText)) {
            setText(text);
            mLastText = text;
        }*/
    }
}
