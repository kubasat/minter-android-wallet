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

package network.minter.bipwallet.coins.ui;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.IdRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.arellomobile.mvp.presenter.InjectPresenter;
import com.arellomobile.mvp.presenter.ProvidePresenter;

import javax.inject.Inject;
import javax.inject.Provider;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;
import network.minter.bipwallet.R;
import network.minter.bipwallet.coins.CoinsTabModule;
import network.minter.bipwallet.coins.views.CoinsTabPresenter;
import network.minter.bipwallet.exchange.ui.ConvertCoinActivity;
import network.minter.bipwallet.home.HomeModule;
import network.minter.bipwallet.home.HomeTabFragment;
import network.minter.bipwallet.home.ui.HomeActivity;
import network.minter.bipwallet.internal.views.widgets.BipCircleImageView;
import network.minter.bipwallet.tx.ui.TransactionListActivity;
import network.minter.explorer.MinterExplorerApi;
import timber.log.Timber;

/**
 * minter-android-wallet. 2018
 * @author Eduard Maximovich <edward.vstock@gmail.com>
 */
public class CoinsTabFragment extends HomeTabFragment implements CoinsTabModule.CoinsTabView {

    @Inject Provider<CoinsTabPresenter> presenterProvider;
    @InjectPresenter CoinsTabPresenter presenter;
    @BindView(R.id.user_avatar) BipCircleImageView avatar;
    @BindView(R.id.username) TextView username;
    @BindView(R.id.balance_int) TextView balanceInt;
    @BindView(R.id.balance_fractions) TextView balanceFract;
    @BindView(R.id.balance_coin_name) TextView balanceCoinName;
    @BindView(R.id.list) RecyclerView list;
    @BindView(R.id.container_swipe_refresh) SwipeRefreshLayout swipeRefreshLayout;
    private Unbinder mUnbinder;

    @Override
    public void onAttach(Context context) {
        HomeModule.getComponent().inject(this);
        super.onAttach(context);
    }

    @Override
    public void setOnRefreshListener(SwipeRefreshLayout.OnRefreshListener listener) {
        swipeRefreshLayout.setOnRefreshListener(listener);
    }

    @Override
    public void showRefreshProgress() {
        swipeRefreshLayout.setRefreshing(true);
    }

    @Override
    public void hideRefreshProgress() {
        swipeRefreshLayout.setRefreshing(false);
    }

    @Override
    public void startExplorer(String hash) {
        getActivity().startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(MinterExplorerApi.FRONT_URL + "/transactions/" + hash)));
    }

    @Override
    public void scrollTop() {
        list.post(() -> list.scrollToPosition(0));
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_tab_coins, container, false);
        mUnbinder = ButterKnife.bind(this, view);
        return view;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        HomeModule.getComponent().inject(this);
        super.onCreate(savedInstanceState);
    }

    @Override
    public void onTrimMemory(int level) {
        super.onTrimMemory(level);
        presenter.onTrimMemory(level);
    }

    @Override
    public void setAvatar(String url) {
        if (url == null) {
            return;
        }

        avatar.setImageUrl(url);
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        presenter.onLowMemory();
        avatar.setImageDrawable(null);
        Timber.d("OnLowMemory");
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mUnbinder.unbind();
        Timber.d("Destroy");
    }

    @Override
    public void setUsername(CharSequence name) {
        username.setText(name);
    }

    @SuppressLint("SetTextI18n")
    @Override
    public void setBalance(String intPart, String fractionalPart, CharSequence coinName) {
        runOnUiThread(() -> {
            if (balanceInt != null) {
                balanceInt.setText(String.valueOf(intPart));
                balanceFract.setText("." + fractionalPart);
                balanceCoinName.setText(coinName);
            }
        });
    }

    @Override
    public void setAdapter(RecyclerView.Adapter<?> adapter) {
        list.setLayoutManager(new LinearLayoutManager(getActivity()));
        list.setAdapter(adapter);
//        list.setNestedScrollingEnabled(false);
    }

    @Override
    public void setOnAvatarClick(View.OnClickListener listener) {
        avatar.setOnClickListener(listener);
    }

    @Override
    public void startTransactionList() {
        startActivity(new Intent(getActivity(), TransactionListActivity.class));
    }

    @Override
    public void hideAvatar() {
        avatar.setVisibility(View.GONE);
    }

    @Override
    public void startConvertCoins() {
        getActivity().startActivity(new Intent(getActivity(), ConvertCoinActivity.class));
    }

    @Override
    public void startTab(@IdRes int tab) {
        if (getActivity() instanceof HomeActivity) {
            ((HomeActivity) getActivity()).setCurrentPageById(tab);
        }
    }

    @ProvidePresenter
    CoinsTabPresenter providePresenter() {
        return presenterProvider.get();
    }
}
