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
        if (schedule.getStart0() != null) yaml.set("start0", schedule.getStart0().toString());
        yaml.set("start1", schedule.getStart1() != null ? schedule.getStart1().toString() : null);
        yaml.set("start2", schedule.getStart2() != null ? schedule.getStart2().toString() : null);
        yaml.set("start3", schedule.getStart3() != null ? schedule.getStart3().toString() : null);
        yaml.set("abschluss", schedule.getAbschluss() != null ? schedule.getAbschluss().toString() : null);
        try { yaml.save(file); } catch (IOException ignore) {}
    }

    public static PhaseSchedule loadPhaseSchedule() {
        PhaseSchedule ps = new PhaseSchedule();
        try {
            if (yaml.contains("start0")) ps.setStart0(Instant.parse(yaml.getString("start0")));
            if (yaml.contains("start1")) ps.setStart1(Instant.parse(yaml.getString("start1")));
            if (yaml.contains("start2")) ps.setStart2(Instant.parse(yaml.getString("start2")));
            if (yaml.contains("start3")) ps.setStart3(Instant.parse(yaml.getString("start3")));
            if (yaml.contains("abschluss")) ps.setAbschluss(Instant.parse(yaml.getString("abschluss")));
        } catch (Exception e) {
            return null;
        }
        return ps;
    }
}
