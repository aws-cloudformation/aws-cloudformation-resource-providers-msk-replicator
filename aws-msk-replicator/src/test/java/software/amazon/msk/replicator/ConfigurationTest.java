package software.amazon.msk.replicator;

import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class ConfigurationTest extends AbstractTestBase {

    Configuration configuration;

    @BeforeEach
    public void setup(){
        configuration = new Configuration();
    }

    @Test
    public void test_NullTags() {
        // Given
        ResourceModel model = ResourceModel.builder().tags(null).build();
        // When
        Map<String, String> response = configuration.resourceDefinedTags(model);
        // Then
        assertThat(response).isNull();
    }

    @Test
    public void test_Tags() {
        // Given
        ResourceModel model = ResourceModel.builder().tags(TagHelper.convertToSet(TAGS)).build();
        // When
        Map<String, String> response = configuration.resourceDefinedTags(model);
        // Then
        assertThat(response).isNotNull();
        assertThat(response).isEqualTo(TagHelper.convertToMap(model.getTags()));
    }
}