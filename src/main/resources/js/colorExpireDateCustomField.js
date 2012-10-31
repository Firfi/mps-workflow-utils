AJS.$.namespace('ru.megaplan.jira.plugins.workflow.mps.utils.js')
jQuery(document).ready(function() {
    var expiredDateCf = jQuery('#customfield_14660-val time');
    if (expiredDateCf) {
        var date = expiredDateCf.attr('datetime');
        if (date) {
            var parsedDate = Date.parse(date);
            if (parsedDate && parsedDate < new Date()) {
                expiredDateCf.css('color','red');
                expiredDateCf.css('font-size','25px');
            }
        }
    }
    var expiredDateOldCf = jQuery('#customfield_12264-val');
    if (expiredDateOldCf) {
        var date = expiredDateOldCf.text();
        if (date) {
            date = date.trim();
            var parsedDate = Date.parse(date);
            if (parsedDate && parsedDate < new Date()) {
                expiredDateOldCf.css('color','red');
                expiredDateOldCf.css('font-size','25px');
            }
        }
    }
});