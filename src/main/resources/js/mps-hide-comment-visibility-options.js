JIRA.bind(JIRA.Events.NEW_CONTENT_ADDED, function(event,context){
    var issueKey = JIRA.Meta.getIssueKey();
    if (!issueKey) issueKey = jQuery('#comment').data('issuekey');
    if (issueKey && issueKey.substr(0,4) == "MPS-") {
        var ctx = context.find('#commentLevel-multi-select');
        if (ctx.length == 0) {
            ctx = jQuery(document);
        }
        context.find('#commentLevel').find('option[value!="role:10000"]').filter('option[value!=""]').remove();
        var userRole = context.find('#commentLevel').find('option[value="role:10000"]');
        if (jQuery(context).hasClass("aui-popup-content")) {
            var popupComment = context.find('.comment-input');
            if (popupComment.length > 0) {
                popupComment.hide();
            }
        }


    }
});
