package ru.megaplan.jira.plugins.workflow.mps.utils.listener;

import com.atlassian.core.util.collection.EasyList;
import com.atlassian.core.util.map.EasyMap;
import com.atlassian.crowd.embedded.api.User;
import com.atlassian.event.api.EventListener;
import com.atlassian.event.api.EventPublisher;
import com.atlassian.jira.bc.issue.IssueService;
import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.config.ConstantsManager;
import com.atlassian.jira.config.StatusManager;
import com.atlassian.jira.event.issue.IssueEvent;
import com.atlassian.jira.event.type.EventType;
import com.atlassian.jira.issue.CustomFieldManager;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.issue.MutableIssue;
import com.atlassian.jira.issue.changehistory.ChangeHistoryItem;
import com.atlassian.jira.issue.changehistory.ChangeHistoryManager;
import com.atlassian.jira.issue.context.JiraContextNode;
import com.atlassian.jira.issue.context.ProjectContext;
import com.atlassian.jira.issue.context.manager.JiraContextTreeManager;
import com.atlassian.jira.issue.customfields.CustomFieldSearcher;
import com.atlassian.jira.issue.customfields.CustomFieldType;
import com.atlassian.jira.issue.customfields.CustomFieldUtils;
import com.atlassian.jira.issue.fields.CustomField;
import com.atlassian.jira.issue.search.constants.SystemSearchConstants;
import com.atlassian.jira.issue.status.Status;
import com.atlassian.jira.project.Project;
import com.atlassian.jira.project.ProjectManager;
import com.atlassian.jira.security.groups.GroupManager;
import com.atlassian.jira.user.util.UserManager;
import org.apache.log4j.Logger;
import org.ofbiz.core.entity.GenericEntityException;
import org.ofbiz.core.entity.GenericValue;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import ru.megaplan.jira.plugins.workflow.mps.utils.service.WorkflowSettingsService;
import ru.megaplan.jira.plugins.workflow.mps.utils.settings.MPSUserResponseSettings;
import ru.megaplan.jira.plugins.workflow.util.traveller.WorkflowTraveller;

import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Created with IntelliJ IDEA.
 * User: Firfi
 * Date: 6/20/12
 * Time: 12:22 AM
 * To change this template use File | Settings | File Templates.
 */
public class MPSUserResponseListener implements InitializingBean, DisposableBean {

    private static final Logger log = Logger.getLogger(MPSUserResponseListener.class);

    private final EventPublisher eventPublisher;
    private final CustomFieldManager customFieldManager;
    private final ProjectManager projectManager;
    private final JiraContextTreeManager jiraContextTreeManager;
    private final WorkflowTraveller workflowTraveller;
    private final StatusManager statusManager;
    private final ChangeHistoryManager changeHistoryManager;
    private final WorkflowSettingsService workflowSettingsService;
    private final UserManager userManager;
    private final GroupManager groupManager;

    private final String PREVIOUS_STATUS_RESPONCE_FIELD = "MPSPreviousStatusResponce";
    private final String SYSTEMREADONLYCF = "com.atlassian.jira.plugin.system.customfieldtypes:readonlyfield";
    private final String PREVIOUS_STATUS_RESPONCE_FIELD_DESCRIPTION = "If this field is set then on comment written by reporter " +
            " issue supposed to going in status whose name is this field value";

    private final static String MPSKey = "MPS";
    private final static String STATUS = "status";
    private final static String MEGAPLAN_BOT = "megaplan";
    private final static String EMAIL_ACCOUNTS = "email-accounts";
    private final static String DONE_STATUS_ID = "5";
    private final static String CLOSED_STATUS_ID = "6";
    private final static String OPEN_STATUS_ID = "1";
    private static final ScheduledExecutorService worker =
            Executors.newSingleThreadScheduledExecutor();


    MPSUserResponseListener(EventPublisher eventPublisher, CustomFieldManager customFieldManager,
                            ProjectManager projectManager,
                            WorkflowTraveller workflowTraveller, StatusManager statusManager, ConstantsManager constantsManager,
                            ChangeHistoryManager changeHistoryManager, WorkflowSettingsService workflowSettingsService, UserManager userManager, GroupManager groupManager) {
        this.eventPublisher = eventPublisher;
        this.customFieldManager = customFieldManager;
        this.projectManager = projectManager;
        this.groupManager = groupManager;
        this.jiraContextTreeManager = ComponentAccessor.getComponentOfType(JiraContextTreeManager.class);
        this.workflowTraveller = workflowTraveller;
        this.statusManager = statusManager;
        this.changeHistoryManager = changeHistoryManager;
        this.userManager = userManager;
        this.workflowSettingsService = workflowSettingsService;
    }


