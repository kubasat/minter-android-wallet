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
package network.minter.bipwallet.exchange.views;

import android.support.annotation.CallSuper;
import android.view.View;
import android.widget.EditText;

import com.annimon.stream.Optional;
import com.annimon.stream.Stream;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import io.reactivex.BackpressureStrategy;
import io.reactivex.Observable;
import io.reactivex.ObservableSource;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.functions.Function;
import io.reactivex.schedulers.Schedulers;
import io.reactivex.subjects.BehaviorSubject;
import network.minter.bipwallet.R;
import network.minter.bipwallet.advanced.models.AccountItem;
import network.minter.bipwallet.advanced.models.SecretData;
import network.minter.bipwallet.advanced.models.UserAccount;
import network.minter.bipwallet.advanced.repo.AccountStorage;
import network.minter.bipwallet.advanced.repo.SecretStorage;
import network.minter.bipwallet.apis.explorer.CachedExplorerTransactionRepository;
import network.minter.bipwallet.exchange.ExchangeModule;
import network.minter.bipwallet.exchange.models.ConvertTransactionData;
import network.minter.bipwallet.exchange.ui.WalletTxConvertStartDialog;
import network.minter.bipwallet.internal.Wallet;
import network.minter.bipwallet.internal.data.CachedRepository;
import network.minter.bipwallet.internal.dialogs.WalletConfirmDialog;
import network.minter.bipwallet.internal.dialogs.WalletProgressDialog;
import network.minter.bipwallet.internal.exceptions.BCResponseException;
import network.minter.bipwallet.internal.mvp.MvpBasePresenter;
import network.minter.blockchain.models.BCResult;
import network.minter.blockchain.models.CountableData;
import network.minter.blockchain.models.TransactionSendResult;
import network.minter.blockchain.models.operational.Transaction;
import network.minter.blockchain.models.operational.TransactionSign;
import network.minter.blockchain.repo.BlockChainAccountRepository;
import network.minter.blockchain.repo.BlockChainCoinRepository;
import network.minter.explorer.models.HistoryTransaction;
import timber.log.Timber;

import static java.math.BigDecimal.ROUND_DOWN;
import static network.minter.bipwallet.internal.ReactiveAdapter.convertToBcErrorResult;
import static network.minter.bipwallet.internal.ReactiveAdapter.rxCallBc;
import static network.minter.bipwallet.internal.common.Preconditions.firstNonNull;
import static network.minter.bipwallet.internal.helpers.MathHelper.bdGT;
import static network.minter.bipwallet.internal.helpers.MathHelper.bdHuman;
import static network.minter.bipwallet.internal.helpers.MathHelper.bdLTE;
import static network.minter.blockchain.models.BCResult.ResultCode.CoinDoesNotExists;

/**
 * minter-android-wallet. 2018
 * @author Eduard Maximovich <edward.vstock@gmail.com>
 */
public abstract class BaseCoinTabPresenter<V extends ExchangeModule.BaseCoinTabView> extends MvpBasePresenter<V> {
    protected final SecretStorage mSecretStorage;
    protected final CachedRepository<UserAccount, AccountStorage> mAccountStorage;
    protected final CachedRepository<List<HistoryTransaction>, CachedExplorerTransactionRepository> mTxRepo;
    protected final BlockChainCoinRepository mCoinRepo;
    protected final BlockChainAccountRepository mAccountRepo;

    private AccountItem mAccount;
    private String mGetCoin = null;
    private BigDecimal mSpendAmount = new BigDecimal(0);
    private BigDecimal mGetAmount = new BigDecimal(0);
    private BehaviorSubject<Boolean> mInputChange;
    private String mGasCoin;
    private List<AccountItem> mAccounts = new ArrayList<>(1);
    private boolean mUseMax = false;
    private AtomicBoolean mEnableUseMax = new AtomicBoolean(false);

    public BaseCoinTabPresenter(
            SecretStorage secretStorage,
            CachedRepository<UserAccount, AccountStorage> accountStorage,
            CachedRepository<List<HistoryTransaction>, CachedExplorerTransactionRepository> txRepo,
            BlockChainCoinRepository coinRepo,
            BlockChainAccountRepository accountRepo
    ) {
        mSecretStorage = secretStorage;
        mAccountStorage = accountStorage;
        mAccountRepo = accountRepo;
        mTxRepo = txRepo;
        mCoinRepo = coinRepo;
    }

