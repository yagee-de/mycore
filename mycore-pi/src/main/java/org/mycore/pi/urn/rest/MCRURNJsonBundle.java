package org.mycore.pi.urn.rest;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.mycore.pi.MCRPIRegistrationInfo;

import java.net.URL;

/**
 * Wraps the urn/url in json as needed by the dnb urn service api.
 *
 * @author shermann
 * */
public class MCRURNJsonBundle {

    private final MCRPIRegistrationInfo urn;

    private final URL url;

    private MCRURNJsonBundle(MCRPIRegistrationInfo urn, URL url) {
        this.urn = urn;
        this.url = url;
    }

    public static MCRURNJsonBundle instance(MCRPIRegistrationInfo urn, URL url) {
        return new MCRURNJsonBundle(urn, url);
    }

    public URL getUrl() {
        return this.url;
    }

    public String toJSON() {
        JsonObject json = new JsonObject();
        json.addProperty("urn", urn.getIdentifier());
        JsonArray urls = new JsonArray();
        json.add("urls", urls);

        JsonObject url = new JsonObject();
        url.addProperty("url", url.toString());
        url.addProperty("priority", "1");
        urls.add(url);

        return json.getAsString();
    }
}
