/*
 * Copyright (C) by MinterTeam. 2018
 * @link <a href="https://github.com/MinterTeam">Org Github</a>
 * @link <a href="https://github.com/edwardstock">Maintainer Github</a>
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

package network.minter.bipwallet.sending.ui;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.TextInputLayout;
import android.support.v7.widget.AppCompatEditText;
import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.Switch;
import android.widget.TextView;

import com.arellomobile.mvp.presenter.InjectPresenter;
import com.arellomobile.mvp.presenter.ProvidePresenter;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Provider;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;
import network.minter.bipwallet.BuildConfig;
import network.minter.bipwallet.R;
import network.minter.bipwallet.advanced.models.AccountItem;
import network.minter.bipwallet.home.HomeModule;
import network.minter.bipwallet.home.HomeTabFragment;
import network.minter.bipwallet.internal.dialogs.WalletConfirmDialog;
import network.minter.bipwallet.internal.dialogs.WalletDialog;
import network.minter.bipwallet.internal.helpers.ViewHelper;
import network.minter.bipwallet.internal.helpers.forms.DecimalInputFilter;
import network.minter.bipwallet.internal.helpers.forms.InputGroup;
import network.minter.bipwallet.internal.helpers.forms.validators.RegexValidator;
import network.minter.bipwallet.sending.SendTabModule;
import network.minter.bipwallet.sending.account.AccountSelectedAdapter;
import network.minter.bipwallet.sending.account.WalletAccountSelectorDialog;
import network.minter.bipwallet.sending.adapters.RecipientListAdapter;
import network.minter.bipwallet.sending.models.RecipientItem;
import network.minter.bipwallet.sending.views.SendTabPresenter;
import network.minter.explorer.MinterExplorerApi;
import permissions.dispatcher.NeedsPermission;
import permissions.dispatcher.OnPermissionDenied;
import permissions.dispatcher.OnShowRationale;
import permissions.dispatcher.PermissionRequest;
import permissions.dispatcher.RuntimePermissions;

/**
 * minter-android-wallet. 2018
 * @author Eduard Maximovich <edward.vstock@gmail.com>
 */
@RuntimePermissions
public class SendTabFragment extends HomeTabFragment implements SendTabModule.SendView {
    @Inject Provider<SendTabPresenter> presenterProvider;
    @InjectPresenter SendTabPresenter presenter;
    @BindView(R.id.input_coin) AppCompatEditText coinInput;
    @BindView(R.id.layout_input_recipient) TextInputLayout recipientLayout;
    @BindView(R.id.input_recipient) AutoCompleteTextView recipientInput;
    @BindView(R.id.layout_input_amount) TextInputLayout amountLayout;
    @BindView(R.id.input_amount) AppCompatEditText amountInput;
    @BindView(R.id.free_value) Switch freeValue;
    @BindView(R.id.action) Button actionSend;
    @BindView(R.id.action_scan_qr) View actionScanQR;
    @BindView(R.id.action_maximum) View actionMaximum;
    @BindView(R.id.text_error) TextView errorView;
    @BindView(R.id.fee_value) TextView feeValue;
    private Unbinder mUnbinder;
    private InputGroup mInputGroup;
    private WalletDialog mCurrentDialog = null;

    @Override
    public void onAttach(Context context) {
        HomeModule.getComponent().inject(this);
        super.onAttach(context);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        HomeModule.getComponent().inject(this);
        super.onCreate(savedInstanceState);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mUnbinder.unbind();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_tab_send, container, false);
        mUnbinder = ButterKnife.bind(this, view);
        mInputGroup = new InputGroup();
        mInputGroup.addInput(recipientInput);
        mInputGroup.addInput(amountInput);
        mInputGroup.addValidator(amountInput, new RegexValidator("^(\\d*)(\\.)?(\\d{1,18})$", "Invalid number"));
        /* ideal case */
        mInputGroup.addValidator(recipientInput,
                new RegexValidator(
                        // address or username with @ at begin or email
                        String.format("(((0|M|m)x)?([a-fA-F0-9]{40}))|(@[a-zA-Z0-9_]{5,32})|%s", Patterns.EMAIL_ADDRESS),
                        "Incorrect recipient format"
                ));

        mInputGroup.addFilter(amountInput, new DecimalInputFilter(() -> amountInput));

        recipientLayout.clearFocus();
        amountLayout.clearFocus();

