(function () {
    function init(el) {
        jQuery(el).find('#datesmodule time').each(function(n, e) { var d = new Date(jQuery(e).attr('datetime')); jQuery(e).text(d.format('dd/mmm/yy HH:MM')); })
    }
    JIRA.bind(JIRA.Events.NEW_CONTENT_ADDED, function (e, context, reason) {
        if (reason !== JIRA.CONTENT_ADDED_REASON.panelRefreshed)
            init(context);
    });

})();
