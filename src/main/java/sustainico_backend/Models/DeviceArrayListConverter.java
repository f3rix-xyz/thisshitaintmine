package sustainico_backend.Models;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBTypeConverter;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.List;

public class DeviceArrayListConverter implements DynamoDBTypeConverter<String, List<DeviceArray>> {
    private static final Gson gson = new Gson();

    @Override
    public String convert(List<DeviceArray> devices) {
        return gson.toJson(devices);
    }

    @Override
    public List<DeviceArray> unconvert(String jsonData) {
        Type listType = new TypeToken<List<DeviceArray>>(){}.getType();
        return gson.fromJson(jsonData, listType);
    }
}
