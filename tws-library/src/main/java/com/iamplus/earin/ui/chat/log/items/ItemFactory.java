package com.iamplus.earin.ui.chat.log.items;

import com.zopim.android.sdk.model.Agent;
import com.zopim.android.sdk.model.items.AgentAttachment;
import com.zopim.android.sdk.model.items.AgentMessage;
import com.zopim.android.sdk.model.items.ChatMemberEvent;
import com.zopim.android.sdk.model.items.RowItem;
import com.zopim.android.sdk.model.items.VisitorAttachment;
import com.zopim.android.sdk.model.items.VisitorMessage;

import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;

import static com.zopim.android.sdk.model.items.RowItem.Type;

/**
 * Factory for creating {@link ViewHolderWrapper} out of {@link RowItem}.
 */
public class ItemFactory {

    private static final List<Type> USED_MESSAGES = Arrays.asList(
            Type.AGENT_MESSAGE, Type.AGENT_ATTACHMENT, Type.VISITOR_ATTACHMENT, Type.VISITOR_MESSAGE
    );

    public static int countUsedMessages(Collection<RowItem> rowItems) {
        int count = 0;
        for(RowItem rowItem : rowItems) {
            if(USED_MESSAGES.contains(rowItem.getType())){
                count++;
            }
        }
        return count;
    }

    public static ViewHolderWrapper get(String id, RowItem rowItem, final LinkedHashMap<String, Agent> agents) {
        switch (rowItem.getType()) {
            case AGENT_MESSAGE: {
                return agentMessage(id, (AgentMessage) rowItem);
            }

            case AGENT_ATTACHMENT: {
                return agentAttachment(id, (AgentAttachment) rowItem);
            }

            case VISITOR_ATTACHMENT: {
                return visitorAttachment(id, (VisitorAttachment) rowItem);
            }

            case VISITOR_MESSAGE: {
                return visitorMessage(id, (VisitorMessage) rowItem);
            }
            case MEMBER_EVENT : {
                return memberEvent(id, (ChatMemberEvent) rowItem);

            }
        }
        return null;
    }


    static AgentMessageWrapper agentMessage(String id, AgentMessage rowItem) {
        return new AgentMessageWrapper(id, rowItem);
    }

    static VisitorMessageWrapper visitorMessage(String id, VisitorMessage rowItem) {
        return new VisitorMessageWrapper(id, rowItem);
    }

    static AgentAttachmentWrapper agentAttachment(String id, AgentAttachment rowItem) {
        return new AgentAttachmentWrapper(id, rowItem);
    }

    static MemberEventWrapper memberEvent(String id, ChatMemberEvent rowItem) {
        return new MemberEventWrapper(id, rowItem);
    }

    static VisitorAttachmentWrapper visitorAttachment(String id, VisitorAttachment rowItem) {
        return new VisitorAttachmentWrapper(id, rowItem);
    }
}