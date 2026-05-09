package com.nixspace.api.repository;

import com.nixspace.domain.model.ChannelMember;
import com.nixspace.domain.model.ids.ChannelMemberId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChannelMemberRepository extends JpaRepository<ChannelMember, ChannelMemberId> {

    ChannelMember findByChannelIdAndUserId(String channelId, String userId);

    List<ChannelMember> findByChannelId(String channelId);

    boolean existsByChannelIdAndUserId(String channelId, String userId);

    void deleteByChannelId(String channelId);

    List<ChannelMember> findByUserId(String userId);
}
