package tools.jackson.dataformat.xml.deser;

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

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Test cases for issue #615: @JacksonXmlText property name mapping for creators.
 * 
 * Tests verify that @JacksonXmlText works correctly with @JsonCreator constructors,
 * demonstrating the fix where @JacksonXmlText parameters are mapped to empty string property name.
 * 
 * This is related to issue #198 with regular classes.
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
        final String name;

        @JacksonXmlText
        final String value;

        @JsonCreator
        public Item(@JacksonXmlProperty(isAttribute = true) String name, 
                    @JacksonXmlText String value) {
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

    // Version with @JsonCreator workaround
    static class ItemWithCreator {
        final String name;
        final String value;

        @JsonCreator
        public ItemWithCreator(
                @JsonProperty("name") @JacksonXmlProperty(localName = "name", isAttribute = true) String name,
                @JsonProperty("value") @JacksonXmlText String value) {
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

    // This test should now work with the fix to findNameForDeserialization
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
