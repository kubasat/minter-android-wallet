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

package network.minter.bipwallet.auth.views;

import android.view.View;
import android.widget.EditText;

import com.arellomobile.mvp.InjectViewState;

import java.util.Locale;

import javax.inject.Inject;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;
import network.minter.bipwallet.R;
import network.minter.bipwallet.advanced.models.SecretData;
import network.minter.bipwallet.advanced.repo.SecretStorage;
import network.minter.bipwallet.auth.AuthModule;
import network.minter.bipwallet.internal.Wallet;
import network.minter.bipwallet.internal.auth.AuthSession;
import network.minter.bipwallet.internal.di.annotations.ActivityScope;
import network.minter.bipwallet.internal.helpers.forms.InputGroup;
import network.minter.bipwallet.internal.mvp.MvpBasePresenter;
import network.minter.profile.models.LoginData;
import network.minter.profile.models.ProfileRequestResult;
import network.minter.profile.models.RegisterData;
import network.minter.profile.repo.ProfileAuthRepository;

import static network.minter.bipwallet.internal.ReactiveAdapter.convertToProfileErrorResult;
import static network.minter.bipwallet.internal.ReactiveAdapter.rxCallProfile;

/**
 * MinterWallet. 2018
 *
 * @author Eduard Maximovich <edward.vstock@gmail.com>
 */
@ActivityScope
@InjectViewState
public class RegisterPresenter extends MvpBasePresenter<AuthModule.RegisterView> {
    @Inject ProfileAuthRepository authRepo;
    @Inject SecretStorage secretStorage;
    @Inject AuthSession session;

    private RegisterData mRegisterData;
    private boolean mValid;

    @Inject
    public RegisterPresenter() {
        mRegisterData = new RegisterData();
    }

    @Override
    public void attachView(AuthModule.RegisterView view) {
        super.attachView(view);

        getViewState().setOnSubmit(this::onSubmit);
        getViewState().setOnTextChangedListener(new InputGroup.OnTextChangedListener() {
            @Override
            public void onTextChanged(EditText editText, boolean valid) {
                final String val = editText.getText().toString();
                switch (editText.getId()) {
                    case R.id.input_username:
                        mRegisterData.username = val;
                        if(!mRegisterData.username.isEmpty() && mRegisterData.username.charAt(0) == '@') {
                            mRegisterData.username = mRegisterData.username.substring(1);
                        }
                        break;
                    case R.id.inputPasswordRepeat:
                        mRegisterData.rawPassword = val;
                        break;
                    case R.id.inputEmail:
                        mRegisterData.email = val;
                        break;
                    case R.id.input_phone:
                        mRegisterData.phone = val;
                        break;
                }

                getViewState().validate(true);
            }
        });
        getViewState().setOnFormValidateListener(valid -> {
            getViewState().setEnableSubmit(valid);
            mValid = valid;
        });
    }

    private void onSubmit(View view) {
        if(!mValid) {
            return;
        }

        getViewState().setEnableSubmit(false);
        getViewState().hideKeyboard();
        getViewState().clearErrors();
        getViewState().showProgress();

        final SecretData secretData = SecretStorage.generateAddress();
        secretStorage.setEncryptionKey(mRegisterData.rawPassword);
        mRegisterData.language = Locale.getDefault().toString();
        try {
            mRegisterData.mainAddress = secretData.toAddressData(true, true, secretStorage.getEncryptionKey());
        } catch (Throwable t) {
            getViewState().onError(t);
            secretStorage.destroy();
            getViewState().setEnableSubmit(true);
            getViewState().hideProgress();
            return;
        }

        rxCallProfile(authRepo.register(mRegisterData.preparePassword()))
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeOn(Schedulers.io())
                .onErrorResumeNext(convertToProfileErrorResult())
                .subscribe(userResult -> {
                    if(!userResult.isSuccess()) {
                        secretStorage.destroy();
                        getViewState().setEnableSubmit(true);
                        getViewState().hideProgress();
                        getViewState().setResultError(userResult.error.message);
                        getViewState().setInputErrors(userResult.getError().getData());
                        return;
                    }

                    secretStorage.add(secretData);

                    if(userResult.data.confirmations != null && !userResult.data.confirmations.isEmpty()) {
                        ProfileRequestResult.Confirmation confirmation = userResult.data.confirmations.get(0);
//                        if(confirmation.type != null) {
                        // @TODO
//                            getViewState().startConfirmation(confirmation.endpoint);
//                            return;
//                        }
                    }

                    final LoginData loginData = new LoginData();
                    loginData.username = mRegisterData.username;
                    loginData.password = mRegisterData.password;

                    safeSubscribeIoToUi(rxCallProfile(authRepo.login(loginData)))
                            .subscribe(loginResult -> {
                                getViewState().hideProgress();
                                if(!loginResult.isSuccess()) {
                                    getViewState().setEnableSubmit(true);
                                    getViewState().setResultError(loginResult.error.message);
                                    getViewState().setInputErrors(loginResult.getError().getData());
                                    return;
                                }

                                session.login(loginResult.data.token.accessToken, loginResult.data, AuthSession.AuthType.Basic);
                                getViewState().startHome();
                            });

                }, Wallet.Rx.errorHandler(getViewState()));
    }
}
