package com.dmdr.personal.portal.booking.model;

	//Override can be created only as ACTIVE. Override will be automatically move to ARCHIVED after overrideEndInstant.
	public enum OverrideStatus {
        ACTIVE,
		ARCHIVED
    }