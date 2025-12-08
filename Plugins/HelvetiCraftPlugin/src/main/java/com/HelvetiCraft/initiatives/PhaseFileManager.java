package com.HelvetiCraft.initiatives;

import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.time.Instant;

public class PhaseFileManager {

    private static File file;
    private static YamlConfiguration yaml;

    public static void init(File pluginFolder) {
        file = new File(pluginFolder, "phase.yml");

        if (!file.exists()) {
            try { file.createNewFile(); } catch (IOException ignored) {}
        }

        yaml = YamlConfiguration.loadConfiguration(file);
    }

    public static void savePhaseSchedule(PhaseSchedule schedule) {
        yaml.set("start1", schedule.getStart1().toString());
        yaml.set("start2", schedule.getStart2().toString());
        yaml.set("start3", schedule.getStart3().toString());
        yaml.set("abschluss", schedule.getAbschluss().toString());

        try { yaml.save(file); } catch (IOException ignore) {}
    }

    public static PhaseSchedule loadPhaseSchedule() {
        PhaseSchedule ps = new PhaseSchedule();

        try {
            ps.setStart1(Instant.parse(yaml.getString("start1")));
            ps.setStart2(Instant.parse(yaml.getString("start2")));
            ps.setStart3(Instant.parse(yaml.getString("start3")));
            ps.setAbschluss(Instant.parse(yaml.getString("abschluss")));
        } catch (Exception e) {
            return null;
        }

        return ps;
    }
}
