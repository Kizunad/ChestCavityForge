package kizuna.guzhenren_event_ext.common.system.def;

import com.google.gson.JsonObject;
import com.google.gson.annotations.SerializedName;

import java.util.List;

/**
 * Represents the structure of a single event definition from a JSON file.
 * This class is designed to be deserialized from JSON using Gson.
 */
public class EventDefinition {

    @SerializedName("id")
    public String id;

    @SerializedName("enabled")
    public boolean enabled = true;

    @SerializedName("trigger_once")
    public boolean triggerOnce = false;

    @SerializedName("trigger")
    public JsonObject trigger;

    @SerializedName("conditions")
    public List<JsonObject> conditions;

    @SerializedName("actions")
    public List<JsonObject> actions;
}
