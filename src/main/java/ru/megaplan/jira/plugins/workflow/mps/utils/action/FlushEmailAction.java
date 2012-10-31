package ru.megaplan.jira.plugins.workflow.mps.utils.action;

import com.atlassian.jira.web.action.JiraWebActionSupport;
import com.atlassian.mail.queue.MailQueue;
import com.atlassian.mail.queue.MailQueueItem;

import java.util.Collection;
import java.util.Queue;

/**
 * Created with IntelliJ IDEA.
 * User: firfi
 * Date: 27.08.12
 * Time: 14:23
 * To change this template use File | Settings | File Templates.
 */
public class FlushEmailAction extends JiraWebActionSupport {

    private String result;

    private final MailQueue mailQueue;

    FlushEmailAction(MailQueue mailQueue) {

        this.mailQueue = mailQueue;

    }

    public String doExecute() {
        if (mailQueue.isSending()) {
            result = "Отсылка уже идет";
            return SUCCESS;
        } else {
            Queue<MailQueueItem> queue = mailQueue.getQueue();
            if (queue.isEmpty()) {
                result = "Нечего отсылать";
                return SUCCESS;
            }
            StringBuilder builder = new StringBuilder("Отосланные сообщения : <br>");
            for (MailQueueItem item : queue) {
                builder.append(item.getSubject()).append("<br/>");
            }
            result = builder.toString();
            mailQueue.sendBuffer();
        }
        return SUCCESS;
    }

    public String getResult() {
        return result;
    }
}
