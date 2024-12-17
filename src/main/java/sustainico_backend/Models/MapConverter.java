package sustainico_backend.Models;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBTypeConverter;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Map;

public class MapConverter
        implements DynamoDBTypeConverter<Map<String, Map<String, String>>, Map<String, Map<String, String>>> {
    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public Map<String, Map<String, String>> convert(Map<String, Map<String, String>> object) {
        return object;
    }

    @Override
    public Map<String, Map<String, String>> unconvert(Map<String, Map<String, String>> object) {
        try {
            // First convert to string and back to handle potential type mismatches
            String json = objectMapper.writeValueAsString(object);
            return objectMapper.readValue(json, new TypeReference<Map<String, Map<String, String>>>() {
            });
        } catch (Exception e) {
            throw new IllegalArgumentException("Error converting DynamoDB map", e);
        }
    }
}