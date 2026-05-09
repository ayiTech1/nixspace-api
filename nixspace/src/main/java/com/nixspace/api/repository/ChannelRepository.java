package com.nixspace.api.repository;

import com.nixspace.domain.enums.ChannelType;
import com.nixspace.domain.model.Channel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChannelRepository extends JpaRepository<Channel, String> {

    Channel findByChannelId(String channelId);

    List<Channel> findByWorkspaceIdAndTypeAndArchivedFalse(String workspaceId, ChannelType type);

    List<Channel> findByChannelIdInAndWorkspaceIdAndArchivedFalse(List<String> channelIds, String workspaceId);
}