package org.schabi.newpipe;

import android.support.annotation.Nullable;
import android.text.TextUtils;

import org.schabi.newpipe.extractor.exceptions.ReCaptchaException;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;


/*
 * Created by Christian Schabesberger on 28.01.16.
 *
 * Copyright (C) Christian Schabesberger 2016 <chris.schabesberger@mailbox.org>
 * Downloader.java is part of NewPipe.
 *
 * NewPipe is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * NewPipe is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with NewPipe.  If not, see <http://www.gnu.org/licenses/>.
 */

public class Downloader implements org.schabi.newpipe.extractor.Downloader {
    public static final String USER_AGENT = "Mozilla/5.0 (Windows NT 6.1; WOW64; rv:43.0) Gecko/20100101 Firefox/43.0";

    private static Downloader instance;
    private String mCookies;
    private OkHttpClient client;

    private Downloader(OkHttpClient.Builder builder) {
        this.client = builder
                .readTimeout(30, TimeUnit.SECONDS)
                //.cache(new Cache(new File(context.getExternalCacheDir(), "okhttp"), 16 * 1024 * 1024))
                .build();
    }

    /**
     * It's recommended to call exactly once in the entire lifetime of the application.
     *
     * @param builder if null, default builder will be used
     */
    public static Downloader init(@Nullable OkHttpClient.Builder builder) {
        return instance = new Downloader(builder != null ? builder : new OkHttpClient.Builder());
    }

    public static Downloader getInstance() {
        return instance;
    }

    public String getCookies() {
        return mCookies;
    }

    public void setCookies(String cookies) {
        mCookies = cookies;
    }

    /**
     * Download the text file at the supplied URL as in download(String),
     * but set the HTTP header field "Accept-Language" to the supplied string.
     *
     * @param siteUrl  the URL of the text file to return the contents of
     * @param language the language (usually a 2-character code) to set as the preferred language
     * @return the contents of the specified text file
     */
    @Override
    public String download(String siteUrl, String language) throws IOException, ReCaptchaException {
        Map<String, String> requestProperties = new HashMap<>();
        requestProperties.put("Accept-Language", language);
        return download(siteUrl, requestProperties);
    }

    /**
     * Download the text file at the supplied URL as in download(String),
     * but set the HTTP headers included in the customProperties map.
     *
     * @param siteUrl          the URL of the text file to return the contents of
     * @param customProperties set request header properties
     * @return the contents of the specified text file
     * @throws IOException
     */
    @Override
    public String download(String siteUrl, Map<String, String> customProperties) throws IOException, ReCaptchaException {
        final Request.Builder requestBuilder = new Request.Builder()
                .method("GET", null).url(siteUrl)
                .addHeader("User-Agent", USER_AGENT);

        for (Map.Entry<String, String> header : customProperties.entrySet()) {
            requestBuilder.addHeader(header.getKey(), header.getValue());
        }

        if (!TextUtils.isEmpty(mCookies)) {
            requestBuilder.addHeader("Cookie", mCookies);
        }

        final Request request = requestBuilder.build();
        final Response response = client.newCall(request).execute();
        final ResponseBody body = response.body();

        if (response.code() == 429) {
            throw new ReCaptchaException("reCaptcha Challenge requested");
        }

        if (body == null) {
            response.close();
            return null;
        }

        return body.string();
    }

    /**
     * Download (via HTTP) the text file located at the supplied URL, and return its contents.
     * Primarily intended for downloading web pages.
     *
     * @param siteUrl the URL of the text file to download
     * @return the contents of the specified text file
     */
    @Override
    public String download(String siteUrl) throws IOException, ReCaptchaException {
        return download(siteUrl, Collections.emptyMap());
    }
}
