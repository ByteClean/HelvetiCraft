package com.HelvetiCraft.initiatives;

import java.time.Instant;
import java.time.ZonedDateTime;

public class PhaseSchedule {

    private Instant start0;
    private Instant start1;
    private Instant start2;
    private Instant start3;
    private Instant abschluss;

    public Instant getStart0() { return start0; }
    public Instant getStart1() { return start1; }
    public Instant getStart2() { return start2; }
    public Instant getStart3() { return start3; }
    public Instant getAbschluss() { return abschluss; }

    public void setStart0(Instant start0) { this.start0 = start0; }
    public void setStart1(Instant start1) { this.start1 = start1; }
    public void setStart2(Instant start2) { this.start2 = start2; }
    public void setStart3(Instant start3) { this.start3 = start3; }
    public void setAbschluss(Instant abschluss) { this.abschluss = abschluss; }

    /** Determine the current phase based on now() for 0-3 system */
    public int getCurrentPhase() {
        Instant now = Instant.now();
        if (start0 == null || start1 == null || start2 == null || start3 == null || abschluss == null) return 3; // fallback: pause
        if (now.isBefore(start1)) return 0; // phase 0: voting
        if (now.isBefore(start2)) return 1; // phase 1: admin acceptance
        if (now.isBefore(start3)) return 2; // phase 2: final voting
        if (now.isBefore(abschluss)) return 3; // phase 3: pause
        return 3;
    }
    public boolean isExpired() {
        return Instant.now().isAfter(abschluss);
    }

}
