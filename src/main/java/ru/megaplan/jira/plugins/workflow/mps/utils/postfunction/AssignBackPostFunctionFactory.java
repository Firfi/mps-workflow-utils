package ru.megaplan.jira.plugins.workflow.mps.utils.postfunction;

import com.atlassian.core.util.map.EasyMap;
import com.atlassian.jira.plugin.workflow.AbstractWorkflowPluginFactory;
import com.atlassian.jira.plugin.workflow.WorkflowPluginFunctionFactory;
import com.opensymphony.workflow.loader.AbstractDescriptor;
import com.opensymphony.workflow.loader.FunctionDescriptor;
import org.apache.log4j.Logger;

import java.util.HashMap;
import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 * User: firfi
 * Date: 22.06.12
 * Time: 16:02
 * To change this template use File | Settings | File Templates.
 */
public class AssignBackPostFunctionFactory extends AbstractWorkflowPluginFactory implements
        WorkflowPluginFunctionFactory {

    private static final Logger log = Logger.getLogger(AssignBackPostFunctionFactory.class);

    public static String MOVEDINSTATUS = "movedInStatus";
    public static String LASTASSIGNEE = "lastAssignee";


    @Override
    protected void getVelocityParamsForInput(Map<String, Object> stringObjectMap) {

    }

    @Override
    protected void getVelocityParamsForEdit(Map<String, Object> stringObjectMap, AbstractDescriptor abstractDescriptor) {
        String movedInStatus =  getOption(abstractDescriptor, MOVEDINSTATUS);
        String lastAssignee = getOption(abstractDescriptor, LASTASSIGNEE);
        if (movedInStatus != null) stringObjectMap.put(MOVEDINSTATUS, movedInStatus);
        if (lastAssignee != null) stringObjectMap.put(LASTASSIGNEE, lastAssignee);
    }

    @Override
    protected void getVelocityParamsForView(Map<String, Object> stringObjectMap, AbstractDescriptor abstractDescriptor) {
        getVelocityParamsForEdit(stringObjectMap, abstractDescriptor);
    }


    @SuppressWarnings("unchecked")
    @Override
    public Map getDescriptorParams(Map conditionParams) {
        String movedInStatus = extractSingleParam(conditionParams, MOVEDINSTATUS);
        String lashAssignee = extractSingleParam(conditionParams, LASTASSIGNEE);
        Map<String, Object> result = new HashMap<String, Object>();
        if (movedInStatus != null && !movedInStatus.isEmpty()) result.put(MOVEDINSTATUS, movedInStatus);
        if (lashAssignee != null && !lashAssignee.isEmpty()) result.put(LASTASSIGNEE, lashAssignee);
        return result;
    }

    private String getOption(AbstractDescriptor descriptor, String option){

        if (!(descriptor instanceof FunctionDescriptor)) {
            throw new IllegalArgumentException("Descriptor must be a FunctionDescriptor.");
        }

        FunctionDescriptor functionDescriptor = (FunctionDescriptor) descriptor;
        String result = (String) functionDescriptor.getArgs().get(option);

        return result;
    }

}
