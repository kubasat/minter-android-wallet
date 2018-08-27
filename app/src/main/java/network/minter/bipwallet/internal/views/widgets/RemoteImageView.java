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

package network.minter.bipwallet.internal.views.widgets;

import android.net.Uri;
import android.support.annotation.DimenRes;
import android.support.annotation.DrawableRes;

import network.minter.bipwallet.internal.common.annotations.Dp;


/**
 * Wallet. 2018
 *
 * @author Eduard Maximovich <edward.vstock@gmail.com>
 */
public interface RemoteImageView {
    void setImageUrl(String url, float size);
    void setImageUrl(Uri uri, @DimenRes int resId);
    void setImageUrl(String url, @DimenRes int resId);
    void setImageUrl(Uri uri, float size);
    void setImageUrl(Uri uri);
    void setImageUrl(String url);
    void setImageUrlFallback(String url, @DrawableRes int fallbackResId);
    void setImageUrlFallback(String url, String fallbackUrl);
    void setImageUrl(RemoteImageContainer imageUrlContainer);
    void setImageUrl(RemoteImageContainer imageUrlContainer, @Dp float size);
    void setImageUrl(RemoteImageContainer imageUrlContainer, @DimenRes int resId);
}
