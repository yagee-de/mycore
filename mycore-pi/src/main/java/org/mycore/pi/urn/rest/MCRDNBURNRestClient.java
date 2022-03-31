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

import org.apache.http.Header;
import org.apache.http.HttpStatus;
import org.apache.http.StatusLine;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mycore.common.config.MCRConfiguration2;
import org.mycore.pi.MCRPIRegistrationInfo;

import java.net.URL;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.Optional;
import java.util.function.Function;

import static org.apache.http.entity.ContentType.APPLICATION_JSON;

/**
 * Created by chi on 25.01.17.
 *
 * @author Huu Chi Vu
 * @author shermann
 */
public class MCRDNBURNRestClient {

    private static final Logger LOGGER = LogManager.getLogger();

    private final Function<MCRPIRegistrationInfo, MCRURNJsonBundle> jsonProvider;

    private final Optional<UsernamePasswordCredentials> credentials;

    /**
     * Creates a new operator with the given configuration.
     *
     * @param bundleProvider
     */
    public MCRDNBURNRestClient(Function<MCRPIRegistrationInfo, MCRURNJsonBundle> bundleProvider) {
        this(bundleProvider, Optional.empty());
    }

    /**
     * @param bundleProvider
     * @param credentials
     * */
    public MCRDNBURNRestClient(Function<MCRPIRegistrationInfo, MCRURNJsonBundle> bundleProvider,
        Optional<UsernamePasswordCredentials> credentials) {
        this.jsonProvider = bundleProvider;
        this.credentials = credentials;
    }

    @Deprecated
    protected String getBaseServiceURL(MCRPIRegistrationInfo urn) {
        return this.getBaseServiceURL();
    }

    /**
     * Returns the base url of the urn registration service.
     *
     * @return the base url as set in mycore property MCR.PI.URNGranular.API.BaseURL
     * */
    protected String getBaseServiceURL() {
        return MCRConfiguration2.getString("MCR.PI.URNGranular.API.BaseURL")
            .orElse("https://api.nbn-resolving.org/sandbox/v2/") + "urns/";
    }

    /**
     * Returns the base url for checking the existence of a given urn.
     * @param urn the {@link MCRPIRegistrationInfo} to test
     *
     * @return the request url
     * */
    protected String getBaseServiceCheckExistsURL(MCRPIRegistrationInfo urn) {
        return getBaseServiceURL() + "urn/" + urn.getIdentifier();
    }

    /**
     * Returns the url for updating the urls assigned to a given urn.
     *
     * @param urn the urn
     * @return the url for updating the urls
     * */
    protected String getUpdateURL(MCRPIRegistrationInfo urn) {
        return getBaseServiceURL() + "urn/" + urn.getIdentifier() + "/my-urls/";
    }

    /**
     * Please see list of status codes and their meaning:
     * <br><br>
     * 204 No Content: URN is in database. No further information asked.<br>
     * 301 Moved Permanently: The given URN is replaced with a newer version.
     * This newer version should be used instead.<br>
     * 404 Not Found: The given URN is not registered in system.<br>
     * 410 Gone: The given URN is registered in system but marked inactive.<br>
     *
     * @return the status code of the request
     */
    public Optional<Date> register(MCRPIRegistrationInfo urn) {
        String url = getBaseServiceCheckExistsURL(urn);
        CloseableHttpResponse response = MCRHttpsClient.head(url);

        StatusLine statusLine = response.getStatusLine();

        if (statusLine == null) {
            LOGGER.warn("HEAD request for {} returns no status line.", url);
            return Optional.empty();
        }

        int headStatus = statusLine.getStatusCode();

        String identifier = urn.getIdentifier();
        switch (headStatus) {
            case HttpStatus.SC_NO_CONTENT:
                LOGGER.info("URN {} is in database. No further information asked", identifier);
                LOGGER.info("Performing update of url");
                return update(urn);
            case HttpStatus.SC_NOT_FOUND:
                LOGGER.info("URN {} is not registered", identifier);
                return registerNew(urn);
            case HttpStatus.SC_MOVED_PERMANENTLY:
                LOGGER.warn("URN {} is replaced with a newer version. This newer version should be used instead",
                    identifier);
                break;
            case HttpStatus.SC_GONE:
                LOGGER.warn("URN {} is registered but marked inactive", identifier);
                break;
            default:
                LOGGER.warn("Could not handle request for urn '{}' status code '{}'", identifier, headStatus);
                break;
        }

        return Optional.empty();
    }

