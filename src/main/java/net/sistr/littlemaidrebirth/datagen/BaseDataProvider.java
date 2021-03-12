package net.sistr.littlemaidrebirth.datagen;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.DirectoryCache;
import net.minecraft.data.IDataProvider;
import net.minecraft.util.ResourceLocation;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

public abstract class BaseDataProvider implements IDataProvider {
    private static final Logger LOGGER = LogManager.getLogger();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    private final Map<ResourceLocation, DataEntry> entryData = new HashMap<>();
    private final DataGenerator generator;

    public BaseDataProvider(DataGenerator dataGeneratorIn) {
        this.generator = dataGeneratorIn;
    }

    protected abstract void addTables();

    protected void addEntryData(ResourceLocation id, DataEntry entry) {
        entryData.put(id, entry);
    }

    @Override
    public void act(DirectoryCache cache) {
        addTables();
        writeTables(cache, entryData);
    }

    private void writeTables(DirectoryCache cache, Map<ResourceLocation, DataEntry> tables) {
        Path outputFolder = this.generator.getOutputFolder();
        tables.forEach((key, lootTable) -> {
            Path path = outputFolder.resolve("data/" + key.getNamespace() + "/" + getPath() + "/" + key.getPath() + ".json");
            try {
                IDataProvider.save(GSON, cache, lootTable.getJsonElement(), path);
            } catch (IOException e) {
                LOGGER.error("Couldn't write loot table {}", path, e);
            }
        });
    }

    abstract public String getPath();

    public interface DataEntry {

        JsonElement getJsonElement();
    }

}