package yagu.yagu.tourapi;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "tourism")
public class TourismProperties {
    private KeyUrl area;
    private KeyUrl relate;

    public KeyUrl getArea() { return area; }
    public void setArea(KeyUrl area) { this.area = area; }
    public KeyUrl getRelate() { return relate; }
    public void setRelate(KeyUrl relate) { this.relate = relate; }

    public static class KeyUrl {
        private String serviceKey;
        private String baseUrl;
        public String getServiceKey() { return serviceKey; }
        public void setServiceKey(String serviceKey) { this.serviceKey = serviceKey; }
        public String getBaseUrl() { return baseUrl; }
        public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }
    }
}
