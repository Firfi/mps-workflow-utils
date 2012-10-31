package ru.megaplan.jira.plugins.workflow.mps.utils.postfunction;

import com.atlassian.crowd.embedded.api.User;
import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.event.type.EventDispatchOption;
import com.atlassian.jira.issue.IssueManager;
import com.atlassian.jira.issue.MutableIssue;
import com.atlassian.jira.issue.changehistory.ChangeHistoryItem;
import com.atlassian.jira.issue.changehistory.ChangeHistoryManager;
import com.atlassian.jira.user.util.UserManager;
import com.atlassian.jira.workflow.function.issue.AbstractJiraFunctionProvider;
import com.opensymphony.module.propertyset.PropertySet;
import com.opensymphony.workflow.WorkflowException;
import org.apache.log4j.Logger;
import ru.megaplan.jira.plugins.history.search.HistorySearchManager;
import ru.megaplan.jira.plugins.history.search.HistorySearchRequest;

import java.util.List;
import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 * User: firfi
 * Date: 22.06.12
 * Time: 15:54
 * To change this template use File | Settings | File Templates.
 */
public class AssignBackPostFunction  extends AbstractJiraFunctionProvider {

    private final HistorySearchManager historySearchManager;
    private final UserManager userManager;

    public AssignBackPostFunction(HistorySearchManager historySearchManager,UserManager userManager) {
        this.historySearchManager = historySearchManager;
        this.userManager = userManager;
        log.warn("creating function");
    }

    private static final Logger log = Logger.getLogger(AssignBackPostFunction.class);

    public void execute(Map transientVars, Map args, PropertySet ps) throws WorkflowException {
        MutableIssue issue = getIssue(transientVars);
        User caller = getCaller(transientVars,args);
        String movedInStatus = (String) args.get(AssignBackPostFunctionFactory.MOVEDINSTATUS);
        String lastAssignee = (String) args.get(AssignBackPostFunctionFactory.LASTASSIGNEE);
        User previous = null;
        String previousUserName = null;
        if (movedInStatus != null) {
            HistorySearchRequest request = historySearchManager.getSearchRequest(issue).setTo(issue.getStatusObject().getName()).setWhat("status");
            List<ChangeHistoryItem> changes = request.find();
            if (changes == null || changes.size() == 0) {
                log.error("wtf");
                return;
            }
            previousUserName =
                    changes.get(changes.size()-1).getUser();
        } else if (lastAssignee != null) {
            HistorySearchRequest request = historySearchManager.getSearchRequest(issue);
            request.setTo(issue.getAssignee().getName()).setWhat("assignee");
            List<ChangeHistoryItem> changes = request.find();
            if (changes == null || changes.size() == 0) {
                log.error("obtained change history is empty");
                return;
            }
            previousUserName =
                    changes.get(changes.size()-1).getFroms().keySet().iterator().next();

        }

        log.warn("previousUserName : " + previousUserName);

        previous = userManager.getUser(previousUserName);
        if (previous == null) {
            log.error("can't find user : " + previousUserName + " in transition : " + issue.getKey());
            return;
        }
        if (issue.getAssignee().equals(previous)) {
            log.warn("assignee equals previous assignee");
            return;
        }
        issue.setAssignee(previous);
        //issueManager.updateIssue(caller, issue, EventDispatchOption.DO_NOT_DISPATCH, false);

    }
}

