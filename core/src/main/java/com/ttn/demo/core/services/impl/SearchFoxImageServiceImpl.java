package com.ttn.demo.core.services.impl;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.ttn.demo.core.services.SearchImageService;

/**
 * The type Search fox image service.
 */
@Component(service = SearchImageService.class, property = {"type=fox"})
public class SearchFoxImageServiceImpl implements SearchImageService {

    private static final Logger LOGGER = LoggerFactory.getLogger(SearchFoxImageServiceImpl.class);

    @Override
    public JsonObject getImage() {
        HttpClient httpClient = HttpClients.createDefault();
        HttpGet httpGet = new HttpGet("https://randomfox.ca/floof/");

        try {
            HttpResponse response = httpClient.execute(httpGet);
            if (response.getStatusLine().getStatusCode() == 200) {
                String jsonResponse = EntityUtils.toString(response.getEntity());
                JsonObject jsonObject = JsonParser.parseString(jsonResponse).getAsJsonObject();

                LOGGER.info("Fox API Response: " + jsonObject.toString());

                return jsonObject;
            } else {
                LOGGER.error("Fox API Error - Status Code: " + response.getStatusLine().getStatusCode());
            }
        } catch (Exception e) {
            LOGGER.error("Error while fetching Fox API data: " + e.getMessage(), e);
        }

        return null;
    }
}
