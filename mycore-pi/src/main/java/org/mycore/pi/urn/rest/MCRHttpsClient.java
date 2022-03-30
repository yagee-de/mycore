/*
 * This file is part of ***  M y C o R e  ***
 * See http://www.mycore.de/ for details.
 *
 * MyCoRe is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MyCoRe is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MyCoRe.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.mycore.pi.urn.rest;

import org.apache.http.HttpEntity;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpEntityEnclosingRequestBase;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.client.methods.HttpPatch;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.ssl.SSLContexts;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mycore.common.config.MCRConfiguration2;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.function.Supplier;

/**
 * Created by chi on 08.05.17.
 *
 * @author Huu Chi Vu
 */
public class MCRHttpsClient {
    final private static Logger LOGGER = LogManager.getLogger(MCRHttpsClient.class);

    private static RequestConfig noRedirect() {
        return RequestConfig
            .copy(RequestConfig.DEFAULT)
            .setRedirectsEnabled(false)
            .build();
    }

    public static CloseableHttpClient getHttpsClient() {
        CredentialsProvider provider = new BasicCredentialsProvider();
        UsernamePasswordCredentials credentials = new UsernamePasswordCredentials(MCRConfiguration2
            .getString("MCR.PI.DNB.Credentials.Login").orElse("test"),
            MCRConfiguration2.getString("MCR.PI.DNB.Credentials.Password").orElse("test"));
        provider.setCredentials(AuthScope.ANY, credentials);

        int timeout = 5;
        RequestConfig config = RequestConfig.custom()
            .setConnectTimeout(timeout * 1000)
            .setConnectionRequestTimeout(timeout * 1000)
            .setSocketTimeout(timeout * 1000).build();

        return HttpClientBuilder
            .create().setDefaultCredentialsProvider(provider)
            .setDefaultRequestConfig(config)
            .setSSLContext(SSLContexts.createSystemDefault())
            .build();
    }

    public static CloseableHttpResponse head(String url) {
        HttpHead httpHead = new HttpHead(url);
        try (CloseableHttpClient httpClient = getHttpsClient()) {
            return httpClient.execute(httpHead);
        } catch (IOException e) {
            LOGGER.error("There is a problem or the connection was aborted for URL: {}", url, e);
        }

        return null;
    }

    public static CloseableHttpResponse put(String url, String contentType, String data) {
        return request(HttpPut::new, url, contentType, new StringEntity(data, "UTF-8"));
    }

    public static CloseableHttpResponse post(String url, String contentType, String data) {
        return request(HttpPost::new, url, contentType, new StringEntity(data, "UTF-8"));
    }

    public static CloseableHttpResponse patch(String url, String contentType, String data) {
        return request(HttpPatch::new, url, contentType, new StringEntity(data, "UTF-8"));
    }

    public static <R extends HttpEntityEnclosingRequestBase> CloseableHttpResponse request(
        Supplier<R> requestSupp, String url,
        String contentType, HttpEntity entity) {

        try (CloseableHttpClient httpClient = getHttpsClient()) {
            R request = requestSupp.get();
            request.setURI(new URI(url));
            request.setHeader("content-type", contentType);
            request.setConfig(noRedirect());
            request.setEntity(entity);

            return httpClient.execute(request);
        } catch (URISyntaxException e) {
            LOGGER.error("Wrong format for URL: {}", url, e);
        } catch (ClientProtocolException e) {
            LOGGER.error("There is a HTTP protocol error for URL: {}", url, e);
        } catch (IOException e) {
            LOGGER.error("There is a problem or the connection was aborted for URL: {}", url, e);
        }

        return null;
    }
}
