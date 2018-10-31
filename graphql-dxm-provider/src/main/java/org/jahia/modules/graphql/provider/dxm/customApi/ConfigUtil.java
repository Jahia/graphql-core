package org.jahia.modules.graphql.provider.dxm.customApi;

import org.apache.commons.lang.StringUtils;

import java.util.Map;

public class ConfigUtil {
    public static void configureCustomApi(String key, String value, Map<String, CustomApi> customApis) {
        String[] s = StringUtils.split(key, '.');
        if (!customApis.containsKey(s[0])) {
            customApis.put(s[0], new CustomApi(s[0]));
        }
        CustomApi api = customApis.get(s[0]);
        switch (s[1]) {
            case "definition": {
                api.setNodeType(value);
                break;
            }
            case "field": {
                if (api.getField(s[2]) == null) {
                    api.addField(s[2], new Field(s[2]));
                }
                Field field = api.getField(s[2]);
                switch (s[3]) {
                    case "property" : {
                        field.setProperty(value);
                        break;
                    }
                }
                break;
            }
            case "finder": {
                if (api.getFinder(s[2]) == null) {
                    api.addFinder(s[2], new Finder(s[2]));
                }
                Finder finder = api.getFinder(s[2]);
                switch (s[3]) {
                    case "property" : {
                        finder.setProperty(value);
                        break;
                    }
                    case "multiple" : {
                        finder.setMultiple(Boolean.valueOf(value));
                        break;
                    }
                    case "type" : {
                        finder.setType(value);
                        break;
                    }
                }
                break;
            }
        }
    }
}
