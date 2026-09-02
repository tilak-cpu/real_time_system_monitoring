package com.neurosys.backend.service;

import com.neurosys.backend.dto.request.CommandStatusUpdateRequest;
import com.neurosys.backend.dto.response.RemotePowerAuditDto;
import com.neurosys.backend.dto.response.RemotePowerCommandDto;
import com.neurosys.backend.entity.Computer;
import com.neurosys.backend.entity.RemotePowerAudit;
import com.neurosys.backend.entity.RemotePowerCommand;
import com.neurosys.backend.enums.ComputerStatus;
import com.neurosys.backend.enums.PowerCommandStatus;
import com.neurosys.backend.enums.PowerCommandType;
import com.neurosys.backend.exception.ResourceNotFoundException;
import com.neurosys.backend.repository.ComputerRepository;
import com.neurosys.backend.repository.RemotePowerAuditRepository;
import com.neurosys.backend.repository.RemotePowerCommandRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class RemotePowerServiceImpl implements RemotePowerService {

    private final RemotePowerCommandRepository commandRepository;
    private final RemotePowerAuditRepository auditRepository;
    private final ComputerRepository computerRepository;

    @Override
    @Transactional
    public RemotePowerCommandDto issueCommand(String computerId, PowerCommandType type, String requestedBy) {
        long startTime = System.currentTimeMillis();
        Computer computer = computerRepository.findById(computerId)
                .orElseThrow(() -> new ResourceNotFoundException("Computer", "id", computerId));

        if (computer.getStatus() == ComputerStatus.PENDING || computer.getStatus() == ComputerStatus.REJECTED) {
            log.warn("Attempted remote power action on unapproved computer {}", computer.getHostname());
            throw new IllegalArgumentException("Computer endpoint is unapproved. Remote actions unavailable.");
        }

        log.info("[PERF LOG] [BACKEND] Received {} request for computer {} ({}) at {}", type, computer.getHostname(), computerId, Instant.now());

        // Check for existing active command to prevent rapid duplicate clicks
        Optional<RemotePowerCommand> activeCmd = commandRepository
                .findFirstByComputerIdAndStatusInOrderByCreatedAtDesc(
                        computerId, List.of(PowerCommandStatus.PENDING, PowerCommandStatus.SENT, PowerCommandStatus.EXECUTING));

        if (activeCmd.isPresent()) {
            RemotePowerCommand existing = activeCmd.get();
            long secondsAgo = Duration.between(existing.getCreatedAt(), Instant.now()).getSeconds();
            if (secondsAgo < 15) {
                log.info("[PERF LOG] [BACKEND] Duplicate click suppressed — Returning existing active command {} (issued {}s ago)", existing.getId(), secondsAgo);
                return mapToCommandDto(existing);
            }
        }

        // Create new RemotePowerCommand (queue command so agent will execute immediately upon polling/heartbeat)
        RemotePowerCommand command = RemotePowerCommand.builder()
                .computer(computer)
                .commandType(type)
                .status(PowerCommandStatus.PENDING)
                .requestedBy(requestedBy != null ? requestedBy : "Administrator")
                .build();

        command = commandRepository.save(command);

        // Record Audit Log
        RemotePowerAudit audit = RemotePowerAudit.builder()
                .userName(command.getRequestedBy())
                .computerName(computer.getHostname())
                .computerId(computer.getId())
                .action(type)
                .status("QUEUED")
                .timestamp(Instant.now())
                .build();
        auditRepository.save(audit);

        long elapsedMs = System.currentTimeMillis() - startTime;
        log.info("[PERF LOG] [BACKEND] Successfully queued {} command {} for computer {} in {}ms at {}", 
                type, command.getId(), computer.getHostname(), elapsedMs, Instant.now());

        return mapToCommandDto(command);
    }

    @Override
    @Transactional
    public RemotePowerCommandDto getPendingCommandForAgent(String agentId) {
        Optional<RemotePowerCommand> pendingCmd = commandRepository
                .findFirstByComputerAgentIdAndStatusOrderByCreatedAtAsc(agentId, PowerCommandStatus.PENDING);

        if (pendingCmd.isPresent()) {
            RemotePowerCommand command = pendingCmd.get();
            command.setStatus(PowerCommandStatus.SENT);
            commandRepository.save(command);
            log.info("[PERF LOG] [BACKEND] Delivered pending {} command {} to agent {} at {}", 
                    command.getCommandType(), command.getId(), agentId, Instant.now());
            return mapToCommandDto(command);
        }
        return null;
    }

    @Override
    @Transactional
    public RemotePowerCommandDto updateCommandStatus(CommandStatusUpdateRequest request) {
        RemotePowerCommand command = commandRepository.findById(request.getCommandId())
                .orElseThrow(() -> new ResourceNotFoundException("RemotePowerCommand", "id", request.getCommandId()));

        command.setStatus(request.getStatus());
        if (request.getFailureReason() != null) {
            command.setFailureReason(request.getFailureReason());
        }
        commandRepository.save(command);

        // Update corresponding Audit record
        RemotePowerAudit audit = RemotePowerAudit.builder()
                .userName(command.getRequestedBy())
                .computerName(command.getComputer() != null ? command.getComputer().getHostname() : "Computer")
                .computerId(command.getComputer() != null ? command.getComputer().getId() : "")
                .action(command.getCommandType())
                .status(request.getStatus().name())
                .failureReason(request.getFailureReason())
                .timestamp(Instant.now())
                .build();
        auditRepository.save(audit);

        log.info("[PERF LOG] [BACKEND] Agent reported command {} status updated to {} at {}", command.getId(), request.getStatus(), Instant.now());
        return mapToCommandDto(command);
    }

    @Override
    @Transactional(readOnly = true)
    public List<RemotePowerAuditDto> getAuditsForComputer(String computerId) {
        return auditRepository.findByComputerIdOrderByTimestampDesc(computerId)
                .stream()
                .map(this::mapToAuditDto)
                .toList();
    }

    @Override
    @Transactional
    public List<RemotePowerCommandDto> issueBulkCommands(String labId, List<String> computerIds, PowerCommandType type, String requestedBy) {
        log.info("[INFO] Bulk power action requested: Action={}, LabID={}, TargetCount={}", type, labId, computerIds != null ? computerIds.size() : 0);

        List<Computer> targetComputers;
        if (computerIds != null && !computerIds.isEmpty()) {
            targetComputers = computerRepository.findAllById(computerIds);
            if (labId != null && !labId.isEmpty() && !"ALL".equalsIgnoreCase(labId)) {
                targetComputers = targetComputers.stream()
                        .filter(c -> c.getLab() != null && labId.equals(c.getLab().getId()))
                        .toList();
            }
        } else if (labId != null && !labId.isEmpty() && !"ALL".equalsIgnoreCase(labId)) {
            targetComputers = computerRepository.findByLabIdAndStatus(labId, ComputerStatus.ONLINE);
        } else {
            targetComputers = computerRepository.findByStatus(ComputerStatus.ONLINE);
        }

        return targetComputers.stream()
                .map(comp -> {
                    try {
                        return issueCommand(comp.getId(), type, requestedBy);
                    } catch (Exception e) {
                        log.warn("Failed to issue bulk command {} for computer {}: {}", type, comp.getHostname(), e.getMessage());
                        return null;
                    }
                })
                .filter(cmd -> cmd != null)
                .toList();
    }

    private RemotePowerCommandDto mapToCommandDto(RemotePowerCommand entity) {
        String compId = "";
        String compName = "Computer";

        try {
            if (entity.getComputer() != null) {
                compId = entity.getComputer().getId();
                compName = entity.getComputer().getHostname();
            }
        } catch (Exception ignored) {
        }

        return RemotePowerCommandDto.builder()
                .id(entity.getId())
                .computerId(compId)
                .computerName(compName)
                .commandType(entity.getCommandType())
                .status(entity.getStatus())
                .requestedBy(entity.getRequestedBy())
                .failureReason(entity.getFailureReason())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    private RemotePowerAuditDto mapToAuditDto(RemotePowerAudit entity) {
        return RemotePowerAuditDto.builder()
                .id(entity.getId())
                .userName(entity.getUserName())
                .computerName(entity.getComputerName())
                .computerId(entity.getComputerId())
                .action(entity.getAction())
                .timestamp(entity.getTimestamp())
                .status(entity.getStatus())
                .failureReason(entity.getFailureReason())
                .build();
    }
}
