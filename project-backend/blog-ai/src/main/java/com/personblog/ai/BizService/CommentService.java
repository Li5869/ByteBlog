package com.personblog.ai.BizService;

import com.personblog.ai.config.PromptManger;
import com.personblog.api.AIAPI.AICommentApi;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

import static com.personblog.ai.constants.AiBusinessConstants.Defaults;
import static com.personblog.ai.constants.LlmPromptType.COMMENT_TYPE;

@Service
@RequiredArgsConstructor
public class CommentService implements AICommentApi {
    private final ChatClient commentClient;
    private final PromptManger promptManger;
    @Override
    public String commentContent(String content) {
        if(content!=null){
           return commentClient
                    .prompt()
                    .system(promptManger.getPrompt(COMMENT_TYPE))
                    .call()
                    .content();
        }
        return Defaults.EMPTY;
    }
}
