package net.electricdog.didigetrobbed;

import net.fabricmc.api.ModInitializer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DidIGetRobbed implements ModInitializer {
	public static final String MOD_ID = "didigetrobbed";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		LOGGER.info("[DidIGetRobbed] Almost ready to protect your chests.");
	}
}