    @Override
    public void destroy() throws Exception {
        eventPublisher.unregister(this);
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        eventPublisher.register(this);
    }

    @EventListener
    public void onIssueEvent(IssueEvent issueEvent) {
        boolean isPureComment = issueEvent.getEventTypeId().equals(EventType.ISSUE_COMMENTED_ID);
        boolean maybeComment = issueEvent.getEventTypeId().equals(EventType.ISSUE_UPDATED_ID);
        if (isPureComment || maybeComment) {
            final Issue issue = issueEvent.getIssue();
            if (!issue.getProjectObject().getKey().equals(MPSKey)) return;
            log.debug("start..." + issue.getKey() + " eventtype : " + issueEvent.getEventTypeId());
            String waitingStatus = workflowSettingsService.getUserResponseAwaitStatus();
            log.debug("waitingStatus : " + waitingStatus + " issue.getStatusObject().getName() : " + issue.getStatusObject().getName() + " for issue : " + issue.getKey());
            if (waitingStatus == null || waitingStatus.isEmpty()) {
                log.error("waitingStatus is not set");
            }
            boolean isWaiting = issue.getStatusObject().getName().equals(waitingStatus);
            boolean isDone = issue.getStatusObject().getId().equals(DONE_STATUS_ID);
            boolean isClosed = issue.getStatusObject().getId().equals(CLOSED_STATUS_ID);
            log.warn("isWaiting : " + isWaiting + "; isDone : " + isDone + " ; isClosed : " + isClosed);
            if (!isWaiting && !isDone && !isClosed) return;
            if (maybeComment && !isCommentWithAttach(issueEvent)) return;
            final User megabot = userManager.getUser(MEGAPLAN_BOT);
            if (megabot == null) {
                log.error("User MEGAPLAN_BOT : " + MEGAPLAN_BOT + " doesn't exist");
                return;
            }
            User commentAuthor = null;
            if (issueEvent.getComment() != null) {
                commentAuthor = issueEvent.getComment().getAuthorUser();
            }
            if (commentAuthor == null || !groupManager.getGroupNamesForUser(commentAuthor).contains(EMAIL_ACCOUNTS)) {
                return;
            }

            Runnable runnableToStatus = null;
            if (isWaiting) {
                final Status previousStatus = getPreviousStatus(issue);
                if (previousStatus == null) {
                    log.error("previous status is null");
                    return;
                }
                runnableToStatus = getTravelRunnable(issue, megabot, previousStatus, issue.getStatusObject().getId());
            } else if (isDone || isClosed) {
                final Status openStatus = statusManager.getStatus(OPEN_STATUS_ID);
                if (openStatus == null) {
                    log.error("can't find status with id : " + OPEN_STATUS_ID);
                    return;
                }
                log.debug("prepare to move in open status");
                runnableToStatus = getTravelRunnable(issue, megabot, openStatus, issue.getStatusObject().getId());
            }
            if (runnableToStatus != null) {
                worker.schedule(runnableToStatus, 1, TimeUnit.SECONDS);
                log.debug("scheduled issue traveling : " + issue.getKey());
            } else {
                log.error("some error in obtaining runnableToStatus");
            }
        }
    }

    private Runnable getTravelRunnable(final Issue issue, final User megabot, final Status previousStatus, final String currentStatusId) {
        return new Runnable() {
            @Override
            public void run() {
                if (!currentStatusId.equals(issue.getStatusObject().getId())) {
                    log.warn("thread waited too long, do nothing for issue : " + issue.getKey());
                    return;
                }
                List<IssueService.TransitionValidationResult> errors = new ArrayList<IssueService.TransitionValidationResult>();
                List<IssueService.IssueResult> travelResult = workflowTraveller.travel(megabot, issue, previousStatus, errors);
                log.debug("status on travel : " + travelResult.get(0).getIssue().getStatusObject().getName());
                logErrors(travelResult, errors);
            }
        };
    }

