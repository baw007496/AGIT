package com.centurylink.icl.admin.bundles.listener;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.BundleListener;
import org.osgi.service.blueprint.container.BlueprintEvent;

/**
 *
 * TODO: use event admin to receive WAIT topics notifications from blueprint extender
 *
 */
public class BlueprintListener implements org.osgi.service.blueprint.container.BlueprintListener, BundleListener,
                                            BundleStateListener, BundleStateListener.Factory
{

    public static enum BlueprintState {
        Unknown,
        Creating,
        Created,
        Destroying,
        Destroyed,
        Failure,
        GracePeriod,
        Waiting
    }

	private static final Log LOG = LogFactory.getLog(BlueprintListener.class);

    private final Map<Long, BlueprintState> states;
    private BundleContext bundleContext;

    public BlueprintListener() {
        this.states = new ConcurrentHashMap<Long, BlueprintState>();
    }

    public String getName() {
        return "Blueprint   ";
    }

    public String getState(Bundle bundle) {
        BlueprintState state = states.get(bundle.getBundleId());
        if (state == null || bundle.getState() != Bundle.ACTIVE || state == BlueprintState.Unknown) {
            return null;
        }
        return state.toString();
    }

    public BundleStateListener getListener() {
        return this;
    }

    public BlueprintState getBlueprintState(Bundle bundle) {
        BlueprintState state = states.get(bundle.getBundleId());
        if (state == null || bundle.getState() != Bundle.ACTIVE) {
            state = BlueprintState.Unknown;
        }
        return state;
    }

    public void blueprintEvent(BlueprintEvent blueprintEvent) {
        BlueprintState state = getState(blueprintEvent);
        LOG.debug("Blueprint app state changed to " + state + " for bundle " + blueprintEvent.getBundle().getBundleId());
        states.put(blueprintEvent.getBundle().getBundleId(), state);
    }

    public void bundleChanged(BundleEvent event) {
        if (event.getType() == BundleEvent.UNINSTALLED) {
            states.remove(event.getBundle().getBundleId());
        }
    }

    public void setBundleContext(BundleContext bundleContext) {
        this.bundleContext = bundleContext;
    }

    public void init() throws Exception {
        bundleContext.addBundleListener(this);
    }

    public void destroy() throws Exception {
        bundleContext.removeBundleListener(this);
    }

    private BlueprintState getState(BlueprintEvent blueprintEvent) {
        switch (blueprintEvent.getType()) {
            case BlueprintEvent.CREATING:
                return BlueprintState.Creating;
            case BlueprintEvent.CREATED:
                return BlueprintState.Created;
            case BlueprintEvent.DESTROYING:
                return BlueprintState.Destroying;
            case BlueprintEvent.DESTROYED:
                return BlueprintState.Destroyed;
            case BlueprintEvent.FAILURE:
                return BlueprintState.Failure;
            case BlueprintEvent.GRACE_PERIOD:
                return BlueprintState.GracePeriod;
            case BlueprintEvent.WAITING:
                return BlueprintState.Waiting;
            default:
                return BlueprintState.Unknown;
        }
    }
}
