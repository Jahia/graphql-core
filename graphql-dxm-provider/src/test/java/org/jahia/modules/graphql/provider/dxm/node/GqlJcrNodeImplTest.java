package org.jahia.modules.graphql.provider.dxm.node;

import org.jahia.services.content.JCRPropertyWrapper;
import org.junit.Test;

import javax.jcr.Property;
import javax.jcr.RepositoryException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Covers how {@code keepTranslatedProperties} keeps the translated property when the node carries a property of the
 * same name.
 */
public class GqlJcrNodeImplTest {

    @Test
    public void translatedPropertyWinsOverThePropertyOfTheNode() throws RepositoryException {
        GqlJcrProperty onNode = property("text", "/sites/s/text");
        GqlJcrProperty translated = property("text", "/sites/s/j:translation_en/text");
        GqlJcrProperty other = property("title", "/sites/s/title");
        List<GqlJcrProperty> properties = new ArrayList<>(Arrays.asList(onNode, translated, other));

        GqlJcrNodeImpl.keepTranslatedProperties(properties, "en");

        assertEquals(Arrays.asList(translated, other), properties);
    }

    @Test
    public void propertiesOfAVersionSessionStay() throws RepositoryException {
        // A version session builds its properties with no raw JCR property.
        GqlJcrProperty text = property("text", null);
        GqlJcrProperty title = property("title", null);
        List<GqlJcrProperty> properties = new ArrayList<>(Arrays.asList(text, title));

        GqlJcrNodeImpl.keepTranslatedProperties(properties, "en");

        assertEquals(Arrays.asList(text, title), properties);
    }

    private static GqlJcrProperty property(String name, String realPath) throws RepositoryException {
        JCRPropertyWrapper wrapper = mock(JCRPropertyWrapper.class);
        when(wrapper.getName()).thenReturn(name);
        if (realPath != null) {
            Property realProperty = mock(Property.class);
            when(realProperty.getPath()).thenReturn(realPath);
            when(wrapper.getRealProperty()).thenReturn(realProperty);
        }
        return new GqlJcrProperty(wrapper, null);
    }
}
