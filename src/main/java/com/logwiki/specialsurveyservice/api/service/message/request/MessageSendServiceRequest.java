package com.logwiki.specialsurveyservice.api.service.message.request;

import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@Getter
public class MessageSendServiceRequest {
    private String type;
    private String from;
    private String content;
    List<Message> messages;
    private String subject;
    List<String> files;

    MessageSendServiceRequest(String type , String from , String content , List<Message> messages) {
        this.type = type;
        this.from = from;
        this.content = content;
        this.messages = messages;
    }

    MessageSendServiceRequest(String type , String from , String content , List<Message> messages , String subject , List<String> files) {
        this.type = type;
        this.from = from;
        this.content = content;
        this.messages = messages;
        this.subject = subject;
        this.files = files;
    }



}
