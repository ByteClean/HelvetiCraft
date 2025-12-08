package com.HelvetiCraft.initiatives;

import java.time.Instant;
import java.time.ZonedDateTime;

public class PhaseSchedule {

    private Instant start1;
    private Instant start2;
    private Instant start3;
    private Instant abschluss;

    public Instant getStart1() { return start1; }
    public Instant getStart2() { return start2; }
    public Instant getStart3() { return start3; }
    public Instant getAbschluss() { return abschluss; }

    public void setStart1(Instant start1) { this.start1 = start1; }
    public void setStart2(Instant start2) { this.start2 = start2; }
    public void setStart3(Instant start3) { this.start3 = start3; }
    public void setAbschluss(Instant abschluss) { this.abschluss = abschluss; }

    /** Determine the current phase based on now() */
    public int getCurrentPhase() {
        Instant now = Instant.now();

        if (now.isBefore(start1)) return 0;
        if (now.isBefore(start2)) return 1;
        if (now.isBefore(start3)) return 2;
        if (now.isBefore(abschluss)) return 0;

        return 0;
    }
    public boolean isExpired() {
        return Instant.now().isAfter(abschluss);
    }

}
