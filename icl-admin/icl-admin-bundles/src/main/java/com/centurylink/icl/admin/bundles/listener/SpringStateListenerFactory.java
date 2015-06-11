package com.centurylink.icl.admin.bundles.listener;

import java.util.Map;
import java.util.Hashtable;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.osgi.context.event.OsgiBundleApplicationContextListener;
import org.springframework.osgi.context.event.OsgiBundleApplicationContextEvent;
import org.springframework.osgi.context.event.OsgiBundleContextFailedEvent;
import org.springframework.osgi.context.event.OsgiBundleContextRefreshedEvent;
import org.springframework.osgi.extender.event.BootstrappingDependencyEvent;
import org.springframework.osgi.service.importer.event.OsgiServiceDependencyEvent;
import org.springframework.osgi.service.importer.event.OsgiServiceDependencyWaitStartingEvent;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.osgi.framework.BundleListener;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.ServiceRegistration;


public class SpringStateListenerFactory implements BundleStateListener.Factory {

    private BundleContext bundleContext;
    private BundleStateListener listener;

    private static final Log LOG = LogFactory.getLog(SpringStateListenerFactory.class);
    
    public void setBundleContext(BundleContext bundleContext) {
        this.bundleContext = bundleContext;
    }

    public void init() {
    	LOG.debug("In Init of SpringStateListenerFactory");
        getListener();
    }

    public void destroy() throws Exception {
        if (listener instanceof Destroyable) {
            ((Destroyable) listener).destroy();
        }
    }

    public synchronized BundleStateListener getListener() {
    	LOG.debug("In getListener of SpringStateListenerFactory");
        if (listener == null) {
            listener = createListener();
        }
        return listener;
    }

    private BundleStateListener createListener() {
    	LOG.debug("In createListener of SpringStateListenerFactory");
        try {
            // Use dynamic class loading to make sure we actually try to reload the class for
            // dynamic imports to kick in   if possible
            Class cl = getClass().getClassLoader().loadClass("com.centurylink.icl.admin.bundles.listener.SpringStateListenerFactory$SpringApplicationListener");
            return (BundleStateListener) cl.getConstructor(BundleContext.class).newInstance(bundleContext);
//            return new SpringApplicationListener(bundleContext);
        } catch (Throwable t) {
        	LOG.warn("In createListener of SpringStateListenerFactory - Exception Caught: " + t.getMessage());
        	t.printStackTrace();
            return null;
        }
    }

    public static interface Destroyable {

        public void destroy() throws Exception;

    }

    public static class SpringApplicationListener implements OsgiBundleApplicationContextListener,
            BundleListener, Destroyable, BundleStateListener {

        public static enum SpringState {
            Unknown,
            Waiting,
            Started,
            Failed,
        }

    	private static final Log LOG = LogFactory.getLog(SpringApplicationListener.class);

        private final Map<Long, SpringState> states;
        private BundleContext bundleContext;
        private ServiceRegistration registration;

        public SpringApplicationListener(BundleContext bundleContext) {
            this.states = new ConcurrentHashMap<Long, SpringState>();
            this.bundleContext = bundleContext;
            this.bundleContext.addBundleListener(this);
            this.registration = this.bundleContext.registerService(OsgiBundleApplicationContextListener.class.getName(), this, new Hashtable());
        }

        public void destroy() throws Exception {
            bundleContext.removeBundleListener(this);
            registration.unregister();
        }

        public String getName() {
            return "Spring ";
        }

        public String getState(Bundle bundle) {
            SpringState state = states.get(bundle.getBundleId());
            if (state == null || bundle.getState() != Bundle.ACTIVE || state == SpringState.Unknown) {
                return null;
            }
            return state.toString();
        }

        public SpringState getSpringState(Bundle bundle) {
            SpringState state = states.get(bundle.getBundleId());
            if (state == null || bundle.getState() != Bundle.ACTIVE) {
                state = SpringState.Unknown;
            }
            return state;
        }

        public void onOsgiApplicationEvent(OsgiBundleApplicationContextEvent event) {
            SpringState state = null;
            if (event instanceof BootstrappingDependencyEvent) {
                OsgiServiceDependencyEvent de = ((BootstrappingDependencyEvent) event).getDependencyEvent();
                if (de instanceof OsgiServiceDependencyWaitStartingEvent) {
                    state = SpringState.Waiting;
                }
            } else if (event instanceof OsgiBundleContextFailedEvent) {
                state = SpringState.Failed;
            } else if (event instanceof OsgiBundleContextRefreshedEvent) {
                state = SpringState.Started;
            }
            if (state != null) {
                LOG.debug("Spring app state changed to " + state + " for bundle " + event.getBundle().getBundleId());
                states.put(event.getBundle().getBundleId(), state);
            }
        }

        public void bundleChanged(BundleEvent event) {
            if (event.getType() == BundleEvent.UNINSTALLED) {
                states.remove(event.getBundle().getBundleId());
            }
        }

    }

}
