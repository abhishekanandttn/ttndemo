package com.ttn.demo.core.servlets;

import com.google.gson.JsonObject;
import com.ttn.demo.core.services.SearchImageService;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.servlets.SlingSafeMethodsServlet;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

import javax.servlet.Servlet;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * The type Search image servlet.
 */
@Component(
        service = Servlet.class,
        property = {
                "sling.servlet.paths=/bin/searchimage",
                "sling.servlet.methods=GET"
        }
)
public class SearchImageServlet extends SlingSafeMethodsServlet {

    private static final Logger LOGGER = LoggerFactory.getLogger(SearchImageServlet.class);

    private final Map<String, SearchImageService> serviceMap = new HashMap<>();

    @Reference(
            service = SearchImageService.class,
            cardinality = ReferenceCardinality.MULTIPLE,
            policy = ReferencePolicy.DYNAMIC,
            bind = "bindService",
            unbind = "unbindService"
    )

    /*
    - By marking the field as volatile, you tell the Java Virtual Machine (JVM) to always read the reference directly from memory,
     ensuring that you get the most up-to-date reference, especially when services are dynamically added or removed.

    - Without volatile, there is a risk that a thread may continue using a cached reference to a service even
      after the service has been unbound and rebound with a new implementation. This can lead to unexpected behavior.
    */
    private volatile SearchImageService[] services;

    /**
     * Bind service.
     *
     * @param service    the service
     * @param properties the properties
     */
    protected void bindService(SearchImageService service, Map<String, Object> properties) {
        String type = (String) properties.get("type");
        if (type != null) {
            serviceMap.put(type, service);
        }
    }

    /**
     * Unbind service.
     *
     * @param service    the service
     * @param properties the properties
     */
    protected void unbindService(SearchImageService service, Map<String, Object> properties) {
        String type = (String) properties.get("type");
        if (type != null) {
            serviceMap.remove(type);
        }
    }

    @Override
    protected void doGet(SlingHttpServletRequest request, SlingHttpServletResponse response) throws IOException {
        String type = request.getParameter("type");

        if (type != null) {
            try {
                SearchImageService searchImageService = serviceMap.get(type);

                if (searchImageService != null) {
                    JsonObject result = searchImageService.getImage();

                    if (result != null) {
                        String imageUrl;

                        if (result.has("message")) {
                            imageUrl = result.get("message").getAsString();
                        } else if (result.has("image")) {
                            imageUrl = result.get("image").getAsString();
                        } else {
                            imageUrl = null; // Neither "message" nor "image" property found
                        }

                        if (imageUrl != null) {
                            // Set the content type to image/jpeg (or the appropriate image format)
                            response.setContentType("image/jpeg");

                            // Fetch and stream the image content directly to the response
                            byte[] imageBytes = loadImageContent(imageUrl);
                            if (imageBytes != null) {
                                response.getOutputStream().write(imageBytes);
                                LOGGER.info("Image rendered successfully.");
                                return;
                            }
                        }
                    }
                }
            } catch (Exception e) {
                LOGGER.error("Error while processing the request: " + e.getMessage(), e);
            }
        }

        response.setStatus(SlingHttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        response.getWriter().write("Error occurred while processing the request.");
    }

    private byte[] loadImageContent(String imageUrl) {
        HttpClient httpClient = HttpClients.createDefault();
        HttpGet httpGet = new HttpGet(imageUrl);

        try {
            HttpResponse response = httpClient.execute(httpGet);
            HttpEntity entity = response.getEntity();

            if (entity != null) {
                return EntityUtils.toByteArray(entity);
            }
        } catch (Exception e) {
            LOGGER.error("Error while fetching image content: " + e.getMessage(), e);
        }

        return null;
    }
}
