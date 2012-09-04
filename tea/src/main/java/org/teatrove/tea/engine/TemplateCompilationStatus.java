package org.teatrove.tea.engine;

import org.teatrove.trove.util.DefaultStatusListener;
import org.teatrove.trove.util.StatusEvent;

public class TemplateCompilationStatus extends DefaultStatusListener {

    private int current;
    private int total;
    
    public int getCurrent() { return current; }
    public int getTotal() { return total; }
    public double getPercent() {
        if (total == 0) { return 0.0; }
        else { return 100.0 * current / total; }
    }
    
    @Override
    public void statusUpdate(StatusEvent event) {
        this.current = event.getCurrent();
        this.total = event.getTotal();
    }
}