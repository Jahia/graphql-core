package org.jahia.modules.graphql.provider.dxm.extensions.node;

import graphql.annotations.annotationTypes.GraphQLField;
import graphql.annotations.annotationTypes.GraphQLName;

/**
 * Created at Nov 2018$
 *
 * @author chooliyip
 **/
public class GqlBundleState {

    public static String STATE_RESTART = "RESTART";
    public static String STATE_STOPPED = "STOPPED";
    public static String STATE_ERROR = "ERROR";

    private String symbolName;

    private String version;

    private String state;

    public GqlBundleState(String symbolName){
        this.symbolName = symbolName;
    }

    @GraphQLField
    @GraphQLName("state")
    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    @GraphQLField
    @GraphQLName("symbol")
    public String getSymbolName() {
        return symbolName;
    }

    public void setSymbolName(String symbolName) {
        this.symbolName = symbolName;
    }

    @GraphQLField
    @GraphQLName("version")
    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }
}
