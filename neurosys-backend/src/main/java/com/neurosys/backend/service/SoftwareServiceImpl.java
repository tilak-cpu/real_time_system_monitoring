package com.neurosys.backend.service;

import com.neurosys.backend.dto.request.SoftwareSyncRequest;
import com.neurosys.backend.dto.response.LabReadinessDto;
import com.neurosys.backend.dto.response.SoftwareSearchResponse;
import com.neurosys.backend.entity.Computer;
import com.neurosys.backend.entity.RequiredSoftware;
import com.neurosys.backend.entity.SoftwareInventory;
import com.neurosys.backend.enums.ComputerStatus;
import com.neurosys.backend.exception.ResourceNotFoundException;
import com.neurosys.backend.repository.ComputerRepository;
import com.neurosys.backend.repository.RequiredSoftwareRepository;
import com.neurosys.backend.repository.SoftwareInventoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class SoftwareServiceImpl implements SoftwareService {

    private final SoftwareInventoryRepository softwareInventoryRepository;
    private final RequiredSoftwareRepository requiredSoftwareRepository;
    private final ComputerRepository computerRepository;

    private static final Map<String, String> ALIAS_MAP = new HashMap<>();

    static {
        // Common software aliases and typo mappings
        ALIAS_MAP.put("python", "Python");
        ALIAS_MAP.put("pyhton", "Python");
        ALIAS_MAP.put("pythn", "Python");
        ALIAS_MAP.put("pyton", "Python");
        ALIAS_MAP.put("vscode", "Visual Studio Code");
        ALIAS_MAP.put("vs code", "Visual Studio Code");
        ALIAS_MAP.put("visual studio code", "Visual Studio Code");
        ALIAS_MAP.put("visual studio cod", "Visual Studio Code");
        ALIAS_MAP.put("intellij", "IntelliJ IDEA");
        ALIAS_MAP.put("idea", "IntelliJ IDEA");
        ALIAS_MAP.put("intellij idea", "IntelliJ IDEA");
        ALIAS_MAP.put("eclips", "Eclipse");
        ALIAS_MAP.put("eclipse", "Eclipse");
        ALIAS_MAP.put("mysql", "MySQL");
        ALIAS_MAP.put("mysql server", "MySQL");
        ALIAS_MAP.put("mysql workbench", "MySQL");
        ALIAS_MAP.put("java", "Java");
        ALIAS_MAP.put("jvaa", "Java");
        ALIAS_MAP.put("jdk", "Java");
        ALIAS_MAP.put("openjdk", "Java");
        ALIAS_MAP.put("jre", "Java");
        ALIAS_MAP.put("git", "Git");
        ALIAS_MAP.put("git for windows", "Git");
        ALIAS_MAP.put("chrome", "Google Chrome");
        ALIAS_MAP.put("chrme", "Google Chrome");
        ALIAS_MAP.put("google chrome", "Google Chrome");
        ALIAS_MAP.put("node", "Node.js");
        ALIAS_MAP.put("nodejs", "Node.js");
        ALIAS_MAP.put("node.js", "Node.js");
        ALIAS_MAP.put("docker", "Docker Desktop");
        ALIAS_MAP.put("postman", "Postman");
        ALIAS_MAP.put("whatsapp", "WhatsApp");
        ALIAS_MAP.put("whats app", "WhatsApp");
        ALIAS_MAP.put("whatsapp desktop", "WhatsApp");
        ALIAS_MAP.put("whatsappdesktop", "WhatsApp");
        ALIAS_MAP.put("5319275a.whatsappdesktop", "WhatsApp");
        ALIAS_MAP.put("dev-c++", "Dev-C++");
        ALIAS_MAP.put("dev c++", "Dev-C++");
        ALIAS_MAP.put("devc++", "Dev-C++");
        ALIAS_MAP.put("dev-cpp", "Dev-C++");
        ALIAS_MAP.put("dev cpp", "Dev-C++");
        ALIAS_MAP.put("c++", "Dev-C++");
        ALIAS_MAP.put("cpp", "Dev-C++");
        ALIAS_MAP.put("bloodshed dev-c++", "Dev-C++");
        ALIAS_MAP.put("jav", "Java");
        ALIAS_MAP.put("microsoft build of openjdk", "Java");
    }

    @Override
    @Transactional
    public void syncSoftwareInventory(SoftwareSyncRequest request) {
        // Robust Computer Lookup: Agent ID -> Hostname -> Auto-create
        Computer computer = computerRepository.findByAgentId(request.getAgentId())
                .or(() -> request.getHostname() != null && !request.getHostname().trim().isEmpty() 
                        ? computerRepository.findByHostnameIgnoreCase(request.getHostname().trim()) 
                        : Optional.empty())
                .orElseGet(() -> {
                    log.info("Auto-registering computer endpoint for software sync [AgentID: {}, Hostname: {}]", 
                            request.getAgentId(), request.getHostname());
                    String host = request.getHostname() != null ? request.getHostname().trim() : "WORKSTATION-" + request.getAgentId();
                    Computer newComp = Computer.builder()
                            .agentId(request.getAgentId())
                            .hostname(host)
                            .computerName(host)
                            .status(ComputerStatus.ONLINE)
                            .lastSeenAt(Instant.now())
                            .labName("Computer Lab")
                            .osName("Windows OS")
                            .build();
                    return computerRepository.save(newComp);
                });

        if (computer.getStatus() == ComputerStatus.REJECTED) {
            log.warn("Ignoring software sync for rejected computer {}", computer.getHostname());
            return;
        }

        if (computer.getStatus() == ComputerStatus.PENDING) {
            computer.setStatus(ComputerStatus.ONLINE);
        }
        computer.setLastSeenAt(Instant.now());
        computerRepository.save(computer);

        log.info("Syncing {} software records for computer {}", 
                request.getSoftwareList() != null ? request.getSoftwareList().size() : 0, computer.getHostname());

        // Wipe previous inventory records ONLY for this specific computer
        List<SoftwareInventory> existing = softwareInventoryRepository.findByComputerId(computer.getId());
        if (existing != null && !existing.isEmpty()) {
            softwareInventoryRepository.deleteAll(existing);
        }

        if (request.getSoftwareList() != null && !request.getSoftwareList().isEmpty()) {
            List<SoftwareInventory> entities = request.getSoftwareList().stream()
                    .filter(dto -> dto.getName() != null && !dto.getName().trim().isEmpty())
                    .filter(dto -> {
                        String n = dto.getName().toLowerCase();
                        return !n.contains("categoryinfo") && !n.contains("commandnotfoundexception") 
                            && !n.contains("operable program") && !n.contains("fullyqualifiederrorid") 
                            && !n.startsWith("+") && !n.startsWith("at line:");
                    })
                    .map(dto -> {
                        String name = sanitizeString(dto.getName(), 190);
                        String version = sanitizeString(dto.getVersion(), 90);
                        String publisher = sanitizeString(dto.getPublisher(), 140);
                        String installDate = sanitizeString(dto.getInstallDate(), 40);

                        SoftwareInventory entity = SoftwareInventory.builder()
                                .computer(computer)
                                .name(name.isEmpty() ? "Unknown Software" : name)
                                .version(version)
                                .publisher(publisher)
                                .installDate(installDate)
                                .lastScannedAt(Instant.now())
                                .build();

                        entity.setCreatedAt(Instant.now());
                        entity.setUpdatedAt(Instant.now());
                        return entity;
                    })
                    .collect(Collectors.toList());

            softwareInventoryRepository.saveAll(entities);
            log.info("Saved {} new software inventory items for computer {}", entities.size(), computer.getHostname());
        }
    }

    private String sanitizeString(String input, int maxLength) {
        if (input == null) return "";
        String cleaned = input.replaceAll("[^\\x20-\\x7E]", "").trim();
        if (cleaned.length() > maxLength) {
            cleaned = cleaned.substring(0, maxLength);
        }
        return cleaned;
    }

    @Override
    @Transactional(readOnly = true)
    public List<SoftwareInventory> getAllSoftwareInventory() {
        return softwareInventoryRepository.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public List<SoftwareInventory> getSoftwareForComputer(String computerId) {
        return softwareInventoryRepository.findByComputerId(computerId);
    }

    @Override
    @Transactional(readOnly = true)
    public SoftwareSearchResponse searchSoftware(String query, String requiredVersion) {
        String rawQuery = (query != null && !query.trim().isEmpty()) ? query.trim() : "Python";
        String normalizedQuery = rawQuery.toLowerCase();

        String matchedSoftware = ALIAS_MAP.get(normalizedQuery);

        List<String> distinctNames = softwareInventoryRepository.findDistinctSoftwareNames();
        List<String> suggestions = new ArrayList<>();

        if (matchedSoftware == null) {
            double bestScore = 0.0;
            String bestMatch = null;

            for (String dbName : distinctNames) {
                double score = calculateSimilarity(normalizedQuery, dbName);
                if (score > bestScore) {
                    bestScore = score;
                    bestMatch = dbName;
                }
                if (score >= 0.45 && !dbName.equalsIgnoreCase(bestMatch)) {
                    suggestions.add(dbName);
                }
            }

            if (bestScore >= 0.45 && bestMatch != null) {
                matchedSoftware = normalizeName(bestMatch);
            } else {
                matchedSoftware = capitalizeWords(rawQuery);
            }
        }

        final String targetMatchedSoftware = matchedSoftware;
        suggestions = suggestions.stream()
                .map(this::normalizeName)
                .filter(s -> !s.equalsIgnoreCase(targetMatchedSoftware))
                .distinct()
                .limit(3)
                .toList();

        List<Computer> approvedComputers = computerRepository.findAll().stream()
                .filter(c -> c.getStatus() != ComputerStatus.REJECTED)
                .sorted(Comparator.comparing(Computer::getHostname))
                .toList();

        List<SoftwareSearchResponse.ComputerSoftwareStatusDto> computerResults = new ArrayList<>();
        List<SoftwareSearchResponse.InstalledComputerInfo> installedLegacy = new ArrayList<>();
        List<SoftwareSearchResponse.MissingComputerInfo> missingLegacy = new ArrayList<>();

        int installedCount = 0;

        for (Computer computer : approvedComputers) {
            List<SoftwareInventory> swList = softwareInventoryRepository.findByComputerId(computer.getId());

            final String targetSearch = matchedSoftware.toLowerCase();
            Optional<SoftwareInventory> match = swList.stream()
                    .filter(s -> isSoftwareMatch(s.getName(), targetSearch, normalizedQuery))
                    .findFirst();

            boolean isInstalled = match.isPresent();
            String detectedVersion = isInstalled ? (match.get().getVersion() != null ? match.get().getVersion() : "Installed") : null;

            if (isInstalled) {
                installedCount++;
                installedLegacy.add(SoftwareSearchResponse.InstalledComputerInfo.builder()
                        .computerId(computer.getId())
                        .hostname(computer.getHostname())
                        .labName(computer.getLabName())
                        .version(detectedVersion)
                        .status(computer.getStatus().name())
                        .build());
            } else {
                missingLegacy.add(SoftwareSearchResponse.MissingComputerInfo.builder()
                        .computerId(computer.getId())
                        .hostname(computer.getHostname())
                        .labName(computer.getLabName())
                        .status(computer.getStatus().name())
                        .build());
            }

            computerResults.add(SoftwareSearchResponse.ComputerSoftwareStatusDto.builder()
                    .computerId(computer.getId())
                    .computerName(computer.getHostname())
                    .hostname(computer.getHostname())
                    .labName(computer.getLabName())
                    .status(computer.getStatus().name())
                    .installed(isInstalled)
                    .version(detectedVersion)
                    .build());
        }

        boolean softwareFound = installedCount > 0;

        return SoftwareSearchResponse.builder()
                .query(rawQuery)
                .matchedSoftware(matchedSoftware)
                .softwareFound(softwareFound)
                .totalComputers(approvedComputers.size())
                .installedCount(installedCount)
                .notInstalledCount(approvedComputers.size() - installedCount)
                .suggestions(suggestions)
                .computers(computerResults)
                .softwareName(matchedSoftware)
                .totalInstalled(installedCount)
                .totalMissing(approvedComputers.size() - installedCount)
                .totalOutdated(0)
                .installedComputers(installedLegacy)
                .missingComputers(missingLegacy)
                .outdatedComputers(Collections.emptyList())
                .build();
    }

    private boolean isSoftwareMatch(String dbSoftwareName, String targetSearch, String rawQuery) {
        if (dbSoftwareName == null) return false;
        String dbLower = dbSoftwareName.toLowerCase();
        if (dbLower.contains(targetSearch) || dbLower.contains(rawQuery) || targetSearch.contains(dbLower) || rawQuery.contains(dbLower)) {
            return true;
        }
        return calculateSimilarity(rawQuery, dbSoftwareName) >= 0.4;
    }

    private String normalizeName(String name) {
        if (name == null) return "";
        for (Map.Entry<String, String> entry : ALIAS_MAP.entrySet()) {
            if (name.toLowerCase().contains(entry.getKey())) {
                return entry.getValue();
            }
        }
        return capitalizeWords(name);
    }

    private String capitalizeWords(String input) {
        if (input == null || input.isEmpty()) return "";
        String[] words = input.split("\\s+");
        StringBuilder sb = new StringBuilder();
        for (String w : words) {
            if (!w.isEmpty()) {
                sb.append(Character.toUpperCase(w.charAt(0)))
                  .append(w.substring(1).toLowerCase())
                  .append(" ");
            }
        }
        return sb.toString().trim();
    }

    private double calculateSimilarity(String s1, String s2) {
        if (s1 == null || s2 == null) return 0.0;
        String a = s1.toLowerCase().trim();
        String b = s2.toLowerCase().trim();
        if (a.equals(b)) return 1.0;
        if (a.contains(b) || b.contains(a)) return 0.85;

        int dist = levenshteinDistance(a, b);
        int maxLen = Math.max(a.length(), b.length());
        if (maxLen == 0) return 1.0;
        return 1.0 - ((double) dist / maxLen);
    }

    private int levenshteinDistance(String a, String b) {
        int[] costs = new int[b.length() + 1];
        for (int j = 0; j <= b.length(); j++) costs[j] = j;
        for (int i = 1; i <= a.length(); i++) {
            costs[0] = i;
            int nw = i - 1;
            for (int j = 1; j <= b.length(); j++) {
                int cj = Math.min(1 + Math.min(costs[j], costs[j - 1]),
                        a.charAt(i - 1) == b.charAt(j - 1) ? nw : nw + 1);
                nw = costs[j];
                costs[j] = cj;
            }
        }
        return costs[b.length()];
    }

    @Override
    @Transactional(readOnly = true)
    public LabReadinessDto getLabReadiness(String labName) {
        return getLabReadiness(labName, null);
    }

    @Override
    @Transactional(readOnly = true)
    public LabReadinessDto getLabReadiness(String labName, String labId) {
        List<Computer> labComputers;
        if (labId != null && !labId.isEmpty() && !"ALL".equalsIgnoreCase(labId)) {
            labComputers = computerRepository.findByLabId(labId).stream()
                    .filter(c -> c.getStatus() != ComputerStatus.PENDING && c.getStatus() != ComputerStatus.REJECTED)
                    .toList();
        } else {
            labComputers = computerRepository.findAll().stream()
                    .filter(c -> c.getStatus() != ComputerStatus.PENDING && c.getStatus() != ComputerStatus.REJECTED)
                    .toList();
        }

        String targetLab = (labName != null && !labName.isEmpty()) ? labName : "Computer Lab";
        List<RequiredSoftware> reqSoftware = requiredSoftwareRepository.findByLabName(targetLab);
        if (reqSoftware.isEmpty()) {
            reqSoftware = requiredSoftwareRepository.findAll();
        }

        List<String> reqNames = reqSoftware.stream().map(RequiredSoftware::getSoftwareName).toList();
        List<LabReadinessDto.LabComputerStatusDto> compStatusList = new ArrayList<>();

        int readyCount = 0;
        for (Computer c : labComputers) {
            List<SoftwareInventory> installed = softwareInventoryRepository.findByComputerId(c.getId());
            boolean hasScannedInventory = !installed.isEmpty();

            List<String> missing = new ArrayList<>();
            List<String> outdated = new ArrayList<>();
            List<String> issues = new ArrayList<>();

            for (RequiredSoftware req : reqSoftware) {
                String reqName = req.getSoftwareName();
                String reqVersion = req.getRequiredVersion();

                if (!hasScannedInventory) {
                    issues.add("Software inventory unavailable");
                    break;
                }

                Optional<SoftwareInventory> match = installed.stream()
                        .filter(s -> isSoftwareMatch(s.getName(), reqName, reqName))
                        .findFirst();

                if (match.isEmpty()) {
                    missing.add(reqName);
                    issues.add(reqName + " is not installed");
                } else if (reqVersion != null && !reqVersion.trim().isEmpty() && !reqVersion.equalsIgnoreCase("optional")) {
                    String detectedVer = match.get().getVersion();
                    if (detectedVer != null && !detectedVer.startsWith(reqVersion.trim())) {
                        outdated.add(reqName);
                        issues.add(reqName + " version " + detectedVer + " is installed, but version " + reqVersion + " is required");
                    }
                }
            }

            if (c.getStatus() == ComputerStatus.OFFLINE) {
                issues.add("Computer is offline");
            }

            boolean isReady = missing.isEmpty() && outdated.isEmpty() && c.getStatus() != ComputerStatus.OFFLINE;

            if (isReady) readyCount++;

            compStatusList.add(LabReadinessDto.LabComputerStatusDto.builder()
                    .computerId(c.getId())
                    .hostname(c.getHostname())
                    .status(c.getStatus().name())
                    .ready(isReady)
                    .missingSoftware(missing)
                    .outdatedSoftware(outdated)
                    .issues(issues)
                    .build());
        }

        int total = labComputers.size();
        double percent = total > 0 ? (double) readyCount / total * 100.0 : 0.0;

        return LabReadinessDto.builder()
                .labName(targetLab)
                .totalComputers(total)
                .readyComputers(readyCount)
                .unreadyComputers(total - readyCount)
                .readinessPercentage(Math.round(percent * 10.0) / 10.0)
                .computers(compStatusList)
                .requiredSoftwareNames(reqNames)
                .build();
    }

    @Override
    @Transactional
    public RequiredSoftware addRequiredSoftware(String labName, String softwareName, String requiredVersion) {
        RequiredSoftware rs = RequiredSoftware.builder()
                .labName(labName)
                .softwareName(softwareName)
                .requiredVersion(requiredVersion)
                .build();
        return requiredSoftwareRepository.save(rs);
    }

    @Override
    @Transactional(readOnly = true)
    public List<RequiredSoftware> getRequiredSoftwareForLab(String labName) {
        return requiredSoftwareRepository.findByLabName(labName);
    }

    @Override
    @Transactional
    public void removeRequiredSoftware(String id) {
        requiredSoftwareRepository.deleteById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> getSoftwareSummary() {
        List<String> distinctNames = softwareInventoryRepository.findDistinctSoftwareNames();
        long totalRecords = softwareInventoryRepository.count();
        Instant lastScanned = softwareInventoryRepository.findLatestScanTime().orElse(null);

        Map<String, Object> summary = new HashMap<>();
        summary.put("totalDistinctSoftware", distinctNames.size());
        summary.put("totalScannedRecords", totalRecords);
        summary.put("lastScannedAt", lastScanned != null ? lastScanned.toString() : null);
        return summary;
    }

    @Override
    @Transactional(readOnly = true)
    public com.neurosys.backend.dto.response.SoftwareFleetSummaryDto getFleetSoftwareSummary() {
        List<Computer> approvedComputers = computerRepository.findAll().stream()
                .filter(c -> c.getStatus() != ComputerStatus.REJECTED)
                .sorted(Comparator.comparing(Computer::getHostname))
                .toList();

        List<SoftwareInventory> allSoftware = softwareInventoryRepository.findAll();
        List<String> distinctNames = softwareInventoryRepository.findDistinctSoftwareNames();
        Instant lastScanned = softwareInventoryRepository.findLatestScanTime().orElse(null);

        return com.neurosys.backend.dto.response.SoftwareFleetSummaryDto.builder()
                .totalComputers(approvedComputers.size())
                .totalScannedRecords(allSoftware.size())
                .totalDistinctSoftware(distinctNames.size())
                .lastScannedAt(lastScanned != null ? lastScanned.toString() : Instant.now().toString())
                .computers(approvedComputers)
                .softwareList(allSoftware)
                .build();
    }
}
