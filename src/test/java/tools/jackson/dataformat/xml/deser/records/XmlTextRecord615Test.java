package tools.jackson.dataformat.xml.deser.records;

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
 * Test cases for issue #615: @JacksonXmlText property name mapping for Java Records.
 * 
 * Tests verify that @JacksonXmlText works correctly with Java Records,
 * demonstrating the fix where @JacksonXmlText parameters are mapped to empty string property name.
 * 
 * This is related to issue #198 using Java Records.
 */
public class XmlTextRecord615Test extends XmlTestUtil
{
    // Version with explicit @JsonCreator to test constructor-based deserialization
    @JacksonXmlRootElement(localName = "ITEMROOT")
    public record ItemRootWithCreator(
            @JsonProperty("Item") @JacksonXmlElementWrapper(useWrapping = false)
            List<ItemWithCreator> item) {

        public record ItemWithCreator(
                @JacksonXmlProperty(localName = "name", isAttribute = true) String name,
                @JacksonXmlText String value) {
            
            // Explicit @JsonCreator to demonstrate the fix works with creators
            @JsonCreator
            public ItemWithCreator(
                    @JsonProperty("name") @JacksonXmlProperty(localName = "name", isAttribute = true) String name,
                    @JsonProperty("") @JacksonXmlText String value) {
                this.name = name;
                this.value = value;
            }
        }
    }

    // Version without explicit @JsonCreator (Records auto-detect canonical constructor)
    @JacksonXmlRootElement(localName = "ITEMROOT")
    public record ItemRoot(
            @JsonProperty("Item") @JacksonXmlElementWrapper(useWrapping = false)
            List<Item> item) {

        public record Item(
                @JsonProperty("name") @JacksonXmlProperty(isAttribute = true) String name,
                @JsonProperty("") @JacksonXmlText String value) {
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
                        new ItemRoot.Item("name1", "value1"),
                        new ItemRoot.Item("name2", "value2")));

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
        assertEquals(2, itemRoot.item().size());
        assertEquals("name1", itemRoot.item().get(0).name());
        assertEquals("value1", itemRoot.item().get(0).value());
        assertEquals("name2", itemRoot.item().get(1).name());
        assertEquals("value2", itemRoot.item().get(1).value());
    }

    // This test should now work with the fix to findNameForDeserialization
    @Test
    public void testDeserializeItemRootWithoutCreator() throws Exception
    {
        String itemRootXml = "<ITEMROOT><Item name=\"name1\">value1</Item><Item name=\"name2\">value2</Item></ITEMROOT>";
        
        ItemRoot itemRoot = MAPPER.readValue(itemRootXml, ItemRoot.class);
        assertEquals(2, itemRoot.item().size());
        assertEquals("name1", itemRoot.item().get(0).name());
        assertEquals("value1", itemRoot.item().get(0).value());
        assertEquals("name2", itemRoot.item().get(1).name());
        assertEquals("value2", itemRoot.item().get(1).value());
    }

    // Simplified test case: single item with attribute and text
    @JacksonXmlRootElement(localName = "Item")
    public record SimpleItem(
            @JsonProperty("name") @JacksonXmlProperty(isAttribute = true) String name,
            @JsonProperty("") @JacksonXmlText String value) {
    }

    @Test
    public void testSerializeSimpleItem() throws Exception
    {
        SimpleItem item = new SimpleItem("testName", "testValue");
        String xml = MAPPER.writeValueAsString(item);
        assertEquals("<Item name=\"testName\">testValue</Item>", xml);
    }

    // This test should now work with the fix to findNameForDeserialization
    @Test
    public void testDeserializeSimpleItem() throws Exception
    {
        String xml = "<Item name=\"testName\">testValue</Item>";
        SimpleItem item = MAPPER.readValue(xml, SimpleItem.class);
        assertEquals("testName", item.name());
        assertEquals("testValue", item.value());
    }
}
