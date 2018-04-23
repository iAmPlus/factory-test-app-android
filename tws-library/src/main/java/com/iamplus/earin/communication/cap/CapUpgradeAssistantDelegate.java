package com.iamplus.earin.communication.cap;

import java.util.Date;

/**
 * Created by Markus Millfjord on 2017-02-08.
 */
public interface CapUpgradeAssistantDelegate
{
	public void upgradeAssistantChangedState(CapUpgradeAssistant assistant, CapUpgradeAssistantState state, int progress, Date estimate);
	public void upgradeAssistantFailed(CapUpgradeAssistant assistant, CapUpgradeAssistantError error, String reason);

	public void shouldRebootAndResume(CapUpgradeAssistant assistant);
	public void shouldCommitUpgrade(CapUpgradeAssistant assistant);
	public void shouldProceedAtTransferComplete(CapUpgradeAssistant assistant);
}
