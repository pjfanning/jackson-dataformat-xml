package tools.jackson.dataformat.xml.tofix.records;

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
 * for deserialization with Java Records without custom @JsonCreator.
 * 
 * This is a reproduction of issue #198 using Java Records.
 */
public class XmlTextRecord615Test extends XmlTestUtil
{
    // Version with @JsonCreator workaround
    @JacksonXmlRootElement(localName = "ITEMROOT")
    public record ItemRootWithCreator(
            @JsonProperty("Item") @JacksonXmlElementWrapper(useWrapping = false)
            List<ItemWithCreator> item) {

        public record ItemWithCreator(
                @JacksonXmlProperty(localName = "name", isAttribute = true) String name,
                @JacksonXmlText String value) {
            
            // Explicit @JsonCreator is the workaround needed for deserialization with @JacksonXmlText
            // The annotations are on the record components above
            @JsonCreator
            public ItemWithCreator {
                // Compact constructor form - field assignments are automatic in Records
            }
        }
    }

    // Version without @JsonCreator - this should fail
    @JacksonXmlRootElement(localName = "ITEMROOT")
    public record ItemRoot(
            @JsonProperty("Item") @JacksonXmlElementWrapper(useWrapping = false)
            List<Item> item) {

        public record Item(
                @JsonProperty("name") @JacksonXmlProperty(isAttribute = true) String name,
                @JacksonXmlText String value) {
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

    // This test fails without @JsonCreator workaround
    // Expected error: InvalidDefinitionException: Could not find creator property with name '' 
    @JacksonTestFailureExpected
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
            @JacksonXmlText String value) {
    }

    @Test
    public void testSerializeSimpleItem() throws Exception
    {
        SimpleItem item = new SimpleItem("testName", "testValue");
        String xml = MAPPER.writeValueAsString(item);
        assertEquals("<Item name=\"testName\">testValue</Item>", xml);
    }

    // This test demonstrates the core issue - deserialization fails for simple case too
    @JacksonTestFailureExpected
    @Test
    public void testDeserializeSimpleItem() throws Exception
    {
        String xml = "<Item name=\"testName\">testValue</Item>";
        SimpleItem item = MAPPER.readValue(xml, SimpleItem.class);
        assertEquals("testName", item.name());
        assertEquals("testValue", item.value());
    }
}
