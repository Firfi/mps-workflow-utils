package ru.megaplan.jira.plugins.workflow.mps.utils.action;

import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.config.StatusManager;
import com.atlassian.jira.issue.index.IssueIndexManager;
import com.atlassian.jira.issue.status.Status;
import com.atlassian.jira.web.action.JiraWebActionSupport;
import ru.megaplan.jira.plugins.workflow.mps.utils.listener.MPSUserResponseListener;
import ru.megaplan.jira.plugins.workflow.mps.utils.settings.MPSUserResponseSettings;

/**
 * Created with IntelliJ IDEA.
 * User: Firfi
 * Date: 6/20/12
 * Time: 4:26 AM
 * To change this template use File | Settings | File Templates.
 */
public class MPSUserResponseConfigAction extends JiraWebActionSupport {

    private final MPSUserResponseSettings mpsUserResponseSettings;
    private final StatusManager statusManager;

    private String waitingStatus;

    MPSUserResponseConfigAction(MPSUserResponseSettings mpsUserResponseSettings, StatusManager statusManager) {
        this.mpsUserResponseSettings = mpsUserResponseSettings;
        this.statusManager = statusManager;
    }


    @Override
    public String doDefault() throws Exception {
        if (waitingStatus != null) {
            Status status = MPSUserResponseListener.getStatusByName(waitingStatus, statusManager);
            if (status == null) {
                addErrorMessage("status with this name is not exist");
                return INPUT;
            }
            mpsUserResponseSettings.setValue(MPSUserResponseSettings.WAITING_STATUS, waitingStatus);
            return getRedirect("MPSUserResponseConfigAction.jspa");
        }
        String waitingStatus = mpsUserResponseSettings.getValue(MPSUserResponseSettings.WAITING_STATUS);
        if (waitingStatus != null) this.waitingStatus = waitingStatus;
        return INPUT;
    }

    @Override
    public String doExecute() throws Exception {
        return doDefault();
    }

    public String getWaitingStatus() {
        return waitingStatus;
    }

    public void setWaitingStatus(String waitingStatus) {
        this.waitingStatus = waitingStatus;
    }

}
