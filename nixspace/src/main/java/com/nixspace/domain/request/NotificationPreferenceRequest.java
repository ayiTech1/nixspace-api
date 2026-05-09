package com.nixspace.domain.request;


import com.nixspace.domain.enums.NotificationPreference;
import lombok.Data;

@Data
public class NotificationPreferenceRequest {
    private NotificationPreference notificationPreference;
}
