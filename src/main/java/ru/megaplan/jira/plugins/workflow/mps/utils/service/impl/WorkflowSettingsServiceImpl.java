package ru.megaplan.jira.plugins.workflow.mps.utils.service.impl;

import com.atlassian.sal.api.lifecycle.LifecycleAware;
import org.apache.log4j.Logger;
import ru.megaplan.jira.plugins.workflow.mps.utils.service.WorkflowSettingsService;
import ru.megaplan.jira.plugins.workflow.mps.utils.settings.MPSUserResponseSettings;

/**
 * Created with IntelliJ IDEA.
 * User: firfi
 * Date: 28.08.12
 * Time: 13:55
 * To change this template use File | Settings | File Templates.
 */
public class WorkflowSettingsServiceImpl implements WorkflowSettingsService, LifecycleAware {

    private final static Logger log = Logger.getLogger(WorkflowSettingsServiceImpl.class);

    private final MPSUserResponseSettings mpsUserResponseSettings;

    WorkflowSettingsServiceImpl(MPSUserResponseSettings mpsUserResponseSettings) {
        this.mpsUserResponseSettings = mpsUserResponseSettings;
    }

    @Override
    public String getUserResponseAwaitStatus() {
        return mpsUserResponseSettings.getValue(MPSUserResponseSettings.WAITING_STATUS);
    }

    @Override
    public void onStart() {
        //To change body of implemented methods use File | Settings | File Templates.
    }
}
