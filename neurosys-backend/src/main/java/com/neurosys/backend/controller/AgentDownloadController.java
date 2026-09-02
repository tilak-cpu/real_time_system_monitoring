package com.neurosys.backend.controller;

import com.neurosys.backend.entity.Lab;
import com.neurosys.backend.entity.LabEnrollmentCode;
import com.neurosys.backend.exception.ResourceNotFoundException;
import com.neurosys.backend.repository.LabEnrollmentCodeRepository;
import com.neurosys.backend.repository.LabRepository;
import com.neurosys.backend.service.LabService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.ByteArrayOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.Optional;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Slf4j
@RestController
@RequestMapping("/api/v1/agent")
@RequiredArgsConstructor
@Tag(name = "Agent Download & Package Generator", description = "REST APIs for generating and downloading pre-configured agent setup packages for specific computer labs")
public class AgentDownloadController {

    private final LabRepository labRepository;
    private final LabEnrollmentCodeRepository enrollmentCodeRepository;
    private final LabService labService;

    @GetMapping("/download")
    @Operation(summary = "Download Lab-Preconfigured NeuroSys Agent Package", description = "Generates a .zip package containing agent binary, pre-configured agent.properties, and enrollment code for the target lab")
    public ResponseEntity<byte[]> downloadAgentPackage(
            @RequestParam(required = false) String labId,
            @RequestParam(required = false) String enrollmentCode) {
        
        Lab lab;
        if (labId != null && !labId.isEmpty() && !"ALL".equalsIgnoreCase(labId)) {
            lab = labRepository.findById(labId)
                    .orElseGet(() -> labRepository.findByCodeIgnoreCase(labId)
                    .orElseGet(() -> labRepository.findAll().stream().findFirst().orElseThrow(() -> new ResourceNotFoundException("Lab", "id", labId))));
        } else {
            lab = labRepository.findAll().stream().findFirst()
                    .orElseThrow(() -> new ResourceNotFoundException("Lab", "default", "none"));
        }

        String activeCode = enrollmentCode;
        if (activeCode == null || activeCode.isEmpty()) {
            Optional<LabEnrollmentCode> existing = enrollmentCodeRepository.findByLabIdOrderByCreatedAtDesc(lab.getId())
                    .stream().filter(c -> !c.getRevoked() && c.getExpiresAt().isAfter(Instant.now())).findFirst();
            if (existing.isPresent()) {
                activeCode = existing.get().getCode();
            } else {
                var newCode = labService.generateEnrollmentCode(lab.getId(), null, "system");
                activeCode = newCode.getCode();
            }
        }

        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ZipOutputStream zos = new ZipOutputStream(baos);

            // 1. Write agent.properties
            String propsContent = String.format(
                    "# NeuroSys Agent Configuration Package\n" +
                    "# Generated for: %s (%s)\n" +
                    "server.url=https://realtimesystemmonitoring-production.up.railway.app/api/v1\n" +
                    "agent.enrollment.code=%s\n" +
                    "agent.lab.name=%s\n" +
                    "agent.collection.interval.seconds=1\n",
                    lab.getName(), lab.getCode(), activeCode, lab.getName()
            );
            ZipEntry propsEntry = new ZipEntry("agent.properties");
            zos.putNextEntry(propsEntry);
            zos.write(propsContent.getBytes());
            zos.closeEntry();

            // 2. Include helper batch files from neurosys-agent directory if available
            addFileToZip(zos, "setup-agent.bat", Paths.get("..", "neurosys-agent", "setup-agent.bat"), Paths.get("neurosys-agent", "setup-agent.bat"));
            addFileToZip(zos, "start-agent.bat", Paths.get("..", "neurosys-agent", "start-agent.bat"), Paths.get("neurosys-agent", "start-agent.bat"));
            addFileToZip(zos, "stop-agent.bat", Paths.get("..", "neurosys-agent", "stop-agent.bat"), Paths.get("neurosys-agent", "stop-agent.bat"));
            addFileToZip(zos, "uninstall-agent.bat", Paths.get("..", "neurosys-agent", "uninstall-agent.bat"), Paths.get("neurosys-agent", "uninstall-agent.bat"));
            addFileToZip(zos, "Run-NeuroSys-Agent.bat", Paths.get("..", "neurosys-agent", "Run-NeuroSys-Agent.bat"), Paths.get("neurosys-agent", "Run-NeuroSys-Agent.bat"));
            addFileToZip(zos, "install-service.ps1", Paths.get("..", "neurosys-agent", "install-service.ps1"), Paths.get("neurosys-agent", "install-service.ps1"));

            // 3. Write README-INSTALLATION.txt
            String readmeContent = String.format(
                    "=========================================================\n" +
                    "NEUROSYS AGENT ONBOARDING PACKAGE\n" +
                    "Target Laboratory: %s (%s)\n" +
                    "Enrollment Code:   %s\n" +
                    "Generated At:      %s\n" +
                    "=========================================================\n\n" +
                    "INSTRUCTIONS FOR SYSTEM ADMINISTRATORS:\n\n" +
                    "1. Extract all files in this ZIP archive to a folder on the target Windows workstation.\n" +
                    "2. Run 'setup-agent.bat' once to register the background agent service.\n" +
                    "3. Use 'start-agent.bat' or 'stop-agent.bat' to start or stop the agent.\n" +
                    "4. To completely remove the agent, run 'uninstall-agent.bat'.\n\n" +
                    "Need assistance? Contact your NeuroSys Lab Supervisor.\n",
                    lab.getName(), lab.getCode(), activeCode, Instant.now().toString()
            );
            ZipEntry readmeEntry = new ZipEntry("README-INSTALLATION.txt");
            zos.putNextEntry(readmeEntry);
            zos.write(readmeContent.getBytes());
            zos.closeEntry();

            // 4. Attach compiled neurosys-agent JAR under both exact and alias names for backwards compatibility
            Path agentJarPath = Paths.get("..", "neurosys-agent", "target", "neurosys-agent-1.0.0-SNAPSHOT-exec.jar");
            if (!Files.exists(agentJarPath)) {
                agentJarPath = Paths.get("neurosys-agent", "target", "neurosys-agent-1.0.0-SNAPSHOT-exec.jar");
            }
            if (!Files.exists(agentJarPath)) {
                agentJarPath = Paths.get("neurosys-agent-1.0.0-SNAPSHOT-exec.jar");
            }
            if (Files.exists(agentJarPath)) {
                byte[] jarBytes = Files.readAllBytes(agentJarPath);

                ZipEntry jarEntry1 = new ZipEntry("neurosys-agent-1.0.0-SNAPSHOT-exec.jar");
                zos.putNextEntry(jarEntry1);
                zos.write(jarBytes);
                zos.closeEntry();

                ZipEntry jarEntry2 = new ZipEntry("neurosys-agent-1.0.0.jar");
                zos.putNextEntry(jarEntry2);
                zos.write(jarBytes);
                zos.closeEntry();
            }

            zos.finish();
            zos.close();

            byte[] zipData = baos.toByteArray();
            String filename = String.format("neurosys-agent-%s.zip", lab.getCode().replaceAll("[^A-Za-z0-9]", ""));

            log.info("[INFO] Generated Agent Package ZIP for Lab {} ({}), Size: {} bytes", lab.getName(), lab.getCode(), zipData.length);

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .body(zipData);

        } catch (Exception e) {
            log.error("Failed to generate agent download package ZIP", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    private void addFileToZip(ZipOutputStream zos, String entryName, Path path1, Path path2) {
        try {
            Path targetPath = Files.exists(path1) ? path1 : (Files.exists(path2) ? path2 : null);
            if (targetPath != null) {
                byte[] content = Files.readAllBytes(targetPath);
                ZipEntry entry = new ZipEntry(entryName);
                zos.putNextEntry(entry);
                zos.write(content);
                zos.closeEntry();
            }
        } catch (Exception e) {
            log.warn("Could not bundle file {} in zip: {}", entryName, e.getMessage());
        }
    }
}