    /**
     * Registers a new URN.
     * <br><br>
     * 201 Created: URN-Record is successfully created.<br>
     * 303 See other: At least one of the given URLs is already registered under another URN,
     * which means you should use this existing URN instead of assigning a new one<br>
     * 409 Conflict: URN-Record already exists and can not be created again.<br>
     *
     * @return the status code of the request
     */
    private Optional<Date> registerNew(MCRPIRegistrationInfo urn) {
        MCRURNJsonBundle bundle = jsonProvider.apply(urn);
        String json = bundle.toJSON(MCRURNJsonBundle.Format.register);
        String baseServiceURL = getBaseServiceURL();
        CloseableHttpResponse response = MCRHttpsClient.post(baseServiceURL, APPLICATION_JSON.toString(), json,
            credentials);

        StatusLine statusLine = response.getStatusLine();

        if (statusLine == null) {
            LOGGER.warn("POST request for {} returns no status line", baseServiceURL);
            return Optional.empty();
        }

        int postStatus = statusLine.getStatusCode();

        String identifier = urn.getIdentifier();
        URL url = bundle.getUrl();
        switch (postStatus) {
            case HttpStatus.SC_CREATED:
                LOGGER.info("URN {} registered to {}", identifier, url);
                return Optional.ofNullable(response.getFirstHeader("Last-Modified"))
                    .map(Header::getValue)
                    .map(DateTimeFormatter.RFC_1123_DATE_TIME::parse)
                    .map(Instant::from)
                    .map(Date::from);
            case HttpStatus.SC_SEE_OTHER:
                LOGGER.warn(
                    "At least one of the given URLs is already registered under another URN, " +
                        "which means you should use this existing URN instead of assigning a new one");
                LOGGER.warn("URN {} could NOT registered to {}", identifier, url);
                break;
            case HttpStatus.SC_CONFLICT:
                LOGGER.warn("URN-Record already exists and will not be created again");
                LOGGER.warn("URN {} could NOT registered to {}", identifier, url);
                break;
            case HttpStatus.SC_FORBIDDEN:
                LOGGER.warn("URN {} record cannot be registered with provided credentials", identifier);
                break;
            default:
                LOGGER.warn("Could not handle urn info request: status={}, urn={}, url={} json={}", postStatus,
                    identifier, url, json);
                break;
        }

        return Optional.empty();
    }

    /**
     * Updates all URLS to a given URN.
     * <br><br>
     * 204 No Content: URN was updated successfully<br>
     * 301 Moved Permanently: URN has a newer version<br>
     * 303 See other: URL is registered for another URN<br>
     *
     * @return the status code of the request
     */

    private Optional<Date> update(MCRPIRegistrationInfo urn) {
        MCRURNJsonBundle bundle = jsonProvider.apply(urn);
        String json = bundle.toJSON(MCRURNJsonBundle.Format.update);
        String updateURL = getUpdateURL(urn);
        CloseableHttpResponse response = MCRHttpsClient.patch(updateURL, APPLICATION_JSON.toString(), json,
            credentials);
        StatusLine statusLine = response.getStatusLine();

        if (statusLine == null) {
            LOGGER.warn("PATCH request for {} returns no status line", updateURL);
            return Optional.empty();
        }

        int postStatus = statusLine.getStatusCode();

        String identifier = urn.getIdentifier();
        switch (postStatus) {
            case HttpStatus.SC_NO_CONTENT:
                LOGGER.info("URN {} updated to {}", identifier, bundle.getUrl());
                return Optional.ofNullable(response.getFirstHeader("Last-Modified"))
                    .map(Header::getValue)
                    .map(DateTimeFormatter.RFC_1123_DATE_TIME::parse)
                    .map(Instant::from)
                    .map(Date::from);
            case HttpStatus.SC_MOVED_PERMANENTLY:
                LOGGER.warn("URN {} has a newer version", identifier);
                break;
            case HttpStatus.SC_SEE_OTHER:
                LOGGER.warn("URL {} is registered for another URN", bundle.getUrl());
                break;
            default:
                LOGGER.warn("URN {} could not be updated. Status {}", identifier, postStatus);
                break;
        }

        return Optional.empty();
    }
}
