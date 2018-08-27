/*
 * Copyright (C) by MinterTeam. 2018
 * @link https://github.com/MinterTeam
 * @link https://github.com/edwardstock
 *
 * The MIT License
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package network.minter.bipwallet.sending.dialogs;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.widget.Button;
import android.widget.TextView;

import butterknife.BindView;
import butterknife.ButterKnife;
import network.minter.bipwallet.R;
import network.minter.bipwallet.internal.dialogs.WalletDialog;
import network.minter.bipwallet.internal.dialogs.WalletDialogBuilder;
import network.minter.bipwallet.internal.views.widgets.BipCircleImageView;

/**
 * MinterWallet. 2018
 *
 * @author Eduard Maximovich <edward.vstock@gmail.com>
 */
public class WalletTxSendSuccessDialog extends WalletDialog {
    private final Builder mBuilder;
    @BindView(R.id.tx_recipient_avatar) BipCircleImageView avatar;
    @BindView(R.id.tx_recipient_name) TextView recipientName;
    @BindView(R.id.action_view_tx) Button actionViewTx;
    @BindView(R.id.action_close) Button actionClose;

    protected WalletTxSendSuccessDialog(@NonNull Context context, Builder builder) {
        super(context);
        mBuilder = builder;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.wallet_tx_send_complete_dialog);
        ButterKnife.bind(this);
        title.setText(mBuilder.getTitle());

        recipientName.setText(mBuilder.mRecipientName);
        if (mBuilder.mAvatarUrl != null) {
            avatar.setImageUrl(mBuilder.mAvatarUrl);
        }

        actionViewTx.setOnClickListener(v -> {
            if (mBuilder.hasPositiveListener()) {
                mBuilder.getPositiveListener().onClick(WalletTxSendSuccessDialog.this, BUTTON_POSITIVE);
            }
        });
        actionClose.setOnClickListener(v -> {
            dismiss();
        });

    }

    public static class Builder extends WalletDialogBuilder<WalletTxSendSuccessDialog, WalletTxSendSuccessDialog.Builder> {
        private CharSequence mRecipientName;
        private String mAvatarUrl;


        public Builder(Context context) {
            super(context);
        }

        public Builder(Context context, CharSequence title) {
            super(context, title);
        }

        public Builder setPositiveAction(CharSequence title, OnClickListener listener) {
            return super.setAction(BUTTON_POSITIVE, title, listener);
        }

        public Builder setNegativeAction(CharSequence title, OnClickListener listener) {
            return super.setAction(BUTTON_NEGATIVE, title, listener);
        }

        public Builder setRecipientName(CharSequence recipientName) {
            mRecipientName = recipientName;
            return this;
        }

        public Builder setAvatar(String avatarUrl) {
            mAvatarUrl = avatarUrl;
            return this;
        }

        @Override
        public WalletTxSendSuccessDialog create() {
            return new WalletTxSendSuccessDialog(getContext(), this);
        }
    }
}