    @Override
    protected void onFirstViewAttach() {
        super.onFirstViewAttach();

        safeSubscribeIoToUi(mAccountStorage.observe())
                .subscribe(res -> {
                    if (!res.isEmpty()) {
                        mAccounts = res.getAccounts();
                        mAccount = res.getAccounts().get(0);
                        onAccountSelected(mAccount, true);
                    }
                });

        mInputChange = BehaviorSubject.create();
        unsubscribeOnDestroy(mInputChange
                .toFlowable(BackpressureStrategy.LATEST)
                .debounce(200, TimeUnit.MILLISECONDS)
                .subscribe(this::onAmountChangedInternal));


        getViewState().setSubmitEnabled(false);
        getViewState().setFormValidationListener(valid -> getViewState().setSubmitEnabled(valid));
        getViewState().setTextChangedListener(this::onInputChanged);
        getViewState().setOnClickSelectAccount(this::onClickSelectAccount);
        getViewState().setOnClickMaximum(this::onClickMaximum);
        getViewState().setOnClickSubmit(this::onClickSubmit);
    }

    protected abstract boolean isAmountForGetting();

    @CallSuper
    protected void setCalculation(String calculation) {
        getViewState().setCalculation(calculation);
    }

    private Optional<AccountItem> findAccountByCoin(String coin) {
        return Stream.of(mAccounts)
                .filter(item -> item.getCoin().equals(coin.toUpperCase()))
                .findFirst();
    }

    private void onClickSubmit(View view) {
        if (mAccount == null || mGetCoin == null || mGetAmount == null || mSpendAmount == null) {
            return;
        }

        Timber.d("Use max: %b", mUseMax);

        getViewState().startDialog(ctx -> new WalletTxConvertStartDialog.Builder(ctx, "Convert coin")
                .setAmount(isAmountForGetting() ? mGetAmount : mSpendAmount)
                .setLabel(isAmountForGetting() ? "Buy" : "Spend")
                .setAmountPostfix(isAmountForGetting() ? mGetCoin.toUpperCase() : mAccount.getCoin().toUpperCase())
                .setFromCoin(mAccount.getCoin())
                .setToCoin(mGetCoin)
                .setPositiveAction("Convert!", (d, w) -> {
                    final ConvertTransactionData txData = new ConvertTransactionData(
                            mUseMax ? ConvertTransactionData.Type.SellAll : !isAmountForGetting() ? ConvertTransactionData.Type.Sell : ConvertTransactionData.Type.Buy,
                            mGasCoin,
                            mAccount.getCoin(),
                            mGetCoin,
                            isAmountForGetting() ? mGetAmount : mSpendAmount
                    );

                    onStartExecuteTransaction(txData);
                })
                .setNegativeAction("Cancel")
                .create());
    }

    private void onStartExecuteTransaction(final ConvertTransactionData txData) {
        getViewState().startDialog(ctx -> {
            WalletProgressDialog dialog = new WalletProgressDialog.Builder(ctx, "Exchanging")
                    .setText("Please, wait few seconds")
                    .create();

            dialog.setCancelable(false);

            safeSubscribeIoToUi(rxCallBc(mAccountRepo.getTransactionCount(mAccount.address)))
                    .onErrorResumeNext(convertToBcErrorResult())
                    .switchMap((Function<BCResult<CountableData>, ObservableSource<BCResult<TransactionSendResult>>>) cntRes -> {
                        if (!cntRes.isSuccess()) {
                            return Observable.just(BCResult.copyError(cntRes));
                        }
                        final BigInteger nonce = cntRes.result.count.add(new BigInteger("1"));
                        final Transaction tx = txData.build(nonce);
                        final SecretData data = mSecretStorage.getSecret(mAccount.address);
                        final TransactionSign sign = tx.sign(data.getPrivateKey());
                        data.cleanup();

                        return safeSubscribeIoToUi(rxCallBc(mAccountRepo.sendTransaction(sign)))
                                .onErrorResumeNext(convertToBcErrorResult());

                    }).subscribe(BaseCoinTabPresenter.this::onSuccessExecuteTransaction, Wallet.Rx.errorHandler(getViewState()));


            return dialog;
        });
    }

