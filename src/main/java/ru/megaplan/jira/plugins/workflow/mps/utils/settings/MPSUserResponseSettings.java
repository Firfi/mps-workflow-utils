package ru.megaplan.jira.plugins.workflow.mps.utils.settings;

import com.atlassian.sal.api.pluginsettings.PluginSettings;
import com.atlassian.sal.api.pluginsettings.PluginSettingsFactory;

/**
 * Created with IntelliJ IDEA.
 * User: Firfi
 * Date: 6/20/12
 * Time: 4:29 AM
 * To change this template use File | Settings | File Templates.
 */
public class MPSUserResponseSettings {
    private final PluginSettingsFactory pluginSettingsFactory;
    private static final String KEY = MPSUserResponseSettings.class.getName()+":";
    public static final String WAITING_STATUS = "waiting.status";

    public MPSUserResponseSettings(final PluginSettingsFactory pluginSettingsFactory)
    {
        this.pluginSettingsFactory = pluginSettingsFactory;
    }

    public void setValue(final String key, final String value)
    {
        final PluginSettings settings = pluginSettingsFactory.createSettingsForKey(KEY+key);
        settings.put(key, value);
    }

    public String getValue(final String key)
    {
        final PluginSettings settings = pluginSettingsFactory.createSettingsForKey(KEY+key);
        return (String) settings.get(key);
    }
}
