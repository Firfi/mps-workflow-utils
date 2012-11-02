package ru.megaplan.jira.plugins.workflow.mps.utils.listener;

import com.atlassian.core.util.map.EasyMap;
import com.atlassian.event.api.EventListener;
import com.atlassian.event.api.EventPublisher;
import com.atlassian.jira.bc.issue.IssueService;
import com.atlassian.jira.event.issue.IssueEvent;
import com.atlassian.jira.event.type.EventType;
import com.atlassian.jira.issue.CustomFieldManager;
import com.atlassian.jira.issue.IssueInputParameters;
import com.atlassian.jira.issue.fields.CustomField;
import com.atlassian.plugin.util.collect.Function;
import org.apache.log4j.Logger;
import org.ofbiz.core.entity.GenericEntityException;
import org.ofbiz.core.entity.GenericValue;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import ru.megaplan.jira.plugins.history.search.HistorySearchManager;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: Firfi
 * Date: 7/4/12
 * Time: 9:50 PM
 * To change this template use File | Settings | File Templates.
 */
public class MPSOriginalSummaryListener implements InitializingBean, DisposableBean {

    private final static Logger log = Logger.getLogger(MPSOriginalSummaryListener.class);
    private final static String ORIGINAL_SUMMARY_FIELD = "Оригинальный заголовок";
    private final static String MPS = "MPS";

    private final EventPublisher eventPublisher;
    private final IssueService issueService;

    private final CustomField originalSummaryCf;

    private final String SUMMARY = "Summary";

    private final Function<HistorySearchManager.ChangeLogRequest, String> findChangeLogFunction;


    MPSOriginalSummaryListener(EventPublisher eventPublisher, CustomFieldManager customFieldManager, IssueService issueService, HistorySearchManager historySearchManager) throws Exception {
        log.debug("initializing originalsummary listener");
        this.eventPublisher = eventPublisher;
        this.issueService = issueService;
        originalSummaryCf = customFieldManager.getCustomFieldObjectByName(ORIGINAL_SUMMARY_FIELD);
        findChangeLogFunction = historySearchManager.getFindInChangeLogFunction();
        if (findChangeLogFunction==null) {
            String error = "cant' get findChangeLogFunction";
            log.error("error");
            throw new Exception(error);
        }
    }

    @Override
    public void destroy() throws Exception {
        eventPublisher.unregister(this);
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        if (originalSummaryCf == null) {
            String error = "cant' find issue custom field original summary with name : " + ORIGINAL_SUMMARY_FIELD;
            log.error(error);
            throw new Exception(error);
        }
        log.debug("registering original summary listener");
        eventPublisher.register(this);
    }

    @EventListener
    public void summaryChanged(IssueEvent issueEvent) {
        log.debug("found issue event");
        if (issueEvent.getProject().getKey().equals(MPS)) {
            if (issueEvent.getChangeLog() == null) return;
            String oldSummary = getSummary(issueEvent.getChangeLog());
            if (oldSummary == null) return;
            log.debug("issue event is updated id and mps project");
            String originalSumary = (String)originalSummaryCf.getValue(issueEvent.getIssue());
            log.debug("originalSumary is " + originalSumary);
            if (originalSumary == null) {
                IssueInputParameters issueInputParameters = issueService.newIssueInputParameters();
                issueInputParameters.addCustomFieldValue(originalSummaryCf.getId(),oldSummary);
                IssueService.UpdateValidationResult issueValidationResult =
                        issueService.validateUpdate(issueEvent.getUser(), issueEvent.getIssue().getId(), issueInputParameters);
                if (!issueValidationResult.isValid()) {
                    log.error("issueValidationResult is invalid : " + Arrays.toString(issueValidationResult.getErrorCollection().getErrorMessages().toArray()));
                    return;
                }
                IssueService.IssueResult issueResult = issueService.update(issueEvent.getUser(), issueValidationResult);

                log.debug("updated complete for issue : " + issueResult.getIssue().getKey() + " new cf : " + issueResult.getIssue().getCustomFieldValue(originalSummaryCf));
            }
        }
    }

    private String getSummary(GenericValue changeLog) {
        HistorySearchManager.ChangeLogRequest changeLogRequest = new HistorySearchManager.ChangeLogRequest(changeLog, SUMMARY);
        changeLogRequest.setLog(log);
        return findChangeLogFunction.get(changeLogRequest);
    }

}