    private void onSuccessExecuteTransaction(BCResult<TransactionSendResult> result) {
        if (!result.isSuccess()) {
            onErrorExecuteTransaction(result);
            return;
        }

        mAccountStorage.update(true);
        mTxRepo.update(true);
        getViewState().startDialog(ctx -> new WalletConfirmDialog.Builder(ctx, "Success!")
                .setText("Coins successfully converted!")
                .setPositiveAction("View transaction", (d, v) -> {
                    getViewState().startExplorer(result.result.txHash.toString());
                    getViewState().finish();
                    d.dismiss();
                })
                .setNegativeAction("Close", (d, w) -> {
                    getViewState().finish();
                    d.dismiss();
                })
                .create());
    }

    private void onErrorExecuteTransaction(BCResult<?> errorResult) {
        Timber.e(errorResult.message, "Unable to send transaction");
        getViewState().startDialog(ctx -> new WalletConfirmDialog.Builder(ctx, "Unable to send transaction")
                .setText(errorResult.message)
                .setPositiveAction("Close")
                .create());
    }

    private void onClickMaximum(View view) {
        if (isAmountForGetting()) {
            return;
        }

        getViewState().setAmount(bdHuman(mAccount.balance));
        mUseMax = true;
    }

    private void onClickSelectAccount(View view) {
        getViewState().startAccountSelector(mAccountStorage.getData().getAccounts(), accountItem -> {
            onAccountSelected(accountItem, false);
        });
    }

    private void onInputChanged(EditText editText, boolean valid) {
        String text = editText.getText().toString();

        switch (editText.getId()) {
            case R.id.input_incoming_coin:
                mGetCoin = text;
                mInputChange.onNext(isAmountForGetting());
                break;
            case R.id.input_amount:
                String amountText = text
                        .replaceAll("\\s", "")
                        .replaceAll("[,.]+", ".")
                        .replace(",", ".");
                if (amountText.isEmpty()) {
                    amountText = "0";
                }
                if (amountText.equals(".")) {
                    amountText = "0";
                } else if (amountText.substring(0, 1).equals(".")) {
                    amountText = "0" + amountText;
                }
                if (amountText.substring(amountText.length() - 1).equals(".")) {
                    amountText = amountText + "0";
                }

                Timber.d("Amount: %s", amountText);
                final BigDecimal am = new BigDecimal(amountText);

                if (isAmountForGetting()) {
                    mGetAmount = am;
                } else {
                    mUseMax = false;
                    mSpendAmount = am;
                }
                getViewState().setSubmitEnabled(am.compareTo(mAccount.balance) <= 0);
                mInputChange.onNext(isAmountForGetting());
                break;
        }
    }

    /**
     * 1. Getting currency
     * 2. Print to calculation "value + commission"
     * 3. Write mSpendAmount = value
     * 4.
     * <pre>
     * {@code
     *      if(value < mAccount.getBalanceBase()) {
     *        trigger error: not enough balance
     *      } else {
     *        enabling: submit action
     *      }
     * }
     * </pre>
     * <p>
     * 5.
     * <pre>
     * {@code
     * if(value+commission < mAccount.getBalanceBase()) {
     *     nextAccount: find suitable account with second coin (example: current BIP, convert to: SUPERCOIN - find account for SUPERCOIN)
     *     if(!nextAccount || nextAccount.getBalanceBase() < value+commission) {
     *         trigger error: not enough balance
     *     } else {
     *         enabling: submit action
     *     }
     * }
     * }
     * </pre>
     * @return true if found another account for gas coin: false otherwise
     */
    private boolean findSuitableAccountForCommission(BigDecimal amountWithCommission, String suitableCoin) {
        Optional<AccountItem> probablySuitableAccount = findAccountByCoin(suitableCoin);
        if (!probablySuitableAccount.isPresent() || bdGT(amountWithCommission, probablySuitableAccount.get().getBalanceBase())) {
            mGasCoin = mAccount.getCoin();
            return false;
        }

        mGasCoin = suitableCoin;
        return true;
    }

