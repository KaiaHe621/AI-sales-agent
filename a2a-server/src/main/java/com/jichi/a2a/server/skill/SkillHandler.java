package com.jichi.a2a.server.skill;

import com.jichi.a2a.server.model.*;

import java.util.List;

public interface SkillHandler {

    String skillId();

    Task handle(String taskId, String sessionId, List<Message> history);

}