package com.neurosys.backend.service;

import com.neurosys.backend.dto.request.CommandStatusUpdateRequest;
import com.neurosys.backend.dto.response.RemotePowerAuditDto;
import com.neurosys.backend.dto.response.RemotePowerCommandDto;
import com.neurosys.backend.enums.PowerCommandType;

import java.util.List;

public interface RemotePowerService {

    RemotePowerCommandDto issueCommand(String computerId, PowerCommandType type, String requestedBy);

    List<RemotePowerCommandDto> issueBulkCommands(String labId, List<String> computerIds, PowerCommandType type, String requestedBy);

    RemotePowerCommandDto getPendingCommandForAgent(String agentId);

    RemotePowerCommandDto updateCommandStatus(CommandStatusUpdateRequest request);

    List<RemotePowerAuditDto> getAuditsForComputer(String computerId);
}
