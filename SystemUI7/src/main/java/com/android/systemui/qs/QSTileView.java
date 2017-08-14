/*
 * Copyright (C) 2014 The Android Open Source Project
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

package com.android.systemui.qs;

import android.content.Context;
import android.content.res.Configuration;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import com.android.systemui.R;
import libcore.util.Objects;

/** View that represents a standard quick settings tile. **/
public class QSTileView extends QSTileBaseView {
    protected final Context mContext;
    protected TextView mLabel;
    private ImageView mPadLock;

    public QSTileView(Context context, QSIconView icon) {
        this(context, icon, false);
    }

    public QSTileView(Context context, QSIconView icon, boolean collapsedView) {
        super(context, icon, collapsedView);
        mContext = context;
        setClipChildren(false);
        setClickable(true);
        setId(View.generateViewId());
        createLabel();
        setOrientation(VERTICAL);
        setGravity(Gravity.CENTER_HORIZONTAL);
    }

    public TextView getLabel() {
        return mLabel;
    }

    public void setLabel(TextView mLabel) {
        this.mLabel = mLabel;
    }

    @Override
    protected void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }

    protected void createLabel() {
        View view = LayoutInflater.from(mContext).inflate(R.layout.qs_tile_label, null);
        mLabel = (TextView) view.findViewById(R.id.tile_label);
        if(getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
            mLabel.setSingleLine();
        } else {
            mLabel.setMaxLines(2);
        }
        mPadLock = (ImageView) view.findViewById(R.id.restricted_padlock);
        addView(view);
    }

    @Override
    public void handleStateChanged(QSTile.State state) {
        super.handleStateChanged(state);
        if (!Objects.equal(mLabel.getText(), state.label)) {
            mLabel.setText(state.label);
        }
        mLabel.setEnabled(!state.disabledByPolicy);
        mPadLock.setVisibility(state.disabledByPolicy ? View.VISIBLE : View.GONE);
    }
}
