/*
 * Copyright (C) 2015 The Android Open Source Project
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
package com.android.systemui.qs.tiles;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.os.BatteryManager;
import android.os.Handler;
import android.os.Looper;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.RelativeSizeSpan;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnAttachStateChangeListener;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Checkable;
import android.widget.ImageView;
import android.widget.TextView;
import com.android.internal.logging.MetricsProto.MetricsEvent;
import com.android.settingslib.BatteryInfo;
import com.android.settingslib.graph.UsageView;
import com.android.systemui.BatteryMeterDrawable;
import com.android.systemui.R;
import com.android.systemui.qs.QSIconView;
import com.android.systemui.qs.QSTile;
import com.android.systemui.statusbar.policy.BatteryController;

import java.text.NumberFormat;

import itel.transsion.settingslib.utils.LogUtil;

public class BatteryTile extends QSTile<QSTile.State> implements BatteryController.BatteryStateChangeCallback {

    private final BatteryController mBatteryController;
    private final BatteryDetail mBatteryDetail = new BatteryDetail();

    private int mLevel;
    private boolean mPowerSave;
    private boolean mCharging;
    private boolean mDetailShown;
    private boolean mPluggedIn;

    public BatteryTile(Host host) {
        super(host);
        // Talpa:PeterHuang add for fixed tfs#Bug 14173@{
        updateBatteryImmediately(host.getContext());
        // @}
        mBatteryController = host.getBatteryController();
    }

    @Override
    public State newTileState() {
        return new QSTile.State();
    }

    //talpa zhw add
    @Override
    public QSIconView createTileView(Context mContext){
        QSIconView view = super.createTileView(mContext);
        view.disableAnimation();
        return view;
    }
    //talpa zhw add end

    @Override
    public DetailAdapter getDetailAdapter() {
        return mBatteryDetail;
    }

    @Override
    public int getMetricsCategory() {
        return MetricsEvent.QS_BATTERY_TILE;
    }

    @Override
    public void setListening(boolean listening) {
        if (listening) {
            mBatteryController.addStateChangedCallback(this);
        } else {
            mBatteryController.removeStateChangedCallback(this);
        }
    }

    @Override
    public void setDetailListening(boolean listening) {
        super.setDetailListening(listening);
        if (!listening) {
            mBatteryDetail.mCurrentView = null;
        }
    }

    @Override
    public Intent getLongClickIntent() {
        return new Intent(Intent.ACTION_POWER_USAGE_SUMMARY);
    }

    @Override
    protected void handleClick() {
        // talpa@andy 2017/5/5 10:17 modify @{
//        showDetail(true);
        mHost.startActivityDismissingKeyguard(new Intent(Intent.ACTION_POWER_USAGE_SUMMARY));
        // @}
    }

    @Override
    public CharSequence getTileLabel() {
        return mContext.getString(R.string.battery);
    }

    @Override
    protected void handleUpdateState(State state, Object arg) {
       // LogUtil.d("handleUpdateState: " + mLevel);
        int level = (arg != null) ? (Integer) arg : mLevel;
        // talpa@andy 2017/5/6 15:51 modify：阿拉伯语下%放在数字左边 @{
//        String percentage = NumberFormat.getPercentInstance().format((double) level / 100.0);
        String percentage = "";
        if(mContext.getResources().getConfiguration().getLayoutDirection() == View.LAYOUT_DIRECTION_LTR){
            percentage = mContext.getResources().getString(R.string.battery_level_template, level);
        } else {
            percentage = mContext.getResources().getString(R.string.battery_level_template_rtl, level);
        }
        // @}
        state.icon = new Icon() {
            @Override
            public Drawable getDrawable(Context context) {
                // SPRD: Bug 587470 new construction.
                BatteryMeterDrawable drawable =
                        new BatteryMeterDrawable(context, new Handler(Looper.getMainLooper()),
                        context.getColor(R.color.batterymeter_frame_color), true);
                //talpa zhw add\
                drawable.setQsBattery(true);
                //talpa zhw add end
                drawable.onBatteryLevelChanged(mLevel, mPluggedIn, mCharging);
                drawable.onPowerSaveChanged(mPowerSave);

                Drawable mdrawable = context.getDrawable(R.drawable.itel_qs_tile_bg_dark);//zhw test
                //talpa zhw return drawable;
                Drawable[] drawables = new Drawable[2];
                drawables[0] = mdrawable;
                drawables[1] = drawable;
                LayerDrawable ldrawable = new LayerDrawable(drawables);
                return ldrawable;
            }

            @Override
            public int getPadding() {
                return mHost.getContext().getResources().getDimensionPixelSize(
                        R.dimen.qs_battery_padding);
            }
        };
        state.label = percentage;
        state.contentDescription = mContext.getString(R.string.accessibility_quick_settings_battery,
                percentage) + "," +
                (mPowerSave ? mContext.getString(R.string.battery_saver_notification_title)
                        : mCharging ? mContext.getString(R.string.expanded_header_battery_charging)
                                : "")
                + "," + mContext.getString(R.string.accessibility_battery_details);
        state.minimalAccessibilityClassName = state.expandedAccessibilityClassName
                = Button.class.getName();
    }

    @Override
    public void onBatteryLevelChanged(int level, boolean pluggedIn, boolean charging) {
        mLevel = level;
        mPluggedIn = pluggedIn;
        mCharging = charging;
        refreshState((Integer) level);
        if (mDetailShown) {
            mBatteryDetail.postBindView();
        }
    }

    @Override
    public void onPowerSaveChanged(boolean isPowerSave) {
        mPowerSave = isPowerSave;
        refreshState(null);
        if (mDetailShown) {
            mBatteryDetail.postBindView();
        }
    }

    private final class BatteryDetail implements DetailAdapter, OnClickListener,
            OnAttachStateChangeListener {
        // SPRD: Bug 587470 new construction.
        private final BatteryMeterDrawable mDrawable = new BatteryMeterDrawable(mHost.getContext(),
                new Handler(), mHost.getContext().getColor(R.color.batterymeter_frame_color), true);
        private View mCurrentView;

        @Override
        public CharSequence getTitle() {
            return mContext.getString(R.string.battery_panel_title, mLevel);
        }

        @Override
        public Boolean getToggleState() {
            return null;
        }

        @Override
        public View createDetailView(Context context, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = LayoutInflater.from(mContext).inflate(R.layout.battery_detail, parent,
                        false);
            }
            mCurrentView = convertView;
            mCurrentView.addOnAttachStateChangeListener(this);
            bindView();
            return convertView;
        }

        private void postBindView() {
            if (mCurrentView == null) return;
            mCurrentView.post(new Runnable() {
                @Override
                public void run() {
                    bindView();
                }
            });
        }

        private void bindView() {
            if (mCurrentView == null) {
                return;
            }
            mDrawable.onBatteryLevelChanged(100, false, false);
            mDrawable.onPowerSaveChanged(true);
            mDrawable.disableShowPercent();
            ((ImageView) mCurrentView.findViewById(android.R.id.icon)).setImageDrawable(mDrawable);
            Checkable checkbox = (Checkable) mCurrentView.findViewById(android.R.id.toggle);
            checkbox.setChecked(mPowerSave);
            BatteryInfo.getBatteryInfo(mContext, new BatteryInfo.Callback() {
                @Override
                public void onBatteryInfoLoaded(BatteryInfo info) {
                    if (mCurrentView != null) {
                        bindBatteryInfo(info);
                    }
                }
            });
            final TextView batterySaverTitle =
                    (TextView) mCurrentView.findViewById(android.R.id.title);
            final TextView batterySaverSummary =
                    (TextView) mCurrentView.findViewById(android.R.id.summary);
            if (mCharging) {
                mCurrentView.findViewById(R.id.switch_container).setAlpha(.7f);
                batterySaverTitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
                batterySaverTitle.setText(R.string.battery_detail_charging_summary);
                mCurrentView.findViewById(android.R.id.toggle).setVisibility(View.GONE);
                mCurrentView.findViewById(R.id.switch_container).setClickable(false);
            } else {
                mCurrentView.findViewById(R.id.switch_container).setAlpha(1);
                batterySaverTitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
                batterySaverTitle.setText(R.string.battery_detail_switch_title);
                batterySaverSummary.setText(R.string.battery_detail_switch_summary);
                mCurrentView.findViewById(android.R.id.toggle).setVisibility(View.VISIBLE);
                mCurrentView.findViewById(R.id.switch_container).setClickable(true);
                mCurrentView.findViewById(R.id.switch_container).setOnClickListener(this);
            }
        }

        private void bindBatteryInfo(BatteryInfo info) {
            SpannableStringBuilder builder = new SpannableStringBuilder();
            builder.append(info.batteryPercentString, new RelativeSizeSpan(2.6f),
                    Spanned.SPAN_INCLUSIVE_EXCLUSIVE);
            if (info.remainingLabel != null) {
                if (mContext.getResources().getBoolean(R.bool.quick_settings_wide)) {
                    builder.append(' ');
                } else {
                    builder.append('\n');
                }
                builder.append(info.remainingLabel);
            }
            ((TextView) mCurrentView.findViewById(R.id.charge_and_estimation)).setText(builder);

            info.bindHistory((UsageView) mCurrentView.findViewById(R.id.battery_usage));
        }

        @Override
        public void onClick(View v) {
            mBatteryController.setPowerSaveMode(!mPowerSave);
        }

        @Override
        public Intent getSettingsIntent() {
            return new Intent(Intent.ACTION_POWER_USAGE_SUMMARY);
        }

        @Override
        public void setToggleState(boolean state) {
            // No toggle state.
        }

        @Override
        public int getMetricsCategory() {
            return MetricsEvent.QS_BATTERY_DETAIL;
        }

        @Override
        public void onViewAttachedToWindow(View v) {
            if (!mDetailShown) {
                mDetailShown = true;
                v.getContext().registerReceiver(mReceiver,
                        new IntentFilter(Intent.ACTION_TIME_TICK));
            }
        }

        @Override
        public void onViewDetachedFromWindow(View v) {
            if (mDetailShown) {
                mDetailShown = false;
                v.getContext().unregisterReceiver(mReceiver);
            }
        }

        private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                postBindView();
            }
        };
    }

    // Talpa:PeterHuang add for update battery while create batteryTile @{
    private void updateBatteryImmediately(Context context){
        if(null != context){
            // register null Receiver get status Immediately
            LogUtil.d("start: "+ mLevel);
            Intent batteryInfoIntent = context.registerReceiver( null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED )) ;
            if (null != batteryInfoIntent) {
                mLevel = batteryInfoIntent.getIntExtra(BatteryManager.EXTRA_LEVEL, 0);
            }
            LogUtil.d("end: "+ mLevel);
        }

    }
    // @}

}
