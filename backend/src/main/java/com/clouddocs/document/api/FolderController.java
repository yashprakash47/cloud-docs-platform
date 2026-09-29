package com.clouddocs.document.api;
import com.clouddocs.document.application.FolderService; import com.clouddocs.document.domain.Folder; import com.clouddocs.identity.application.AuthenticationService; import jakarta.servlet.http.HttpServletRequest; import jakarta.validation.Valid; import org.springframework.http.*; import org.springframework.web.bind.annotation.*; import java.util.*;
@RestController @RequestMapping("/api/v1/tenants/{tenantId}/folders") public class FolderController {private final AuthenticationService authentication;private final FolderService service;public FolderController(AuthenticationService a,FolderService s){authentication=a;service=s;}
 @PostMapping @ResponseStatus(HttpStatus.CREATED) public Folder create(@PathVariable UUID tenantId,@Valid @RequestBody CreateFolderRequest b,HttpServletRequest r){return service.create(authentication.requireCurrentUser(r),tenantId,b.name(),b.parentId());}
 @GetMapping public List<Folder> list(@PathVariable UUID tenantId,@RequestParam(required=false) UUID parentId,HttpServletRequest r){return service.list(authentication.requireCurrentUser(r),tenantId,parentId);}
 @PatchMapping("/{id}") public Folder update(@PathVariable UUID tenantId,@PathVariable UUID id,@Valid @RequestBody UpdateFolderRequest b,HttpServletRequest r){return service.update(authentication.requireCurrentUser(r),tenantId,id,b.name(),b.parentId());}
}
