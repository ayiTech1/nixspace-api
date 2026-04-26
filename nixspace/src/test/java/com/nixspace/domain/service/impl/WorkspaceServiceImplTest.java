package com.nixspace.domain.service.impl;

import com.nixspace.api.dto.request.WorkspaceRequests.*;
import com.nixspace.api.dto.response.Responses.*;
import com.nixspace.api.exception.NixSpaceExceptions.*;
import com.nixspace.api.mapper.WorkspaceMapper;
import com.nixspace.common.enums.WorkspaceRole;
import com.nixspace.domain.model.*;
import com.nixspace.domain.repository.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("WorkspaceServiceImpl")
class WorkspaceServiceImplTest {

    @Mock WorkspaceRepository       workspaceRepository;
    @Mock WorkspaceMemberRepository workspaceMemberRepository;
    @Mock UserRepository            userRepository;
    @Mock ChannelRepository         channelRepository;
    @Mock WorkspaceMapper           workspaceMapper;

    @InjectMocks WorkspaceServiceImpl workspaceService;

    // ─── Fixtures ──────────────────────────────────────────────────────────

    private User user(Long id) {
        return User.builder().id(id).email("user" + id + "@test.com")
                .displayName("User " + id).active(true).build();
    }

    private Workspace workspace(Long id, User owner) {
        return Workspace.builder().id(id).name("Acme").slug("acme")
                .owner(owner).active(true).build();
    }

    private WorkspaceMember member(Workspace ws, User u, WorkspaceRole role) {
        return WorkspaceMember.builder().workspace(ws).user(u).role(role).build();
    }

    private WorkspaceResponse wsResponse(Workspace ws) {
        return new WorkspaceResponse(ws.getId(), ws.getName(), ws.getSlug(),
                null, null, null, 1L, null);
    }

    // ─── createWorkspace() ─────────────────────────────────────────────────

    @Nested
    @DisplayName("createWorkspace()")
    class CreateWorkspace {

        @Test
        @DisplayName("creates workspace, owner member and default #general channel")
        void create_success() {
            User owner = user(1L);
            var req = new CreateWorkspaceRequest("Acme Corp", "acme-corp", "Our workspace");

            when(workspaceRepository.existsBySlug("acme-corp")).thenReturn(false);
            when(userRepository.findById(1L)).thenReturn(Optional.of(owner));
            when(workspaceRepository.save(any(Workspace.class))).thenAnswer(i -> {
                Workspace w = i.getArgument(0);
                w.setId(100L);
                return w;
            });
            when(workspaceMapper.toResponse(any())).thenAnswer(i ->
                    wsResponse(i.getArgument(0)));

            WorkspaceResponse resp = workspaceService.createWorkspace(1L, req);

            assertThat(resp).isNotNull();
            verify(workspaceMemberRepository).save(argThat(m ->
                    m.getRole() == WorkspaceRole.OWNER));
            verify(channelRepository).save(argThat(c ->
                    c.getName().equals("general") && c.isDefaultChannel()));
        }

        @Test
        @DisplayName("throws DuplicateResourceException for a taken slug")
        void create_slugTaken_throws() {
            when(workspaceRepository.existsBySlug("acme")).thenReturn(true);

            assertThatThrownBy(() -> workspaceService.createWorkspace(1L,
                    new CreateWorkspaceRequest("Acme", "acme", null)))
                    .isInstanceOf(DuplicateResourceException.class)
                    .hasMessageContaining("acme");
        }
    }

    // ─── getWorkspace() ────────────────────────────────────────────────────

    @Nested
    @DisplayName("getWorkspace()")
    class GetWorkspace {

        @Test
        @DisplayName("returns workspace for an active member")
        void get_success() {
            User owner = user(1L);
            Workspace ws = workspace(10L, owner);

            when(workspaceRepository.findById(10L)).thenReturn(Optional.of(ws));
            when(workspaceMemberRepository.existsByWorkspaceIdAndUserId(10L, 1L)).thenReturn(true);
            when(workspaceMapper.toResponse(ws)).thenReturn(wsResponse(ws));

            assertThat(workspaceService.getWorkspace(1L, 10L)).isNotNull();
        }

        @Test
        @DisplayName("throws AccessDeniedException for non-members")
        void get_nonMember_throws() {
            User owner = user(1L);
            Workspace ws = workspace(10L, owner);

            when(workspaceRepository.findById(10L)).thenReturn(Optional.of(ws));
            when(workspaceMemberRepository.existsByWorkspaceIdAndUserId(10L, 99L)).thenReturn(false);

            assertThatThrownBy(() -> workspaceService.getWorkspace(99L, 10L))
                    .isInstanceOf(AccessDeniedException.class);
        }
    }

    // ─── inviteMember() ────────────────────────────────────────────────────

    @Nested
    @DisplayName("inviteMember()")
    class InviteMember {

