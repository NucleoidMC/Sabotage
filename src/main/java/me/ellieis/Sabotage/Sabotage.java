package me.ellieis.Sabotage;

import me.ellieis.Sabotage.game.SabotageConfig;
import me.ellieis.Sabotage.game.phase.SabotageWaiting;
import net.fabricmc.api.ModInitializer;

import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import xyz.nucleoid.plasmid.game.GameType;

public class Sabotage implements ModInitializer {
	public static final String MOD_ID = "sabotage";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
	private static final Identifier SABOTAGE_ID = new Identifier(MOD_ID, "sabotage");
	public static final GameType GAME_TYPE = GameType.register(SABOTAGE_ID, SabotageConfig.CODEC, SabotageWaiting::Open);

	@Override
	public void onInitialize() {

	}
}