    private boolean checkEnoughBalance(BigDecimal amount) {
        boolean enoughBalance = bdLTE(amount, mAccount.getBalanceBase());
        getViewState().setSubmitEnabled(enoughBalance);
        return enoughBalance;
    }

    private void onAmountChangedInternal(Boolean incoming) {
        if (mGetCoin == null) {
            Timber.i("Can't exchange: coin is not set");
            return;
        }

        if (incoming) {
            // get
            rxCallBc(mCoinRepo.getCoinExchangeCurrencyToBuy(mAccount.getCoin(), mGetAmount, mGetCoin))
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .onErrorResumeNext(convertToBcErrorResult())
                    .doOnSubscribe(this::unsubscribeOnDestroy)
                    .subscribe(res -> {
                        if (!res.isSuccess()) {
                            if (res.code == BCResult.ResultCode.EmptyResponse || res.statusCode == 404 || res.code == CoinDoesNotExists) {
                                getViewState().setError("income_coin", firstNonNull(res.message, "Coin does not exists"));
                                mEnableUseMax.set(false);
                                return;
                            }
                            Timber.w(new BCResponseException(res));
                            return;
                        }
                        mEnableUseMax.set(true);
                        mSpendAmount = res.result.getAmount();
                        getViewState().setError("income_coin", null);
                        setCalculation(String.format("%s %s", res.result.getAmountWithCommission().setScale(4, ROUND_DOWN).toPlainString(), mGetCoin));

                        if (!checkEnoughBalance(mSpendAmount)) {
                            return;
                        }

                        if (!findSuitableAccountForCommission(res.result.getAmountWithCommission(), mAccount.getCoin())) {
                            if (!findSuitableAccountForCommission(res.result.getAmountWithCommission(), mGetCoin)) {
                                getViewState().setError("income_coin", "Not enough balance");
                            }
                        }

                    }, t -> {
                        Timber.e(t, "Unable to get currency");
                        getViewState().setError("income_coin", "Unable to get currency");
                    });
        } else {
            // spend
            rxCallBc(mCoinRepo.getCoinExchangeCurrencyToSell(mAccount.getCoin(), mSpendAmount, mGetCoin))
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .onErrorResumeNext(convertToBcErrorResult())
                    .doOnSubscribe(this::unsubscribeOnDestroy)
                    .subscribe(res -> {
                        if (!res.isSuccess()) {
                            //FIXME: check all possible codes
                            if (res.code == BCResult.ResultCode.EmptyResponse || res.statusCode == 404 || res.code == CoinDoesNotExists) {
                                getViewState().setError("income_coin", firstNonNull(res.message, "Coin does not exists"));
                                mEnableUseMax.set(false);
                                return;
                            }
                            Timber.w(new BCResponseException(res));
                            return;
                        }
                        mEnableUseMax.set(true);
                        getViewState().setError("income_coin", null);
                        setCalculation(String.format("%s %s", res.result.getAmount().setScale(4, ROUND_DOWN).toPlainString(), mGetCoin));
                        mGetAmount = res.result.getAmount();
                        if (!checkEnoughBalance(mSpendAmount)) {
                            return;
                        }
                        if (!findSuitableAccountForCommission(mSpendAmount, mAccount.getCoin())) {
                            if (!findSuitableAccountForCommission(mSpendAmount, mGetCoin)) {
                                getViewState().setError("income_coin", "Not enough balance");
                            }
                        }
                    }, t -> {
                        Timber.e(t, "Unable to get currency");
                        getViewState().setError("income_coin", "Unable to get currency");
                    });
        }
    }

    private void onAccountSelected(AccountItem accountItem, boolean initial) {
        if (accountItem == null) return;

        mGasCoin = accountItem.getCoin();
        mAccount = accountItem;
        getViewState().setMaximumEnabled(accountItem.balance.compareTo(new BigDecimal(0)) > 0);
        getViewState().setOutAccountName(String.format("%s (%s)", accountItem.getCoin().toUpperCase(), accountItem.balance.stripTrailingZeros().toPlainString()));

        if (!initial) {
            mInputChange.onNext(isAmountForGetting());
        }
    }


}
