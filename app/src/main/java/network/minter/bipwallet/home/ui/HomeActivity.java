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

package network.minter.bipwallet.home.ui;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.Service;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.IdRes;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.SparseArray;
import android.view.ViewGroup;

import com.arellomobile.mvp.presenter.InjectPresenter;
import com.arellomobile.mvp.presenter.ProvidePresenter;
import com.ittianyu.bottomnavigationviewex.BottomNavigationViewEx;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Provider;

import butterknife.BindView;
import butterknife.ButterKnife;
import network.minter.bipwallet.R;
import network.minter.bipwallet.home.HomeModule;
import network.minter.bipwallet.home.HomeTabFragment;
import network.minter.bipwallet.home.HomeTabsClasses;
import network.minter.bipwallet.home.views.HomePresenter;
import network.minter.bipwallet.internal.BaseMvpActivity;
import network.minter.bipwallet.internal.system.ActivityBuilder;
import network.minter.bipwallet.internal.system.BackPressedDelegate;
import network.minter.bipwallet.internal.system.BackPressedListener;
import timber.log.Timber;

/**
 * minter-android-wallet. 2018
 *
 * @author Eduard Maximovich <edward.vstock@gmail.com>
 */
public class HomeActivity extends BaseMvpActivity implements HomeModule.HomeView, BackPressedDelegate {

    @Inject Provider<HomePresenter> presenterProvider;
    @InjectPresenter HomePresenter presenter;
    @Inject @HomeTabsClasses List<Class<? extends HomeTabFragment>> tabsClasses;

    @BindView(R.id.navigation_bottom) BottomNavigationViewEx bottomNavigation;
    @BindView(R.id.home_pager) ViewPager homePager;

    private SparseArray<WeakReference<HomeTabFragment>> mActiveTabs = new SparseArray<>();
    private List<BackPressedListener> mBackPressedListeners = new ArrayList<>(1);
    private boolean mIsLowRamDevice = false;

    @Override
    public void setCurrentPage(int position) {
        runOnUiThread(() -> {
            homePager.setCurrentItem(position);
        });
    }

    @Override
    public void startUrl(@NonNull String url) {
        startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
    }

    @Override
    public void addBackPressedListener(BackPressedListener listener) {
        mBackPressedListeners.add(listener);
    }

    @Override
    public void removeBackPressedListener(BackPressedListener listener) {
        mBackPressedListeners.remove(listener);
    }

    @Override
    public void clearBackPressedListeners() {
        mBackPressedListeners.clear();
    }

    @Override
    public void onBackPressed() {
        for (BackPressedListener listener : mBackPressedListeners) {
            if (!listener.onBackPressed()) {
                return;
            }
        }

        super.onBackPressed();
    }

    @Override
    public void showProgress() {

    }

    @Override
    public void hideProgress() {

    }

    public void setCurrentPageById(@IdRes int tab) {
        int pos = presenter.getBottomPositionById(tab);
        setCurrentPage(pos);
    }

    @ProvidePresenter
    HomePresenter providePresenter() {
        return presenterProvider.get();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        for (int i = 0; i < mActiveTabs.size(); i++) {
            if (mActiveTabs.get(i) != null && mActiveTabs.get(i).get() != null) {
                mActiveTabs.get(i).get().onActivityResult(requestCode, resultCode, data);
            }
        }
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        if (homePager.getOffscreenPageLimit() > 1) {
            homePager.setOffscreenPageLimit(1);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        HomeModule.destroy();
        Timber.d("Destroy");
    }

    @Override
    public void onTrimMemory(int level) {
        super.onTrimMemory(level);
        for (int i = 0; i < mActiveTabs.size(); i++) {
            if (mActiveTabs.get(i) != null && mActiveTabs.get(i).get() != null) {
                mActiveTabs.get(i).get().onTrimMemory(level);
            }
        }

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        HomeModule.create(this).inject(this);
        super.onCreate(savedInstanceState);
        final ActivityManager am = ((ActivityManager) getSystemService(ACTIVITY_SERVICE));
        mIsLowRamDevice = am.isLowRamDevice();

        setContentView(R.layout.activity_home);
        ButterKnife.bind(this);
        presenter.handleExtras(getIntent());

        setupTabAdapter();
        setupBottomNavigation();
        Timber.d("HOME ACTIVITY CREATED");
    }

    private void setupTabAdapter() {
        final FragmentPagerAdapter adapter = new FragmentPagerAdapter(getSupportFragmentManager()) {
            @Override
            public Fragment getItem(int position) {
                Timber.d("Get item by position: %d", position);
                HomeTabFragment fragment = null;
                try {
                    fragment = tabsClasses.get(position).newInstance();
                    if (fragment == null) {
                        throw new NullPointerException("Wtf?");
                    }
                } catch (InstantiationException | IllegalAccessException e) {
                    e.printStackTrace();
                }

                if (fragment != null && getIntent() != null && getIntent().getExtras() != null) {
                    fragment.setArguments(getIntent().getExtras());
                }
                mActiveTabs.put(position, new WeakReference<>(fragment));

                if (homePager.getCurrentItem() == position && fragment != null) {
                    //                    fragment.createToolbarMenuOptions(toolbar.getMenu());
                    //                    fragment.onTabSelected();
                    //                    toolbar.setTitle(fragment.getTitle());
                }

                return fragment;
            }

            @NonNull
            @Override
            public Object instantiateItem(ViewGroup container, int position) {
                HomeTabFragment fragment = (HomeTabFragment) super.instantiateItem(container, position);
                Timber.d("Instantiate item %s by position: %d", fragment.getClass().getSimpleName(), position);

                mActiveTabs.put(position, new WeakReference<>(fragment));
                return fragment;
            }

            @Override
            public void destroyItem(ViewGroup container, int position, Object object) {
                super.destroyItem(container, position, object);
                mActiveTabs.delete(position);
            }

            @Override
            public int getCount() {
                return tabsClasses.size();
            }
        };


        homePager.setOffscreenPageLimit(mIsLowRamDevice ? 1 : 4);
        homePager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
            }

            @Override
            public void onPageSelected(int position) {
                //                toolbar.getMenu().clear();
                presenter.onPageSelected(position);
                if (mActiveTabs.get(position) != null && mActiveTabs.get(position).get() != null) {
                    //                    mActiveTabs.get(position).get().createToolbarMenuOptions(toolbar.getMenu());
                    mActiveTabs.get(position).get().onTabSelected();

                    //                    toolbar.setTitle(mActiveTabs.get(position).get().getTitle());
                }

                bottomNavigation.setSelectedItemId(presenter.getBottomIdByPosition(position));
            }

            @Override
            public void onPageScrollStateChanged(int state) {
            }
        });
        homePager.setAdapter(adapter);
    }

    private void setupBottomNavigation() {
        bottomNavigation.enableShiftingMode(false);
        bottomNavigation.enableItemShiftingMode(false);
        bottomNavigation.enableAnimation(true);
        bottomNavigation.setTextVisibility(true);


        bottomNavigation.setOnNavigationItemSelectedListener(item -> {
            runOnUiThread(() -> homePager.setCurrentItem(presenter.getBottomPositionById(item.getItemId())));
            return true;
        });
    }

    public static final class Builder extends ActivityBuilder {
        public Builder(@NonNull Activity from) {
            super(from);
        }

        public Builder(@NonNull Fragment from) {
            super(from);
        }

        public Builder(@NonNull Service from) {
            super(from);
        }

        @Override
        protected Class<?> getActivityClass() {
            return HomeActivity.class;
        }
    }
}
