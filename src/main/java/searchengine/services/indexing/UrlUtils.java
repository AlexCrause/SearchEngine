package searchengine.services.indexing;

import java.net.MalformedURLException;
import java.net.URL;
import java.text.Normalizer;

public class UrlUtils {


    public static String normalizeUrlWithWWW(String url) throws MalformedURLException {
        if (url.startsWith("http://")) {
            url = url.replace("http://", "https://");
        }
        URL u = new URL(url);
        String host = u.getHost();
        if (!host.startsWith("www.")) {
            host = "www." + host;
        }
        return u.getProtocol() + "://" + host + (u.getPath().isEmpty() ? "/" : u.getPath());
    }

    public static String normalizeUrl(String url) {
        if (url == null || url.isBlank()) return "";
        String normalize = url.split("#")[0];
        if (!normalize.endsWith("/")) normalize += "/";
        return normalize.replaceAll("(?<!:)/+", "/");
    }

    public static String getDomainHost(String url) throws MalformedURLException {
        String host = new URL(url).getHost();
        return host.startsWith("www.") ? host.substring(4) : host;
    }

    public static String getSiteUrl(String url) throws MalformedURLException {
        String urlSite = "";
        String protocol = new URL(url).getProtocol();
        String host = new URL(url).getHost();
        return urlSite = protocol + "://" + host + "/";
    }

    public static String cleanContent(String content) {
        if (content == null) {
            return null;
        }
        String cleaned = content.replaceAll("[^\\u0000-\\uFFFF]", "");
        cleaned = Normalizer.normalize(cleaned, Normalizer.Form.NFC);

        return cleaned;
    }
}
