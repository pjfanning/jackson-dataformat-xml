package tools.jackson.dataformat.xml.tofix;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import tools.jackson.dataformat.xml.XmlMapper;
import tools.jackson.dataformat.xml.XmlTestUtil;
import tools.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import tools.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import tools.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import tools.jackson.dataformat.xml.annotation.JacksonXmlText;
import tools.jackson.dataformat.xml.testutil.failure.JacksonTestFailureExpected;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Test cases to reproduce issue #615: @JacksonXmlText doesn't work properly
 * for deserialization without custom @JsonCreator.
 * 
 * This is a reproduction of issue #198 with regular classes.
 */
public class XmlTextDeser615Test extends XmlTestUtil
{
    @JacksonXmlRootElement(localName = "ITEMROOT")
    static class ItemRoot {
        @JsonProperty("Item")
        @JacksonXmlElementWrapper(useWrapping = false)
        List<Item> item;

        public ItemRoot() { }

        public ItemRoot(List<Item> item) {
            this.item = item;
        }

        public List<Item> getItem() {
            return item;
        }

        public void setItem(List<Item> item) {
            this.item = item;
        }
    }

    static class Item {
        @JsonProperty("name")
        @JacksonXmlProperty(isAttribute = true)
        String name;

        @JacksonXmlText
        String value;

        public Item() { }

        public Item(String name, String value) {
            this.name = name;
            this.value = value;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getValue() {
            return value;
        }

        public void setValue(String value) {
            this.value = value;
        }
    }

    // Version with @JsonCreator workaround
    static class ItemWithCreator {
        @JsonProperty("name")
        @JacksonXmlProperty(isAttribute = true)
        String name;

        @JacksonXmlText
        String value;

        @JsonCreator
        public ItemWithCreator(@JsonProperty("name") String name, @JsonProperty("") String value) {
            this.name = name;
            this.value = value;
        }

        public String getName() {
            return name;
        }

        public String getValue() {
            return value;
        }
    }

    @JacksonXmlRootElement(localName = "ITEMROOT")
    static class ItemRootWithCreator {
        @JsonProperty("Item")
        @JacksonXmlElementWrapper(useWrapping = false)
        List<ItemWithCreator> item;

        public ItemRootWithCreator() { }

        public ItemRootWithCreator(List<ItemWithCreator> item) {
            this.item = item;
        }

        public List<ItemWithCreator> getItem() {
            return item;
        }

        public void setItem(List<ItemWithCreator> item) {
            this.item = item;
        }
    }

    /*
    /**********************************************************
    /* Test methods
    /**********************************************************
     */

    private final XmlMapper MAPPER = newMapper();

    @Test
    public void testSerializeItemRoot() throws Exception
    {
        ItemRoot itemRoot = new ItemRoot(
                List.of(
                        new Item("name1", "value1"),
                        new Item("name2", "value2")));

        String xml = MAPPER.writeValueAsString(itemRoot);

        // Verify serialization works - exact format may vary but should contain the elements
        String expectedXml = "<ITEMROOT><Item name=\"name1\">value1</Item><Item name=\"name2\">value2</Item></ITEMROOT>";
        assertEquals(expectedXml, xml);
    }

    // This test passes because of @JsonCreator workaround
    @Test
    public void testDeserializeItemRootWithCreator() throws Exception
    {
        String itemRootXml = "<ITEMROOT><Item name=\"name1\">value1</Item><Item name=\"name2\">value2</Item></ITEMROOT>";
        
        ItemRootWithCreator itemRoot = MAPPER.readValue(itemRootXml, ItemRootWithCreator.class);
        assertEquals(2, itemRoot.getItem().size());
        assertEquals("name1", itemRoot.getItem().get(0).getName());
        assertEquals("value1", itemRoot.getItem().get(0).getValue());
        assertEquals("name2", itemRoot.getItem().get(1).getName());
        assertEquals("value2", itemRoot.getItem().get(1).getValue());
    }

    // This test fails without @JsonCreator workaround
    // Expected error: InvalidDefinitionException: Could not find creator property with name '' 
    @JacksonTestFailureExpected
    @Test
    public void testDeserializeItemRootWithoutCreator() throws Exception
    {
        String itemRootXml = "<ITEMROOT><Item name=\"name1\">value1</Item><Item name=\"name2\">value2</Item></ITEMROOT>";
        
        ItemRoot itemRoot = MAPPER.readValue(itemRootXml, ItemRoot.class);
        assertEquals(2, itemRoot.getItem().size());
        assertEquals("name1", itemRoot.getItem().get(0).getName());
        assertEquals("value1", itemRoot.getItem().get(0).getValue());
        assertEquals("name2", itemRoot.getItem().get(1).getName());
        assertEquals("value2", itemRoot.getItem().get(1).getValue());
    }
}
