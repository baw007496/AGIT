package com.centurylink.icl.admin.bundles.listener;

import org.osgi.framework.Bundle;

public interface BundleStateListener {
   
	public interface Factory {

        BundleStateListener getListener();

    }

    String getName();

    String getState(Bundle bundle);

}