        return view;
    }

    @Override
    public void setOnClickAccountSelectedListener(View.OnClickListener listener) {
        coinInput.setOnClickListener(listener);
    }

    @Override
    public void setOnClickMaximum(View.OnClickListener listener) {
        actionMaximum.setOnClickListener(listener);
    }

    @Override
    public void setOnTextChangedListener(InputGroup.OnTextChangedListener listener) {
        mInputGroup.addTextChangedListener(listener);
    }

    @Override
    public void setAccountName(CharSequence accountName) {
        coinInput.setText(accountName);
    }

    @Override
    public void setOnSubmit(View.OnClickListener listener) {
        actionSend.setOnClickListener(listener);
    }

    @Override
    public void setSubmitEnabled(boolean enabled) {
        actionSend.setEnabled(enabled);
    }

    @Override
    public void clearInputs() {
        recipientInput.setText(null);
        amountInput.setText(null);
        recipientLayout.clearFocus();
        amountLayout.clearFocus();
        mInputGroup.clearErrors();
    }

    @Override
    public void startDialog(WalletDialog.DialogExecutor executor) {
        mCurrentDialog = WalletDialog.switchDialogWithExecutor(this, mCurrentDialog, executor);
    }

    @Override
    public void startExplorer(String txHash) {
        startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(MinterExplorerApi.FRONT_URL + "/transactions/" + txHash)));
    }

    @Override
    public void setOnClickScanQR(View.OnClickListener listener) {
        actionScanQR.setOnClickListener(listener);
    }

    @NeedsPermission(Manifest.permission.CAMERA)
    @Override
    public void startScanQR(int requestCode) {
        Intent i = new Intent(getActivity(), QRCodeScannerActivity.class);
        getActivity().startActivityForResult(i, requestCode);
    }

    @Override
    public void startScanQRWithPermissions(int requestCode) {
        SendTabFragmentPermissionsDispatcher.startScanQRWithPermissionCheck(this, requestCode);
    }

    @Override
    public void setRecipient(CharSequence to) {
        recipientInput.setText(to);
    }

    @Override
    public void setRecipientError(CharSequence error) {
        mInputGroup.setError("recipient", error);
    }

    @Override
    public void setAmountError(CharSequence error) {
        mInputGroup.setError("amount", error);
    }

    @Override
    public void setError(CharSequence error) {
        ViewHelper.visible(errorView, error != null && error.length() > 0);
        errorView.setText(error);
    }

    @Override
    public void setAmount(CharSequence amount) {
        amountInput.setText(amount);
    }

    @Override
    public void setFee(CharSequence fee) {
        feeValue.setText(fee);
    }

    @Override
    public void setRecipientsAutocomplete(List<RecipientItem> items, RecipientListAdapter.OnItemClickListener listener) {
        if (items.size() > 0) {
            final RecipientListAdapter.OnItemClickListener cl = (item, position) -> {
                listener.onClick(item, position);
                recipientInput.dismissDropDown();
            };

            final RecipientListAdapter adapter = new RecipientListAdapter(getActivity(), items);
            adapter.setOnItemClickListener(cl);
            recipientInput.setAdapter(adapter);
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        presenter.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void setFormValidationListener(InputGroup.OnFormValidateListener listener) {
        mInputGroup.addFormValidateListener(listener);
    }

    @Override
    public void startAccountSelector(List<AccountItem> accounts, AccountSelectedAdapter.OnClickListener clickListener) {
        new WalletAccountSelectorDialog.Builder(getActivity(), "Select account")
                .setItems(accounts)
                .setOnClickListener(clickListener)
                .create().show();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        SendTabFragmentPermissionsDispatcher.onRequestPermissionsResult(this, requestCode, grantResults);
    }

    @OnShowRationale(Manifest.permission.CAMERA)
    void showRationaleForCamera(final PermissionRequest request) {
        new WalletConfirmDialog.Builder(getActivity(), "Camera request")
                .setText("We need access to your camera to take a shot with Minter Address QR Code")
                .setPositiveAction("Sure", (d, w) -> {
                    request.proceed();
                    d.dismiss();
                })
                .setNegativeAction("No, I've change my mind", (d, w) -> {
                    request.cancel();
                    d.dismiss();
                }).create()
                .show();
    }

    @OnPermissionDenied(Manifest.permission.CAMERA)
    void showOpenPermissionsForCamera() {
        new WalletConfirmDialog.Builder(getActivity(), "Camera request")
                .setText("We need access to your camera to take a shot with Minter Address QR Code")
                .setPositiveAction("Open settings", (d, w) -> {
                    Intent intent = new Intent();
                    intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                    Uri uri = Uri.fromParts("package", BuildConfig.APPLICATION_ID, null);
                    intent.setData(uri);
                    startActivity(intent);
                    d.dismiss();
                })
                .setNegativeAction("Cancel", (d, w) -> {
                    d.dismiss();
                })
                .create()
                .show();
    }

    @ProvidePresenter
    SendTabPresenter providePresenter() {
        return presenterProvider.get();
    }
}
