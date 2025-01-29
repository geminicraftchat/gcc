package cn.ningmo.geminicraftchat.persona;

import cn.ningmo.geminicraftchat.GeminiCraftChat;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class PersonaManager {
    private final GeminiCraftChat plugin;
    private final Map<String, Persona> personas;
    private final Map<String, String> playerPersonas; // 玩家ID -> 人设名称

    public PersonaManager(GeminiCraftChat plugin) {
        this.plugin = plugin;
        this.personas = new HashMap<>();
        this.playerPersonas = new ConcurrentHashMap<>();
        loadPersonas();
    }

    private void loadPersonas() {
        ConfigurationSection personasSection = plugin.getConfig().getConfigurationSection("personas");
        if (personasSection == null) return;

        for (String key : personasSection.getKeys(false)) {
            ConfigurationSection personaSection = personasSection.getConfigurationSection(key);
            if (personaSection == null) continue;

            Persona persona = new Persona(
                personaSection.getString("name", key),
                personaSection.getString("description", ""),
                personaSection.getString("context", "")
            );
            personas.put(key, persona);
        }
    }

    public void savePersonas() {
        ConfigurationSection personasSection = plugin.getConfig().createSection("personas");
        for (Map.Entry<String, Persona> entry : personas.entrySet()) {
            ConfigurationSection personaSection = personasSection.createSection(entry.getKey());
            Persona persona = entry.getValue();
            personaSection.set("name", persona.getName());
            personaSection.set("description", persona.getDescription());
            personaSection.set("context", persona.getContext());
        }
        plugin.saveConfig();
    }

    public boolean createPersona(String key, String name, String description, String context) {
        if (personas.containsKey(key)) {
            return false;
        }
        personas.put(key, new Persona(name, description, context));
        savePersonas();
        return true;
    }

    public boolean deletePersona(String key) {
        if (!personas.containsKey(key)) {
            return false;
        }
        personas.remove(key);
        playerPersonas.values().removeIf(v -> v.equals(key));
        savePersonas();
        return true;
    }

    public Optional<Persona> getPersona(String key) {
        return Optional.ofNullable(personas.get(key));
    }

    public Map<String, Persona> getAllPersonas() {
        return new HashMap<>(personas);
    }

    public void setPlayerPersona(Player player, String personaKey) {
        if (personas.containsKey(personaKey)) {
            playerPersonas.put(player.getUniqueId().toString(), personaKey);
        }
    }

    public Optional<Persona> getPlayerPersona(Player player) {
        String personaKey = playerPersonas.get(player.getUniqueId().toString());
        return personaKey == null ? Optional.empty() : Optional.ofNullable(personas.get(personaKey));
    }

    public void clearPlayerPersona(Player player) {
        playerPersonas.remove(player.getUniqueId().toString());
    }
} 