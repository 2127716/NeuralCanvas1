package com.agui.neuralcanvas;

import java.util.ArrayList;
import java.util.List;

public class AiResponse {
    private String answer;
    private List<AiCommand> commands;

    public AiResponse() {
        commands = new ArrayList<>();
    }

    public String getAnswer() {
        return answer == null ? "" : answer;
    }

    public void setAnswer(String answer) {
        this.answer = answer;
    }

    public List<AiCommand> getCommands() {
        if (commands == null) commands = new ArrayList<>();
        return commands;
    }

    public void setCommands(List<AiCommand> commands) {
        this.commands = commands == null ? new ArrayList<>() : commands;
    }
}
