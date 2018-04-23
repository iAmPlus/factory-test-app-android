package com.iamplus.earin.communication.cap.protocols;

public enum CapProtocolUpgradeState {
	IDLE,
	PENDING_CONFIRMATION,
	SUCCESSFUL_CONFIRMATION,
	PENDING_RESPONSE,
	SUCCESSFUL_RESPONSE,
	FAILED,
	TIMEOUT
}