        @Test
        @DisplayName("adds user as MEMBER by default")
        void invite_success() {
            User admin   = user(1L);
            User invitee = user(2L);
            Workspace ws = workspace(10L, admin);

            when(workspaceRepository.findById(10L)).thenReturn(Optional.of(ws));
            when(workspaceMemberRepository.existsByWorkspaceIdAndUserId(10L, 1L)).thenReturn(true);
            when(workspaceMemberRepository.findRoleByWorkspaceIdAndUserId(10L, 1L))
                    .thenReturn(Optional.of(WorkspaceRole.ADMIN));
            when(userRepository.findByEmail("user2@test.com")).thenReturn(Optional.of(invitee));
            when(workspaceMemberRepository.existsByWorkspaceIdAndUserId(10L, 2L)).thenReturn(false);
            when(workspaceRepository.getReferenceById(10L)).thenReturn(ws);
            when(workspaceMemberRepository.save(any())).thenAnswer(i -> i.getArgument(0));
            when(workspaceMapper.toMemberResponse(any())).thenReturn(
                    new WorkspaceMemberResponse(1L, null, WorkspaceRole.MEMBER, null));

            WorkspaceMemberResponse resp = workspaceService.inviteMember(1L, 10L,
                    new InviteMemberRequest("user2@test.com", null));

            assertThat(resp.role()).isEqualTo(WorkspaceRole.MEMBER);
        }

        @Test
        @DisplayName("throws DuplicateResourceException when already a member")
        void invite_alreadyMember_throws() {
            User admin   = user(1L);
            User invitee = user(2L);
            Workspace ws = workspace(10L, admin);

            when(workspaceRepository.findById(10L)).thenReturn(Optional.of(ws));
            when(workspaceMemberRepository.existsByWorkspaceIdAndUserId(10L, 1L)).thenReturn(true);
            when(workspaceMemberRepository.findRoleByWorkspaceIdAndUserId(10L, 1L))
                    .thenReturn(Optional.of(WorkspaceRole.ADMIN));
            when(userRepository.findByEmail("user2@test.com")).thenReturn(Optional.of(invitee));
            when(workspaceMemberRepository.existsByWorkspaceIdAndUserId(10L, 2L)).thenReturn(true);

            assertThatThrownBy(() -> workspaceService.inviteMember(1L, 10L,
                    new InviteMemberRequest("user2@test.com", null)))
                    .isInstanceOf(DuplicateResourceException.class);
        }
    }

    // ─── leaveWorkspace() ──────────────────────────────────────────────────

    @Nested
    @DisplayName("leaveWorkspace()")
    class LeaveWorkspace {

        @Test
        @DisplayName("removes MEMBER from workspace successfully")
        void leave_member_success() {
            User member = user(2L);
            Workspace ws = workspace(10L, user(1L));
            WorkspaceMember wm = member(ws, member, WorkspaceRole.MEMBER);

            when(workspaceMemberRepository.findByWorkspaceIdAndUserId(10L, 2L))
                    .thenReturn(Optional.of(wm));

            workspaceService.leaveWorkspace(2L, 10L);

            verify(workspaceMemberRepository).delete(wm);
        }

        @Test
        @DisplayName("throws BusinessRuleException when OWNER tries to leave")
        void leave_owner_throws() {
            User owner = user(1L);
            Workspace ws = workspace(10L, owner);
            WorkspaceMember wm = member(ws, owner, WorkspaceRole.OWNER);

            when(workspaceMemberRepository.findByWorkspaceIdAndUserId(10L, 1L))
                    .thenReturn(Optional.of(wm));

            assertThatThrownBy(() -> workspaceService.leaveWorkspace(1L, 10L))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessageContaining("owner");
        }
    }

    // ─── updateMemberRole() ────────────────────────────────────────────────

    @Nested
    @DisplayName("updateMemberRole()")
    class UpdateMemberRole {

        @Test
        @DisplayName("owner can promote MEMBER to ADMIN")
        void promote_success() {
            User owner  = user(1L);
            User target = user(2L);
            Workspace ws = workspace(10L, owner);
            WorkspaceMember targetMember = member(ws, target, WorkspaceRole.MEMBER);

            when(workspaceMemberRepository.findRoleByWorkspaceIdAndUserId(10L, 1L))
                    .thenReturn(Optional.of(WorkspaceRole.OWNER));
            when(workspaceMemberRepository.findByWorkspaceIdAndUserId(10L, 2L))
                    .thenReturn(Optional.of(targetMember));
            when(workspaceMemberRepository.save(targetMember)).thenReturn(targetMember);
            when(workspaceMapper.toMemberResponse(targetMember))
                    .thenReturn(new WorkspaceMemberResponse(1L, null, WorkspaceRole.ADMIN, null));

            var resp = workspaceService.updateMemberRole(1L, 10L, 2L,
                    new UpdateMemberRoleRequest(WorkspaceRole.ADMIN));

            assertThat(resp.role()).isEqualTo(WorkspaceRole.ADMIN);
            assertThat(targetMember.getRole()).isEqualTo(WorkspaceRole.ADMIN);
        }

        @Test
        @DisplayName("throws BusinessRuleException when trying to change OWNER role")
        void changeOwnerRole_throws() {
            User owner  = user(1L);
            Workspace ws = workspace(10L, owner);
            WorkspaceMember ownerMember = member(ws, owner, WorkspaceRole.OWNER);

            when(workspaceMemberRepository.findRoleByWorkspaceIdAndUserId(10L, 1L))
                    .thenReturn(Optional.of(WorkspaceRole.OWNER));
            when(workspaceMemberRepository.findByWorkspaceIdAndUserId(10L, 1L))
                    .thenReturn(Optional.of(ownerMember));

            assertThatThrownBy(() -> workspaceService.updateMemberRole(1L, 10L, 1L,
                    new UpdateMemberRoleRequest(WorkspaceRole.MEMBER)))
                    .isInstanceOf(BusinessRuleException.class);
        }
    }
}
