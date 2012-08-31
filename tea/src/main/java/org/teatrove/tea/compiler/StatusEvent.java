package org.teatrove.tea.compiler;

/**
 * This has been moved to {@link org.teatrove.trove.util.StatusEvent} 
 *
 * @deprecated
 */
@Deprecated
public class StatusEvent extends org.teatrove.trove.util.StatusEvent {

    private static final long serialVersionUID = 1L;

    public StatusEvent(Object source, int current, int total, String currentName) {
        super(source, current, total, currentName);
    }

}
