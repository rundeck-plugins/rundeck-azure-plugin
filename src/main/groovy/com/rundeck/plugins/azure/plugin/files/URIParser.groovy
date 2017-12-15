package com.rundeck.plugins.azure.plugin.files

/**
 * Created by luistoledo on 11/14/17.
 */
class URIParser {
    private final URI uri;

    URIParser(String str) throws URISyntaxException {
        uri = new URI(str);
    }

    String getFile() {
        return uri.getQuery() == null ? uri.getPath() : uri.getPath() + "?" + uri.getQuery();
    }

    String getScheme() {
        return uri.getScheme();
    }

    String getProtocol() {
        return getScheme();
    }

    String getUserInfo() {
        return uri.getUserInfo();
    }

    String getHost() {
        return uri.getHost();
    }

    int getPort() {
        return uri.getPort();
    }

    String getPath() {
        return uri.getPath();
    }

    String getQuery() {
        return uri.getQuery();
    }


    String getFragment() {
        return uri.getFragment();
    }

    String getSchemeSpecificPart() {
        return uri.getSchemeSpecificPart();
    }

    String getAuthority() {
        return uri.getAuthority();
    }

    String getFileName() {
        int index = getFile().lastIndexOf("/");
        String fileName = getFile().substring(index + 1);

        return fileName;
    }


    @Override
    public String toString() {
        return "URIParser{" +
                "uri=" + uri.toString() +
                '}';
    }
}