    private boolean isCommentWithAttach(IssueEvent issueEvent) {
        if (!issueEvent.getEventTypeId().equals(EventType.ISSUE_UPDATED_ID)) return false;
        boolean isAttachmentEvent = false;
        GenericValue changeLog = issueEvent.getChangeLog();
        List<GenericValue> changeItems = null;
        try {
            changeItems = changeLog.internalDelegator.findByAnd("ChangeItem", EasyMap.build("group", changeLog.get("id")));
        } catch (GenericEntityException e) {
            log.error("some generic exception in genericvalue",e);
            return false;
        }
        if (changeItems == null) return false;
        Iterator<GenericValue> it = changeItems.iterator();
        while(it.hasNext()){
            String p = it.next().getString("field");
            if(p.equalsIgnoreCase("Attachment"))
                isAttachmentEvent = true;
        }
        if (isAttachmentEvent && issueEvent.getComment() == null) {
            log.warn("user supposed to comment issue and attach too");
        }
        return isAttachmentEvent;
    }


    private void logErrors(List<IssueService.IssueResult> travelResult, List<IssueService.TransitionValidationResult> errors) {
        if (errors.size() > 0) {
            for (IssueService.TransitionValidationResult errorResult : errors) {
                log.error(Arrays.toString(errorResult.getErrorCollection().getErrorMessages().toArray()));
            }

        }
        if (travelResult == null || travelResult.isEmpty()) {
            log.error("travel result is null or empty");
            return;
        }
        if (travelResult.size() > 0) {
            log.debug(travelResult.get(0).getIssue().getKey());
        }
    }

    private Status getPreviousStatus(Issue issue) {
        List<ChangeHistoryItem> changeItems = changeHistoryManager.getAllChangeItems(issue);
        if (changeItems == null || changeItems.size() == 0) return null;
        String statusName = null;
        for (int i = changeItems.size() - 1; i != 0; --i) {
            ChangeHistoryItem lastItem = changeItems.get(i);
            if (!STATUS.equals(lastItem.getField())) continue;
            Object statusObj = lastItem.getFroms().values().iterator().next();
            if (statusObj != null) {
                statusName = statusObj.toString();
                break;
            }
        }
        if (statusName == null) return null;
        Status status = statusManager.getStatus(statusName);
        if (status == null) status = getStatusByName(statusName, statusManager);
        return status;
    }

    public static Status getStatusByName(String statusName, StatusManager statusManager) {
        if (statusName == null || statusName.isEmpty()) return null;
        Collection<Status> allStatuses = statusManager.getStatuses();
        for (Status status : allStatuses) {
            if (statusName.equals(status.getName())) return status;
        }
        return null;
    }

  /*  private CustomField createPreviousStatusCustomField() {
        CustomFieldType readOnlyCf = customFieldManager.getCustomFieldType(SYSTEMREADONLYCF);
        CustomField createdCf = null;
        try {
            CustomFieldSearcher customFieldSearcher = null;
            List searchers = customFieldManager.getCustomFieldSearchers(readOnlyCf);
            if (searchers != null && !searchers.isEmpty()) customFieldSearcher = (CustomFieldSearcher) searchers.iterator().next();
            boolean isGlobal = false;
            Project mps = projectManager.getProjectObjByKey(MPSKey);
            Long[] projectIds = new Long[0];
            if  (mps == null) {
                isGlobal = true;
            } else {
                projectIds = new Long[] {mps.getId()};
            }
            // this was supposed to work but it just create custom field without any contexts
            List<JiraContextNode> contexts = CustomFieldUtils.buildJiraIssueContexts(isGlobal,
                    new Long[0], //project categories
                    projectIds,
                    jiraContextTreeManager);
            log.warn(contexts.get(0));
            createdCf =
                    customFieldManager.createCustomField(PREVIOUS_STATUS_RESPONCE_FIELD,
                            PREVIOUS_STATUS_RESPONCE_FIELD_DESCRIPTION,
                            readOnlyCf,
                            customFieldSearcher,
                            contexts,
                            new ArrayList());
            log.warn("created cf : " + createdCf.getName());
        } catch (GenericEntityException e) {
            e.printStackTrace();
        }
        return createdCf;
    }       */
}
