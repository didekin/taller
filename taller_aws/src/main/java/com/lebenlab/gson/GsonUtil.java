package com.lebenlab.gson;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializer;
import com.lebenlab.Jsonable;

import java.time.LocalDate;

import static java.time.LocalDate.of;

/**
 * User: pedro@didekin
 * Date: 21/05/2020
 * Time: 19:12
 */
public class GsonUtil {

    private static final Gson myGson = new GsonBuilder()
            .registerTypeAdapter(
                    LocalDate.class,
                    (JsonDeserializer<LocalDate>) (jsonLocalDate, type, jsonDeserialCtx) -> {
                        final var jsonDate = jsonLocalDate.getAsJsonObject();
                        return of(jsonDate.get("year").getAsInt(), jsonDate.get("month").getAsInt(), jsonDate.get("day").getAsInt());
                    }
            ).registerTypeAdapter(
                    LocalDate.class,
                    (JsonSerializer<LocalDate>) (localDate, type, jsonSerialCtx) -> {
                        final var dateMap = new JsonObject();
                        dateMap.addProperty("year", localDate.getYear());
                        dateMap.addProperty("month", localDate.getMonthValue());
                        dateMap.addProperty("day", localDate.getDayOfMonth());
                        return dateMap;
                    }
            )
            .create();

    public static <T extends Jsonable> T objectFromJsonStr(String jsonStr, Class<T> clasObj)
    {
        return myGson.fromJson(jsonStr, clasObj);
    }

    public static String objectToJsonStr(Jsonable object)
    {
        return myGson.toJson(object);
    }
}
