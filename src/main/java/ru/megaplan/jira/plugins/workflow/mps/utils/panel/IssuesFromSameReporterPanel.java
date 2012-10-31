package ru.megaplan.jira.plugins.workflow.mps.utils.panel;

import com.atlassian.core.util.map.EasyMap;
import com.atlassian.crowd.embedded.api.User;
import com.atlassian.jira.bc.issue.search.SearchService;
import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.config.properties.APKeys;
import com.atlassian.jira.config.properties.ApplicationProperties;
import com.atlassian.jira.issue.CustomFieldManager;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.issue.fields.CustomField;
import com.atlassian.jira.issue.search.SearchException;
import com.atlassian.jira.issue.search.SearchResults;
import com.atlassian.jira.issue.search.managers.IssueSearcherManager;
import com.atlassian.jira.jql.builder.JqlClauseBuilder;
import com.atlassian.jira.jql.builder.JqlQueryBuilder;
import com.atlassian.jira.plugin.issuetabpanel.*;
import com.atlassian.jira.web.bean.PagerFilter;
import com.atlassian.query.Query;
import com.atlassian.query.clause.Clause;
import com.atlassian.query.order.SortOrder;
import com.atlassian.velocity.VelocityManager;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.log4j.Logger;
import org.apache.velocity.app.event.implement.EscapeHtmlReference;
import org.apache.velocity.exception.VelocityException;

import java.io.UnsupportedEncodingException;
import java.lang.ref.Reference;
import java.net.URLEncoder;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 * User: firfi
 * Date: 13.07.12
 * Time: 16:28
 * To change this template use File | Settings | File Templates.
 */
public class IssuesFromSameReporterPanel extends AbstractIssueTabPanel2 {

    private final static Logger log = Logger.getLogger(IssuesFromSameReporterPanel.class);

    private final static String templatePath = "templates/ru/megaplan/jira/plugins/workflow/mps/utils/panel/issuesFromSameReporterView.vm";
    private final static Long accountNameCfId = 11261L;
    private final static String MPS = "MPS";
    private final int MAX_ISSUES_ON_PAGE = 20;

    private final VelocityManager velocityManager;
    private final SearchService searchService;
    private final ApplicationProperties applicationProperties;
    private final String baseUrl;
    private final CustomFieldManager customFieldManager;
    private final CustomField accountNameCf;

    IssuesFromSameReporterPanel(VelocityManager velocityManager, SearchService searchService, ApplicationProperties applicationProperties, CustomFieldManager customFieldManager) {
        this.velocityManager = velocityManager;
        this.searchService = searchService;

        this.applicationProperties = applicationProperties;
        this.customFieldManager = customFieldManager;
        baseUrl = (String) applicationProperties.asMap().get(APKeys.JIRA_BASEURL);
        accountNameCf = customFieldManager.getCustomFieldObject(accountNameCfId);

    }

    @Override
    public ShowPanelReply showPanel(ShowPanelRequest showPanelRequest) {
        Issue issue = showPanelRequest.issue();
        if (!MPS.equals(issue.getProjectObject().getKey())) return ShowPanelReply.create(false);
        return ShowPanelReply.create(hasIssuesFromSameReporter(issue, showPanelRequest.remoteUser()));
    }

    @Override
    public GetActionsReply getActions(GetActionsRequest getActionsRequest) {
        String result = null;
        Issue issue = getActionsRequest.issue();
        Query sameReporterQuery = getSameReporterQuery(issue);
        SearchResults searchResults = getIssuesFromSameReporter(getActionsRequest.remoteUser(), sameReporterQuery);
        List<Issue> issues = searchResults.getIssues();
        if (issues == null) {
            result = "error in search";
        } else {
            Map context = EasyMap.build("issues", issues, "baseurl",baseUrl);
            if (accountNameCf != null) {
                context.put("accountNameCf", accountNameCf);
            }
            if (searchResults.getTotal() > MAX_ISSUES_ON_PAGE) {
                try {
                    context.put("queryString", URLEncoder.encode(searchService.getGeneratedJqlString(sameReporterQuery),"UTF-8"));
                } catch (UnsupportedEncodingException e) {
                    throw new Error("wtf can't find UTF-8");
                }
                context.put("queryStart", MAX_ISSUES_ON_PAGE);
            }
            context.put("accountNameCf", accountNameCf);
            context.put("reporterName", issue.getReporter()!=null?issue.getReporter().getName():"");
            result = render(templatePath,context);
        }
        final String finalResult = result;
        IssueAction issueAction = new IssueAction() {
            @Override
            public String getHtml() {
                return finalResult;
            }

            @Override
            public Date getTimePerformed() {
                return new Date();
            }

            @Override
            public boolean isDisplayActionAllTab() {
                return false;
            }
        };
        GetActionsReply gar = GetActionsReply.create(issueAction);
        return gar;
    }

    private SearchResults getIssuesFromSameReporter(User remoteUser,Query query) {
        SearchResults searchResults = null;
        try {
            searchResults = searchService.search(remoteUser, query, PagerFilter.newPageAlignedFilter(0,MAX_ISSUES_ON_PAGE));
        } catch (SearchException e) {
            log.error("error in search",e);
            return null;
        }
        return searchResults;
    }

    private boolean hasIssuesFromSameReporter(Issue issue, User remoteUser) {

        Query query = getSameReporterQuery(issue);
        SearchResults searchResults = null;
        try {
            searchResults = searchService.search(remoteUser, query, PagerFilter.newPageAlignedFilter(0,1));
        } catch (SearchException e) {
            log.error("error in search",e);
            return false;
        }
        return searchResults.getTotal() > 0;
    }

    private Query getSameReporterQuery(Issue issue) {
        JqlQueryBuilder jqlQueryBuilder = JqlQueryBuilder.newBuilder();
        JqlClauseBuilder jqlClauseBuilder = jqlQueryBuilder.where().sub().reporter().eq(issue.getReporter().getName());
        if (accountNameCf != null) {
            Object value = issue.getCustomFieldValue(accountNameCf);
            if (value != null)
                jqlClauseBuilder.or().customField(accountNameCf.getIdAsLong()).like(value.toString());

        }
        jqlClauseBuilder.endsub();
        jqlClauseBuilder.and().issue().notEq(issue.getKey());
        jqlClauseBuilder.endWhere().orderBy().updatedDate(SortOrder.DESC);
        Query query = jqlClauseBuilder.buildQuery();
        return query;
    }

    private String render(String template, Map<String,Object> context) throws VelocityException {
        return velocityManager.getEncodedBody("/",template,baseUrl,"UTF-8",context);
    }

}
