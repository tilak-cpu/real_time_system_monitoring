package com.neurosys.backend.service;

import com.neurosys.backend.dto.response.ComputerDto;

import java.util.List;

public interface ComputerService {
    List<ComputerDto> getAllComputers();
    List<ComputerDto> getPendingComputers();
    ComputerDto getComputerById(String id);
    ComputerDto getComputerByAgentId(String agentId);
    List<ComputerDto> getComputersByLab(String labName);
    List<ComputerDto> getComputersByLabId(String labId);
    ComputerDto approveComputer(String computerId);
    ComputerDto rejectComputer(String computerId);
    String getAgentStatus(String agentId);
}
