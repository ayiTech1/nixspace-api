//package com.nixspace.application.controllers;
//
//import com.nixspace.application.models.constants.ResponseCode;
//import com.nixspace.application.models.requests.FileUploadRequest;
//import com.nixspace.application.models.responses.OperationResponse;
//import com.nixspace.application.services.FileService;
//import jakarta.validation.ConstraintViolation;
//import jakarta.validation.ConstraintViolationException;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.http.HttpStatus;
//import org.springframework.validation.annotation.Validated;
//import org.springframework.web.bind.annotation.*;
//
//import java.util.Set;
//
//@RestController
//@RequestMapping("file")
//@Slf4j
//public class FileController {
//
//    @Autowired
//    FileService fileService;
//
//
//    @PostMapping("upload")
//    public OperationResponse uploadFileRecord(@RequestBody @Validated FileUploadRequest fileUploadRequest) {
//        OperationResponse uploadFileRecordResponse = fileService.uploadFileRecord(fileUploadRequest);
//        log.info("Response for uploadFileRecord {} {}", fileUploadRequest, uploadFileRecordResponse);
//        return uploadFileRecordResponse;
//    }
//
//    @GetMapping("list/{workspaceId}")
//    public OperationResponse listWorkspaceFiles(@PathVariable String workspaceId,
//                                                @RequestParam(defaultValue = "0") int page,
//                                                @RequestParam(defaultValue = "20") int size) {
//        OperationResponse listWorkspaceFilesResponse = fileService.listWorkspaceFiles(workspaceId, page, size);
//        log.info("Response for listWorkspaceFiles {} {} {} {}", workspaceId, page, size, listWorkspaceFilesResponse);
//        return listWorkspaceFilesResponse;
//    }
//
//    @GetMapping("retrieve/{fileEntityId}")
//    public OperationResponse getFileRecord(@PathVariable String fileEntityId) {
//        OperationResponse getFileRecordResponse = fileService.getFileRecord(fileEntityId);
//        log.info("Response for getFileRecord {} {}", fileEntityId, getFileRecordResponse);
//        return getFileRecordResponse;
//    }
//
//    @PostMapping("delete/{fileEntityId}/{uploadedBy}")
//    public OperationResponse deleteFile(@PathVariable String fileEntityId, @PathVariable String uploadedBy) {
//        OperationResponse deleteFileResponse = fileService.deleteFile(fileEntityId, uploadedBy);
//        log.info("Response for deleteFile {} {} {}", fileEntityId, uploadedBy, deleteFileResponse);
//        return deleteFileResponse;
//    }
//
//
//    @ResponseStatus(HttpStatus.BAD_REQUEST)
//    @ExceptionHandler(ConstraintViolationException.class)
//    public OperationResponse handleConstraintViolationException(ConstraintViolationException e) {
//        Set<ConstraintViolation<?>> violations = e.getConstraintViolations();
//        StringBuilder strBuilder = new StringBuilder();
//        OperationResponse operationResponse = new OperationResponse();
//        operationResponse.setResponseCode(ResponseCode.BAD_REQUEST);
//        for (ConstraintViolation<?> violation : violations) {
//            strBuilder.append(violation.getMessage()).append(", ");
//        }
//        operationResponse.setResponseMessage(strBuilder.toString());
//        return operationResponse;
//    }
//}