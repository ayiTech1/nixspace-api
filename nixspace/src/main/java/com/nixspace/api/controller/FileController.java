package com.nixspace.api.controller;

import com.nixspace.api.dto.response.Responses.*;
import com.nixspace.domain.service.FileService;
import com.nixspace.security.UserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/workspaces/{workspaceId}/files")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Files", description = "File upload (presigned S3 URL flow) and management")
public class FileController {

    private final FileService fileService;

    @PostMapping("/upload-url")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Request a presigned S3 upload URL. Upload the file directly, then call /confirm.")
    public FileUploadUrlResponse requestUploadUrl(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long workspaceId,
            @RequestParam @NotBlank  String fileName,
            @RequestParam @NotBlank  String mimeType,
            @RequestParam @Positive  long   fileSize
    ) {
        return fileService.requestUploadUrl(principal.getId(), workspaceId, fileName, mimeType, fileSize);
    }

    @PostMapping("/confirm")
    @Operation(summary = "Confirm an S3 upload is complete and get the file record with a download URL")
    public FileResponse confirmUpload(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long workspaceId,
            @RequestParam @NotBlank String storageKey
    ) {
        return fileService.confirmUpload(principal.getId(), workspaceId, storageKey);
    }

    @GetMapping
    @Operation(summary = "List all files in a workspace (paginated)")
    public PagedResponse<FileResponse> getWorkspaceFiles(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long workspaceId,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return fileService.getWorkspaceFiles(
                principal.getId(), workspaceId,
                PageRequest.of(page, Math.min(size, 100), Sort.by("createdAt").descending())
        );
    }

    @GetMapping("/{fileId}")
    @Operation(summary = "Get a file record and its presigned download URL")
    public FileResponse getFile(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long workspaceId,
            @PathVariable Long fileId
    ) {
        return fileService.getFile(principal.getId(), fileId);
    }

    @DeleteMapping("/{fileId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete a file (uploader only)")
    public void deleteFile(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long workspaceId,
            @PathVariable Long fileId
    ) {
        fileService.deleteFile(principal.getId(), fileId);
    }
}
